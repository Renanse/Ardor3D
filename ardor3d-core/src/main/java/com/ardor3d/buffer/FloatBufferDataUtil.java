/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.buffer;

import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;

public class FloatBufferDataUtil {
  public static FloatBufferData makeNew(final ReadOnlyVector2[] coords) {
    if (coords == null) {
      return null;
    }

    return new FloatBufferData(BufferUtils.createFloatBuffer(coords), 2);
  }

  public static FloatBufferData makeNew(final ReadOnlyVector3[] coords) {
    if (coords == null) {
      return null;
    }

    return new FloatBufferData(BufferUtils.createFloatBuffer(coords), 3);
  }

  public static FloatBufferData makeNew(final float[] coords) {
    if (coords == null) {
      return null;
    }

    return new FloatBufferData(BufferUtils.createFloatBuffer(coords), 1);
  }
}
