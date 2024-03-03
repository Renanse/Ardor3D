/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image;

public class TextureCubeMap extends Texture {

  private transient Face _currentRTTFace = Face.PositiveX;

  /**
   * Face of the Cubemap as described by its directional offset from the origin.
   */
  public enum Face {
    PositiveX, NegativeX, PositiveY, NegativeY, PositiveZ, NegativeZ;
  }

  @Override
  public Texture createSimpleClone() {
    return createSimpleClone(new TextureCubeMap());
  }

  @Override
  public Texture createSimpleClone(final Texture rVal) {
    if (rVal instanceof TextureCubeMap) {
      ((TextureCubeMap) rVal).setCurrentRTTFace(_currentRTTFace);
    }
    return super.createSimpleClone(rVal);
  }

  /**
   * Set the cubemap Face to use for the next Render To Texture operation (when used with
   * TextureRenderer.) NB: This field is transient - not saved by Savable.
   *
   * @param currentRTTFace
   *          the face to use
   */
  public void setCurrentRTTFace(final Face currentRTTFace) { _currentRTTFace = currentRTTFace; }

  /**
   * @return the cubemap Face to use for the next Render To Texture operation (when used with
   *         TextureRenderer.)
   */
  public Face getCurrentRTTFace() { return _currentRTTFace; }

  @Override
  public Type getType() { return Type.CubeMap; }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof TextureCubeMap)) {
      return false;
    }
    return super.equals(other);
  }

}
