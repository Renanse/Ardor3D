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

import com.ardor3d.math.Matrix3
import com.ardor3d.math.Quaternion
import com.ardor3d.math.Vector3
import com.ardor3d.math.util.MathUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class EulerUtilTest {

    private fun assertSameRotation(expected: Quaternion, actual: Quaternion, message: String) {
        // Compare by applying to basis vectors so q and -q count as equal.
        for (basis in listOf(Vector3.UNIT_X, Vector3.UNIT_Y, Vector3.UNIT_Z)) {
            val e = expected.apply(basis, null)
            val a = actual.apply(basis, null)
            assertEquals("$message: x of rotated $basis", e.x, a.x, 1e-9)
            assertEquals("$message: y of rotated $basis", e.y, a.y, 1e-9)
            assertEquals("$message: z of rotated $basis", e.z, a.z, 1e-9)
        }
    }

    @Test
    fun singleAxisAnglesMapToLabeledAxes() {
        val cases = listOf(
            Triple(Triple(37.5, 0.0, 0.0), Vector3.UNIT_X, "X"),
            Triple(Triple(0.0, 37.5, 0.0), Vector3.UNIT_Y, "Y"),
            Triple(Triple(0.0, 0.0, 37.5), Vector3.UNIT_Z, "Z")
        )
        for ((angles, axis, label) in cases) {
            val (x, y, z) = angles
            val fromEuler = EulerUtil.fromEulerDegrees(x, y, z)
            val fromAxis = Quaternion().fromAngleAxis(MathUtils.DEG_TO_RAD * 37.5, axis)
            assertSameRotation(fromAxis, fromEuler, "rotation about $label")
        }
    }

    @Test
    fun toEulerDegreesReadsBackSingleAxisAngles() {
        for (index in 0..2) {
            val input = doubleArrayOf(0.0, 0.0, 0.0)
            input[index] = -28.25
            val rotation = EulerUtil.fromEulerDegrees(input[0], input[1], input[2])
                .toRotationMatrix(Matrix3())
            val output = EulerUtil.toEulerDegrees(rotation)
            for (check in 0..2) {
                assertEquals("component $check for input axis $index", input[check], output[check], 1e-9)
            }
        }
    }

    @Test
    fun roundTripRecoversCombinedRotation() {
        // Stay away from the attitude (Z) gimbal singularity at +/-90 degrees.
        val samples = listOf(
            Triple(10.0, 20.0, 30.0),
            Triple(-45.0, 60.0, -75.0),
            Triple(120.0, -150.0, 45.0),
            Triple(-5.0, 175.0, -85.0)
        )
        for ((x, y, z) in samples) {
            val original = EulerUtil.fromEulerDegrees(x, y, z)
            val angles = EulerUtil.toEulerDegrees(original.toRotationMatrix(Matrix3()))
            val roundTripped = EulerUtil.fromEulerDegrees(angles[0], angles[1], angles[2])
            assertSameRotation(original, roundTripped, "round trip of ($x, $y, $z)")
        }
    }
}
