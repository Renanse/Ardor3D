/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;

public class CharacterDescriptor {

  public static final CharacterDescriptor CR = new CharacterDescriptor('\n', 0, 0, 0, 0, 0, 0, 0, 1.0, null);

  /** The character id, i.e. char value, of this character. */
  private final char _id;

  /** The x location, in pixels, of our upper left corner */
  private final int _x;
  /** The y location, in pixels, of our upper left corner */
  private final int _y;

  /** The width, in pixels, of our part of the texture */
  private final int _width;
  /** The height, in pixels, of our part of the texture */
  private final int _height;

  /** pixels to advance the pen by when drawing this character */
  private final int _xAdvance;

  /** offset from character origin to draw this character */
  private final int _xOffset, _yOffset;

  /** scaling to apply - optional */
  private double _scale = 1;

  /** tint to apply - optional */
  private ColorRGBA _tint = null;

  public CharacterDescriptor(final char id, final int x, final int y, final int width, final int height,
    final int xAdvance) {
    this(id, xAdvance, y, width, height, xAdvance, 0, 0, 1, null);
  }

  public CharacterDescriptor(final CharacterDescriptor source) {
    this(source._id, source._x, source._y, source._width, source._height, source._xAdvance, source._xOffset,
        source._yOffset, source._scale, source._tint);
  }

  public CharacterDescriptor(final char id, final int x, final int y, final int width, final int height,
    final int xAdvance, final int xOffset, final int yOffset, final double scale, final ReadOnlyColorRGBA tint) {
    _id = id;
    _x = x;
    _y = y;
    _width = width;
    _height = height;
    _xAdvance = xAdvance;
    _xOffset = xOffset;
    _yOffset = yOffset;
    setTint(tint);
  }

  public int getXAdvance() { return _xAdvance; }

  public int getXOffset() { return _xOffset; }

  public int getYOffset() { return _yOffset; }

  public int getX() { return _x; }

  public int getY() { return _y; }

  public int getHeight() { return _height; }

  public int getWidth() { return _width; }

  public double getScale() { return _scale; }

  public void setScale(final double scale) { _scale = scale; }

  public ReadOnlyColorRGBA getTint() { return _tint; }

  public void setTint(final ReadOnlyColorRGBA tint) {
    if (tint == null) {
      _tint = null;
    } else if (_tint == null) {
      _tint = new ColorRGBA(tint);
    } else {
      _tint.set(tint);
    }
  }

  public void setTintAlpha(final float alpha) {
    if (_tint == null) {
      _tint = new ColorRGBA(1.0f, 1.0f, 1.0f, alpha);
    } else {
      _tint.setAlpha(alpha);
    }
  }
}
