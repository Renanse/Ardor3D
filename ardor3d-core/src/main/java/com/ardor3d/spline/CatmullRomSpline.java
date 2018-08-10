/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

/**
 * 
 */

package com.ardor3d.spline;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * CatmullRomSpline class is an implementation of spline that uses the Catmull-Rom equation:
 * 
 * <pre>
 * q(t) = 0.5 * ((2 * P1) + (-P0 + P2) * t + (2 * P0 - 5 * P1 + 4 * P2 - P3) * t2 + (-P0 + 3 * P1 - 3 * P2 + P3) * t3)
 * </pre>
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Cubic_Hermite_spline#Catmull.E2.80.93Rom_spline">Cubic Hermite spline -
 *      Wikipedia, the free encyclopedia</a>
 */
public class CatmullRomSpline implements Spline {

    /**
     * @see #interpolate(ReadOnlyVector3, ReadOnlyVector3, ReadOnlyVector3, ReadOnlyVector3, double, Vector3)
     */
    public Vector3 interpolate(final ReadOnlyVector3 p0, final ReadOnlyVector3 p1, final ReadOnlyVector3 p2,
            final ReadOnlyVector3 p3, final double t) {

        return interpolate(p0, p1, p2, p3, t, new Vector3());
    }

    /**
     * If any vector is <code>null</code> then the result is just returned unchanged.
     * 
     * @param p0
     *            The start control point.
     * @param p1
     *            Result will be between this and p2.
     * @param p2
     *            result will be between this and p1.
     * @param p3
     *            The end control point.
     * @param t
     *            <code>0.0 <= t <= 1.0</code>, if t <= 0.0 then result will be contain exact same values as p1 and if
     *            its >= 1.0 it will contain the exact same values as p2.
     * @param result
     *            The results from the interpolation will be stored in this vector.
     * @return The result vector as a convenience.
     */
    public Vector3 interpolate(final ReadOnlyVector3 p0, final ReadOnlyVector3 p1, final ReadOnlyVector3 p2,
            final ReadOnlyVector3 p3, final double t, final Vector3 result) {

        if (null != result && null != p0 && null != p1 && null != p2 && null != p3) {
            if (t <= 0.0) {
                result.set(p1);

            } else if (t >= 1.0) {
                result.set(p2);

            } else {
                final double t2 = t * t;
                final double t3 = t2 * t;

                result.setX(0.5 * ((2.0 * p1.getX()) + (-p0.getX() + p2.getX()) * t
                        + (2.0 * p0.getX() - 5.0 * p1.getX() + 4.0 * p2.getX() - p3.getX()) * t2 + (-p0.getX() + 3.0
                        * p1.getX() - 3.0 * p2.getX() + p3.getX())
                        * t3));

                result.setY(0.5 * ((2.0 * p1.getY()) + (-p0.getY() + p2.getY()) * t
                        + (2.0 * p0.getY() - 5.0 * p1.getY() + 4.0 * p2.getY() - p3.getY()) * t2 + (-p0.getY() + 3.0
                        * p1.getY() - 3.0 * p2.getY() + p3.getY())
                        * t3));

                result.setZ(0.5 * ((2.0 * p1.getZ()) + (-p0.getZ() + p2.getZ()) * t
                        + (2.0 * p0.getZ() - 5.0 * p1.getZ() + 4.0 * p2.getZ() - p3.getZ()) * t2 + (-p0.getZ() + 3.0
                        * p1.getZ() - 3.0 * p2.getZ() + p3.getZ())
                        * t3));
            }
        }

        return result;
    }

}
