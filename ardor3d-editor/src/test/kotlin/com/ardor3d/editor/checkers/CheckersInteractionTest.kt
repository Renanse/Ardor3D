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
import com.ardor3d.intersection.PrimitivePickResults
import com.ardor3d.renderer.Camera
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The click-to-move glue of [CheckersGame], tested headlessly through a fake [GameContext] (clicks
 * are driven by square, bypassing pick; materialization is a no-op). Verifies the full select ->
 * highlight -> move -> animate -> finalize -> sync -> turn-switch path over the real model and view.
 */
class CheckersInteractionTest {

    private class FakeGameContext : GameContext {
        override val sceneRoot: Node = Node("scene")
        override val camera: Camera = Camera(1, 1)
        var lastStatus: String? = null
        override fun pick(x: Int, y: Int): PrimitivePickResults = PrimitivePickResults()
        override fun setStatus(text: String?) { lastStatus = text }
        override fun materialize(spatial: Spatial) { /* no material system in this test */ }
    }

    private fun sq(row: Int, col: Int) = Square(row, col)

    private fun highlightCount(game: CheckersGame): Int =
        (game.viewForTest.root.children.first { it.name == "Highlights" } as Node).numberOfChildren

    @Test
    fun selectingHighlightsThenMovingAdvancesModelViewAndTurn() {
        val ctx = FakeGameContext()
        val game = CheckersGame()
        game.onStart(ctx)

        assertEquals("board attached to the scene", 1, ctx.sceneRoot.numberOfChildren)
        assertEquals(24, game.viewForTest.pieceCount)
        assertEquals("Red to move", ctx.lastStatus)

        // Select the front man on (2,0): its one legal opening move to (3,1) lights up.
        game.clickSquareForTest(sq(2, 0))
        assertEquals("one legal destination highlighted", 1, highlightCount(game))

        // Click the destination: the move begins animating (input ignored until it finishes).
        game.clickSquareForTest(sq(3, 1))
        assertTrue("move is animating", game.isAnimatingForTest)
        assertEquals("highlights cleared once the move starts", 0, highlightCount(game))

        // Drive the slide to completion (0.22s at 0.1s/frame -> 3 frames).
        repeat(4) { game.update(0.1, GameInput.EMPTY) }
        assertFalse("animation finished", game.isAnimatingForTest)

        val board = game.boardForTest
        assertNull("piece left its origin", board.pieceAt(sq(2, 0)))
        assertEquals("piece reached its destination", Piece(PieceColor.RED), board.pieceAt(sq(3, 1)))
        assertEquals("turn passed to Black", PieceColor.BLACK, board.toMove)

        assertNull("view: origin now empty", game.viewForTest.pieceNodeAt(sq(2, 0)))
        assertNotNull("view: piece shown on destination", game.viewForTest.pieceNodeAt(sq(3, 1)))
        assertEquals("status updated to Black", "Black to move", ctx.lastStatus)
    }

    @Test
    fun clickingEmptyElsewhereDeselects() {
        val ctx = FakeGameContext()
        val game = CheckersGame()
        game.onStart(ctx)

        game.clickSquareForTest(sq(2, 0))
        assertEquals(1, highlightCount(game))

        // An empty square that is not a legal destination clears the selection.
        game.clickSquareForTest(sq(3, 5))
        assertEquals("selection cleared", 0, highlightCount(game))
        assertFalse("no move started", game.isAnimatingForTest)
    }
}
