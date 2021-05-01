/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.loader.dds;

import com.ardor3d.image.ImageDataFormat;

public class DdsUtils {

  /**
   * Get the necessary bit shifts needed to align mask with 0.
   *
   * @param mask
   *          the bit mask to test
   * @return number of bits to shift to the right to align mask with 0.
   */
  static final int shiftCount(int mask) {
    if (mask == 0) {
      return 0;
    }

    int i = 0;
    while ((mask & 0x1) == 0) {
      mask = mask >> 1;
      i++;
      if (i > 32) {
        throw new Error(Integer.toHexString(mask));
      }
    }

    return i;
  }

  /**
   * Check a value against a bit mask to see if it is set.
   *
   * @param value
   *          the value to check
   * @param bitMask
   *          our mask
   * @return true if the mask passes
   */
  static final boolean isSet(final int value, final int bitMask) {
    return (value & bitMask) == bitMask;
  }

  /**
   * Get the string as a dword int value.
   *
   * @param string
   *          our string... should only be 1-4 chars long.
   * @return the int value
   */
  static final int getInt(final String string) {
    return getInt(string.getBytes());
  }

  /**
   * Get the byte array as a dword int value.
   *
   * @param bytes
   *          our array... should only be 1-4 bytes long.
   * @return the int value
   */
  static final int getInt(final byte[] bytes) {
    int rVal = 0;
    rVal |= ((bytes[0] & 0xff) << 0);
    if (bytes.length > 1) {
      rVal |= ((bytes[1] & 0xff) << 8);
    }
    if (bytes.length > 2) {
      rVal |= ((bytes[2] & 0xff) << 16);
    }
    if (bytes.length > 3) {
      rVal |= ((bytes[3] & 0xff) << 24);
    }
    return rVal;
  }

  /**
   * Flip a dxt mipmap/image. Inspired by similar code in opentk and the nvidia sdk.
   *
   * @param rawData
   *          our unflipped image as raw bytes
   * @param width
   *          our image's width
   * @param height
   *          our image's height
   * @param format
   *          our image's format
   * @return the flipped image as raw bytes.
   */
  public static byte[] flipDXT(final byte[] rawData, final int width, final int height, final ImageDataFormat format) {
    final byte[] returnData = new byte[rawData.length];

    final int blocksPerColumn = (width + 3) >> 2;
    final int blocksPerRow = (height + 3) >> 2;
    final int bytesPerBlock = format.getComponents() * 8;

    for (int sourceRow = 0; sourceRow < blocksPerRow; sourceRow++) {
      final int targetRow = blocksPerRow - sourceRow - 1;
      for (int column = 0; column < blocksPerColumn; column++) {
        final int target = (targetRow * blocksPerColumn + column) * bytesPerBlock;
        final int source = (sourceRow * blocksPerColumn + column) * bytesPerBlock;
        switch (format) {
          case PrecompressedDXT1:
          case PrecompressedDXT1A:
          case PrecompressedLATC_L:
            System.arraycopy(rawData, source, returnData, target, 4);
            returnData[target + 4] = rawData[source + 7];
            returnData[target + 5] = rawData[source + 6];
            returnData[target + 6] = rawData[source + 5];
            returnData[target + 7] = rawData[source + 4];
            break;
          case PrecompressedDXT3:
            // Alpha
            returnData[target + 0] = rawData[source + 6];
            returnData[target + 1] = rawData[source + 7];
            returnData[target + 2] = rawData[source + 4];
            returnData[target + 3] = rawData[source + 5];
            returnData[target + 4] = rawData[source + 2];
            returnData[target + 5] = rawData[source + 3];
            returnData[target + 6] = rawData[source + 0];
            returnData[target + 7] = rawData[source + 1];

            // Color
            System.arraycopy(rawData, source + 8, returnData, target + 8, 4);
            returnData[target + 12] = rawData[source + 15];
            returnData[target + 13] = rawData[source + 14];
            returnData[target + 14] = rawData[source + 13];
            returnData[target + 15] = rawData[source + 12];
            break;
          case PrecompressedDXT5:
            // Alpha, the first 2 bytes remain
            returnData[target + 0] = rawData[source + 0];
            returnData[target + 1] = rawData[source + 1];

            // extract 3 bits each and flip them
            getBytesFromUInt24(returnData, target + 5, flipUInt24(getUInt24(rawData, source + 2)));
            getBytesFromUInt24(returnData, target + 2, flipUInt24(getUInt24(rawData, source + 5)));

            // Color
            System.arraycopy(rawData, source + 8, returnData, target + 8, 4);
            returnData[target + 12] = rawData[source + 15];
            returnData[target + 13] = rawData[source + 14];
            returnData[target + 14] = rawData[source + 13];
            returnData[target + 15] = rawData[source + 12];
            break;
          case PrecompressedLATC_LA:
            // alpha
            System.arraycopy(rawData, source, returnData, target, 4);
            returnData[target + 4] = rawData[source + 7];
            returnData[target + 5] = rawData[source + 6];
            returnData[target + 6] = rawData[source + 5];
            returnData[target + 7] = rawData[source + 4];

            // Color
            System.arraycopy(rawData, source + 8, returnData, target + 8, 4);
            returnData[target + 12] = rawData[source + 15];
            returnData[target + 13] = rawData[source + 14];
            returnData[target + 14] = rawData[source + 13];
            returnData[target + 15] = rawData[source + 12];
            break;
          default:
            // not a format we care about
            break;
        }
      }
    }
    return returnData;
  }

  // DXT5 Alpha block flipping, inspired by code from Evan Hart (nVidia SDK)
  private static int getUInt24(final byte[] input, final int offset) {
    int result = 0;
    result |= (input[offset + 0] & 0xff) << 0;
    result |= (input[offset + 1] & 0xff) << 8;
    result |= (input[offset + 2] & 0xff) << 16;
    return result;
  }

  private static void getBytesFromUInt24(final byte[] input, final int offset, final int uint24) {
    input[offset + 0] = (byte) (uint24 & 0x000000ff);
    input[offset + 1] = (byte) ((uint24 & 0x0000ff00) >> 8);
    input[offset + 2] = (byte) ((uint24 & 0x00ff0000) >> 16);
    return;
  }

  private static final int ThreeBitMask = 0x7;

  private static int flipUInt24(int uint24) {
    final byte[][] threeBits = new byte[2][];
    for (int i = 0; i < 2; i++) {
      threeBits[i] = new byte[4];
    }

    // extract 3 bits each into the array
    threeBits[0][0] = (byte) (uint24 & ThreeBitMask);
    uint24 >>= 3;
    threeBits[0][1] = (byte) (uint24 & ThreeBitMask);
    uint24 >>= 3;
    threeBits[0][2] = (byte) (uint24 & ThreeBitMask);
    uint24 >>= 3;
    threeBits[0][3] = (byte) (uint24 & ThreeBitMask);
    uint24 >>= 3;
    threeBits[1][0] = (byte) (uint24 & ThreeBitMask);
    uint24 >>= 3;
    threeBits[1][1] = (byte) (uint24 & ThreeBitMask);
    uint24 >>= 3;
    threeBits[1][2] = (byte) (uint24 & ThreeBitMask);
    uint24 >>= 3;
    threeBits[1][3] = (byte) (uint24 & ThreeBitMask);

    // stuff 8x 3bits into 3 bytes
    int result = 0;
    result = result | (threeBits[1][0] << 0);
    result = result | (threeBits[1][1] << 3);
    result = result | (threeBits[1][2] << 6);
    result = result | (threeBits[1][3] << 9);
    result = result | (threeBits[0][0] << 12);
    result = result | (threeBits[0][1] << 15);
    result = result | (threeBits[0][2] << 18);
    result = result | (threeBits[0][3] << 21);
    return result;
  }
}
