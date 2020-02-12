/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.filter.UpdateFilter;
import com.ardor3d.input.mouse.MouseState;

public class BasicFilterList implements IFilterList {
    final List<UpdateFilter> _filters = new ArrayList<>();

    public Iterator<UpdateFilter> iterator() {
        return _filters.iterator();
    }

    public void applyFilters(final InteractManager manager, final AbstractInteractWidget widget) {
        // apply any filters to our state
        for (final UpdateFilter filter : _filters) {
            filter.applyFilter(manager, widget);
        }
    }

    public void beginDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
        for (final UpdateFilter filter : _filters) {
            filter.beginDrag(manager, widget, state);
        }
    }

    public void endDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
        for (final UpdateFilter filter : _filters) {
            filter.endDrag(manager, widget, state);
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
