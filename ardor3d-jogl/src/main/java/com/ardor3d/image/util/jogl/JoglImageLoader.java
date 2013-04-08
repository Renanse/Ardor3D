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
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import com.ardor3d.framework.jogl.CapsUtil;
import com.ardor3d.image.Image;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.util.ImageLoader;
import com.ardor3d.image.util.ImageLoaderUtil;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.util.geom.BufferUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class JoglImageLoader implements ImageLoader {

    private static boolean createOnHeap = false;

    private enum TYPE {
        BYTE, SHORT, CHAR, INT, FLOAT, LONG, DOUBLE
    };

    private static final String[] supportedFormats = new String[] { "." + TextureIO.DDS.toUpperCase(),
            "." + TextureIO.GIF.toUpperCase(), "." + TextureIO.JPG.toUpperCase(), "." + TextureIO.PAM.toUpperCase(),
            "." + TextureIO.PNG.toUpperCase(), "." + TextureIO.PPM.toUpperCase(), "." + TextureIO.SGI.toUpperCase(),
            "." + TextureIO.TGA.toUpperCase(), "." + TextureIO.TIFF.toUpperCase() };

    public static String[] getSupportedFormats() {
        return supportedFormats;
    }

    public static void registerLoader() {
        ImageLoaderUtil.registerHandler(new JoglImageLoader(), supportedFormats);
    }

    public JoglImageLoader() {}

    @Override
    public Image load(final InputStream is, final boolean flipped) throws IOException {
        final TextureData textureData = TextureIO.newTextureData(CapsUtil.getProfile(), is, true, null);
        final Buffer textureDataBuffer = textureData.getBuffer();
        final Image ardorImage = new Image();
        final TYPE bufferDataType;
        if (textureDataBuffer instanceof ByteBuffer) {
            bufferDataType = TYPE.BYTE;
        } else {
            if (textureDataBuffer instanceof ShortBuffer) {
                bufferDataType = TYPE.SHORT;
            } else {
                if (textureDataBuffer instanceof CharBuffer) {
                    bufferDataType = TYPE.CHAR;
                } else {
                    if (textureDataBuffer instanceof IntBuffer) {
                        bufferDataType = TYPE.INT;
                    } else {
                        if (textureDataBuffer instanceof FloatBuffer) {
                            bufferDataType = TYPE.FLOAT;
                        } else {
                            if (textureDataBuffer instanceof LongBuffer) {
                                bufferDataType = TYPE.LONG;
                            } else {
                                if (textureDataBuffer instanceof DoubleBuffer) {
                                    bufferDataType = TYPE.DOUBLE;
                                } else {
                                    bufferDataType = null;
                                }
                            }
                        }
                    }
                }
            }
        }
        if (bufferDataType == null) {
            throw new UnsupportedOperationException("Unknown buffer type " + textureDataBuffer.getClass().getName());
        } else {
            final int pixelComponentSize;
            switch (bufferDataType) {
                case BYTE:
                    pixelComponentSize = Buffers.SIZEOF_BYTE;
                    break;
                case SHORT:
                    pixelComponentSize = Buffers.SIZEOF_SHORT;
                    break;
                case CHAR:
                    pixelComponentSize = Buffers.SIZEOF_CHAR;
                    break;
                case INT:
                    pixelComponentSize = Buffers.SIZEOF_INT;
                    break;
                case FLOAT:
                    pixelComponentSize = Buffers.SIZEOF_FLOAT;
                    break;
                case LONG:
                    pixelComponentSize = Buffers.SIZEOF_LONG;
                    break;
                case DOUBLE:
                    pixelComponentSize = Buffers.SIZEOF_DOUBLE;
                    break;
                default:
                    // it should never happen
                    pixelComponentSize = 0;
            }
            final int dataSize = textureDataBuffer.capacity() * pixelComponentSize;
            final ByteBuffer scratch = createOnHeap ? BufferUtils.createByteBufferOnHeap(dataSize) : Buffers
                    .newDirectByteBuffer(dataSize);
            if (flipped) {
                final int bytesPerPixel = dataSize / (textureData.getWidth() * textureData.getHeight());
                while (scratch.hasRemaining()) {
                    final int srcPixelIndex = scratch.position() / bytesPerPixel;
                    final int srcPixelComponentOffset = scratch.position() - (srcPixelIndex * bytesPerPixel);
                    // final int srcElementIndex = srcPixelIndex * bytesPerPixel + srcPixelComponentOffset;
                    final int srcColumnIndex = srcPixelIndex % textureData.getWidth();
                    final int scrRowIndex = (srcPixelIndex - srcColumnIndex) / textureData.getHeight();
                    final int dstColumnIndex = srcColumnIndex;
                    final int dstRowIndex = (textureData.getHeight() - 1) - scrRowIndex;
                    final int dstPixelIndex = dstRowIndex * textureData.getWidth() + dstColumnIndex;
                    final int dstPixelComponentOffset = srcPixelComponentOffset;
                    final int dstElementIndex = dstPixelIndex * bytesPerPixel + dstPixelComponentOffset;
                    switch (bufferDataType) {
                        case BYTE:
                            scratch.put(((ByteBuffer) textureDataBuffer).get(dstElementIndex));
                            break;
                        case SHORT:
                            scratch.putShort(((ShortBuffer) textureDataBuffer).get(dstElementIndex));
                            break;
                        case CHAR:
                            scratch.putChar(((CharBuffer) textureDataBuffer).get(dstElementIndex));
                            break;
                        case INT:
                            scratch.putInt(((IntBuffer) textureDataBuffer).get(dstElementIndex));
                            break;
                        case FLOAT:
                            scratch.putFloat(((FloatBuffer) textureDataBuffer).get(dstElementIndex));
                            break;
                        case LONG:
                            scratch.putLong(((LongBuffer) textureDataBuffer).get(dstElementIndex));
                            break;
                        case DOUBLE:
                            scratch.putDouble(((DoubleBuffer) textureDataBuffer).get(dstElementIndex));
                            break;
                        default:
                            // it should never happen
                    }
                }

            } else {
                switch (bufferDataType) {
                    case BYTE:
                        scratch.put((ByteBuffer) textureDataBuffer);
                        break;
                    case SHORT:
                        scratch.asShortBuffer().put((ShortBuffer) textureDataBuffer);
                        break;
                    case CHAR:
                        scratch.asCharBuffer().put((CharBuffer) textureDataBuffer);
                        break;
                    case INT:
                        scratch.asIntBuffer().put((IntBuffer) textureDataBuffer);
                        break;
                    case FLOAT:
                        scratch.asFloatBuffer().put((FloatBuffer) textureDataBuffer);
                    case LONG:
                        scratch.asLongBuffer().put((LongBuffer) textureDataBuffer);
                        break;
                    case DOUBLE:
                        scratch.asDoubleBuffer().put((DoubleBuffer) textureDataBuffer);
                        break;
                    default:
                        // it should never happen
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
}
