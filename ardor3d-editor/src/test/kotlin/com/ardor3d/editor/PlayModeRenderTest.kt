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

import com.ardor3d.editor.util.CameraObjectUtil
import com.ardor3d.framework.DisplaySettings
import com.ardor3d.framework.lwjgl3.GLFWCanvas
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer
import com.ardor3d.image.util.awt.AWTImageLoader
import com.ardor3d.input.PhysicalLayer
import com.ardor3d.input.logical.LogicalLayer
import com.ardor3d.renderer.material.MaterialManager
import com.ardor3d.scenegraph.extension.CameraNode
import com.ardor3d.util.ReadOnlyTimer
import com.ardor3d.util.resource.ResourceLocatorTool
import com.ardor3d.util.resource.SimpleResourceLocator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

/**
 * End-to-end check that entering play mode actually drives the viewport's render camera from a
 * camera object, and leaving it restores the editor fly camera. It runs the real [EditorScene]
 * through a headless GLFW canvas - the only place [EditorScene.enterPlayMode] /
 * [EditorScene.syncPlayCamera] can be exercised, since they read the live render camera and
 * viewport size.
 *
 * Like the readout smoke test it SKIPS when no GL context is available, so it never breaks builds
 * over infrastructure; set ARDOR3D_REQUIRE_GL_SMOKE=1 (as CI does, under Xvfb + llvmpipe) to turn
 * the skip into a failure.
 */
class PlayModeRenderTest {

    private companion object {
        const val W = 320
        const val H = 240
        val TIMER: ReadOnlyTimer = object : ReadOnlyTimer {
            override fun getTimeInSeconds() = 0.0
            override fun getTime() = 0L
            override fun getResolution() = 1_000_000_000L
            override fun getFrameRate() = 20.0
            override fun getTimePerFrame() = 0.05
            override fun getPreviousFrameTime() = 0L
        }
    }

    @Test
    fun playModeDrivesRenderCameraFromCameraObjectAndRestoresOnStop() {
        setupResourceLocators()

        val state = EditorState()
        val scene = EditorScene(state)
        val canvasRenderer = Lwjgl3CanvasRenderer(scene)

        var canvas: GLFWCanvas? = null
        try {
            canvas = GLFWCanvas(DisplaySettings(W, H, 24, 0), canvasRenderer)
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

            // Warm-up frame sets up the editor fly camera (location 8,6,12 looking at the origin).
            canvas.draw(null)
            val render = canvasRenderer.camera
            val editorLocation = com.ardor3d.math.Vector3(render.location)

            // Add a camera object, give it a distinctive frustum and a distinctive pose so the
            // assertions can't accidentally match the editor camera.
            scene.addCamera()
            val cameraNode = state.primarySelection as CameraNode
            val gameCam = cameraNode.camera
            assertNotNull("camera object should carry a managed camera", gameCam)
            CameraObjectUtil.setPerspective(gameCam!!, 30.0, 1.0, 0.2, 500.0)
            cameraNode.setTranslation(0.0, 2.0, 20.0)

            // The pre-play scene, for the discard-on-stop check below.
            val preplayChildCount = state.sceneRoot.numberOfChildren

            // Enter play mode and run one frame: syncPlayCamera should copy the camera object's
            // pose and field of view onto the render camera, fitted to the viewport aspect.
            scene.enterPlayMode()
            assertTrue("editor should report playing", state.playing)
            scene.update(TIMER)

            assertEquals("render camera adopts the camera object's field of view",
                30.0, CameraObjectUtil.fovYDegrees(render), 1e-3)
            assertEquals("render camera uses the viewport aspect",
                W.toDouble() / H.toDouble(), CameraObjectUtil.aspect(render), 1e-3)
            assertEquals("render camera moves to the camera object's position (x)",
                0.0, render.location.x, 1e-6)
            assertEquals("render camera moves to the camera object's position (y)",
                2.0, render.location.y, 1e-6)
            assertEquals("render camera moves to the camera object's position (z)",
                20.0, render.location.z, 1e-6)

            // Simulate a play-mode mutation of the live scene (a game would do this). It must not
            // survive Stop.
            state.sceneRoot.attachChild(com.ardor3d.scenegraph.Node("PlayMutation"))
            assertEquals("play-mode mutation is applied to the live scene",
                preplayChildCount + 1, state.sceneRoot.numberOfChildren)

            // Stop: the document swap is deferred, so drain it with one update. The editor fly
            // camera's pose is restored exactly, and the play-mode mutation is gone.
            scene.exitPlayMode()
            scene.update(TIMER)
            assertFalse("editor should report not playing", state.playing)
            assertEquals("play-mode mutation is discarded on stop",
                preplayChildCount, state.sceneRoot.numberOfChildren)
            assertEquals(editorLocation.x, render.location.x, 1e-6)
            assertEquals(editorLocation.y, render.location.y, 1e-6)
            assertEquals(editorLocation.z, render.location.z, 1e-6)
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
