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
import com.ardor3d.editor.play.GameInput
import com.ardor3d.math.ColorRGBA
import com.ardor3d.math.type.ReadOnlyColorRGBA
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The param-channel gate: a non-transform value (highlight glow) meets composition under the
 * same ordering discipline as the transform track. The marker's shown color is derived by the
 * sampler from a composition-owned base times a channel-owned intensity, so the adversarial
 * ordering is: recompose the owning marker (a kind change rewrites the base) while a pulse is
 * mid-flight - the binding must survive and the in-flight intensity must not be stomped; the
 * new base simply meets the old intensity. Disposal mid-pulse retires the param slot and kills
 * the writer by name, and the real game's hover pulse breathes through exactly this mechanism.
 */
class GlowChannelGateTest {

    private fun sq(row: Int, col: Int) = Square(row, col)

    private class Harness(initial: Board) : AutoCloseable {
        val root = Node("Checkers")
        val channels = SceneChannels(transformCapacity = 32, paramCapacity = 48)
        var board by mutableStateOf(initial)
        var highlights by mutableStateOf<Map<Square, HighlightKind>>(emptyMap())

        private val composition = SceneComposition(root)
        private var frames = 0L

        init {
            composition.setContent {
                BoardScene(channels, board = { board }, highlights = { highlights })
            }
        }

        fun frame() {
            composition.frame(++frames)
            channels.sample()
        }

        fun marker(): Spatial = (root.getChild("Highlights") as Node).getChild(0)

        override fun close() = composition.close()
    }

    private fun scaled(base: ReadOnlyColorRGBA, intensity: Float) =
        ColorRGBA(base.red * intensity, base.green * intensity, base.blue * intensity, base.alpha)

    @Test
    fun kindRecompositionMidPulseKeepsTheBindingAndTheIntensity() {
        Harness(Board.initial()).use { h ->
            val square = sq(3, 3)
            h.highlights = mapOf(square to HighlightKind.MOVE)
            h.frame()
            val marker = h.marker()
            val handle = h.channels.paramHandle(square)
            val moveBase = ColorRGBA(marker.getDefaultColor()) // intensity is at rest = the base

            // A pulse is mid-flight: intensity 0.7 has landed.
            h.channels.writeParam(handle, 0.7f)
            h.frame()
            assertEquals(scaled(moveBase, 0.7f), ColorRGBA(marker.getDefaultColor()))

            // Adversarial ordering: the owning marker recomposes mid-pulse (its kind changes,
            // rewriting the composition-owned base).
            h.highlights = mapOf(square to HighlightKind.MOVE_HOVER)
            h.frame()

            assertSame("the marker survived its recomposition", marker, h.marker())
            assertEquals("the binding survived", handle, h.channels.paramHandle(square))
            assertEquals("still exactly one param bound", 1, h.channels.liveParamCount)
            val shown = ColorRGBA(marker.getDefaultColor())
            assertNotEquals("the base did change", scaled(moveBase, 0.7f), shown)
            // Pin the unstomped intensity exactly: raising it to rest reveals the pure new
            // base, and the mid-pulse color must have been that base times the old 0.7.
            h.channels.writeParam(handle, GLOW_REST)
            h.frame()
            val hoverBase = ColorRGBA(marker.getDefaultColor())
            assertEquals(scaled(hoverBase, 0.7f), shown)

            // The writer is still live through it all.
            h.channels.writeParam(handle, 0.5f)
            h.frame()
            assertEquals(scaled(hoverBase, 0.5f), ColorRGBA(marker.getDefaultColor()))
        }
    }

    @Test
    fun markerDisposalMidPulseRetiresTheParamAndKillsTheWriter() {
        Harness(Board.initial()).use { h ->
            val square = sq(3, 3)
            h.highlights = mapOf(square to HighlightKind.MOVE)
            h.frame()
            val handle = h.channels.paramHandle(square)
            h.channels.writeParam(handle, 0.8f) // mid-pulse
            h.frame()
            assertEquals(1, h.channels.liveParamCount)

            h.highlights = emptyMap() // the marker leaves the composition mid-pulse
            h.frame()

            assertEquals("the param slot retired exactly once", 0, h.channels.liveParamCount)
            assertTrue(!h.channels.hasParam(square))
            val e = assertThrows(IllegalStateException::class.java) {
                h.channels.writeParam(handle, 1f)
            }
            assertTrue("the writer died by name: ${e.message}", e.message!!.contains("stale"))
        }
    }

    @Test
    fun theDestinationUnderThePointerBreathesAndComesToRestWhenLeft() {
        val game = CheckersGame()
        game.onStart(FakeGameContext())
        val context = game.channelsForTest!!

        // Select the opening piece and park the pointer on its destination.
        game.clickSquareForTest(sq(2, 0))
        game.forcedHoverForTest = sq(3, 1)
        game.update(0.1, GameInput.EMPTY)
        assertEquals(HighlightKind.MOVE_HOVER, game.highlightForTest(sq(3, 1)))
        assertTrue("the pulse bound its marker", context.hasParam(sq(3, 1)))

        // The marker breathes: its shown color changes from frame to frame.
        val marker = markerAt(game, sq(3, 1))
        val first = ColorRGBA(marker.getDefaultColor())
        game.update(0.1, GameInput.EMPTY)
        val second = ColorRGBA(marker.getDefaultColor())
        game.update(0.1, GameInput.EMPTY)
        val third = ColorRGBA(marker.getDefaultColor())
        assertNotEquals("the glow is alive", first, second)
        assertNotEquals(second, third)
        assertTrue("the pulse dims, never brightens past the base", third.red <= first.red + 1e-6f)

        // Leaving the destination returns the marker to rest and stops the pulse.
        game.forcedHoverForTest = null
        game.update(0.1, GameInput.EMPTY)
        assertEquals(
            "the marker downgraded to a plain destination",
            HighlightKind.MOVE, game.highlightForTest(sq(3, 1))
        )
        val atRest = ColorRGBA(markerAt(game, sq(3, 1)).getDefaultColor())
        game.update(0.1, GameInput.EMPTY)
        game.update(0.1, GameInput.EMPTY)
        assertEquals("and it stays at rest", atRest, ColorRGBA(markerAt(game, sq(3, 1)).getDefaultColor()))
        game.onStop()
    }

    /** The highlight marker shown on [square], found by its placement. */
    private fun markerAt(game: CheckersGame, square: Square): Spatial {
        val root = game.pieceNodeForTest(sq(2, 0))?.parent?.parent
            ?: game.pieceNodeForTest(sq(3, 1))?.parent?.parent
            ?: error("no board root reachable")
        val markers = (root as Node).getChild("Highlights") as Node
        return markers.children.first {
            Math.abs(it.translation.x - (square.col - 3.5)) < 1e-6 &&
                Math.abs(it.translation.z - (square.row - 3.5)) < 1e-6
        }
    }
}
