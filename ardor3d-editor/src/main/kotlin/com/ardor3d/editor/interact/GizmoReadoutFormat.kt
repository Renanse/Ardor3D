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
import com.ardor3d.math.type.ReadOnlyVector3
import java.util.Locale

/**
 * Axis-aware text for the transform gizmos' drag readouts. Naming the axis - and showing only the
 * components the active handle actually manipulates - keeps the readout legible and, more to the
 * point, disambiguates the axis for viewers who cannot rely on the red/green/blue = X/Y/Z handle
 * coloring (the red/green X/Y pair is exactly the deuteranopia-confusable one). These feed the
 * gizmos' own ReadoutFormatter hooks, so no gizmo-library change is needed.
 *
 * [part] is the active handle's GizmoPart (from AbstractGizmo.getActiveHandle), or null when it
 * cannot be resolved - in which case each formatter falls back to its fullest labelled form.
 *
 * All formatting is pinned to [Locale.ROOT]: the strings use '.' decimals and, for translation, a
 * space delimiter, so a comma-decimal JVM locale can neither swap the separator nor collide it with
 * the delimiter.
 */
internal object GizmoReadoutFormat {

    /** Signed translation delta, per axis, in world units. */
    fun translate(delta: ReadOnlyVector3, part: GizmoPart?): String = when (part) {
        GizmoPart.AxisX -> component("X", delta.x)
        GizmoPart.AxisY -> component("Y", delta.y)
        GizmoPart.AxisZ -> component("Z", delta.z)
        GizmoPart.PlaneXY -> component("X", delta.x) + GAP + component("Y", delta.y)
        GizmoPart.PlaneXZ -> component("X", delta.x) + GAP + component("Z", delta.z)
        GizmoPart.PlaneYZ -> component("Y", delta.y) + GAP + component("Z", delta.z)
        // Center (view-plane) drag, or an unresolved handle: label all three.
        else -> component("X", delta.x) + GAP + component("Y", delta.y) + GAP + component("Z", delta.z)
    }

    /** Drag angle in degrees (ASCII 'deg'; the readout font carries no degree glyph). */
    fun rotate(angleRadians: Double, part: GizmoPart?): String {
        val deg = String.format(Locale.ROOT, "%.1f deg", Math.toDegrees(angleRadians))
        return when (part) {
            GizmoPart.RingX -> "X $deg"
            GizmoPart.RingY -> "Y $deg"
            GizmoPart.RingZ -> "Z $deg"
            // Screen-space ring rotates about the view direction - no world axis to name.
            else -> deg
        }
    }

    /** Scale factor since the drag began (ASCII 'x', not the multiply glyph). */
    fun scale(factor: ReadOnlyVector3, part: GizmoPart?): String = when (part) {
        GizmoPart.AxisX -> factorText("X", factor.x)
        GizmoPart.AxisY -> factorText("Y", factor.y)
        GizmoPart.AxisZ -> factorText("Z", factor.z)
        // Uniform scale: every axis shares the factor, so name none (matches the built-in text).
        else -> String.format(Locale.ROOT, "%.2fx", factor.x)
    }

    /** Two spaces between per-axis components, so they stay readable in the centered label. */
    private const val GAP = "  "

    private fun component(axis: String, value: Double) = String.format(Locale.ROOT, "%s %+.2f", axis, value)

    private fun factorText(axis: String, value: Double) = String.format(Locale.ROOT, "%s %.2fx", axis, value)
}
