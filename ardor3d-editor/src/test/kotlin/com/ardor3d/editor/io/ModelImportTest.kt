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

import com.ardor3d.scenegraph.Mesh
import com.ardor3d.scenegraph.Node
import com.ardor3d.scenegraph.Spatial
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelImportTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun collectMeshes(spatial: Spatial, into: MutableList<Mesh>) {
        if (spatial is Mesh) {
            into.add(spatial)
        }
        if (spatial is Node) {
            spatial.children.forEach { collectMeshes(it, into) }
        }
    }

    private fun meshesOf(scene: Node): List<Mesh> = mutableListOf<Mesh>().also { collectMeshes(scene, it) }

    private fun writeFile(name: String, content: String): File =
        folder.newFile(name).apply { writeText(content) }

    @Test
    fun ensureModelBoundsReplacesTheDefaultInfiniteSphere() {
        // A new Mesh defaults to an infinite bounding sphere, not a null bound
        val box = com.ardor3d.scenegraph.shape.Box("box", com.ardor3d.math.Vector3.ZERO, 1.0, 1.0, 1.0)
        assertTrue((box.modelBound as com.ardor3d.bounding.BoundingSphere).radius.isInfinite())
        val scene = Node("scene").apply { attachChild(box) }

        ModelImport.ensureModelBounds(scene)

        val radius = (box.modelBound as com.ardor3d.bounding.BoundingSphere).radius
        assertTrue("bound radius should be finite after import, was $radius", radius.isFinite())
        assertTrue(radius > 0.0)
    }

    @Test
    fun loadsObjTriangle() {
        val file = writeFile(
            "triangle.obj",
            """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            f 1 2 3
            """.trimIndent()
        )

        val scene = ModelImport.load(file)

        assertEquals("triangle", scene.name)
        val meshes = meshesOf(scene)
        assertTrue("expected at least one mesh", meshes.isNotEmpty())
        assertEquals(3, meshes.sumOf { it.meshData.vertexCount })
        meshes.forEach { assertNotNull("mesh should have a bound for picking", it.modelBound) }
    }

    @Test
    fun loadsColladaTriangle() {
        val file = writeFile("triangle.dae", MINIMAL_TRIANGLE_DAE)

        val scene = ModelImport.load(file)

        assertEquals("triangle", scene.name)
        val meshes = meshesOf(scene)
        assertTrue("expected at least one mesh", meshes.isNotEmpty())
        assertEquals(3, meshes.sumOf { it.meshData.vertexCount })
        meshes.forEach { assertNotNull("mesh should have a bound for picking", it.modelBound) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownExtension() {
        ModelImport.load(writeFile("model.xyz", "nonsense"))
    }

    companion object {
        private val MINIMAL_TRIANGLE_DAE = """
            <?xml version="1.0" encoding="utf-8"?>
            <COLLADA xmlns="http://www.collada.org/2005/11/COLLADASchema" version="1.4.1">
              <asset><up_axis>Y_UP</up_axis></asset>
              <library_geometries>
                <geometry id="triGeom" name="tri">
                  <mesh>
                    <source id="positions">
                      <float_array id="positions-array" count="9">0 0 0 1 0 0 0 1 0</float_array>
                      <technique_common>
                        <accessor source="#positions-array" count="3" stride="3">
                          <param name="X" type="float"/>
                          <param name="Y" type="float"/>
                          <param name="Z" type="float"/>
                        </accessor>
                      </technique_common>
                    </source>
                    <vertices id="verts"><input semantic="POSITION" source="#positions"/></vertices>
                    <triangles count="1">
                      <input semantic="VERTEX" source="#verts" offset="0"/>
                      <p>0 1 2</p>
                    </triangles>
                  </mesh>
                </geometry>
              </library_geometries>
              <library_visual_scenes>
                <visual_scene id="scene">
                  <node id="triNode" name="triNode">
                    <instance_geometry url="#triGeom"/>
                  </node>
                </visual_scene>
              </library_visual_scenes>
              <scene><instance_visual_scene url="#scene"/></scene>
            </COLLADA>
        """.trimIndent()
    }
}
