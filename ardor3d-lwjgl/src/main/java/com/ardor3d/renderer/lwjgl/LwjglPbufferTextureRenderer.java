/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.lwjgl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.opengl.RenderTexture;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.AbstractPbufferTextureRenderer;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.scene.state.lwjgl.LwjglTextureStateUtil;
import com.ardor3d.scene.state.lwjgl.util.LwjglTextureUtil;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <p>
 * This class is used by Ardor3D's LWJGL implementation to render textures. Users should <b>not </b> create this class
 * directly.
 * </p>
 * 
 * @see TextureRendererFactory
 */
public class LwjglPbufferTextureRenderer extends AbstractPbufferTextureRenderer {
    private static final Logger logger = Logger.getLogger(LwjglPbufferTextureRenderer.class.getName());

    /* Pbuffer instance */
    private Pbuffer _pbuffer;

    private RenderTexture _texture;

    public LwjglPbufferTextureRenderer(final DisplaySettings settings, final Renderer parentRenderer,
            final ContextCapabilities caps) {
        super(settings, parentRenderer, caps);

        int pTarget = RenderTexture.RENDER_TEXTURE_2D;

        if (!MathUtils.isPowerOfTwo(_width) || !MathUtils.isPowerOfTwo(_height)) {
            pTarget = RenderTexture.RENDER_TEXTURE_RECTANGLE;
        }

        // signature: boolean useRGB, boolean useRGBA, boolean useDepth, boolean isRectangle, int target, int mipmaps
        _texture = new RenderTexture(false, true, true, pTarget == RenderTexture.RENDER_TEXTURE_RECTANGLE, pTarget, 0);

        setMultipleTargets(false);
    }

    /**
     * <code>setupTexture</code> initializes a new Texture object for use with TextureRenderer. Generates a valid gl
     * texture id for this texture and inits the data type for the texture.
     */
    public void setupTexture(final Texture tex) {
        if (tex.getType() != Type.TwoDimensional) {
            throw new IllegalArgumentException("Unsupported type: " + tex.getType());
        }
        final RenderContext context = ContextManager.getCurrentContext();
        final TextureStateRecord record = (TextureStateRecord) context.getStateRecord(RenderState.StateType.Texture);

        // check if we are already setup... if so, throw error.
        if (tex.getTextureKey() == null) {
            tex.setTextureKey(TextureKey.getRTTKey(tex.getMinificationFilter()));
        } else if (tex.getTextureIdForContext(context.getGlContextRep()) != 0) {
            throw new Ardor3dException("Texture is already setup and has id.");
        }

        // Create the texture
        final IntBuffer ibuf = BufferUtils.createIntBuffer(1);
        GL11.glGenTextures(ibuf);
        final int textureId = ibuf.get(0);
        tex.setTextureIdForContext(context.getGlContextRep(), textureId);

        LwjglTextureStateUtil.doTextureBind(tex, 0, true);

        // Initialize our texture with some default data.
        final int internalFormat = LwjglTextureUtil.getGLInternalFormat(tex.getTextureStoreFormat());
        final int dataFormat = LwjglTextureUtil.getGLPixelFormatFromStoreFormat(tex.getTextureStoreFormat());
        final int pixelDataType = LwjglTextureUtil.getGLPixelDataType(tex.getRenderedTexturePixelDataType());

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, _width, _height, 0, dataFormat, pixelDataType,
                (ByteBuffer) null);

        // Setup filtering and wrap
        final TextureRecord texRecord = record.getTextureRecord(textureId, tex.getType());
        LwjglTextureStateUtil.applyFilter(tex, texRecord, 0, record, context.getCapabilities());
        LwjglTextureStateUtil.applyWrap(tex, texRecord, 0, record, context.getCapabilities());

        logger.fine("setup pbuffer tex" + textureId + ": " + _width + "," + _height);
    }

    public void render(final Spatial spat, final Texture tex, final int clear) {
        render(null, spat, null, tex, clear);
    }

    public void render(final List<? extends Spatial> spat, final Texture tex, final int clear) {
        render(spat, null, null, tex, clear);
    }

    public void render(final Scene scene, final Texture tex, final int clear) {
        render(null, null, scene, tex, clear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final Scene toDrawC,
            final Texture tex, final int clear) {
        try {
            if (_pbuffer == null || _pbuffer.isBufferLost()) {
                if (_pbuffer != null && _pbuffer.isBufferLost()) {
                    logger.warning("PBuffer contents lost - will recreate the buffer");
                    deactivate();
                    _pbuffer.destroy();
                }
                initPbuffer();
            }

            if (_useDirectRender && !tex.getTextureStoreFormat().isDepthFormat()) {
                // setup and render directly to a 2d texture.
                _pbuffer.releaseTexImage(Pbuffer.FRONT_LEFT_BUFFER);
                activate();
                switchCameraIn(clear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else if (toDrawB != null) {
                    doDraw(toDrawB);
                } else {
                    doDraw(toDrawC);
                }

                deactivate();
                switchCameraOut();
                LwjglTextureStateUtil.doTextureBind(tex, 0, true);
                _pbuffer.bindTexImage(Pbuffer.FRONT_LEFT_BUFFER);
            } else {
                // render and copy to a texture
                activate();
                switchCameraIn(clear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                copyToTexture(tex, 0, 0, _width, _height, 0, 0);

                deactivate();
            }

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture)", "Exception", e);
        }
    }

    public void render(final Spatial spat, final List<Texture> texs, final int clear) {
        render(null, spat, null, texs, clear);
    }

    public void render(final List<? extends Spatial> spat, final List<Texture> texs, final int clear) {
        render(spat, null, null, texs, clear);
    }

    public void render(final Scene scene, final List<Texture> texs, final int clear) {
        render(null, null, scene, texs, clear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final Scene toDrawC,
            final List<Texture> texs, final int clear) {
        try {
            if (_pbuffer == null || _pbuffer.isBufferLost()) {
                if (_pbuffer != null && _pbuffer.isBufferLost()) {
                    logger.warning("PBuffer contents lost - will recreate the buffer");
                    deactivate();
                    _pbuffer.destroy();
                }
                initPbuffer();
            }

            if (texs.size() == 1 && _useDirectRender && !texs.get(0).getTextureStoreFormat().isDepthFormat()) {
                // setup and render directly to a 2d texture.
                LwjglTextureStateUtil.doTextureBind(texs.get(0), 0, true);
                activate();
                switchCameraIn(clear);
                _pbuffer.releaseTexImage(Pbuffer.FRONT_LEFT_BUFFER);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                deactivate();
                _pbuffer.bindTexImage(Pbuffer.FRONT_LEFT_BUFFER);
            } else {
                // render and copy to a texture
                activate();
                switchCameraIn(clear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();

                for (int i = 0; i < texs.size(); i++) {
                    copyToTexture(texs.get(i), 0, 0, _width, _height, 0, 0);
                }

                deactivate();
            }

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "render(Spatial, Texture)", "Exception", e);
        }
    }

    public void copyToTexture(final Texture tex, final int x, final int y, final int width, final int height,
            final int xoffset, final int yoffset) {
        LwjglTextureStateUtil.doTextureBind(tex, 0, true);

        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, xoffset, yoffset, x, y, width, height);
    }

    @Override
    protected void clearBuffers(final int clear) {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        _parentRenderer.clearBuffers(clear);
    }

    private void initPbuffer() {

        try {
            if (_pbuffer != null) {
                giveBackContext();
                ContextManager.removeContext(_pbuffer);
            }
            final PixelFormat format = new PixelFormat(_settings.getAlphaBits(), _settings.getDepthBits(),
                    _settings.getStencilBits()).withSamples(_settings.getSamples())
                    .withBitsPerPixel(_settings.getColorDepth()).withStereo(_settings.isStereo());
            _pbuffer = new Pbuffer(_width, _height, format, _texture, null);
            final Object contextKey = _pbuffer;
            try {
                _pbuffer.makeCurrent();
            } catch (final LWJGLException e) {
                throw new RuntimeException(e);
            }

            final LwjglContextCapabilities caps = new LwjglContextCapabilities(GLContext.getCapabilities());
            ContextManager.addContext(contextKey,
                    new RenderContext(contextKey, caps, ContextManager.getCurrentContext()));

        } catch (final Exception e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "initPbuffer()", "Exception", e);

            if (_texture != null && _useDirectRender) {
                logger.warning("Your card claims to support Render to Texture but fails to enact it.  Updating your driver might solve this problem.");
                logger.warning("Attempting to fall back to Copy Texture.");
                _texture = null;
                _useDirectRender = false;
                initPbuffer();
                return;
            }

            logger.log(Level.WARNING, "Failed to create Pbuffer.", e);
            return;
        }

        try {
            activate();

            _width = _pbuffer.getWidth();
            _height = _pbuffer.getHeight();

            deactivate();
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Failed to initialize created Pbuffer.", e);
            return;
        }
    }

    private void activate() {
        if (_active == 0) {
            try {
                _oldContext = ContextManager.getCurrentContext();
                _pbuffer.makeCurrent();

                ContextManager.switchContext(_pbuffer);

                ContextManager.getCurrentContext().clearEnforcedStates();
                ContextManager.getCurrentContext().enforceStates(_enforcedStates);

                if (_bgColorDirty) {
                    GL11.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(),
                            _backgroundColor.getBlue(), _backgroundColor.getAlpha());
                    _bgColorDirty = false;
                }
            } catch (final LWJGLException e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "activate()", "Exception", e);
                throw new Ardor3dException();
            }
        }
        _active++;
    }

    private void deactivate() {
        if (_active == 1) {
            try {
                giveBackContext();
            } catch (final LWJGLException e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "deactivate()", "Exception", e);
                throw new Ardor3dException();
            }
        }
        _active--;
    }

    // XXX: Need another look at this to make it generic?
    private void giveBackContext() throws LWJGLException {
        if (Display.isCreated()) {
            Display.makeCurrent();
            ContextManager.switchContext(_oldContext.getContextKey());
        } else if (_oldContext.getContextKey() instanceof AWTGLCanvas) {
            ((AWTGLCanvas) _oldContext.getContextKey()).makeCurrent();
            ContextManager.switchContext(_oldContext.getContextKey());
        }
    }

    public void cleanup() {
        ContextManager.removeContext(_pbuffer);
        _pbuffer.destroy();
    }

    public void setMultipleTargets(final boolean force) {
        if (force) {
            logger.fine("Copy Texture Pbuffer used!");
            _useDirectRender = false;
            _texture = null;
            if (_pbuffer != null) {
                try {
                    giveBackContext();
                } catch (final LWJGLException ex) {
                }
                ContextManager.removeContext(_pbuffer);
            }
        } else {
            if ((Pbuffer.getCapabilities() & Pbuffer.RENDER_TEXTURE_SUPPORTED) != 0) {
                logger.fine("Render to Texture Pbuffer supported!");
                if (_texture == null) {
                    logger.fine("No RenderTexture used in init, falling back to Copy Texture PBuffer.");
                    _useDirectRender = false;
                } else {
                    _useDirectRender = true;
                }
            } else {
                logger.fine("Copy Texture Pbuffer supported!");
                _texture = null;
            }
        }
    }
}
