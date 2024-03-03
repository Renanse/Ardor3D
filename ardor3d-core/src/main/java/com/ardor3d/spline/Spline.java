/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.spline;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Spline interface allows an interpolated vector to be calculated along a path.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Spline_(mathematics)">Spline (mathematics) -
 *      Wikipedia, the free encyclopedia</a>
 */
public interface Spline {
  /**
   * Will return an interpolated vector between parameters <code>p1</code> and <code>p2</code> using
   * <code>t</code>.
   * 
   * @param p0
   *          The starting control point.
   * @param p1
   *          The second control point.
   * @param p2
   *          The third control point.
   * @param p3
   *          The final control point.
   * @param t
   *          Should be between zero and one. Zero will return point <code>p1</code> while one will
   *          return <code>p2</code>, a value in between will return an interpolated vector between
   *          the two.
   * @return The interpolated vector.
   */
  Vector3 interpolate(ReadOnlyVector3 p0, ReadOnlyVector3 p1, ReadOnlyVector3 p2, ReadOnlyVector3 p3, double t);

  /**
   * Will return an interpolated vector between parameters <code>p1</code> and <code>p2</code> using
   * <code>t</code>.
   * 
   * @param p0
   *          The starting control point.
   * @param p1
   *          The second control point.
   * @param p2
   *          The third control point.
   * @param p3
   *          The final control point.
   * @param t
   *          Should be between zero and one. Zero will return point <code>p1</code> while one will
   *          return <code>p2</code>, a value in between will return an interpolated vector between
   *          the two.
   * @param result
   *          The interpolated values will be added to this vector.
   * @return The result vector passed in.
   */
  Vector3 interpolate(ReadOnlyVector3 p0, ReadOnlyVector3 p1, ReadOnlyVector3 p2, ReadOnlyVector3 p3, double t,
      Vector3 result);

}
