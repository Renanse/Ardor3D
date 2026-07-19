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

import com.ardor3d.compose.allocationAccounting
import com.ardor3d.editor.play.GameInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The allocation gate for continuous motion: a steady-state slide frame - channel write,
 * composition frame, sampler apply, through the real game loop on the thread that drives it -
 * allocates exactly nothing on the Java heap. Not "little": zero bytes, summed over hundreds
 * of measured frames.
 *
 * Two kings shuttle between corners so slides repeat indefinitely without captures (and
 * without ever creating a mandatory jump). Event-rate frames are excluded by design: the
 * update that lands beginMove's state writes and the update that finalizes the move both
 * recompose, and that traffic is priced elsewhere - this gate is the price of *motion*.
 *
 * The shuttle runs as two game sessions, each within the 80-ply draw counter (19 cycles x 4
 * plies = 76): the first exists purely to warm - measured empirically, the JIT's one-time
 * tier-transition artifacts land around cycle 11-12 and are quiet afterwards - and the second
 * re-warms its fresh per-session objects briefly, then measures.
 */
class MotionAllocationGateTest {

    private fun sq(row: Int, col: Int) = Square(row, col)

    @Test
    fun aSteadyStateMotionFrameAllocatesExactlyNothing() {
        val threads = allocationAccounting()
        val self = Thread.currentThread().id

        // One slide; returns the bytes allocated across its strictly-interior frames.
        // Timing: ANIM_DURATION 0.22s at 0.02s frames = the event frame, nine interior
        // frames (elapsed 0.04..0.20), then the finalize frame(s).
        fun slide(game: CheckersGame, from: Square, to: Square): Long {
            game.clickSquareForTest(from)
            game.clickSquareForTest(to)
            check(game.isAnimatingForTest) { "no slide began for $from -> $to" }
            game.update(FRAME, GameInput.EMPTY) // event frame: beginMove's writes land

            val before = threads.getThreadAllocatedBytes(self)
            repeat(INTERIOR_FRAMES) { game.update(FRAME, GameInput.EMPTY) }
            val delta = threads.getThreadAllocatedBytes(self) - before

            check(game.isAnimatingForTest) { "measured frames must all be mid-slide" }
            while (game.isAnimatingForTest) game.update(FRAME, GameInput.EMPTY) // land (event)
            return delta
        }

        fun cycle(game: CheckersGame): Long {
            var bytes = 0L
            bytes += slide(game, sq(0, 0), sq(1, 1))
            bytes += slide(game, sq(7, 7), sq(6, 6))
            bytes += slide(game, sq(1, 1), sq(0, 0))
            bytes += slide(game, sq(6, 6), sq(7, 7))
            return bytes
        }

        fun startSession(): CheckersGame {
            val game = CheckersGame()
            game.initialBoardForTest = Board.of(
                PieceColor.RED,
                mapOf(
                    sq(0, 0) to Piece(PieceColor.RED, king = true),
                    sq(7, 7) to Piece(PieceColor.BLACK, king = true)
                )
            )
            game.onStart(FakeGameContext())
            return game
        }

        // Session one: pure warmup, discarded.
        val warmup = startSession()
        repeat(SESSION_CYCLES) { cycle(warmup) }
        warmup.onStop()

        // Session two: fresh per-session objects settle over a short re-warm, then the gate.
        val game = startSession()
        repeat(REWARM_CYCLES) { cycle(game) }
        var total = 0L
        repeat(MEASURED_CYCLES) { total += cycle(game) }

        assertEquals(
            "a motion frame must allocate exactly nothing " +
                "(${MEASURED_CYCLES * 4 * INTERIOR_FRAMES} frames measured)",
            0L, total
        )
        assertTrue("the shuttle actually played", game.boardForTest.pieceAt(sq(0, 0)) != null)
        game.onStop()
    }

    private companion object {
        const val FRAME = 0.02
        const val INTERIOR_FRAMES = 9
        const val SESSION_CYCLES = 19
        const val REWARM_CYCLES = 4
        const val MEASURED_CYCLES = 6
    }
}
