/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.interact.filter;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.data.SpatialState;
import com.ardor3d.math.type.ReadOnlyVector3;

public class AllowScaleFilter implements UpdateFilter {

    protected boolean _xAxis, _yAxis, _zAxis;

    public AllowScaleFilter(final boolean xAxis, final boolean yAxis, final boolean zAxis) {
        _xAxis = xAxis;
        _yAxis = yAxis;
        _zAxis = zAxis;
    }

    @Override
    public void applyFilter(final InteractManager manager) {
        final ReadOnlyVector3 oldScale = manager.getSpatialTarget().getScale();
        final SpatialState state = manager.getSpatialState();
        final ReadOnlyVector3 scale = state.getTransform().getScale();

        state.getTransform().setScale( //
                _xAxis ? scale.getX() : oldScale.getX(), //
                _yAxis ? scale.getY() : oldScale.getY(), //
                _zAxis ? scale.getZ() : oldScale.getZ());
    }

    @Override
    public void beginDrag(final InteractManager manager) {}

    @Override
    public void endDrag(final InteractManager manager) {}
}
