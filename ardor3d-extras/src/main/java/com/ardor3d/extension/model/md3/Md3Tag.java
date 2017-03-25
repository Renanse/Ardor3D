/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.md3;

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Tag of MD3: http://en.wikipedia.org/wiki/MD3_%28file_format%29#Tag
 */
final class Md3Tag {

    /** name */
    final String _name;
    /** coordinates */
    final Vector3 _origin;
    /** 3x3 rotation matrix */
    final Matrix3 _axis;

    Md3Tag(final String name, final ReadOnlyVector3 origin, final ReadOnlyMatrix3 axis) {
        super();
        _origin = new Vector3();
        _axis = new Matrix3();
        _name = name;
        _origin.set(origin);
        _axis.set(axis);
    }
}
