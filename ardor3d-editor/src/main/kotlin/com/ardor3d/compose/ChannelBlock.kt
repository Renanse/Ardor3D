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

import com.ardor3d.buffer.BufferUtils
import java.nio.FloatBuffer

/**
 * A fixed-stride SoA store for values that change every frame - translation tracks, per-node
 * float params. Composed scenes own *structure* through recomposition, but frame-rate motion
 * must never ride that path (a recomposition per animation frame is exactly the cost the
 * composed layer exists to avoid). Instead, a scene node binds a **channel**: a slot in this
 * block, allocated when the binding enters the composition, released when it leaves. Game code
 * writes the slot imperatively every frame by handle; a sampler drains [forEachDirtyClearing]
 * after recomposition and applies the committed values to the live spatials. Composition owns
 * the binding, never the value.
 *
 * Values live off the Java heap in a direct [FloatBuffer] (the scene graph's native idiom),
 * one tightly packed array per channel. The per-frame surface - [set], [get], [touch],
 * [forEachDirtyClearing] - is primitives only: nothing here may allocate at frame rate, which
 * is what lets a slide or a pulse run through a channel at exactly zero bytes per frame (the
 * gate tests assert this).
 *
 * **Slot lifecycle, made loud:** [allocate] returns a long handle packing (generation, index);
 * [free] bumps the slot's generation first, so any later write, read, or re-free through a
 * stale handle throws instead of silently landing in recycled memory - a bare int index cannot
 * fail loudly once its index is reused. Structure and motion meet exactly here: a node disposed
 * mid-animation retires its slot once, and the animation's next write is an error with a name,
 * not a corrupted stranger's transform. [free] also returns the slot for immediate reuse -
 * there is no retired-but-unrecycled state, which is sound here because the single-threaded
 * host has no reader mid-pass to protect.
 *
 * **The binding writer rule:** [allocate] does not clear the slot's floats - a recycled slot
 * still holds whatever its previous tenant last wrote. Whoever binds a slot must write *every*
 * channel with real initial values before the first drain. Clearing would not make skipping
 * that safe: an all-zero rotation is degenerate, not neutral, so zeros are just a different
 * flavor of quietly-wrong data. The no-fill behavior is pinned by test.
 */
class ChannelBlock(val channels: Int, val capacity: Int) {

    companion object {
        /** px py pz | qx qy qz qw - a node's local translation and rotation. */
        const val TRANSFORM_CHANNELS = 7

        /** One float param (applied as written - never interpolated). */
        const val PARAM_CHANNELS = 1
    }

    init {
        require(channels > 0 && capacity > 0)
    }

    /** The block's values: [channels] contiguous arrays of [capacity] floats. */
    private val values: FloatBuffer = BufferUtils.createFloatBuffer(channels * capacity)

    // Free-list allocation, primitives only (pops ascending); a live-bit set backs the
    // double-free check and the dirty set below is the sampler's work queue.
    private val free = IntArray(capacity) { capacity - 1 - it }
    private var freeTop = capacity
    private val live = LongArray((capacity + 63) / 64)
    private val generation = IntArray(capacity)

    @PublishedApi // for the inline [forEachDirtyClearing]; not API - treat as private
    internal val dirty = LongArray((capacity + 63) / 64)

    /** Slots currently bound. */
    val liveCount: Int get() = capacity - freeTop

    fun allocate(): Long {
        check(freeTop > 0) { "channel block exhausted: all $capacity slots live" }
        val index = free[--freeTop]
        live[index ushr 6] = live[index ushr 6] or (1L shl (index and 63))
        return (generation[index].toLong() shl 32) or index.toLong()
    }

    /**
     * Releases [handle]'s slot. The generation bumps *first*, so the handle (and any copy of
     * it an animation still holds) is stale from this call onward; the slot only re-enters the
     * free list here, giving the next [allocate] a fresh generation over the same index.
     */
    fun free(handle: Long) {
        val index = checkedIndex(handle)
        generation[index]++
        val mask = 1L shl (index and 63)
        require(live[index ushr 6] and mask != 0L) { "double free of channel slot $index" }
        live[index ushr 6] = live[index ushr 6] and mask.inv()
        dirty[index ushr 6] = dirty[index ushr 6] and mask.inv()
        free[freeTop++] = index
    }

    /** The raw slot index behind [handle] - how a sampler keys its target tables. Checks staleness. */
    fun indexOf(handle: Long): Int = checkedIndex(handle)

    /** Writes one channel of [handle]'s slot and queues the slot for the next sampler drain. */
    fun set(channel: Int, handle: Long, value: Float) {
        val index = checkedIndex(handle)
        values.put(offset(channel, index), value)
        dirty[index ushr 6] = dirty[index ushr 6] or (1L shl (index and 63))
    }

    fun get(channel: Int, handle: Long): Float = values.get(offset(channel, checkedIndex(handle)))

    /**
     * Queues [handle]'s slot for the next sampler drain without writing a value - for when an
     * input the sampler combines with the channel value (held outside the block) changed.
     */
    fun touch(handle: Long) {
        val index = checkedIndex(handle)
        dirty[index ushr 6] = dirty[index ushr 6] or (1L shl (index and 63))
    }

    /** Reads one channel by raw slot index - the sampler's drain-path read (no handle in hand). */
    internal fun getByIndex(channel: Int, index: Int): Float = values.get(offset(channel, index))

    /**
     * Visits every dirty slot index in ascending order and clears the set - the sampler's
     * drain. Inline so the per-frame caller pays no lambda object; like the rest of the
     * per-frame surface, this must add nothing to the Java heap.
     *
     * [action] must not mutate the block (no allocate/free/set/touch from inside the drain):
     * each word is snapshotted before its slots are visited, so a mid-drain mutation would be
     * half-seen. The drain reads values by raw index, deliberately without a staleness check -
     * that is only sound while this contract holds.
     */
    inline fun forEachDirtyClearing(action: (Int) -> Unit) {
        for (word in dirty.indices) {
            var bits = dirty[word]
            dirty[word] = 0L
            while (bits != 0L) {
                action((word shl 6) + bits.countTrailingZeroBits())
                bits = bits and (bits - 1)
            }
        }
    }

    private fun offset(channel: Int, index: Int): Int {
        require(channel in 0 until channels) { "channel $channel outside 0 until $channels" }
        return channel * capacity + index
    }

    private fun checkedIndex(handle: Long): Int {
        val index = handle.toInt()
        val gen = (handle ushr 32).toInt()
        require(index in 0 until capacity) { "handle index $index outside capacity $capacity" }
        check(gen == generation[index]) {
            "stale channel handle: slot $index generation $gen, current ${generation[index]}"
        }
        return index
    }
}
