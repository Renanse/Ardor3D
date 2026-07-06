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
import com.ardor3d.extension.model.collada.jdom.ColladaImporter
import com.ardor3d.extension.model.obj.ObjImporter
import com.ardor3d.scenegraph.Mesh
import com.ardor3d.scenegraph.Node
import com.ardor3d.util.resource.SimpleResourceLocator
import com.ardor3d.util.resource.URLResourceSource
import java.io.File

/**
 * Loads model files into editor-ready scene subtrees. Pure loading and preparation - no GL
 * context needed; the caller assigns render materials and attaches the result to the document.
 */
object ModelImport {

    /** File extensions (lower case, no dot) accepted by [load]. */
    val supportedExtensions: List<String> = listOf("obj", "dae")

    /**
     * Loads [file] by extension (Wavefront OBJ or COLLADA), returning a subtree named after
     * the file, with model bounds ensured on every mesh so picking works.
     */
    fun load(file: File): Node {
        val scene = when (file.extension.lowercase()) {
            "obj" -> loadObj(file)
            "dae" -> loadCollada(file)
            else -> throw IllegalArgumentException(
                "Unsupported model format '${file.extension}' (expected one of $supportedExtensions)"
            )
        }
        scene.name = file.name.substringBeforeLast('.')
        scene.acceptVisitor({ spatial ->
            if (spatial is Mesh && spatial.modelBound == null) {
                spatial.setModelBound(BoundingBox())
            }
        }, false)
        return scene
    }

    private fun loadObj(file: File): Node {
        val importer = ObjImporter().setLoadTextures(true)
        file.parentFile?.let { importer.setTextureLocator(SimpleResourceLocator(it.toURI())) }
        return importer.load(URLResourceSource(file.toURI().toURL())).scene
    }

    private fun loadCollada(file: File): Node {
        val importer = ColladaImporter().setLoadTextures(true)
        file.parentFile?.let { dir ->
            val locator = SimpleResourceLocator(dir.toURI())
            importer.setTextureLocator(locator)
            importer.setModelLocator(locator)
        }
        return importer.load(URLResourceSource(file.toURI().toURL())).scene
    }
}
