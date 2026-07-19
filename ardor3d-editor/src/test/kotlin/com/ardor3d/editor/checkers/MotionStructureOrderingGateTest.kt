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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ardor3d.compose.CountingApplier
import com.ardor3d.compose.SceneChannels
import com.ardor3d.compose.SceneComposition
import com.ardor3d.compose.SceneSpatial
import com.ardor3d.compose.TransformChannel
import com.ardor3d.compose.rememberTransformChannel
import com.ardor3d.scenegraph.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ordering gate: structure and motion must not stomp each other, proven under adversarial
 * interleavings the real game never produces on purpose. A slide is in flight - its writer
 * holding a channel handle, values landing every frame - while recomposition restructures
 * around it, over it, and out from under it:
 *
 *  1. an unrelated subtree recomposes: nothing of the mover is touched;
 *  2. the mover's *own* subtree recomposes: the binding survives and the in-flight value is
 *     not snapped to the model square (there is no snap left to fire);
 *  3. the mover is disposed mid-slide (a capture): its slot retires exactly once and every
 *     later write through the straggler handle throws by name;
 *  4. kinging at landing: a composition-owned change (the crown) lands adjacent to the
 *     channel-owned transform, cleanly;
 *  5. keyed replacement mid-slide: the old binding is forgotten and the new one remembered
 *     within one recomposition, with no index aliasing - driven through the scene idiom
 *     directly, because board piece ids are positional and stable by design: the real game
 *     structurally cannot re-key a live piece, and this gate exists to hold the *mechanism*
 *     to a rule the game never tests;
 *  6. the whole composition is disposed with the writer live: everything retires exactly
 *     once, the writer dies loudly;
 *  7. composition fails *after* a binding was made in that same composition: onAbandoned
 *     releases it - no leak.
 */
class MotionStructureOrderingGateTest {

    private fun sq(row: Int, col: Int) = Square(row, col)

    /** Two-piece board: RED man at (2,2) is id 0, BLACK man at (5,5) is id 1 (board order). */
    private fun twoPieceBoard() = Board.of(
        PieceColor.RED,
        mapOf(sq(2, 2) to Piece(PieceColor.RED), sq(5, 5) to Piece(PieceColor.BLACK))
    )

    private class Harness(initial: Board) : AutoCloseable {
        val root = Node("Checkers")
        val applier = CountingApplier(root)
        val channels = SceneChannels(transformCapacity = 32, paramCapacity = 48)
        var board by mutableStateOf(initial)
        var highlights by mutableStateOf<Map<Square, HighlightKind>>(emptyMap())

        var syncs = 0
            private set

        val composition = SceneComposition(applier)
        private var frames = 0L

        init {
            composition.setContent {
                BoardScene(channels, board = { board }, highlights = { highlights }, onSync = { syncs++ })
            }
        }

        fun frame() {
            composition.frame(++frames)
            channels.sample()
        }

        override fun close() = composition.close()
    }

    @Test
    fun unrelatedSubtreeRecompositionTouchesNothingOfTheMover() {
        Harness(twoPieceBoard()).use { h ->
            val mover = pieceNodeAt(h.root, sq(2, 2))!!
            val handle = h.channels.transformHandle(0)

            // Slide in flight: a mid-slide value has landed.
            h.channels.writeTranslation(handle, -1.25, 0.11, -1.25)
            h.frame()
            val syncsBefore = h.syncs
            h.applier.resetCounts()

            // An unrelated subtree recomposes: a highlight marker appears.
            h.highlights = mapOf(sq(4, 0) to HighlightKind.MOVE)
            h.frame()

            assertEquals("only the highlights subtree recomposed", syncsBefore + 1, h.syncs)
            // One insertion arrives through both insert hooks (top-down and bottom-up), so a
            // single new marker is exactly two structural ops - anything more touched the mover.
            assertEquals("structural traffic is the marker alone", 2, h.applier.structuralOps)
            assertSame("the mover keeps its node", mover, pieceNodeAt(h.root, sq(2, 2)))
            assertEquals("the in-flight value is untouched", -1.25, mover.translation.x, 1e-6)
            val channelValue = h.channels.transformsForTest.get(0, handle).toDouble()
            assertEquals("the channel still holds the written value", -1.25, channelValue, 1e-6)

            // And the writer is still live.
            h.channels.writeTranslation(handle, -1.0, 0.11, -1.0)
            h.frame()
            assertEquals(-1.0, mover.translation.x, 1e-6)
        }
    }

    @Test
    fun ownSubtreeRecompositionMidSlideKeepsTheBindingAndTheValue() {
        Harness(twoPieceBoard()).use { h ->
            val mover = pieceNodeAt(h.root, sq(2, 2))!!
            val handle = h.channels.transformHandle(0)
            val midSlide = worldPositionOf(sq(2, 2)).lerpLocal(worldPositionOf(sq(3, 3)), 0.4)

            h.channels.writeTranslation(handle, midSlide.x, midSlide.y, midSlide.z)
            h.frame()

            // Adversarial ordering: the model applies the move while the slide is mid-flight,
            // recomposing the mover's own subtree (its square-keyed metadata changes).
            h.board = h.board.apply(h.board.legalMovesFrom(sq(2, 2)).first { it.to == sq(3, 3) })
            h.frame()

            assertSame("the node survived its own recomposition", mover, pieceNodeAt(h.root, sq(3, 3)))
            assertEquals("the binding survived", handle, h.channels.transformHandle(0))
            assertEquals(
                "the in-flight value was not snapped to the destination",
                midSlide.x, mover.translation.x, 1e-6
            )

            // The slide lands afterwards, through the same binding.
            val end = worldPositionOf(sq(3, 3))
            h.channels.writeTranslation(handle, end.x, end.y, end.z)
            h.frame()
            assertEquals(end.x, mover.translation.x, 1e-6)
            assertEquals(end.z, mover.translation.z, 1e-6)
        }
    }

    @Test
    fun captureDisposingTheMoverMidSlideRetiresOnceAndLaterWritesFailLoudly() {
        Harness(twoPieceBoard()).use { h ->
            val mover = pieceNodeAt(h.root, sq(5, 5))!!
            val handle = h.channels.transformHandle(1)
            assertEquals(2, h.channels.liveTransformCount)

            // Slide in flight for the black piece...
            h.channels.writeTranslation(handle, 1.2, 0.11, 1.2)
            h.frame()

            // ...when a capture removes it from the board. Its subtree leaves the composition.
            h.board = Board.of(PieceColor.RED, mapOf(sq(2, 2) to Piece(PieceColor.RED)))
            h.frame()

            assertEquals("the slot retired exactly once", 1, h.channels.liveTransformCount)
            assertNull("the node left the scene", mover.parent)
            assertTrue("its registry entry is gone", !h.channels.hasTransform(1))

            val first = assertThrows(IllegalStateException::class.java) {
                h.channels.writeTranslation(handle, 9.0, 9.0, 9.0)
            }
            assertTrue("the failure names staleness: ${first.message}", first.message!!.contains("stale"))
            // Exactly once: the second write dies the same way, not by corrupting a free list.
            assertThrows(IllegalStateException::class.java) {
                h.channels.writeTranslation(handle, 9.0, 9.0, 9.0)
            }
            assertEquals(1, h.channels.liveTransformCount)
        }
    }

    @Test
    fun kingingAtLandingAppliesTheCrownWithoutTouchingTheLandedTransform() {
        // RED man one step from crowning; the far BLACK man keeps the game legal.
        val board = Board.of(
            PieceColor.RED,
            mapOf(sq(6, 2) to Piece(PieceColor.RED), sq(0, 0) to Piece(PieceColor.BLACK))
        )
        Harness(board).use { h ->
            val moverId = h.board.idAt(sq(6, 2))
            val mover = pieceNodeAt(h.root, sq(6, 2))!!
            val handle = h.channels.transformHandle(moverId)
            assertEquals("a man is just its disk", 1, mover.numberOfChildren)

            // The slide runs to its endpoint...
            val end = worldPositionOf(sq(7, 3))
            h.channels.writeTranslation(handle, end.x, end.y, end.z)
            h.frame()

            // ...and the landing applies the model: the crown is composition-owned structure,
            // adjacent to the channel-owned transform.
            h.board = h.board.apply(h.board.legalMovesFrom(sq(6, 2)).first { it.to == sq(7, 3) })
            h.frame()

            assertSame("crowning keeps the node", mover, pieceNodeAt(h.root, sq(7, 3)))
            assertEquals("and adds the crown", 2, mover.numberOfChildren)
            assertEquals(true, mover.getProperty(PROP_KING, false))
            assertEquals("the landed transform is exactly the slide's endpoint", end.x, mover.translation.x, 1e-6)
            assertEquals(end.z, mover.translation.z, 1e-6)
            assertEquals("the binding survived the crowning", handle, h.channels.transformHandle(moverId))
        }
    }

    @Test
    fun keyedReplacementMidSlideHandsOverInOneRecompositionWithoutAliasing() {
        // Driven through the scene idiom directly (see the class doc for why the board can't
        // express this): a BoardPiece-shaped composable under an explicit key, replaced while
        // its writer is live.
        val channels = SceneChannels(transformCapacity = 4, paramCapacity = 4)
        val root = Node("root")
        SceneComposition(root).use { scene ->
            var which by mutableStateOf(1)
            var current: TransformChannel? = null
            var currentNode: Node? = null
            scene.setContent {
                key(which) {
                    val node = remember {
                        Node("piece_$which").apply { setTranslation(1.0 * which, 0.0, 0.0) }
                    }
                    currentNode = node
                    current = rememberTransformChannel(channels, "piece", node)
                    SceneSpatial(factory = { node })
                }
            }
            val old = current!!
            val oldNode = currentNode!!
            var t = 0L

            // Writer live: a mid-slide value is in the old binding's slot.
            old.writeTranslation(0.4, 0.0, 0.0)
            scene.frame(++t)
            channels.sample()
            assertEquals(0.4, oldNode.translation.x, 1e-6)

            which = 2 // the replacement: forgotten and remembered within this one frame
            scene.frame(++t)
            channels.sample()
            val replacement = current!!
            val newNode = currentNode!!

            assertTrue("a new binding", replacement !== old)
            assertNotEquals("a new handle - no aliasing", old.handle, replacement.handle)
            assertEquals("exactly one live binding", 1, channels.liveTransformCount)
            assertEquals("the registry follows the replacement", replacement.handle, channels.transformHandle("piece"))
            assertThrows("the straggler writer is dead", IllegalStateException::class.java) {
                old.writeTranslation(0.6, 0.0, 0.0)
            }
            assertEquals(
                "the new tenant's seed is its own spawn, not the old slide's leftovers",
                2.0, newNode.translation.x, 1e-6
            )

            // And the new binding animates its own node.
            replacement.writeTranslation(2.5, 0.0, 0.0)
            scene.frame(++t)
            channels.sample()
            assertEquals(2.5, newNode.translation.x, 1e-6)
            assertEquals("the old node is not written through the new slot", 0.4, oldNode.translation.x, 1e-6)
        }
    }

    @Test
    fun disposingTheCompositionWithALiveWriterRetiresEverythingOnceAndKillsTheWriter() {
        val h = Harness(twoPieceBoard())
        try {
            h.highlights = mapOf(sq(3, 3) to HighlightKind.MOVE)
            h.frame()
            val handle = h.channels.transformHandle(0)
            h.channels.writeTranslation(handle, -1.25, 0.11, -1.25) // slide in flight
            assertEquals(2, h.channels.liveTransformCount)
            assertEquals(1, h.channels.liveParamCount)

            h.close() // the onStop-mid-slide shape

            assertEquals("every transform retired", 0, h.channels.liveTransformCount)
            assertEquals("every param retired", 0, h.channels.liveParamCount)
            val e = assertThrows(IllegalStateException::class.java) {
                h.channels.writeTranslation(handle, 9.0, 9.0, 9.0)
            }
            assertTrue("the writer died by name: ${e.message}", e.message!!.contains("stale"))
        } finally {
            h.close() // idempotent - cleans up if an assertion failed before the act above
        }
    }

    @Test
    fun compositionFailureAfterANewBindingReleasesItThroughOnAbandoned() {
        val channels = SceneChannels(transformCapacity = 32, paramCapacity = 48)
        val root = Node("Checkers")
        val composition = SceneComposition(root)
        try {
            var board by mutableStateOf(twoPieceBoard())
            var explode by mutableStateOf(false)
            composition.setContent {
                BoardScene(channels, board = { board }, highlights = { emptyMap() })
                if (explode) error("recomposition fails after the new piece bound")
            }
            val before = channels.liveTransformCount
            assertEquals(2, before)
            val pieces = root.getChild("Pieces") as Node

            // One frame both adds a piece (a new binding is created during recomposition) and
            // fails the composition: the new binding must be released through onAbandoned.
            board = Board.of(
                PieceColor.RED,
                mapOf(
                    sq(2, 2) to Piece(PieceColor.RED),
                    sq(5, 5) to Piece(PieceColor.BLACK),
                    sq(1, 1) to Piece(PieceColor.RED)
                )
            )
            explode = true
            assertThrows(IllegalStateException::class.java) { composition.frame(1L) }

            assertEquals("the abandoned binding leaked nothing", before, channels.liveTransformCount)
            assertEquals("no structural change from the failed frame", 2, pieces.numberOfChildren)
        } finally {
            composition.close()
        }
        // Disposal after the failure still releases the survivors exactly once.
        assertEquals(0, channels.liveTransformCount)
    }
}
