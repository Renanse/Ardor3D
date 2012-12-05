/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.type;

import com.ardor3d.math.Ring;
import com.ardor3d.math.Vector3;

public interface ReadOnlyRing {

    ReadOnlyVector3 getCenter();

    ReadOnlyVector3 getUp();

    double getInnerRadius();

    double getOuterRadius();

    Vector3 random(Vector3 store);

    Ring clone();
}
