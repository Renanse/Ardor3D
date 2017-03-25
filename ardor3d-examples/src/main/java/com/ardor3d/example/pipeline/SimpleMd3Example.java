/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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
import com.ardor3d.extension.model.md3.Md3DataStore;
import com.ardor3d.extension.model.md3.Md3Importer;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Node;

/**
 * Simplest example of loading a model in MD3 format. FIXME update the description and the thumbnail
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.SimpleMd2Example", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_SimpleMd2Example.jpg", //
maxHeapMemory = 64)
public class SimpleMd3Example extends ExampleBase {
    public static void main(final String[] args) {
        ExampleBase.start(SimpleMd3Example.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Simple Md3 Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 5, 20));

        // Load the scene
        final long time = System.currentTimeMillis();
        final Md3Importer importer = new Md3Importer();
        // try {
        // importer.setTextureLocator(new MultiFormatResourceLocator(ResourceLocatorTool.getClassPathResource(
        // SimpleObjExample.class, "com/ardor3d/example/media/models/md2/"), ".dds", ".jpg", ".png", ".tga",
        // ".pcx"));
        // } catch (final URISyntaxException ex) {
        // ex.printStackTrace();
        // }

        final Md3DataStore storage = importer.load("md3/barrel1.md3");
        System.out.println("Importing Took " + (System.currentTimeMillis() - time) + " ms");

        final Node model = storage.getScene();
        // md2 models are usually z-up - switch to y-up
        model.setRotation(new Quaternion().fromAngleAxis(-MathUtils.HALF_PI, Vector3.UNIT_X));
        // attack to root
        _root.attachChild(model);

        // speed us up a little
        // final KeyframeController<Mesh> controller = storage.getController();
        // controller.setSpeed(8);
        // controller.setRepeatType(RepeatType.WRAP);
    }
}
