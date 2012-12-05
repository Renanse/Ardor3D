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

import java.nio.FloatBuffer;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.util.geom.BufferUtils;

public class TextureRecord extends StateRecord {

    public int wrapS, wrapT, wrapR;
    public int magFilter, minFilter;
    public int depthTextureMode, depthTextureCompareFunc, depthTextureCompareMode;
    public float anisoLevel = -1;
    public static FloatBuffer colorBuffer = BufferUtils.createColorBuffer(1);
    public ColorRGBA borderColor = new ColorRGBA(-1, -1, -1, -1);

    public TextureRecord() {}

    @Override
    public void invalidate() {
        super.invalidate();
        wrapS = wrapT = wrapR = 0;
        magFilter = minFilter = 0;
        depthTextureMode = depthTextureCompareFunc = depthTextureCompareMode = 0;
        anisoLevel = -1;
        borderColor.set(-1, -1, -1, -1);
    }
}
