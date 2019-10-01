/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
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
import com.ardor3d.extension.model.stl.StlDataStore;
import com.ardor3d.extension.model.stl.StlImporter;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Simplest example of loading a stereolithography STL file.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.pipeline.SimpleStlExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_SimpleStlExample.jpg", //
        maxHeapMemory = 64)
public class SimpleStlExample extends ExampleBase {
    public static void main(final String[] args) {
        ExampleBase.start(SimpleStlExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D - Simple Stl Example");
        _canvas.getCanvasRenderer().getCamera().setLocation(new Vector3(0, 5, 20));

        // Load the stl scene
        final long time = System.currentTimeMillis();
        final StlImporter importer = new StlImporter();
        final StlDataStore storage = importer.load("stl/face_milling_cutter.stl");
        System.out.println("Importing Took " + (System.currentTimeMillis() - time) + " ms");

        _root.attachChild(storage.getScene());
        _root.getChild(0).setScale(.2);
    }

    /** Rotation matrix for the spinning box. */
    private final Matrix3 rotate = new Matrix3();

    /** Angle of rotation for the box. */
    private double angle = 0;

    /** Axis to rotate the box around. */
    private final Vector3 axis = new Vector3(0, 1, 0f).normalizeLocal();

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        // Update the angle using the current tpf to rotate at a constant speed.
        angle += timer.getTimePerFrame() * 10;
        // Wrap the angle to keep it inside 0-360 range
        angle %= 360;

        // Update the rotation matrix using the angle and rotation axis.
        rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
        // Update the box rotation using the rotation matrix.
        _root.setRotation(rotate);
    }

}