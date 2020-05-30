/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.util.geom.BufferUtils;

public abstract class GeneratedImageFactory {

  /**
   * Creates a side x side sized image of a single solid color, of data type byte.
   *
   * @param color
   *          the color of our image
   * @param useAlpha
   *          if true, the image will have an alpha component (whose value will come from the supplied
   *          color.)
   * @param side
   *          the length of a side of our square image
   * @return the new Image
   */
  public static Image createSolidColorImage(final ReadOnlyColorRGBA color, final boolean useAlpha, final int side) {
    final ByteBuffer data = BufferUtils.createByteBuffer(side * side * (useAlpha ? 4 : 3));
    final byte[] b = new byte[useAlpha ? 4 : 3];
    b[0] = (byte) (color.getRed() * 255);
    b[1] = (byte) (color.getGreen() * 255);
    b[2] = (byte) (color.getBlue() * 255);
    if (useAlpha) {
      b[3] = (byte) (color.getAlpha() * 255);
    }

    for (int i = 0, max = side * side; i < max; i++) {
      data.put(b);
    }
    data.rewind();
    final ImageDataFormat fmt = useAlpha ? ImageDataFormat.RGBA : ImageDataFormat.RGB;
    return new Image(fmt, PixelDataType.UnsignedByte, side, side, data, null);
  }

  /**
   * Creates a one dimensional color image using each color given as a single pixel.
   *
   * @param useAlpha
   *          if true, the image will have an alpha component (whose value will come from each
   *          supplied color.)
   * @param colors
   *          one or more colors
   * @return a 1D image, width == colors.length
   */
  public static Image create1DColorImage(final boolean useAlpha, final ReadOnlyColorRGBA... colors) {
    final ByteBuffer data = BufferUtils.createByteBuffer(colors.length * (useAlpha ? 4 : 3));
    for (int i = 0; i < colors.length; i++) {
      final ReadOnlyColorRGBA color = colors[i];
      data.put((byte) (color.getRed() * 255));
      data.put((byte) (color.getGreen() * 255));
      data.put((byte) (color.getBlue() * 255));

      if (useAlpha) {
        data.put((byte) (color.getAlpha() * 255));
      }
    }
    data.rewind();
    final ImageDataFormat fmt = (useAlpha) ? ImageDataFormat.RGBA : ImageDataFormat.RGB;
    return new Image(fmt, PixelDataType.UnsignedByte, colors.length, 1, data, null);
  }

  /**
   * Creates an 8 bit single channel image using the given function as input. The domain of the image
   * will be [-1, 1] on each axis unless that axis is size 0 (in which case the domain is [0, 0]. The
   * expected range of the function result set is also [-1, 1] and this is remapped to [0, 255] for
   * storage as a byte.
   *
   * @param source
   *          the source function to evaluate at each pixel of the image.
   * @param width
   *          the width of the image
   * @param height
   *          the height of the image
   * @param depth
   *          the depth of the image (for 3D texture or texture array use)
   * @return the resulting Image.
   */
  public static Image createRed8Image(final Function3D source, final int width, final int height, final int depth) {
    // default range is [-1, 1] on each axis, unless that axis is size 1.
    return createRed8Image(source, width, height, depth, //
        width == 1 ? 0 : -1, width == 1 ? 0 : 1, // X
        height == 1 ? 0 : -1, height == 1 ? 0 : 1, // Y
        depth == 1 ? 0 : -1, depth == 1 ? 0 : 1, // Z
        -1, 1);
  }

  /**
   * Creates an 8 bit single channel image using the given function as input.
   *
   * @param source
   *          the source function to evaluate at each pixel of the image.
   * @param width
   *          the width of the image
   * @param height
   *          the height of the image
   * @param depth
   *          the depth of the image (for 3D texture or texture array use)
   * @param startX
   *          the lowest domain value of the X axis. Or in other words, when a pixel on the leftmost
   *          side of the image is evaluated, this is the X value sent to the function.
   * @param endX
   *          the highest domain value of the X axis. Or in other words, when a pixel on the rightmost
   *          side of the image is evaluated, this is the X value sent to the function.
   * @param startY
   *          the lowest domain value of the Y axis.
   * @param endY
   *          the highest domain value of the Y axis.
   * @param startZ
   *          the lowest domain value of the Z axis.
   * @param endZ
   *          the highest domain value of the Z axis.
   * @param rangeStart
   *          the expected lowest value of the output from the source function. This is used to map
   *          the output to a byte.
   * @param rangeEnd
   *          the expected highest value of the output from the source function. This is used to map
   *          the output to a byte.
   * @return the resulting Image.
   */
  public static Image createRed8Image(final Function3D source, final int width, final int height, final int depth,
      final double startX, final double endX, final double startY, final double endY, final double startZ,
      final double endZ, final double rangeStart, final double rangeEnd) {
    double val;
    final double rangeDiv = 1.0 / (rangeEnd - rangeStart);
    // prepare list of image slices.
    final List<ByteBuffer> dataList = new ArrayList<>(depth);

    final byte[] data = new byte[width * height];
    for (double z = 0; z < depth; z++) {
      // calc our z coordinate, using start and end Z and our current progress along depth
      final double dz = (z / depth) * (endZ - startZ) + startZ;
      int i = 0;
      for (double y = 0; y < height; y++) {
        // calc our y coordinate, using start and end Y and our current progress along height
        final double dy = (y / height) * (endY - startY) + startY;
        for (double x = 0; x < width; x++) {
          // calc our x coordinate, using start and end X and our current progress along width
          final double dx = (x / width) * (endX - startX) + startX;

          // Evaluate for dx, dy, dz
          val = source.eval(dx, dy, dz);

          // Keep us in [rangeStart, rangeEnd]
          val = MathUtils.clamp(val, rangeStart, rangeEnd);

          // Convert to [0, 255]
          val = ((val - rangeStart) * rangeDiv) * 255.0;
          data[i++] = (byte) val;
        }
      }
      final ByteBuffer dataBuf = BufferUtils.createByteBuffer(data.length);
      dataBuf.put(data);
      dataBuf.rewind();
      dataList.add(dataBuf);
    }

    return new Image(ImageDataFormat.Red, PixelDataType.UnsignedByte, width, height, dataList, null);
  }

  /**
   * Converts an 8 bit luminance Image to an RGB or RGBA color image by mapping the given values to a
   * corresponding color value in the given colorTable.
   *
   * <p>
   * XXX: perhaps replace the color array with some gradient class?
   * </p>
   *
   * @param lumImage
   *          the Image to convert.
   * @param useAlpha
   *          if true, the final image will use have an alpha channel, populated by the alpha values
   *          of the given colors.
   * @param colorTable
   *          a set of colors, should be length 256.
   * @return the new Image.
   */
  public static Image createColorImageFromLuminance8(final Image lumImage, final boolean useAlpha,
      final ReadOnlyColorRGBA... colorTable) {
    assert (colorTable.length == 256) : "color table must be size 256.";

    final List<ByteBuffer> dataList = new ArrayList<>(lumImage.getDepth());
    ReadOnlyColorRGBA c;
    for (int i = 0; i < lumImage.getDepth(); i++) {
      final ByteBuffer src = lumImage.getData(i);
      final int size = src.capacity();
      final ByteBuffer out = BufferUtils.createByteBuffer(size * (useAlpha ? 4 : 3));
      final byte[] data = new byte[out.capacity()];
      int j = 0;
      for (int x = 0; x < size; x++) {
        c = colorTable[src.get(x) & 0xFF];
        data[j++] = (byte) (c.getRed() * 255);
        data[j++] = (byte) (c.getGreen() * 255);
        data[j++] = (byte) (c.getBlue() * 255);
        if (useAlpha) {
          data[j++] = (byte) (c.getAlpha() * 255);
        }
      }
      out.put(data);
      out.rewind();
      dataList.add(out);
    }

    return new Image(useAlpha ? ImageDataFormat.RGBA : ImageDataFormat.RGB, PixelDataType.UnsignedByte,
        lumImage.getWidth(), lumImage.getHeight(), dataList, null);
  }

  /**
   * Fill any empty spots in the given color array by linearly interpolating the non-empty values
   * above and below it.
   *
   * @param colors
   *          the color table - must be length 256.
   */
  public static void fillInColorTable(final ReadOnlyColorRGBA[] colors) {
    assert (colors.length == 256) : "colors must be length 256";

    // make sure we have a start color...
    if (colors[0] == null) {
      colors[0] = ColorRGBA.BLACK;
    }
    // make sure we have a end color...
    if (colors[255] == null) {
      colors[255] = ColorRGBA.WHITE;
    }

    int begin = 0, end = findNonNull(1, colors);
    // step through
    for (int i = 1; i < 255; i++) {
      if (colors[i] != null) {
        begin = i;
        end = findNonNull(begin + 1, colors);
        if (end == -1) {
          break; // done!
        }
        continue;
      }
      final float scalar = (float) (i - begin) / (end - begin);
      colors[i] = ColorRGBA.lerp(colors[begin], colors[end], scalar, null);
    }
  }

  private static int findNonNull(final int start, final ReadOnlyColorRGBA[] colors) {
    for (int i = start; i < colors.length; i++) {
      if (colors[i] != null) {
        return i;
      }
    }
    return -1;
  }
}
