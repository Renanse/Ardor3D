/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.interact

import com.ardor3d.extension.interact.widget.gizmo.GizmoPart
import com.ardor3d.math.Vector3
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * The gizmo drag readout must name the axis (or axes) the active handle actually manipulates, so
 * the value is unambiguous without leaning on the red/green/blue handle coloring. Single-axis
 * handles show one labelled component, planes show two, and a center/free drag (or an unresolved
 * handle) labels all three. Rotate rings and per-axis scale name their axis; the screen-space ring
 * and uniform scale have no single axis to name.
 */
class GizmoReadoutFormatTest {

    private val delta = Vector3(1.2, -3.4, 0.5)

    @Test
    fun translateSingleAxisNamesOnlyThatComponent() {
        assertEquals("X +1.20", GizmoReadoutFormat.translate(delta, GizmoPart.AxisX))
        assertEquals("Y -3.40", GizmoReadoutFormat.translate(delta, GizmoPart.AxisY))
        assertEquals("Z +0.50", GizmoReadoutFormat.translate(delta, GizmoPart.AxisZ))
    }

    @Test
    fun translatePlaneNamesItsTwoComponents() {
        assertEquals("X +1.20  Y -3.40", GizmoReadoutFormat.translate(delta, GizmoPart.PlaneXY))
        assertEquals("X +1.20  Z +0.50", GizmoReadoutFormat.translate(delta, GizmoPart.PlaneXZ))
        assertEquals("Y -3.40  Z +0.50", GizmoReadoutFormat.translate(delta, GizmoPart.PlaneYZ))
    }

    @Test
    fun translateCenterOrUnknownLabelsAllThree() {
        val expected = "X +1.20  Y -3.40  Z +0.50"
        assertEquals(expected, GizmoReadoutFormat.translate(delta, GizmoPart.Center))
        assertEquals(expected, GizmoReadoutFormat.translate(delta, null))
    }

    @Test
    fun rotateRingNamesItsAxis() {
        val quarter = Math.toRadians(45.0)
        assertEquals("X 45.0 deg", GizmoReadoutFormat.rotate(quarter, GizmoPart.RingX))
        assertEquals("Y 45.0 deg", GizmoReadoutFormat.rotate(quarter, GizmoPart.RingY))
        assertEquals("Z 45.0 deg", GizmoReadoutFormat.rotate(quarter, GizmoPart.RingZ))
    }

    @Test
    fun rotateScreenRingHasNoAxisLabel() {
        assertEquals("45.0 deg", GizmoReadoutFormat.rotate(Math.toRadians(45.0), GizmoPart.RingView))
        assertEquals("45.0 deg", GizmoReadoutFormat.rotate(Math.toRadians(45.0), null))
    }

    @Test
    fun scaleSingleAxisNamesThatAxisFactor() {
        val factor = Vector3(1.5, 2.0, 0.25)
        assertEquals("X 1.50x", GizmoReadoutFormat.scale(factor, GizmoPart.AxisX))
        assertEquals("Y 2.00x", GizmoReadoutFormat.scale(factor, GizmoPart.AxisY))
        assertEquals("Z 0.25x", GizmoReadoutFormat.scale(factor, GizmoPart.AxisZ))
    }

    @Test
    fun scaleUniformIsUnlabelled() {
        // Uniform scale carries the same factor on every axis; the built-in text uses X.
        val factor = Vector3(1.5, 1.5, 1.5)
        assertEquals("1.50x", GizmoReadoutFormat.scale(factor, GizmoPart.Center))
        assertEquals("1.50x", GizmoReadoutFormat.scale(factor, null))
    }

    @Test
    fun formattingIsLocaleIndependent() {
        // A comma-decimal locale must not swap '.' for ',' (which, for translate, would also collide
        // the decimal with the space delimiter's neighbours). Everything is pinned to Locale.ROOT.
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("X +1.20", GizmoReadoutFormat.translate(delta, GizmoPart.AxisX))
            assertEquals("X 45.0 deg", GizmoReadoutFormat.rotate(Math.toRadians(45.0), GizmoPart.RingX))
            assertEquals("X 1.50x", GizmoReadoutFormat.scale(Vector3(1.5, 1.0, 1.0), GizmoPart.AxisX))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
