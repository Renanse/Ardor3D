/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.filter.UpdateFilter;

/**
 * Provides a clean way to capture all of beginDrag, endDrag, and applyFilters. This scheme allows a compound widget to
 * implement IFilerList and pass itself to leaf widgets, who can then forward calls to the parent list.
 *
 */
public interface IFilterList extends Iterable<UpdateFilter> {

    public void applyFilters(final InteractManager manager);

    public void beginDrag(final InteractManager manager);

    public void endDrag(final InteractManager manager);

    public int size();

    public UpdateFilter get(int index);

    public boolean add(final UpdateFilter filter);

    public boolean remove(final UpdateFilter filter);

    public void clear();
}
