/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.state.record;

/**
 * Represents a texture unit in opengl
 */
public class TextureUnitRecord extends StateRecord {
  public int boundTexture = -1;
  public float lodBias = 0f;

  public TextureUnitRecord() {}

  @Override
  public void invalidate() {
    super.invalidate();

    boundTexture = -1;
    lodBias = 0;
  }
}
