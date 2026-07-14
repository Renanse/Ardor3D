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

import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scene view of the board, tested headlessly - it is pure scene-graph construction (no GL, no
 * materials): the opening lays out 24 pieces on the right squares, re-syncing tracks a capture,
 * crowning shows on the node metadata, and pickable tiles map back to their squares.
 */
class CheckersViewTest {

    private fun sq(row: Int, col: Int) = Square(row, col)

    private fun tileNamed(view: CheckersView, name: String): Spatial {
        val tiles = view.root.children.first { it.name == "Tiles" } as Node
        return tiles.children.first { it.name == name }
    }

    @Test
    fun openingLaysOutTwentyFourPieces() {
        val view = CheckersView()
        view.rebuild(Board.initial())

        assertEquals(24, view.pieceCount)
        assertNotNull("a red man sits on (0,0)", view.pieceNodeAt(sq(0, 0)))
        assertNotNull("a black man sits on (7,7)", view.pieceNodeAt(sq(7, 7)))
        assertNull("the middle rows are empty", view.pieceNodeAt(sq(3, 1)))

        // The piece node sits at the square's world position.
        val node = view.pieceNodeAt(sq(0, 0))!!
        val expected = view.worldPositionOf(sq(0, 0))
        assertEquals(expected.x, node.translation.x, 1e-9)
        assertEquals(expected.z, node.translation.z, 1e-9)
    }

    @Test
    fun rebuildTracksACapture() {
        val view = CheckersView()
        val board = Board.of(PieceColor.RED, mapOf(sq(2, 2) to Piece(PieceColor.RED), sq(3, 3) to Piece(PieceColor.BLACK)))
        view.rebuild(board)
        assertEquals(2, view.pieceCount)

        val after = board.apply(board.legalMoves().single())
        view.rebuild(after)
        assertEquals(1, view.pieceCount)
        assertNull("captured piece removed from the view", view.pieceNodeAt(sq(3, 3)))
        assertNull("mover no longer on its origin", view.pieceNodeAt(sq(2, 2)))
        assertNotNull("mover shown on its destination", view.pieceNodeAt(sq(4, 4)))
    }

    @Test
    fun pieceMetadataReflectsColorAndKing() {
        val view = CheckersView()
        view.rebuild(Board.of(PieceColor.RED, mapOf(
            sq(0, 0) to Piece(PieceColor.RED),
            sq(7, 7) to Piece(PieceColor.BLACK, king = true)
        )))

        val man = view.pieceNodeAt(sq(0, 0))!!
        assertEquals("RED", man.getProperty(CheckersView.PROP_COLOR, ""))
        assertFalse(man.getProperty(CheckersView.PROP_KING, false))

        val king = view.pieceNodeAt(sq(7, 7))!!
        assertEquals("BLACK", king.getProperty(CheckersView.PROP_COLOR, ""))
        assertTrue("king metadata set", king.getProperty(CheckersView.PROP_KING, false))
    }

    @Test
    fun playableTilesMapBackToTheirSquares() {
        val view = CheckersView()
        // (0,0) is playable (0+0 even); (0,1) is a light square and not a click target.
        assertEquals(sq(0, 0), view.squareForTile(tileNamed(view, "tile_0_0")))
        assertEquals(sq(5, 3), view.squareForTile(tileNamed(view, "tile_5_3")))
        assertNull("light squares are not click targets", view.squareForTile(tileNamed(view, "tile_0_1")))
    }
}
