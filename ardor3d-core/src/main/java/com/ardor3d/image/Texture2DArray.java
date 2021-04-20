/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image;

public class Texture2DArray extends TextureArray {

  public static Texture2DArray from(final Texture2D... textures) {
    final var rVal = new Texture2DArray();
    copyTextureDataInto(rVal, textures);
    return rVal;
  }

  @Override
  public Texture createSimpleClone() {
    return createSimpleClone(new Texture2DArray());
  }

  @Override
  public Type getType() { return Type.TwoDimensionalArray; }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof Texture2DArray)) {
      return false;
    }
    return super.equals(other);
  }

}
