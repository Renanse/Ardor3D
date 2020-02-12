/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.type;

import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;

public interface ReadOnlyRay3 extends ReadOnlyLine3Base {

    double getDistanceToPrimitive(Vector3[] worldVertices);

    boolean intersects(final Vector3[] polygonVertices, final Vector3 locationStore);

    boolean intersectsTriangle(ReadOnlyVector3 pointA, ReadOnlyVector3 pointB, ReadOnlyVector3 pointC,
            Vector3 locationStore);

    boolean intersectsTrianglePlanar(ReadOnlyVector3 pointA, ReadOnlyVector3 pointB, ReadOnlyVector3 pointC,
            Vector3 locationStore);

    boolean intersectsQuad(ReadOnlyVector3 pointA, ReadOnlyVector3 pointB, ReadOnlyVector3 pointC,
            ReadOnlyVector3 pointD, Vector3 locationStore);

    boolean intersectsQuadPlanar(ReadOnlyVector3 pointA, ReadOnlyVector3 pointB, ReadOnlyVector3 pointC,
            ReadOnlyVector3 pointD, Vector3 locationStore);

    boolean intersectsPlane(ReadOnlyPlane plane, Vector3 locationStore);

    Ray3 clone();
}
