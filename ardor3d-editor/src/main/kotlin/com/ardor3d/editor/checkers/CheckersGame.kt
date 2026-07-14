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

import com.ardor3d.editor.play.GameContext
import com.ardor3d.editor.play.GameInput
import com.ardor3d.editor.play.GameMode
import com.ardor3d.light.PointLight
import com.ardor3d.math.Vector3
import com.ardor3d.scenegraph.Spatial
import com.ardor3d.scenegraph.extension.CameraNode

/**
 * A playable game of checkers, driven by the editor's play mode. It is the worked example of the
 * plan's state/view split and the [GameMode] SPI: the pure [Board] is the authoritative state, the
 * [CheckersView] is its scene view, and this class is the only place they meet - it turns clicks
 * into model moves and re-syncs the view.
 *
 * Interaction: hovering a movable piece previews its legal moves; clicking one selects it (it
 * stays lit with its destinations), and the destination under the pointer highlights distinctly.
 * Clicking a destination moves (a jump plays its whole mandatory chain); clicking another of your
 * pieces reselects; clicking elsewhere clears. Moves animate with a short slide; input is ignored
 * mid-slide and once the game is over.
 */
class CheckersGame : GameMode {
    private var context: GameContext? = null
    private lateinit var view: CheckersView
    private var board: Board = Board.initial()
    private var selected: Square? = null

    // The highlight set currently shown, so it is only rebuilt/re-materialized when it changes.
    private var lastHighlights: Map<Square, HighlightKind> = emptyMap()

    // Move animation: slide the moving piece from its old square to its new one before the board
    // (and view) snap to the applied position.
    private var animating = false
    private var animElapsed = 0.0
    private var animNode: com.ardor3d.scenegraph.Node? = null
    private val animFrom = Vector3()
    private val animTo = Vector3()
    private var pendingBoard: Board? = null

    override fun onStart(context: GameContext) {
        this.context = context
        board = Board.initial()
        selected = null
        animating = false

        // Take over the scene: clear the editing content (keeping camera objects, which play mode
        // renders through) so the board stands alone. This is inside the snapshot boundary, so Stop
        // restores whatever was here.
        context.sceneRoot.children.filter { it !is CameraNode }.toList()
            .forEach { context.sceneRoot.detachChild(it) }

        val view = CheckersView()
        this.view = view
        view.rebuild(board)

        // Own light so the board reads no matter how the host scene is lit.
        val light = PointLight().apply {
            name = "Checkers Light"
            setTranslation(2.0, 9.0, 3.0)
            intensity = 1.1f
            isEnabled = true
        }
        view.root.attachChild(light)

        context.materialize(view.root)
        context.sceneRoot.attachChild(view.root)
        context.setStatus(statusText())
    }

    override fun update(timePerFrame: Double, input: GameInput) {
        val context = this.context ?: return
        if (animating) {
            advanceAnimation(timePerFrame)
            return
        }
        if (board.isGameOver()) return

        // The square under the pointer this frame (null = off the board).
        val hover = squareAt(context, input.mouseX, input.mouseY)
        val click = input.click
        if (click != null) {
            // Resolve the click where it happened, not where the pointer is now - the pointer can
            // move between the release and this frame's poll.
            processClick(squareAt(context, click.x, click.y))
            if (animating) return // a move started; beginMove already cleared the highlights
        }
        refreshHighlights(hover)
    }

    override fun onStop() {
        context?.setStatus(null)
        context = null
        // view.root is thrown away when the pre-play scene is restored - nothing else to release.
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

    /** Recomputes the hover/selection highlight set and applies it if it changed. */
    private fun refreshHighlights(hover: Square?) {
        applyHighlights(computeHighlights(hover))
    }

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

    private fun applyHighlights(desired: Map<Square, HighlightKind>) {
        if (desired == lastHighlights) return
        lastHighlights = desired
        view.setHighlights(desired)
        context?.materialize(view.highlightRoot)
    }

    private fun beginMove(move: Move) {
        deselect()
        applyHighlights(emptyMap()) // clear highlights for the duration of the slide
        pendingBoard = board.apply(move)
        val node = view.pieceNodeAt(move.from)
        if (node == null) {
            finalizeMove() // nothing to animate; just snap
            return
        }
        animNode = node
        animFrom.set(view.worldPositionOf(move.from))
        animTo.set(view.worldPositionOf(move.to))
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
        board = pendingBoard ?: board
        pendingBoard = null
        animating = false
        animNode = null
        view.rebuild(board)
        applyHighlights(emptyMap())
        context?.let {
            it.materialize(view.root)
            it.setStatus(statusText())
        }
    }

    // --- test seams (same package/module only) -----------------------------------------------

    /** The current model position - for interaction tests. */
    internal val boardForTest: Board get() = board

    /** The scene view - for interaction tests. */
    internal val viewForTest: CheckersView get() = view

    /** Whether a move slide is in progress - for interaction tests. */
    internal val isAnimatingForTest: Boolean get() = animating

    /** Number of highlight markers currently shown - for interaction tests. */
    internal val highlightCountForTest: Int get() = lastHighlights.size

    /** The highlight kind shown on [square], or null - for interaction tests. */
    internal fun highlightForTest(square: Square): HighlightKind? = lastHighlights[square]

    /** Simulates a click at [square] (null = off the board), then refreshes highlights - for tests. */
    internal fun clickSquareForTest(square: Square?) {
        processClick(square)
        if (!animating) refreshHighlights(square)
    }

    /** Simulates the pointer moving to [square] (null = off the board) - for tests. */
    internal fun hoverSquareForTest(square: Square?) {
        if (!animating) refreshHighlights(square)
    }

    // --- helpers -----------------------------------------------------------------------------

    private fun squareAt(context: GameContext, x: Int, y: Int): Square? {
        val results = context.pick(x, y)
        if (results.number == 0) return null
        val target = results.getPickData(0).target as? Spatial ?: return null
        return view.squareForTile(target)
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
