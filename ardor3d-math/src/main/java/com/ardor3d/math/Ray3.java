/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

import com.ardor3d.math.type.ReadOnlyPlane;
import com.ardor3d.math.type.ReadOnlyRay3;
import com.ardor3d.math.type.ReadOnlyVector3;

public class Ray3 extends Line3Base implements ReadOnlyRay3, Poolable {

    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Ray3> RAY_POOL = ObjectPool.create(Ray3.class, MathConstants.maxMathPoolSize);

    /**
     * Constructs a new ray with an origin at (0,0,0) and a direction of (0,0,1).
     */
    public Ray3() {
        super(Vector3.ZERO, Vector3.UNIT_Z);
    }

    /**
     * Copy constructor.
     * 
     * @param source
     *            the ray to copy from.
     */
    public Ray3(final ReadOnlyRay3 source) {
        this(source.getOrigin(), source.getDirection());
    }

    /**
     * Constructs a new ray using the supplied origin point and unit length direction vector
     * 
     * @param origin
     * @param direction
     *            - unit length
     */
    public Ray3(final ReadOnlyVector3 origin, final ReadOnlyVector3 direction) {
        super(origin, direction);
    }

    /**
     * Copies the values of the given source ray into this ray.
     * 
     * @param source
     * @return this ray for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Ray3 set(final ReadOnlyRay3 source) {
        _origin.set(source.getOrigin());
        _direction.set(source.getDirection());
        return this;
    }

    /**
     * @param worldVertices
     *            an array of vectors describing a polygon
     * @return the distance from our origin to the primitive or POSITIVE_INFINITY if we do not intersect.
     */
    @Override
    public double getDistanceToPrimitive(final Vector3[] worldVertices) {
        // Intersection test
        final Vector3 intersect = Vector3.fetchTempInstance();
        try {
            if (intersects(worldVertices, intersect)) {
                return getOrigin().distance(intersect);
            }
        } finally {
            Vector3.releaseTempInstance(intersect);
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * @param polygonVertices
     * @param locationStore
     * @return true if this ray intersects a polygon described by the given vertices.
     */
    @Override
    public boolean intersects(final Vector3[] polygonVertices, final Vector3 locationStore) {
        if (polygonVertices.length == 3) {
            // TRIANGLE
            return intersectsTriangle(polygonVertices[0], polygonVertices[1], polygonVertices[2], locationStore);
        } else if (polygonVertices.length == 4) {
            // QUAD
            return intersectsQuad(polygonVertices[0], polygonVertices[1], polygonVertices[2], polygonVertices[3],
                    locationStore);
        }
        // TODO: Add support for line and point
        return false;
    }

    /**
     * @param pointA
     * @param pointB
     * @param pointC
     * @param locationStore
     *            if not null, and this ray intersects, the point of intersection is calculated and stored in this
     *            Vector3
     * @return true if this ray intersects a triangle formed by the given three points.
     * @throws NullPointerException
     *             if any of the points are null.
     */
    @Override
    public boolean intersectsTriangle(final ReadOnlyVector3 pointA, final ReadOnlyVector3 pointB,
            final ReadOnlyVector3 pointC, final Vector3 locationStore) {
        return intersects(pointA, pointB, pointC, locationStore, false);
    }

    /**
     * @param pointA
     * @param pointB
     * @param pointC
     * @param locationStore
     *            if not null, and this ray intersects, the point of intersection is calculated and stored in this
     *            Vector3 as (t, u, v) where t is the distance from the _origin to the point of intersection and (u, v)
     *            is the intersection point on the triangle plane.
     * @return true if this ray intersects a triangle formed by the given three points.
     * @throws NullPointerException
     *             if any of the points are null.
     */
    @Override
    public boolean intersectsTrianglePlanar(final ReadOnlyVector3 pointA, final ReadOnlyVector3 pointB,
            final ReadOnlyVector3 pointC, final Vector3 locationStore) {
        return intersects(pointA, pointB, pointC, locationStore, true);
    }

    /**
     * @param pointA
     * @param pointB
     * @param pointC
     * @param pointD
     * @param locationStore
     *            if not null, and this ray intersects, the point of intersection is calculated and stored in this
     *            Vector3
     * @return true if this ray intersects a triangle formed by the given three points. The points are assumed to be
     *         coplanar.
     * @throws NullPointerException
     *             if any of the points are null.
     */
    @Override
    public boolean intersectsQuad(final ReadOnlyVector3 pointA, final ReadOnlyVector3 pointB,
            final ReadOnlyVector3 pointC, final ReadOnlyVector3 pointD, final Vector3 locationStore) {
        return intersects(pointA, pointB, pointC, locationStore, false)
                || intersects(pointA, pointC, pointD, locationStore, false);
    }

    /**
     * @param pointA
     * @param pointB
     * @param pointC
     * @param pointD
     * @param locationStore
     *            if not null, and this ray intersects, the point of intersection is calculated and stored in this
     *            Vector3 as (t, u, v) where t is the distance from the _origin to the point of intersection and (u, v)
     *            is the intersection point on the triangle plane.
     * @return true if this ray intersects a quad formed by the given four points. The points are assumed to be
     *         coplanar.
     * @throws NullPointerException
     *             if any of the points are null.
     */
    @Override
    public boolean intersectsQuadPlanar(final ReadOnlyVector3 pointA, final ReadOnlyVector3 pointB,
            final ReadOnlyVector3 pointC, final ReadOnlyVector3 pointD, final Vector3 locationStore) {
        return intersects(pointA, pointB, pointC, locationStore, true)
                || intersects(pointA, pointC, pointD, locationStore, true);
    }

    /**
     * Ray vs triangle implementation.
     * 
     * @param pointA
     * @param pointB
     * @param pointC
     * @param locationStore
     * @param doPlanar
     * @return true if this ray intersects a triangle formed by the given three points.
     * @throws NullPointerException
     *             if any of the points are null.
     */
    protected boolean intersects(final ReadOnlyVector3 pointA, final ReadOnlyVector3 pointB,
            final ReadOnlyVector3 pointC, final Vector3 locationStore, final boolean doPlanar) {
        final Vector3 diff = Vector3.fetchTempInstance().set(_origin).subtractLocal(pointA);
        final Vector3 edge1 = Vector3.fetchTempInstance().set(pointB).subtractLocal(pointA);
        final Vector3 edge2 = Vector3.fetchTempInstance().set(pointC).subtractLocal(pointA);
        final Vector3 norm = Vector3.fetchTempInstance().set(edge1).crossLocal(edge2);

        double dirDotNorm = _direction.dot(norm);
        double sign;
        if (dirDotNorm > MathUtils.EPSILON) {
            sign = 1.0;
        } else if (dirDotNorm < -MathUtils.EPSILON) {
            sign = -1.0;
            dirDotNorm = -dirDotNorm;
        } else {
            // ray and triangle/quad are parallel
            return false;
        }

        final double dirDotDiffxEdge2 = sign * _direction.dot(diff.cross(edge2, edge2));
        boolean result = false;
        if (dirDotDiffxEdge2 >= 0.0) {
            final double dirDotEdge1xDiff = sign * _direction.dot(edge1.crossLocal(diff));
            if (dirDotEdge1xDiff >= 0.0) {
                if (dirDotDiffxEdge2 + dirDotEdge1xDiff <= dirDotNorm) {
                    final double diffDotNorm = -sign * diff.dot(norm);
                    if (diffDotNorm >= 0.0) {
                        // ray intersects triangle
                        // if storage vector is null, just return true,
                        if (locationStore == null) {
                            return true;
                        }
                        // else fill in.
                        final double inv = 1f / dirDotNorm;
                        final double t = diffDotNorm * inv;
                        if (!doPlanar) {
                            locationStore.set(_origin).addLocal(_direction.getX() * t, _direction.getY() * t,
                                    _direction.getZ() * t);
                        } else {
                            // these weights can be used to determine
                            // interpolated values, such as texture coord.
                            // eg. texcoord s,t at intersection point:
                            // s = w0*s0 + w1*s1 + w2*s2;
                            // t = w0*t0 + w1*t1 + w2*t2;
                            final double w1 = dirDotDiffxEdge2 * inv;
                            final double w2 = dirDotEdge1xDiff * inv;
                            // double w0 = 1.0 - w1 - w2;
                            locationStore.set(t, w1, w2);
                        }
                        result = true;
                    }
                }
            }
        }
        Vector3.releaseTempInstance(diff);
        Vector3.releaseTempInstance(edge1);
        Vector3.releaseTempInstance(edge2);
        Vector3.releaseTempInstance(norm);
        return result;
    }

    /**
     * @param plane
     * @param locationStore
     *            if not null, and this ray intersects the plane, the world location of the point of intersection is
     *            stored in this vector.
     * @return true if the ray collides with the given Plane
     * @throws NullPointerException
     *             if the plane is null.
     */
    @Override
    public boolean intersectsPlane(final ReadOnlyPlane plane, final Vector3 locationStore) {
        final ReadOnlyVector3 normal = plane.getNormal();
        final double denominator = normal.dot(_direction);

        if (denominator > -MathUtils.EPSILON && denominator < MathUtils.EPSILON) {
            return false; // coplanar
        }

        final double numerator = -normal.dot(_origin) + plane.getConstant();
        final double ratio = numerator / denominator;

        if (ratio < MathUtils.EPSILON) {
            return false; // intersects behind _origin
        }

        if (locationStore != null) {
            locationStore.set(_direction).multiplyLocal(ratio).addLocal(_origin);
        }

        return true;
    }

    /**
     * @param point
     * @param store
     *            if not null, the closest point is stored in this param
     * @return the squared distance from this ray to the given point.
     * @throws NullPointerException
     *             if the point is null.
     */
    @Override
    public double distanceSquared(final ReadOnlyVector3 point, final Vector3 store) {
        final Vector3 vectorA = Vector3.fetchTempInstance();
        vectorA.set(point).subtractLocal(_origin);
        final double t0 = _direction.dot(vectorA);
        if (t0 > 0) {
            // d = |P - (O + t*D)|
            vectorA.set(_direction).multiplyLocal(t0);
            vectorA.addLocal(_origin);
        } else {
            // ray is closest to origin point
            vectorA.set(_origin);
        }

        // Save away the closest point if requested.
        if (store != null) {
            store.set(vectorA);
        }

        point.subtract(vectorA, vectorA);
        final double lSQ = vectorA.lengthSquared();
        Vector3.releaseTempInstance(vectorA);
        return lSQ;
    }

    /**
     * Check a ray... if it is null or the values of its origin or direction are NaN or infinite, return false. Else
     * return true.
     * 
     * @param ray
     *            the ray to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyRay3 ray) {
        if (ray == null) {
            return false;
        }

        return Vector3.isValid(ray.getDirection()) && Vector3.isValid(ray.getOrigin());
    }

    /**
     * @return the string representation of this ray.
     */
    @Override
    public String toString() {
        return "com.ardor3d.math.Ray [Origin: " + _origin + " - Direction: " + _direction + "]";
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this ray and the provided ray have the same constant and normal values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyRay3)) {
            return false;
        }
        final ReadOnlyRay3 comp = (ReadOnlyRay3) o;
        return _origin.equals(comp.getOrigin()) && _direction.equals(comp.getDirection());
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Ray3 clone() {
        return new Ray3(this);
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Ray that is intended for temporary use in calculations and so forth. Multiple calls to the
     *         method should return instances of this class that are not currently in use.
     */
    public final static Ray3 fetchTempInstance() {
        if (MathConstants.useMathPools) {
            return Ray3.RAY_POOL.fetch();
        } else {
            return new Ray3();
        }
    }

    /**
     * Releases a Ray back to be used by a future call to fetchTempInstance. TAKE CARE: this Ray object should no longer
     * have other classes referencing it or "Bad Things" will happen.
     * 
     * @param ray
     *            the Ray to release.
     */
    public final static void releaseTempInstance(final Ray3 ray) {
        if (MathConstants.useMathPools) {
            Ray3.RAY_POOL.release(ray);
        }
    }
}
