/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math;

import com.ardor3d.math.type.ReadOnlyLine3;
import com.ardor3d.math.type.ReadOnlyVector3;

import java.io.Serial;
import java.util.Objects;

public class Line3 extends Line3Base implements ReadOnlyLine3, Poolable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final ObjectPool<Line3> LINE3_POOL = ObjectPool.create(Line3.class, MathConstants.maxMathPoolSize);

  /**
   * Constructs a new line with an origin at (0,0,0) and a direction of (0,0,1).
   */
  public Line3() {
    super(Vector3.ZERO, Vector3.UNIT_Z);
  }

  /**
   * Constructs a new line using the supplied origin point and unit length direction vector
   *
   * @param origin
   *          the origin of the line.
   * @param direction
   *          the direction of the line. Should be of unit length.
   */
  public Line3(final ReadOnlyVector3 origin, final ReadOnlyVector3 direction) {
    super(origin, direction);
  }

  /**
   * Constructs a new line using the supplied source line
   *
   * @param source
   */
  public Line3(final ReadOnlyLine3 source) {
    super(source.getOrigin(), source.getDirection());
  }

  /**
   * Copies the values of the given source line into this line.
   *
   * @param source
   * @return this line for chaining
   * @throws NullPointerException
   *           if source is null.
   */
  public Line3 set(final ReadOnlyLine3 source) {
    _origin.set(source.getOrigin());
    _direction.set(source.getDirection());
    return this;
  }

  /**
   * @param point
   * @param store
   *          if not null, the closest point is stored in this param
   * @return the squared distance from this line to the given point.
   * @throws NullPointerException
   *           if the point is null.
   */
  @Override
  public double distanceSquared(final ReadOnlyVector3 point, final Vector3 store) {
    final Vector3 vectorA = Vector3.fetchTempInstance();
    vectorA.set(point).subtractLocal(_origin);

    // Note: assumes direction is normalized
    final double t0 = _direction.dot(vectorA);
    // d = |P - (O + t*D)|
    vectorA.set(_direction).multiplyLocal(t0);
    vectorA.addLocal(_origin);

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
   * Check a line... if it is null or the values of its origin or direction are NaN or infinite,
   * return false. Else return true.
   *
   * @param line
   *          the line to check
   * @return true or false as stated above.
   */
  public static boolean isFinite(final ReadOnlyLine3 line) {
    if (line == null) {
      return false;
    }

    return Vector3.isFinite(line.getDirection()) //
        && Vector3.isFinite(line.getOrigin());
  }

  /**
   * @return the string representation of this line.
   */
  @Override
  public String toString() {
    return "com.ardor3d.math.Line3 [Origin: " + _origin + " - Direction: " + _direction + "]";
  }

  /**
   * @param o
   *          the object to compare for equality
   * @return true if this line and the provided line have the same origin and direction values.
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReadOnlyLine3 comp)) {
      return false;
    }
    return Objects.equals(getOrigin(), comp.getOrigin()) //
        && Objects.equals(getDirection(), comp.getDirection());
  }

  // /////////////////
  // Method for Cloneable
  // /////////////////

  @Override
  public Line3 clone() {
    return new Line3(this);
  }

  // /////////////////
  // Methods for creating temp variables (pooling)
  // /////////////////

  /**
   * @return An instance of Line3 that is intended for temporary use in calculations and so forth.
   *         Multiple calls to the method should return instances of this class that are not currently
   *         in use.
   */
  public static Line3 fetchTempInstance() {
    if (MathConstants.useMathPools) {
      return Line3.LINE3_POOL.fetch();
    } else {
      return new Line3();
    }
  }

  /**
   * Releases a Line3 back to be used by a future call to fetchTempInstance. TAKE CARE: this Line3
   * object should no longer have other classes referencing it or "Bad Things" will happen.
   *
   * @param line
   *          the Line3 to release.
   */
  public static void releaseTempInstance(final Line3 line) {
    if (MathConstants.useMathPools) {
      Line3.LINE3_POOL.release(line);
    }
  }
}
