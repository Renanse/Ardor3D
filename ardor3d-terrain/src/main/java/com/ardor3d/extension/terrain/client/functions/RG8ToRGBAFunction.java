/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client.functions;

import java.nio.ByteBuffer;

public class RG8ToRGBAFunction {

  private static RG8ToRGBAFunction INSTANCE;

  public static RG8ToRGBAFunction getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RG8ToRGBAFunction();
    }
    return INSTANCE;
  }

  public void doConversion(final ByteBuffer sourceData, final byte[] store, final int destX, final int destY,
      final int dataSize, final int tileSize) {
    final int offset = (destY * tileSize * dataSize + destX * tileSize) * 4;
    try {
      int destIndex = offset;
      int sourceIndex = 0;
      for (int y = 0; y < tileSize; y++) {
        for (int x = 0; x < tileSize; x++) {
          final byte sourceValue = sourceData.get(sourceIndex++);
          final byte sourceAlpha = sourceData.get(sourceIndex++);
          store[destIndex++] = sourceValue;
          store[destIndex++] = sourceValue;
          store[destIndex++] = sourceValue;
          store[destIndex++] = sourceAlpha;
        }
        destIndex += (dataSize - tileSize) * 4;
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }
}
