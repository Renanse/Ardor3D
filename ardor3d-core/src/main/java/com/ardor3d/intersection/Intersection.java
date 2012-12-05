/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.intersection;

import com.ardor3d.math.Vector3;

/**
 * <code>Intersection</code> provides functional methods for calculating the intersection of some objects. All the
 * methods are static to allow for quick and easy calls. <code>Intersection</code> relays requests to specific classes
 * to handle the actual work. By providing checks to just <code>BoundingVolume</code> the client application need not
 * worry about what type of bounding volume is being used.
 */
public abstract class Intersection {

    public static boolean intersection(final Vector3[] verticesA, final Vector3[] verticesB) {
        switch (verticesA.length) {
            case 3:
                switch (verticesB.length) {
                    case 3:
                        // Triangle on Triangle
                        return TriangleTriangleIntersect.intersectTriTri(verticesA[0], verticesA[1], verticesA[2],
                                verticesB[0], verticesB[1], verticesB[2]);
                    case 4:
                        // TODO: Triangle on Quad
                        return false;
                    case 2:
                        // TODO: Triangle on Line
                        return false;
                    case 1:
                        // TODO: Triangle on Point
                        return false;
                }
            case 4:
                switch (verticesB.length) {
                    case 3:
                        // TODO: Quad on Triangle
                        return false;
                    case 4:
                        // TODO: Quad on Quad
                        return false;
                    case 2:
                        // TODO: Quad on Line
                        return false;
                    case 1:
                        // TODO: Quad on Point
                        return false;
                }
            case 2:
                switch (verticesB.length) {
                    case 3:
                        // TODO: Line on Triangle
                        return false;
                    case 4:
                        // TODO: Line on Quad
                        return false;
                    case 2:
                        // TODO: Line on Line
                        return false;
                    case 1:
                        // TODO: Line on Point
                        return false;
                }
            case 1:
                switch (verticesB.length) {
                    case 3:
                        // TODO: Point on Triangle
                        return false;
                    case 4:
                        // TODO: Point on Quad
                        return false;
                    case 2:
                        // TODO: Point on Line
                        return false;
                    case 1:
                        // Point on Point
                        return verticesA[0].equals(verticesB[0]);
                }
        }
        return false;
    }
}