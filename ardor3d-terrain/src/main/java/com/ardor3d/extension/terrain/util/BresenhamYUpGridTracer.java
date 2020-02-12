/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.util;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * An implementation of AbstractBresenhamTracer that works on the XZ plane, with positive Y as up.
 */
public class BresenhamYUpGridTracer extends AbstractBresenhamTracer {

    // a "near zero" value we will use to determine if the walkRay is
    // perpendicular to the grid.
    protected static double TOLERANCE = 0.0000001;

    private int _stepXDirection;
    private int _stepZDirection;

    // from current position along ray
    private double _distToNextXIntersection, _distToNextZIntersection;
    private double _distBetweenXIntersections, _distBetweenZIntersections;

    @Override
    public void startWalk(final Ray3 walkRay) {
        // store ray
        _walkRay.set(walkRay);

        // simplify access to direction
        final ReadOnlyVector3 direction = _walkRay.getDirection();

        // Move start point to grid space
        final Vector3 start = _walkRay.getOrigin().subtract(_gridOrigin, null);

        _gridLocation[0] = (int) MathUtils.floor(start.getX() / _gridSpacing.getX());
        _gridLocation[1] = (int) MathUtils.floor(start.getZ() / _gridSpacing.getZ());

        final double invDirX = 1.0 / direction.getX();
        final double invDirZ = 1.0 / direction.getZ();

        // Check which direction on the X world axis we are moving.
        if (direction.getX() > BresenhamYUpGridTracer.TOLERANCE) {
            _distToNextXIntersection = ((_gridLocation[0] + 1) * _gridSpacing.getX() - start.getX()) * invDirX;
            _distBetweenXIntersections = _gridSpacing.getX() * invDirX;
            _stepXDirection = 1;
        } else if (direction.getX() < -BresenhamYUpGridTracer.TOLERANCE) {
            _distToNextXIntersection = (start.getX() - _gridLocation[0] * _gridSpacing.getX()) * -direction.getX();
            _distBetweenXIntersections = -_gridSpacing.getX() * invDirX;
            _stepXDirection = -1;
        } else {
            _distToNextXIntersection = Double.MAX_VALUE;
            _distBetweenXIntersections = Double.MAX_VALUE;
            _stepXDirection = 0;
        }

        // Check which direction on the Z world axis we are moving.
        if (direction.getZ() > BresenhamYUpGridTracer.TOLERANCE) {
            _distToNextZIntersection = ((_gridLocation[1] + 1) * _gridSpacing.getZ() - start.getZ()) * invDirZ;
            _distBetweenZIntersections = _gridSpacing.getZ() * invDirZ;
            _stepZDirection = 1;
        } else if (direction.getZ() < -BresenhamYUpGridTracer.TOLERANCE) {
            _distToNextZIntersection = (start.getZ() - _gridLocation[1] * _gridSpacing.getZ()) * -direction.getZ();
            _distBetweenZIntersections = -_gridSpacing.getZ() * invDirZ;
            _stepZDirection = -1;
        } else {
            _distToNextZIntersection = Double.MAX_VALUE;
            _distBetweenZIntersections = Double.MAX_VALUE;
            _stepZDirection = 0;
        }

        // Reset some variables
        _rayLocation.set(start);
        _totalTravel = 0.0;
        _stepDirection = Direction.None;
    }

    @Override
    public void next() {
        // Walk us to our next location based on distances to next X or Z grid
        // line.
        if (_distToNextXIntersection < _distToNextZIntersection) {
            _totalTravel = _distToNextXIntersection;
            _gridLocation[0] += _stepXDirection;
            _distToNextXIntersection += _distBetweenXIntersections;
            switch (_stepXDirection) {
                case -1:
                    _stepDirection = Direction.NegativeX;
                    break;
                case 0:
                    _stepDirection = Direction.None;
                    break;
                case 1:
                    _stepDirection = Direction.PositiveX;
                    break;
            }
        } else {
            _totalTravel = _distToNextZIntersection;
            _gridLocation[1] += _stepZDirection;
            _distToNextZIntersection += _distBetweenZIntersections;
            switch (_stepZDirection) {
                case -1:
                    _stepDirection = Direction.NegativeZ;
                    break;
                case 0:
                    _stepDirection = Direction.None;
                    break;
                case 1:
                    _stepDirection = Direction.PositiveZ;
                    break;
            }
        }

        _rayLocation.set(_walkRay.getDirection()).multiplyLocal(_totalTravel).addLocal(_walkRay.getOrigin());
    }

    @Override
    public boolean isRayPerpendicularToGrid() {
        return _stepXDirection == 0 && _stepZDirection == 0;
    }

    @Override
    public Vector3 get3DPoint(final double gridX, final double gridY, final double height, final Vector3 store) {
        final Vector3 rVal = store != null ? store : new Vector3();

        return rVal.set(gridX, height, gridY);
    }

    @Override
    public Vector2 get2DPoint(final ReadOnlyVector3 worldLocation, final Vector2 store) {
        final Vector2 rVal = store != null ? store : new Vector2();

        return rVal.set(worldLocation.getX(), worldLocation.getZ());
    }
}
