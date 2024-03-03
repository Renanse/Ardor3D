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

public class Texture1D extends Texture {

  @Override
  public Texture createSimpleClone() {
    return createSimpleClone(new Texture1D());
  }

  @Override
  public Type getType() { return Type.OneDimensional; }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof Texture1D)) {
      return false;
    }
    return super.equals(other);
  }

}
