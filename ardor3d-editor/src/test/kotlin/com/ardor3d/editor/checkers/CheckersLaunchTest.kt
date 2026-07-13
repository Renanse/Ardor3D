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
import com.ardor3d.renderer.material.MaterialManager
import com.ardor3d.scenegraph.Node
import com.ardor3d.util.ReadOnlyTimer
import com.ardor3d.util.resource.ResourceLocatorTool
import com.ardor3d.util.resource.SimpleResourceLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

/**
 * End-to-end launch of checkers in the editor: Game > Play Checkers adds a camera if needed, enters
 * play, and starts the game (board attached, status reported); Stop discards the board (it was added
 * after the snapshot) and clears the game. Runs the real [EditorScene] through a headless GLFW
 * canvas; SKIPS without a GL context (ARDOR3D_REQUIRE_GL_SMOKE=1 turns the skip into a failure).
 */
class CheckersLaunchTest {

    private companion object {
        val TIMER: ReadOnlyTimer = object : ReadOnlyTimer {
            override fun getTimeInSeconds() = 0.0
            override fun getTime() = 0L
            override fun getResolution() = 1_000_000_000L
            override fun getFrameRate() = 20.0
            override fun getTimePerFrame() = 0.05
            override fun getPreviousFrameTime() = 0L
        }
    }

    private fun checkersNode(root: Node): Node? = root.children.firstOrNull { it.name == "Checkers" } as? Node

    @Test
    fun playCheckersStartsAndStopDiscardsIt() {
        setupResourceLocators()

        val state = EditorState()
        val scene = EditorScene(state)
        val canvasRenderer = Lwjgl3CanvasRenderer(scene)

        var canvas: GLFWCanvas? = null
        try {
            canvas = GLFWCanvas(DisplaySettings(320, 240, 24, 0), canvasRenderer)
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
            canvas.draw(null) // warm-up: editor camera + input controller

            // Launch. No camera in the default scene, so startCheckers should add one, then play.
            scene.startCheckers()
            assertTrue("entered play", state.playing)

            val checkers = checkersNode(state.sceneRoot)
            assertNotNull("the checkers board was added to the play scene", checkers)
            val pieces = checkers!!.children.first { it.name == "Pieces" } as Node
            assertEquals("opening lays out 24 pieces", 24, pieces.numberOfChildren)
            assertEquals("status is surfaced", "Red to move", state.gameStatus)

            // Stop: the board (added after the snapshot) is discarded and the game is cleared.
            scene.exitPlayMode()
            scene.update(TIMER)
            assertFalse("no longer playing", state.playing)
            assertNull("checkers board discarded on stop", checkersNode(state.sceneRoot))
            assertNull("status cleared", state.gameStatus)
            assertNull("active game cleared (each launch is one game)", scene.activeGameMode)
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
