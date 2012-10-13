/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.geom.BufferUtils;

public class FloatBufferDataUtil {
    public static FloatBufferData makeNew(final ReadOnlyVector2[] coords) {
        if (coords == null) {
            return null;
        }

        return new FloatBufferData(BufferUtils.createFloatBuffer(coords), 2);
    }

    public static FloatBufferData makeNew(final ReadOnlyVector3[] coords) {
        if (coords == null) {
            return null;
        }

        return new FloatBufferData(BufferUtils.createFloatBuffer(coords), 3);
    }

    public static FloatBufferData makeNew(final float[] coords) {
        if (coords == null) {
            return null;
        }

        return new FloatBufferData(BufferUtils.createFloatBuffer(coords), 1);
    }

    /**
     * Check an incoming TexCoords object for null and correct size.
     * 
     * @param tc
     * @param vertexCount
     * @param perVert
     * @return tc if it is not null and the right size, otherwise it will be a new TexCoords object.
     */
    public static FloatBufferData ensureSize(final FloatBufferData tc, final int vertexCount, final int coordsPerVertex) {
        if (tc == null) {
            return new FloatBufferData(BufferUtils.createFloatBuffer(vertexCount * coordsPerVertex), coordsPerVertex);
        }

        if (tc.getBuffer().limit() == coordsPerVertex * vertexCount && tc.getValuesPerTuple() == coordsPerVertex) {
            tc.getBuffer().rewind();
            return tc;
        } else if (tc.getBuffer().limit() == coordsPerVertex * vertexCount) {
            tc.setValuesPerTuple(coordsPerVertex);
        } else {
            return new FloatBufferData(BufferUtils.createFloatBuffer(vertexCount * coordsPerVertex), coordsPerVertex);
        }

        return tc;
    }
}
