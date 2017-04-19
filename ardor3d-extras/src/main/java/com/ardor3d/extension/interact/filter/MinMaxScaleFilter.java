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
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

public class MinMaxScaleFilter implements UpdateFilter {
    protected Vector3 _minScale = new Vector3();
    protected Vector3 _maxScale = new Vector3();

    public MinMaxScaleFilter(final double min, final double max) {
        _minScale.set(min, min, min);
        _maxScale.set(max, max, max);
    }

    public MinMaxScaleFilter(final ReadOnlyVector3 min, final ReadOnlyVector3 max) {
        _minScale.set(min);
        _maxScale.set(max);
    }

    @Override
    public void applyFilter(final InteractManager manager) {
        final SpatialState state = manager.getSpatialState();
        final ReadOnlyVector3 scale = state.getTransform().getScale();
        final double x = MathUtils.clamp(scale.getX(), _minScale.getX(), _maxScale.getX());
        final double y = MathUtils.clamp(scale.getY(), _minScale.getY(), _maxScale.getY());
        final double z = MathUtils.clamp(scale.getZ(), _minScale.getZ(), _maxScale.getZ());

        state.getTransform().setScale(x, y, z);
    }

    @Override
    public void beginDrag(final InteractManager manager) { /**/}

    @Override
    public void endDrag(final InteractManager manager) { /**/}
}
