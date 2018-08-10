/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.interact.widget;

import java.util.Iterator;
import java.util.List;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.filter.UpdateFilter;
import com.google.common.collect.Lists;

public class BasicFilterList implements IFilterList {
    final List<UpdateFilter> _filters = Lists.newArrayList();

    public Iterator<UpdateFilter> iterator() {
        return _filters.iterator();
    }

    public void applyFilters(final InteractManager manager) {
        // apply any filters to our state
        for (final UpdateFilter filter : _filters) {
            filter.applyFilter(manager);
        }
    }

    public void beginDrag(final InteractManager manager) {
        for (final UpdateFilter filter : _filters) {
            filter.beginDrag(manager);
        }
    }

    public void endDrag(final InteractManager manager) {
        for (final UpdateFilter filter : _filters) {
            filter.endDrag(manager);
        }
    }

    public int size() {
        return _filters.size();
    }

    public UpdateFilter get(final int index) {
        return _filters.get(index);
    }

    public boolean add(final UpdateFilter filter) {
        return _filters.add(filter);
    }

    public boolean remove(final UpdateFilter filter) {
        return _filters.remove(filter);
    }

    public void clear() {
        _filters.clear();
    }

}
