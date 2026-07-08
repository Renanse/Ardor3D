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
import com.ardor3d.math.type.ReadOnlyMatrix3

/**
 * Helpers for editing rotations as XYZ Euler angles, in degrees, as presented in the inspector.
 *
 * Ardor3D's Quaternion Euler API works in (heading, attitude, bank) order - rotations about the
 * Y, Z and X axes respectively - so these helpers own the mapping between that convention and
 * the X/Y/Z labels shown to the user.
 */
object EulerUtil {

    /**
     * Extracts inspector-facing Euler angles, in degrees, from the given rotation:
     * index 0 = X (bank), 1 = Y (heading), 2 = Z (attitude).
     */
    fun toEulerDegrees(rotation: ReadOnlyMatrix3): DoubleArray {
        // toEulerAngles returns [heading (Y), attitude (Z), bank (X)]
        val angles = Quaternion().fromRotationMatrix(rotation).toEulerAngles(null)
        return doubleArrayOf(
            Math.toDegrees(angles[2]),
            Math.toDegrees(angles[0]),
            Math.toDegrees(angles[1])
        )
    }

    /**
     * Builds a quaternion from inspector-facing XYZ Euler angles, in degrees.
     */
    fun fromEulerDegrees(xDegrees: Double, yDegrees: Double, zDegrees: Double): Quaternion =
        Quaternion().fromEulerAngles(
            Math.toRadians(yDegrees), // heading (Y)
            Math.toRadians(zDegrees), // attitude (Z)
            Math.toRadians(xDegrees)  // bank (X)
        )

    /**
     * True when the inspector-facing [anglesDegrees] (X, Y, Z) produce [rotation]. Near the
     * attitude (Z) +/-90 degree singularity many angle triplets build the same matrix, so this
     * - not an angle comparison - is the way to decide whether an edited triplet still matches
     * the spatial's rotation or the rotation was changed externally (gizmo drag, undo).
     */
    fun representsRotation(anglesDegrees: DoubleArray, rotation: ReadOnlyMatrix3, epsilon: Double = 1e-7): Boolean {
        val built = fromEulerDegrees(anglesDegrees[0], anglesDegrees[1], anglesDegrees[2])
            .toRotationMatrix(Matrix3())
        for (row in 0..2) {
            for (col in 0..2) {
                if (Math.abs(built.getValue(row, col) - rotation.getValue(row, col)) > epsilon) {
                    return false
                }
            }
        }
        return true
    }
}
