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

import java.net.URISyntaxException;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.model.md2.Md2DataStore;
import com.ardor3d.extension.model.md2.Md2Importer;
import com.ardor3d.extension.model.util.KeyframeController;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.controller.ComplexSpatialController.RepeatType;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.resource.MultiFormatResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * Simplest example of loading a model in MD2 format.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.SimpleMd2Example", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_SimpleMd2Example.jpg", //
        maxHeapMemory = 64)
public class SimpleMd2Example extends ExampleBase {
    public static void main(final String[] args) {
        ExampleBase.start(SimpleMd2Example.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Simple Md2 Example");
        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.BLUE);
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 5, 20));

        // Load the scene
        final long time = System.currentTimeMillis();
        final Md2Importer importer = new Md2Importer();
        try {
            importer.setTextureLocator(
                    new MultiFormatResourceLocator(ResourceLocatorTool.getClassPathResource(SimpleObjExample.class,
                            "com/ardor3d/example/media/models/md2/"), ".dds", ".jpg", ".png", ".tga", ".pcx"));
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        final Md2DataStore storage = importer.load("md2/drfreak.md2");
        System.out.println("Importing Took " + (System.currentTimeMillis() - time) + " ms");

        final Mesh model = storage.getScene();
        // md2 models are usually z-up - switch to y-up
        model.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_X));
        // attack to root
        _root.attachChild(model);
        _root.updateWorldRenderStates(true);
        MaterialUtil.autoMaterials(_root);

        // speed us up a little
        final KeyframeController<Mesh> controller = storage.getController();
        controller.setSpeed(8);
        controller.setRepeatType(RepeatType.WRAP);
    }
}
