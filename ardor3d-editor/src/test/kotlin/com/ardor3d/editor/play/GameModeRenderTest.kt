/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.play

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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test

/**
 * End-to-end check that a [GameMode] set on the editor is driven by the play boundary: onStart when
 * Play begins (with a [GameContext] over the live play scene and the render camera), update every
 * play frame, onStop when Stop ends - and that whatever the game did to the scene is discarded on
 * Stop by the snapshot restore. Runs the real [EditorScene] through a headless GLFW canvas, the only
 * place the play loop can be exercised; SKIPS when no GL context is available (set
 * ARDOR3D_REQUIRE_GL_SMOKE=1 to turn the skip into a failure), like the other editor GL tests.
 */
class GameModeRenderTest {

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

    /** A GameMode that records its lifecycle and mutates the play scene in onStart. */
    private class RecordingGameMode : GameMode {
        var starts = 0
        var updates = 0
        var stops = 0
        var lastTpf = 0.0
        var startContext: GameContext? = null

        override fun onStart(context: GameContext) {
            starts++
            startContext = context
            context.sceneRoot.attachChild(Node("GameMarker"))
        }

        override fun update(timePerFrame: Double, input: GameInput) {
            updates++
            lastTpf = timePerFrame
            assertNotNull("update always gets an input", input)
        }

        override fun onStop() {
            stops++
        }
    }

    @Test
    fun playDrivesTheGameModeLifecycleAndDiscardsItsSceneChanges() {
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

            // Warm-up frame sets up the editor camera and the game input controller.
            canvas.draw(null)

            scene.addCamera()
            val game = RecordingGameMode()
            scene.activeGameMode = game
            val preplayChildCount = state.sceneRoot.numberOfChildren
            val preplayRoot = state.sceneRoot

            // Enter play: onStart fires once, with a context over the live scene + a camera, and its
            // scene mutation is applied.
            scene.enterPlayMode()
            assertEquals("onStart called once", 1, game.starts)
            assertNotNull("context provided to onStart", game.startContext)
            assertSame("context exposes the live play scene", preplayRoot, game.startContext!!.sceneRoot)
            assertNotNull("context exposes a camera", game.startContext!!.camera)
            assertEquals("onStart's scene mutation is applied to the live scene",
                preplayChildCount + 1, state.sceneRoot.numberOfChildren)

            // Two play frames: update fires once each, with the timer's tpf; no stop yet.
            scene.update(TIMER)
            scene.update(TIMER)
            assertEquals("update called once per play frame", 2, game.updates)
            assertEquals("update gets the frame delta", 0.05, game.lastTpf, 1e-9)
            assertEquals("no stop while playing", 0, game.stops)

            // Stop: deferred, so drain with one update. onStop fires once and the game's scene
            // change is gone (snapshot restored the pre-play scene).
            scene.exitPlayMode()
            scene.update(TIMER)
            assertFalse("no longer playing", state.playing)
            assertEquals("onStop called once", 1, game.stops)
            assertEquals("update not called on the stop frame", 2, game.updates)
            assertEquals("the game's scene mutation is discarded on stop",
                preplayChildCount, state.sceneRoot.numberOfChildren)
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
