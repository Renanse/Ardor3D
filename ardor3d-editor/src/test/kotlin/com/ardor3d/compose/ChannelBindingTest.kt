/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ardor3d.scenegraph.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The binding lifecycle against a real composition: a channel exists exactly as long as its
 * binding is in the composition - entering binds, leaving releases, disposal releases, a
 * failed composition releases through onAbandoned, and a keyed replacement hands over with no
 * aliasing: the new binding allocates before the old releases, and the straggler handle dies
 * loudly. This is the mechanism the scene ownership policy rests on, tested on its own before
 * any board is composed over it.
 */
class ChannelBindingTest {

    /** A minimal composed host: content under a root node, driven frame by frame. */
    private class Harness : AutoCloseable {
        val root = Node("root")
        val channels = SceneChannels(transformCapacity = 4, paramCapacity = 4)
        val scene = SceneComposition(root)
        private var t = 0L
        fun frame() = scene.frame(++t)
        override fun close() = scene.close()
    }

    @Test
    fun enteringTheCompositionBindsAndLeavingReleases() {
        Harness().use { h ->
            var show by mutableStateOf(true)
            var bound: TransformChannel? = null
            h.scene.setContent {
                if (show) {
                    val node = remember { Node("piece") }
                    bound = rememberTransformChannel(h.channels, "piece", node)
                    SceneSpatial(factory = { node })
                }
            }
            assertEquals(1, h.channels.liveTransformCount)
            assertEquals(bound!!.handle, h.channels.transformHandle("piece"))

            show = false
            h.frame()

            assertEquals("leaving the composition releases the slot", 0, h.channels.liveTransformCount)
            assertTrue(!h.channels.hasTransform("piece"))
            assertThrows("the straggler handle is loud", IllegalStateException::class.java) {
                bound!!.writeTranslation(1.0, 1.0, 1.0)
            }
        }
    }

    @Test
    fun writesThroughTheBindingReachTheNodeAtSample() {
        Harness().use { h ->
            var channel: TransformChannel? = null
            var node: Node? = null
            h.scene.setContent {
                val n = remember { Node("piece") }
                node = n
                channel = rememberTransformChannel(h.channels, null, n)
                SceneSpatial(factory = { n })
            }
            h.channels.sample() // settle the seed

            channel!!.writeTranslation(2.0, 0.5, -1.0)
            h.channels.sample()

            assertEquals(2.0, node!!.translation.x, 1e-6)
            assertEquals(-1.0, node!!.translation.z, 1e-6)
        }
    }

    @Test
    fun keyedReplacementHandsOverWithoutAliasing() {
        Harness().use { h ->
            var which by mutableStateOf(1)
            var current: TransformChannel? = null
            h.scene.setContent {
                key(which) {
                    val node = remember { Node("piece_$which") }
                    current = rememberTransformChannel(h.channels, "piece", node)
                    SceneSpatial(factory = { node })
                }
            }
            val old = current!!
            val oldHandle = old.handle

            which = 2
            h.frame()
            val replacement = current!!

            assertNotSameBinding(old, replacement)
            assertEquals("exactly one binding lives on", 1, h.channels.liveTransformCount)
            assertEquals(
                "the registry points at the replacement",
                replacement.handle, h.channels.transformHandle("piece")
            )
            assertNotEquals("no handle aliasing across the replacement", oldHandle, replacement.handle)
            assertThrows("the replaced binding's writer is dead", IllegalStateException::class.java) {
                old.writeTranslation(9.0, 9.0, 9.0)
            }
            // And the survivor still works.
            replacement.writeTranslation(1.0, 2.0, 3.0)
            h.channels.sample()
        }
    }

    private fun assertNotSameBinding(a: TransformChannel, b: TransformChannel) {
        assertTrue("replacement must be a new binding", a !== b)
    }

    @Test
    fun replacementOverlapNeedsCapacityHeadroom() {
        // Pins the allocation ordering the no-aliasing claim rests on: the replacement binds
        // BEFORE the old binding is forgotten, so a block sized exactly to the live count
        // cannot host a keyed replacement - it exhausts, loudly. If this ever starts fitting
        // in capacity 1, the runtime's replacement order changed: re-verify the aliasing
        // analysis before loosening anything.
        val channels = SceneChannels(transformCapacity = 1, paramCapacity = 1)
        SceneComposition(Node("root")).use { scene ->
            var which by mutableStateOf(1)
            scene.setContent {
                key(which) {
                    val node = remember { Node("piece_$which") }
                    rememberTransformChannel(channels, "piece", node)
                    SceneSpatial(factory = { node })
                }
            }
            assertEquals(1, channels.liveTransformCount)

            which = 2
            var t = 0L
            val e = assertThrows(IllegalStateException::class.java) { scene.frame(++t) }
            assertTrue(
                "the failure names the exhaustion, not a downstream symptom",
                generateSequence<Throwable>(e) { it.cause }.any { it.message?.contains("exhausted") == true }
            )
        }
    }

    @Test
    fun aFailedCompositionReleasesItsBindingThroughOnAbandoned() {
        Harness().use { h ->
            assertThrows(RuntimeException::class.java) {
                h.scene.setContent {
                    val node = remember { Node("piece") }
                    rememberTransformChannel(h.channels, "piece", node)
                    error("composition fails after the binding was made")
                }
            }

            assertEquals("onAbandoned released the slot", 0, h.channels.liveTransformCount)
            assertTrue("and the registry entry", !h.channels.hasTransform("piece"))
        }
    }

    @Test
    fun disposalReleasesEveryBinding() {
        val h = Harness()
        var transform: TransformChannel? = null
        var param: ParamChannel? = null
        h.scene.setContent {
            val a = remember { Node("a") }
            val b = remember { Node("b") }
            transform = rememberTransformChannel(h.channels, "a", a)
            rememberTransformChannel(h.channels, "b", b)
            param = rememberParamChannel(h.channels, "glow", 1f, { })
            SceneNode("group") {
                SceneSpatial(factory = { a })
                SceneSpatial(factory = { b })
            }
        }
        assertEquals(2, h.channels.liveTransformCount)
        assertEquals(1, h.channels.liveParamCount)

        h.close()

        assertEquals("disposal releases every transform", 0, h.channels.liveTransformCount)
        assertEquals("and every param", 0, h.channels.liveParamCount)
        assertThrows(IllegalStateException::class.java) { transform!!.writeTranslation(1.0, 1.0, 1.0) }
        assertThrows(IllegalStateException::class.java) { param!!.write(0.5f) }
    }

    @Test
    fun paramBindingDeliversSeedAndWritesThroughItsTarget() {
        Harness().use { h ->
            var received = -1f
            var channel: ParamChannel? = null
            h.scene.setContent {
                channel = rememberParamChannel(h.channels, "glow", 0.5f, { received = it })
            }
            h.channels.sample()
            assertEquals("the seed arrives with the first sample", 0.5f, received, 0f)

            channel!!.write(0.75f)
            h.channels.sample()
            assertEquals(0.75f, received, 0f)
        }
    }

    @Test
    fun bindingsSurviveRecompositionOfTheirOwnPosition() {
        Harness().use { h ->
            var label by mutableStateOf("first")
            var channel: TransformChannel? = null
            var node: Node? = null
            h.scene.setContent {
                val n = remember { Node("piece") }
                node = n
                channel = rememberTransformChannel(h.channels, "piece", n)
                SceneSpatial(factory = { n }, update = { set(label) { setProperty("label", it) } })
            }
            val before = channel!!

            label = "second"
            h.frame()

            assertSame("recomposition must not re-bind", before, channel)
            assertEquals(1, h.channels.liveTransformCount)
            assertNotNull(node!!.getProperty("label", null as String?))
            // The channel value is untouched by the recomposition: the writer still owns it.
            before.writeTranslation(5.0, 0.0, 0.0)
            h.channels.sample()
            assertEquals(5.0, node!!.translation.x, 1e-6)
        }
    }
}
