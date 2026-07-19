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
import com.ardor3d.scenegraph.Spatial

/**
 * One composed scene's channels: the value blocks, the target tables that map slots back to
 * live scene objects, and the sampler that applies committed values. This is how frame-rate
 * motion coexists with a composed scene without riding recomposition:
 *
 *  - **Composition owns the binding.** A composable binds a channel when it enters the
 *    composition and releases it when it leaves; the binding seeds every channel from the
 *    target's current state ([ChannelBlock]'s binding writer rule), so the slot starts as the
 *    status quo, never a recycled slot's leftovers.
 *  - **Game code owns the value.** Per-frame writers ([writeTranslation], [writeParam]) go
 *    straight to the block by handle - no recomposition, no applier traffic, no allocation.
 *    Recomposition of the owning node must not write a channel-owned value; the block is the
 *    single source of truth for it.
 *  - **The sampler commits.** [sample] drains the dirty slots and applies them - translation
 *    and rotation onto the bound [Spatial], params through their [FloatTarget] - once per
 *    update, after the composition frame, so structural changes and motion land in a known
 *    order.
 *
 * Exactly two channel types exist: a transform track (translation + rotation; scale stays
 * composition-owned) and a single float param. Single-threaded by the same contract as
 * [SceneComposition]: bind, write, [sample], and release all on the thread driving the update
 * loop. The per-frame surface allocates nothing; registries and binding are event-rate.
 */
class SceneChannels(transformCapacity: Int = 64, paramCapacity: Int = 64) {

    /** Where a param channel's committed value lands - implementations write render state. */
    fun interface FloatTarget {
        fun apply(value: Float)
    }

    private val transforms = ChannelBlock(ChannelBlock.TRANSFORM_CHANNELS, transformCapacity)
    private val params = ChannelBlock(ChannelBlock.PARAM_CHANNELS, paramCapacity)

    private val transformTargets = arrayOfNulls<Spatial>(transformCapacity)
    private val paramTargets = arrayOfNulls<FloatTarget>(paramCapacity)

    // Key -> handle, so event-rate game code (a move starting, a pulse attaching) can find a
    // composed binding without walking the scene graph. Never touched at frame rate.
    private val transformsByKey = HashMap<Any, Long>()
    private val paramsByKey = HashMap<Any, Long>()

    private val quat = Quaternion() // sampler scratch, reused every drain

    /** Transform slots currently bound. */
    val liveTransformCount: Int get() = transforms.liveCount

    /** Param slots currently bound. */
    val liveParamCount: Int get() = params.liveCount

    // --- binding (event rate) ------------------------------------------------------------

    /**
     * Binds a transform channel to [target], seeding every channel from the target's current
     * local translation and rotation, and registers it under [key] (if any) for
     * [transformHandle] lookup. The first [sample] after this re-applies the status quo.
     */
    fun bindTransform(key: Any?, target: Spatial): Long {
        val handle = transforms.allocate()
        transformTargets[transforms.indexOf(handle)] = target
        val t = target.translation
        transforms.set(PX, handle, t.x.toFloat())
        transforms.set(PY, handle, t.y.toFloat())
        transforms.set(PZ, handle, t.z.toFloat())
        quat.fromRotationMatrix(target.rotation)
        transforms.set(QX, handle, quat.x.toFloat())
        transforms.set(QY, handle, quat.y.toFloat())
        transforms.set(QZ, handle, quat.z.toFloat())
        transforms.set(QW, handle, quat.w.toFloat())
        if (key != null) transformsByKey[key] = handle
        return handle
    }

    /**
     * Releases a transform binding. The registry entry for [key] is removed only if it still
     * points at [handle]: when a binding is replaced under the same key, the replacement
     * registers *before* the old binding is forgotten, and its entry must survive this call.
     */
    fun releaseTransform(key: Any?, handle: Long) {
        transformTargets[transforms.indexOf(handle)] = null
        if (key != null && transformsByKey[key] == handle) transformsByKey.remove(key)
        transforms.free(handle)
    }

    /** Binds a param channel delivering to [target], seeded with [initial]. See [bindTransform]. */
    fun bindParam(key: Any?, initial: Float, target: FloatTarget): Long {
        val handle = params.allocate()
        paramTargets[params.indexOf(handle)] = target
        params.set(PARAM, handle, initial)
        if (key != null) paramsByKey[key] = handle
        return handle
    }

    /** Releases a param binding. Same registry rule as [releaseTransform]. */
    fun releaseParam(key: Any?, handle: Long) {
        paramTargets[params.indexOf(handle)] = null
        if (key != null && paramsByKey[key] == handle) paramsByKey.remove(key)
        params.free(handle)
    }

    /** The handle bound under [key] - how a starting animation finds its channel. Loud if absent. */
    fun transformHandle(key: Any): Long =
        transformsByKey[key] ?: error("no transform channel bound under key $key")

    /** The param handle bound under [key]. Loud if absent. */
    fun paramHandle(key: Any): Long =
        paramsByKey[key] ?: error("no param channel bound under key $key")

    /** Whether a transform channel is currently bound under [key]. */
    fun hasTransform(key: Any): Boolean = transformsByKey.containsKey(key)

    /** Whether a param channel is currently bound under [key]. */
    fun hasParam(key: Any): Boolean = paramsByKey.containsKey(key)

    // --- the per-frame surface (must not allocate) -----------------------------------------

    /** Writes [handle]'s translation; the next [sample] applies it. Throws if [handle] is stale. */
    fun writeTranslation(handle: Long, x: Double, y: Double, z: Double) {
        transforms.set(PX, handle, x.toFloat())
        transforms.set(PY, handle, y.toFloat())
        transforms.set(PZ, handle, z.toFloat())
    }

    /** Writes [handle]'s param value; the next [sample] delivers it. Throws if [handle] is stale. */
    fun writeParam(handle: Long, value: Float) {
        params.set(PARAM, handle, value)
    }

    /**
     * Queues [handle]'s param for re-delivery at the next [sample] without changing its value -
     * for when an input the target combines with the value (held outside the block, e.g. a base
     * color) has changed.
     */
    fun touchParam(handle: Long) {
        params.touch(handle)
    }

    /**
     * Applies every value written (or touched) since the last call: transforms onto their bound
     * spatials, params through their targets. Nothing dirty, nothing done - an idle sample is a
     * bitset walk. Allocation-free; call once per update, after the composition frame.
     */
    fun sample() {
        transforms.forEachDirtyClearing { index ->
            val target = checkNotNull(transformTargets[index]) { "dirty transform slot $index has no target" }
            target.setTranslation(
                transforms.getByIndex(PX, index).toDouble(),
                transforms.getByIndex(PY, index).toDouble(),
                transforms.getByIndex(PZ, index).toDouble()
            )
            quat.set(
                transforms.getByIndex(QX, index).toDouble(),
                transforms.getByIndex(QY, index).toDouble(),
                transforms.getByIndex(QZ, index).toDouble(),
                transforms.getByIndex(QW, index).toDouble()
            )
            target.setRotation(quat)
        }
        params.forEachDirtyClearing { index ->
            val target = checkNotNull(paramTargets[index]) { "dirty param slot $index has no target" }
            target.apply(params.getByIndex(PARAM, index))
        }
    }

    // --- test access (same module only) ----------------------------------------------------

    internal val transformsForTest: ChannelBlock get() = transforms
    internal val paramsForTest: ChannelBlock get() = params

    internal companion object {
        // Transform block channel order.
        internal const val PX = 0
        internal const val PY = 1
        internal const val PZ = 2
        internal const val QX = 3
        internal const val QY = 4
        internal const val QZ = 5
        internal const val QW = 6

        // The param block's single channel.
        internal const val PARAM = 0
    }
}
