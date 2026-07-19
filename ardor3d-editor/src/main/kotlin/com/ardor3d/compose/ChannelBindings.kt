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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import com.ardor3d.scenegraph.Spatial

/**
 * The composition side of a channel: a binding is *made* by composition and *outlives
 * recomposition* - the slot is allocated when the binding enters the composition (inside the
 * remember lambda, so it exists even if that composition later fails to apply) and released
 * exactly once when it leaves, whether that is [onForgotten] (left the composition, or the
 * composition was disposed) or [onAbandoned] (the composition threw before applying). A second
 * release is a stale-handle error, deliberately loud: if lifecycle notifications ever double
 * up, that is a bug to hear about, not absorb.
 *
 * Replacement under the same remember position (a key change) creates the new binding *before*
 * the old one is forgotten - the two briefly hold different slots, so a channel capacity must
 * cover that transient overlap. The generation tag keeps the order honest: however the indexes
 * recycle, a straggler writing through the replaced binding throws instead of reaching the new
 * tenant.
 */
class TransformChannel internal constructor(
    private val channels: SceneChannels,
    private val key: Any?,
    target: Spatial
) : RememberObserver {

    /** The slot handle - allocated here, at composition time. */
    val handle: Long = channels.bindTransform(key, target)

    /** Per-frame writer for game code holding this binding. Throws once the binding is released. */
    fun writeTranslation(x: Double, y: Double, z: Double) =
        channels.writeTranslation(handle, x, y, z)

    override fun onRemembered() {
        // Nothing to do: the slot was already allocated in the remember lambda (see [handle]).
    }

    override fun onForgotten() = channels.releaseTransform(key, handle)
    override fun onAbandoned() = channels.releaseTransform(key, handle)
}

/** A param channel's binding; same lifecycle contract as [TransformChannel]. */
class ParamChannel internal constructor(
    private val channels: SceneChannels,
    private val key: Any?,
    initial: Float,
    target: SceneChannels.FloatTarget
) : RememberObserver {

    /** The slot handle - allocated here, at composition time. */
    val handle: Long = channels.bindParam(key, initial, target)

    /** Per-frame writer. Throws once the binding is released. */
    fun write(value: Float) = channels.writeParam(handle, value)

    /** Queues re-delivery of the current value - for when an outside input the target combines changed. */
    fun touch() = channels.touchParam(handle)

    override fun onRemembered() {
        // Nothing to do: the slot was already allocated in the remember lambda (see [handle]).
    }

    override fun onForgotten() = channels.releaseParam(key, handle)
    override fun onAbandoned() = channels.releaseParam(key, handle)
}

/**
 * Binds a transform channel for [target], alive exactly as long as this call stays in the
 * composition. Re-keyed by all three parameters: a new [key] or a new [target] instance is a
 * new binding (old released, new seeded from the target's then-current transform). [key] also
 * registers the binding for [SceneChannels.transformHandle] lookup by event-rate game code.
 */
@Composable
fun rememberTransformChannel(channels: SceneChannels, key: Any?, target: Spatial): TransformChannel =
    remember(channels, key, target) { TransformChannel(channels, key, target) }

/**
 * Binds a param channel delivering to [target], alive exactly as long as this call stays in
 * the composition. [initial] is captured at bind time only - it seeds the slot and is not a
 * remember key, so a changed initial alone does not re-bind.
 */
@Composable
fun rememberParamChannel(
    channels: SceneChannels,
    key: Any?,
    initial: Float,
    target: SceneChannels.FloatTarget
): ParamChannel =
    remember(channels, key, target) { ParamChannel(channels, key, initial, target) }
