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

import com.ardor3d.editor.checkers.PieceColor.BLACK
import com.ardor3d.editor.checkers.PieceColor.RED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The checkers rules engine, tested in isolation (no scene, no GL): opening setup, mandatory
 * capture, maximal jump chains, crowning (including that it ends a jump), king movement, and
 * win/draw detection.
 */
class BoardTest {

    private fun sq(row: Int, col: Int) = Square(row, col)

    @Test
    fun openingPositionHasTwelveEachAndRedToMove() {
        val board = Board.initial()
        assertEquals(12, board.count(RED))
        assertEquals(12, board.count(BLACK))
        assertEquals(RED, board.toMove)
        assertEquals(Piece(RED), board.pieceAt(sq(0, 0)))
        assertEquals(Piece(BLACK), board.pieceAt(sq(7, 7)))
        assertNull("middle rows are empty", board.pieceAt(sq(3, 1)))
        // Only the four front men (row 2) have room; standard opening = 7 moves.
        assertEquals(7, board.legalMoves().size)
        assertTrue("no captures at the opening", board.legalMoves().none { it.isCapture })
    }

    @Test
    fun captureIsMandatory() {
        // A jump exists for the (2,2) man; a quiet man at (0,0) also could slide, but must not.
        val board = Board.of(RED, mapOf(
            sq(2, 2) to Piece(RED),
            sq(0, 0) to Piece(RED),
            sq(3, 3) to Piece(BLACK)
        ))
        val moves = board.legalMoves()
        assertEquals("only the capture is legal", 1, moves.size)
        val jump = moves.single()
        assertTrue(jump.isCapture)
        assertEquals(sq(2, 2), jump.from)
        assertEquals(sq(4, 4), jump.to)
        assertEquals(listOf(sq(3, 3)), jump.captured)
    }

    @Test
    fun mustPlayTheWholeJumpChain() {
        // (2,2) can jump (3,3)->(4,4) then (5,3)->(6,2): a single maximal 2-capture chain.
        val board = Board.of(RED, mapOf(
            sq(2, 2) to Piece(RED),
            sq(3, 3) to Piece(BLACK),
            sq(5, 3) to Piece(BLACK)
        ))
        val moves = board.legalMoves()
        assertEquals(1, moves.size)
        val chain = moves.single()
        assertEquals(listOf(sq(4, 4), sq(6, 2)), chain.path)
        assertEquals(setOf(sq(3, 3), sq(5, 3)), chain.captured.toSet())
        assertEquals(sq(6, 2), chain.to)
    }

    @Test
    fun crowningEndsAJumpChain() {
        // (5,1) jumps (6,2)->(7,3): lands on the king row and is crowned, so it may NOT continue on
        // to capture (6,4) even though a king there could.
        val board = Board.of(RED, mapOf(
            sq(5, 1) to Piece(RED),
            sq(6, 2) to Piece(BLACK),
            sq(6, 4) to Piece(BLACK)
        ))
        val moves = board.legalMoves()
        assertEquals(1, moves.size)
        val jump = moves.single()
        assertEquals(sq(7, 3), jump.to)
        assertEquals("chain stops at crowning", listOf(sq(6, 2)), jump.captured)
    }

    @Test
    fun manIsCrownedOnReachingTheFarRow() {
        val board = Board.of(RED, mapOf(sq(6, 2) to Piece(RED), sq(0, 0) to Piece(BLACK)))
        val step = board.legalMovesFrom(sq(6, 2)).first { it.to == sq(7, 3) }
        val after = board.apply(step)
        assertEquals("man becomes a king on the far row", Piece(RED, king = true), after.pieceAt(sq(7, 3)))
        assertEquals("turn passes to the opponent", BLACK, after.toMove)
    }

    @Test
    fun kingMovesAndCapturesBackward() {
        val king = Piece(RED, king = true)
        val board = Board.of(RED, mapOf(sq(4, 4) to king, sq(0, 0) to Piece(BLACK)))
        // All four diagonals are open on-board squares.
        assertEquals(4, board.legalMovesFrom(sq(4, 4)).size)

        // A king can jump a piece diagonally behind it.
        val capBoard = Board.of(RED, mapOf(sq(4, 4) to king, sq(3, 3) to Piece(BLACK)))
        val backJump = capBoard.legalMoves().single()
        assertEquals(sq(2, 2), backJump.to)
        assertEquals(listOf(sq(3, 3)), backJump.captured)
    }

    @Test
    fun applyRemovesCapturedAndSwitchesTurn() {
        val board = Board.of(RED, mapOf(sq(2, 2) to Piece(RED), sq(3, 3) to Piece(BLACK)))
        val after = board.apply(board.legalMoves().single())
        assertNull("captured piece removed", after.pieceAt(sq(3, 3)))
        assertNull("origin vacated", after.pieceAt(sq(2, 2)))
        assertEquals(Piece(RED), after.pieceAt(sq(4, 4)))
        assertEquals(0, after.count(BLACK))
        assertEquals(BLACK, after.toMove)
    }

    @Test
    fun sideWithNoPiecesHasLost() {
        val board = Board.of(BLACK, mapOf(sq(0, 0) to Piece(RED)))
        assertTrue(board.legalMoves().isEmpty())
        assertEquals(RED, board.winner())
        assertTrue(board.isGameOver())
        assertFalse(board.isDraw())
    }

    @Test
    fun blockedSideHasLost() {
        // BLACK man in the corner, RED wall so it can neither slide nor jump (the jump landing is off-board).
        val board = Board.of(BLACK, mapOf(
            sq(7, 7) to Piece(BLACK),
            sq(6, 6) to Piece(RED),
            sq(5, 5) to Piece(RED)
        ))
        assertTrue("no slide and the jump would land off-board", board.legalMoves().isEmpty())
        assertEquals(RED, board.winner())
    }

    @Test
    fun noProgressReachesADraw() {
        val kings = mapOf(sq(0, 0) to Piece(RED, king = true), sq(7, 7) to Piece(BLACK, king = true))
        assertFalse(Board.of(RED, kings, Board.DRAW_PLIES - 1).isDraw())
        val drawn = Board.of(RED, kings, Board.DRAW_PLIES)
        assertTrue(drawn.legalMoves().isNotEmpty())
        assertTrue(drawn.isDraw())
        assertNull(drawn.winner())
        assertTrue(drawn.isGameOver())
    }

    @Test
    fun drawClockResetsOnProgressAndTicksOnQuietKingMoves() {
        val kings = mapOf(sq(4, 4) to Piece(RED, king = true), sq(0, 0) to Piece(BLACK, king = true))
        val quiet = Board.of(RED, kings, 5)
        val afterQuiet = quiet.apply(quiet.legalMovesFrom(sq(4, 4)).first { it.to == sq(5, 5) })
        assertEquals("quiet king move advances the draw clock", 6, afterQuiet.pliesSinceProgress)

        val manMove = Board.of(RED, mapOf(sq(2, 2) to Piece(RED), sq(0, 0) to Piece(BLACK)), 30)
        val afterMan = manMove.apply(manMove.legalMovesFrom(sq(2, 2)).first())
        assertEquals("a man move resets the draw clock", 0, afterMan.pliesSinceProgress)
    }
}
