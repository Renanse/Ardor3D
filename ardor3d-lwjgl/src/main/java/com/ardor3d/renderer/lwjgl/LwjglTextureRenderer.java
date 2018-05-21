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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBDepthTexture;
import org.lwjgl.opengl.ARBDrawBuffers;
import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import com.ardor3d.framework.Scene;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.AbstractFBOTextureRenderer;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRendererFactory;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.record.RendererRecord;
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
 * This class is used by Ardor3D's LWJGL implementation to render textures. Users should <b>not</b> create this class
 * directly.
 * </p>
 *
 * @see TextureRendererFactory
 */
public class LwjglTextureRenderer extends AbstractFBOTextureRenderer {
    private static final Logger logger = Logger.getLogger(LwjglTextureRenderer.class.getName());

    public LwjglTextureRenderer(final int width, final int height, final int depthBits, final int samples,
            final Renderer parentRenderer, final ContextCapabilities caps) {
        super(width, height, depthBits, samples, parentRenderer, caps);

        if (caps.getMaxFBOColorAttachments() > 1) {
            _attachBuffer = BufferUtils.createIntBuffer(caps.getMaxFBOColorAttachments());
            for (int i = 0; i < caps.getMaxFBOColorAttachments(); i++) {
                _attachBuffer.put(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT + i);
            }
        }
    }

    /**
     * <code>setupTexture</code> initializes a new Texture object for use with TextureRenderer. Generates a valid OpenGL
     * texture id for this texture and initializes the data type for the texture.
     */
    public void setupTexture(final Texture tex) {
        if (tex.getType() != Type.TwoDimensional && tex.getType() != Type.CubeMap) {
            throw new IllegalArgumentException("Texture type not supported: " + tex.getType());
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

        if (tex.getType() == Type.TwoDimensional) {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, _width, _height, 0, dataFormat, pixelDataType,
                    (ByteBuffer) null);
        } else {
            for (final Face face : Face.values()) {
                GL11.glTexImage2D(LwjglTextureStateUtil.getGLCubeMapFace(face), 0, internalFormat, _width, _height, 0,
                        dataFormat, pixelDataType, (ByteBuffer) null);
            }
        }

        // Initialize mipmapping for this texture, if requested
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            EXTFramebufferObject.glGenerateMipmapEXT(LwjglTextureStateUtil.getGLType(tex.getType()));
        }

        // Setup filtering and wrap
        final TextureRecord texRecord = record.getTextureRecord(textureId, tex.getType());
        LwjglTextureStateUtil.applyFilter(tex, texRecord, 0, record, context.getCapabilities());
        LwjglTextureStateUtil.applyWrap(tex, texRecord, 0, record, context.getCapabilities());

        logger.fine("setup fbo tex with id " + textureId + ": " + _width + "," + _height);
    }

    public void render(final Spatial spat, final List<Texture> texs, final int clear) {
        render(null, spat, null, texs, clear);
    }

    public void render(final List<? extends Spatial> spat, final List<Texture> texs, final int clear) {
        render(spat, null, null, texs, clear);
    }

    @Override
    public void render(final Scene scene, final List<Texture> texs, final int clear) {
        render(null, null, scene, texs, clear);
    }

    private void render(final List<? extends Spatial> toDrawA, final Spatial toDrawB, final Scene toDrawC,
            final List<Texture> texs, final int clear) {

        final int maxDrawBuffers = ContextManager.getCurrentContext().getCapabilities().getMaxFBOColorAttachments();

        // if we only support 1 draw buffer at a time anyway, we'll have to render to each texture individually...
        if (maxDrawBuffers == 1 || texs.size() == 1) {
            try {
                ContextManager.getCurrentContext().pushFBOTextureRenderer(this);

                for (int i = 0; i < texs.size(); i++) {
                    final Texture tex = texs.get(i);

                    setupForSingleTexDraw(tex);

                    if (_samples > 0 && _supportsMultisample) {
                        setMSFBO();
                    }

                    switchCameraIn(clear);
                    if (toDrawA != null) {
                        doDraw(toDrawA);
                    } else if (toDrawB != null) {
                        doDraw(toDrawB);
                    } else {
                        doDraw(toDrawC);
                    }
                    switchCameraOut();

                    if (_samples > 0 && _supportsMultisample) {
                        blitMSFBO();
                    }

                    takedownForSingleTexDraw(tex);
                }
            } finally {
                ContextManager.getCurrentContext().popFBOTextureRenderer();
            }
            return;
        }
        try {
            ContextManager.getCurrentContext().pushFBOTextureRenderer(this);

            // Otherwise, we can streamline this by rendering to multiple textures at once.
            // first determine how many groups we need
            final LinkedList<Texture> depths = new LinkedList<Texture>();
            final LinkedList<Texture> colors = new LinkedList<Texture>();
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

            final RenderContext context = ContextManager.getCurrentContext();
            for (int i = 0; i < groups; i++) {
                // First handle colors
                int colorsAdded = 0;
                while (colorsAdded < maxDrawBuffers && !colors.isEmpty()) {
                    final Texture tex = colors.removeFirst();
                    if (tex.getType() == Type.TwoDimensional) {
                        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT + colorsAdded, GL11.GL_TEXTURE_2D,
                                tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else if (tex.getType() == Type.CubeMap) {
                        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT + colorsAdded,
                                LwjglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()),
                                tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else {
                        throw new IllegalArgumentException("Invalid texture type: " + tex.getType());
                    }
                    colorsAdded++;
                }

                // Now take care of depth.
                if (!depths.isEmpty()) {
                    final Texture tex = depths.removeFirst();
                    // Set up our depth texture
                    if (tex.getType() == Type.TwoDimensional) {
                        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, GL11.GL_TEXTURE_2D,
                                tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else if (tex.getType() == Type.CubeMap) {
                        EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                                EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                                LwjglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()),
                                tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else {
                        throw new IllegalArgumentException("Invalid texture type: " + tex.getType());
                    }
                    _usingDepthRB = false;
                } else if (!_usingDepthRB && _depthRBID != 0) {
                    // setup our default depth render buffer if not already set
                    EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                            EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                            _depthRBID);
                    _usingDepthRB = true;
                }

                setDrawBuffers(colorsAdded);
                setReadBuffer(colorsAdded != 0 ? EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT : GL11.GL_NONE);

                // Check FBO complete
                checkFBOComplete(_fboID);

                switchCameraIn(clear);

                if (toDrawA != null) {
                    doDraw(toDrawA);
                } else {
                    doDraw(toDrawB);
                }

                switchCameraOut();
            }

            // automatically generate mipmaps for our textures.
            for (int x = 0, max = texs.size(); x < max; x++) {
                if (texs.get(x).getMinificationFilter().usesMipMapLevels()) {
                    final Texture tex = texs.get(x);
                    if (tex.getMinificationFilter().usesMipMapLevels()) {
                        LwjglTextureStateUtil.doTextureBind(texs.get(x), 0, true);
                        EXTFramebufferObject.glGenerateMipmapEXT(LwjglTextureStateUtil.getGLType(tex.getType()));
                    }
                }
            }

        } finally {
            ContextManager.getCurrentContext().popFBOTextureRenderer();
        }
    }

    @Override
    protected void setupForSingleTexDraw(final Texture tex) {
        final RenderContext context = ContextManager.getCurrentContext();
        final int textureId = tex.getTextureIdForContext(context.getGlContextRep());

        if (tex.getTextureStoreFormat().isDepthFormat()) {
            // No color buffer
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, 0);

            // Setup depth texture into FBO
            if (tex.getType() == Type.TwoDimensional) {
                EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                        EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, GL11.GL_TEXTURE_2D, textureId, 0);
            } else if (tex.getType() == Type.CubeMap) {
                EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                        EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT,
                        LwjglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()), textureId,
                        0);
            } else {
                throw new IllegalArgumentException("Can not render to texture of type: " + tex.getType());
            }

            setDrawBuffer(GL11.GL_NONE);
            setReadBuffer(GL11.GL_NONE);
        } else {
            // Set color texture into FBO
            if (tex.getType() == Type.TwoDimensional) {
                EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                        EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, GL11.GL_TEXTURE_2D, textureId, 0);
            } else if (tex.getType() == Type.CubeMap) {
                EXTFramebufferObject.glFramebufferTexture2DEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                        EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT,
                        LwjglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()), textureId,
                        0);
            } else {
                throw new IllegalArgumentException("Can not render to texture of type: " + tex.getType());
            }

            // setup depth RB
            if (_depthRBID != 0) {
                EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                        EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                        _depthRBID);
            }

            setDrawBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
            setReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
        }

        // Check FBO complete
        checkFBOComplete(_fboID);
    }

    private void setReadBuffer(final int attachVal) {
        GL11.glReadBuffer(attachVal);
    }

    private void setDrawBuffer(final int attachVal) {
        GL11.glDrawBuffer(attachVal);
    }

    private void setDrawBuffers(final int maxEntry) {
        if (maxEntry <= 1) {
            setDrawBuffer(maxEntry != 0 ? EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT : GL11.GL_NONE);
        } else {
            // We should only get to this point if we support ARBDrawBuffers.
            _attachBuffer.clear();
            _attachBuffer.limit(maxEntry);
            ARBDrawBuffers.glDrawBuffersARB(_attachBuffer);
        }
    }

    @Override
    protected void takedownForSingleTexDraw(final Texture tex) {
        // automatically generate mipmaps for our texture.
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            LwjglTextureStateUtil.doTextureBind(tex, 0, true);
            EXTFramebufferObject.glGenerateMipmapEXT(LwjglTextureStateUtil.getGLType(tex.getType()));
        }
    }

    @Override
    protected void setMSFBO() {
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT, _msfboID);
    }

    @Override
    protected void blitMSFBO() {
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT, _msfboID);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT, _fboID);
        EXTFramebufferBlit.glBlitFramebufferEXT(0, 0, _width, _height, 0, 0, _width, _height, GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);

        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT, 0);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT, 0);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
    }

    /**
     * Check the currently bound FBO status for completeness. The passed in fboID is for informational purposes only.
     *
     * @param fboID
     *            an id to use for log messages, particularly if there are any issues.
     */
    public static void checkFBOComplete(final int fboID) {
        final int status = EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
        switch (status) {
            case EXTFramebufferObject.GL_FRAMEBUFFER_COMPLETE_EXT:
                break;
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT exception");
            case EXTFramebufferObject.GL_FRAMEBUFFER_UNSUPPORTED_EXT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_UNSUPPORTED_EXT exception.");
            case EXTFramebufferMultisample.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE_EXT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE_EXT exception.");
            default:
                throw new IllegalStateException("Unexpected reply from glCheckFramebufferStatusEXT: " + status);
        }
    }

    public void copyToTexture(final Texture tex, final int x, final int y, final int width, final int height,
            final int xoffset, final int yoffset) {
        LwjglTextureStateUtil.doTextureBind(tex, 0, true);

        if (tex.getType() == Type.TwoDimensional) {
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, xoffset, yoffset, x, y, width, height);
        } else if (tex.getType() == Type.CubeMap) {
            GL11.glCopyTexSubImage2D(
                    LwjglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()), 0, xoffset,
                    yoffset, x, y, width, height);
        } else {
            throw new IllegalArgumentException("Invalid texture type: " + tex.getType());
        }
    }

    @Override
    protected void clearBuffers(final int clear) {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        _parentRenderer.clearBuffers(clear);
    }

    @Override
    protected void activate() {
        // Lazy init
        if (_fboID == 0) {
            final IntBuffer buffer = BufferUtils.createIntBuffer(1);

            // Create our texture binding FBO
            EXTFramebufferObject.glGenFramebuffersEXT(buffer); // generate id
            _fboID = buffer.get(0);

            // Create a depth renderbuffer to use for RTT use
            if (_supportsDepthTexture && _depthBits != 0) {
                EXTFramebufferObject.glGenRenderbuffersEXT(buffer); // generate id
                _depthRBID = buffer.get(0);
                EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _depthRBID);
                EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                        getDepthFormat(), _width, _height);
            }

            // unbind...
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, 0);
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);

            // If we support it, rustle up a multisample framebuffer + renderbuffers
            if (_samples != 0 && _supportsMultisample) {
                // create ms framebuffer object
                EXTFramebufferObject.glGenFramebuffersEXT(buffer);
                _msfboID = buffer.get(0);

                // create ms renderbuffers
                EXTFramebufferObject.glGenRenderbuffersEXT(buffer); // generate id
                _mscolorRBID = buffer.get(0);
                EXTFramebufferObject.glGenRenderbuffersEXT(buffer); // generate id
                _msdepthRBID = buffer.get(0);

                // set up renderbuffer properties
                EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _mscolorRBID);
                EXTFramebufferMultisample.glRenderbufferStorageMultisampleEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                        _samples, GL11.GL_RGBA, _width, _height);

                if (_supportsDepthTexture && _depthBits > 0) {
                    EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _msdepthRBID);
                    EXTFramebufferMultisample.glRenderbufferStorageMultisampleEXT(
                            EXTFramebufferObject.GL_RENDERBUFFER_EXT, _samples, getDepthFormat(), _width, _height);
                }

                EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, 0);

                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _msfboID);
                EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                        EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                        _mscolorRBID);
                EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                        EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                        _msdepthRBID);

                // check for errors
                checkFBOComplete(_msfboID);

                // release
                EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
            }

        }

        if (_active == 0) {

            final RenderContext context = ContextManager.getCurrentContext();
            final RendererRecord record = context.getRendererRecord();

            // needed as FBOs do not share this flag it seems
            record.setClippingTestValid(false);

            // push a delimiter onto the clip stack
            _neededClip = _parentRenderer.isClipTestEnabled();
            if (_neededClip) {
                _parentRenderer.pushEmptyClip();
            }

            GL11.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                    _backgroundColor.getAlpha());
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _fboID);
            ContextManager.getCurrentContext().pushEnforcedStates();
            ContextManager.getCurrentContext().clearEnforcedStates();
            ContextManager.getCurrentContext().enforceStates(_enforcedStates);
        }
        _active++;
    }

    private int getDepthFormat() {
        int format = GL11.GL_DEPTH_COMPONENT;
        switch (_depthBits) {
            case 16:
                format = ARBDepthTexture.GL_DEPTH_COMPONENT16_ARB;
                break;
            case 24:
                format = ARBDepthTexture.GL_DEPTH_COMPONENT24_ARB;
                break;
            case 32:
                format = ARBDepthTexture.GL_DEPTH_COMPONENT32_ARB;
                break;
            default:
                // stick with the "undefined" GL_DEPTH_COMPONENT
        }
        return format;
    }

    @Override
    protected void deactivate() {
        if (_active == 1) {
            final ReadOnlyColorRGBA bgColor = _parentRenderer.getBackgroundColor();
            GL11.glClearColor(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgColor.getAlpha());
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);

            ContextManager.getCurrentContext().popEnforcedStates();

            if (_neededClip) {
                _parentRenderer.popClip();
            }
        }
        _active--;
    }

    public void cleanup() {
        if (_fboID != 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_fboID);
            id.rewind();
            EXTFramebufferObject.glDeleteFramebuffersEXT(id);
        }

        if (_depthRBID != 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_depthRBID);
            id.rewind();
            EXTFramebufferObject.glDeleteRenderbuffersEXT(id);
        }
    }
}
