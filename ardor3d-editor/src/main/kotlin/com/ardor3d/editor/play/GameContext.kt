/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.play

import com.ardor3d.intersection.PickingUtil
import com.ardor3d.intersection.PrimitivePickResults
import com.ardor3d.math.Vector2
import com.ardor3d.renderer.Camera
import com.ardor3d.scenegraph.Node

/**
 * The durable runtime services a [GameMode] gets for the lifetime of a play session: the live play
 * scene it reads and mutates, the camera the viewport renders through, and picking. Frame-varying
 * input is *not* here - that is handed to [GameMode.update] as a [GameInput] each frame.
 *
 * Reminder on the state/view split this plan is built around: the scene graph reached through
 * [sceneRoot] is the game's *view*. A [GameMode] should own its own plain-data model and sync the
 * scene from it - not treat the scene graph as the source of truth.
 */
interface GameContext {
    /** The live play-scene root the game reads and mutates. Restored to its pre-play state on stop. */
    val sceneRoot: Node

    /** The camera the viewport renders through while playing (the frame [pick] rays are cast from). */
    val camera: Camera

    /**
     * Picks the play scene under a canvas pixel ([GameInput] coordinates: bottom-left origin),
     * against actual primitives, closest-first. Empty results mean nothing was hit.
     */
    fun pick(x: Int, y: Int): PrimitivePickResults
}

/**
 * The standard [GameContext]: picking casts a ray from [camera] through the pixel and tests the
 * primitives under [sceneRoot], mirroring the editor's own click-picking.
 */
class SceneGameContext(
    override val sceneRoot: Node,
    override val camera: Camera
) : GameContext {
    override fun pick(x: Int, y: Int): PrimitivePickResults {
        val results = PrimitivePickResults()
        results.setCheckDistance(true)
        val ray = camera.getPickRay(Vector2(x.toDouble(), y.toDouble()), false, null)
        PickingUtil.findPick(sceneRoot, ray, results)
        return results
    }
}
