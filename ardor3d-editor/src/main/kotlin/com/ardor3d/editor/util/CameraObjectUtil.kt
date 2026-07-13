/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.util

import com.ardor3d.renderer.Camera

/**
 * Pure math for the editor's camera objects. A camera object is a
 * [com.ardor3d.scenegraph.extension.CameraNode] whose managed [Camera] carries the frustum the
 * inspector edits and play mode renders through.
 *
 * Field of view is always *derived* from the frustum planes rather than read from
 * [Camera.getFovY]: that value is only populated by [Camera.setFrustumPerspective] and is not
 * written by [Camera.write], so it is NaN after a binary round trip.
 */
object CameraObjectUtil {
    const val DEFAULT_FOV_DEGREES = 60.0
    const val DEFAULT_NEAR = 0.1
    const val DEFAULT_FAR = 1000.0

    /** Sensible bounds for the vertical field of view the inspector exposes. */
    const val MIN_FOV_DEGREES = 5.0
    const val MAX_FOV_DEGREES = 170.0

    /**
     * Vertical field of view in degrees, recovered from the frustum geometry:
     * `setFrustumPerspective` sets `top = tan(fovY/2) * near`, so `fovY = 2*atan(top/near)`.
     * Falls back to [DEFAULT_FOV_DEGREES] for a degenerate (non-positive near) frustum.
     */
    fun fovYDegrees(camera: Camera): Double {
        val near = camera.frustumNear
        if (near <= 0.0) return DEFAULT_FOV_DEGREES
        return Math.toDegrees(2.0 * Math.atan(camera.frustumTop / near))
    }

    /**
     * Aspect ratio (width / height) recovered from the frustum planes
     * (`right = top * aspect`). Falls back to 1.0 for a degenerate frustum.
     */
    fun aspect(camera: Camera): Double {
        val top = camera.frustumTop
        if (top == 0.0) return 1.0
        return camera.frustumRight / top
    }

    /**
     * Applies a symmetric perspective frustum, clamping every input so the camera is never left
     * degenerate: fov to [[MIN_FOV_DEGREES], [MAX_FOV_DEGREES]], a non-finite or non-positive
     * aspect to 1.0, near above zero, and far strictly beyond near.
     */
    fun setPerspective(camera: Camera, fovDegrees: Double, aspect: Double, near: Double, far: Double) {
        val fov = fovDegrees.coerceIn(MIN_FOV_DEGREES, MAX_FOV_DEGREES)
        val safeAspect = if (aspect.isFinite() && aspect > 0.0) aspect else 1.0
        val safeNear = if (near > 0.0) near else DEFAULT_NEAR
        val safeFar = if (far > safeNear) far else safeNear + 1.0
        camera.setFrustumPerspective(fov, safeAspect, safeNear, safeFar)
    }

    /**
     * A signature of the frustum's *shape* (near, top, right) used to detect when a camera's
     * overlay gizmo needs its geometry rebuilt. Location and orientation are not included -
     * those are tracked live by moving the gizmo, not by rebuilding it.
     */
    fun frustumShape(camera: Camera): DoubleArray =
        doubleArrayOf(camera.frustumNear, camera.frustumTop, camera.frustumRight)
}
