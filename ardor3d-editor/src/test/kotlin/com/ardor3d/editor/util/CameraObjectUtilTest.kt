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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Field-of-view and frustum math for camera objects. A plain [Camera] is a pure JVM object here -
 * no GL context is required to set or read its frustum planes.
 */
class CameraObjectUtilTest {

    @Test
    fun fovAndAspectRecoveredFromFrustumPlanes() {
        val cam = Camera(100, 100)
        CameraObjectUtil.setPerspective(cam, 60.0, 1.5, 0.1, 500.0)

        assertEquals(60.0, CameraObjectUtil.fovYDegrees(cam), 1e-9)
        assertEquals(1.5, CameraObjectUtil.aspect(cam), 1e-9)
        assertEquals(0.1, cam.frustumNear, 1e-9)
        assertEquals(500.0, cam.frustumFar, 1e-9)
    }

    @Test
    fun editingNearPreservesFieldOfView() {
        val cam = Camera(100, 100)
        CameraObjectUtil.setPerspective(cam, 45.0, 1.0, 0.1, 100.0)

        // Mirror the inspector's Near edit: hold fov and aspect, change only near.
        CameraObjectUtil.setPerspective(
            cam, CameraObjectUtil.fovYDegrees(cam), CameraObjectUtil.aspect(cam), 2.0, cam.frustumFar
        )

        assertEquals(45.0, CameraObjectUtil.fovYDegrees(cam), 1e-9)
        assertEquals(2.0, cam.frustumNear, 1e-9)
        assertEquals(100.0, cam.frustumFar, 1e-9)
    }

    @Test
    fun fieldOfViewIsClampedToSaneBounds() {
        val cam = Camera(100, 100)
        CameraObjectUtil.setPerspective(cam, 1000.0, 1.0, 0.1, 100.0)
        assertEquals(CameraObjectUtil.MAX_FOV_DEGREES, CameraObjectUtil.fovYDegrees(cam), 1e-6)

        CameraObjectUtil.setPerspective(cam, 0.0, 1.0, 0.1, 100.0)
        assertEquals(CameraObjectUtil.MIN_FOV_DEGREES, CameraObjectUtil.fovYDegrees(cam), 1e-6)
    }

    @Test
    fun degenerateInputsAreSanitized() {
        val cam = Camera(100, 100)

        // A non-positive/non-finite aspect must not produce a degenerate frustum.
        CameraObjectUtil.setPerspective(cam, 60.0, 0.0, 0.1, 100.0)
        assertTrue("frustum width must be positive", cam.frustumRight > 0.0)

        CameraObjectUtil.setPerspective(cam, 60.0, Double.NaN, 0.1, 100.0)
        assertTrue("frustum width must be positive", cam.frustumRight > 0.0)

        // Far must end up strictly beyond near even when asked for the reverse.
        CameraObjectUtil.setPerspective(cam, 60.0, 1.0, 5.0, 1.0)
        assertTrue("far must exceed near", cam.frustumFar > cam.frustumNear)
    }

    @Test
    fun frustumShapeIgnoresPose() {
        val cam = Camera(100, 100)
        CameraObjectUtil.setPerspective(cam, 60.0, 1.0, 0.1, 100.0)
        val before = CameraObjectUtil.frustumShape(cam)

        // Moving the camera must not change its frustum shape signature.
        cam.setLocation(10.0, 20.0, 30.0)
        cam.lookAt(0.0, 0.0, 0.0, com.ardor3d.math.Vector3.UNIT_Y)
        val after = CameraObjectUtil.frustumShape(cam)

        assertTrue("pose changes must not alter the frustum shape", before.contentEquals(after))
    }
}
