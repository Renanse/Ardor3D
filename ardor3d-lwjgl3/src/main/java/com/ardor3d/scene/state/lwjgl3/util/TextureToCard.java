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

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.util.Ardor3dException;

public abstract class TextureToCard {

  /**
   * Convenience method for sending a texture to the graphics card where the available data is only
   * the base level of the texture. Data is read from the Texture's Image and sent to level 0 of the
   * Texture.
   *
   * For CubeMaps, the first 6 items in the Image's Data list are sent. For Texture3D, all data
   * buffers in the Image are concatenated and sent as the base level.
   *
   * @param texture
   *          the Texture to send.
   */
  public static void sendNonMipMappedTexture(final Texture texture) {
    final var image = texture.getImage();
    final var hasBorder = texture.hasBorder();
    final var imageWidth = image.getWidth();
    final var imageHeight = image.getHeight();
    final var imageDepth = image.getDepth();
    final var storeFormat = texture.getTextureStoreFormat();
    final var dataFormat = image.getDataFormat();
    final var dataType = image.getDataType();

    ByteBuffer data;
    final Type type = texture.getType();
    if (type == Type.CubeMap) {
      for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
        data = image.getData(face.ordinal());
        data.rewind();
        sendTexture(type, face, 0, storeFormat, imageWidth, imageHeight, 0, hasBorder, dataFormat, dataType, data);
      }
      return;
    }

    if (type == Type.ThreeDimensional) {
      // special case - we maintain our image as slices
      data = BufferUtils.concat(image.getData());
    } else {
      // all other types...
      data = image.getData(0);
      data.rewind();
    }

    sendTexture(type, null, 0, storeFormat, imageWidth, imageHeight, imageDepth, hasBorder, dataFormat, dataType, data);
  }

  public static void sendMipMappedTexture(final Texture texture) {
    final Texture.Type type = texture.getType();
    final Image image = texture.getImage();
    if (type == Type.CubeMap) {
      // walk through each face...
      for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
        final var data = image.getData(face.ordinal());
        TextureToCard.sendMipMaps(texture, face, image, data);
      }
      return;
    }

    // all other types
    final var data = type == Type.ThreeDimensional ? BufferUtils.concat(image.getData()) : image.getData(0);
    TextureToCard.sendMipMaps(texture, null, image, data);
  }

  protected static void sendMipMaps(final Texture texture, final TextureCubeMap.Face face, final Image image,
      final ByteBuffer data) {
    final Texture.Type type = texture.getType();

    // send each mipmap for the current face
    final int[] mipSizes = image.getMipMapByteSizes();

    // figure out and set our max mip level
    int maxMipLevel = mipSizes.length - 1;
    if (texture.getTextureMaxLevel() >= 0) {
      maxMipLevel = Math.min(maxMipLevel, texture.getTextureMaxLevel());
    }
    GL11C.glTexParameteri(TextureConstants.getGLType(type), GL12C.GL_TEXTURE_MAX_LEVEL, maxMipLevel);

    for (int m = 0, pos = 0; m <= maxMipLevel; m++) {
      final int width = Math.max(1, image.getWidth() >> m);
      final int height = Math.max(1, image.getHeight() >> m);
      final int depth = Math.max(1, image.getDepth() >> m);

      data.position(pos);
      data.limit(pos + mipSizes[m]);

      TextureToCard.sendTexture(type, face, m, texture.getTextureStoreFormat(), width, height, depth,
          texture.hasBorder(), image.getDataFormat(), image.getDataType(), data);

      pos += mipSizes[m];
    }
  }

  public static void sendTexture(final Texture.Type type, final TextureCubeMap.Face face, final int level,
      final TextureStoreFormat storeFormat, final int width, final int height, final int depth, final boolean hasBorder,
      final ImageDataFormat dataFormat, final PixelDataType dataType, final ByteBuffer data) {
    if (storeFormat.isCompressed()) {
      sendCompressedTexture(type, face, level, storeFormat, width, height, depth, hasBorder, dataFormat, dataType,
          data);
    } else {
      sendUncompressedTexture(type, face, level, storeFormat, width, height, depth, hasBorder, dataFormat, dataType,
          data);
    }
  }

  protected static void sendUncompressedTexture(final Texture.Type type, final TextureCubeMap.Face face,
      final int level, final TextureStoreFormat storeFormat, final int width, final int height, final int depth,
      final boolean hasBorder, final ImageDataFormat dataFormat, final PixelDataType dataType, final ByteBuffer data) {

    final var glType = TextureConstants.getGLType(type);
    final var glStoreFormat = TextureConstants.getGLInternalFormat(storeFormat);
    final var glDataFormat = TextureConstants.getGLPixelFormat(dataFormat);
    final var glDataType = TextureConstants.getGLPixelDataType(dataType);

    switch (type) {
      case OneDimensional: {
        GL11C.glTexImage1D(glType, level, glStoreFormat, width, hasBorder ? 1 : 0, glDataFormat, glDataType, data);
        break;
      }

      case TwoDimensional:
      case OneDimensionalArray:
      case CubeMap: {
        final int target2D = type == Type.CubeMap ? TextureConstants.getGLCubeMapFace(face) : glType;
        GL11C.glTexImage2D(target2D, level, glStoreFormat, width, height, hasBorder ? 1 : 0, glDataFormat, glDataType,
            data);
        break;
      }

      case ThreeDimensional:
      case TwoDimensionalArray:
      case CubeMapArray: {
        GL12C.glTexImage3D(glType, level, glStoreFormat, width, height, depth, hasBorder ? 1 : 0, glDataFormat,
            glDataType, data);
        break;
      }

      default:
        throw new Ardor3dException("Unsupported texture type: " + type);
    }
  }

  protected static void sendCompressedTexture(final Texture.Type type, final TextureCubeMap.Face face, final int level,
      final TextureStoreFormat storeFormat, final int width, final int height, final int depth, final boolean hasBorder,
      final ImageDataFormat dataFormat, final PixelDataType dataType, final ByteBuffer data) {

    final var glType = TextureConstants.getGLType(type);
    final var glStoreFormat = TextureConstants.getGLInternalFormat(storeFormat);

    switch (type) {
      case OneDimensional: {
        GL13C.glCompressedTexImage1D(glType, level, glStoreFormat, width, hasBorder ? 1 : 0, data);
        break;
      }

      case TwoDimensional:
      case OneDimensionalArray:
      case CubeMap: {
        final int target2D = type == Type.CubeMap ? TextureConstants.getGLCubeMapFace(face) : glType;
        GL13C.glCompressedTexImage2D(target2D, level, glStoreFormat, width, height, hasBorder ? 1 : 0, data);
        break;
      }

      case ThreeDimensional:
      case TwoDimensionalArray:
      case CubeMapArray: {
        GL13C.glCompressedTexImage3D(glType, level, glStoreFormat, width, height, depth, hasBorder ? 1 : 0, data);
        break;
      }

      default:
        throw new Ardor3dException("Unsupported texture type: " + type);
    }
  }

}
