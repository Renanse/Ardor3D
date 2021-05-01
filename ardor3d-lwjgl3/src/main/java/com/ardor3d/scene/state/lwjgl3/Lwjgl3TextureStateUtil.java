/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scene.state.lwjgl3;

import java.nio.IntBuffer;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryStack;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.Texture.WrapAxis;
import com.ardor3d.image.Texture1D;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.Texture3D;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.renderer.state.record.TextureUnitRecord;
import com.ardor3d.scene.state.lwjgl3.util.TextureConstants;
import com.ardor3d.scene.state.lwjgl3.util.TextureToCard;
import com.ardor3d.util.Constants;
import com.ardor3d.util.TextureManager;
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

    // optimistically mark us clean for the current context
    texture.getTextureKey().markClean(context);

    // our texture type
    final Texture.Type type = texture.getType();

    // bind our texture id to this unit.
    doTextureBind(texture, unit, false);

    // grab our texture image data for sending to the card
    final Image image = texture.getImage();
    if (image == null) {
      if (logger.isLoggable(Level.FINE)) {
        logger.fine("Image data for texture is null.");
      }
      return;
    }

    // ensure our image data is not bigger than our max supported texture size.
    final int maxSize = caps.getMaxTextureSize();
    final int imageWidth = image.getWidth();
    final int imageHeight = image.getHeight();
    final int imageDepth = image.getDepth();
    if (imageWidth > maxSize || imageHeight > maxSize || imageDepth > maxSize) {
      logger.warning("(card unsupported) Attempted to apply texture with size bigger than max texture size [" + maxSize
          + "]: " + imageWidth + " x " + imageHeight + " x " + imageDepth);
      return;
    }

    // We expect image data to be tightly packed.
    GL11C.glPixelStorei(GL11C.GL_UNPACK_ALIGNMENT, 1);

    // Time to push our image data to the card...
    // ...handle textures that don't contain mipmaps:
    if (!image.hasMipmaps()) {
      TextureToCard.sendNonMipMappedTexture(texture);

      // generate mipmaps, if we are uncompressed and our filter makes use of them.
      if (!texture.getTextureStoreFormat().isCompressed() && texture.getMinificationFilter().usesMipMapLevels()) {
        // Ask the card to generate mipmaps
        GL30C.glGenerateMipmap(TextureConstants.getGLType(type));

        // Override the max mipmap level, if we have that set.
        if (texture.getTextureMaxLevel() >= 0) {
          GL11C.glTexParameteri(TextureConstants.getGLType(type), GL12C.GL_TEXTURE_MAX_LEVEL,
              texture.getTextureMaxLevel());
        }
      }
      return;
    }

    // ...handle textures that have embedded mipmaps:
    TextureToCard.sendMipMappedTexture(texture);
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
        if (texture == null || texture != null && textureId == 0 && texture.getImage() == null) {
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
            GL11C.glBindTexture(TextureConstants.getGLType(type), textureId);
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
      GL11C.glTexParameterf(TextureConstants.getGLType(texture.getType()), GL14C.GL_TEXTURE_LOD_BIAS, bias);
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
      GL11C.glTexParameterfv(TextureConstants.getGLType(texture.getType()), GL11C.GL_TEXTURE_BORDER_COLOR,
          TextureRecord.colorBuffer);
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

    final int depthCompareMode = TextureConstants.getGLDepthTextureCompareMode(texture.getDepthCompareMode());
    // set up magnification filter
    if (!texRecord.isValid() || texRecord.depthTextureCompareMode != depthCompareMode) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(TextureConstants.getGLType(type), GL14C.GL_TEXTURE_COMPARE_MODE, depthCompareMode);
      texRecord.depthTextureCompareMode = depthCompareMode;
    }

    final int depthCompareFunc = TextureConstants.getGLDepthTextureCompareFunc(texture.getDepthCompareFunc());
    // set up magnification filter
    if (!texRecord.isValid() || texRecord.depthTextureCompareFunc != depthCompareFunc) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(TextureConstants.getGLType(type), GL14C.GL_TEXTURE_COMPARE_FUNC, depthCompareFunc);
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

    final int magFilter = TextureConstants.getGLMagFilter(texture.getMagnificationFilter());
    // set up magnification filter
    if (!texRecord.isValid() || texRecord.magFilter != magFilter) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(TextureConstants.getGLType(type), GL11C.GL_TEXTURE_MAG_FILTER, magFilter);
      texRecord.magFilter = magFilter;
    }

    final int minFilter = TextureConstants.getGLMinFilter(texture.getMinificationFilter());
    // set up mipmap filter
    if (!texRecord.isValid() || texRecord.minFilter != minFilter) {
      checkAndSetUnit(unit, record, caps);
      GL11C.glTexParameteri(TextureConstants.getGLType(type), GL11C.GL_TEXTURE_MIN_FILTER, minFilter);
      texRecord.minFilter = minFilter;
    }

    // set up aniso filter
    if (caps.isAnisoSupported()) {
      float aniso = texture.getAnisotropicFilterPercent() * (caps.getMaxAnisotropic() - 1.0f);
      aniso += 1.0f;
      if (!texRecord.isValid() || texRecord.anisoLevel - aniso > MathUtils.ZERO_TOLERANCE) {
        checkAndSetUnit(unit, record, caps);
        GL11C.glTexParameterf(TextureConstants.getGLType(type), GL46C.GL_TEXTURE_MAX_ANISOTROPY, aniso);
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
    final int wrapS = TextureConstants.getGLWrap(texture.getWrap(WrapAxis.S), caps);
    final int wrapT = TextureConstants.getGLWrap(texture.getWrap(WrapAxis.T), caps);
    final int wrapR = TextureConstants.getGLWrap(texture.getWrap(WrapAxis.R), caps);

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
    final int wrapS = TextureConstants.getGLWrap(texture.getWrap(WrapAxis.S), caps);

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
    final int wrapS = TextureConstants.getGLWrap(texture.getWrap(WrapAxis.S), caps);
    final int wrapT = TextureConstants.getGLWrap(texture.getWrap(WrapAxis.T), caps);

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
    final int wrapS = TextureConstants.getGLWrap(cubeMap.getWrap(WrapAxis.S), caps);
    final int wrapT = TextureConstants.getGLWrap(cubeMap.getWrap(WrapAxis.T), caps);
    final int wrapR = TextureConstants.getGLWrap(cubeMap.getWrap(WrapAxis.R), caps);

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
    GL11C.glBindTexture(TextureConstants.getGLType(texture.getType()), id);
    if (Constants.stats) {
      StatCollector.addStat(StatType.STAT_TEXTURE_BINDS, 1);
    }
    record.units[unit].boundTexture = id;
  }
}
