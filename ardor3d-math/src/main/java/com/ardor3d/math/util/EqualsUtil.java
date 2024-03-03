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

public class EqualsUtil {

  /**
   * @param a
   * @param b
   * @return true if a is equal to b, for purposes of class equality. Note that 0f and -0f are not
   *         considered equal.
   * @see java.lang.Float#equals(Object)
   */
  public static boolean equals(final float a, final float b) {
    return Float.floatToIntBits(a) == Float.floatToIntBits(b);
  }

  /**
   *
   * @param a
   * @param b
   * @param epsilon
   * @return true if abs(a - b) is less than or equals to epsilon, or in other words, the value a is
   *         "close enough" to the value b.
   */
  public static boolean equals(final float a, final float b, final float epsilon) {
    return Math.abs(a - b) <= epsilon;
  }

  /**
   * @param a
   * @param b
   * @return true if a is equal to b, for purposes of class equality. Note that 0.0 and -0.0 are not
   *         considered equal.
   * @see java.lang.Double#equals(Object)
   */
  public static boolean equals(final double a, final double b) {
    return Double.doubleToLongBits(a) == Double.doubleToLongBits(b);
  }

  /**
   *
   * @param a
   * @param b
   * @param epsilon
   * @return true if abs(a - b) is less than or equals to epsilon, or in other words, the value a is
   *         "close enough" to the value b.
   */
  public static boolean equals(final double a, final double b, final double epsilon) {
    return Math.abs(a - b) <= epsilon;
  }

}
