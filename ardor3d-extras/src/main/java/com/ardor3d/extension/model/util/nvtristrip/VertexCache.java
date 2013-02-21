/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.util.nvtristrip;

import java.util.Arrays;

/**
 * Ported from <a href="http://developer.nvidia.com/object/nvtristrip_library.html">NVIDIA's NvTriStrip Library</a>
 */
final class VertexCache {

    private final int[] _entries;
    private final int _numEntries;

    public VertexCache(final int size) {
        _numEntries = size;

        _entries = new int[_numEntries];

        for (int i = 0; i < _numEntries; i++) {
            _entries[i] = -1;
        }
    }

    public VertexCache() {
        this(16);
    }

    public boolean inCache(final int entry) {
        boolean returnVal = false;
        for (int i = 0; i < _numEntries; i++) {
            if (_entries[i] == entry) {
                returnVal = true;
                break;
            }
        }

        return returnVal;
    }

    public int addEntry(final int entry) {
        int removed;

        removed = _entries[_numEntries - 1];

        // push everything right one
        for (int i = _numEntries - 2; i >= 0; i--) {
            _entries[i + 1] = _entries[i];
        }

        _entries[0] = entry;

        return removed;
    }

    public void clear() {
        Arrays.fill(_entries, -1);
    }

    public void copy(final VertexCache inVcache) {
        for (int i = 0; i < _numEntries; i++) {
            inVcache.set(i, _entries[i]);
        }
    }

    public int at(final int index) {
        return _entries[index];
    }

    public void set(final int index, final int value) {
        _entries[index] = value;
    }
}
