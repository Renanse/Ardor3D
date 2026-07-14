/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.checkers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ardor3d.compose.SceneComposition
import com.ardor3d.editor.play.GameContext
import com.ardor3d.editor.play.GameInput
import com.ardor3d.editor.play.GameMode
import com.ardor3d.math.Vector3
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import com.ardor3d.scenegraph.extension.CameraNode

/**
 * A playable game of checkers, driven by the editor's play mode. It is the worked example of the
 * plan's state/view split and the [GameMode] SPI, with the view side composed: the pure [Board]
 * and the highlight set are snapshot state, [BoardScene] declares the scene as a function of them,
 * and this class only advances the model, writes that state, and drives one
 * [SceneComposition.frame] per update - recomposition does the view sync that used to be
 * hand-written here.
 *
 * Interaction: hovering a movable piece previews its legal moves; clicking one selects it (it
 * stays lit with its destinations), and the destination under the pointer highlights distinctly.
 * Clicking a destination moves (a jump plays its whole mandatory chain); clicking another of your
 * pieces reselects; clicking elsewhere clears. Moves animate with a short slide; input is ignored
 * mid-slide and once the game is over.
 *
 * The move slide is the deliberate exception to declarative sync: it writes the sliding piece's
 * translation imperatively every frame, with zero recomposition mid-slide. Composition tolerates
 * that because [BoardScene] only rewrites a translation when the piece's square changes - and the
 * post-move snap it then writes is the slide's own endpoint.
 */
class CheckersGame : GameMode {
    private var context: GameContext? = null

    // Snapshot state - everything the composed view reads. Writing these is the entire view sync.
    private var board by mutableStateOf(Board.initial())
    private var highlights by mutableStateOf<Map<Square, HighlightKind>>(emptyMap())

    private var selected: Square? = null

    // The composed scene: built per session in onStart, closed on stop.
    private var boardRoot: Node? = null
    private var composition: SceneComposition? = null
    private var frameNanos = 0L
    private var syncs = 0

    // Move animation: slide the moving piece from its old square to its new one before the board
    // state flips to the applied position (imperative by design - see the class doc).
    private var animating = false
    private var animElapsed = 0.0
    private var animNode: Node? = null
    private val animFrom = Vector3()
    private val animTo = Vector3()
    private var pendingBoard: Board? = null

    override fun onStart(context: GameContext) {
        this.context = context
        board = Board.initial()
        highlights = emptyMap()
        selected = null
        animating = false
        pendingBoard = null

        // Take over the scene: clear the editing content (keeping camera objects, which play mode
        // renders through) so the board stands alone. This is inside the snapshot boundary, so Stop
        // restores whatever was here.
        context.sceneRoot.children.filter { it !is CameraNode }.toList()
            .forEach { context.sceneRoot.detachChild(it) }

        val root = Node("Checkers")
        boardRoot = root
        composition = SceneComposition(root).apply {
            setContent {
                BoardScene(
                    board = { board },
                    highlights = { highlights },
                    materialize = context::materialize,
                    onSync = { syncs++ }
                )
            }
        }
        context.sceneRoot.attachChild(root)
        context.setStatus(statusText())
    }

    override fun update(timePerFrame: Double, input: GameInput) {
        val context = this.context ?: return
        frameNanos += (timePerFrame * 1_000_000_000).toLong()
        if (animating) {
            advanceAnimation(timePerFrame)
        } else if (!board.isGameOver()) {
            // The square under the pointer this frame (null = off the board).
            val hover = squareAt(context, input.mouseX, input.mouseY)
            val click = input.click
            if (click != null) {
                // Resolve the click where it happened, not where the pointer is now - the pointer
                // can move between the release and this frame's poll.
                processClick(squareAt(context, click.x, click.y))
            }
            // beginMove already cleared the highlights if a move just started.
            if (!animating) highlights = computeHighlights(hover)
        }
        // One composition frame per game update: state written above (including by finalizeMove)
        // lands in the scene now, synchronously. An idle frame does nothing.
        composition?.frame(frameNanos)
    }

    override fun onStop() {
        composition?.close()
        composition = null
        boardRoot = null
        context?.setStatus(null)
        context = null
        // The board scene itself is thrown away when the pre-play scene is restored.
    }

    // --- interaction -------------------------------------------------------------------------

    /** Handles a click on [square] (null = off the board): select, move, reselect, or deselect. */
    private fun processClick(square: Square?) {
        if (square == null) {
            deselect()
            return
        }
        val from = selected
        if (from == null) {
            trySelect(square)
            return
        }
        val move = board.legalMovesFrom(from).firstOrNull { it.to == square }
        when {
            move != null -> beginMove(move)
            isMovable(square) -> selected = square // clicked another of my movable pieces: reselect
            else -> deselect()
        }
    }

    /** Selects [square] if it holds a piece of the side to move that has at least one legal move. */
    private fun trySelect(square: Square) {
        if (isMovable(square)) selected = square
    }

    private fun isMovable(square: Square): Boolean {
        val piece = board.pieceAt(square) ?: return false
        return piece.color == board.toMove && board.legalMovesFrom(square).isNotEmpty()
    }

    private fun deselect() {
        selected = null
    }

    // --- highlighting ------------------------------------------------------------------------

    private fun computeHighlights(hover: Square?): Map<Square, HighlightKind> {
        val map = linkedMapOf<Square, HighlightKind>()
        val from = selected
        if (from != null) {
            // A piece is committed: mark it and its destinations; light the one under the pointer.
            map[from] = HighlightKind.SELECTED
            for (dest in board.legalMovesFrom(from).map { it.to }) {
                map[dest] = if (dest == hover) HighlightKind.MOVE_HOVER else HighlightKind.MOVE
            }
        } else if (hover != null && isMovable(hover)) {
            // Nothing committed: preview the hovered piece's moves.
            map[hover] = HighlightKind.HOVER_PIECE
            for (dest in board.legalMovesFrom(hover).map { it.to }) {
                map[dest] = HighlightKind.PREVIEW
            }
        }
        return map
    }

    // --- the move ----------------------------------------------------------------------------

    private fun beginMove(move: Move) {
        deselect()
        highlights = emptyMap() // clear highlights for the duration of the slide
        pendingBoard = board.apply(move)
        val node = boardRoot?.let { pieceNodeAt(it, move.from) }
        if (node == null) {
            finalizeMove() // nothing to animate; just snap
            return
        }
        animNode = node
        animFrom.set(worldPositionOf(move.from))
        animTo.set(worldPositionOf(move.to))
        animElapsed = 0.0
        animating = true
    }

    private fun advanceAnimation(timePerFrame: Double) {
        animElapsed += timePerFrame
        val t = (animElapsed / ANIM_DURATION).coerceIn(0.0, 1.0)
        animNode?.setTranslation(
            animFrom.x + (animTo.x - animFrom.x) * t,
            animFrom.y + (animTo.y - animFrom.y) * t,
            animFrom.z + (animTo.z - animFrom.z) * t
        )
        if (t >= 1.0) finalizeMove()
    }

    private fun finalizeMove() {
        board = pendingBoard ?: board // recomposition restructures the pieces from this write
        pendingBoard = null
        animating = false
        animNode = null
        highlights = emptyMap()
        context?.setStatus(statusText())
    }

    // --- test seams (same package/module only) -----------------------------------------------

    /** The current model position - for interaction tests. */
    internal val boardForTest: Board get() = board

    /** Whether a move slide is in progress - for interaction tests. */
    internal val isAnimatingForTest: Boolean get() = animating

    /** Number of highlighted squares - for interaction tests. */
    internal val highlightCountForTest: Int get() = highlights.size

    /** The highlight kind shown on [square], or null - for interaction tests. */
    internal fun highlightForTest(square: Square): HighlightKind? = highlights[square]

    /** The composed piece node on [square] (as of the last applied frame), or null - for tests. */
    internal fun pieceNodeForTest(square: Square): Node? = boardRoot?.let { pieceNodeAt(it, square) }

    /** How many times a state-driven subtree has (re)composed - the slide-silence gate reads this. */
    internal val syncCountForTest: Int get() = syncs

    /** Simulates a click at [square] (null = off the board), then refreshes highlights - for tests. */
    internal fun clickSquareForTest(square: Square?) {
        processClick(square)
        if (!animating) highlights = computeHighlights(square)
    }

    /** Simulates the pointer moving to [square] (null = off the board) - for tests. */
    internal fun hoverSquareForTest(square: Square?) {
        if (!animating) highlights = computeHighlights(square)
    }

    // --- helpers -----------------------------------------------------------------------------

    private fun squareAt(context: GameContext, x: Int, y: Int): Square? {
        val results = context.pick(x, y)
        if (results.number == 0) return null
        val target = results.getPickData(0).target as? Spatial ?: return null
        return squareForTile(target)
    }

    private fun statusText(): String {
        if (board.isGameOver()) {
            val winner = board.winner() ?: return "Draw"
            return "${label(winner)} wins!"
        }
        return "${label(board.toMove)} to move"
    }

    private fun label(color: PieceColor): String = if (color == PieceColor.RED) "Red" else "Black"

    companion object {
        private const val ANIM_DURATION = 0.22
    }
}
