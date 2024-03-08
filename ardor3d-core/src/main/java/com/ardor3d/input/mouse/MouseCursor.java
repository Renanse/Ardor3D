/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.mouse;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;

import java.util.Objects;

/**
 * An immutable representation of a mouse cursor. A mouse cursor consists of an image and a hotspot
 * where clicking is done.
 *
 */
@Immutable
public class MouseCursor {
  /**
   * This constant is used to identify that the native operating system's default cursor should be
   * used. It is not a valid mouse cursor in itself.
   */
  public static final MouseCursor SYSTEM_DEFAULT = new MouseCursor("system default",
      new Image(ImageDataFormat.RGBA, PixelDataType.UnsignedByte, 1, 1, BufferUtils.createByteBuffer(4), null), 0, 0);

  private final String _name;
  private final Image _image;
  private final int _hotspotX;
  private final int _hotspotY;

  /**
   * Instantiates a MouseCursor.
   * 
   * @param name
   *          the name of this cursor, for debugging purposes.
   * @param image
   *          the image that will be shown when this cursor is active.
   * @param hotspotX
   *          the X coordinate of the clicking hotspot, 0 = left side
   * @param hotspotY
   *          the Y coordinate of the clicking hotspot, 0 = bottom
   */
  public MouseCursor(final String name, final Image image, final int hotspotX, final int hotspotY) {
    _name = name;
    _image = image;
    _hotspotX = hotspotX;
    _hotspotY = hotspotY;

    if (hotspotX < 0 || hotspotX >= image.getWidth()) {
      throw new IllegalArgumentException("hotspot X is out of bounds: 0 <= " + hotspotX + " < " + image.getWidth());
    }
    if (hotspotY < 0 || hotspotY >= image.getHeight()) {
      throw new IllegalArgumentException("hotspot Y is out of bounds: 0 <= " + hotspotY + " < " + image.getHeight());
    }
  }

  public String getName() { return _name; }

  public Image getImage() { return _image; }

  public int getWidth() { return _image.getWidth(); }

  public int getHeight() { return _image.getHeight(); }

  public int getHotspotX() { return _hotspotX; }

  public int getHotspotY() { return _hotspotY; }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final MouseCursor that = (MouseCursor) o;

    if (_hotspotX != that._hotspotX) {
      return false;
    }
    if (_hotspotY != that._hotspotY) {
      return false;
    }
    if (!Objects.equals(_image, that._image)) {
      return false;
    }
    // noinspection RedundantIfStatement
    if (!Objects.equals(_name, that._name)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = _name != null ? _name.hashCode() : 0;
    result = 31 * result + (_image != null ? _image.hashCode() : 0);
    result = 31 * result + _hotspotX;
    result = 31 * result + _hotspotY;
    return result;
  }
}
