/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.util;

public class IntColorUtils {

  public static int getColor(final int r, final int g, final int b, final int a) {
    return r << 24 | g << 16 | b << 8 | a;
  }

  public static int getColor(final byte r, final byte g, final byte b, final byte a) {
    return getColor(r & 0xFF, g & 0xFF, b & 0xFF, a & 0xFF);
  }

  public static int lerp(final double percent, final int startColor, final int endColor) {
    if (startColor == endColor) {
      return startColor;
    } else if (percent <= 0.0) {
      return startColor;
    } else if (percent >= 1.0) {
      return endColor;
    }

    final int r = (int) ((1.0 - percent) * (startColor >> 24 & 0xFF) + percent * (endColor >> 24 & 0xFF));
    final int g = (int) ((1.0 - percent) * (startColor >> 16 & 0xFF) + percent * (endColor >> 16 & 0xFF));
    final int b = (int) ((1.0 - percent) * (startColor >> 8 & 0xFF) + percent * (endColor >> 8 & 0xFF));
    final int a = (int) ((1.0 - percent) * (startColor & 0xFF) + percent * (endColor & 0xFF));

    return r << 24 | g << 16 | b << 8 | a;
  }

  public static String toString(final int color) {
    final int r = color >> 24 & 0xFF;
    final int g = color >> 16 & 0xFF;
    final int b = color >> 8 & 0xFF;
    final int a = color & 0xFF;

    return "[r=" + r + ", g=" + g + ", b=" + b + ", a=" + a + "]";
  }
}
