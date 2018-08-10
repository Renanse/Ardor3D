/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
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
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.util.geom.BufferUtils;

public class FogStateRecord extends StateRecord {

    public boolean enabled = false;
    public float fogStart = -1;
    public float fogEnd = -1;
    public float density = -1;
    public int fogMode = -1;
    public int fogHint = -1;
    public ColorRGBA fogColor = null;
    public FloatBuffer colorBuff = null;
    public FogState.CoordinateSource source = null;

    public FogStateRecord() {
        fogColor = new ColorRGBA(0, 0, 0, -1);
        colorBuff = BufferUtils.createColorBuffer(1);
    }

    @Override
    public void invalidate() {
        super.invalidate();

        enabled = false;
        fogStart = -1;
        fogEnd = -1;
        density = -1;
        fogMode = -1;
        fogHint = -1;
        fogColor.set(0, 0, 0, -1);
        source = null;
    }
}
