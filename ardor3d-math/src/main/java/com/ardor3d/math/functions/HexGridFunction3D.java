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

import com.ardor3d.math.util.MathUtils;

/**
 * Creates a hexagon pattern in the XY plane. This is not really 3D, since it ignores the z factor.
 */
public class HexGridFunction3D extends GridPatternFunction3D {

  private static final double EQUILATERAL_TRIANGLE_HEIGHT = Math.sqrt(3.0) / 2.0;

  private static final double[][] FIXED_VALUES = new double[6][6];

  static {
    FIXED_VALUES[0][0] = FIXED_VALUES[5][0] = -1;
    FIXED_VALUES[0][1] = FIXED_VALUES[5][1] = 0;
    FIXED_VALUES[0][2] = FIXED_VALUES[5][2] = 0;
    FIXED_VALUES[0][3] = FIXED_VALUES[5][3] = 1;
    FIXED_VALUES[0][4] = FIXED_VALUES[5][4] = 1;
    FIXED_VALUES[0][5] = FIXED_VALUES[5][5] = -1;

    FIXED_VALUES[2][0] = FIXED_VALUES[3][0] = 1;
    FIXED_VALUES[2][1] = FIXED_VALUES[3][1] = 1;
    FIXED_VALUES[2][2] = FIXED_VALUES[3][2] = -1;
    FIXED_VALUES[2][3] = FIXED_VALUES[3][3] = -1;
    FIXED_VALUES[2][4] = FIXED_VALUES[3][4] = 0;
    FIXED_VALUES[2][5] = FIXED_VALUES[3][5] = 0;

    FIXED_VALUES[1][0] = FIXED_VALUES[4][0] = -1;
    FIXED_VALUES[1][1] = FIXED_VALUES[4][1] = 1;
    FIXED_VALUES[1][2] = FIXED_VALUES[4][2] = 0;
    FIXED_VALUES[1][3] = FIXED_VALUES[4][3] = -1;
    FIXED_VALUES[1][4] = FIXED_VALUES[4][4] = 1;
    FIXED_VALUES[1][5] = FIXED_VALUES[4][5] = 0;
  }

  private static final double[] ALTERNATE_VALUES = {1, 0, -1, 1, 0, -1};

  public HexGridFunction3D() {
    super(FIXED_VALUES, 0.5, EQUILATERAL_TRIANGLE_HEIGHT);
  }

  @Override
  protected double getCellValue(final int gridX, final int gridY, final double scaledX, final double scaledY) {
    double value = 0.0;

    switch (gridX) {
      case 0:
      case 2:
      case 3:
      case 5:
        value = super.getCellValue(gridX, gridY, scaledX, scaledY);
        break;
      case 1:
      case 4:
        double x = scaledX - gridX;
        final double y = scaledY - gridY;

        // If a grid block has a negative slope, flip it.
        if (((gridX + gridY) % 2) != 0) {
          x = 1.0 - x;
        }

        if (x == 0.0) {
          x = MathUtils.ZERO_TOLERANCE;
        }

        final boolean lowAngle = ((y / x) < 1.0);
        if (lowAngle) {
          value = super.getCellValue(gridX, gridY, scaledX, scaledY);
        } else {
          value = ALTERNATE_VALUES[gridY];
        }
        break;
    }

    return value;
  }
}
