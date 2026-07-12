/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor

import com.ardor3d.math.ColorRGBA
import com.ardor3d.math.Vector3
import com.ardor3d.scenegraph.Line
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.shape.Box
import com.ardor3d.scenegraph.shape.Sphere

/**
 * Editor-only overlay geometry (grid, starter content, light gizmos). These are stateless
 * factories: they build fresh scene graphs the [EditorScene] attaches to its overlay/document
 * roots, and hold no editor state of their own.
 */

/**
 * Creates an editor grid on the XZ plane.
 */
internal fun createGrid(): Node {
    val gridNode = Node("EditorGrid")

    val gridLines = 21  // Number of lines (odd for center line)
    val gridSpacing = 1.0f  // 1 unit spacing
    val gridSize = (gridLines - 1) / 2 * gridSpacing  // Half-size of grid

    // Create vertices for grid lines
    val vertices = mutableListOf<Vector3>()

    for (i in 0 until gridLines) {
        val coord = (i - gridLines / 2) * gridSpacing
        // Lines along X axis
        vertices.add(Vector3(-gridSize.toDouble(), 0.0, coord.toDouble()))
        vertices.add(Vector3(gridSize.toDouble(), 0.0, coord.toDouble()))
        // Lines along Z axis
        vertices.add(Vector3(coord.toDouble(), 0.0, -gridSize.toDouble()))
        vertices.add(Vector3(coord.toDouble(), 0.0, gridSize.toDouble()))
    }

    val grid = Line("Grid", vertices.toTypedArray(), null, null, null)
    grid.setDefaultColor(ColorRGBA(0.35f, 0.35f, 0.4f, 1f))  // Subtle gray
    grid.lineWidth = 1.0f
    gridNode.attachChild(grid)

    // Add colored axis lines (X=red, Z=blue) at center
    val axisLength = gridSize.toDouble() + 0.5

    // X axis (red)
    val xAxisVerts = arrayOf(
        Vector3(-axisLength, 0.01, 0.0),
        Vector3(axisLength, 0.01, 0.0)
    )
    val xAxis = Line("XAxis", xAxisVerts, null, null, null)
    xAxis.setDefaultColor(ColorRGBA(0.8f, 0.2f, 0.2f, 1f))
    xAxis.lineWidth = 2.0f
    gridNode.attachChild(xAxis)

    // Z axis (blue)
    val zAxisVerts = arrayOf(
        Vector3(0.0, 0.01, -axisLength),
        Vector3(0.0, 0.01, axisLength)
    )
    val zAxis = Line("ZAxis", zAxisVerts, null, null, null)
    zAxis.setDefaultColor(ColorRGBA(0.2f, 0.2f, 0.8f, 1f))
    zAxis.lineWidth = 2.0f
    gridNode.attachChild(zAxis)

    return gridNode
}

/**
 * Creates a test cube for the scene.
 */
internal fun createTestCube(): Box {
    val cube = Box("TestCube", Vector3.ZERO, 1.0, 1.0, 1.0)
    cube.setTranslation(0.0, 1.0, 0.0)
    cube.modelBound = com.ardor3d.bounding.BoundingBox()
    return cube
}

/**
 * Creates a visual gizmo to show light position.
 */
internal fun createLightGizmo(): Node {
    val gizmoNode = Node("LightGizmo")

    // Small yellow sphere to represent light
    val sphere = Sphere("LightSphere", 8, 8, 0.3)
    sphere.setDefaultColor(ColorRGBA(1f, 0.9f, 0.3f, 1f))  // Yellow
    gizmoNode.attachChild(sphere)

    // Add rays emanating from the light to make it more visible
    val rayLength = 0.6
    val rayColor = ColorRGBA(1f, 0.95f, 0.5f, 0.7f)

    // Create 6 rays pointing in cardinal directions
    val directions = listOf(
        Vector3.UNIT_X, Vector3.NEG_UNIT_X,
        Vector3.UNIT_Y, Vector3.NEG_UNIT_Y,
        Vector3.UNIT_Z, Vector3.NEG_UNIT_Z
    )

    for ((index, dir) in directions.withIndex()) {
        val rayVerts = arrayOf(
            Vector3.ZERO,
            Vector3(dir).multiplyLocal(rayLength)
        )
        val ray = Line("LightRay$index", rayVerts, null, null, null)
        ray.setDefaultColor(rayColor)
        ray.lineWidth = 2.0f
        gizmoNode.attachChild(ray)
    }

    return gizmoNode
}
