/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util.jogl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GLProfile;

import com.ardor3d.image.Image;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.util.ImageLoader;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.util.geom.BufferUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class JoglImageLoader implements ImageLoader {

    // private static final Logger logger = Logger.getLogger(JoglImageLoader.class.getName());

    private static boolean createOnHeap = false;

    private static final String[] supportedFormats = new String[] { TextureIO.DDS, TextureIO.GIF, TextureIO.JPG,
            TextureIO.JPG, TextureIO.PAM, TextureIO.PNG, TextureIO.PNG, TextureIO.PPM, TextureIO.SGI, TextureIO.TGA,
            TextureIO.TIFF };

    public static String[] getSupportedFormats() {
        return supportedFormats;
    }

    public static void registerLoader() {}

    public JoglImageLoader() {}

    @Override
    public Image load(final InputStream is, final boolean flipped) throws IOException {
        final TextureData textureData = TextureIO.newTextureData(GLProfile.getMaximum(true), is, true, null);
        final Buffer textureDataBuffer = textureData.getBuffer();
        final Image ardorImage = new Image();

        int dataSize = textureDataBuffer.capacity();
        if (textureDataBuffer instanceof ShortBuffer) {
            dataSize *= Buffers.SIZEOF_SHORT;
        } else {
            if (textureDataBuffer instanceof IntBuffer) {
                dataSize *= Buffers.SIZEOF_INT;
            } else {
                if (textureDataBuffer instanceof LongBuffer) {
                    dataSize *= Buffers.SIZEOF_LONG;
                } else {
                    if (textureDataBuffer instanceof FloatBuffer) {
                        dataSize *= Buffers.SIZEOF_FLOAT;
                    } else {
                        if (textureDataBuffer instanceof DoubleBuffer) {
                            dataSize *= Buffers.SIZEOF_DOUBLE;
                        }
                    }
                }
            }
        }
        final ByteBuffer scratch = createOnHeap ? BufferUtils.createByteBufferOnHeap(dataSize) : Buffers
                .newDirectByteBuffer(dataSize);
        if (textureDataBuffer instanceof ShortBuffer) {
            final ShortBuffer shortTextureDataBuffer = (ShortBuffer) textureDataBuffer;
            while (textureDataBuffer.hasRemaining()) {
                scratch.putShort(shortTextureDataBuffer.get());
            }
        } else {
            if (textureDataBuffer instanceof IntBuffer) {
                final IntBuffer intTextureDataBuffer = (IntBuffer) textureDataBuffer;
                while (textureDataBuffer.hasRemaining()) {
                    scratch.putInt(intTextureDataBuffer.get());
                }
            } else {
                if (textureDataBuffer instanceof LongBuffer) {
                    final LongBuffer longTextureDataBuffer = (LongBuffer) textureDataBuffer;
                    while (textureDataBuffer.hasRemaining()) {
                        scratch.putLong(longTextureDataBuffer.get());
                    }
                } else {
                    if (textureDataBuffer instanceof FloatBuffer) {
                        final FloatBuffer floatTextureDataBuffer = (FloatBuffer) textureDataBuffer;
                        while (textureDataBuffer.hasRemaining()) {
                            scratch.putFloat(floatTextureDataBuffer.get());
                        }
                    } else {
                        if (textureDataBuffer instanceof DoubleBuffer) {
                            final DoubleBuffer doubleTextureDataBuffer = (DoubleBuffer) textureDataBuffer;
                            while (textureDataBuffer.hasRemaining()) {
                                scratch.putDouble(doubleTextureDataBuffer.get());
                            }
                        } else {
                            if (textureDataBuffer instanceof ByteBuffer) {
                                scratch.put((ByteBuffer) textureDataBuffer);
                            }
                        }
                    }
                }
            }
        }
        scratch.rewind();
        textureDataBuffer.rewind();
        ardorImage.setWidth(textureData.getWidth());
        ardorImage.setHeight(textureData.getHeight());
        ardorImage.setData(scratch);
        ardorImage.setDataFormat(JoglTextureUtil.getImageDataFormat(textureData.getPixelFormat()));
        // ardorImage.setDataType(JoglTextureUtil.getPixelDataType(textureData.getPixelType()));
        ardorImage.setDataType(PixelDataType.UnsignedByte);
        return ardorImage;
    }
}
