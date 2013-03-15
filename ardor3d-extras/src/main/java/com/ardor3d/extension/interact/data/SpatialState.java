/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.interact.data;

import com.ardor3d.math.Transform;
import com.ardor3d.scenegraph.Spatial;

public class SpatialState {

    protected Transform _transform = new Transform();

    public Transform getTransform() {
        return _transform;
    }

    public void applyState(final Spatial target) {
        target.setTransform(_transform);
    }

}
