/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.image.util.awt;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.image.Image;
import com.ardor3d.image.PixelDataType;

/**
 * Utility methods for converting Ardor3D Images to AWT BufferedImages.
 */
public abstract class AWTImageUtil {

  /**
   * Convert the given Ardor3D Image to a List of BufferedImages. It is a List because Ardor3D Images
   * may contain multiple layers (for example, in the case of cube maps or 3D textures).
   * 
   * @param input
   *          the Ardor3D Image to convert
   * @return the BufferedImage(s) created in the conversion
   */
  public static List<BufferedImage> convertToAWT(final Image input) {
    // convert, using a full white tint (i.e. no applied color change from original data.)
    return convertToAWT(input, Color.WHITE);
  }

  /**
   * Convert the given Ardor3D Image to a List of BufferedImages. It is a List because Ardor3D Images
   * may contain multiple layers (for example, in the case of cube maps or 3D textures). The given AWT
   * Color is used to modulate or "tint" the returned image.
   * 
   * TODO: Add support for more formats.<br/>
   * XXX: Note that only images of data type ImageDataType.UnsignedByte and ImageDataFormat of RGB or
   * RGBA are currently supported.
   * 
   * @param input
   *          the Ardor3D Image to convert
   * @param tint
   *          the Color to apply to the generated image
   * @return the BufferedImage(s) created in the conversion
   */
  public static List<BufferedImage> convertToAWT(final Image input, final Color tint) {
    if (input.getDataType() != PixelDataType.UnsignedByte) {
      throw new Error("Unhandled Ardor3D image data type: " + input.getDataType());
    }
    // count the number of layers we will be converting.
    final int size = input.getData().size();

    // grab our image width and height
    final int width = input.getWidth(), height = input.getHeight();

    // create our return list
    final List<BufferedImage> rVal = new ArrayList<>();

    // Calculate our modulation or "tint" values per channel
    final double tRed = tint != null ? tint.getRed() / 255. : 1.0;
    final double tGreen = tint != null ? tint.getGreen() / 255. : 1.0;
    final double tBlue = tint != null ? tint.getBlue() / 255. : 1.0;
    final double tAlpha = tint != null ? tint.getAlpha() / 255. : 1.0;

    // go through each layer
    for (int i = 0; i < size; i++) {
      BufferedImage image;
      final ByteBuffer data = input.getData(i);
      data.rewind();
      boolean alpha = false;
      switch (input.getDataFormat()) {
        case RGBA:
          alpha = true;
          // Falls through on purpose.
        case RGB:
          if (alpha) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
          } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
          }

          int index, r, g, b, a, argb;

          // Go through each pixel
          for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
              index = (alpha ? 4 : 3) * (y * width + x);
              r = (int) Math.round(((data.get(index + 0)) & 0xFF) * tRed);
              g = (int) Math.round(((data.get(index + 1)) & 0xFF) * tGreen);
              b = (int) Math.round(((data.get(index + 2)) & 0xFF) * tBlue);

              // convert to integer expression
              argb = (r << 16) | (g << 8) | (b);

              // add alpha, if applicable
              if (alpha) {
                a = (int) Math.round(((data.get(index + 3)) & 0xFF) * tAlpha);
                argb |= (a & 0xFF) << 24;
              }

              // apply to image
              image.setRGB(x, y, argb);
            }
          }
          break;
        default:
          throw new Error("Unhandled image data format: " + input.getDataFormat());
      }

      // add to our list
      rVal.add(image);
    }

    // return list
    return rVal;
  }

}
