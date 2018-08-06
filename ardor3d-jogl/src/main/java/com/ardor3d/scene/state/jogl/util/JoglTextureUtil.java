/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl.util;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GL2GL3;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture.DepthTextureCompareFunc;
import com.ardor3d.image.Texture.DepthTextureCompareMode;
import com.ardor3d.image.Texture.DepthTextureMode;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.TextureStoreFormat;

public abstract class JoglTextureUtil {

    public static int getGLInternalFormat(final TextureStoreFormat format) {
        switch (format) {
        // first some frequently used formats
            case RGBA8:
                return GL.GL_RGBA8;
            case RGB8:
                return GL.GL_RGB8;
            case CompressedRGBA:
                return GL2GL3.GL_COMPRESSED_RGBA;
            case CompressedRGB:
                return GL2GL3.GL_COMPRESSED_RGB;
            case CompressedRG:
                return GL2GL3.GL_COMPRESSED_RG;
            case CompressedRed:
                return GL2GL3.GL_COMPRESSED_RED;
            case NativeDXT1:
                return GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            case NativeDXT1A:
                return GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            case NativeDXT3:
                return GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
            case NativeDXT5:
                return GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
            case NativeLATC_L:
                return GL2.GL_COMPRESSED_LUMINANCE_LATC1_EXT;
            case NativeLATC_LA:
                return GL2.GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;

            case R3G3B2:
                return GL2GL3.GL_R3_G3_B2;
            case RGB4:
                return GL2GL3.GL_RGB4;
            case RGB5:
                return GL2GL3.GL_RGB5;
            case RGB10:
                return GL.GL_RGB10;
            case RGB12:
                return GL2GL3.GL_RGB12;
            case RGB16:
                return GL2GL3.GL_RGB16;
            case RGBA2:
                return GL2GL3.GL_RGBA2;
            case RGBA4:
                return GL.GL_RGBA4;
            case RGB5A1:
                return GL.GL_RGB5_A1;
            case RGB10A2:
                return GL.GL_RGB10_A2;
            case RGBA12:
                return GL2GL3.GL_RGBA12;
            case RGBA16:
                return GL2GL3.GL_RGBA16;
            case Depth:
                return GL2ES2.GL_DEPTH_COMPONENT;
            case Depth16:
                return GL.GL_DEPTH_COMPONENT16;
            case Depth24:
                return GL.GL_DEPTH_COMPONENT24;
            case Depth32:
                return GL.GL_DEPTH_COMPONENT32;
            case Depth32F:
                return GL2ES3.GL_DEPTH_COMPONENT32F;
            case RGB16F:
                return GL.GL_RGB16F;
            case RGB32F:
                return GL.GL_RGB32F;
            case RGBA16F:
                return GL.GL_RGBA16F;
            case RGBA32F:
                return GL.GL_RGBA32F;
            case R8:
                return GL2ES2.GL_R8;
            case R16:
                return GL2GL3.GL_R16;
            case RG8:
                return GL2ES2.GL_RG8;
            case RG16:
                return GL2GL3.GL_RG16;
            case R16F:
                return GL2ES2.GL_R16F;
            case R32F:
                return GL2ES2.GL_R32F;
            case RG16F:
                return GL2ES2.GL_RG16F;
            case RG32F:
                return GL2ES2.GL_RG32F;
            case R8I:
                return GL2ES3.GL_R8I;
            case R8UI:
                return GL2ES3.GL_R8UI;
            case R16I:
                return GL2ES3.GL_R16I;
            case R16UI:
                return GL2ES3.GL_R16UI;
            case R32I:
                return GL2ES3.GL_R32I;
            case R32UI:
                return GL2ES3.GL_R32UI;
            case RG8I:
                return GL2ES3.GL_RG8I;
            case RG8UI:
                return GL2ES3.GL_RG8UI;
            case RG16I:
                return GL2ES3.GL_RG16I;
            case RG16UI:
                return GL2ES3.GL_RG16UI;
            case RG32I:
                return GL2ES3.GL_RG32I;
            case RG32UI:
                return GL2ES3.GL_RG32UI;
            default:
                break;
        }
        throw new IllegalArgumentException("Incorrect format set: " + format);
    }

    public static int getGLPixelDataType(final PixelDataType type) {
        switch (type) {
            case Byte:
                return GL.GL_BYTE;
            case Float:
                return GL.GL_FLOAT;
            case HalfFloat:
                return GL.GL_HALF_FLOAT;
            case Short:
                return GL.GL_SHORT;
            case UnsignedShort:
                return GL.GL_UNSIGNED_SHORT;
            case Int:
                return GL2ES2.GL_INT;
            case UnsignedInt:
                return GL.GL_UNSIGNED_INT;
            case UnsignedByte:
                return GL.GL_UNSIGNED_BYTE;
            default:
                throw new Error("Unhandled type: " + type);
        }
    }

    public static PixelDataType getPixelDataType(final int glPixelDataType) {
        switch (glPixelDataType) {
            case GL.GL_BYTE:
                return PixelDataType.Byte;
            case GL.GL_FLOAT:
                return PixelDataType.Float;
            case GL.GL_HALF_FLOAT:
                return PixelDataType.HalfFloat;
            case GL.GL_SHORT:
                return PixelDataType.Short;
            case GL.GL_UNSIGNED_SHORT:
                return PixelDataType.UnsignedShort;
            case GL2ES2.GL_INT:
                return PixelDataType.Int;
            case GL.GL_UNSIGNED_INT:
                return PixelDataType.UnsignedInt;
            case GL.GL_UNSIGNED_BYTE:
                return PixelDataType.UnsignedByte;
            default:
                throw new Error("Unhandled gl pixel data type: " + glPixelDataType);
        }
    }

    public static ImageDataFormat getImageDataFormat(final int glPixelFormat) {
        switch (glPixelFormat) {
            case GL.GL_RGBA:
                return ImageDataFormat.RGBA;
            case GL.GL_RGB:
                return ImageDataFormat.RGB;
            case GL.GL_ALPHA:
                return ImageDataFormat.Alpha;
            case GL.GL_LUMINANCE:
                return ImageDataFormat.Luminance;
            case GL2.GL_INTENSITY:
                return ImageDataFormat.Intensity;
            case GL.GL_LUMINANCE_ALPHA:
                return ImageDataFormat.LuminanceAlpha;
            case GL2ES2.GL_DEPTH_COMPONENT:
                return ImageDataFormat.Depth;
            case GL2GL3.GL_BGR:
                return ImageDataFormat.BGR;
            case GL.GL_BGRA:
                return ImageDataFormat.BGRA;
            case GL2ES2.GL_RED:
                return ImageDataFormat.Red;
            case GL2ES3.GL_BLUE:
                return ImageDataFormat.Blue;
            case GL2ES3.GL_GREEN:
                return ImageDataFormat.Green;
            case GL2ES2.GL_STENCIL_INDEX:
                return ImageDataFormat.StencilIndex;
            case GL2ES2.GL_RG:
                return ImageDataFormat.RG;
            default:
                break;
        }
        throw new IllegalArgumentException("Incorrect gl pixel format set: " + glPixelFormat);
    }

    public static int getGLPixelFormat(final ImageDataFormat format) {
        switch (format) {
            case RGBA:
                return GL.GL_RGBA;
            case RGB:
                return GL.GL_RGB;
            case Alpha:
                return GL.GL_ALPHA;
            case Luminance:
                return GL.GL_LUMINANCE;
            case Intensity:
                return GL2.GL_INTENSITY;
            case LuminanceAlpha:
                return GL.GL_LUMINANCE_ALPHA;
            case Depth:
                return GL2ES2.GL_DEPTH_COMPONENT;
            case BGR:
                return GL2GL3.GL_BGR;
            case BGRA:
                return GL.GL_BGRA;
            case Red:
                return GL2ES2.GL_RED;
            case Blue:
                return GL2ES3.GL_BLUE;
            case Green:
                return GL2ES3.GL_GREEN;
            case StencilIndex:
                return GL2ES2.GL_STENCIL_INDEX;
            case RG:
                return GL2ES2.GL_RG;
            case PrecompressedDXT1:
                break;
            case PrecompressedDXT1A:
                break;
            case PrecompressedDXT3:
                break;
            case PrecompressedDXT5:
                break;
            case PrecompressedLATC_L:
                break;
            case PrecompressedLATC_LA:
                break;
            default:
                break;
        }
        throw new IllegalArgumentException("Incorrect format set: " + format);
    }

    public static int getGLPixelFormatFromStoreFormat(final TextureStoreFormat format) {
        switch (format) {
            case RGBA2:
            case RGBA4:
            case RGBA8:
            case RGB5A1:
            case RGB10A2:
            case RGBA12:
            case RGBA16:
            case CompressedRGBA:
            case NativeDXT1A:
            case NativeDXT3:
            case NativeDXT5:
            case RGBA16F:
            case RGBA32F:
                return GL.GL_RGBA;
            case R3G3B2:
            case RGB4:
            case RGB5:
            case RGB8:
            case RGB10:
            case RGB12:
            case RGB16:
            case CompressedRGB:
            case NativeDXT1:
            case RGB16F:
            case RGB32F:
                return GL.GL_RGB;
            case NativeLATC_L:
                return GL.GL_LUMINANCE;
            case NativeLATC_LA:
                return GL.GL_LUMINANCE_ALPHA;
            case Depth:
            case Depth16:
            case Depth24:
            case Depth32:
            case Depth32F:
                return GL2ES2.GL_DEPTH_COMPONENT;
            case R8:
            case R16:
            case R16F:
            case R32F:
            case R8I:
            case R8UI:
            case R16I:
            case R16UI:
            case R32I:
            case R32UI:
            case CompressedRed:
                return GL2ES2.GL_RED;
            case RG8:
            case RG16:
            case RG16F:
            case RG32F:
            case RG8I:
            case RG8UI:
            case RG16I:
            case RG16UI:
            case RG32I:
            case RG32UI:
            case CompressedRG:
                return GL2ES2.GL_RG;
            default:
                break;
        }
        throw new IllegalArgumentException("Incorrect format set: " + format);
    }

    public static int getGLDepthTextureMode(final DepthTextureMode mode) {
        switch (mode) {
            case Alpha:
                return GL.GL_ALPHA;
            case Luminance:
                return GL.GL_LUMINANCE;
            case Intensity:
            default:
                return GL2.GL_INTENSITY;
        }
    }

    public static int getGLDepthTextureCompareMode(final DepthTextureCompareMode mode) {
        switch (mode) {
            case RtoTexture:
                return GL2.GL_COMPARE_R_TO_TEXTURE;
            case None:
            default:
                return GL.GL_NONE;
        }
    }

    public static int getGLDepthTextureCompareFunc(final DepthTextureCompareFunc func) {
        switch (func) {
            case GreaterThanEqual:
                return GL.GL_GEQUAL;
            case LessThanEqual:
            default:
                return GL.GL_LEQUAL;
        }
    }

    public static int getGLMagFilter(final MagnificationFilter magFilter) {
        switch (magFilter) {
            case Bilinear:
                return GL.GL_LINEAR;
            case NearestNeighbor:
            default:
                return GL.GL_NEAREST;

        }
    }

    public static int getGLMinFilter(final MinificationFilter filter) {
        switch (filter) {
            case BilinearNoMipMaps:
                return GL.GL_LINEAR;
            case Trilinear:
                return GL.GL_LINEAR_MIPMAP_LINEAR;
            case BilinearNearestMipMap:
                return GL.GL_LINEAR_MIPMAP_NEAREST;
            case NearestNeighborNoMipMaps:
                return GL.GL_NEAREST;
            case NearestNeighborNearestMipMap:
                return GL.GL_NEAREST_MIPMAP_NEAREST;
            case NearestNeighborLinearMipMap:
                return GL.GL_NEAREST_MIPMAP_LINEAR;
        }
        throw new IllegalArgumentException("invalid MinificationFilter type: " + filter);
    }
}
