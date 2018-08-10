/**
 * Copyright (c) 2008-2010 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util.jogl;

import java.nio.ByteBuffer;
import java.util.List;

import javax.media.nativewindow.util.PixelFormat;

import com.ardor3d.framework.jogl.CapsUtil;
import com.ardor3d.image.Image;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.google.common.collect.Lists;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.texture.TextureData;

/**
 * Utility methods for converting Ardor3D Images to JOGL texture data.
 */
public class JoglImageUtil {

    /**
     * Convert the given Ardor3D Image to a List of TextureData instances. It is a List because Ardor3D Images may
     * contain multiple layers (for example, in the case of cube maps or 3D textures).
     *
     * @param input
     *            the Ardor3D Image to convert
     * @return the TextureData instance(s) created in the conversion
     */
    public static List<TextureData> convertToJogl(final Image input) {
        return (convertToJogl(new CapsUtil(), input));
    }

    /**
     * Convert the given Ardor3D Image to a List of TextureData instances. It is a List because Ardor3D Images may
     * contain multiple layers (for example, in the case of cube maps or 3D textures).
     *
     * @param capsUtil
     * @param input
     *            the Ardor3D Image to convert
     * @return the TextureData instance(s) created in the conversion
     */
    public static List<TextureData> convertToJogl(final CapsUtil capsUtil, final Image input) {
        // count the number of layers we will be converting.
        final int size = input.getData().size();

        // grab our image width and height
        final int width = input.getWidth(), height = input.getHeight();

        // create our return list
        final List<TextureData> rVal = Lists.newArrayList();

        // go through each layer
        for (int i = 0; i < size; i++) {
            final int border = 0;// the texture can have a border whereas an image has none
            // final int pixFormat = JoglTextureUtil.getGLPixelFormat(input.getDataFormat());
            final int pixDataType = JoglTextureUtil.getGLPixelDataType(input.getDataType());
            // final int bpp = ImageUtils.getPixelByteSize(input.getDataFormat(), input.getDataType());
            final ByteBuffer data = input.getData(i);
            data.rewind();
            final ByteBuffer dest = Buffers.copyByteBuffer(data);
            final PixelFormat pixelFormat;
            switch (input.getDataFormat()) {
                case RGBA:
                    pixelFormat = PixelFormat.RGBA8888;
                    break;
                case BGR:
                    pixelFormat = PixelFormat.BGR888;
                    break;
                case BGRA:
                    pixelFormat = PixelFormat.BGRA8888;
                    break;
                case RGB:
                    pixelFormat = PixelFormat.RGB888;
                    break;
                case Red:
                    pixelFormat = PixelFormat.LUMINANCE;
                    break;
                default:
                    pixelFormat = null;
            }
            final GLPixelAttributes pixelAtt = GLPixelAttributes.convert(pixelFormat, capsUtil.getProfile());
            // pixel data type = internal format?
            final TextureData image = new TextureData(capsUtil.getProfile(), pixDataType, width, height, border,
                    pixelAtt, false, false, false, dest, null);
            rVal.add(image);
        }
        return rVal;
    }
}
