/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.shape;

import java.nio.FloatBuffer;

import com.ardor3d.math.Vector3;

public class MultiFaceBox extends Box {

    public MultiFaceBox() {
        super();
        remap();
    }

    public MultiFaceBox(final String name) {
        super(name);
        remap();
    }

    public MultiFaceBox(final String name, final Vector3 min, final Vector3 max) {
        super(name, min, max);
        remap();
    }

    public MultiFaceBox(final String name, final Vector3 center, final float xExtent, final float yExtent,
            final float zExtent) {
        super(name, center, xExtent, yExtent, zExtent);
        remap();
    }

    private void remap() {
        final FloatBuffer fb = _meshData.getTextureCoords(0).getBuffer();
        fb.rewind();
        for (int i = 0; i < 6; i++) {
            final float bottom = i / 8f;
            final float top = (i + 1) / 8f;
            final float[] tex = new float[] { 1, bottom, 0, bottom, 0, top, 1, top };
            fb.put(tex);
        }
    }
}