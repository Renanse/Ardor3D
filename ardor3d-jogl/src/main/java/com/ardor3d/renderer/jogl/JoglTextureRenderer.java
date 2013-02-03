/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;

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
import com.ardor3d.scene.state.jogl.JoglTextureStateUtil;
import com.ardor3d.scene.state.jogl.util.JoglTextureUtil;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <p>
 * This class is used by Ardor3D's JOGL implementation to render textures. Users should <b>not</b> create this class
 * directly.
 * </p>
 * 
 * @see TextureRendererFactory
 */
public class JoglTextureRenderer extends AbstractFBOTextureRenderer {
    private static final Logger logger = Logger.getLogger(JoglTextureRenderer.class.getName());

    public JoglTextureRenderer(final int width, final int height, final int depthBits, final int samples,
            final Renderer parentRenderer, final ContextCapabilities caps) {
        super(width, height, depthBits, samples, parentRenderer, caps);

        if (caps.getMaxFBOColorAttachments() > 1) {
            _attachBuffer = BufferUtils.createIntBuffer(caps.getMaxFBOColorAttachments());
            for (int i = 0; i < caps.getMaxFBOColorAttachments(); i++) {
                _attachBuffer.put(GL.GL_COLOR_ATTACHMENT0 + i);
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

        final GL gl = GLContext.getCurrentGL();

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
        gl.glGenTextures(ibuf.limit(), ibuf); // TODO Check <size>
        final int textureId = ibuf.get(0);
        tex.setTextureIdForContext(context.getGlContextRep(), textureId);

        JoglTextureStateUtil.doTextureBind(tex, 0, true);

        // Initialize our texture with some default data.
        final int internalFormat = JoglTextureUtil.getGLInternalFormat(tex.getTextureStoreFormat());
        final int dataFormat = JoglTextureUtil.getGLPixelFormatFromStoreFormat(tex.getTextureStoreFormat());
        final int pixelDataType = JoglTextureUtil.getGLPixelDataType(tex.getRenderedTexturePixelDataType());

        if (tex.getType() == Type.TwoDimensional) {
            gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, _width, _height, 0, dataFormat, pixelDataType, null);
        } else {
            for (final Face face : Face.values()) {
                gl.glTexImage2D(JoglTextureStateUtil.getGLCubeMapFace(face), 0, internalFormat, _width, _height, 0,
                        dataFormat, pixelDataType, null);
            }
        }

        // Initialize mipmapping for this texture, if requested
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            gl.glGenerateMipmap(JoglTextureStateUtil.getGLType(tex.getType()));
        }

        // Setup filtering and wrap
        final TextureRecord texRecord = record.getTextureRecord(textureId, tex.getType());
        JoglTextureStateUtil.applyFilter(tex, texRecord, 0, record, context.getCapabilities());
        JoglTextureStateUtil.applyWrap(tex, texRecord, 0, record, context.getCapabilities());

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
        final GL gl = GLContext.getCurrentGL();

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
                    } else {
                        doDraw(toDrawB);
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
                        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0 + colorsAdded,
                                GL.GL_TEXTURE_2D, tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else if (tex.getType() == Type.CubeMap) {
                        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0 + colorsAdded,
                                JoglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()),
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
                        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_TEXTURE_2D,
                                tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else if (tex.getType() == Type.CubeMap) {
                        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,
                                JoglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()),
                                tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else {
                        throw new IllegalArgumentException("Invalid texture type: " + tex.getType());
                    }
                    _usingDepthRB = false;
                } else if (!_usingDepthRB) {
                    // setup our default depth render buffer if not already set
                    gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER,
                            _depthRBID);
                    _usingDepthRB = true;
                }

                setDrawBuffers(colorsAdded);
                setReadBuffer(colorsAdded != 0 ? GL.GL_COLOR_ATTACHMENT0 : GL.GL_NONE);

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
                final Texture tex = texs.get(x);
                if (tex.getMinificationFilter().usesMipMapLevels()) {
                    JoglTextureStateUtil.doTextureBind(tex, 0, true);
                    gl.glGenerateMipmap(JoglTextureStateUtil.getGLType(tex.getType()));
                }
            }
        } finally {
            ContextManager.getCurrentContext().popFBOTextureRenderer();
        }
    }

    @Override
    protected void setupForSingleTexDraw(final Texture tex) {
        final GL gl = GLContext.getCurrentGL();

        final RenderContext context = ContextManager.getCurrentContext();
        final int textureId = tex.getTextureIdForContext(context.getGlContextRep());

        if (tex.getTextureStoreFormat().isDepthFormat()) {
            // No color buffer
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_RENDERBUFFER, 0);

            // Setup depth texture into FBO
            if (tex.getType() == Type.TwoDimensional) {
                gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_TEXTURE_2D, textureId, 0);
            } else if (tex.getType() == Type.CubeMap) {
                gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,
                        JoglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()), textureId, 0);
            } else {
                throw new IllegalArgumentException("Can not render to texture of type: " + tex.getType());
            }

            setDrawBuffer(GL.GL_NONE);
            setReadBuffer(GL.GL_NONE);
        } else {
            // Set color texture into FBO
            if (tex.getType() == Type.TwoDimensional) {
                gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, textureId, 0);
            } else if (tex.getType() == Type.CubeMap) {
                gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
                        JoglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()), textureId, 0);
            } else {
                throw new IllegalArgumentException("Can not render to texture of type: " + tex.getType());
            }

            // setup depth RB
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, _depthRBID);

            setDrawBuffer(GL.GL_COLOR_ATTACHMENT0);
            setReadBuffer(GL.GL_COLOR_ATTACHMENT0);
        }

        // Check FBO complete
        checkFBOComplete(_fboID);
    }

    private void setReadBuffer(final int attachVal) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2GL3().glReadBuffer(attachVal);
    }

    private void setDrawBuffer(final int attachVal) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2GL3().glDrawBuffer(attachVal);
    }

    private void setDrawBuffers(final int maxEntry) {
        final GL gl = GLContext.getCurrentGL();

        if (maxEntry <= 1) {
            setDrawBuffer(maxEntry != 0 ? GL.GL_COLOR_ATTACHMENT0 : GL.GL_NONE);
        } else {
            // We should only get to this point if we support ARBDrawBuffers.
            _attachBuffer.clear();
            _attachBuffer.limit(maxEntry);
            gl.getGL2GL3().glDrawBuffers(_attachBuffer.limit(), _attachBuffer); // TODO Check <size>
        }
    }

    @Override
    protected void takedownForSingleTexDraw(final Texture tex) {
        final GL gl = GLContext.getCurrentGL();

        // automatically generate mipmaps for our texture.
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            JoglTextureStateUtil.doTextureBind(tex, 0, true);
            gl.glGenerateMipmap(JoglTextureStateUtil.getGLType(tex.getType()));
        }
    }

    @Override
    protected void setMSFBO() {
        final GL gl = GLContext.getCurrentGL();

        gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, _msfboID);
    }

    @Override
    protected void blitMSFBO() {
        final GL gl = GLContext.getCurrentGL();

        gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, _msfboID);
        gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, _fboID);
        gl.getGL2GL3().glBlitFramebuffer(0, 0, _width, _height, 0, 0, _width, _height,
                GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT, GL.GL_NEAREST);

        gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
    }

    /**
     * Check the currently bound FBO status for completeness. The passed in fboID is for informational purposes only.
     * 
     * @param fboID
     *            an id to use for log messages, particularly if there are any issues.
     */
    public static void checkFBOComplete(final int fboID) {
        final GL gl = GLContext.getCurrentGL();

        final int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
        switch (status) {
            case GL.GL_FRAMEBUFFER_COMPLETE:
                break;
            case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT exception");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT exception");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT exception");
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT exception");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT exception");
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT exception");
            case GL.GL_FRAMEBUFFER_UNSUPPORTED:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_UNSUPPORTED_EXT exception");
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE_EXT exception.");
            default:
                throw new RuntimeException("Unexpected reply from glCheckFramebufferStatusEXT: " + status);
        }
    }

    public void copyToTexture(final Texture tex, final int x, final int y, final int width, final int height,
            final int xoffset, final int yoffset) {
        final GL gl = GLContext.getCurrentGL();

        JoglTextureStateUtil.doTextureBind(tex, 0, true);

        if (tex.getType() == Type.TwoDimensional) {
            gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, 0, xoffset, yoffset, x, y, width, height);
        } else if (tex.getType() == Type.CubeMap) {
            gl.glCopyTexSubImage2D(JoglTextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()),
                    0, xoffset, yoffset, x, y, width, height);
        } else {
            throw new IllegalArgumentException("Invalid texture type: " + tex.getType());
        }
    }

    @Override
    protected void clearBuffers(final int clear) {
        final GL gl = GLContext.getCurrentGL();

        gl.glDisable(GL.GL_SCISSOR_TEST);
        _parentRenderer.clearBuffers(clear);
    }

    @Override
    protected void activate() {
        final GL gl = GLContext.getCurrentGL();

        // Lazy init
        if (_fboID == 0) {
            final IntBuffer buffer = BufferUtils.createIntBuffer(1);

            // Create our texture binding FBO
            gl.glGenFramebuffers(1, buffer); // generate id
            _fboID = buffer.get(0);

            // Create a depth renderbuffer to use for RTT use
            gl.glGenRenderbuffers(1, buffer); // generate id
            _depthRBID = buffer.get(0);
            gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, _depthRBID);
            int format = GL2ES2.GL_DEPTH_COMPONENT;
            if (_supportsDepthTexture && _depthBits > 0) {
                switch (_depthBits) {
                    case 16:
                        format = GL.GL_DEPTH_COMPONENT16;
                        break;
                    case 24:
                        format = GL.GL_DEPTH_COMPONENT24;
                        break;
                    case 32:
                        format = GL.GL_DEPTH_COMPONENT32;
                        break;
                    default:
                        // stick with the "undefined" GL_DEPTH_COMPONENT
                }
            }
            gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, format, _width, _height);

            // unbind...
            gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

            // If we support it, rustle up a multisample framebuffer + renderbuffers
            if (_samples != 0 && _supportsMultisample) {
                // create ms framebuffer object
                gl.glGenFramebuffers(1, buffer);
                _msfboID = buffer.get(0);

                // create ms renderbuffers
                gl.glGenRenderbuffers(1, buffer); // generate id
                _mscolorRBID = buffer.get(0);
                gl.glGenRenderbuffers(1, buffer); // generate id
                _msdepthRBID = buffer.get(0);

                // set up renderbuffer properties
                gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, _mscolorRBID);
                gl.getGL2GL3().glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, _samples, GL.GL_RGBA, _width,
                        _height);

                gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, _msdepthRBID);
                gl.getGL2GL3().glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, _samples, format, _width, _height);

                gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);

                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, _msfboID);
                gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_RENDERBUFFER,
                        _mscolorRBID);
                gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER,
                        _msdepthRBID);

                // check for errors
                checkFBOComplete(_msfboID);

                // release
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
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

            gl.glClearColor(_backgroundColor.getRed(), _backgroundColor.getGreen(), _backgroundColor.getBlue(),
                    _backgroundColor.getAlpha());
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, _fboID);
            ContextManager.getCurrentContext().pushEnforcedStates();
            ContextManager.getCurrentContext().clearEnforcedStates();
            ContextManager.getCurrentContext().enforceStates(_enforcedStates);
        }
        _active++;
    }

    @Override
    protected void deactivate() {
        final GL gl = GLContext.getCurrentGL();

        if (_active == 1) {
            final ReadOnlyColorRGBA bgColor = _parentRenderer.getBackgroundColor();
            gl.glClearColor(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), bgColor.getAlpha());
            gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
            ContextManager.getCurrentContext().popEnforcedStates();

            if (_neededClip) {
                _parentRenderer.popClip();
            }
        }
        _active--;
    }

    public void cleanup() {
        final GL gl = GLContext.getCurrentGL();

        if (_fboID != 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_fboID);
            id.rewind();
            gl.glDeleteFramebuffers(id.limit(), id);
        }

        if (_depthRBID != 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_depthRBID);
            id.rewind();
            gl.glDeleteRenderbuffers(id.limit(), id);
        }
    }
}
