/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.geom;

import java.util.Map;

import com.ardor3d.scenegraph.Mesh;

public class VertMap {

    private int[] _lookupTable;

    public VertMap(final Mesh mesh) {
        setupTable(mesh);
    }

    private void setupTable(final Mesh mesh) {
        _lookupTable = new int[mesh.getMeshData().getVertexCount()];
        for (int x = 0; x < _lookupTable.length; x++) {
            _lookupTable[x] = x;
        }
    }

    public int getNewIndex(final int oldIndex) {
        return _lookupTable[oldIndex];
    }

    public int getFirstOldIndex(final int newIndex) {
        for (int i = 0; i < _lookupTable.length; i++) {
            if (_lookupTable[i] == newIndex) {
                return i;
            }
        }
        return -1;
    }

    public void applyRemapping(final Map<Integer, Integer> indexRemap) {
        for (int i = 0; i < _lookupTable.length; i++) {
            if (indexRemap.containsKey(_lookupTable[i])) {
                _lookupTable[i] = indexRemap.get(_lookupTable[i]);
            }
        }
    }

    public int[] getLookupTable() {
        return _lookupTable;
    }
}
