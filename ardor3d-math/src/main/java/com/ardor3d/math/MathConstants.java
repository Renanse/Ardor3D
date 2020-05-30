/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
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

  public static final boolean useFastMath;

  public static final int maxMathPoolSize;

  static {
    boolean hasPropertyAccess = true;
    try {
      if (System.getSecurityManager() != null) {
        System.getSecurityManager().checkPropertiesAccess();
      }
    } catch (final SecurityException e) {
      hasPropertyAccess = false;
    }

    if (hasPropertyAccess) {
      useMathPools = (System.getProperty("ardor3d.noMathPools") == null);
      useFastMath = (System.getProperty("ardor3d.useFastMath") != null);
      maxMathPoolSize = (System.getProperty("ardor3d.maxMathPoolSize") != null
          ? Integer.parseInt(System.getProperty("ardor3d.maxMathPoolSize"))
          : 11);
    } else {
      useMathPools = true;
      useFastMath = false;
      maxMathPoolSize = 11;
    }
  }
}
