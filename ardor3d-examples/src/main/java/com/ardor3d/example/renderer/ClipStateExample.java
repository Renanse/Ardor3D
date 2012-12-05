/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * Illustrates the ClipState class; which specifies a plane to test for clipping of a Node.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.ClipStateExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_ClipStateExample.jpg", //
maxHeapMemory = 64)
public class ClipStateExample extends ExampleBase {

    public static void main(final String[] args) {
        start(ClipStateExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {

    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Ardor3D: ClipState Example");

        _canvas.getCanvasRenderer().getCamera().setLocation(20, 40, 80);
        _canvas.getCanvasRenderer().getCamera().lookAt(0, 0, 0, Vector3.UNIT_Y);

        final Quad floor = new Quad("floor", 100, 100);
        floor.setRotation(new Matrix3().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.UNIT_X));
        _root.attachChild(floor);

        final Teapot teapot = new Teapot("teapot");
        teapot.setScale(5);
        _root.attachChild(teapot);

        // Add a clip state to the scene.
        final ClipState cs = new ClipState();
        cs.setEnableClipPlane(0, true);
        cs.setClipPlaneEquation(0, 1, 0, 0, 0);
        _root.setRenderState(cs);
    }
}
