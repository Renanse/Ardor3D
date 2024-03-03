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

public class HashUtil {

  private static final int HASH_PRIME = 31;

  public static int hash(final int seed, final boolean value) {
    return HashUtil.HASH_PRIME * seed + (value ? 1 : 0);
  }

  public static int hash(final int seed, final char value) {
    return HashUtil.HASH_PRIME * seed + value;
  }

  public static int hash(final int seed, final short value) {
    return HashUtil.HASH_PRIME * seed + value;
  }

  public static int hash(final int seed, final int value) {
    return HashUtil.HASH_PRIME * seed + value;
  }

  public static int hash(final int seed, final long value) {
    return HashUtil.HASH_PRIME * seed + (int) (value ^ value >>> 32);
  }

  public static int hash(final int seed, final float value) {
    return hash(seed, Float.floatToIntBits(value));
  }

  public static int hash(final int seed, final double value) {
    return hash(seed, Double.doubleToLongBits(value));
  }

  public static int hash(final int seed, final Object value) {
    if (value == null) {
      return hash(seed, 0);
    } else if (!value.getClass().isArray()) {
      return hash(seed, value.hashCode());
    }
    throw new IllegalArgumentException("hash of arrays is not currently supported");
  }

}
