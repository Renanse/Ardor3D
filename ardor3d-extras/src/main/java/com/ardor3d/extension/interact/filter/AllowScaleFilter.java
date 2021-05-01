/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
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
import com.ardor3d.math.type.ReadOnlyVector3;

public class AllowScaleFilter extends UpdateFilterAdapter {

  protected boolean _xAxis, _yAxis, _zAxis;

  public AllowScaleFilter(final boolean xAxis, final boolean yAxis, final boolean zAxis) {
    _xAxis = xAxis;
    _yAxis = yAxis;
    _zAxis = zAxis;
  }

  @Override
  public void applyFilter(final InteractManager manager, final AbstractInteractWidget widget) {
    final ReadOnlyVector3 oldScale = manager.getSpatialTarget().getScale();
    final SpatialState state = manager.getSpatialState();
    final ReadOnlyVector3 scale = state.getTransform().getScale();

    state.getTransform().setScale( //
        _xAxis ? scale.getX() : oldScale.getX(), //
        _yAxis ? scale.getY() : oldScale.getY(), //
        _zAxis ? scale.getZ() : oldScale.getZ());
  }
}
