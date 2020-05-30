/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.heightmap;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.math.MathUtils;

public class ImageHeightMap {

  public static float[] generateHeightMap(final Image ardorImage, final float min, final float max) {
    if (max <= min) {
      throw new IllegalArgumentException("max must be greater than min");
    }

    final ImageDataFormat format = ardorImage.getDataFormat();
    if (format != ImageDataFormat.RGB && format != ImageDataFormat.RGBA && format != ImageDataFormat.Red) {
      throw new IllegalArgumentException("Unhandled format (must be Red, RGB or RGBA): " + format);
    }

    if (ardorImage.getWidth() != ardorImage.getHeight() || !MathUtils.isPowerOfTwo(ardorImage.getWidth())) {
      throw new IllegalArgumentException("Only pow2, square images are supported.");
    }

    final int size = ardorImage.getWidth(), comps = format.getComponents();

    // initialize the height data attributes
    final float[] heightData = new float[ardorImage.getWidth() * ardorImage.getHeight()];
    final byte[] data = new byte[heightData.length * comps];
    ardorImage.getData(0).get(data);

    int index = 0, dataIndex, gray;
    final int rowsize = size * comps;
    final float byteScale = (max - min) / 255f;
    for (int h = 0; h < size; h++) {
      for (int w = 0; w < size; w++) {
        dataIndex = h * rowsize + w * comps;
        if (comps == 1) {
          gray = data[dataIndex] & 0xFF;
        } else {
          final int red = data[dataIndex] & 0xFF;
          final int green = data[dataIndex + 1] & 0xFF;
          final int blue = data[dataIndex + 2] & 0xFF;

          gray = (int) (0.30 * red + 0.59 * green + 0.11 * blue);
        }

        heightData[index++] = gray * byteScale + min;
      }
    }
    return heightData;
  }
}
