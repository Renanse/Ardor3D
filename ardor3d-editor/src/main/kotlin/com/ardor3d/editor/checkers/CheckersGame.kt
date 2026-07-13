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

/**
 * A playable game of checkers, driven by the editor's play mode. It is the worked example of the
 * plan's state/view split and the [GameMode] SPI: the pure [Board] is the authoritative state, the
 * [CheckersView] is its scene view, and this class is the only place they meet - it turns clicks
 * into model moves and re-syncs the view.
 *
 * Interaction: click a piece of the side to move to select it (its legal destinations light up),
 * then click a highlighted square to move (a jump plays its whole mandatory chain). Clicking
 * elsewhere reselects or clears. Moves animate with a short slide; input is ignored mid-slide and
 * once the game is over.
 */
class CheckersGame : GameMode {
    private var context: GameContext? = null
    private lateinit var view: CheckersView
    private var board: Board = Board.initial()
    private var selected: Square? = null

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

        val click = input.click ?: return
        val square = squareAt(context, click.x, click.y)
        if (square == null) {
            deselect()
            return
        }
        onSquareClicked(square)
    }

    override fun onStop() {
        context?.setStatus(null)
        context = null
        // view.root is thrown away when the pre-play scene is restored - nothing else to release.
    }

    // --- interaction -------------------------------------------------------------------------

    private fun onSquareClicked(square: Square) {
        val from = selected
        if (from == null) {
            trySelect(square)
            return
        }
        val move = board.legalMovesFrom(from).firstOrNull { it.to == square }
        when {
            move != null -> beginMove(move)
            board.pieceAt(square)?.color == board.toMove -> {
                deselect()
                trySelect(square)
            }
            else -> deselect()
        }
    }

    private fun trySelect(square: Square) {
        val piece = board.pieceAt(square)
        if (piece == null || piece.color != board.toMove) return
        val destinations = board.legalMovesFrom(square).map { it.to }
        if (destinations.isEmpty()) return // a piece with no legal move (e.g. a capture is forced elsewhere)
        selected = square
        view.showHighlights(destinations)
        context?.materialize(view.root) // materialize the new highlight meshes
    }

    private fun deselect() {
        selected = null
        view.clearHighlights()
    }

    private fun beginMove(move: Move) {
        deselect()
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

    /** Drives a click on [square] (bypassing pick) - for interaction tests. */
    internal fun clickSquareForTest(square: Square) = onSquareClicked(square)

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
