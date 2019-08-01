/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.geom.Debugger;

/**
 * Camera with additional pssm related functionality.
 */
public class DrawableCamera extends Mesh {
    private final Camera trackedCamera;

    private final ColorRGBA color;

    private final short pattern;

    /**
     * Instantiates a new drawable camera.
     */
    public DrawableCamera() {
        this(null, new ColorRGBA(0, 1, 1, 1), (short) 0xF000);
    }

    /**
     * Instantiates a new drawable camera.
     * 
     * @param width
     *            the width
     * @param height
     *            the height
     */
    public DrawableCamera(final Camera camera, final ColorRGBA color, final short pattern) {
        super("DrawableCamera");
        trackedCamera = camera;
        this.color = color;
        this.pattern = pattern;
    }

    @Override
    public void draw(final Renderer r) {
        Debugger.drawCameraFrustum(r, trackedCamera, color, pattern, true);
    }

}
