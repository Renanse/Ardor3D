/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.basic;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * A simple example showing a textured and lit box spinning.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.basic.BoxExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_BoxExample.jpg", //
        maxHeapMemory = 64)
public class BoxExample extends ExampleBase {

    /** Keep a reference to the box to be able to rotate it each frame. */
    private Mesh box;

    /** Rotation matrix for the spinning box. */
    private final Matrix3 rotate = new Matrix3();

    /** Angle of rotation for the box. */
    private double angle = 0;

    /** Axis to rotate the box around. */
    private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();

    public static void main(final String[] args) {
        start(BoxExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        // Update the angle using the current tpf to rotate at a constant speed.
        angle += timer.getTimePerFrame() * 50;
        // Wrap the angle to keep it inside 0-360 range
        angle %= 360;

        // Update the rotation matrix using the angle and rotation axis.
        rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
        // Update the box rotation using the rotation matrix.
        box.setRotation(rotate);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Box Example");
        _canvas.getCanvasRenderer().getRenderer().setBackgroundColor(ColorRGBA.BLACK);

        // Create a new box centered at (0,0,0) with width/height/depth of size 10.
        box = new Box("Box", new Vector3(0, 0, 0), 5, 5, 5);
        // Set a bounding box for frustum culling.
        box.setModelBound(new BoundingBox());
        // Move the box out from the camera 15 units.
        box.setTranslation(new Vector3(0, 0, -15));
        // Give the box some nice colors.
        box.setRandomColors();
        // Set the box's material
        box.setRenderMaterial("lit/textured/vertex_color_phong.yaml");
        // Attach the box to the scenegraph root.
        _root.attachChild(box);

        // XXX: try out fog by uncommenting below
        // box.setRenderMaterial("lit/textured/vertex_color_phong_fog.yaml");
        // box.setProperty(FogParams.DefaultPropertyKey, new FogParams(50f, 100f));

        // Add a texture to the box.
        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        box.setRenderState(ts);
    }
}
