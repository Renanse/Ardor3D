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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The channel store on its own: slots round-trip values per channel independently, the dirty
 * drain visits exactly what was written and clears itself, and - the load-bearing part - every
 * touch of a freed handle fails loudly, including after its slot index has been recycled to a
 * new tenant. An animation holding a handle to a disposed node must get an exception with a
 * name, never a silent write into someone else's slot.
 */
class ChannelBlockTest {

    @Test
    fun slotsRoundTripValuesPerChannelIndependently() {
        val block = ChannelBlock(channels = 3, capacity = 4)
        val a = block.allocate()
        val b = block.allocate()

        block.set(0, a, 1f)
        block.set(2, a, 3f)
        block.set(0, b, 10f)

        assertEquals(1f, block.get(0, a), 0f)
        assertEquals(0f, block.get(1, a), 0f)
        assertEquals(3f, block.get(2, a), 0f)
        assertEquals(10f, block.get(0, b), 0f)
        // First tenancy only: a fresh buffer is born zeroed. A recycled slot is NOT - see
        // recycledSlotRetainsThePreviousTenantsValuesUntilOverwritten.
        assertEquals("b's other channels untouched (fresh buffer)", 0f, block.get(2, b), 0f)
        assertEquals(2, block.liveCount)
    }

    @Test
    fun staleHandleThrowsOnWriteReadAndReFree() {
        val block = ChannelBlock(channels = 1, capacity = 2)
        val handle = block.allocate()
        block.set(0, handle, 5f)
        block.free(handle)

        assertThrows(IllegalStateException::class.java) { block.set(0, handle, 6f) }
        assertThrows(IllegalStateException::class.java) { block.get(0, handle) }
        assertThrows(IllegalStateException::class.java) { block.touch(handle) }
        assertThrows("a double free is a stale handle too", IllegalStateException::class.java) {
            block.free(handle)
        }
        assertEquals(0, block.liveCount)
    }

    @Test
    fun recycledIndexGetsAFreshGenerationAndTheOldHandleStaysDead() {
        val block = ChannelBlock(channels = 1, capacity = 1)
        val old = block.allocate()
        block.free(old)

        val recycled = block.allocate()
        assertEquals("one slot: the index must be reused", block.indexOf(recycled), old.toInt())
        assertNotEquals("but under a new handle", old, recycled)

        block.set(0, recycled, 42f)
        assertThrows("the old tenant's handle must not reach the new tenant's value",
            IllegalStateException::class.java) { block.get(0, old) }
        assertEquals(42f, block.get(0, recycled), 0f)
    }

    @Test
    fun recycledSlotRetainsThePreviousTenantsValuesUntilOverwritten() {
        // Pins the binding writer rule's sharp edge: allocate() hands back the slot's floats
        // exactly as the previous tenant left them - no clear, by design (zeroing would not
        // make a partial bind safe: an all-zero rotation is degenerate, not neutral; the rule
        // is that a binder writes every channel before the first drain). If a fill is ever
        // added, this test goes stale on purpose - re-decide the writer rule before deleting it.
        val block = ChannelBlock(channels = 2, capacity = 1)
        val old = block.allocate()
        block.set(0, old, 7f)
        block.set(1, old, 9f)
        block.free(old)

        val recycled = block.allocate()
        assertEquals("same slot index", 0, block.indexOf(recycled))
        block.set(0, recycled, 1f) // the new tenant writes only channel 0...
        assertEquals(1f, block.get(0, recycled), 0f)
        assertEquals(
            "...and an unwritten channel shows the dead tenant's value",
            9f, block.get(1, recycled), 0f
        )
    }

    @Test
    fun exhaustionFailsLoudly() {
        val block = ChannelBlock(channels = 1, capacity = 2)
        block.allocate()
        block.allocate()
        val e = assertThrows(IllegalStateException::class.java) { block.allocate() }
        assertTrue(e.message!!.contains("exhausted"))
    }

    @Test
    fun dirtyDrainVisitsWritesOnceAscendingAndClears() {
        val block = ChannelBlock(channels = 2, capacity = 70) // spans two bitset words
        val handles = LongArray(70) { block.allocate() }

        block.set(0, handles[65], 1f) // written out of order,
        block.set(1, handles[3], 2f)
        block.set(0, handles[3], 3f) // twice on one slot,
        block.touch(handles[40]) // and one touch with no value write

        val visited = mutableListOf<Int>()
        block.forEachDirtyClearing { visited.add(it) }
        assertEquals("each dirty slot once, in ascending order", listOf(3, 40, 65), visited)

        visited.clear()
        block.forEachDirtyClearing { visited.add(it) }
        assertTrue("the drain cleared the set", visited.isEmpty())
    }

    @Test
    fun freeingASlotRemovesItFromTheDirtySet() {
        val block = ChannelBlock(channels = 1, capacity = 4)
        val doomed = block.allocate()
        val kept = block.allocate()
        block.set(0, doomed, 1f)
        block.set(0, kept, 2f)
        block.free(doomed)

        val visited = mutableListOf<Int>()
        block.forEachDirtyClearing { visited.add(it) }
        assertEquals("a freed slot must never reach the sampler", listOf(block.indexOf(kept)), visited)
    }

    @Test
    fun forgedAndOutOfRangeHandlesAreRejected() {
        val block = ChannelBlock(channels = 1, capacity = 2)
        block.allocate()
        assertThrows(IllegalArgumentException::class.java) { block.get(0, 5L) } // index out of range
        assertThrows(IllegalArgumentException::class.java) { block.get(0, -1L) }
        val e = assertThrows(IllegalStateException::class.java) {
            block.get(0, (7L shl 32) or 0L) // right index, wrong generation
        }
        assertTrue(e.message!!.contains("stale"))
    }

    @Test
    fun channelBoundsAreChecked() {
        val block = ChannelBlock(channels = 2, capacity = 1)
        val handle = block.allocate()
        assertThrows(IllegalArgumentException::class.java) { block.set(2, handle, 1f) }
        assertThrows(IllegalArgumentException::class.java) { block.get(-1, handle) }
    }
}
