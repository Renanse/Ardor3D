/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.md3;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * frame of MD3: http://en.wikipedia.org/wiki/MD3_%28file_format%29#Frame
 */
final class Md3Frame {

    /** First corner of the bounding box. */
    final Vector3 _minBounds;
    /** Second corner of the bounding box. */
    final Vector3 _maxBounds;
    /** Local origin, usually (0, 0, 0). */
    final Vector3 _localOrigin;
    /** Radius of the bounding sphere. */
    final float _radius;
    /** name */
    final String _name;

    Md3Frame(final ReadOnlyVector3 minBounds, final ReadOnlyVector3 maxBounds, final ReadOnlyVector3 localOrigin,
            final float radius, final String name) {
        super();
        _minBounds = new Vector3();
        _maxBounds = new Vector3();
        _localOrigin = new Vector3();
        _minBounds.set(minBounds);
        _maxBounds.set(maxBounds);
        _localOrigin.set(localOrigin);
        _radius = radius;
        _name = name;
    }

}
