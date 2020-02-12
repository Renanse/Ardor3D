/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.functions;

import java.util.HashMap;
import java.util.Map;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;

/**
 * Function that produces a <a href="http://en.wikipedia.org/wiki/Voronoi_diagram">Voronoi graph</a> by placing a random
 * point in every 1x1x1 unit cube in space and then finding the closest of these points at each eval location.
 * 
 */
public class VoroniFunction3D implements Function3D {

    private static final int SEARCH_RADIUS = 2; // can miss corner cases with only a radius of 1

    private double _frequency = 1;
    private boolean _useDistance = false;
    private double _displacement = 1;
    private int _seed = 0;

    // A cache for cube values
    private final Map<Key, Vector3> _points = new HashMap<Key, Vector3>();

    /**
     * Construct with default values.
     */
    public VoroniFunction3D() {}

    /**
     * Construct a new Voronoi graph function.
     * 
     * @param frequency
     *            used to modulate input coordinates
     * @param displacement
     *            use to modulate the contribution of the random points in each unit cube.
     * @param useDistance
     *            if true, we will add the distance from the closest point to our output, giving us variation across the
     *            cell.
     * @param seed
     *            the random seed value to give to our integer random function.
     */
    public VoroniFunction3D(final double frequency, final double displacement, final boolean useDistance, final int seed) {
        _frequency = frequency;
        _displacement = displacement;
        _useDistance = useDistance;
        _seed = seed;
    }

    public double eval(final double x, final double y, final double z) {
        final double dx = x * _frequency, dy = y * _frequency, dz = z * _frequency;

        // find which integer based unit cube we're in
        final int ix = (int) MathUtils.floor(dx), iy = (int) MathUtils.floor(dy), iz = (int) MathUtils.floor(dz);

        final Key k = new Key();
        final Vector3 minPoint = new Vector3();
        double nearestSq = Double.MAX_VALUE;
        // Each cube has a point... Walk through all nearby cubes and see where our closest point lies.
        for (int a = ix - SEARCH_RADIUS; a <= ix + SEARCH_RADIUS; a++) {
            k.x = a;
            for (int b = iy - SEARCH_RADIUS; b <= iy + SEARCH_RADIUS; b++) {
                k.y = b;
                for (int c = iz - SEARCH_RADIUS; c <= iz + SEARCH_RADIUS; c++) {
                    k.z = c;
                    Vector3 point = _points.get(k);
                    if (point == null) {
                        final double pX = a + point(a, b, c, _seed);
                        final double pY = b + point(a, b, c, _seed + 1);
                        final double pZ = c + point(a, b, c, _seed + 2);
                        point = new Vector3(pX, pY, pZ);
                        // cache for future lookups
                        _points.put(new Key(k), point);
                    }
                    final double xDist = point.getX() - dx;
                    final double yDist = point.getY() - dy;
                    final double zDist = point.getZ() - dz;
                    final double distSq = xDist * xDist + yDist * yDist + zDist * zDist;

                    // check distance
                    if (distSq < nearestSq) {
                        nearestSq = distSq;
                        minPoint.set(point);
                    }
                }
            }
        }

        double value;
        if (_useDistance) {
            // Determine the distance to the nearest point.
            value = MathUtils.sqrt(nearestSq);
        } else {
            value = 0.0;
        }

        // Return the calculated distance + with the displacement value applied using the value of our cube.
        return value
                + (_displacement * point(MathUtils.floor(minPoint.getXf()), MathUtils.floor(minPoint.getYf()),
                        MathUtils.floor(minPoint.getZf()), 0));
    }

    /**
     * Calc the "random" point in unit cube that starts at the given coords.
     */
    private double point(final int unitX, final int unitY, final int unitZ, final int seed) {
        int n = (4241 * unitX + 7817 * unitY + 38261 * unitZ + 1979 * seed) & 0x7fffffff;
        n = (n >> 13) ^ n;
        return 1.0 - ((n * (n * n * 15731 + 789221) + 1376312589) & 0x7fffffff) / (double) 0x40000000;
    }

    public double getFrequency() {
        return _frequency;
    }

    public void setFrequency(final double frequency) {
        _frequency = frequency;
    }

    public boolean isUseDistance() {
        return _useDistance;
    }

    public void setUseDistance(final boolean useDistance) {
        _useDistance = useDistance;
    }

    public double getDisplacement() {
        return _displacement;
    }

    public void setDisplacement(final double displacement) {
        _displacement = displacement;
    }

    public int getSeed() {
        return _seed;
    }

    public void setSeed(final int seed) {
        _seed = seed;
    }

    private static class Key {
        int x, y, z;

        public Key() {}

        public Key(final Key k) {
            x = k.x;
            y = k.y;
            z = k.z;
        }

        @Override
        public int hashCode() {
            int result = 17;

            result += 31 * result + x;
            result += 31 * result + y;
            result += 31 * result + z;

            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key)) {
                return false;
            }
            final Key comp = (Key) o;
            return x == comp.x && y == comp.y && z == comp.z;
        }
    }
}
