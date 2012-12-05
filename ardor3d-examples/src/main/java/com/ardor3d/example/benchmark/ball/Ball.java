/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.benchmark.ball;

public class Ball {
    protected final static int radius = 26;

    protected final double elastity = -.02;
    protected final double maxSpeed = 3.0;

    protected double _x = 0;
    protected double _y = 0;
    protected double _vx = 0;
    protected double _vy = 0;
    protected double _r = 0;
    protected double _d = 0;
    protected double _d2 = 0;

    public Ball() {
        // default provisioning
        _vx = 2 * maxSpeed * Math.random() - maxSpeed;
        _vy = 2 * maxSpeed * Math.random() - maxSpeed;
        _r = radius; // d = 52 px
        _d = 2 * _r;
        _d2 = _d * _d;
    }

    public void setRandomPositionIn(final int areaWidth, final int areaHeight) {
        _x = (areaWidth - 2 * radius) * Math.random();
        _y = (areaHeight - 2 * radius) * Math.random();
    }

    public void move(final int areaWidth, final int areaHeight) {

        _x += _vx;
        _y += _vy;

        // wall collisions

        // left
        if (_x < 0 && _vx < 0) {
            // _vx += _x * elastity;
            _vx = -_vx;
        }
        // top
        if (_y < 0 && _vy < 0) {
            // _vy += _y * elastity;
            _vy = -_vy;
        }
        // right
        if (_x > areaWidth - _d && _vx > 0) {
            // _vx += (_x - areaWidth + _d) * elastity;
            _vx = -_vx;
        }
        // bottom
        if (_y > areaHeight - _d && _vy > 0) {
            // _vy += (_y - areaHeight + _d) * elastity;
            _vy = -_vy;
        }
    }

    public boolean doCollide(final Ball b) {
        // calculate some vectors
        final double dx = _x - b._x;
        final double dy = _y - b._y;
        final double dvx = _vx - b._vx;
        final double dvy = _vy - b._vy;
        final double distance2 = dx * dx + dy * dy;

        if (Math.abs(dx) > _d || Math.abs(dy) > _d) {
            return false;
        }
        if (distance2 > _d2) {
            return false;
        }

        // make absolutely elastic collision
        double mag = dvx * dx + dvy * dy;

        // test that balls move towards each other
        if (mag > 0) {
            return false;
        }

        mag /= distance2;

        final double delta_vx = dx * mag;
        final double delta_vy = dy * mag;

        _vx -= delta_vx;
        _vy -= delta_vy;

        b._vx += delta_vx;
        b._vy += delta_vy;

        return true;
    }
}