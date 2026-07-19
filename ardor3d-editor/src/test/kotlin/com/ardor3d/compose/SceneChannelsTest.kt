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

import com.ardor3d.math.Quaternion
import com.ardor3d.math.Vector3
import com.ardor3d.scenegraph.Node
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rig around the channel store: binding seeds the slot from where the target already
 * stands (the binding writer rule, so a recycled slot never leaks its dead tenant into a new
 * node), the sampler applies exactly what was written and nothing else, release makes every
 * later touch loud, the key registry survives same-key replacement, and the whole
 * write-then-sample frame path allocates nothing - measured, not assumed.
 */
class SceneChannelsTest {

    private fun quarterTurnX() = Quaternion().fromAngleAxis(Math.PI / 2, Vector3.UNIT_X)

    @Test
    fun bindingSeedsTheSlotFromTheTargetsCurrentTransform() {
        val channels = SceneChannels()
        val node = Node("n")
        node.setTranslation(1.0, 2.0, 3.0)
        node.setRotation(quarterTurnX())

        val handle = channels.bindTransform("n", node)

        // Perturb the node behind the rig's back; the seeded values must win the next sample.
        node.setTranslation(9.0, 9.0, 9.0)
        node.setRotation(Quaternion.IDENTITY)
        channels.sample()

        assertEquals(1.0, node.translation.x, 1e-6)
        assertEquals(2.0, node.translation.y, 1e-6)
        assertEquals(3.0, node.translation.z, 1e-6)
        // A quarter turn about X carries +Y to +Z.
        val turned = node.rotation.applyPost(Vector3.UNIT_Y, Vector3())
        assertEquals(0.0, turned.x, 1e-6)
        assertEquals(0.0, turned.y, 1e-6)
        assertEquals(1.0, turned.z, 1e-6)
        assertEquals(1, channels.liveTransformCount)
        assertEquals(handle, channels.transformHandle("n"))
    }

    @Test
    fun writesLandOnTheTargetAtSampleAndUnwrittenChannelsKeepTheirSeed() {
        val channels = SceneChannels()
        val node = Node("n")
        node.setRotation(quarterTurnX())
        val handle = channels.bindTransform(null, node)
        channels.sample() // settle the seed

        channels.writeTranslation(handle, 4.0, 5.0, 6.0)
        assertEquals("nothing moves before sample", 0.0, node.translation.x, 0.0)
        channels.sample()

        assertEquals(4.0, node.translation.x, 1e-6)
        assertEquals(5.0, node.translation.y, 1e-6)
        assertEquals(6.0, node.translation.z, 1e-6)
        val turned = node.rotation.applyPost(Vector3.UNIT_Y, Vector3())
        assertEquals("rotation channels keep their seeded value", 1.0, turned.z, 1e-6)
    }

    @Test
    fun sampleAppliesOnlyWhatWasWritten() {
        val channels = SceneChannels()
        val moved = Node("moved")
        val still = Node("still")
        val handle = channels.bindTransform(null, moved)
        channels.bindTransform(null, still)
        channels.sample() // settle both seeds

        // Perturb the unwritten node directly: if the sampler blindly re-applied every live
        // slot, it would stomp this back to the seed.
        still.setTranslation(7.0, 7.0, 7.0)
        channels.writeTranslation(handle, 1.0, 0.0, 0.0)
        channels.sample()

        assertEquals(1.0, moved.translation.x, 1e-6)
        assertEquals("an unwritten slot must not be touched", 7.0, still.translation.x, 0.0)
    }

    @Test
    fun releaseMakesLaterWritesLoudAndUnbindsTheTarget() {
        val channels = SceneChannels()
        val node = Node("n")
        val handle = channels.bindTransform("k", node)
        channels.sample()

        channels.releaseTransform("k", handle)

        assertThrows(IllegalStateException::class.java) { channels.writeTranslation(handle, 1.0, 1.0, 1.0) }
        assertThrows(IllegalStateException::class.java) { channels.transformHandle("k") }
        assertEquals(0, channels.liveTransformCount)
        node.setTranslation(5.0, 5.0, 5.0)
        channels.sample() // nothing dirty, nothing applied
        assertEquals(5.0, node.translation.x, 0.0)
    }

    @Test
    fun aRecycledSlotServesItsNewTenantOnly() {
        val channels = SceneChannels(transformCapacity = 1, paramCapacity = 1)
        val first = Node("first")
        first.setTranslation(1.0, 1.0, 1.0)
        val old = channels.bindTransform(null, first)
        channels.sample()
        channels.releaseTransform(null, old)

        val second = Node("second")
        second.setTranslation(2.0, 2.0, 2.0)
        val recycled = channels.bindTransform(null, second)
        assertNotEquals("same slot, new generation", old, recycled)
        channels.sample()

        assertEquals("the seed is the new tenant's, not the corpse's", 2.0, second.translation.x, 1e-6)
        assertEquals("the old tenant is not the new slot's target", 1.0, first.translation.x, 1e-6)
        assertThrows(IllegalStateException::class.java) { channels.writeTranslation(old, 9.0, 9.0, 9.0) }
    }

    @Test
    fun sameKeyReplacementRegistersBeforeTheOldBindingReleases() {
        // The composition runtime's replacement order: the new binding is created (and
        // registers) before the old one is forgotten. The old release must not evict the
        // replacement's registry entry.
        val channels = SceneChannels()
        val oldNode = Node("old")
        val newNode = Node("new")
        val oldHandle = channels.bindTransform("piece", oldNode)
        val newHandle = channels.bindTransform("piece", newNode)

        channels.releaseTransform("piece", oldHandle)

        assertEquals("the replacement's entry survives", newHandle, channels.transformHandle("piece"))
        assertTrue(channels.hasTransform("piece"))
        assertEquals(1, channels.liveTransformCount)
    }

    @Test
    fun paramsDeliverSeedWritesAndTouchThroughTheirTarget() {
        val channels = SceneChannels()
        var received = -1f
        var deliveries = 0
        val handle = channels.bindParam("glow", 1f, { value -> received = value; deliveries++ })

        channels.sample()
        assertEquals("the seed is delivered by the first sample", 1f, received, 0f)
        assertEquals(1, deliveries)

        channels.sample()
        assertEquals("an idle sample delivers nothing", 1, deliveries)

        channels.writeParam(handle, 0.25f)
        channels.sample()
        assertEquals(0.25f, received, 0f)
        assertEquals(2, deliveries)

        // touch: same value, re-delivered - for when a combined outside input changed.
        channels.touchParam(handle)
        channels.sample()
        assertEquals(0.25f, received, 0f)
        assertEquals(3, deliveries)

        channels.releaseParam("glow", handle)
        assertThrows(IllegalStateException::class.java) { channels.writeParam(handle, 1f) }
        assertThrows(IllegalStateException::class.java) { channels.paramHandle("glow") }
    }

    @Test
    fun theWriteSampleFramePathAllocatesNothing() {
        val channels = SceneChannels()
        val parent = Node("parent") // a parent, so dirty propagation walks up as it will live
        val node = Node("n")
        parent.attachChild(node)
        val transform = channels.bindTransform(null, node)
        var sink = 0f
        val param = channels.bindParam(null, 1f, { sink = it })

        val threads = allocationAccounting()
        val self = Thread.currentThread().id

        var x = 0.0
        repeat(10_000) { // warm until JIT is done allocating
            channels.writeTranslation(transform, x, 0.5, -x)
            channels.writeParam(param, (x % 1.0).toFloat())
            channels.sample()
            x += 0.01
        }

        val before = threads.getThreadAllocatedBytes(self)
        repeat(5_000) {
            channels.writeTranslation(transform, x, 0.5, -x)
            channels.writeParam(param, (x % 1.0).toFloat())
            channels.sample()
            x += 0.01
        }
        val allocated = threads.getThreadAllocatedBytes(self) - before

        assertEquals("write + sample must be allocation-free per frame", 0L, allocated)
        assertTrue(sink >= 0f) // keep the param sink observably live
    }
}
