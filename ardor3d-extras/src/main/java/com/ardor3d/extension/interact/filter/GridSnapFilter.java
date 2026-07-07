/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.filter;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.data.SpatialState;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Filter that snaps the state's translation to a regular grid, in the target's parent coordinate
 * space. Attach to translation widgets; toggle with {@link #setEnabled(boolean)} to make snapping
 * a modifier-key behavior (e.g. while Ctrl is held).
 */
public class GridSnapFilter extends UpdateFilterAdapter {

  protected final Vector3 _calcVec = new Vector3();

  protected double _gridSize;
  protected boolean _enabled = true;

  public GridSnapFilter(final double gridSize) {
    _gridSize = gridSize;
  }

  @Override
  public void applyFilter(final InteractManager manager, final AbstractInteractWidget widget) {
    if (!_enabled || _gridSize <= 0) {
      return;
    }
    final SpatialState state = manager.getSpatialState();
    final ReadOnlyVector3 translation = state.getTransform().getTranslation();
    _calcVec.set( //
        Math.round(translation.getX() / _gridSize) * _gridSize,
        Math.round(translation.getY() / _gridSize) * _gridSize,
        Math.round(translation.getZ() / _gridSize) * _gridSize);
    state.getTransform().setTranslation(_calcVec);
  }

  public double getGridSize() { return _gridSize; }

  public void setGridSize(final double gridSize) { _gridSize = gridSize; }

  public boolean isEnabled() { return _enabled; }

  public void setEnabled(final boolean enabled) { _enabled = enabled; }
}
