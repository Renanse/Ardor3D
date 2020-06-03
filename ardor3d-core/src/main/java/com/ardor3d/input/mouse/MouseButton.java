/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.mouse;

import java.util.EnumMap;

public enum MouseButton {
  LEFT, RIGHT, MIDDLE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, UNKNOWN;

  public static EnumMap<MouseButton, ButtonState> makeMap(final ButtonState left, final ButtonState right,
      final ButtonState middle) {
    if (left == null) {
      throw new IllegalArgumentException("left was null");
    }
    if (right == null) {
      throw new IllegalArgumentException("right was null");
    }
    if (middle == null) {
      throw new IllegalArgumentException("middle was null");
    }
    final EnumMap<MouseButton, ButtonState> map = new EnumMap<>(MouseButton.class);
    map.put(LEFT, left);
    map.put(RIGHT, right);
    map.put(MIDDLE, middle);
    return map;
  }
}
