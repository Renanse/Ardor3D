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

import com.ardor3d.bounding.BoundingBox
import com.ardor3d.buffer.BufferUtils
import com.ardor3d.extension.interact.InteractManager
import com.ardor3d.extension.interact.widget.gizmo.AbstractGizmo
import com.ardor3d.framework.DisplaySettings
import com.ardor3d.framework.Scene
import com.ardor3d.framework.lwjgl3.GLFWCanvas
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer
import com.ardor3d.image.ImageDataFormat
import com.ardor3d.image.util.awt.AWTImageLoader
import com.ardor3d.input.InputState
import com.ardor3d.input.PhysicalLayer
import com.ardor3d.input.character.CharacterInputState
import com.ardor3d.input.controller.ControllerState
import com.ardor3d.input.gesture.GestureState
import com.ardor3d.input.keyboard.KeyboardState
import com.ardor3d.input.logical.LogicalLayer
import com.ardor3d.input.logical.TwoInputStates
import com.ardor3d.input.mouse.ButtonState
import com.ardor3d.input.mouse.MouseButton
import com.ardor3d.input.mouse.MouseState
import com.ardor3d.intersection.PickResults
import com.ardor3d.math.Ray3
import com.ardor3d.math.Vector3
import com.ardor3d.math.type.ReadOnlyVector3
import com.ardor3d.renderer.Renderer
import com.ardor3d.renderer.material.MaterialManager
import com.ardor3d.scenegraph.shape.Box
import com.ardor3d.util.ReadOnlyTimer
import com.ardor3d.util.resource.ResourceLocatorTool
import com.ardor3d.util.resource.SimpleResourceLocator
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import java.nio.ByteBuffer
import java.util.EnumMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO

/**
 * GL smoke test for the editor's transform-gizmo drag readout. It drives the real [EditorScene]
 * through a headless GLFW canvas, selects a box, scripts a drag of the +X translate arrow, and reads
 * the framebuffer back to assert the readout actually rasterizes - the on-screen ortho pass this
 * scene adds ([EditorScene.render]) is the first place the readout is drawn to pixels anywhere, so
 * nothing else covers it. It also confirms the axis-aware editor formatter is wired: a +X drag reads
 * out as "X ...", not the gizmo library's default "+dx, +dy, +dz" triple.
 *
 * Like the gizmo-library GL smoke test this SKIPS when no usable GL context or framebuffer readback
 * is available (e.g. WSLg's D3D12 driver returns zeros from default-framebuffer readback), so it
 * cannot break builds over infrastructure. Set ARDOR3D_REQUIRE_GL_SMOKE=1 (as CI does, under Xvfb +
 * llvmpipe) to turn those skips into failures. Pass -Dreadout.shot.dir=<dir> to also dump the
 * captured frame as a PNG for eyeballing.
 */
class EditorReadoutRenderSmokeTest {

    private companion object {
        const val W = 480
        const val H = 320
        const val FRAMES = 10
        const val DRAG_START = 4
        const val BEFORE_FRAME = 2 // gizmo shown, no drag yet: baseline with no readout
        val CLEAR = intArrayOf(38, 41, 51) // EditorScene background (0.15, 0.16, 0.2) * 255

        val TIMER: ReadOnlyTimer = object : ReadOnlyTimer {
            override fun getTimeInSeconds() = 0.0
            override fun getTime() = 0L
            override fun getResolution() = 1_000_000_000L
            override fun getFrameRate() = 20.0
            override fun getTimePerFrame() = 0.05
            override fun getPreviousFrameTime() = 0L
        }
    }

    /** Wraps the editor scene so the framebuffer can be grabbed inside render() - context current,
     * before the buffer swap - which is the only point a readback sees the finished frame. */
    private class Capturer(val inner: EditorScene) : Scene {
        var grab: ((Renderer) -> Unit)? = null
        override fun render(renderer: Renderer): Boolean {
            inner.render(renderer)
            grab?.invoke(renderer)
            return true
        }
        override fun doPick(pickRay: Ray3): PickResults = inner.doPick(pickRay)
    }

    private var lastDragMouse: MouseState? = null

    @Test
    fun readoutRendersDuringATranslateDrag() {
        setupResourceLocators()

        val state = EditorState()
        val scene = EditorScene(state)
        val capturer = Capturer(scene)
        val canvasRenderer = Lwjgl3CanvasRenderer(capturer)

        var canvas: GLFWCanvas? = null
        try {
            canvas = GLFWCanvas(DisplaySettings(W, H, 24, 0), canvasRenderer)
            canvas.init()
        } catch (t: Throwable) {
            canvas?.close() // constructed but init() failed - release it before we bail
            skipOrFail("Could not create a GL context here: $t")
            return
        }

        try {
            scene.canvasRenderer = canvasRenderer
            scene.canvas = canvas
            scene.setupInteractManager(canvas, PhysicalLayer.Builder().build(), LogicalLayer())

            // Select a box at the origin and stay in the default translate mode.
            val box = Box("DragMe", Vector3.ZERO, 0.5, 0.5, 0.5).apply {
                modelBound = BoundingBox()
                updateModelBound()
            }
            state.sceneRoot.attachChild(box)
            state.select(box)
            state.transformMode = TransformMode.TRANSLATE

            val latest = arrayOfNulls<ByteBuffer>(1)
            capturer.grab = { r ->
                val buf = BufferUtils.createByteBuffer(W * H * 3)
                r.grabScreenContents(buf, ImageDataFormat.RGB, 0, 0, W, H)
                latest[0] = buf
            }

            // Warm-up draw sets up the camera, background and scene registration; one update() then
            // syncs the gizmo target (the selected box) and the active widget. After that we drive
            // the drag directly and do NOT call update() again: its input-trigger pass feeds the
            // dummy physical layer's button-up state, which would end the drag between frames.
            canvas.draw(null)
            scene.update(TIMER)

            val mgr = scene.interactManagerForTest!!
            var bandBefore = -1
            var bgPixels = 0
            for (frame in 0 until FRAMES) {
                if (frame >= DRAG_START) {
                    dragXArrow(mgr, canvas, box, frame - DRAG_START)
                }
                canvas.draw(null)
                if (frame == BEFORE_FRAME) {
                    bandBefore = readoutBandBright(latest[0]!!) // gizmo only, no readout yet
                    bgPixels = countBackground(latest[0]!!)
                }
            }

            val glRenderer = org.lwjgl.opengl.GL11C.glGetString(org.lwjgl.opengl.GL11C.GL_RENDERER)
            if (bgPixels < W * H / 2) {
                skipOrFail("Framebuffer readback unusable on '$glRenderer' ($bgPixels/${W * H} background pixels)")
                return
            }

            val bandDuring = readoutBandBright(latest[0]!!)
            val readoutText = (mgr.activeWidget as AbstractGizmo).readout.text
            System.out.println("EDITOR READOUT SMOKE on '$glRenderer'" +
                " bandBefore=$bandBefore bandDuring=$bandDuring readout='$readoutText'")

            val shotDir = System.getProperty("readout.shot.dir") ?: System.getenv("READOUT_SHOT_DIR")
            if (shotDir != null) {
                writePng(latest[0]!!, File(shotDir, "editor-readout.png"))
            }

            // Assert the readout rasterizes and is axis-aware. We count bright pixels in the readout's
            // top-center band rather than exact-match white: the outlined, anti-aliased glyphs blend
            // toward the dark background at their edges, so only the glyph cores are pure white.
            val on = " on '$glRenderer'"
            assertTrue("readout should be axis-aware ('X ...'), was '$readoutText'$on",
                readoutText != null && readoutText.startsWith("X "))
            assertTrue("readout did not rasterize in its screen band (before=$bandBefore during=$bandDuring)$on",
                bandDuring > bandBefore + 40)
        } finally {
            canvas.close()
        }
    }

    /** One frame of a held left-button drag of the +X translate arrow, walked outward along X. */
    private fun dragXArrow(manager: InteractManager, canvas: GLFWCanvas, box: Box, step: Int) {
        val gizmo = manager.activeWidget as AbstractGizmo
        val cam = canvas.canvasRenderer.camera
        val handleScale = gizmo.handle.scale.x
        // Start mid-shaft on the +X arrow (so the pick grabs AxisX), then walk outward.
        val dist = (0.6 + step * 0.15) * handleScale
        val onAxis = Vector3(dist, 0.0, 0.0).addLocal(box.worldTranslation)
        val screen = cam.getScreenCoordinates(onAxis)
        applyDrag(gizmo, screen, canvas, manager, box)
    }

    private fun applyDrag(gizmo: AbstractGizmo, screen: ReadOnlyVector3, canvas: GLFWCanvas,
                          manager: InteractManager, box: Box) {
        val x = screen.x.toInt()
        val y = screen.y.toInt()
        val buttons = EnumMap<MouseButton, ButtonState>(MouseButton::class.java)
        buttons[MouseButton.LEFT] = ButtonState.DOWN
        // First frame's previous state has the button up at the same spot, starting the drag there.
        val previous = lastDragMouse ?: MouseState(x, y, 0, 0, 0, null, null)
        val current = MouseState(x, y, x - previous.x, y - previous.y, 0, buttons, null)
        lastDragMouse = current

        val prevState = InputState(KeyboardState.NOTHING, previous, ControllerState.NOTHING,
            GestureState.NOTHING, CharacterInputState.NOTHING)
        val curState = InputState(KeyboardState.NOTHING, current, ControllerState.NOTHING,
            GestureState.NOTHING, CharacterInputState.NOTHING)
        gizmo.processInput(canvas, TwoInputStates(prevState, curState), AtomicBoolean(false), manager)
        manager.spatialState.applyState(box)
    }

    /**
     * Count bright (channel sum > 500) pixels in the top band where the readout floats - above the
     * gizmo's own footprint, so its gray center handle and saturated axis colors don't register.
     * The readout's white outlined glyphs are the only bright thing up there. yTop is from the top.
     */
    private fun readoutBandBright(buf: ByteBuffer): Int {
        var n = 0
        for (yTop in 15 until 110) {
            val row = H - 1 - yTop
            for (col in 0 until W) {
                val i = (row * W + col) * 3
                val sum = (buf.get(i).toInt() and 0xFF) + (buf.get(i + 1).toInt() and 0xFF) +
                    (buf.get(i + 2).toInt() and 0xFF)
                if (sum > 500) n++
            }
        }
        return n
    }

    private fun countBackground(buf: ByteBuffer): Int {
        var n = 0
        for (i in 0 until W * H) {
            val r = buf.get(i * 3).toInt() and 0xFF
            val g = buf.get(i * 3 + 1).toInt() and 0xFF
            val b = buf.get(i * 3 + 2).toInt() and 0xFF
            if (Math.abs(r - CLEAR[0]) < 25 && Math.abs(g - CLEAR[1]) < 25 && Math.abs(b - CLEAR[2]) < 25) n++
        }
        return n
    }

    /** Writes the RGB readback (GL origin bottom-left) to a PNG, flipped to top-left rows. */
    private fun writePng(buf: ByteBuffer, file: File) {
        val img = BufferedImage(W, H, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until H) {
            val srcRow = H - 1 - y
            for (x in 0 until W) {
                val i = (srcRow * W + x) * 3
                val r = buf.get(i).toInt() and 0xFF
                val g = buf.get(i + 1).toInt() and 0xFF
                val b = buf.get(i + 2).toInt() and 0xFF
                img.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
        }
        file.parentFile?.mkdirs()
        ImageIO.write(img, "png", file)
    }

    private fun setupResourceLocators() {
        // Register the AWT/ImageIO image loader so the readout's PNG font atlas decodes (as the
        // editor itself does) - without it the text falls back to the magenta missing-texture image.
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
