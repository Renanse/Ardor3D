/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.util;

import java.util.Random;

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

public class MathUtils {

  /** A "close to zero" double epsilon value for use */
  public static final double EPSILON = 2.220446049250313E-16d;

  /** A "close to zero" double epsilon value for use */
  public static final double ZERO_TOLERANCE = 0.0001;

  public static final double ONE_THIRD = 1.0 / 3.0;

  /** The value PI as a double. (180 degrees) */
  public static final double PI = Math.PI;

  /** The value PI^2 as a double. */
  public final static double SQUARED_PI = MathUtils.PI * MathUtils.PI;

  /** The value 2PI as a double. (360 degrees) */
  public static final double TWO_PI = 2.0 * MathUtils.PI;

  /** The value PI/2 as a double. (90 degrees) */
  public static final double HALF_PI = 0.5 * MathUtils.PI;

  /** The value PI/4 as a double. (45 degrees) */
  public static final double QUARTER_PI = 0.25 * MathUtils.PI;

  /** The value 3/4 PI as a double. (135 degrees) */
  public final static double THREE_PI_HALVES = MathUtils.TWO_PI - MathUtils.HALF_PI;

  /** The value 1/PI as a double. */
  public static final double INV_PI = 1.0 / MathUtils.PI;

  /** The value 1/(2PI) as a double. */
  public static final double INV_TWO_PI = 1.0 / MathUtils.TWO_PI;

  /** A value to multiply a degree value by, to convert it to radians. */
  public static final double DEG_TO_RAD = MathUtils.PI / 180.0;

  /** A value to multiply a radian value by, to convert it to degrees. */
  public static final double RAD_TO_DEG = 180.0 / MathUtils.PI;

  /** A precreated random object for random numbers. */
  public static final Random rand = new Random(System.currentTimeMillis());

  public static double inverseSqrt(final double value) {
    return 1.0 / Math.sqrt(value);
  }

  /**
   * Converts a point from Spherical coordinates to Cartesian (using positive Y as up) and stores the
   * results in the store var.
   *
   * @param sphereCoords
   *          (Radius, Azimuth, Polar)
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   */
  public static Vector3 sphericalToCartesian(final ReadOnlyVector3 sphereCoords, final Vector3 store) {
    final double a = sphereCoords.getX() * Math.cos(sphereCoords.getZ());
    final double x = a * Math.cos(sphereCoords.getY());
    final double y = sphereCoords.getX() * Math.sin(sphereCoords.getZ());
    final double z = a * Math.sin(sphereCoords.getY());

    Vector3 rVal = store;
    if (rVal == null) {
      rVal = new Vector3();
    }
    return rVal.set(x, y, z);
  }

  /**
   * Converts a point from Cartesian coordinates (using positive Y as up) to Spherical and stores the
   * results in the store var. (Radius, Azimuth, Polar)
   *
   * @param cartCoords
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   */
  public static Vector3 cartesianToSpherical(final ReadOnlyVector3 cartCoords, final Vector3 store) {
    final double cartX = Math.abs(cartCoords.getX()) <= MathUtils.EPSILON ? MathUtils.EPSILON : cartCoords.getX();
    final double cartY = cartCoords.getY();
    final double cartZ = cartCoords.getZ();

    final double x = Math.sqrt(cartX * cartX + cartY * cartY + cartZ * cartZ);
    final double y = Math.atan(cartZ / cartX) + (cartX < 0.0 ? MathUtils.PI : 0);
    final double z = Math.asin(cartY / x);

    Vector3 rVal = store;
    if (rVal == null) {
      rVal = new Vector3();
    }
    return rVal.set(x, y, z);
  }

  /**
   * Converts a point from Spherical coordinates to Cartesian (using positive Z as up) and stores the
   * results in the store var.
   *
   * @param sphereCoords
   *          (Radius, Azimuth, Polar)
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   */
  public static Vector3 sphericalToCartesianZ(final ReadOnlyVector3 sphereCoords, final Vector3 store) {
    final double a = sphereCoords.getX() * Math.cos(sphereCoords.getZ());
    final double x = a * Math.cos(sphereCoords.getY());
    final double y = a * Math.sin(sphereCoords.getY());
    final double z = sphereCoords.getX() * Math.sin(sphereCoords.getZ());

    Vector3 rVal = store;
    if (rVal == null) {
      rVal = new Vector3();
    }
    return rVal.set(x, y, z);
  }

  /**
   * Converts a point from Cartesian coordinates (using positive Z as up) to Spherical and stores the
   * results in the store var. (Radius, Azimuth, Polar)
   *
   * @param cartCoords
   * @param store
   *          the vector to store the result in for return. If null, a new vector object is created
   *          and returned.
   */
  public static Vector3 cartesianZToSpherical(final ReadOnlyVector3 cartCoords, final Vector3 store) {
    final double cartX = Math.abs(cartCoords.getX()) <= MathUtils.EPSILON ? MathUtils.EPSILON : cartCoords.getX();
    final double cartY = cartCoords.getY();
    final double cartZ = cartCoords.getZ();

    final double x = Math.sqrt(cartX * cartX + cartY * cartY + cartZ * cartZ);
    final double y = Math.asin(cartY / x);
    final double z = Math.atan(cartZ / cartX) + (cartX < 0.0 ? MathUtils.PI : 0);

    Vector3 rVal = store;
    if (rVal == null) {
      rVal = new Vector3();
    }
    return rVal.set(x, y, z);
  }

  /**
   * Returns true if the number is a power of 2 (2,4,8,16...)
   *
   * A good implementation found on the Java boards. note: a number is a power of two if and only if
   * it is the smallest number with that number of significant bits. Therefore, if you subtract 1, you
   * know that the new number will have fewer bits, so ANDing the original number with anything less
   * than it will give 0.
   *
   * @param number
   *          The number to test.
   * @return True if it is a power of two.
   */
  public static boolean isPowerOfTwo(final int number) {
    return number > 0 && (number & number - 1) == 0;
  }

  /**
   * @param number
   * @return the closest power of two to the given number.
   */
  public static int nearestPowerOfTwo(final int number) {
    return (int) Math.pow(2, Math.ceil(Math.log(number) / Math.log(2)));
  }

  /**
   * @param value
   * @param base
   * @return the logarithm of value with given base, calculated as log(value)/log(base) such that
   *         pow(base, return)==value
   */
  public static double log(final double value, final double base) {
    return Math.log(value) / Math.log(base);
  }

  /**
   * Sets the seed to use for "random" operations. The default is the current system milliseconds.
   *
   * @param seed
   */
  public static void setRandomSeed(final long seed) {
    MathUtils.rand.setSeed(seed);
  }

  /**
   * Returns a random double between 0 and 1.
   *
   * @return A random double between <tt>0.0</tt> (inclusive) to <tt>1.0</tt> (exclusive).
   */
  public static double nextRandomDouble() {
    return MathUtils.rand.nextDouble();
  }

  /**
   * Returns a random float between 0 and 1.
   *
   * @return A random float between <tt>0.0f</tt> (inclusive) to <tt>1.0f</tt> (exclusive).
   */
  public static float nextRandomFloat() {
    return MathUtils.rand.nextFloat();
  }

  /**
   * @return A random int between Integer.MIN_VALUE and Integer.MAX_VALUE.
   */
  public static int nextRandomInt() {
    return MathUtils.rand.nextInt();
  }

  /**
   * Returns a random int between min and max.
   *
   * @return A random int between <tt>min</tt> (inclusive) to <tt>max</tt> (inclusive).
   */
  public static int nextRandomInt(final int min, final int max) {
    return (int) (nextRandomFloat() * (max - min + 1)) + min;
  }

  /**
   *
   * @param percent
   * @param startValue
   * @param endValue
   * @return
   */
  public static float lerp(final float percent, final float startValue, final float endValue) {
    if (startValue == endValue) {
      return startValue;
    }
    return (1 - percent) * startValue + percent * endValue;
  }

  /**
   *
   * @param percent
   * @param startValue
   * @param endValue
   * @return
   */
  public static double lerp(final double percent, final double startValue, final double endValue) {
    if (startValue == endValue) {
      return startValue;
    }
    return (1 - percent) * startValue + percent * endValue;
  }

  /**
   * plot a given value on the cubic S-curve: 3t^2 - 2t^3
   *
   * @param t
   *          our input value
   * @return the plotted value
   */
  public static float scurve3(final float t) {
    final float t2 = t * t;
    final float t3 = t * t2;
    return 3f * t2 - 2f * t3;
  }

  /**
   * plot a given value on the cubic S-curve: 3t^2 - 2t^3
   *
   * @param t
   *          our input value
   * @return the plotted value
   */
  public static double scurve3(final double t) {
    final double t2 = t * t;
    final double t3 = t * t2;
    return 3. * t2 - 2. * t3;
  }

  /**
   * plot a given value on the quintic S-curve: 6t^5 - 15t^4 + 10t^3
   *
   * @param t
   *          our input value
   * @return the plotted value
   */
  public static float scurve5(final float t) {
    final float t3 = t * t * t;
    final float t4 = t * t3;
    final float t5 = t * t4;
    return 6f * t5 - 15f * t4 + 10f * t3;
  }

  /**
   * plot a given value on the quintic S-curve: 6t^5 - 15t^4 + 10t^3
   *
   * @param t
   *          our input value
   * @return the plotted value
   */
  public static double scurve5(final double t) {
    final double t3 = t * t * t;
    final double t4 = t * t3;
    final double t5 = t * t4;
    return 6. * t5 - 15. * t4 + 10. * t3;
  }

  /**
   *
   * @param left
   * @param right
   * @param bottom
   * @param top
   * @param nearZ
   * @param farZ
   * @param store
   */
  public static void matrixFrustum(final double left, final double right, final double bottom, final double top,
      final double nearZ, final double farZ, final Matrix4 store) {
    final double x = 2.0 * nearZ / (right - left);
    final double y = 2.0 * nearZ / (top - bottom);
    final double a = (right + left) / (right - left);
    final double b = (top + bottom) / (top - bottom);
    final double c = -(farZ + nearZ) / (farZ - nearZ);
    final double d = -(2.0 * farZ * nearZ) / (farZ - nearZ);

    store.set(x, 0.0, 0.0, 0.0, 0.0, y, 0.0, 0.0, a, b, c, -1.0, 0.0, 0.0, d, 0.0);
  }

  /**
   *
   * @param left
   * @param right
   * @param bottom
   * @param top
   * @param nearZ
   * @param farZ
   * @param store
   */
  public static void matrixOrtho(final double left, final double right, final double bottom, final double top,
      final double nearZ, final double farZ, final Matrix4 store) {
    store.set(2.0 / (right - left), 0.0, 0.0, 0.0, 0.0, 2.0 / (top - bottom), 0.0, 0.0, 0.0, 0.0, -2.0 / (farZ - nearZ),
        0.0, -(right + left) / (right - left), -(top + bottom) / (top - bottom), -(farZ + nearZ) / (farZ - nearZ), 1.0);
  }

  /**
   *
   * @param fovY
   * @param aspect
   * @param zNear
   * @param zFar
   * @param store
   */
  public static void matrixPerspective(final double fovY, final double aspect, final double zNear, final double zFar,
      final Matrix4 store) {
    final double height = zNear * Math.tan(fovY * 0.5 * MathUtils.DEG_TO_RAD);
    final double width = height * aspect;

    matrixFrustum(-width, width, -height, height, zNear, zFar, store);
  }

  /**
   *
   * @param position
   * @param target
   * @param up
   * @param store
   */
  public static void matrixLookAt(final ReadOnlyVector3 position, final ReadOnlyVector3 target,
      final ReadOnlyVector3 worldUp, final Matrix4 store) {
    final Vector3 direction = Vector3.fetchTempInstance();
    final Vector3 side = Vector3.fetchTempInstance();
    final Vector3 up = Vector3.fetchTempInstance();

    direction.set(target).subtractLocal(position).normalizeLocal();
    direction.cross(worldUp, side).normalizeLocal();
    side.cross(direction, up);

    store.set(side.getX(), up.getX(), -direction.getX(), 0.0, side.getY(), up.getY(), -direction.getY(), 0.0,
        side.getZ(), up.getZ(), -direction.getZ(), 0.0,
        side.getX() * -position.getX() + side.getY() * -position.getY() + side.getZ() * -position.getZ(),
        up.getX() * -position.getX() + up.getY() * -position.getY() + up.getZ() * -position.getZ(),
        -direction.getX() * -position.getX() + -direction.getY() * -position.getY()
            + -direction.getZ() * -position.getZ(),
        1.0);

    Vector3.releaseTempInstance(up);
    Vector3.releaseTempInstance(side);
    Vector3.releaseTempInstance(direction);
  }

  /**
   *
   * @param position
   * @param target
   * @param up
   * @param store
   */
  public static void matrixLookAt(final ReadOnlyVector3 position, final ReadOnlyVector3 target,
      final ReadOnlyVector3 worldUp, final Matrix3 store) {
    final Vector3 direction = Vector3.fetchTempInstance();
    final Vector3 side = Vector3.fetchTempInstance();
    final Vector3 up = Vector3.fetchTempInstance();

    direction.set(target).subtractLocal(position).normalizeLocal();
    direction.cross(worldUp, side).normalizeLocal();
    side.cross(direction, up);

    store.set(side.getX(), up.getX(), -direction.getX(), side.getY(), up.getY(), -direction.getY(), side.getZ(),
        up.getZ(), -direction.getZ());

    Vector3.releaseTempInstance(up);
    Vector3.releaseTempInstance(side);
    Vector3.releaseTempInstance(direction);
  }

  public static boolean approximately(final float a, final float b) {
    return Math.abs(b - a) < MathUtils.EPSILON;
  }

  /**
   * Faster floor function. Does not handle NaN and Infinity. (Not handled when doing Math.floor and
   * just casting anyways, so question is if we want to handle it or not)
   *
   * @param val
   *          Value to floor
   * @return Floored int value
   */
  public static int floor(final float val) {
    final int intVal = (int) val;
    return val < 0 ? val == intVal ? intVal : intVal - 1 : intVal;
  }

  /**
   * Faster floor function. Does not handle NaN and Infinity. (Not handled when doing Math.floor and
   * just casting anyways, so question is if we want to handle it or not)
   *
   * @param val
   *          Value to floor
   * @return Floored long value
   */
  public static long floor(final double val) {
    final long longVal = (long) val;
    return val < 0 ? val == longVal ? longVal : longVal - 1 : longVal;
  }

  public static int round(final float val) {
    return floor(val + 0.5f);
  }

  public static long round(final double val) {
    return floor(val + 0.5d);
  }

  public static double clamp(final double val, final double min, final double max) {
    return val < min ? min : val > max ? max : val;
  }

  public static float clamp(final float val, final float min, final float max) {
    return val < min ? min : val > max ? max : val;
  }

  public static int clamp(final int val, final int min, final int max) {
    return val < min ? min : val > max ? max : val;
  }

  public static double clamp01(final double val) {
    return clamp(val, 0.0, 1.0);
  }

  public static float clamp01(final float val) {
    return clamp(val, 0.0f, 1.0f);
  }

  public static int moduloPositive(final int value, final int size) {
    int wrappedValue = value % size;
    wrappedValue += wrappedValue < 0 ? size : 0;
    return wrappedValue;
  }

  public static float moduloPositive(final float value, final float size) {
    float wrappedValue = value % size;
    wrappedValue += wrappedValue < 0 ? size : 0;
    return wrappedValue;
  }

  public static double moduloPositive(final double value, final double size) {
    double wrappedValue = value % size;
    wrappedValue += wrappedValue < 0 ? size : 0;
    return wrappedValue;
  }

  /**
   * Simple 2^x
   *
   * @param x
   *          power
   * @return 2^x
   */
  public static int pow2(final int x) {
    if (x <= 0) {
      return 1;
    }
    return 2 << x - 1;
  }
}
