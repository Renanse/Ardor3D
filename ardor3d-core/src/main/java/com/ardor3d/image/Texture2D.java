/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class Texture2D extends Texture {

  private WrapMode _wrapS = WrapMode.Repeat;
  private WrapMode _wrapT = WrapMode.Repeat;

  @Override
  public Texture createSimpleClone() {
    return createSimpleClone(new Texture2D());
  }

  @Override
  public Texture createSimpleClone(final Texture rVal) {
    rVal.setWrap(WrapAxis.S, _wrapS);
    rVal.setWrap(WrapAxis.T, _wrapT);
    return super.createSimpleClone(rVal);
  }

  /**
   * <code>setWrap</code> sets the wrap mode of this texture for a particular axis.
   *
   * @param axis
   *          the texture axis to define a wrapmode on.
   * @param mode
   *          the wrap mode for the given axis of the texture.
   * @throws IllegalArgumentException
   *           if axis or mode are null or invalid
   */
  @Override
  public void setWrap(final WrapAxis axis, final WrapMode mode) {
    if (mode == null) {
      throw new IllegalArgumentException("mode can not be null.");
    } else if (axis == null) {
      throw new IllegalArgumentException("axis can not be null.");
    }
    switch (axis) {
      case S:
        _wrapS = mode;
        break;
      case T:
        _wrapT = mode;
        break;
      case R:
      default:
        throw new IllegalArgumentException("invalid WrapAxis: " + axis);
    }
  }

  /**
   * <code>setWrap</code> sets the wrap mode of this texture for all axis.
   *
   * @param mode
   *          the wrap mode for the given axis of the texture.
   * @throws IllegalArgumentException
   *           if mode is null
   */
  @Override
  public void setWrap(final WrapMode mode) {
    if (mode == null) {
      throw new IllegalArgumentException("mode can not be null.");
    }
    _wrapS = mode;
    _wrapT = mode;
  }

  /**
   * <code>getWrap</code> returns the wrap mode for a given coordinate axis on this texture.
   *
   * @param axis
   *          the axis to return for
   * @return the wrap mode of the texture.
   * @throws IllegalArgumentException
   *           if axis is null
   */
  @Override
  public WrapMode getWrap(final WrapAxis axis) {
    switch (axis) {
      case S:
        return _wrapS;
      case T:
        return _wrapT;
      case R:
      default:
        throw new IllegalArgumentException("invalid WrapAxis: " + axis);
    }
  }

  @Override
  public Type getType() { return Type.TwoDimensional; }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof Texture2D)) {
      return false;
    }
    final Texture2D that = (Texture2D) other;
    if (getWrap(WrapAxis.S) != that.getWrap(WrapAxis.S)) {
      return false;
    }
    if (getWrap(WrapAxis.T) != that.getWrap(WrapAxis.T)) {
      return false;
    }
    return super.equals(other);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_wrapS, "wrapS", WrapMode.EdgeClamp);
    capsule.write(_wrapT, "wrapT", WrapMode.EdgeClamp);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _wrapS = capsule.readEnum("wrapS", WrapMode.class, WrapMode.EdgeClamp);
    _wrapT = capsule.readEnum("wrapT", WrapMode.class, WrapMode.EdgeClamp);
  }
}
