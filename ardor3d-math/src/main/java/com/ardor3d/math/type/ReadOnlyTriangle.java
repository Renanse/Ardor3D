/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.type;

import com.ardor3d.math.Triangle;

public interface ReadOnlyTriangle {
    int getIndex();

    ReadOnlyVector3 get(int index);

    ReadOnlyVector3 getA();

    ReadOnlyVector3 getB();

    ReadOnlyVector3 getC();

    ReadOnlyVector3 getNormal();

    ReadOnlyVector3 getCenter();

    Triangle clone();
}
