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

import com.ardor3d.math.MathUtils;

/**
 * A simple checker board pattern, with each unit cube alternating between -1 and 1 in value.
 */
public class CheckerFunction3D implements Function3D {

  @Override
  public double eval(final double x, final double y, final double z) {
    if ((MathUtils.floor(x) + MathUtils.floor(y) + MathUtils.floor(z)) % 2 == 0) {
      return -1;
    } else {
      return 1;
    }
  }
}
