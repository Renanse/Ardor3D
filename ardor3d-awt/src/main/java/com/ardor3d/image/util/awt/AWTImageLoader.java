/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util.awt;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.util.ImageLoader;
import com.ardor3d.image.util.ImageLoaderUtil;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Lists;

/**
 * Image loader that makes use of AWT's ImageIO to load image file data.
 */
public class AWTImageLoader implements ImageLoader {
    private static final Logger logger = Logger.getLogger(AWTImageLoader.class.getName());

    private static boolean createOnHeap = false;

    private static String[] supportedFormats;

    public static String[] getSupportedFormats() {
        return supportedFormats;
    }

    public static void registerLoader() {
        if (supportedFormats == null) {
            final List<String> formats = Lists.newArrayList();
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

        final boolean hasAlpha = image.getColorModel().hasAlpha();
        final boolean grayscale = image.getColorModel().getNumComponents() == 1;
        BufferedImage tex;

        if (flipImage
                || ((image).getType() != BufferedImage.TYPE_BYTE_GRAY && (hasAlpha ? (image).getType() != BufferedImage.TYPE_4BYTE_ABGR
                        : (image).getType() != BufferedImage.TYPE_3BYTE_BGR))) {
            // Obtain the image data.
            try {
                tex = new BufferedImage(image.getWidth(null), image.getHeight(null),
                        grayscale ? BufferedImage.TYPE_BYTE_GRAY : hasAlpha ? BufferedImage.TYPE_4BYTE_ABGR
                                : BufferedImage.TYPE_3BYTE_BGR);
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
                tex.setRGB(0, y, imageWidth, 1, tmpData, 0, imageWidth);
            }

        } else {
            tex = image;
        }

        // Get a pointer to the image memory
        final byte data[] = asByteArray(tex);
        final ByteBuffer scratch = createOnHeap ? BufferUtils.createByteBufferOnHeap(data.length) : BufferUtils
                .createByteBuffer(data.length);
        scratch.clear();
        scratch.put(data);
        scratch.flip();
        final Image ardorImage = new Image();
        ardorImage.setDataFormat(grayscale ? ImageDataFormat.Red : hasAlpha ? ImageDataFormat.RGBA
                : ImageDataFormat.RGB);
        ardorImage.setDataType(PixelDataType.UnsignedByte);
        ardorImage.setWidth(tex.getWidth());
        ardorImage.setHeight(tex.getHeight());
        ardorImage.setData(scratch);
        return ardorImage;
    }

    public static Image makeArdor3dImage(final RenderableImage image, final boolean flipImage) {
        return makeArdor3dImage(image.createDefaultRendering(), flipImage);
    }

    public static Image makeArdor3dImage(final RenderedImage image, final boolean flipImage) {
        if (image == null) {
            return null;
        }

        final ColorModel colorModel = image.getColorModel();
        final boolean hasAlpha = colorModel.hasAlpha();
        final boolean grayscale = colorModel.getNumComponents() == 1;

        // Get a pointer to the image memory
        final byte data[] = asByteArray(image, grayscale, hasAlpha);
        final ByteBuffer scratch = createOnHeap ? BufferUtils.createByteBufferOnHeap(data.length) : BufferUtils
                .createByteBuffer(data.length);
        scratch.clear();
        scratch.put(data);
        scratch.flip();
        final Image ardorImage = new Image();
        ardorImage.setDataFormat(grayscale ? ImageDataFormat.Red : hasAlpha ? ImageDataFormat.RGBA
                : ImageDataFormat.RGB);
        ardorImage.setDataType(PixelDataType.UnsignedByte);
        ardorImage.setWidth(image.getWidth());
        ardorImage.setHeight(image.getHeight());
        ardorImage.setData(scratch);
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

    public static byte[] asByteArray(final RenderedImage image, final boolean isGreyscale, final boolean hasAlpha) {
        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final Raster raster = image.getData();

        if (raster.getTransferType() == DataBuffer.TYPE_BYTE) {
            return (byte[]) image.getData().getDataElements(0, 0, imageWidth, imageHeight, null);
        }

        final byte[] rVal = new byte[imageWidth * imageHeight * (isGreyscale ? 1 : (hasAlpha ? 4 : 3))];
        final int[] tmpData = new int[imageWidth];
        int index = 0;
        for (int y = 0; y < imageHeight; y++) {
            getRGB(raster, image.getColorModel(), 0, y, imageWidth, 1, tmpData, 0, imageWidth);
            for (int i = 0; i < imageWidth; i++) {
                final int argb = tmpData[i];
                if (isGreyscale) {
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

    /**
     * Extract rgb values from raster using the colormodel.
     */
    private static int[] getRGB(final Raster raster, final ColorModel colorModel, final int startX, final int startY,
            final int w, final int h, int[] rgbArray, final int offset, final int scansize) {
        Object data;
        final int nbands = raster.getNumBands();
        final int dataType = raster.getDataBuffer().getDataType();
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                data = new byte[nbands];
                break;
            case DataBuffer.TYPE_USHORT:
                data = new short[nbands];
                break;
            case DataBuffer.TYPE_INT:
                data = new int[nbands];
                break;
            case DataBuffer.TYPE_FLOAT:
                data = new float[nbands];
                break;
            case DataBuffer.TYPE_DOUBLE:
                data = new double[nbands];
                break;
            default:
                throw new IllegalArgumentException("Unknown data buffer type: " + dataType);
        }

        if (rgbArray == null) {
            rgbArray = new int[offset + h * scansize];
        }

        int yoff = offset;
        int off;
        for (int y = startY; y < startY + h; y++, yoff += scansize) {
            off = yoff;
            for (int x = startX; x < startX + w; x++) {
                rgbArray[off++] = colorModel.getRGB(raster.getDataElements(x, y, data));
            }
        }

        return rgbArray;
    }

    public static void setCreateOnHeap(final boolean createOnHeap) {
        AWTImageLoader.createOnHeap = createOnHeap;
    }

    public static boolean isCreateOnHeap() {
        return createOnHeap;
    }
}
