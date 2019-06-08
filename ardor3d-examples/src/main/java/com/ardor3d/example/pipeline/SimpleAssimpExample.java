/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.pipeline;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.model.assimp.AssimpModelImporter;
import com.ardor3d.extension.model.assimp.ModelDataStore;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.MaterialUtil;

/**
 * Simplest example of loading a Wavefront OBJ model using Assimp.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.SimpleAssimpExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_SimpleAssimpExample.jpg", //
        maxHeapMemory = 64)
public class SimpleAssimpExample extends ExampleBase {
    public static void main(final String[] args) {
        ExampleBase.start(SimpleAssimpExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Simple Assimp Obj Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 5, 20));

        // Load the scene
        final long time = System.currentTimeMillis();
        final AssimpModelImporter importer = new AssimpModelImporter();
        final ModelDataStore storage = importer.load("obj/pitcher.obj");
        System.out.println("Importing Took " + (System.currentTimeMillis() - time) + " ms");

        _root.attachChild(storage.getScene());
        _root.updateWorldRenderStates(true);
        MaterialUtil.autoMaterials(_root);
    }
}