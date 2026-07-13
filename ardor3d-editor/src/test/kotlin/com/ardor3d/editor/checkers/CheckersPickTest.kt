/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.checkers

import com.ardor3d.editor.EditorScene
import com.ardor3d.editor.EditorState
import com.ardor3d.framework.DisplaySettings
import com.ardor3d.framework.lwjgl3.GLFWCanvas
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer
import com.ardor3d.image.util.awt.AWTImageLoader
import com.ardor3d.input.PhysicalLayer
import com.ardor3d.input.logical.LogicalLayer
import com.ardor3d.intersection.PickingUtil
import com.ardor3d.intersection.PrimitivePickResults
import com.ardor3d.math.Vector2
import com.ardor3d.math.Vector3
import com.ardor3d.renderer.material.MaterialManager
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import com.ardor3d.util.ReadOnlyTimer
import com.ardor3d.util.resource.ResourceLocatorTool
import com.ardor3d.util.resource.SimpleResourceLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume
import org.junit.Test

/**
 * Guards the two bugs that made the board unclickable: board tiles need a finite model bound (the
 * default is an infinite BoundingSphere, whose ray broad-phase fails, so picking would miss every
 * tile), and starting checkers must clear the editing scene so leftover geometry (e.g. the default
 * cube at the origin) doesn't intercept clicks at the board centre. Projects a square's world centre
 * to the screen through the live render camera and casts a pick ray back - the exact path a click
 * takes. Runs the real scene through a headless GLFW canvas; SKIPS without a GL context.
 */
class CheckersPickTest {

    private val TIMER: ReadOnlyTimer = object : ReadOnlyTimer {
        override fun getTimeInSeconds() = 0.0
        override fun getTime() = 0L
        override fun getResolution() = 1_000_000_000L
        override fun getFrameRate() = 20.0
        override fun getTimePerFrame() = 0.05
        override fun getPreviousFrameTime() = 0L
    }

    @Test
    fun clicksResolveToTilesAndTheSceneIsCleared() {
        setupResourceLocators()

        val state = EditorState()
        val scene = EditorScene(state)
        val canvasRenderer = Lwjgl3CanvasRenderer(scene)
        var canvas: GLFWCanvas? = null
        try {
            canvas = GLFWCanvas(DisplaySettings(640, 480, 24, 0), canvasRenderer)
            canvas.init()
        } catch (t: Throwable) {
            canvas?.close()
            skipOrFail("Could not create a GL context here: $t")
            return
        }

        try {
            scene.canvasRenderer = canvasRenderer
            scene.canvas = canvas
            scene.setupInteractManager(canvas, PhysicalLayer.Builder().build(), LogicalLayer())
            canvas.draw(null)

            // Launch from the default scene (which has a cube at the origin). Do NOT clear manually -
            // starting checkers should clear it for us.
            scene.startCheckers()
            repeat(4) { scene.update(TIMER); canvas.draw(null) }

            // #1: the editing content is gone; only the (kept) camera and the checkers board remain.
            assertNull("default cube cleared", state.sceneRoot.children.firstOrNull { it.name == "TestCube" })
            val checkers = state.sceneRoot.children.first { it.name == "Checkers" } as Node

            // #2: a ray cast at each square's projected screen position hits that square's tile -
            // corners, a piece square, and the centre (which the default cube used to block).
            val camera = canvasRenderer.camera
            fun tileHitAt(row: Int, col: Int): String? {
                val world = Vector3(col - 3.5, 0.0, row - 3.5)
                val screen = camera.getScreenCoordinates(world)
                val ray = camera.getPickRay(Vector2(screen.x, screen.y), false, null)
                val results = PrimitivePickResults().apply { setCheckDistance(true) }
                PickingUtil.findPick(state.sceneRoot, ray, results)
                if (results.number == 0) return null
                return (results.getPickData(0).target as? Spatial)?.name
            }
            for ((row, col) in listOf(0 to 0, 7 to 7, 2 to 0, 3 to 3)) {
                assertEquals("click on ($row,$col) resolves to its tile",
                    "tile_${row}_$col", tileHitAt(row, col))
            }
            assertEquals("board present", "Checkers", checkers.name)
        } finally {
            canvas.close()
        }
    }

    private fun setupResourceLocators() {
        AWTImageLoader.registerLoader()
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MATERIAL, SimpleResourceLocator(
            ResourceLocatorTool.getClassPathResource(MaterialManager::class.java, "com/ardor3d/renderer/material")))
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_SHADER, SimpleResourceLocator(
            ResourceLocatorTool.getClassPathResource(MaterialManager::class.java, "com/ardor3d/renderer/shader")))
    }

    private fun skipOrFail(reason: String) {
        if ("1" == System.getenv("ARDOR3D_REQUIRE_GL_SMOKE")) {
            org.junit.Assert.fail("$reason - and ARDOR3D_REQUIRE_GL_SMOKE=1 requires this test to run")
        }
        Assume.assumeTrue(reason, false)
    }
}
