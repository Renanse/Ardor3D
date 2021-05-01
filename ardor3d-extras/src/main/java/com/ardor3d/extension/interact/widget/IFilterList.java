/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
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
import com.ardor3d.input.mouse.MouseState;

/**
 * Provides a clean way to capture all of beginDrag, endDrag, and applyFilters. This scheme allows a
 * compound widget to implement IFilterList and pass itself to leaf widgets, who can then forward
 * calls to the parent list.
 */
public interface IFilterList extends Iterable<UpdateFilter> {

  void applyFilters(final InteractManager manager, final AbstractInteractWidget widget);

  void beginDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state);

  void endDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state);

  int size();

  UpdateFilter get(final int index);

  boolean add(final UpdateFilter filter);

  boolean remove(final UpdateFilter filter);

  void clear();
}
