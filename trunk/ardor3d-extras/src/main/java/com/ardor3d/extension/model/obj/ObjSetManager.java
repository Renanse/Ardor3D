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

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ObjSetManager {
    private final Map<ObjIndexSet, Integer> _store = Maps.newLinkedHashMap();
    private final List<Integer> _indices = Lists.newArrayList();
    private final List<Integer> _lengths = Lists.newArrayList();

    public int findSet(final ObjIndexSet set) {
        if (_store.containsKey(set)) {
            return _store.get(set);
        }

        final int index = _store.size();
        _store.put(set, index);
        return index;
    }

    public void addIndex(final int index) {
        _indices.add(index);
    }

    public void addLength(final int length) {
        _lengths.add(length);
    }

    public Map<ObjIndexSet, Integer> getStore() {
        return _store;
    }

    public List<Integer> getIndices() {
        return _indices;
    }

    public List<Integer> getLengths() {
        return _lengths;
    }
}