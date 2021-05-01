/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scene.state.lwjgl3.util;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.logging.Logger;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.image.util.ImageUtils;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.texture.ITextureUtils;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3TextureStateUtil;
import com.ardor3d.util.Ardor3dException;

public class Lwjgl3TextureUtils implements ITextureUtils {

  private static final Logger logger = Logger.getLogger(Lwjgl3TextureUtils.class.getName());

  @Override
  public void loadTexture(final Texture texture, final int unit) {
    Lwjgl3TextureStateUtil.load(texture, unit);
  }

  @Override
  public void deleteTexture(final Texture texture) {
    Lwjgl3TextureStateUtil.deleteTexture(texture);
  }

  @Override
  public void deleteTextureIds(final Collection<Integer> ids) {
    Lwjgl3TextureStateUtil.deleteTextureIds(ids);
  }

  @Override
  public void updateTexture1DSubImage(final Texture1D destination, final int dstOffsetX, final int dstWidth,
      final ByteBuffer source, final int srcOffsetX) {
    updateTexSubImage(destination, dstOffsetX, 0, 0, dstWidth, 0, 0, source, srcOffsetX, 0, 0, 0, 0, null);
  }

  @Override
  public void updateTexture2DSubImage(final Texture2D destination, final int dstOffsetX, final int dstOffsetY,
      final int dstWidth, final int dstHeight, final ByteBuffer source, final int srcOffsetX, final int srcOffsetY,
      final int srcTotalWidth) {
    updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX, srcOffsetY, 0,
        srcTotalWidth, 0, null);
  }

  @Override
  public void updateTexture3DSubImage(final Texture3D destination, final int dstOffsetX, final int dstOffsetY,
      final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
      final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
      final int srcTotalHeight) {
    updateTexSubImage(destination, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth, dstHeight, dstDepth, source,
        srcOffsetX, srcOffsetY, srcOffsetZ, srcTotalWidth, srcTotalHeight, null);
  }

  @Override
  public void updateTextureCubeMapSubImage(final TextureCubeMap destination, final Face dstFace, final int dstOffsetX,
      final int dstOffsetY, final int dstWidth, final int dstHeight, final ByteBuffer source, final int srcOffsetX,
      final int srcOffsetY, final int srcTotalWidth) {
    updateTexSubImage(destination, dstOffsetX, dstOffsetY, 0, dstWidth, dstHeight, 0, source, srcOffsetX, srcOffsetY, 0,
        srcTotalWidth, 0, dstFace);
  }

  private static void updateTexSubImage(final Texture destination, final int dstOffsetX, final int dstOffsetY,
      final int dstOffsetZ, final int dstWidth, final int dstHeight, final int dstDepth, final ByteBuffer source,
      final int srcOffsetX, final int srcOffsetY, final int srcOffsetZ, final int srcTotalWidth,
      final int srcTotalHeight, final Face dstFace) {

    // Ignore textures that do not have an id set
    if (destination.getTextureIdForContext(ContextManager.getCurrentContext()) == 0) {
      logger.warning("Attempting to update a texture that is not currently on the card.");
      return;
    }

    // Determine the original texture configuration, so that this method can
    // restore the texture configuration to its original state.
    final int origAlignment = GL11C.glGetInteger(GL11C.GL_UNPACK_ALIGNMENT);
    final int origRowLength = 0;
    final int origImageHeight = 0;
    final int origSkipPixels = 0;
    final int origSkipRows = 0;
    final int origSkipImages = 0;

    final int alignment = 1;

    int rowLength;
    if (srcTotalWidth == dstWidth) {
      // When the row length is zero, then the width parameter is used.
      // We use zero in these cases in the hope that we can avoid two
      // unnecessary calls to glPixelStorei.
      rowLength = 0;
    } else {
      // The number of pixels in a row is different than the number of
      // pixels in the region to be uploaded to the texture.
      rowLength = srcTotalWidth;
    }

    int imageHeight;
    if (srcTotalHeight == dstHeight) {
      // When the image height is zero, then the height parameter is used.
      // We use zero in these cases in the hope that we can avoid two
      // unnecessary calls to glPixelStorei.
      imageHeight = 0;
    } else {
      // The number of pixels in a row is different than the number of
      // pixels in the region to be uploaded to the texture.
      imageHeight = srcTotalHeight;
    }

    // Grab pixel format
    final int pixelFormat;
    if (destination.getImage() != null) {
      pixelFormat = TextureConstants.getGLPixelFormat(destination.getImage().getDataFormat());
    } else {
      pixelFormat = TextureConstants.getGLPixelFormatFromStoreFormat(destination.getTextureStoreFormat());
    }

    // bind...
    Lwjgl3TextureStateUtil.doTextureBind(destination, 0, false);

    // Update the texture configuration (when necessary).

    if (origAlignment != alignment) {
      GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, alignment);
    }
    if (origRowLength != rowLength) {
      GL11C.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, rowLength);
    }
    if (origSkipPixels != srcOffsetX) {
      GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_PIXELS, srcOffsetX);
    }
    // NOTE: The below will be skipped for texture types that don't support them because we are passing
    // in 0's.
    if (origSkipRows != srcOffsetY) {
      GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_ROWS, srcOffsetY);
    }
    if (origImageHeight != imageHeight) {
      GL11C.glPixelStorei(GL12C.GL_UNPACK_IMAGE_HEIGHT, imageHeight);
    }
    if (origSkipImages != srcOffsetZ) {
      GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_IMAGES, srcOffsetZ);
    }

    final var type = destination.getType();
    final var glType = TextureConstants.getGLType(type);

    // Upload the image region into the texture.
    try {
      switch (type) {
        case OneDimensional: {
          GL11C.glTexSubImage1D(glType, 0, dstOffsetX, dstWidth, pixelFormat, GL11C.GL_UNSIGNED_BYTE, source);
          break;
        }

        case TwoDimensional:
        case OneDimensionalArray:
        case CubeMap: {
          final int target2D = type == Type.CubeMap ? TextureConstants.getGLCubeMapFace(dstFace) : glType;
          GL11C.glTexSubImage2D(target2D, 0, dstOffsetX, dstOffsetY, dstWidth, dstHeight, pixelFormat,
              GL11C.GL_UNSIGNED_BYTE, source);
          break;
        }

        case ThreeDimensional:
        case TwoDimensionalArray:
        case CubeMapArray: {
          GL12C.glTexSubImage3D(GL12C.GL_TEXTURE_3D, 0, dstOffsetX, dstOffsetY, dstOffsetZ, dstWidth, dstHeight,
              dstDepth, pixelFormat, GL11C.GL_UNSIGNED_BYTE, source);
          break;
        }

        default:
          throw new Ardor3dException("Unsupported texture type: " + type);
      }
    } finally {
      // Restore the texture configuration (when necessary)...
      // Restore alignment.
      if (origAlignment != alignment) {
        GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, origAlignment);
      }
      // Restore row length.
      if (origRowLength != rowLength) {
        GL11C.glPixelStorei(GL11C.GL_UNPACK_ROW_LENGTH, origRowLength);
      }
      // Restore skip pixels.
      if (origSkipPixels != srcOffsetX) {
        GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_PIXELS, origSkipPixels);
      }
      // Restore skip rows.
      if (origSkipRows != srcOffsetY) {
        GL11C.glPixelStorei(GL11C.GL_UNPACK_SKIP_ROWS, origSkipRows);
      }
      // Restore image height.
      if (origImageHeight != imageHeight) {
        GL11C.glPixelStorei(GL12C.GL_UNPACK_IMAGE_HEIGHT, origImageHeight);
      }
      // Restore skip images.
      if (origSkipImages != srcOffsetZ) {
        GL11C.glPixelStorei(GL12C.GL_UNPACK_SKIP_IMAGES, origSkipImages);
      }
    }
  }

  @Override
  public ByteBuffer readTextureContents(final Texture texture, final int level, final int baseWidth,
      final int baseHeight, final ImageDataFormat imageFormat, final PixelDataType pixelType, final ByteBuffer store) {
    var rVal = store;

    // make sure texture is current
    Lwjgl3TextureStateUtil.doTextureBind(texture, 0, true);

    // make sure our buffer is big enough
    final int width = baseWidth >> level;
    final int height = baseHeight >> level;
    final int size = width * height * ImageUtils.getPixelByteSize(imageFormat, pixelType);
    if (rVal == null || rVal.capacity() < size) {
      rVal = BufferUtils.createByteBuffer(size);
    } else {
      rVal.limit(size);
      rVal.rewind();
    }

    // grab texture data in the specified format
    GL11C.glGetTexImage(//
        TextureConstants.getGLType(texture.getType()), // type
        level, // level
        TextureConstants.getGLPixelFormat(imageFormat), //
        TextureConstants.getGLPixelDataType(pixelType), //
        rVal // buffer
    );

    return rVal;
  }
}
