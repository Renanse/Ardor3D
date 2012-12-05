/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.image.util;

import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.TextureStoreFormat;

public abstract class ImageUtils {

    public static final int getPixelByteSize(final ImageDataFormat format, final PixelDataType type) {
        return type.getBytesPerPixel(format.getComponents());
    }

    public static final TextureStoreFormat getTextureStoreFormat(final TextureStoreFormat format, final Image image) {
        if (format != TextureStoreFormat.GuessCompressedFormat && format != TextureStoreFormat.GuessNoCompressedFormat) {
            return format;
        }
        if (image == null) {
            throw new Error("Unable to guess format type... Image is null.");
        }

        final PixelDataType type = image.getDataType();
        final ImageDataFormat dataFormat = image.getDataFormat();
        switch (dataFormat) {
            case ColorIndex:
            case BGRA:
            case RGBA:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedRGBA;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.RGBA8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.RGBA16;
                    case HalfFloat:
                        return TextureStoreFormat.RGBA16F;
                    case Float:
                        return TextureStoreFormat.RGBA32F;
                }
                break;
            case BGR:
            case RGB:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedRGB;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.RGB8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.RGB16;
                    case HalfFloat:
                        return TextureStoreFormat.RGB16F;
                    case Float:
                        return TextureStoreFormat.RGB32F;
                }
                break;
            case RG:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedRG;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.RG8;
                    case Short:
                    case UnsignedShort:
                        return TextureStoreFormat.RG16;
                    case Int:
                        return TextureStoreFormat.RG16I;
                    case UnsignedInt:
                        return TextureStoreFormat.RG16UI;
                    case HalfFloat:
                        return TextureStoreFormat.RG16F;
                    case Float:
                        return TextureStoreFormat.RG32F;
                }
                break;
            case Luminance:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedLuminance;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.Luminance8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.Luminance16;
                    case HalfFloat:
                        return TextureStoreFormat.Luminance16F;
                    case Float:
                        return TextureStoreFormat.Luminance32F;
                }
                break;
            case LuminanceAlpha:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedLuminanceAlpha;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.Luminance4Alpha4;
                    case Short:
                    case UnsignedShort:
                        return TextureStoreFormat.Luminance8Alpha8;
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.Luminance16Alpha16;
                    case HalfFloat:
                        return TextureStoreFormat.LuminanceAlpha16F;
                    case Float:
                        return TextureStoreFormat.LuminanceAlpha32F;
                }
                break;
            case Alpha:
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.Alpha8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.Alpha16;
                    case HalfFloat:
                        return TextureStoreFormat.Alpha16F;
                    case Float:
                        return TextureStoreFormat.Alpha32F;
                }
                break;
            case Red:
                if (format == TextureStoreFormat.GuessCompressedFormat) {
                    return TextureStoreFormat.CompressedRed;
                }
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.R8;
                    case Short:
                    case UnsignedShort:
                        return TextureStoreFormat.R16;
                    case Int:
                        return TextureStoreFormat.R16I;
                    case UnsignedInt:
                        return TextureStoreFormat.R16UI;
                    case HalfFloat:
                        return TextureStoreFormat.R16F;
                    case Float:
                        return TextureStoreFormat.R32F;
                }
                break;
            case Intensity:
            case Green:
            case Blue:
            case StencilIndex:
                switch (type) {
                    case Byte:
                    case UnsignedByte:
                        return TextureStoreFormat.Intensity8;
                    case Short:
                    case UnsignedShort:
                    case Int:
                    case UnsignedInt:
                        return TextureStoreFormat.Intensity16;
                    case HalfFloat:
                        return TextureStoreFormat.Intensity16F;
                    case Float:
                        return TextureStoreFormat.Intensity32F;
                }
                break;
            case Depth:
                // XXX: Should we actually switch here? Depth textures can be slightly fussy.
                return TextureStoreFormat.Depth;
            case PrecompressedDXT1:
                return TextureStoreFormat.NativeDXT1;
            case PrecompressedDXT1A:
                return TextureStoreFormat.NativeDXT1A;
            case PrecompressedDXT3:
                return TextureStoreFormat.NativeDXT3;
            case PrecompressedDXT5:
                return TextureStoreFormat.NativeDXT5;
            case PrecompressedLATC_L:
                return TextureStoreFormat.NativeLATC_L;
            case PrecompressedLATC_LA:
                return TextureStoreFormat.NativeLATC_LA;
        }

        throw new Error("Unhandled type / format combination: " + type + " / " + dataFormat);
    }
}
