/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scene.state.lwjgl3;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBShadow;
import org.lwjgl.opengl.ARBTextureCompression;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL44C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryStack;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.Texture.WrapAxis;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.renderer.state.record.TextureUnitRecord;
import com.ardor3d.scene.state.lwjgl3.util.Lwjgl3TextureUtils;
import com.ardor3d.util.Constants;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

public abstract class Lwjgl3TextureStateUtil {
  private static final Logger logger = Logger.getLogger(Lwjgl3TextureStateUtil.class.getName());

  public static void load(final Texture texture, final int unit) {
    if (texture == null) {
      return;
    }

    final RenderContext context = ContextManager.getCurrentContext();
    if (context == null) {
      logger.warning("RenderContext is null for texture: " + texture);
      return;
    }

    final ContextCapabilities caps = context.getCapabilities();
    final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);

    // Check we are in the right unit
    if (record != null) {
      checkAndSetUnit(unit, record, caps);
    }

    // Create the texture...
    // First, look for a texture in the cache just like ours
    final Texture cached = TextureManager.findCachedTexture(texture.getTextureKey());

    if (cached == null) {
      TextureManager.addToCache(texture);
    } else {
      final int textureId = cached.getTextureIdForContext(context);
      if (textureId != 0) {
        doTextureBind(cached, unit, false);
        return;
      }
    }

    // Create a new texture id for this texture
    final int textureId = GL11C.glGenTextures();

    // store the new id by our current gl context.
    texture.setTextureIdForContext(context, textureId);

    update(texture, unit);
  }

  /**
   * bind texture and upload image data to card
   */
  public static void update(final Texture texture, final int unit) {
    final RenderContext context = ContextManager.getCurrentContext();
    final ContextCapabilities caps = context.getCapabilities();

    texture.getTextureKey().markClean(context);

    // our texture type:
    final Texture.Type type = texture.getType();

    // bind our texture id to this unit.
    doTextureBind(texture, unit, false);

    // pass image data to OpenGL
    final Image image = texture.getImage();
    final boolean hasBorder = texture.hasBorder();
    if (image == null) {
      logger.warning("Image data for texture is null.");
    }

    // set alignment to support images with width % 4 != 0, as images are
    // not aligned
    GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, 1);

    // Get texture image data. Not all textures have image data.
    // For example, ApplyMode.Combine modes can use primary colors,
    // texture output, and constants to modify fragments via the
    // texture units.
    if (image != null) {
      final int maxSize = caps.getMaxTextureSize();
      final int actualWidth = image.getWidth();
      final int actualHeight = image.getHeight();

      if (actualWidth > maxSize || actualHeight > maxSize) {
        if (actualWidth > maxSize || actualHeight > maxSize) {
          logger.warning("(card unsupported) Attempted to apply texture with size bigger than max texture size ["
              + maxSize + "]: " + image.getWidth() + " x " + image.getHeight());
          return;
        }
      }

      if (!texture.getMinificationFilter().usesMipMapLevels() && !texture.getTextureStoreFormat().isCompressed()) {

        // Load textures which do not need mipmap auto-generating and
        // which aren't using compressed images.

        switch (texture.getType()) {
          case TwoDimensional:
            // ensure the buffer is ready for reading
            image.getData(0).rewind();
            // send top level to card
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0,
                Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), image.getWidth(),
                image.getHeight(), hasBorder ? 1 : 0, Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), image.getData(0));
            break;
          case OneDimensional:
            // ensure the buffer is ready for reading
            image.getData(0).rewind();
            // send top level to card
            GL11C.glTexImage1D(GL11C.GL_TEXTURE_1D, 0,
                Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), image.getWidth(),
                hasBorder ? 1 : 0, Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), image.getData(0));
            break;
          case ThreeDimensional:
            // concat data into single buffer:
            int dSize = 0;
            int count = 0;
            ByteBuffer data = null;
            for (int x = 0; x < image.getData().size(); x++) {
              if (image.getData(x) != null) {
                data = image.getData(x);
                dSize += data.limit();
                count++;
              }
            }
            // reuse buffer if we can.
            if (count != 1) {
              data = BufferUtils.createByteBuffer(dSize);
              for (int x = 0; x < image.getData().size(); x++) {
                if (image.getData(x) != null) {
                  data.put(image.getData(x));
                }
              }
              // ensure the buffer is ready for reading
              data.flip();
            }
            // send top level to card
            GL12C.glTexImage3D(GL12C.GL_TEXTURE_3D, 0,
                Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), image.getWidth(),
                image.getHeight(), image.getDepth(), hasBorder ? 1 : 0,
                Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), data);
            break;
          case CubeMap:
            // NOTE: Cubemaps MUST be square, so height is ignored on purpose.
            for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
              // ensure the buffer is ready for reading
              image.getData(face.ordinal()).rewind();
              // send top level to card
              GL11C.glTexImage2D(getGLCubeMapFace(face), 0,
                  Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), image.getWidth(),
                  image.getWidth(), hasBorder ? 1 : 0, Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                  Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), image.getData(face.ordinal()));
            }
            break;
        }
      } else if (texture.getMinificationFilter().usesMipMapLevels() && !image.hasMipmaps()
          && !texture.getTextureStoreFormat().isCompressed()) {

        // For textures which need mipmaps auto-generating and which
        // aren't using compressed images, generate the mipmaps.
        // A new mipmap builder may be needed to build mipmaps for
        // compressed textures.

        switch (type) {
          case TwoDimensional:
            // ensure the buffer is ready for reading
            image.getData(0).rewind();
            // send top level to card
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0,
                Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), image.getWidth(),
                image.getHeight(), hasBorder ? 1 : 0, Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), image.getData(0));
            break;
          case OneDimensional:
            // ensure the buffer is ready for reading
            image.getData(0).rewind();
            // send top level to card
            GL11C.glTexImage1D(GL11C.GL_TEXTURE_1D, 0,
                Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), image.getWidth(),
                hasBorder ? 1 : 0, Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), image.getData(0));
            break;
          case ThreeDimensional:
            // concat data into single buffer:
            int dSize = 0;
            int count = 0;
            ByteBuffer data = null;
            for (int x = 0; x < image.getData().size(); x++) {
              if (image.getData(x) != null) {
                data = image.getData(x);
                dSize += data.limit();
                count++;
              }
            }
            // reuse buffer if we can.
            if (count != 1) {
              data = BufferUtils.createByteBuffer(dSize);
              for (int x = 0; x < image.getData().size(); x++) {
                if (image.getData(x) != null) {
                  data.put(image.getData(x));
                }
              }
              // ensure the buffer is ready for reading
              data.flip();
            }
            // send top level to card
            GL12C.glTexImage3D(GL12C.GL_TEXTURE_3D, 0,
                Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), image.getWidth(),
                image.getHeight(), image.getDepth(), hasBorder ? 1 : 0,
                Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), data);
            break;
          case CubeMap:
            // NOTE: Cubemaps MUST be square, so height is ignored on purpose.
            for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
              // ensure the buffer is ready for reading
              image.getData(face.ordinal()).rewind();
              // send top level to card
              GL11C.glTexImage2D(getGLCubeMapFace(face), 0,
                  Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), image.getWidth(),
                  image.getWidth(), hasBorder ? 1 : 0, Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                  Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), image.getData(face.ordinal()));
            }
            break;
        }

        // Ask the card to generate mipmaps
        GL30C.glGenerateMipmap(getGLType(type));

        if (texture.getTextureMaxLevel() >= 0) {
          GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_MAX_LEVEL, texture.getTextureMaxLevel());
        }
      } else {
        // Here we handle textures that are either compressed or have predefined mipmaps.
        // Get mipmap data sizes and amount of mipmaps to send to opengl. Then loop through all mipmaps and
        // send
        // them.
        int[] mipSizes = image.getMipMapByteSizes();
        ByteBuffer data = null;

        if (type == Type.CubeMap) {
          for (final TextureCubeMap.Face face : TextureCubeMap.Face.values()) {
            data = image.getData(face.ordinal());
            int pos = 0;
            int max = 1;

            if (mipSizes == null) {
              mipSizes = new int[] {data.capacity()};
            } else if (texture.getMinificationFilter().usesMipMapLevels()) {
              max = mipSizes.length;
            }

            // set max mip level
            GL11C.glTexParameteri(getGLCubeMapFace(face), GL12C.GL_TEXTURE_MAX_LEVEL, max - 1);

            for (int m = 0; m < max; m++) {
              final int width = Math.max(1, image.getWidth() >> m);
              final int height = Math.max(1, image.getHeight() >> m);

              data.position(pos);
              data.limit(pos + mipSizes[m]);

              if (texture.getTextureStoreFormat().isCompressed()) {
                ARBTextureCompression.glCompressedTexImage2DARB(getGLCubeMapFace(face), m,
                    Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), width, height, data);
              } else {
                GL11C.glTexImage2D(getGLCubeMapFace(face), m,
                    Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), width, height,
                    hasBorder ? 1 : 0, Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                    Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), data);
              }
              pos += mipSizes[m];
            }
          }
        } else {
          data = image.getData(0);
          int pos = 0;
          int max = 1;

          if (mipSizes == null) {
            mipSizes = new int[] {data.capacity()};
          } else if (texture.getMinificationFilter().usesMipMapLevels()) {
            max = mipSizes.length;
          }

          // Set max mip level
          switch (type) {
            case TwoDimensional:
              GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL12C.GL_TEXTURE_MAX_LEVEL, max - 1);
              break;
            case ThreeDimensional:
              GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL12C.GL_TEXTURE_MAX_LEVEL, max - 1);
              break;
            case OneDimensional:
              GL11C.glTexParameteri(GL11C.GL_TEXTURE_1D, GL12C.GL_TEXTURE_MAX_LEVEL, max - 1);
              break;
            case CubeMap:
              // handled above
              break;
          }

          if (type == Type.ThreeDimensional) {
            // concat data into single buffer:
            int dSize = 0;
            int count = 0;
            for (int x = 0; x < image.getData().size(); x++) {
              if (image.getData(x) != null) {
                data = image.getData(x);
                dSize += data.limit();
                count++;
              }
            }
            // reuse buffer if we can.
            if (count != 1) {
              data = BufferUtils.createByteBuffer(dSize);
              for (int x = 0; x < image.getData().size(); x++) {
                if (image.getData(x) != null) {
                  data.put(image.getData(x));
                }
              }
              // ensure the buffer is ready for reading
              data.flip();
            }
          }

          for (int m = 0; m < max; m++) {
            final int width = Math.max(1, image.getWidth() >> m);
            final int height = Math.max(1, image.getHeight() >> m);

            data.position(pos);
            data.limit(pos + mipSizes[m]);

            switch (type) {
              case TwoDimensional:
                if (texture.getTextureStoreFormat().isCompressed()) {
                  ARBTextureCompression.glCompressedTexImage2DARB(GL11C.GL_TEXTURE_2D, m,
                      Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), width, height, data);
                } else {
                  GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, m,
                      Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), width, height,
                      hasBorder ? 1 : 0, Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                      Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), data);
                }
                break;
              case OneDimensional:
                if (texture.getTextureStoreFormat().isCompressed()) {
                  ARBTextureCompression.glCompressedTexImage1DARB(GL11C.GL_TEXTURE_1D, m,
                      Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), width, data);
                } else {
                  GL11C.glTexImage1D(GL11C.GL_TEXTURE_1D, m,
                      Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), width, hasBorder ? 1 : 0,
                      Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                      Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), data);
                }
                break;
              case ThreeDimensional:
                final int depth = Math.max(1, image.getDepth() >> m);
                // already checked for support above...
                if (texture.getTextureStoreFormat().isCompressed()) {
                  ARBTextureCompression.glCompressedTexImage3DARB(GL12C.GL_TEXTURE_3D, m,
                      Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), width, height, depth,
                      data);
                } else {
                  GL12C.glTexImage3D(GL12C.GL_TEXTURE_3D, m,
                      Lwjgl3TextureUtils.getGLInternalFormat(texture.getTextureStoreFormat()), width, height, depth,
                      hasBorder ? 1 : 0, Lwjgl3TextureUtils.getGLPixelFormat(image.getDataFormat()),
                      Lwjgl3TextureUtils.getGLPixelDataType(image.getDataType()), data);
                }
                break;
              case CubeMap:
                // handled above
                break;
            }
            pos += mipSizes[m];
          }
        }
        if (data != null) {
          data.clear();
        }
      }
    }
  }

  public static void apply(final TextureState state) {
    // ask for the current state record
    final RenderContext context = ContextManager.getCurrentContext();
    final ContextCapabilities caps = context.getCapabilities();
    final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);
    context.setCurrentState(StateType.Texture, state);

    if (state.isEnabled()) {
      Texture texture;
      Texture.Type type;
      TextureUnitRecord unitRecord;
      TextureRecord texRecord;

      // loop through all available texture units...
      for (int i = 0; i < caps.getNumberOfTotalTextureUnits(); i++) {
        unitRecord = record.units[i];

        // grab a texture for this unit, if available
        texture = state.getTexture(i);

        // pull our texture id for this texture, for this context.
        int textureId = texture != null ? texture.getTextureIdForContext(context) : 0;

        // check for invalid textures - ones that have no opengl id and
        // no image data
        if (texture == null || (texture != null && textureId == 0 && texture.getImage() == null)) {
          continue;
        }

        type = texture.getType();

        // Time to bind the texture, so see if we need to load in image
        // data for this texture.
        if (textureId == 0) {
          // texture not yet loaded.
          // this will load and bind and set the records...
          load(texture, i);
          textureId = texture.getTextureIdForContext(context);
          if (textureId == 0) {
            continue;
          }
        } else if (texture.isDirty(context)) {
          update(texture, i);
          textureId = texture.getTextureIdForContext(context);
          if (textureId == 0) {
            continue;
          }
        } else {
          // texture already exists in OpenGL, just bind it if needed
          if (!unitRecord.isValid() || unitRecord.boundTexture != textureId) {
            checkAndSetUnit(i, record, caps);
            GL11C.glBindTexture(getGLType(type), textureId);
            if (Constants.stats) {
              StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
            }
            unitRecord.boundTexture = textureId;
          }
        }

        // Use the Java Integer object for the getTextureRecord call to avoid
        // boxing/unboxing ints for map lookups.
        final Integer textureIdInteger = texture.getTextureIdForContextAsInteger(context);

        // Grab our record for this texture
        texRecord = record.getTextureRecord(textureIdInteger, texture.getType());

        // Set the keyCache value for this unit of this texture state
        // This is done so during state comparison we don't have to
        // spend a lot of time pulling out classes and finding field
        // data.
        state._keyCache[i] = texture.getTextureKey();

        // Other items only apply to textures below the frag unit limit
        if (i < caps.getNumberOfFragmentTextureUnits()) {

          // texture specific params
          applyFilter(texture, texRecord, i, record, caps);
          applyWrap(texture, texRecord, i, record, caps);
          applyShadow(texture, texRecord, i, record, caps);

          // Set our border color, if needed.
          applyBorderColor(texture, texRecord, i, record);

          // Set our texture lod bias, if needed.
          applyLodBias(texture, unitRecord, i, record, caps);

          // all states have now been applied for a tex record, so we
          // can safely make it valid
          if (!texRecord.isValid()) {
            texRecord.validate();
          }

        }
      }
    }

    if (!record.isValid()) {
      record.validate();
    }
  }

  public static void applyLodBias(final Texture texture, final TextureUnitRecord unitRecord, final int unit,
      final TextureStateRecord record, final ContextCapabilities caps) {
    final float bias = texture.getLodBias() < caps.getMaxLodBias() ? texture.getLodBias() : caps.getMaxLodBias();
    if (!unitRecord.isValid() || unitRecord.lodBias != bias) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameterf(getGLType(texture.getType()), GL14C.GL_TEXTURE_LOD_BIAS, bias);
      unitRecord.lodBias = bias;
    }
  }

  public static void applyBorderColor(final Texture texture, final TextureRecord texRecord, final int unit,
      final TextureStateRecord record) {
    final ReadOnlyColorRGBA texBorder = texture.getBorderColor();
    if (!texRecord.isValid() || !texRecord.borderColor.equals(texBorder)) {
      TextureRecord.colorBuffer.clear();
      TextureRecord.colorBuffer.put(texBorder.getRed()).put(texBorder.getGreen()).put(texBorder.getBlue())
          .put(texBorder.getAlpha());
      TextureRecord.colorBuffer.rewind();
      GL11C.glTexParameterfv(getGLType(texture.getType()), GL11C.GL_TEXTURE_BORDER_COLOR, TextureRecord.colorBuffer);
      texRecord.borderColor.set(texBorder);
    }
  }

  // If we support multi-texturing, specify the unit we are affecting.
  public static void checkAndSetUnit(final int unit, final TextureStateRecord record, final ContextCapabilities caps) {
    // No need to worry about valid record, since invalidate sets record's
    // currentUnit to -1.
    if (record.currentUnit != unit) {
      if (unit >= caps.getNumberOfTotalTextureUnits() || unit < 0) {
        // ignore this request as it is not valid for the user's hardware.
        return;
      }
      GL13C.glActiveTexture(GL13C.GL_TEXTURE0 + unit);
      record.currentUnit = unit;
    }
  }

  /**
   * Check if the filter settings of this particular texture have been changed and apply as needed.
   *
   * @param texture
   *          our texture object
   * @param texRecord
   *          our record of the last state of the texture in gl
   * @param record
   */
  public static void applyShadow(final Texture texture, final TextureRecord texRecord, final int unit,
      final TextureStateRecord record, final ContextCapabilities caps) {
    final Type type = texture.getType();

    final int depthCompareMode = Lwjgl3TextureUtils.getGLDepthTextureCompareMode(texture.getDepthCompareMode());
    // set up magnification filter
    if (!texRecord.isValid() || texRecord.depthTextureCompareMode != depthCompareMode) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(getGLType(type), ARBShadow.GL_TEXTURE_COMPARE_MODE_ARB, depthCompareMode);
      texRecord.depthTextureCompareMode = depthCompareMode;
    }

    final int depthCompareFunc = Lwjgl3TextureUtils.getGLDepthTextureCompareFunc(texture.getDepthCompareFunc());
    // set up magnification filter
    if (!texRecord.isValid() || texRecord.depthTextureCompareFunc != depthCompareFunc) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(getGLType(type), ARBShadow.GL_TEXTURE_COMPARE_FUNC_ARB, depthCompareFunc);
      texRecord.depthTextureCompareFunc = depthCompareFunc;
    }
  }

  /**
   * Check if the filter settings of this particular texture have been changed and apply as needed.
   *
   * @param texture
   *          our texture object
   * @param texRecord
   *          our record of the last state of the texture in gl
   * @param record
   */
  public static void applyFilter(final Texture texture, final TextureRecord texRecord, final int unit,
      final TextureStateRecord record, final ContextCapabilities caps) {
    final Type type = texture.getType();

    final int magFilter = Lwjgl3TextureUtils.getGLMagFilter(texture.getMagnificationFilter());
    // set up magnification filter
    if (!texRecord.isValid() || texRecord.magFilter != magFilter) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(getGLType(type), GL11C.GL_TEXTURE_MAG_FILTER, magFilter);
      texRecord.magFilter = magFilter;
    }

    final int minFilter = Lwjgl3TextureUtils.getGLMinFilter(texture.getMinificationFilter());
    // set up mipmap filter
    if (!texRecord.isValid() || texRecord.minFilter != minFilter) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(getGLType(type), GL11C.GL_TEXTURE_MIN_FILTER, minFilter);
      texRecord.minFilter = minFilter;
    }

    // set up aniso filter
    if (caps.isAnisoSupported()) {
      float aniso = texture.getAnisotropicFilterPercent() * (caps.getMaxAnisotropic() - 1.0f);
      aniso += 1.0f;
      if (!texRecord.isValid() || (texRecord.anisoLevel - aniso > MathUtils.ZERO_TOLERANCE)) {
        checkAndSetUnit(unit, record, caps);
        GL11C.glTexParameterf(getGLType(type), GL46C.GL_TEXTURE_MAX_ANISOTROPY, aniso);
        texRecord.anisoLevel = aniso;
      }
    }
  }

  /**
   * Check if the wrap mode of this particular texture has been changed and apply as needed.
   *
   * @param texture
   *          our texture object
   * @param texRecord
   *          our record of the last state of the unit in gl
   * @param record
   */
  public static void applyWrap(final Texture3D texture, final TextureRecord texRecord, final int unit,
      final TextureStateRecord record, final ContextCapabilities caps) {
    final int wrapS = getGLWrap(texture.getWrap(WrapAxis.S), caps);
    final int wrapT = getGLWrap(texture.getWrap(WrapAxis.T), caps);
    final int wrapR = getGLWrap(texture.getWrap(WrapAxis.R), caps);

    if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_S, wrapS);
      texRecord.wrapS = wrapS;
    }
    if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL11C.GL_TEXTURE_WRAP_T, wrapT);
      texRecord.wrapT = wrapT;
    }
    if (!texRecord.isValid() || texRecord.wrapR != wrapR) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(GL12C.GL_TEXTURE_3D, GL12C.GL_TEXTURE_WRAP_R, wrapR);
      texRecord.wrapR = wrapR;
    }

  }

  /**
   * Check if the wrap mode of this particular texture has been changed and apply as needed.
   *
   * @param texture
   *          our texture object
   * @param texRecord
   *          our record of the last state of the unit in gl
   * @param record
   */
  public static void applyWrap(final Texture1D texture, final TextureRecord texRecord, final int unit,
      final TextureStateRecord record, final ContextCapabilities caps) {
    final int wrapS = getGLWrap(texture.getWrap(WrapAxis.S), caps);

    if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(GL11C.GL_TEXTURE_1D, GL11C.GL_TEXTURE_WRAP_S, wrapS);
      texRecord.wrapS = wrapS;
    }
  }

  /**
   * Check if the wrap mode of this particular texture has been changed and apply as needed.
   *
   * @param texture
   *          our texture object
   * @param texRecord
   *          our record of the last state of the unit in gl
   * @param record
   */
  public static void applyWrap(final Texture texture, final TextureRecord texRecord, final int unit,
      final TextureStateRecord record, final ContextCapabilities caps) {
    if (texture instanceof Texture2D) {
      applyWrap((Texture2D) texture, texRecord, unit, record, caps);
    } else if (texture instanceof Texture1D) {
      applyWrap((Texture1D) texture, texRecord, unit, record, caps);
    } else if (texture instanceof Texture3D) {
      applyWrap((Texture3D) texture, texRecord, unit, record, caps);
    } else if (texture instanceof TextureCubeMap) {
      applyWrap((TextureCubeMap) texture, texRecord, unit, record, caps);
    }
  }

  /**
   * Check if the wrap mode of this particular texture has been changed and apply as needed.
   *
   * @param texture
   *          our texture object
   * @param texRecord
   *          our record of the last state of the unit in gl
   * @param record
   */
  public static void applyWrap(final Texture2D texture, final TextureRecord texRecord, final int unit,
      final TextureStateRecord record, final ContextCapabilities caps) {
    final int wrapS = getGLWrap(texture.getWrap(WrapAxis.S), caps);
    final int wrapT = getGLWrap(texture.getWrap(WrapAxis.T), caps);

    if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, wrapS);
      texRecord.wrapS = wrapS;
    }
    if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, wrapT);
      texRecord.wrapT = wrapT;
    }

  }

  /**
   * Check if the wrap mode of this particular texture has been changed and apply as needed.
   *
   * @param cubeMap
   *          our texture object
   * @param texRecord
   *          our record of the last state of the unit in gl
   * @param record
   */
  public static void applyWrap(final TextureCubeMap cubeMap, final TextureRecord texRecord, final int unit,
      final TextureStateRecord record, final ContextCapabilities caps) {
    final int wrapS = getGLWrap(cubeMap.getWrap(WrapAxis.S), caps);
    final int wrapT = getGLWrap(cubeMap.getWrap(WrapAxis.T), caps);
    final int wrapR = getGLWrap(cubeMap.getWrap(WrapAxis.R), caps);

    if (!texRecord.isValid() || texRecord.wrapS != wrapS) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(GL13C.GL_TEXTURE_CUBE_MAP, GL11C.GL_TEXTURE_WRAP_S, wrapS);
      texRecord.wrapS = wrapS;
    }
    if (!texRecord.isValid() || texRecord.wrapT != wrapT) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(GL13C.GL_TEXTURE_CUBE_MAP, GL11C.GL_TEXTURE_WRAP_T, wrapT);
      texRecord.wrapT = wrapT;
    }
    if (!texRecord.isValid() || texRecord.wrapR != wrapR) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(GL13C.GL_TEXTURE_CUBE_MAP, GL12C.GL_TEXTURE_WRAP_R, wrapR);
      texRecord.wrapR = wrapR;
    }
  }

  public static void deleteTexture(final Texture texture) {
    // ask for the current state record
    final RenderContext context = ContextManager.getCurrentContext();
    final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);

    final Integer id = texture.getTextureIdForContextAsInteger(context);
    if (id.intValue() == 0) {
      // Not on card... return.
      return;
    }

    GL11C.glDeleteTextures(id.intValue());
    record.removeTextureRecord(id);
    texture.removeFromIdCache(context);
  }

  public static void deleteTextureIds(final Collection<Integer> ids) {
    // ask for the current state record
    final RenderContext context = ContextManager.getCurrentContext();
    final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);

    try (MemoryStack stack = MemoryStack.stackPush()) {
      final IntBuffer idBuffer = stack.mallocInt(ids.size());
      for (final Integer i : ids) {
        if (i != null) {
          idBuffer.put(i);
          record.removeTextureRecord(i);
        }
      }
      idBuffer.flip();
      if (idBuffer.remaining() > 0) {
        GL11C.glDeleteTextures(idBuffer);
      }
    }
  }

  /**
   * Useful for external lwjgl based classes that need to safely set the current texture.
   */
  public static void doTextureBind(final Texture texture, final int unit, final boolean invalidateState) {
    // ask for the current state record
    final RenderContext context = ContextManager.getCurrentContext();
    final ContextCapabilities caps = context.getCapabilities();
    final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(StateType.Texture);
    if (invalidateState) {
      // Set this to null because no current state really matches anymore
      context.setCurrentState(StateType.Texture, null);
    }
    checkAndSetUnit(unit, record, caps);

    final int id = texture.getTextureIdForContext(context);
    GL11C.glBindTexture(getGLType(texture.getType()), id);
    if (Constants.stats) {
      StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
    }
    if (record != null) {
      record.units[unit].boundTexture = id;
    }
  }

  public static int getGLType(final Type type) {
    switch (type) {
      case TwoDimensional:
        return GL11C.GL_TEXTURE_2D;
      case OneDimensional:
        return GL11C.GL_TEXTURE_1D;
      case ThreeDimensional:
        return GL12C.GL_TEXTURE_3D;
      case CubeMap:
        return GL13C.GL_TEXTURE_CUBE_MAP;
    }
    throw new IllegalArgumentException("invalid texture type: " + type);
  }

  public static int getGLCubeMapFace(final TextureCubeMap.Face face) {
    switch (face) {
      case PositiveX:
        return GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
      case NegativeX:
        return GL13C.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
      case PositiveY:
        return GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
      case NegativeY:
        return GL13C.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
      case PositiveZ:
        return GL13C.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
      case NegativeZ:
        return GL13C.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
    }
    throw new IllegalArgumentException("invalid cubemap face: " + face);
  }

  public static int getGLWrap(final WrapMode wrap, final ContextCapabilities caps) {
    switch (wrap) {
      case Repeat:
        return GL11C.GL_REPEAT;
      case MirroredRepeat:
        return GL14C.GL_MIRRORED_REPEAT;
      case BorderClamp:
        return GL13C.GL_CLAMP_TO_BORDER;
      case MirrorEdgeClamp:
        return GL44C.GL_MIRROR_CLAMP_TO_EDGE;
      case EdgeClamp:
        return GL12C.GL_CLAMP_TO_EDGE;
    }
    throw new IllegalArgumentException("invalid WrapMode type: " + wrap);
  }
}
