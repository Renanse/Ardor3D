/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.util.awt;

import java.awt.Color;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;

public class AwtColorUtil {

  public static ColorRGBA makeColorRGBA(final Color color) {
    if (color == null) {
      return new ColorRGBA(0, 0, 0, 1);
    }
    return new ColorRGBA(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f,
        color.getAlpha() / 255f);
  }

  public static Color makeColor(final ReadOnlyColorRGBA color) {
    return makeColor(color, true);
  }

  public static Color makeColor(final ReadOnlyColorRGBA color, final boolean useAlpha) {
    if (color == null) {
      return new Color(0, 0, 0, 1);
    }
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), useAlpha ? color.getAlpha() : 1.0f);
  }
}
