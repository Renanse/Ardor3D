/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.lwjgl3;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderable;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.renderer.texture.AbstractFBOTextureRenderer;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3TextureStateUtil;
import com.ardor3d.scene.state.lwjgl3.util.TextureConstants;
import com.ardor3d.scene.state.lwjgl3.util.TextureToCard;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.TextureKey;

/**
 * <p>
 * This class is used by Ardor3D's LWJGL implementation to render textures. Users should <b>not</b>
 * create this class directly.
 * </p>
 *
 * @see TextureRendererFactory
 */
public class Lwjgl3TextureRenderer extends AbstractFBOTextureRenderer {
  private static final Logger logger = Logger.getLogger(Lwjgl3TextureRenderer.class.getName());

  private final Consumer<Renderable> drawRendConsumer = (final Renderable r) -> this.doDraw(r);
  private final Consumer<List<? extends Renderable>> drawRendListConsumer =
      (final List<? extends Renderable> list) -> this.doDraw(list);
  private final Consumer<Spatial> drawSpatConsumer = (final Spatial s) -> doDrawSpatial(s);
  private final Consumer<List<? extends Spatial>> drawSpatListConsumer =
      (final List<? extends Spatial> list) -> doDrawSpatials(list);

  public Lwjgl3TextureRenderer(final int width, final int height, final int layers, final int depthBits,
    final int samples, final Renderer parentRenderer, final ContextCapabilities caps) {
    super(width, height, layers, depthBits, samples, parentRenderer, caps);

    if (caps.getMaxFBOColorAttachments() > 1) {
      _attachBuffer = BufferUtils.createIntBuffer(caps.getMaxFBOColorAttachments());
      for (int i = 0; i < caps.getMaxFBOColorAttachments(); i++) {
        _attachBuffer.put(GL30C.GL_COLOR_ATTACHMENT0 + i);
      }
    }
  }

  /**
   * <code>setupTexture</code> initializes a new Texture object for use with TextureRenderer.
   * Generates a valid OpenGL texture id for this texture and initializes the data type for the
   * texture.
   */
  @Override
  public void setupTexture(final Texture tex) {
    final RenderContext context = ContextManager.getCurrentContext();
    final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(RenderState.StateType.Texture);

    // check if we are already setup... if so, throw error.
    if (tex.getTextureKey() == null) {
      tex.setTextureKey(TextureKey.getRTTKey(tex.getMinificationFilter()));
    } else if (tex.getTextureIdForContext(context) != 0) {
      throw new Ardor3dException("Texture is already setup and has id.");
    }

    // Create the texture
    final int textureId = GL11C.glGenTextures();
    tex.setTextureIdForContext(context, textureId);

    Lwjgl3TextureStateUtil.doTextureBind(tex, 0, true);

    // Initialize our texture with some default data.
    final var storeFormat = tex.getTextureStoreFormat();
    final var dataFormat = TextureConstants.getImageDataFormatFromStoreFormat(storeFormat);
    final var dataType = tex.getRenderedTexturePixelDataType();
    final var type = tex.getType();

    if (type == Type.CubeMap) {
      for (final Face face : Face.values()) {
        TextureToCard.sendTexture(type, face, 0, storeFormat, _width, _height, _layers, tex.hasBorder(), dataFormat,
            dataType, null);
      }
    } else {
      TextureToCard.sendTexture(type, null, 0, storeFormat, _width, _height, _layers, tex.hasBorder(), dataFormat,
          dataType, null);
    }

    // Initialize mipmapping for this texture, if requested
    // We do this regardless of the EnableMipGeneration flag, to init storage.
    if (tex.getMinificationFilter().usesMipMapLevels()) {
      GL30C.glGenerateMipmap(TextureConstants.getGLType(type));
    }

    // Setup filtering and wrap
    final TextureRecord texRecord = record.getTextureRecord(textureId, type);
    Lwjgl3TextureStateUtil.applyFilter(tex, texRecord, 0, record, context.getCapabilities());
    Lwjgl3TextureStateUtil.applyWrap(tex, texRecord, 0, record, context.getCapabilities());
    Lwjgl3TextureStateUtil.applyShadow(tex, texRecord, 0, record, context.getCapabilities());
    Lwjgl3TextureStateUtil.applyBorderColor(tex, texRecord, 0, record);

    if (logger.isLoggable(Level.FINE)) {
      logger.fine("setup fbo tex with id " + textureId + ": " + _width + "," + _height);
    }
  }

  @Override
  public void render(final Renderable renderable, final List<Texture> texs, final int clear) {
    render(renderable, drawRendConsumer, texs, clear);
  }

  @Override
  public void render(final List<? extends Renderable> renderables, final List<Texture> texs, final int clear) {
    render(renderables, drawRendListConsumer, texs, clear);
  }

  @Override
  public void renderSpatial(final Spatial spat, final List<Texture> texs, final int clear) {
    render(spat, drawSpatConsumer, texs, clear);
  }

  @Override
  public void renderSpatials(final List<? extends Spatial> spats, final List<Texture> texs, final int clear) {
    render(spats, drawSpatListConsumer, texs, clear);
  }

  protected <T> void render(final T toRender, final Consumer<T> consumer, final List<Texture> texs, final int clear) {

    final RenderContext context = ContextManager.getCurrentContext();
    final int maxDrawBuffers = context.getCapabilities().getMaxFBOColorAttachments();

    // if we only support 1 draw buffer at a time anyway, we'll have to render to each texture
    // individually...
    if (maxDrawBuffers == 1 || texs.size() == 1) {
      context.pushFBOTextureRenderer(this);
      try {
        for (int i = 0; i < texs.size(); i++) {
          final Texture tex = texs.get(i);

          setupForSingleTexDraw(tex);

          if (_samples > 0 && _supportsMultisample) {
            setMSFBO();
          }

          switchCameraIn(clear);
          if (toRender != null) {
            consumer.accept(toRender);
          }
          switchCameraOut();

          if (_samples > 0 && _supportsMultisample) {
            blitMSFBO();
          }

          takedownForSingleTexDraw(tex);
        }
      } finally {
        context.popFBOTextureRenderer();
      }
      return;
    }
    try {
      context.pushFBOTextureRenderer(this);

      // Otherwise, we can streamline this by rendering to multiple textures at once.
      // first determine how many groups we need
      final LinkedList<Texture> depths = new LinkedList<>();
      final LinkedList<Texture> colors = new LinkedList<>();
      for (int i = 0; i < texs.size(); i++) {
        final Texture tex = texs.get(i);
        if (tex.getTextureStoreFormat().isDepthFormat()) {
          depths.add(tex);
        } else {
          colors.add(tex);
        }
      }
      // we can only render to 1 depth texture at a time, so # groups is at minimum == numDepth
      final int groups = Math.max(depths.size(), (int) Math.ceil(colors.size() / (float) maxDrawBuffers));

      for (int i = 0; i < groups; i++) {
        // First handle colors
        int colorsAdded = 0;
        Texture tex;
        while (colorsAdded < maxDrawBuffers && !colors.isEmpty()) {
          tex = colors.removeFirst();
          attachTextureToFramebuffer(tex, GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0 + colorsAdded);
          colorsAdded++;
        }

        // Now take care of depth.
        if (!depths.isEmpty()) {
          tex = depths.removeFirst();
          attachTextureToFramebuffer(tex, GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT);
          _usingDepthRB = false;
        } else if (!_usingDepthRB && _depthRBID != 0) {
          // setup our default depth render buffer if not already set
          GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL30C.GL_RENDERBUFFER,
              _depthRBID);
          _usingDepthRB = true;
        }

        setDrawBuffers(colorsAdded);
        setReadBuffer(colorsAdded != 0 ? GL30C.GL_COLOR_ATTACHMENT0 : GL11C.GL_NONE);

        // Check FBO complete
        checkFBOComplete(_fboID);

        switchCameraIn(clear);
        if (toRender != null) {
          consumer.accept(toRender);
        }
        switchCameraOut();
      }

      if (isEnableMipGeneration()) {
        // automatically generate mipmaps for our textures that support it.
        for (int x = 0, max = texs.size(); x < max; x++) {
          if (texs.get(x).getMinificationFilter().usesMipMapLevels()) {
            final Texture tex = texs.get(x);
            if (tex.getMinificationFilter().usesMipMapLevels()) {
              Lwjgl3TextureStateUtil.doTextureBind(texs.get(x), 0, true);
              GL30C.glGenerateMipmap(TextureConstants.getGLType(tex.getType()));
            }
          }
        }
      }

      for (int x = 0, max = texs.size(); x < max; x++) {
        texs.get(x).getTextureKey().markClean(context);
      }

    } finally {
      context.popFBOTextureRenderer();
    }
  }

  private void attachTextureToFramebuffer(final Texture tex, final int target, final int attachment) {
    final RenderContext context = ContextManager.getCurrentContext();
    final Texture.Type type = tex.getType();
    final var glType = TextureConstants.getGLType(type);
    switch (type) {
      case OneDimensional: {
        GL30C.glFramebufferTexture1D(target, attachment, glType, tex.getTextureIdForContext(context),
            tex.getTexRenderMipLevel());
        break;
      }

      case TwoDimensional:
      case OneDimensionalArray:
      case CubeMap: {
        final int target2D =
            type == Type.CubeMap ? TextureConstants.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace())
                : glType;
        GL30C.glFramebufferTexture2D(target, attachment, target2D, tex.getTextureIdForContext(context),
            tex.getTexRenderMipLevel());
        break;
      }

      case ThreeDimensional: {
        GL30C.glFramebufferTexture3D(target, attachment, glType, tex.getTextureIdForContext(context),
            tex.getTexRenderMipLevel(), tex.getTexRenderLayer());
        break;
      }

      case TwoDimensionalArray:
      case CubeMapArray: {
        GL30C.glFramebufferTextureLayer(target, attachment, tex.getTextureIdForContext(context),
            tex.getTexRenderMipLevel(), tex.getTexRenderLayer());
        break;
      }

      default:
        throw new Ardor3dException("Unsupported texture type: " + type);
    }
  }

  @Override
  protected void setupForSingleTexDraw(final Texture tex) {

    if (tex.getTextureStoreFormat().isDepthFormat()) {
      // No color buffer
      GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL30C.GL_RENDERBUFFER, 0);
      // Attach depth texture to FBO
      attachTextureToFramebuffer(tex, GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT);

      setDrawBuffer(GL11C.GL_NONE);
      setReadBuffer(GL11C.GL_NONE);
    } else {
      // Attach color texture to FBO
      attachTextureToFramebuffer(tex, GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0);
      // Setup depth RB
      if (_depthRBID != 0) {
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL30C.GL_RENDERBUFFER,
            _depthRBID);
      }

      setDrawBuffer(GL30C.GL_COLOR_ATTACHMENT0);
      setReadBuffer(GL30C.GL_COLOR_ATTACHMENT0);
    }

    // Check FBO complete
    checkFBOComplete(_fboID);
  }

  private void setReadBuffer(final int attachVal) {
    GL11C.glReadBuffer(attachVal);
  }

  private void setDrawBuffer(final int attachVal) {
    GL11C.glDrawBuffer(attachVal);
  }

  private void setDrawBuffers(final int maxEntry) {
    if (maxEntry <= 1) {
      setDrawBuffer(maxEntry != 0 ? GL30C.GL_COLOR_ATTACHMENT0 : GL11C.GL_NONE);
    } else {
      // We should only get to this point if we support ARBDrawBuffers.
      _attachBuffer.clear();
      _attachBuffer.limit(maxEntry);
      GL20C.glDrawBuffers(_attachBuffer);
    }
  }

  @Override
  protected void takedownForSingleTexDraw(final Texture tex) {
    if (isEnableMipGeneration()) {
      // automatically generate mipmaps for our texture, if supported.
      if (tex.getMinificationFilter().usesMipMapLevels()) {
        Lwjgl3TextureStateUtil.doTextureBind(tex, 0, true);
        GL30C.glGenerateMipmap(TextureConstants.getGLType(tex.getType()));
      }
    }

    tex.getTextureKey().markClean(ContextManager.getCurrentContext());
  }

  @Override
  protected void setMSFBO() {
    GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, _msfboID);
  }

  @Override
  protected void blitMSFBO() {
    GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, _msfboID);
    GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, _fboID);
    GL30C.glBlitFramebuffer(0, 0, _width, _height, 0, 0, _width, _height,
        GL11C.GL_COLOR_BUFFER_BIT | GL11C.GL_DEPTH_BUFFER_BIT, GL11C.GL_NEAREST);

    GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, _fboID);
  }

  /**
   * Check the currently bound FBO status for completeness. The passed in fboID is for informational
   * purposes only.
   *
   * @param fboID
   *          an id to use for log messages, particularly if there are any issues.
   */
  public static void checkFBOComplete(final int fboID) {
    final int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
    switch (status) {
      case GL30C.GL_FRAMEBUFFER_COMPLETE:
        break;
      case GL30C.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
        throw new IllegalStateException(
            "FrameBuffer: " + fboID + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT exception");
      case GL30C.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
        throw new IllegalStateException(
            "FrameBuffer: " + fboID + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT exception");
      case GL30C.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
        throw new IllegalStateException(
            "FrameBuffer: " + fboID + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER exception");
      case GL30C.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
        throw new IllegalStateException(
            "FrameBuffer: " + fboID + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER exception");
      case GL30C.GL_FRAMEBUFFER_UNSUPPORTED:
        throw new IllegalStateException(
            "FrameBuffer: " + fboID + ", has caused a GL_FRAMEBUFFER_UNSUPPORTED exception.");
      case GL30C.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
        throw new IllegalStateException(
            "FrameBuffer: " + fboID + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE exception.");
      default:
        throw new IllegalStateException("Unexpected reply from glCheckFramebufferStatusEXT: " + status);
    }
  }

  @Override
  public void copyToTexture(final Texture tex, final int x, final int y, final int width, final int height,
      final int xoffset, final int yoffset) {
    Lwjgl3TextureStateUtil.doTextureBind(tex, 0, true);

    if (tex.getType() == Type.TwoDimensional) {
      GL11C.glCopyTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, xoffset, yoffset, x, y, width, height);
    } else if (tex.getType() == Type.CubeMap) {
      GL11C.glCopyTexSubImage2D(TextureConstants.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()), 0,
          xoffset, yoffset, x, y, width, height);
    } else {
      throw new IllegalArgumentException("Invalid texture type: " + tex.getType());
    }
  }

  @Override
  protected void clearBuffers(final int clear) {
    GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
    _parentRenderer.clearBuffers(clear);
  }

  @Override
  public void resize(final int width, final int height, final int depthBits) {
    if (_width == width && _height == height && _depthBits == depthBits) {
      return;
    }

    _width = width;
    _height = height;
    _depthBits = depthBits;

    _camera.resize(_width, _height);

    // if we already have these render buffers, update their storage
    if (_depthRBID > 0) {
      GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, _depthRBID);
      GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, getDepthFormat(), _width, _height);
      GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);
    }

    if (_samples > 0) {
      if (_mscolorRBID > 0) {
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, _mscolorRBID);
        GL30C.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, _samples, GL11C.GL_RGBA, _width, _height);
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);
      }

      if (_msdepthRBID > 0) {
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, _msdepthRBID);
        GL30C.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, _samples, getDepthFormat(), _width, _height);
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);
      }
    }
  }

  @Override
  public void activate() {
    // Lazy init
    if (_fboID == 0) {
      // Create our texture binding FBO
      _fboID = GL30C.glGenFramebuffers(); // generate id

      // Create a depth renderbuffer to use for RTT use
      if (_depthBits >= 0) {
        _depthRBID = GL30C.glGenRenderbuffers(); // generate id
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, _depthRBID);
        GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, getDepthFormat(), _width, _height);
      }

      // unbind...
      GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);

      // If we support it, rustle up a multisample framebuffer + renderbuffers
      if (_samples != 0 && _supportsMultisample) {
        // create ms framebuffer object
        _msfboID = GL30C.glGenFramebuffers();

        // create ms renderbuffers
        _mscolorRBID = GL30C.glGenRenderbuffers(); // generate id
        _msdepthRBID = GL30C.glGenRenderbuffers(); // generate id

        // set up renderbuffer properties
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, _mscolorRBID);
        GL30C.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, _samples, GL11C.GL_RGBA, _width, _height);

        if (_depthBits >= 0) {
          GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, _msdepthRBID);
          GL30C.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, _samples, getDepthFormat(), _width, _height);
        }

        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, _msfboID);
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL30C.GL_RENDERBUFFER,
            _mscolorRBID);
        GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL30C.GL_RENDERBUFFER,
            _msdepthRBID);

        // check for errors
        checkFBOComplete(_msfboID);

        // release
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
      }
    }

    if (_active == 0) {

      final RenderContext context = ContextManager.getCurrentContext();
      final RendererRecord record = context.getRendererRecord();

      // needed as FBOs do not share this flag it seems
      record.setClippingTestValid(false);

      // push a delimiter onto the clip stack
      _neededClip = _parentRenderer.getScissorUtils().isClipTestEnabled();
      if (_neededClip) {
        _parentRenderer.getScissorUtils().pushEmptyClip();
      }

      GL11C.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
          _backgroundColor.getAlpha());
      GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, _fboID);

      // Set our enforced states
      ContextManager.getCurrentContext().pushEnforcedStates();
      ContextManager.getCurrentContext().clearEnforcedStates();
      ContextManager.getCurrentContext().enforceStates(_enforcedStates);

      // Set our enforced material
      ContextManager.getCurrentContext().pushEnforcedMaterial();
      ContextManager.getCurrentContext().enforceMaterial(_enforcedMaterial);
    }
    _active++;
  }

  private int getDepthFormat() {
    int format = GL11C.GL_DEPTH_COMPONENT;
    switch (_depthBits) {
      case 16:
        format = GL14C.GL_DEPTH_COMPONENT16;
        break;
      case 24:
        format = GL14C.GL_DEPTH_COMPONENT24;
        break;
      case 32:
        format = GL14C.GL_DEPTH_COMPONENT32;
        break;
      default:
        // stick with the "undefined" GL_DEPTH_COMPONENT
    }
    return format;
  }

  @Override
  public void deactivate() {
    if (_active == 1) {
      final ReadOnlyColorRGBA bgColor = _parentRenderer.getBackgroundColor();
      GL11C.glClearColor(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgColor.getAlpha());
      GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);

      ContextManager.getCurrentContext().popEnforcedStates();
      ContextManager.getCurrentContext().popEnforcedMaterial();

      if (_neededClip) {
        _parentRenderer.getScissorUtils().popClip();
      }
    }
    _active--;
  }

  @Override
  public void cleanup() {
    if (_fboID != 0) {
      GL30C.glDeleteFramebuffers(_fboID);
    }

    if (_depthRBID != 0) {
      GL30C.glDeleteRenderbuffers(_depthRBID);
    }
  }
}
