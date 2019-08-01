/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.util;

import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * AbstractBresenhamTracer is a simple base class for using Bresenham's line equation. Bresenham's line equation is
 * useful for doing various tasks that involve regularly spaced grids (such as picking against a height-map.) This class
 * is not intended to do any picking, only traveling and reporting the grid squares traversed.
 */
public abstract class AbstractBresenhamTracer {

    protected final Vector3 _gridOrigin = new Vector3();
    protected final Vector3 _gridSpacing = new Vector3(1, 1, 1);
    protected final int[] _gridLocation = new int[2];
    protected final Vector3 _rayLocation = new Vector3();
    protected final Ray3 _walkRay = new Ray3();

    protected Direction _stepDirection = Direction.None;
    protected double _totalTravel;

    public enum Direction {
        None, PositiveX, NegativeX, PositiveY, NegativeY, PositiveZ, NegativeZ;
    };

    /**
     * @return the direction of our last step on the grid.
     */
    public Direction getLastStepDirection() {
        return _stepDirection;
    }

    /**
     * @return the row and column we are currently in on the grid.
     */
    public int[] getGridLocation() {
        return _gridLocation;
    }

    /**
     * @return the total length we have traveled from the origin of our ray set in startWalk.
     */
    public double getTotalTraveled() {
        return _totalTravel;
    }

    /**
     * Set the world origin of our grid. This is useful to the tracer when doing conversion between world coordinates
     * and grid locations.
     * 
     * @param origin
     *            our new origin (copied into the tracer)
     */
    public void setGridOrigin(final Vector3 origin) {
        _gridOrigin.set(origin);
    }

    /**
     * @return the current grid origin
     * @see #setGridOrigin(Vector3)
     */
    public Vector3 getGridOrigin() {
        return _gridOrigin;
    }

    /**
     * Set the world spacing (scale) of our grid. Also useful for converting between world coordinates and grid
     * location.
     * 
     * @param spacing
     *            our new spacing (copied into the tracer)
     */
    public void setGridSpacing(final Vector3 spacing) {
        _gridSpacing.set(spacing);
    }

    /**
     * @return the current grid spacing
     * @see #setGridSpacing(Vector3)
     */
    public Vector3 getGridSpacing() {
        return _gridSpacing;
    }

    /**
     * Set up our position on the grid and initialize the tracer using the provided ray.
     * 
     * @param walkRay
     *            the world ray along which we we walk the grid.
     */
    public abstract void startWalk(Ray3 walkRay);

    /**
     * Move us along our walkRay to the next grid location.
     */
    public abstract void next();

    /**
     * @return true if our walkRay, specified in startWalk, ended up being perpendicular to the grid (and therefore can
     *         not move to a new grid location on calls to next(). You should test this after calling startWalk and
     *         before calling next().
     */
    public abstract boolean isRayPerpendicularToGrid();

    /**
     * Turns a point on a 2D grid and a height into a 3D point based on the world up of this tracer.
     * 
     * @param row
     * @param col
     * @param height
     * @param store
     *            the vector to store our result in. if null a new vector is created and returned.
     * @return
     */
    public abstract Vector3 get3DPoint(double gridX, double gridY, double height, Vector3 store);

    /**
     * Casts a world location to the local plane of this tracer.
     * 
     * @param worldLocation
     * @return the point on the plane used by this tracer.
     */
    public abstract Vector2 get2DPoint(ReadOnlyVector3 worldLocation, Vector2 store);
}
