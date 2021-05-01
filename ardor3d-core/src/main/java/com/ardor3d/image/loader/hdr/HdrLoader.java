/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.loader.hdr;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.loader.ImageLoader;

public class HdrLoader implements ImageLoader {
  @Override
  public Image load(final InputStream is, final boolean flipped) throws IOException {
    // open a stream to the file
    final BufferedInputStream bis = new BufferedInputStream(is, 8192);
    final DataInputStream dis = new DataInputStream(bis);

    final RGBE.Header header = RGBE.readHeader(dis);

    final int width = header.getWidth();
    final int height = header.getHeight();

    final ByteBuffer imageData = BufferUtils.createByteBuffer(width * height * 3 * 4); // w x h x
                                                                                       // rgb x 4bytes per float
    imageData.clear();
    final FloatBuffer imageDataAsFloat = imageData.asFloatBuffer();

    final byte[] scanline = new byte[width * 4];

    for (int i = 0; i < height; i++) {
      RGBE.readPixelsRawRLE(dis, scanline, 0, width, 1);
      if (flipped) {
        imageDataAsFloat.position((height - i - 1) * width * 3);
      }
      RGBE.rgbe2float(imageDataAsFloat, scanline);
    }

    imageDataAsFloat.rewind();

    // Create the ardor3d.image.Image object
    final com.ardor3d.image.Image textureImage = new com.ardor3d.image.Image();

    textureImage.setDataFormat(ImageDataFormat.RGB);
    textureImage.setDataType(PixelDataType.Float);
    textureImage.setWidth(width);
    textureImage.setHeight(height);
    textureImage.setData(imageData);

    return textureImage;
  }
}
