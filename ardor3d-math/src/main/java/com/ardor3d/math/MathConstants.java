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

/**
 * Just a simple flag holder for runtime stripping of various ardor3d logging and debugging
 * features.
 */
public class MathConstants {

  public static final boolean useMathPools;

  public static final int maxMathPoolSize;

  static {
    boolean hasPropertyAccess = true;
    try {
      System.getProperty("ardor3d.noMathPools");
    } catch (final SecurityException e) {
      // It appears the user does not have permission to access System properties
      hasPropertyAccess = false;
    }

    if (hasPropertyAccess) {
      useMathPools = (System.getProperty("ardor3d.noMathPools") == null);
      maxMathPoolSize = (System.getProperty("ardor3d.maxMathPoolSize") != null
          ? Integer.parseInt(System.getProperty("ardor3d.maxMathPoolSize"))
          : 11);
    } else {
      useMathPools = true;
      maxMathPoolSize = 11;
    }
  }
}
