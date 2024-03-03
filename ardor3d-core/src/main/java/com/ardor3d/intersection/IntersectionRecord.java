/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.intersection;

import java.util.Arrays;
import java.util.List;

import com.ardor3d.math.Vector3;
import com.ardor3d.util.Ardor3dException;

public class IntersectionRecord {

  private static final class Intersection implements Comparable<Intersection> {
    private final double _distance;
    private final Vector3 _point;
    private final Vector3 _normal;
    private final PrimitiveKey _primitiveKey;

    private Intersection(final double distance, final Vector3 point, final Vector3 normal,
      final PrimitiveKey primitiveKey) {
      _distance = distance;
      _point = point;
      _normal = normal;
      _primitiveKey = primitiveKey;
    }

    @Override
    public int compareTo(final Intersection other) {
      return Double.compare(_distance, other._distance);
    }
  }

  private final Intersection[] _intersections;

  private boolean _isSorted = true;

  /**
   * Instantiates a new IntersectionRecord defining the distances and points.
   * 
   * @param distances
   *          the distances of this intersection.
   * @param points
   *          the points of this intersection.
   * @throws Ardor3dException
   *           if distances.length != points.length
   */
  public IntersectionRecord(final double[] distances, final Vector3[] points) {
    this(distances, points, null);
  }

  /**
   * Instantiates a new IntersectionRecord defining the distances and points.
   * 
   * @param distances
   *          the distances of this intersection.
   * @param points
   *          the points of this intersection.
   * @param primitives
   *          the primitives at each index. May be null.
   * @throws Ardor3dException
   *           if distances.length != points.length or points.length != primitives.size() (if
   *           primitives is not null)
   */
  public IntersectionRecord(final double[] distances, final Vector3[] points, final List<PrimitiveKey> primitives) {
    this(distances, points, null, primitives);
  }

  /**
   * Instantiates a new IntersectionRecord defining the distances and points.
   * 
   * @param distances
   *          the distances of this intersection.
   * @param points
   *          the points of this intersection.
   * @param points
   *          the normals of this intersection.
   * @param primitives
   *          the primitives at each index. May be null.
   * @throws Ardor3dException
   *           if distances.length != points.length or points.length != primitives.size() (if
   *           primitives is not null)
   */
  public IntersectionRecord(final double[] distances, final Vector3[] points, final Vector3[] normals,
    final List<PrimitiveKey> primitives) {
    if (distances.length != points.length || (primitives != null && points.length != primitives.size())
        || (normals != null && points.length != normals.length)) {
      throw new Ardor3dException("All arguments must have an equal number of elements.");
    }
    _isSorted = distances.length < 2;
    _intersections = new Intersection[distances.length];
    for (int i = 0; i < distances.length; i++) {
      _intersections[i] = new Intersection(distances[i], points[i], normals != null ? normals[i] : null,
          primitives != null ? primitives.get(i) : null);
    }
  }

  /**
   * Sorts intersections from near to far
   */
  public void sortIntersections() {
    if (!_isSorted) {
      Arrays.sort(_intersections);
      _isSorted = true;
    }
  }

  /**
   * @return the number of intersections that occurred.
   */
  public int getNumberOfIntersections() { return _intersections.length; }

  /**
   * Returns an intersection point at a provided index.
   * 
   * @param index
   *          the index of the point to obtain.
   * @return the point at the index of the array.
   */
  public Vector3 getIntersectionPoint(final int index) {
    return _intersections[index]._point;
  }

  /**
   * Returns an intersection normal at a provided index.
   * 
   * @param index
   *          the index of the point to obtain.
   * @return the normal at the index of the array.
   */
  public Vector3 getIntersectionNormal(final int index) {
    return _intersections[index]._normal;
  }

  /**
   * Returns an intersection distance at a provided index.
   * 
   * @param index
   *          the index of the distance to obtain.
   * @return the distance at the index of the array.
   */
  public double getIntersectionDistance(final int index) {
    return _intersections[index]._distance;
  }

  /**
   * @param index
   *          the index of the primitive to obtain.
   * @return the primitive at the given index.
   */
  public PrimitiveKey getIntersectionPrimitive(final int index) {
    return _intersections[index]._primitiveKey;
  }

  /**
   * @return the smallest distance in the distance array or -1 if there are no distances in this.
   */
  public double getClosestDistance() {
    final int i = getClosestIntersection();
    return i != -1 ? _intersections[i]._distance : -1.0;
  }

  /**
   * @return the largest distance in the distance array or -1 if there are no distances in this.
   */
  public double getFurthestDistance() {
    final int i = getFurthestIntersection();
    return i != -1 ? _intersections[i]._distance : -1.0;
  }

  /**
   * @return the index in this record with the smallest relative distance or -1 if there are no
   *         distances in this record.
   */
  public int getClosestIntersection() {
    int index = -1;
    if (_isSorted) {
      index = _intersections.length > 0 ? 0 : -1;
    } else {
      double min = Double.MAX_VALUE;
      for (int i = _intersections.length; --i >= 0;) {
        final double val = _intersections[i]._distance;
        if (val < min) {
          min = val;
          index = i;
        }
      }
    }
    return index;
  }

  /**
   * @return the index in this record with the largest relative distance or -1 if there are no
   *         distances in this record.
   */
  public int getFurthestIntersection() {
    int index = -1;
    if (_isSorted) {
      index = _intersections.length > 0 ? _intersections.length - 1 : -1;
    } else {
      double max = -Double.MAX_VALUE;
      for (int i = _intersections.length; --i >= 0;) {
        final double val = _intersections[i]._distance;
        if (val > max) {
          max = val;
          index = i;
        }
      }
    }
    return index;
  }

}
