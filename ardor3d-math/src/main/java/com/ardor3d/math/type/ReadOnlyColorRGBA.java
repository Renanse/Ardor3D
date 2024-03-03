/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.type;

import com.ardor3d.math.ColorRGBA;

public interface ReadOnlyColorRGBA {

  float getRed();

  float getGreen();

  float getBlue();

  float getAlpha();

  float getValue(int index);

  float[] toArray(float[] store);

  ColorRGBA clamp(ColorRGBA store);

  int asIntARGB();

  int asIntRGBA();

  ColorRGBA add(float r, float g, float b, float a, ColorRGBA store);

  ColorRGBA add(ReadOnlyColorRGBA source, ColorRGBA store);

  ColorRGBA subtract(float r, float g, float b, float a, ColorRGBA store);

  ColorRGBA subtract(ReadOnlyColorRGBA source, ColorRGBA store);

  ColorRGBA multiply(float scalar, ColorRGBA store);

  ColorRGBA multiply(ReadOnlyColorRGBA scale, ColorRGBA store);

  ColorRGBA divide(float scalar, ColorRGBA store);

  ColorRGBA divide(ReadOnlyColorRGBA scale, ColorRGBA store);

  ColorRGBA lerp(ReadOnlyColorRGBA endColor, float scalar, ColorRGBA store);

  String asHexRRGGBBAA();

  ColorRGBA clone();
}
