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

import androidx.compose.runtime.snapshots.Snapshot
import com.ardor3d.compose.CountingApplier
import com.ardor3d.editor.play.GameInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The silence gate for continuous motion: while a piece slides, the composition runtime must
 * not merely do little - it must be provably out of the loop, on every instrument at once:
 *
 *  - zero recompositions (the leaf-sync counter),
 *  - zero applier traffic, structural *and* navigational,
 *  - the frame clock gains no awaiters and [com.ardor3d.compose.SceneComposition.frame] sends
 *    no frame - a global assertion over the host, not a per-subtree count,
 *  - and a whole mid-slide update observed under [Snapshot.observe] records zero snapshot
 *    reads and zero writes: the runtime never so much as *sees* a continuous value.
 *
 * The companion canary test drives the same scene with motion deliberately wired through
 * snapshot state and asserts every one of these instruments fires - so a green here means the
 * instruments watched and saw nothing, not that they cannot see.
 */
class SlideSilenceGateTest {

    private fun sq(row: Int, col: Int) = Square(row, col)

    @Test
    fun theSlideIsInvisibleToTheCompositionRuntime() {
        lateinit var applier: CountingApplier
        val game = CheckersGame(applierFactory = { CountingApplier(it).also { a -> applier = a } })
        game.onStart(FakeGameContext())

        // Select and move: (2,0) has the single opening move to (3,1).
        game.clickSquareForTest(sq(2, 0))
        game.clickSquareForTest(sq(3, 1))
        assertTrue(game.isAnimatingForTest)

        // The first update applies the event-rate state beginMove wrote (the highlight clear);
        // the slide's silence is gated strictly after it.
        game.update(0.05, GameInput.EMPTY)
        assertTrue(game.isAnimatingForTest)
        val composition = game.compositionForTest!!
        val syncsAtSlideStart = game.syncCountForTest
        val framesSentAtSlideStart = composition.framesSentForTest
        applier.resetCounts()

        // Mid-slide frames. After every one: piece moving, nothing composed.
        repeat(2) {
            game.update(0.05, GameInput.EMPTY)
            assertTrue("still sliding", game.isAnimatingForTest)
            assertFalse("no awaiters on the frame clock", composition.hasFrameAwaitersForTest)
        }
        assertEquals("zero recompositions during the slide", syncsAtSlideStart, game.syncCountForTest)
        assertEquals("zero applier operations (structural and navigation)", 0, applier.operations)
        assertEquals("frame() sent no frame", framesSentAtSlideStart, composition.framesSentForTest)

        // One whole mid-slide update under snapshot observation: the runtime never observes a
        // continuous value - not as a read, not as a write.
        var reads = 0
        var writes = 0
        Snapshot.observe(readObserver = { reads++ }, writeObserver = { writes++ }) {
            game.update(0.05, GameInput.EMPTY)
        }
        assertTrue("the observed frame was still mid-slide", game.isAnimatingForTest)
        assertEquals("zero snapshot reads in a motion frame", 0, reads)
        assertEquals("zero snapshot writes in a motion frame", 0, writes)

        // And the gate must not have starved the landing: finish the slide, the move applies.
        while (game.isAnimatingForTest) game.update(0.05, GameInput.EMPTY)
        assertTrue("the landing recomposed the board", game.syncCountForTest > syncsAtSlideStart)
        assertEquals(Piece(PieceColor.RED), game.boardForTest.pieceAt(sq(3, 1)))
        game.onStop()
    }
}
