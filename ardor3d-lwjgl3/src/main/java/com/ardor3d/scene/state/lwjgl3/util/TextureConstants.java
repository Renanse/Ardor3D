/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scene.state.lwjgl3.util;

import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.EXTTextureCompressionLATC;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL40C;
import org.lwjgl.opengl.GL44C;

import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture.DepthTextureCompareFunc;
import com.ardor3d.image.Texture.DepthTextureCompareMode;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureStoreFormat;

/**
 * Utility class offering conversion from Ardor3D enums to LWJGL OpenGL constants.
 */
public final class TextureConstants {

  public static int getGLType(final Type type) {
    return switch (type) {
      case TwoDimensional -> GL11C.GL_TEXTURE_2D;
      case OneDimensional -> GL11C.GL_TEXTURE_1D;
      case ThreeDimensional -> GL12C.GL_TEXTURE_3D;
      case CubeMap -> GL13C.GL_TEXTURE_CUBE_MAP;
      case TwoDimensionalArray -> GL30C.GL_TEXTURE_2D_ARRAY;
      case OneDimensionalArray -> GL30C.GL_TEXTURE_1D_ARRAY;
      case CubeMapArray -> GL40C.GL_TEXTURE_CUBE_MAP_ARRAY;
      default -> throw new IllegalArgumentException("invalid texture type: " + type);
    };
  }

  public static int getGLCubeMapFace(final TextureCubeMap.Face face) {
    return switch (face) {
      case PositiveX -> GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
      case NegativeX -> GL13C.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
      case PositiveY -> GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
      case NegativeY -> GL13C.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
      case PositiveZ -> GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
      case NegativeZ -> GL13C.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
      default -> throw new IllegalArgumentException("invalid cube map face: " + face);
    };
  }

  public static int getGLWrap(final WrapMode wrap) {
    return switch (wrap) {
      case Repeat -> GL11C.GL_REPEAT;
      case MirroredRepeat -> GL14C.GL_MIRRORED_REPEAT;
      case BorderClamp -> GL13C.GL_CLAMP_TO_BORDER;
      case MirrorEdgeClamp -> GL44C.GL_MIRROR_CLAMP_TO_EDGE;
      case EdgeClamp -> GL12C.GL_CLAMP_TO_EDGE;
      default -> throw new IllegalArgumentException("invalid WrapMode type: " + wrap);
    };
  }

  public static int getGLInternalFormat(final TextureStoreFormat format) {
    return switch (format) {
      // first some frequently used formats
      case RGBA8 -> GL11C.GL_RGBA8;
      case RGB8 -> GL11C.GL_RGB8;
      case CompressedRGBA -> GL13C.GL_COMPRESSED_RGBA;
      case CompressedRGB -> GL13C.GL_COMPRESSED_RGB;
      case CompressedRG -> GL30C.GL_COMPRESSED_RG;
      case CompressedRed -> GL30C.GL_COMPRESSED_RED;
      case NativeDXT1 -> EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
      case NativeDXT1A -> EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
      case NativeDXT3 -> EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
      case NativeDXT5 -> EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
      case NativeLATC_L -> EXTTextureCompressionLATC.GL_COMPRESSED_LUMINANCE_LATC1_EXT;
      case NativeLATC_LA -> EXTTextureCompressionLATC.GL_COMPRESSED_LUMINANCE_ALPHA_LATC2_EXT;

      // The rest...
      case R3G3B2 -> GL11C.GL_R3_G3_B2;
      case RGB4 -> GL11C.GL_RGB4;
      case RGB5 -> GL11C.GL_RGB5;
      case RGB10 -> GL11C.GL_RGB10;
      case RGB12 -> GL11C.GL_RGB12;
      case RGB16 -> GL11C.GL_RGB16;
      case RGBA2 -> GL11C.GL_RGBA2;
      case RGBA4 -> GL11C.GL_RGBA4;
      case RGB5A1 -> GL11C.GL_RGB5_A1;
      case RGB10A2 -> GL11C.GL_RGB10_A2;
      case RGBA12 -> GL11C.GL_RGBA12;
      case RGBA16 -> GL11C.GL_RGBA16;
      case Depth -> GL11C.GL_DEPTH_COMPONENT;
      case Depth16 -> GL14C.GL_DEPTH_COMPONENT16;
      case Depth24 -> GL14C.GL_DEPTH_COMPONENT24;
      case Depth32 -> GL14C.GL_DEPTH_COMPONENT32;
      case Depth32F -> GL30C.GL_DEPTH_COMPONENT32F;
      case RGB16F -> GL30C.GL_RGB16F;
      case RGB32F -> GL30C.GL_RGB32F;
      case RGBA16F -> GL30C.GL_RGBA16F;
      case RGBA32F -> GL30C.GL_RGBA32F;
      case R8 -> GL30C.GL_R8;
      case R16 -> GL30C.GL_R16;
      case RG8 -> GL30C.GL_RG8;
      case RG16 -> GL30C.GL_RG16;
      case R16F -> GL30C.GL_R16F;
      case R32F -> GL30C.GL_R32F;
      case RG16F -> GL30C.GL_RG16F;
      case RG32F -> GL30C.GL_RG32F;
      case R8I -> GL30C.GL_R8I;
      case R8UI -> GL30C.GL_R8UI;
      case R16I -> GL30C.GL_R16I;
      case R16UI -> GL30C.GL_R16UI;
      case R32I -> GL30C.GL_R32I;
      case R32UI -> GL30C.GL_R32UI;
      case RG8I -> GL30C.GL_RG8I;
      case RG8UI -> GL30C.GL_RG8UI;
      case RG16I -> GL30C.GL_RG16I;
      case RG16UI -> GL30C.GL_RG16UI;
      case RG32I -> GL30C.GL_RG32I;
      case RG32UI -> GL30C.GL_RG32UI;
      default -> throw new IllegalArgumentException("Incorrect format set: " + format);
    };
  }

  public static int getGLPixelDataType(final PixelDataType type) {
    return switch (type) {
      case Byte -> GL11C.GL_BYTE;
      case Float -> GL11C.GL_FLOAT;
      case HalfFloat -> GL30C.GL_HALF_FLOAT;
      case Short -> GL11C.GL_SHORT;
      case UnsignedShort -> GL11C.GL_UNSIGNED_SHORT;
      case Int -> GL11C.GL_INT;
      case UnsignedInt -> GL11C.GL_UNSIGNED_INT;
      case UnsignedByte -> GL11C.GL_UNSIGNED_BYTE;
      case UnsignedByte_3_3_2 -> GL12C.GL_UNSIGNED_BYTE_3_3_2;
      case UnsignedByte_2_3_3_Rev -> GL12C.GL_UNSIGNED_BYTE_2_3_3_REV;
      case UnsignedShort_5_6_5 -> GL12C.GL_UNSIGNED_SHORT_5_6_5;
      case UnsignedShort_5_6_5_Rev -> GL12C.GL_UNSIGNED_SHORT_5_6_5_REV;
      case UnsignedShort_4_4_4_4 -> GL12C.GL_UNSIGNED_SHORT_4_4_4_4;
      case UnsignedShort_4_4_4_4_Rev -> GL12C.GL_UNSIGNED_SHORT_4_4_4_4_REV;
      case UnsignedShort_5_5_5_1 -> GL12C.GL_UNSIGNED_SHORT_5_5_5_1;
      case UnsignedShort_1_5_5_5_Rev -> GL12C.GL_UNSIGNED_SHORT_1_5_5_5_REV;
      case UnsignedInt_8_8_8_8 -> GL12C.GL_UNSIGNED_INT_8_8_8_8;
      case UnsignedInt_8_8_8_8_Rev -> GL12C.GL_UNSIGNED_INT_8_8_8_8_REV;
      case UnsignedInt_10_10_10_2 -> GL12C.GL_UNSIGNED_INT_10_10_10_2;
      case UnsignedInt_2_10_10_10_Rev -> GL12C.GL_UNSIGNED_INT_2_10_10_10_REV;
      default -> throw new Error("Unhandled type: " + type);
    };
  }

  public static int getGLPixelFormat(final ImageDataFormat format) {
    return switch (format) {
      case RGBA -> GL11C.GL_RGBA;
      case RGB -> GL11C.GL_RGB;
      case RG -> GL30C.GL_RG;
      case Alpha -> GL11C.GL_ALPHA;
      case Depth -> GL11C.GL_DEPTH_COMPONENT;
      case BGR -> GL12C.GL_BGR;
      case BGRA -> GL12C.GL_BGRA;
      case Red -> GL11C.GL_RED;
      case Blue -> GL11C.GL_BLUE;
      case Green -> GL11C.GL_GREEN;
      case StencilIndex -> GL11C.GL_STENCIL_INDEX;
      default -> throw new IllegalArgumentException("Incorrect format set: " + format);
    };
  }

  public static ImageDataFormat getImageDataFormatFromStoreFormat(final TextureStoreFormat format) {
    return switch (format) {
      case RGBA2, RGBA4, RGBA8, RGB5A1, RGB10A2, RGBA12, RGBA16, CompressedRGBA, NativeDXT1A, NativeDXT3, NativeDXT5, RGBA16F, RGBA32F ->
          ImageDataFormat.RGBA;
      case R3G3B2, RGB4, RGB5, RGB8, RGB10, RGB12, RGB16, CompressedRGB, NativeDXT1, RGB16F, RGB32F ->
          ImageDataFormat.RGB;
      case Depth, Depth16, Depth24, Depth32, Depth32F -> ImageDataFormat.Depth;
      case R8, R16, R16F, R32F, R8I, R8UI, R16I, R16UI, R32I, R32UI, CompressedRed, NativeLATC_L -> ImageDataFormat.Red;
      case RG8, RG16, RG16F, RG32F, RG8I, RG8UI, RG16I, RG16UI, RG32I, RG32UI, CompressedRG, NativeLATC_LA ->
          ImageDataFormat.RG;
      default -> throw new IllegalArgumentException("Incorrect format set: " + format);
    };
  }

  public static int getGLPixelFormatFromStoreFormat(final TextureStoreFormat format) {
    return switch (format) {
      case RGBA2, RGBA4, RGBA8, RGB5A1, RGB10A2, RGBA12, RGBA16, CompressedRGBA, NativeDXT1A, NativeDXT3, NativeDXT5, RGBA16F, RGBA32F ->
          GL11C.GL_RGBA;
      case R3G3B2, RGB4, RGB5, RGB8, RGB10, RGB12, RGB16, CompressedRGB, NativeDXT1, RGB16F, RGB32F -> GL11C.GL_RGB;
      case NativeLATC_L -> GL11.GL_LUMINANCE; // XXX: Not sure about this in core
      case NativeLATC_LA -> GL11.GL_LUMINANCE_ALPHA; // XXX: Not sure about this in core
      case Depth, Depth16, Depth24, Depth32, Depth32F -> GL11C.GL_DEPTH_COMPONENT;
      case R8, R16, R16F, R32F, R8I, R8UI, R16I, R16UI, R32I, R32UI, CompressedRed -> GL11C.GL_RED;
      case RG8, RG16, RG16F, RG32F, RG8I, RG8UI, RG16I, RG16UI, RG32I, RG32UI, CompressedRG -> GL30C.GL_RG;
      default -> throw new IllegalArgumentException("Incorrect format set: " + format);
    };
  }

  public static int getGLDepthTextureCompareMode(final DepthTextureCompareMode mode) {
    return switch (mode) {
      case RtoTexture -> ARBShadow.GL_COMPARE_R_TO_TEXTURE_ARB;
      case None -> GL11C.GL_NONE;
      default -> throw new IllegalArgumentException("Unhandled DepthTextureCompareMode: " + mode);
    };
  }

  public static int getGLDepthTextureCompareFunc(final DepthTextureCompareFunc func) {
    return switch (func) {
      case GreaterThanEqual -> GL11C.GL_GEQUAL;
      case LessThanEqual -> GL11C.GL_LEQUAL;
      default -> throw new IllegalArgumentException("Unhandled DepthTextureCompareFunc: " + func);
    };
  }

  public static int getGLMagFilter(final MagnificationFilter magFilter) {
    return switch (magFilter) {
      case Bilinear -> GL11C.GL_LINEAR;
      case NearestNeighbor -> GL11C.GL_NEAREST;
      default -> throw new IllegalArgumentException("Unhandled MagnificationFilter type: " + magFilter);
    };
  }

  public static int getGLMinFilter(final MinificationFilter filter) {
    return switch (filter) {
      case BilinearNoMipMaps -> GL11C.GL_LINEAR;
      case Trilinear -> GL11C.GL_LINEAR_MIPMAP_LINEAR;
      case BilinearNearestMipMap -> GL11C.GL_LINEAR_MIPMAP_NEAREST;
      case NearestNeighborNoMipMaps -> GL11C.GL_NEAREST;
      case NearestNeighborNearestMipMap -> GL11C.GL_NEAREST_MIPMAP_NEAREST;
      case NearestNeighborLinearMipMap -> GL11C.GL_NEAREST_MIPMAP_LINEAR;
      default -> throw new IllegalArgumentException("Unhandled MinificationFilter type: " + filter);
    };
  }
}
