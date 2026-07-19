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
import com.ardor3d.compose.SceneChannels
import com.ardor3d.compose.SceneComposition
import com.ardor3d.compose.SpatialApplier
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
 * stays lit with its destinations), and the destination under the pointer highlights distinctly
 * and breathes (a gentle glow pulse). Clicking a destination moves (a jump plays its whole
 * mandatory chain); clicking another of your pieces reselects; clicking elsewhere clears. Moves
 * animate with a short slide; input is ignored mid-slide and once the game is over.
 *
 * Frame-rate motion - the move slide, the glow pulse - runs through [SceneChannels], not through
 * recomposition: composition binds a channel per piece (and per highlight marker), this class
 * writes the animated values by handle each frame, and one [SceneChannels.sample] after the
 * composition frame applies them. The update loop's order is the contract: advance the model and
 * write channels, [SceneComposition.frame] for structure, then sample - so structural changes
 * (a capture, a crowning) and motion land in a known order every frame, and a slide runs with
 * zero recomposition and zero applier traffic by construction, not by convention.
 */
class CheckersGame(
    /** How the composed scene gets its applier - tests inject a counting one to gate silence. */
    private val applierFactory: (Node) -> SpatialApplier = { SpatialApplier(it) }
) : GameMode {
    private var context: GameContext? = null

    // Snapshot state - everything the composed view reads. Writing these is the entire view sync.
    private var board by mutableStateOf(Board.initial())
    private var highlights by mutableStateOf<Map<Square, HighlightKind>>(emptyMap())

    private var selected: Square? = null

    // The composed scene: built per session in onStart, closed on stop.
    private var boardRoot: Node? = null
    private var composition: SceneComposition? = null
    private var channels: SceneChannels? = null
    private var frameNanos = 0L
    private var syncs = 0

    // Move animation: slide the moving piece from its old square to its new one before the board
    // state flips to the applied position. The slide is a channel writer (see the class doc):
    // the handle is looked up once when the move starts, written every frame, and the t=1 write
    // is the endpoint itself - recomposition never snaps a translation.
    private var animating = false
    private var animElapsed = 0.0
    private var animHandle = 0L
    private val animFrom = Vector3()
    private val animTo = Vector3()
    private var pendingBoard: Board? = null

    // The glow pulse on the destination under the pointer. The channel handle is re-looked-up
    // only when the pulsing square changes (bindings are event-rate); the per-frame work is one
    // writeParam. boundPulse/pulseLive track what the last lookup found.
    private var pulseTime = 0.0
    private var pulseSquare: Square? = null
    private var boundPulse: Square? = null
    private var pulseLive = false
    private var pulseHandle = 0L

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
        // Capacities cover the board plus binding-replacement overlap (a replaced binding holds
        // its slot until the old one is forgotten, so peak occupancy briefly exceeds live count).
        val sceneChannels = SceneChannels(transformCapacity = 32, paramCapacity = 48)
        channels = sceneChannels
        composition = SceneComposition(applierFactory(root)).apply {
            setContent {
                BoardScene(
                    channels = sceneChannels,
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
        pulseTime += timePerFrame
        if (animating) {
            advanceAnimation(timePerFrame)
        } else if (!board.isGameOver()) {
            // The square under the pointer this frame (null = off the board).
            val hover = forcedHoverForTest ?: squareAt(context, input.mouseX, input.mouseY)
            val click = input.click
            if (click != null) {
                // Resolve the click where it happened, not where the pointer is now - the pointer
                // can move between the release and this frame's poll.
                processClick(squareAt(context, click.x, click.y))
            }
            // beginMove already cleared the highlights (and the pulse) if a move just started.
            if (!animating) {
                highlights = computeHighlights(hover)
                pulseSquare = if (hover != null && highlights[hover] == HighlightKind.MOVE_HOVER) hover else null
            }
        }
        // One composition frame per game update: state written above (including by finalizeMove)
        // lands in the scene now, synchronously. An idle frame does nothing.
        composition?.frame(frameNanos)
        // Then motion: the pulse writes its channel (bindings made by the frame above are
        // visible here), and one sample applies everything written this update.
        channels?.let { ch ->
            refreshPulse(ch)
            ch.sample()
        }
    }

    override fun onStop() {
        composition?.close()
        composition = null
        channels = null
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
        pulseSquare = null
        val ch = channels
        val id = board.idAt(move.from)
        pendingBoard = board.apply(move)
        if (ch == null || !ch.hasTransform(id)) {
            finalizeMove() // nothing composed to animate
            return
        }
        // The channel is looked up once, here, by the piece's stable identity - never by
        // re-querying the scene graph - and written every frame until the slide lands.
        animHandle = ch.transformHandle(id)
        animFrom.set(worldPositionOf(move.from))
        animTo.set(worldPositionOf(move.to))
        animElapsed = 0.0
        animating = true
    }

    private fun advanceAnimation(timePerFrame: Double) {
        animElapsed += timePerFrame
        val t = (animElapsed / ANIM_DURATION).coerceIn(0.0, 1.0)
        channels?.writeTranslation(
            animHandle,
            animFrom.x + (animTo.x - animFrom.x) * t,
            animFrom.y + (animTo.y - animFrom.y) * t,
            animFrom.z + (animTo.z - animFrom.z) * t
        )
        // The t=1 write above is the endpoint itself; the board flip below only restructures.
        if (t >= 1.0) finalizeMove()
    }

    private fun finalizeMove() {
        board = pendingBoard ?: board // recomposition restructures the pieces from this write
        pendingBoard = null
        animating = false
        highlights = emptyMap()
        context?.setStatus(statusText())
    }

    // --- the glow pulse ----------------------------------------------------------------------

    /**
     * Drives the breathing glow on the destination marker under the pointer. Handle lookup and
     * the leave-at-rest write happen only when the pulsing square changes; steady pulsing is one
     * [SceneChannels.writeParam] per frame. Runs after the composition frame, so a marker bound
     * (or disposed) by this frame's recomposition is already reflected in the registry.
     */
    private fun refreshPulse(ch: SceneChannels) {
        if (pulseSquare != boundPulse) {
            val old = boundPulse
            if (old != null && pulseLive && ch.hasParam(old) && ch.paramHandle(old) == pulseHandle) {
                ch.writeParam(pulseHandle, GLOW_REST) // the marker we leave returns to rest
            }
            boundPulse = pulseSquare
            val square = pulseSquare
            pulseLive = square != null && ch.hasParam(square)
            if (pulseLive) pulseHandle = ch.paramHandle(square!!)
        }
        if (pulseLive) {
            ch.writeParam(pulseHandle, (1.0 - PULSE_DEPTH * (0.5 - 0.5 * Math.cos(pulseTime * PULSE_RADIANS))).toFloat())
        }
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

    /** Pins the hover for update()-driven tests, whose fake pick would otherwise see nothing. */
    internal var forcedHoverForTest: Square? = null

    /** The channel rig - for the gate tests. */
    internal val channelsForTest: SceneChannels? get() = channels

    /** The composed host - the silence gates read its frame-clock instruments. */
    internal val compositionForTest: SceneComposition? get() = composition

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

        // The glow pulse: a raised-cosine dip, GLOW_REST at the top, ~1.2 s period.
        private const val PULSE_DEPTH = 0.35
        private const val PULSE_RADIANS = 2.0 * Math.PI / 1.2
    }
}
