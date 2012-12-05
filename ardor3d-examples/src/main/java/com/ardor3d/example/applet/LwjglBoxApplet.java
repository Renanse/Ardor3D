/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.applet;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.image.Texture;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.TextureManager;

/**
 * The classic "Box Example" as an Ardor3D/LWJGL applet.
 */
public class LwjglBoxApplet extends LwjglBaseApplet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void initAppletScene() {
        // Make a box...
        final Box box = new Box("Box", Vector3.ZERO, 5, 5, 5);

        // Make it a bit more colorful.
        box.setRandomColors();

        // Setup a bounding box for it.
        box.setModelBound(new BoundingBox());

        // Set its location in space.
        box.setTranslation(new Vector3(0, 0, -15));

        // Add to root.
        _root.attachChild(box);

        // set it to rotate:
        box.addController(new SpatialController<Spatial>() {
            private final Vector3 _axis = new Vector3(1, 1, 0.5f).normalizeLocal();
            private final Matrix3 _rotate = new Matrix3();
            private double _angle = 0;

            public void update(final double time, final Spatial caller) {
                // update our rotation
                _angle = _angle + (_timer.getTimePerFrame() * 25);
                if (_angle > 180) {
                    _angle = -180;
                }

                _rotate.fromAngleNormalAxis(_angle * MathUtils.DEG_TO_RAD, _axis);
                box.setRotation(_rotate);
            }
        });

        // Create a texture from the Ardor3D logo.
        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        box.setRenderState(ts);
    }
}
