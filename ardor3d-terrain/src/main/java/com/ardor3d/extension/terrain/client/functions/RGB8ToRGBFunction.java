/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client.functions;

import java.nio.ByteBuffer;

public class RGB8ToRGBFunction implements SourceCacheFunction {

  private static RGB8ToRGBFunction INSTANCE;

  public static RGB8ToRGBFunction getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new RGB8ToRGBFunction();
    }
    return INSTANCE;
  }

  @Override
  public void doConversion(final ByteBuffer sourceData, final byte[] store, final int destX, final int destY,
      final int dataSize, final int tileSize) {
    try {
      int destIndex = (destY * tileSize * dataSize + destX * tileSize) * 3;
      int sourceIndex = 0;
      final int width = tileSize * 3;

      for (int y = 0; y < tileSize; y++) {
        sourceData.position(sourceIndex);
        sourceData.get(store, destIndex, width);

        sourceIndex += width;
        destIndex += dataSize * 3;
      }
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }

}
