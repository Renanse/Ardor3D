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

import com.ardor3d.framework.DisplaySettings
import com.ardor3d.framework.lwjgl3.GLFWCanvas
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer
import com.ardor3d.image.util.awt.AWTImageLoader
import com.ardor3d.renderer.material.MaterialManager
import com.ardor3d.util.ReadOnlyTimer
import com.ardor3d.util.resource.ResourceLocatorTool
import com.ardor3d.util.resource.SimpleResourceLocator
import org.junit.Assert
import org.junit.Assume

/**
 * Shared fixtures for the editor's GL smoke tests: the fixed-step timer, resource-locator setup,
 * the skip-without-GL policy (ARDOR3D_REQUIRE_GL_SMOKE=1 turns skips into failures, as CI does
 * under Xvfb + software GL), and headless canvas creation.
 */
internal object EditorGlTestSupport {

    /** A fixed-step timer for driving [EditorScene.update] deterministically (20 fps, tpf 0.05). */
    val TIMER: ReadOnlyTimer = object : ReadOnlyTimer {
        override fun getTimeInSeconds() = 0.0
        override fun getTime() = 0L
        override fun getResolution() = 1_000_000_000L
        override fun getFrameRate() = 20.0
        override fun getTimePerFrame() = 0.05
        override fun getPreviousFrameTime() = 0L
    }

    /** Registers the PNG/JPG image loader and the material/shader locators every GL test needs. */
    fun setupResourceLocators() {
        AWTImageLoader.registerLoader()
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MATERIAL, SimpleResourceLocator(
            ResourceLocatorTool.getClassPathResource(MaterialManager::class.java, "com/ardor3d/renderer/material")))
        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_SHADER, SimpleResourceLocator(
            ResourceLocatorTool.getClassPathResource(MaterialManager::class.java, "com/ardor3d/renderer/shader")))
    }

    /** Skips the calling test for [reason] - or fails it when ARDOR3D_REQUIRE_GL_SMOKE=1. */
    fun skipOrFail(reason: String) {
        if ("1" == System.getenv("ARDOR3D_REQUIRE_GL_SMOKE")) {
            Assert.fail("$reason - and ARDOR3D_REQUIRE_GL_SMOKE=1 requires this test to run")
        }
        Assume.assumeTrue(reason, false)
    }

    /**
     * Creates and initializes a headless GLFW canvas, or ends the calling test via [skipOrFail]
     * when no GL context can be created here. The caller owns closing the returned canvas.
     */
    fun initCanvasOrSkip(
        canvasRenderer: Lwjgl3CanvasRenderer, width: Int = 320, height: Int = 240
    ): GLFWCanvas {
        var canvas: GLFWCanvas? = null
        try {
            canvas = GLFWCanvas(DisplaySettings(width, height, 24, 0), canvasRenderer)
            canvas.init()
            return canvas
        } catch (t: Throwable) {
            canvas?.close()
            skipOrFail("Could not create a GL context here: $t")
            throw AssertionError("unreachable - skipOrFail always throws")
        }
    }
}
