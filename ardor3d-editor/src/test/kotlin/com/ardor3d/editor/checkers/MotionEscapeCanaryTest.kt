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

import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.ardor3d.compose.CountingApplier
import com.ardor3d.compose.SceneChannels
import com.ardor3d.compose.SceneComposition
import com.ardor3d.compose.SpatialApplier
import com.ardor3d.compose.rememberTransformChannel
import com.ardor3d.scenegraph.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The instrument canary for [SlideSilenceGateTest]: motion wired the forbidden way - a
 * continuous value read by composition and re-fed every frame, the animateFloatAsState shape -
 * must turn every silence instrument red. The piece even has a proper channel binding; the
 * writer just ignores it and pushes the value through snapshot state instead.
 *
 * This test is permanent. It proves the gate's green means "the instruments watched and saw
 * nothing," not "the instruments cannot see" - delete it and the silence gate becomes theater.
 */
class MotionEscapeCanaryTest {

    @Test
    fun perFrameSnapshotMotionTurnsEveryInstrumentRed() {
        val channels = SceneChannels(transformCapacity = 4, paramCapacity = 4)
        val root = Node("root")
        val applier = CountingApplier(root)
        SceneComposition(applier).use { scene ->
            var t by mutableStateOf(0.0)
            var syncs = 0
            scene.setContent {
                val node = remember { Node("piece") }
                rememberTransformChannel(channels, "piece", node) // bound - and bypassed
                ComposeNode<Node, SpatialApplier>(
                    factory = { node },
                    update = {
                        // Forbidden: the continuous value flows through composition.
                        set(t) { setTranslation(it, 0.11, 0.0) }
                    }
                )
                SideEffect { syncs++ }
            }
            val node = root.getChild(0) as Node
            var clock = 0L
            scene.frame(++clock) // settle the initial composition
            val syncsBefore = syncs
            val framesSentBefore = scene.framesSentForTest
            applier.resetCounts()

            val frames = 5
            repeat(frames) { i ->
                t = (i + 1) * 0.1
                // The write becomes an invalidation and the runtime queues for the next frame:
                // the clock has an awaiter before the frame is even sent.
                Snapshot.sendApplyNotifications()
                assertTrue("the frame clock gained an awaiter", scene.hasFrameAwaitersForTest)
                scene.frame(++clock)
            }

            assertEquals("one recomposition per motion frame", syncsBefore + frames, syncs)
            assertEquals("every frame was sent through the clock", framesSentBefore + frames, scene.framesSentForTest)
            assertTrue("the applier was dragged into the loop", applier.operations > 0)
            assertEquals("the forbidden wiring does move the piece", 0.5, node.translation.x, 1e-9)

            // And the runtime observes the continuous value - both the write and the read.
            var reads = 0
            var writes = 0
            Snapshot.observe(readObserver = { reads++ }, writeObserver = { writes++ }) {
                t = 0.6
                scene.frame(++clock)
            }
            assertTrue("the runtime observed snapshot writes", writes > 0)
            assertTrue("the runtime observed snapshot reads", reads > 0)
        }
    }
}
