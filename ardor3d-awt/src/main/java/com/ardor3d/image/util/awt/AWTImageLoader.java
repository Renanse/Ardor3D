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

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.loader.ImageLoader;
import com.ardor3d.image.loader.ImageLoaderUtil;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.Ardor3dException;

/**
 * Image loader that makes use of AWT's ImageIO to load image file data.
 */
public class AWTImageLoader implements ImageLoader {
  private static final Logger logger = Logger.getLogger(AWTImageLoader.class.getName());
  private static boolean createOnHeap = false;

  private static String[] supportedFormats;

  public static String[] getSupportedFormats() { return supportedFormats; }

  public static void registerLoader() {
    if (supportedFormats == null) {
      final List<String> formats = new ArrayList<>();
      for (String format : ImageIO.getReaderFormatNames()) {
        format = "." + format.toUpperCase();
        if (!formats.contains(format)) {
          formats.add(format);
        }
      }
      supportedFormats = formats.toArray(new String[formats.size()]);
    }
    ImageLoaderUtil.registerHandler(new AWTImageLoader(), supportedFormats);
  }

  @Override
  public Image load(final InputStream is, final boolean flipImage) throws IOException {
    final BufferedImage image = ImageIO.read(is);
    if (image == null) {
      return null;
    }

    return makeArdor3dImage(image, flipImage);
  }

  public static Image makeArdor3dImage(final BufferedImage image, final boolean flipImage) {
    if (image == null) {
      return null;
    }

    final ColorModel colorModel = image.getColorModel();
    final int numComponents = colorModel.getColorSpace().getNumComponents();
    final var dataBuffer = image.getRaster().getDataBuffer();

    // special case for float textures - should be 1 component
    if (dataBuffer instanceof DataBufferFloat) {
      if (numComponents != 1) {
        throw new Ardor3dException(
            "Unhandled buffered image - could not convert float data buffer with " + numComponents + " components.");
      }

      return convertDataBuffer((DataBufferFloat) dataBuffer, image, flipImage);
    }

    final boolean hasAlpha = colorModel.hasAlpha();
    final boolean grayscale = colorModel.getNumComponents() == 1;

    // make sure we have an image that is of correct type and orientation to read from.
    BufferedImage texToConvert;
    if (flipImage || ((image).getType() != BufferedImage.TYPE_BYTE_GRAY
        && (hasAlpha ? (image).getType() != BufferedImage.TYPE_4BYTE_ABGR
            : (image).getType() != BufferedImage.TYPE_3BYTE_BGR))) {
      // Obtain the image data.
      try {
        texToConvert =
            new BufferedImage(image.getWidth(null), image.getHeight(null), grayscale ? BufferedImage.TYPE_BYTE_GRAY
                : hasAlpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
      } catch (final IllegalArgumentException e) {
        logger.warning("Problem creating buffered Image: " + e.getMessage());
        return TextureState.getDefaultTextureImage();
      }

      final int imageWidth = image.getWidth(null);
      final int imageHeight = image.getHeight(null);
      final int[] tmpData = new int[imageWidth];
      int row = 0;
      for (int y = imageHeight - 1; y >= 0; y--) {
        image.getRGB(0, (flipImage ? row++ : y), imageWidth, 1, tmpData, 0, imageWidth);
        texToConvert.setRGB(0, y, imageWidth, 1, tmpData, 0, imageWidth);
      }

    } else {
      texToConvert = image;
    }

    // Get image as a byte buffer - note this corrects order of Alpha component from ARGB to RGBA
    final byte[] data = asByteArray(texToConvert);
    final ByteBuffer scratch =
        createOnHeap ? BufferUtils.createByteBufferOnHeap(data.length) : BufferUtils.createByteBuffer(data.length);
    scratch.clear();
    scratch.put(data);
    scratch.flip();
    final Image ardorImage = new Image();
    ardorImage.setDataFormat(grayscale ? ImageDataFormat.Red : hasAlpha ? ImageDataFormat.RGBA : ImageDataFormat.RGB);
    ardorImage.setDataType(PixelDataType.UnsignedByte);
    ardorImage.setWidth(texToConvert.getWidth());
    ardorImage.setHeight(texToConvert.getHeight());
    ardorImage.setData(scratch);
    return ardorImage;
  }

  protected static Image convertDataBuffer(final DataBufferFloat dataBuffer, final BufferedImage source,
      final boolean flipImage) {
    final var width = source.getWidth();
    final var height = source.getHeight();
    final var data = dataBuffer.getData();
    final var colorModel = source.getColorModel();
    final var bpp = colorModel.getPixelSize();
    final var rowWidth = width;

    if (flipImage) {
      // flip data in place
      final var tmp = new float[rowWidth];
      for (int y = 0, maxY = height / 2; y < maxY; y++) {
        // read line A into tmp
        System.arraycopy(data, y * rowWidth, tmp, 0, rowWidth);
        // copy line B over line A
        System.arraycopy(data, (height - y - 1) * rowWidth, data, y * rowWidth, rowWidth);
        // copy saved line A over line B
        System.arraycopy(tmp, 0, data, (height - y - 1) * rowWidth, rowWidth);
      }
    }

    // create image byte buffer
    final var byteBuffer = BufferUtils.createByteBuffer(data.length * 4);
    byteBuffer.asFloatBuffer().put(data);

    final Image ardorImage = new Image();
    ardorImage.setWidth(width);
    ardorImage.setHeight(height);
    ardorImage.setData(byteBuffer);
    // NB: not too sure if bpp==16 is possible in java.awt
    ardorImage.setDataType(bpp == 16 ? PixelDataType.HalfFloat : PixelDataType.Float);
    ardorImage.setDataFormat(ImageDataFormat.Red);

    // return our new Ardor3d image
    return ardorImage;
  }

  public static byte[] asByteArray(final BufferedImage image) {
    final int imageWidth = image.getWidth(null);
    final int imageHeight = image.getHeight(null);
    final boolean hasAlpha = image.getColorModel().hasAlpha();
    final boolean grayscale = image.getColorModel().getNumComponents() == 1;

    if (image.getRaster().getTransferType() == DataBuffer.TYPE_BYTE) {
      return (byte[]) image.getRaster().getDataElements(0, 0, imageWidth, imageHeight, null);
    }

    final byte[] rVal = new byte[imageWidth * imageHeight * (grayscale ? 1 : (hasAlpha ? 4 : 3))];
    final int[] tmpData = new int[imageWidth];
    int index = 0;
    for (int y = 0; y < imageHeight; y++) {
      image.getRGB(0, y, imageWidth, 1, tmpData, 0, imageWidth);
      for (int i = 0; i < imageWidth; i++) {
        final int argb = tmpData[i];
        if (grayscale) {
          rVal[index++] = (byte) (argb & 0xFF);
        } else {
          rVal[index++] = (byte) ((argb >> 16) & 0xFF);
          rVal[index++] = (byte) ((argb >> 8) & 0xFF);
          rVal[index++] = (byte) (argb & 0xFF);
          if (hasAlpha) {
            rVal[index++] = (byte) ((argb >> 24) & 0xFF);
          }
        }
      }
    }
    return rVal;
  }

  public static void setCreateOnHeap(final boolean createOnHeap) { AWTImageLoader.createOnHeap = createOnHeap; }

  public static boolean isCreateOnHeap() { return createOnHeap; }
}
