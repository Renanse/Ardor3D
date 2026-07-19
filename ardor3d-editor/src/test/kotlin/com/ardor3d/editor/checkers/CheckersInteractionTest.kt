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

import com.ardor3d.editor.play.GameInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The hover/click interaction of [CheckersGame], tested headlessly through a fake [GameContext]
 * (hover and clicks are driven by square, bypassing pick; materialization is a no-op). Covers the
 * UX: hovering a movable piece previews its moves; clicking selects it; hovering a destination lights
 * it distinctly; clicking a destination moves and switches turns; clicking elsewhere clears.
 */
class CheckersInteractionTest {

    private fun sq(row: Int, col: Int) = Square(row, col)

    @Test
    fun hoveringAMovablePiecePreviewsItsMovesAndClearsWhenLeaving() {
        val game = CheckersGame().also { it.onStart(FakeGameContext()) }
        assertEquals(0, game.highlightCountForTest)

        // (2,0) is a red front man with one opening move, to (3,1).
        game.hoverSquareForTest(sq(2, 0))
        assertEquals(HighlightKind.HOVER_PIECE, game.highlightForTest(sq(2, 0)))
        assertEquals(HighlightKind.PREVIEW, game.highlightForTest(sq(3, 1)))
        assertEquals(2, game.highlightCountForTest)

        game.hoverSquareForTest(null)
        assertEquals("leaving the piece clears the preview", 0, game.highlightCountForTest)

        // Hovering an empty square previews nothing.
        game.hoverSquareForTest(sq(4, 4))
        assertEquals(0, game.highlightCountForTest)
    }

    @Test
    fun selectHoverDestinationThenMove() {
        val ctx = FakeGameContext()
        val game = CheckersGame().also { it.onStart(ctx) }

        // Click the piece: it stays selected with its destination lit.
        game.clickSquareForTest(sq(2, 0))
        assertEquals(HighlightKind.SELECTED, game.highlightForTest(sq(2, 0)))
        assertEquals(HighlightKind.MOVE, game.highlightForTest(sq(3, 1)))

        // Hover the destination: it lights distinctly.
        game.hoverSquareForTest(sq(3, 1))
        assertEquals(HighlightKind.MOVE_HOVER, game.highlightForTest(sq(3, 1)))

        // Click the destination: the move animates, then model, view, and turn advance.
        game.clickSquareForTest(sq(3, 1))
        assertTrue(game.isAnimatingForTest)
        repeat(4) { game.update(0.1, GameInput.EMPTY) }
        assertFalse(game.isAnimatingForTest)

        assertNull(game.boardForTest.pieceAt(sq(2, 0)))
        assertEquals(Piece(PieceColor.RED), game.boardForTest.pieceAt(sq(3, 1)))
        assertEquals(PieceColor.BLACK, game.boardForTest.toMove)
        assertNotNull(game.pieceNodeForTest(sq(3, 1)))
        assertEquals("highlights cleared after the move", 0, game.highlightCountForTest)
        assertEquals("Black to move", ctx.lastStatus)
    }

    @Test
    fun slideIsImperativeWithZeroRecompositionAndTheNodeSurvivesTheMove() {
        val game = CheckersGame().also { it.onStart(FakeGameContext()) }

        game.clickSquareForTest(sq(2, 0))
        val node = game.pieceNodeForTest(sq(2, 0))
        assertNotNull(node)
        game.clickSquareForTest(sq(3, 1))
        assertTrue(game.isAnimatingForTest)

        // The first update applies the highlight clear that beginMove wrote; count syncs after it.
        game.update(0.05, GameInput.EMPTY)
        val syncsAtSlideStart = game.syncCountForTest
        val xAtStart = node!!.translation.x

        // Mid-slide frames: the piece moves by imperative writes alone - zero recomposition.
        repeat(2) { game.update(0.05, GameInput.EMPTY) }
        assertTrue("still sliding", game.isAnimatingForTest)
        assertTrue("the piece is actually moving", node.translation.x > xAtStart)
        assertEquals("zero recomposition during the slide", syncsAtSlideStart, game.syncCountForTest)

        // Finish: the board applies, and recomposition keeps the very same node - moved, not
        // rebuilt - snapping its transform to the slide's own endpoint rather than stomping it.
        game.update(0.2, GameInput.EMPTY)
        assertFalse(game.isAnimatingForTest)
        assertTrue("the move recomposed the pieces", game.syncCountForTest > syncsAtSlideStart)
        assertSame("the same node instance after the move", node, game.pieceNodeForTest(sq(3, 1)))
        val dest = worldPositionOf(sq(3, 1))
        assertEquals(dest.x, node.translation.x, 1e-9)
        assertEquals(dest.z, node.translation.z, 1e-9)
    }

    @Test
    fun clickingElsewhereDeselects() {
        val game = CheckersGame().also { it.onStart(FakeGameContext()) }

        game.clickSquareForTest(sq(2, 2))
        assertTrue("a piece is selected", game.highlightCountForTest > 0)

        // An empty, non-destination square clears the selection.
        game.clickSquareForTest(sq(3, 5))
        assertEquals(0, game.highlightCountForTest)

        // Clicking off the board (null) also clears.
        game.clickSquareForTest(sq(2, 2))
        assertTrue(game.highlightCountForTest > 0)
        game.clickSquareForTest(null)
        assertEquals(0, game.highlightCountForTest)
    }
}
