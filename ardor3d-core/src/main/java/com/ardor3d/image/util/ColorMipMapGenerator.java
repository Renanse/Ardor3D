/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import java.nio.ByteBuffer;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.BufferUtils;

/**
 *
 * <code>ColorMipMapGenerator</code> is useful for producing textures where consecutive mipmaps are different colors,
 * allowing you to visually see how mipmaps are used in a scene.
 *
 */
public abstract class ColorMipMapGenerator {

    /**
     * Generates an ardor3d Image object containing a mipmapped Image. Each mipmap is a solid color. The first X mipmap
     * colors are defined in topColors, any remaining mipmaps are a shade of default color.
     *
     * @param size
     *            dimensions of the texture (square)
     * @param topColors
     *            initial colors to use for the mipmaps
     * @param defaultColor
     *            color to use for remaining mipmaps, scaled darker for each successive mipmap
     * @return generated Image object
     */
    public static Image generateColorMipMap(final int size, final ColorRGBA[] topColors, final ColorRGBA defaultColor) {

        if (!MathUtils.isPowerOfTwo(size)) {
            throw new Ardor3dException("size must be power of two!");
        }

        final int mips = (int) (MathUtils.log(size, 2)) + 1;

        int bufLength = size * size * 4;
        final int[] mipLengths = new int[mips];
        mipLengths[0] = bufLength;
        for (int x = 1; x < mips; x++) {
            mipLengths[x] = mipLengths[x - 1] >> 1;
            bufLength += (mipLengths[x]);
        }

        final ByteBuffer bb = BufferUtils.createByteBuffer(bufLength);

        final int[] base = new int[] { (int) (defaultColor.getRed() * 255), (int) (defaultColor.getGreen() * 255),
                (int) (defaultColor.getBlue() * 255) };

        for (int x = 0; x < mips; x++) {
            final int length = mipLengths[x] >> 2;
            final float div = (float) (mips - x + topColors.length) / mips;
            for (int i = 0; i < length; i++) {
                if (x >= topColors.length) {
                    bb.put((byte) (base[0] * div));
                    bb.put((byte) (base[1] * div));
                    bb.put((byte) (base[2] * div));
                } else {
                    bb.put((byte) (topColors[x].getRed() * 255));
                    bb.put((byte) (topColors[x].getGreen() * 255));
                    bb.put((byte) (topColors[x].getBlue() * 255));
                }
                bb.put((byte) 255);
            }
        }
        bb.rewind();

        return new Image(ImageDataFormat.RGBA, PixelDataType.UnsignedByte, size, size, bb, mipLengths);
    }
}
