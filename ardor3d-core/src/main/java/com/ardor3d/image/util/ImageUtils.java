/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.util;

import java.nio.ByteBuffer;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.util.MathUtils;

public abstract class ImageUtils {

  public static int getPixelByteSize(final ImageDataFormat format, final PixelDataType type) {
    return type.getBytesPerPixel(format.getComponents());
  }

  public static TextureStoreFormat getTextureStoreFormat(final TextureStoreFormat format, final Image image) {
    if (format != TextureStoreFormat.GuessCompressedFormat && format != TextureStoreFormat.GuessNoCompressedFormat) {
      return format;
    }
    if (image == null) {
      throw new Error("Unable to guess format type... Image is null.");
    }

    final PixelDataType type = image.getDataType();
    final ImageDataFormat dataFormat = image.getDataFormat();
    switch (dataFormat) {
      case BGRA:
      case RGBA:
        if (format == TextureStoreFormat.GuessCompressedFormat) {
          return TextureStoreFormat.CompressedRGBA;
        }
        switch (type) {
          case Byte:
          case UnsignedByte:
            return TextureStoreFormat.RGBA8;
          case Short:
          case UnsignedShort:
          case Int:
          case UnsignedInt:
            return TextureStoreFormat.RGBA16;
          case HalfFloat:
            return TextureStoreFormat.RGBA16F;
          case Float:
            return TextureStoreFormat.RGBA32F;
          default:
            break;
        }
        break;
      case BGR:
      case RGB:
        if (format == TextureStoreFormat.GuessCompressedFormat) {
          return TextureStoreFormat.CompressedRGB;
        }
        switch (type) {
          case Byte:
          case UnsignedByte:
            return TextureStoreFormat.RGB8;
          case Short:
          case UnsignedShort:
          case Int:
          case UnsignedInt:
            return TextureStoreFormat.RGB16;
          case HalfFloat:
            return TextureStoreFormat.RGB16F;
          case Float:
            return TextureStoreFormat.RGB32F;
          default:
            break;
        }
        break;
      case RG:
        if (format == TextureStoreFormat.GuessCompressedFormat) {
          return TextureStoreFormat.CompressedRG;
        }
        switch (type) {
          case Byte:
          case UnsignedByte:
            return TextureStoreFormat.RG8;
          case Short:
          case UnsignedShort:
            return TextureStoreFormat.RG16;
          case Int:
            return TextureStoreFormat.RG16I;
          case UnsignedInt:
            return TextureStoreFormat.RG16UI;
          case HalfFloat:
            return TextureStoreFormat.RG16F;
          case Float:
            return TextureStoreFormat.RG32F;
          default:
            break;
        }
        break;
      case Red:
        if (format == TextureStoreFormat.GuessCompressedFormat) {
          return TextureStoreFormat.CompressedRed;
        }
        switch (type) {
          case Byte:
          case UnsignedByte:
            return TextureStoreFormat.R8;
          case Short:
          case UnsignedShort:
            return TextureStoreFormat.R16;
          case Int:
            return TextureStoreFormat.R16I;
          case UnsignedInt:
            return TextureStoreFormat.R16UI;
          case HalfFloat:
            return TextureStoreFormat.R16F;
          case Float:
            return TextureStoreFormat.R32F;
          default:
            break;
        }
        break;
      case Alpha:
      case Green:
      case Blue:
      case StencilIndex:
        switch (type) {
          case Byte:
          case UnsignedByte:
            return TextureStoreFormat.R8;
          case Short:
          case UnsignedShort:
          case Int:
          case UnsignedInt:
            return TextureStoreFormat.R16;
          case HalfFloat:
            return TextureStoreFormat.R16F;
          case Float:
            return TextureStoreFormat.R32F;
          default:
            break;
        }
        break;
      case Depth:
        // XXX: Should we actually switch here? Depth textures can be slightly fussy.
        return TextureStoreFormat.Depth;
      case PrecompressedDXT1:
        return TextureStoreFormat.NativeDXT1;
      case PrecompressedDXT1A:
        return TextureStoreFormat.NativeDXT1A;
      case PrecompressedDXT3:
        return TextureStoreFormat.NativeDXT3;
      case PrecompressedDXT5:
        return TextureStoreFormat.NativeDXT5;
      case PrecompressedLATC_L:
        return TextureStoreFormat.NativeLATC_L;
      case PrecompressedLATC_LA:
        return TextureStoreFormat.NativeLATC_LA;
    }

    throw new Error("Unhandled type / format combination: " + type + " / " + dataFormat);
  }

  /**
   * Generate a new sub-image from the given source image, by bilinear-interpolation across a
   * quadrilateral defined by the uv values of four corners. The resulting image will be of the same
   * image format and data type as the source image.
   *
   * @param srcImg
   *          the source image
   * @param width
   *          the desired width of our new texture image
   * @param height
   *          the desired height of our new texture image
   * @param bottomLeft
   *          the bottom left corner of the quadrilateral. The full image's bottom left is [0, 0]
   * @param topLeft
   *          the top left corner of the quadrilateral. The full image's top left is [0, 1]
   * @param topRight
   *          the top right corner of the quadrilateral. The full image's top right is [1, 1]
   * @param bottomRight
   *          the bottom right corner of the quadrilateral. The full image's bottom right is [1, 0]
   * @return the new Image.
   */
  public static Image generateSubImage(final Image srcImg, final int width, final int height,
      final ReadOnlyVector2 bottomLeft, final ReadOnlyVector2 topLeft, final ReadOnlyVector2 topRight,
      final ReadOnlyVector2 bottomRight) {

    final int bpp = srcImg.getDataType().getBytesPerPixel(srcImg.getDataFormat().getComponents());
    final ByteBuffer dstBuff = BufferUtils.createByteBuffer(width * height * bpp);

    final Vector2 uv = new Vector2();
    byte[] pixel = new byte[0];
    for (int j = 0; j < height; j++) {
      final float y = j / (height - 1f);
      for (int i = 0; i < width; i++) {
        final float x = i / (width - 1f);
        uv.zero();
        bottomLeft.scaleAdd((1 - x) * (1 - y), uv, uv);
        topLeft.scaleAdd((1 - x) * y, uv, uv);
        topRight.scaleAdd(x * y, uv, uv);
        bottomRight.scaleAdd(x * (1 - y), uv, uv);

        pixel = getPixel(srcImg, uv, pixel);
        dstBuff.put(pixel);
      }
    }

    dstBuff.flip();
    return new Image(srcImg.getDataFormat(), srcImg.getDataType(), width, height, dstBuff, null);
  }

  /**
   * Grabs the byte value of a pixel at the given uv texture coordinate in a given source Image.
   *
   * @param srcImg
   *          the source image to pull pixel color from.
   * @param uv
   *          the point at which to query pixel color. Should be in the range of [0,0]-[1,1] (bottom
   *          left to top right). Values outside of this range are clamped.
   * @param store
   *          optional byte array object to store our result in. If null or incorrect sized buffer is
   *          passed, a new byte buffer array is instantiated.
   * @return the pixel value as a byte array.
   */
  public static byte[] getPixel(final Image srcImg, final Vector2 uv, final byte[] store) {

    // Determine our pixel byte size in srcImg
    final ByteBuffer buff = srcImg.getData(0);
    final int originalPos = buff.position();
    final int width = srcImg.getWidth();
    final int height = srcImg.getHeight();
    final int x = Math.round(MathUtils.clamp01(uv.getXf()) * (width - 1));
    final int y = Math.round(MathUtils.clamp01(uv.getYf()) * (height - 1));
    final int bpp = srcImg.getDataType().getBytesPerPixel(srcImg.getDataFormat().getComponents());

    final byte[] rVal = store != null && store.length == bpp ? store : new byte[bpp];

    final int offset = bpp * (width * y + x);
    buff.position(offset);
    buff.get(rVal);

    buff.position(originalPos);

    return rVal;
  }

}
