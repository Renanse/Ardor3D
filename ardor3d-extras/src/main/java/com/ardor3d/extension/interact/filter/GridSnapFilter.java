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
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.Vector3;

/**
 * Filter that snaps the translation accumulated over the course of a drag to grid increments, in
 * the target's parent coordinate space. The translation at drag start is captured, the raw
 * per-event offsets the widget applies are accumulated (the manager resets the state from the
 * target every input event, and the target only ever receives snapped values - the filter must
 * keep the running total itself), and the target moves in whole grid steps from where the drag
 * began. Toggle with {@link #setEnabled(boolean)} to make snapping a modifier-key behavior (e.g.
 * while Ctrl is held); the raw total keeps accumulating while disabled, so mid-drag toggles snap
 * relative to the whole drag.
 */
public class GridSnapFilter extends UpdateFilterAdapter {

  protected final Vector3 _calcVec = new Vector3();

  protected double _gridSize;
  protected boolean _enabled = true;

  /** Target translation captured at drag start; null when no drag is active. */
  protected Vector3 _startTranslation = null;
  /** Raw (unsnapped) offset the drag has applied since it started. */
  protected final Vector3 _accumulated = new Vector3();

  public GridSnapFilter(final double gridSize) {
    _gridSize = gridSize;
  }

  @Override
  public void beginDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
    if (manager.getSpatialTarget() != null) {
      _startTranslation = new Vector3(manager.getSpatialTarget().getTransform().getTranslation());
      _accumulated.zero();
    }
  }

  @Override
  public void endDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState state) {
    _startTranslation = null;
  }

  @Override
  public void applyFilter(final InteractManager manager, final AbstractInteractWidget widget) {
    if (_startTranslation == null || manager.getSpatialTarget() == null) {
      return;
    }
    final SpatialState state = manager.getSpatialState();

    // This event's raw widget offset is the state's divergence from the target, which the
    // manager has not yet updated this cycle. Fold it into the running total.
    _calcVec.set(state.getTransform().getTranslation())
        .subtractLocal(manager.getSpatialTarget().getTransform().getTranslation());
    _accumulated.addLocal(_calcVec);

    if (!_enabled || _gridSize <= 0) {
      return;
    }

    // Move in whole grid steps from the drag start.
    _calcVec.set( //
        Math.round(_accumulated.getX() / _gridSize) * _gridSize,
        Math.round(_accumulated.getY() / _gridSize) * _gridSize,
        Math.round(_accumulated.getZ() / _gridSize) * _gridSize);
    state.getTransform().setTranslation(_calcVec.addLocal(_startTranslation));
  }

  public double getGridSize() { return _gridSize; }

  public void setGridSize(final double gridSize) { _gridSize = gridSize; }

  public boolean isEnabled() { return _enabled; }

  public void setEnabled(final boolean enabled) { _enabled = enabled; }
}
