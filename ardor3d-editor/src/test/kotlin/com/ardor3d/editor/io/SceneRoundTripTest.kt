/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.editor.io

import com.ardor3d.bounding.BoundingBox
import com.ardor3d.editor.util.CameraObjectUtil
import com.ardor3d.light.PointLight
import com.ardor3d.math.ColorRGBA
import com.ardor3d.math.Vector3
import com.ardor3d.renderer.Camera
import com.ardor3d.renderer.state.RenderState
import com.ardor3d.renderer.state.WireframeState
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.extension.CameraNode
import com.ardor3d.scenegraph.shape.Box
import com.ardor3d.surface.ColorSurface
import com.ardor3d.util.export.binary.BinaryExporter
import com.ardor3d.util.export.binary.BinaryImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Verifies that the scene content the editor writes (File > Save) survives a binary round trip:
 * hierarchy, transforms, lights, ColorSurface material properties and render states.
 */
class SceneRoundTripTest {

    @Test
    fun editorSceneContentSurvivesBinaryRoundTrip() {
        val root = Node("Scene Root")

        val box = Box("Crate", Vector3.ZERO, 0.5, 0.5, 0.5)
        box.setModelBound(BoundingBox())
        box.setTranslation(1.0, 2.0, 3.0)
        val surface = ColorSurface()
        surface.diffuse = ColorRGBA(0.9f, 0.1f, 0.2f, 1f)
        box.setProperty(ColorSurface.DefaultPropertyKey, surface)
        val wire = WireframeState()
        wire.isEnabled = true
        box.setRenderState(wire)
        root.attachChild(box)

        val group = Node("Group")
        group.setTranslation(-4.0, 0.0, 0.0)
        root.attachChild(group)

        val light = PointLight()
        light.name = "Main Light"
        light.intensity = 0.75f
        light.setTranslation(5.0, 8.0, 5.0)
        root.attachChild(light)

        val cameraObject = Camera(100, 100)
        CameraObjectUtil.setPerspective(cameraObject, 35.0, 1.6, 0.5, 250.0)
        cameraObject.setLocation(2.0, 3.0, 9.0)
        cameraObject.lookAt(0.0, 0.0, 0.0, Vector3.UNIT_Y)
        val cameraNode = CameraNode("Main Camera", cameraObject)
        cameraNode.updateFromCamera()
        root.attachChild(cameraNode)

        val bytes = ByteArrayOutputStream()
        BinaryExporter().save(root, bytes)
        val loaded = BinaryImporter().load(ByteArrayInputStream(bytes.toByteArray())) as Node

        assertEquals("Scene Root", loaded.name)
        assertEquals(4, loaded.numberOfChildren)

        val loadedBox = loaded.getChild("Crate") as Box
        assertEquals(1.0, loadedBox.translation.x, 0.0)
        assertEquals(2.0, loadedBox.translation.y, 0.0)
        assertEquals(3.0, loadedBox.translation.z, 0.0)
        assertNotNull("model bound should survive", loadedBox.modelBound)

        val loadedSurface =
            loadedBox.getProperty<ColorSurface>(ColorSurface.DefaultPropertyKey, null)
        assertNotNull("ColorSurface property should survive", loadedSurface)
        assertEquals(0.9f, loadedSurface!!.diffuse.red, 1e-6f)
        assertEquals(0.1f, loadedSurface.diffuse.green, 1e-6f)

        val loadedWire = loadedBox.getLocalRenderState(RenderState.StateType.Wireframe) as? WireframeState
        assertNotNull("WireframeState should survive", loadedWire)
        assertTrue(loadedWire!!.isEnabled)

        val loadedLight = loaded.getChild("Main Light") as PointLight
        assertEquals(0.75f, loadedLight.intensity, 1e-6f)
        assertEquals(5.0, loadedLight.translation.x, 0.0)

        val loadedGroup = loaded.getChild("Group") as Node
        assertEquals(-4.0, loadedGroup.translation.x, 0.0)

        // Camera object: the CameraNode and its managed camera's frustum survive. Field of view is
        // recovered from the frustum planes, since Camera.write() does not persist the fovY value.
        val loadedCamera = loaded.getChild("Main Camera") as CameraNode
        val loadedCam = loadedCamera.camera
        assertNotNull("managed camera should survive", loadedCam)
        assertEquals(0.5, loadedCam!!.frustumNear, 1e-6)
        assertEquals(250.0, loadedCam.frustumFar, 1e-6)
        assertEquals(35.0, CameraObjectUtil.fovYDegrees(loadedCam), 1e-6)
        assertEquals(1.6, CameraObjectUtil.aspect(loadedCam), 1e-6)
    }
}
