/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.util.nvtristrip;

import com.ardor3d.renderer.IndexMode;

public class PrimitiveGroup {

    private IndexMode _type;
    private int[] _indices;
    private int _numIndices;

    PrimitiveGroup() {
        setType(IndexMode.Triangles);
        setIndices(null);
        setNumIndices(0);
    }

    int[] _getIndices() {
        return _indices;
    }

    public int[] getIndices() {
        if (_indices.length == _numIndices) {
            return _indices;
        }

        // crop it down to actual size...
        final int[] realIndices = new int[_numIndices];
        System.arraycopy(_indices, 0, realIndices, 0, _numIndices);
        _indices = realIndices;
        return _indices;
    }

    int getNumIndices() {
        return _numIndices;
    }

    public IndexMode getType() {
        return _type;
    }

    void setType(final IndexMode type) {
        _type = type;
    }

    void setIndices(final int[] indices) {
        _indices = indices;
    }

    void setNumIndices(final int numIndices) {
        this._numIndices = numIndices;
    }
}
