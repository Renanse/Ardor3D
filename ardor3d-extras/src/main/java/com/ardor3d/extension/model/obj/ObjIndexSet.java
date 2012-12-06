/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.obj;

public class ObjIndexSet {
    private final int _vIndex, _vtIndex;
    private final long _sGroup;
    private int _vnIndex;

    public ObjIndexSet(final String parts, final ObjDataStore store, final long smoothGroup) {
        final String[] tokens = parts.split("/");
        _vIndex = tokens.length < 1 ? -1 : parseValue(tokens[0], store.getVertices().size());
        _vtIndex = tokens.length < 2 ? -1 : parseValue(tokens[1], store.getUvs().size());
        _vnIndex = tokens.length < 3 ? -1 : parseValue(tokens[2], store.getNormals().size());
        _sGroup = smoothGroup;
    }

    private int parseValue(final String token, final int currentPosition) {
        if (token == null || "".equals(token)) {
            return -1;
        } else {
            int value = Integer.parseInt(token);
            if (value < 0) {
                value += currentPosition;
            } else {
                // OBJ is 1 based, so drop 1.
                value--;
            }
            return value;
        }
    }

    public long getSmoothGroup() {
        // normals override smoothing
        if (_vnIndex >= 0) {
            return 0;
        }
        return _sGroup;
    }

    public int getVIndex() {
        return _vIndex;
    }

    public int getVtIndex() {
        return _vtIndex;
    }

    public void setVnIndex(final int index) {
        _vnIndex = index;
    }

    public int getVnIndex() {
        return _vnIndex;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 31 * result + _vIndex;
        result += 31 * result + _vtIndex;
        result += 31 * result + _vnIndex;
        result += 31 * result + _sGroup;
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ObjIndexSet)) {
            return false;
        }
        final ObjIndexSet comp = (ObjIndexSet) o;
        return _vIndex == comp._vIndex && _vnIndex == comp._vnIndex && _vtIndex == comp._vtIndex
                && _sGroup == comp._sGroup;
    }
}
