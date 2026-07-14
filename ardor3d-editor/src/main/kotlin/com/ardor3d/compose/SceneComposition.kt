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

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import com.ardor3d.scenegraph.Node
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Hosts a scene-graph composition: composable content emits spatials under a root [Node] through a
 * [SpatialApplier], and recomposition keeps the emitted tree in sync with the state it read. The
 * host owns its own [Recomposer] and frame clock, deliberately separate from the Compose Desktop UI
 * recomposer: scene recomposition is driven by the caller's update loop via [frame], not by the
 * UI's vsync, so scene changes land at a known point in the frame and never race rendering.
 *
 * Single-threaded by contract: construct, write state, [frame], and [close] all on one thread (in
 * the editor, the update thread). Recomposition and applier changes run synchronously inside
 * [frame] before it returns; an idle frame - no state written since the last one - does no work
 * and touches nothing.
 */
class SceneComposition(applier: SpatialApplier) : AutoCloseable {

    /** Composes directly under [root] with a plain [SpatialApplier]. */
    constructor(root: Node) : this(SpatialApplier(root))

    private val clock = BroadcastFrameClock()

    // Recomposition failures land here (the runner coroutine dies); rethrown by the next frame()
    // so the driving loop sees the real stack instead of a composition that silently stops syncing.
    private var failure: Throwable? = null

    private val scope = CoroutineScope(
        InlineDispatcher + clock + Job() + CoroutineExceptionHandler { _, e -> failure = e })
    private val recomposer = Recomposer(scope.coroutineContext)
    private val composition = Composition(applier, recomposer)

    init {
        // UNDISPATCHED + the inline dispatcher: the runner starts here and now, and every later
        // resumption stays on whichever thread resumed it - the machinery never leaves the caller.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { recomposer.runRecomposeAndApplyChanges() }
    }

    /** Sets (or replaces) the composed content; the initial tree is emitted before this returns. */
    fun setContent(content: @Composable () -> Unit) {
        composition.setContent(content)
    }

    /**
     * Drives one composition frame: flushes snapshot state writes into invalidations, then - only if
     * something now awaits a frame - sends [frameTimeNanos] through the clock, which recomposes and
     * applies the resulting scene changes synchronously before this returns.
     */
    fun frame(frameTimeNanos: Long) {
        Snapshot.sendApplyNotifications()
        if (clock.hasAwaiters) {
            clock.sendFrame(frameTimeNanos)
        }
        failure?.let { throw IllegalStateException("Scene recomposition failed", it) }
    }

    override fun close() {
        composition.dispose()
        recomposer.cancel()
        scope.cancel()
    }

    /**
     * Runs every resumption inline on the resuming thread - this is what makes [frame] (and with it
     * all recomposition) synchronous instead of scheduled.
     */
    private object InlineDispatcher : CoroutineDispatcher() {
        override fun isDispatchNeeded(context: CoroutineContext): Boolean = false
        override fun dispatch(context: CoroutineContext, block: Runnable) = block.run()
    }
}
