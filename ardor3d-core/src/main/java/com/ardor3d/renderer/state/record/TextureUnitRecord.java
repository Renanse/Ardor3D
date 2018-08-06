/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

import com.ardor3d.math.Matrix4;

/**
 * Represents a texture unit in opengl
 */
public class TextureUnitRecord extends StateRecord {
    public Matrix4 texMatrix = new Matrix4();
    public int boundTexture = -1;
    public boolean identityMatrix = true;
    public float lodBias = 0f;

    public TextureUnitRecord() {}

    @Override
    public void invalidate() {
        super.invalidate();

        texMatrix.setIdentity();
        boundTexture = -1;
        lodBias = 0;
        identityMatrix = false;
    }
}
