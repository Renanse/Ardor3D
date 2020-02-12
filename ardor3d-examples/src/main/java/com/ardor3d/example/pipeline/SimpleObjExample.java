/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.pipeline;

import java.net.URISyntaxException;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.model.obj.ObjGeometryStore;
import com.ardor3d.extension.model.obj.ObjImporter;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * Simplest example of loading a Wavefront OBJ model.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.SimpleObjExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_SimpleObjExample.jpg", //
        maxHeapMemory = 64)
public class SimpleObjExample extends ExampleBase {
    public static void main(final String[] args) {
        ExampleBase.start(SimpleObjExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Simple Obj Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 5, 20));

        // Load the collada scene
        final long time = System.currentTimeMillis();
        final ObjImporter importer = new ObjImporter();
        try {
            importer.setTextureLocator(new SimpleResourceLocator(ResourceLocatorTool
                    .getClassPathResource(SimpleObjExample.class, "com/ardor3d/example/media/models/obj/")));
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }
        final ObjGeometryStore storage = importer.load("obj/pitcher.obj");
        System.out.println("Importing Took " + (System.currentTimeMillis() - time) + " ms");

        _root.attachChild(storage.getScene());
        _root.updateWorldRenderStates(true);
        MaterialUtil.autoMaterials(_root);
    }
}