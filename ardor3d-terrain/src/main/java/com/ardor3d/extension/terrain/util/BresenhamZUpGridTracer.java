/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.util;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * An implementation of AbstractBresenhamTracer that works on the XY plane, with positive Z as up.
 */
public class BresenhamZUpGridTracer extends AbstractBresenhamTracer {

    // a "near zero" value we will use to determine if the walkRay is
    // perpendicular to the grid.
    protected static double TOLERANCE = 0.0000001;

    private int _stepXDirection;
    private int _stepYDirection;

    // from current position along ray
    private double _distToNextXIntersection, _distToNextYIntersection;
    private double _distBetweenXIntersections, _distBetweenYIntersections;

    @Override
    public void startWalk(final Ray3 walkRay) {
        // store ray
        _walkRay.set(walkRay);

        // simplify access to direction
        final ReadOnlyVector3 direction = _walkRay.getDirection();

        // Move start point to grid space
        final Vector3 start = _walkRay.getOrigin().subtract(_gridOrigin, null);

        _gridLocation[0] = (int) MathUtils.floor(start.getX() / _gridSpacing.getX());
        _gridLocation[1] = (int) MathUtils.floor(start.getY() / _gridSpacing.getY());

        final double invDirX = 1.0 / direction.getX();
        final double invDirY = 1.0 / direction.getY();

        // Check which direction on the X world axis we are moving.
        if (direction.getX() > BresenhamZUpGridTracer.TOLERANCE) {
            _distToNextXIntersection = ((_gridLocation[0] + 1) * _gridSpacing.getX() - start.getX()) * invDirX;
            _distBetweenXIntersections = _gridSpacing.getX() * invDirX;
            _stepXDirection = 1;
        } else if (direction.getX() < -BresenhamZUpGridTracer.TOLERANCE) {
            _distToNextXIntersection = (start.getX() - _gridLocation[0] * _gridSpacing.getX()) * -direction.getX();
            _distBetweenXIntersections = -_gridSpacing.getX() * invDirX;
            _stepXDirection = -1;
        } else {
            _distToNextXIntersection = Double.MAX_VALUE;
            _distBetweenXIntersections = Double.MAX_VALUE;
            _stepXDirection = 0;
        }

        // Check which direction on the Y world axis we are moving.
        if (direction.getY() > BresenhamZUpGridTracer.TOLERANCE) {
            _distToNextYIntersection = ((_gridLocation[1] + 1) * _gridSpacing.getY() - start.getY()) * invDirY;
            _distBetweenYIntersections = _gridSpacing.getY() * invDirY;
            _stepYDirection = 1;
        } else if (direction.getY() < -BresenhamZUpGridTracer.TOLERANCE) {
            _distToNextYIntersection = (start.getY() - _gridLocation[1] * _gridSpacing.getY()) * -direction.getY();
            _distBetweenYIntersections = -_gridSpacing.getY() * invDirY;
            _stepYDirection = -1;
        } else {
            _distToNextYIntersection = Double.MAX_VALUE;
            _distBetweenYIntersections = Double.MAX_VALUE;
            _stepYDirection = 0;
        }

        // Reset some variables
        _rayLocation.set(start);
        _totalTravel = 0.0;
        _stepDirection = Direction.None;
    }

    @Override
    public void next() {
        // Walk us to our next location based on distances to next X or Y grid
        // line.
        if (_distToNextXIntersection < _distToNextYIntersection) {
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
            _totalTravel = _distToNextYIntersection;
            _gridLocation[1] += _stepYDirection;
            _distToNextYIntersection += _distBetweenYIntersections;
            switch (_stepYDirection) {
                case -1:
                    _stepDirection = Direction.NegativeY;
                    break;
                case 0:
                    _stepDirection = Direction.None;
                    break;
                case 1:
                    _stepDirection = Direction.PositiveY;
                    break;
            }
        }

        _rayLocation.set(_walkRay.getDirection()).multiplyLocal(_totalTravel).addLocal(_walkRay.getOrigin());
    }

    @Override
    public boolean isRayPerpendicularToGrid() {
        return _stepXDirection == 0 && _stepYDirection == 0;
    }

    @Override
    public Vector3 get3DPoint(final double gridX, final double gridY, final double height, final Vector3 store) {
        final Vector3 rVal = store != null ? store : new Vector3();

        return rVal.set(gridX, gridY, height);
    }

    @Override
    public Vector2 get2DPoint(final ReadOnlyVector3 worldLocation, final Vector2 store) {
        final Vector2 rVal = store != null ? store : new Vector2();

        return rVal.set(worldLocation.getX(), worldLocation.getY());
    }
}
