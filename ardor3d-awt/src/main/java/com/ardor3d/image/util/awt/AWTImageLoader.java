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
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.loader.ImageLoader;
import com.ardor3d.image.loader.ImageLoaderUtil;
import com.ardor3d.util.Ardor3dException;

/**
 * Image loader that makes use of AWT's ImageIO to load image file data.
 */
public class AWTImageLoader implements ImageLoader {
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

    final var dataBuffer = image.getRaster().getDataBuffer();

    if (dataBuffer instanceof DataBufferByte) {
      return convertDataBuffer((DataBufferByte) dataBuffer, image, flipImage);
    }

    if (dataBuffer instanceof DataBufferShort) {
      return convertDataBuffer((DataBufferShort) dataBuffer, image, flipImage);
    }

    if (dataBuffer instanceof DataBufferInt) {
      return convertDataBuffer((DataBufferInt) dataBuffer, image, flipImage);
    }

    if (dataBuffer instanceof DataBufferFloat) {
      return convertDataBuffer((DataBufferFloat) dataBuffer, image, flipImage);
    }

    throw new Ardor3dException(
        "Unhandled buffered image - could not convert data buffer of type: " + dataBuffer.getClass().getName());
  }

  protected static Image convertDataBuffer(final DataBufferByte dataBuffer, final BufferedImage source,
      final boolean flipImage) {
    final var width = source.getWidth();
    final var height = source.getHeight();
    final var data = dataBuffer.getData();
    final var colorModel = source.getColorModel();
    final var bpp = colorModel.getPixelSize();
    final var rowWidth = width * bpp >> 3;

    if (flipImage) {
      // flip data in place
      final var tmp = new byte[rowWidth];
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
    final var byteBuffer = BufferUtils.createByteBuffer(data.length);
    byteBuffer.put(data);
    byteBuffer.flip();

    final Image ardorImage = new Image();
    ardorImage.setWidth(width);
    ardorImage.setHeight(height);
    ardorImage.setData(byteBuffer);
    ardorImage.setDataType(PixelDataType.UnsignedByte);

    // Figure out our type
    if (colorModel.getNumColorComponents() == 3) {
      ardorImage.setDataFormat(colorModel.hasAlpha() ? ImageDataFormat.RGBA : ImageDataFormat.RGB);
    } else if (colorModel.getNumColorComponents() == 2) {
      ardorImage.setDataFormat(ImageDataFormat.RG);
    } else if (colorModel.getNumColorComponents() == 1) {
      ardorImage.setDataFormat(ImageDataFormat.Red);
    }

    // return our new Ardor3d image
    return ardorImage;
  }

  protected static Image convertDataBuffer(final DataBufferShort dataBuffer, final BufferedImage source,
      final boolean flipImage) {
    final var width = source.getWidth();
    final var height = source.getHeight();
    final var data = dataBuffer.getData();
    final var colorModel = source.getColorModel();
    final var bpp = colorModel.getPixelSize();
    final var rowWidth = width * bpp >> 4;

    if (flipImage) {
      // flip data in place
      final var tmp = new short[rowWidth];
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
    final var byteBuffer = BufferUtils.createByteBuffer(data.length * 2);
    byteBuffer.asShortBuffer().put(data);

    final Image ardorImage = new Image();
    ardorImage.setWidth(width);
    ardorImage.setHeight(height);
    ardorImage.setData(byteBuffer);
    ardorImage.setDataType(PixelDataType.UnsignedShort);
    ardorImage.setDataFormat(ImageDataFormat.Red); // ??? Not sure here

    // return our new Ardor3d image
    return ardorImage;
  }

  protected static Image convertDataBuffer(final DataBufferInt dataBuffer, final BufferedImage source,
      final boolean flipImage) {
    final var width = source.getWidth();
    final var height = source.getHeight();
    final var data = dataBuffer.getData();
    final var rowWidth = width;

    if (flipImage) {
      // flip data in place
      final var tmp = new int[rowWidth];
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
    byteBuffer.asIntBuffer().put(data);

    final Image ardorImage = new Image();
    ardorImage.setWidth(width);
    ardorImage.setHeight(height);
    ardorImage.setData(byteBuffer);
    ardorImage.setDataType(PixelDataType.UnsignedByte);
    ardorImage.setDataFormat(ImageDataFormat.RGBA);

    // return our new Ardor3d image
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
