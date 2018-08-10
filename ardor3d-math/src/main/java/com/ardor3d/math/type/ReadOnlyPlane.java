/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.type;

import com.ardor3d.math.Plane;
import com.ardor3d.math.Vector3;

public interface ReadOnlyPlane {

    public enum Side {
        /**
         * On the side of the plane opposite of the plane's normal vector.
         */
        Inside,

        /**
         * On the same side of the plane as the plane's normal vector.
         */
        Outside,

        /**
         * Not on either side - in other words, on the plane itself.
         */
        Neither;
    }

    double getConstant();

    ReadOnlyVector3 getNormal();

    double pseudoDistance(ReadOnlyVector3 point);

    Side whichSide(ReadOnlyVector3 point);

    Vector3 reflectVector(ReadOnlyVector3 unitVector, Vector3 store);

    Plane clone();
}
