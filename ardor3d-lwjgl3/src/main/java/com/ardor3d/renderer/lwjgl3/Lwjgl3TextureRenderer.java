/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.lwjgl3;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.Type;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.renderer.state.record.TextureRecord;
import com.ardor3d.renderer.state.record.TextureStateRecord;
import com.ardor3d.renderer.texture.AbstractFBOTextureRenderer;
import com.ardor3d.renderer.texture.TextureRendererFactory;
import com.ardor3d.scene.state.lwjgl3.Lwjgl3TextureStateUtil;
import com.ardor3d.scene.state.lwjgl3.util.Lwjgl3TextureUtils;
import com.ardor3d.scenegraph.Renderable;
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
public class Lwjgl3TextureRenderer extends AbstractFBOTextureRenderer {
    private static final Logger logger = Logger.getLogger(Lwjgl3TextureRenderer.class.getName());

    private final Consumer<Renderable> drawRendConsumer = (final Renderable r) -> this.doDraw(r);
    private final Consumer<List<? extends Renderable>> drawRendListConsumer = (
            final List<? extends Renderable> list) -> this.doDraw(list);
    private final Consumer<Spatial> drawSpatConsumer = (final Spatial s) -> doDrawSpatial(s);
    private final Consumer<List<? extends Spatial>> drawSpatListConsumer = (
            final List<? extends Spatial> list) -> doDrawSpatials(list);

    public Lwjgl3TextureRenderer(final int width, final int height, final int depthBits, final int samples,
            final Renderer parentRenderer, final ContextCapabilities caps) {
        super(width, height, depthBits, samples, parentRenderer, caps);

        if (caps.getMaxFBOColorAttachments() > 1) {
            _attachBuffer = BufferUtils.createIntBuffer(caps.getMaxFBOColorAttachments());
            for (int i = 0; i < caps.getMaxFBOColorAttachments(); i++) {
                _attachBuffer.put(GL30C.GL_COLOR_ATTACHMENT0 + i);
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
        GL11C.glGenTextures(ibuf);
        final int textureId = ibuf.get(0);
        tex.setTextureIdForContext(context.getGlContextRep(), textureId);

        Lwjgl3TextureStateUtil.doTextureBind(tex, 0, true);

        // Initialize our texture with some default data.
        final int internalFormat = Lwjgl3TextureUtils.getGLInternalFormat(tex.getTextureStoreFormat());
        final int dataFormat = Lwjgl3TextureUtils.getGLPixelFormatFromStoreFormat(tex.getTextureStoreFormat());
        final int pixelDataType = Lwjgl3TextureUtils.getGLPixelDataType(tex.getRenderedTexturePixelDataType());

        if (tex.getType() == Type.TwoDimensional) {
            GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, internalFormat, _width, _height, 0, dataFormat, pixelDataType,
                    (ByteBuffer) null);
        } else {
            for (final Face face : Face.values()) {
                GL11C.glTexImage2D(Lwjgl3TextureStateUtil.getGLCubeMapFace(face), 0, internalFormat, _width, _height, 0,
                        dataFormat, pixelDataType, (ByteBuffer) null);
            }
        }

        // Initialize mipmapping for this texture, if requested
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            GL30C.glGenerateMipmap(Lwjgl3TextureStateUtil.getGLType(tex.getType()));
        }

        // Setup filtering and wrap
        final TextureRecord texRecord = record.getTextureRecord(textureId, tex.getType());
        Lwjgl3TextureStateUtil.applyFilter(tex, texRecord, 0, record, context.getCapabilities());
        Lwjgl3TextureStateUtil.applyWrap(tex, texRecord, 0, record, context.getCapabilities());

        logger.fine("setup fbo tex with id " + textureId + ": " + _width + "," + _height);
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
                        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0 + colorsAdded,
                                GL11C.GL_TEXTURE_2D, tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else if (tex.getType() == Type.CubeMap) {
                        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0 + colorsAdded,
                                Lwjgl3TextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()),
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
                        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                                GL11C.GL_TEXTURE_2D, tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else if (tex.getType() == Type.CubeMap) {
                        GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                                Lwjgl3TextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()),
                                tex.getTextureIdForContext(context.getGlContextRep()), 0);
                    } else {
                        throw new IllegalArgumentException("Invalid texture type: " + tex.getType());
                    }
                    _usingDepthRB = false;
                } else if (!_usingDepthRB && _depthRBID != 0) {
                    // setup our default depth render buffer if not already set
                    GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                            GL30C.GL_RENDERBUFFER, _depthRBID);
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

            // automatically generate mipmaps for our textures.
            for (int x = 0, max = texs.size(); x < max; x++) {
                if (texs.get(x).getMinificationFilter().usesMipMapLevels()) {
                    final Texture tex = texs.get(x);
                    if (tex.getMinificationFilter().usesMipMapLevels()) {
                        Lwjgl3TextureStateUtil.doTextureBind(texs.get(x), 0, true);
                        GL30C.glGenerateMipmap(Lwjgl3TextureStateUtil.getGLType(tex.getType()));
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
            GL30C.glFramebufferRenderbuffer(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL30C.GL_RENDERBUFFER, 0);

            // Setup depth texture into FBO
            if (tex.getType() == Type.TwoDimensional) {
                GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL11C.GL_TEXTURE_2D,
                        textureId, 0);
            } else if (tex.getType() == Type.CubeMap) {
                GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                        Lwjgl3TextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()), textureId,
                        0);
            } else {
                throw new IllegalArgumentException("Can not render to texture of type: " + tex.getType());
            }

            setDrawBuffer(GL11C.GL_NONE);
            setReadBuffer(GL11C.GL_NONE);
        } else {
            // Set color texture into FBO
            if (tex.getType() == Type.TwoDimensional) {
                GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D,
                        textureId, 0);
            } else if (tex.getType() == Type.CubeMap) {
                GL30C.glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                        Lwjgl3TextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()), textureId,
                        0);
            } else {
                throw new IllegalArgumentException("Can not render to texture of type: " + tex.getType());
            }

            // setup depth RB
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
        // automatically generate mipmaps for our texture.
        if (tex.getMinificationFilter().usesMipMapLevels()) {
            Lwjgl3TextureStateUtil.doTextureBind(tex, 0, true);
            GL30C.glGenerateMipmap(Lwjgl3TextureStateUtil.getGLType(tex.getType()));
        }
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

        GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, 0);
        GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, 0);
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
    }

    /**
     * Check the currently bound FBO status for completeness. The passed in fboID is for informational purposes only.
     *
     * @param fboID
     *            an id to use for log messages, particularly if there are any issues.
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
                throw new IllegalStateException("FrameBuffer: " + fboID
                        + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT exception");
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

    public void copyToTexture(final Texture tex, final int x, final int y, final int width, final int height,
            final int xoffset, final int yoffset) {
        Lwjgl3TextureStateUtil.doTextureBind(tex, 0, true);

        if (tex.getType() == Type.TwoDimensional) {
            GL11C.glCopyTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, xoffset, yoffset, x, y, width, height);
        } else if (tex.getType() == Type.CubeMap) {
            GL11C.glCopyTexSubImage2D(
                    Lwjgl3TextureStateUtil.getGLCubeMapFace(((TextureCubeMap) tex).getCurrentRTTFace()), 0, xoffset,
                    yoffset, x, y, width, height);
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
    public void activate() {
        // Lazy init
        if (_fboID == 0) {
            final IntBuffer buffer = BufferUtils.createIntBuffer(1);

            // Create our texture binding FBO
            GL30C.glGenFramebuffers(buffer); // generate id
            _fboID = buffer.get(0);

            // Create a depth renderbuffer to use for RTT use
            if (_depthBits != 0) {
                GL30C.glGenRenderbuffers(buffer); // generate id
                _depthRBID = buffer.get(0);
                GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, _depthRBID);
                GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, getDepthFormat(), _width, _height);
            }

            // unbind...
            GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);
            GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);

            // If we support it, rustle up a multisample framebuffer + renderbuffers
            if (_samples != 0 && _supportsMultisample) {
                // create ms framebuffer object
                GL30C.glGenFramebuffers(buffer);
                _msfboID = buffer.get(0);

                // create ms renderbuffers
                GL30C.glGenRenderbuffers(buffer); // generate id
                _mscolorRBID = buffer.get(0);
                GL30C.glGenRenderbuffers(buffer); // generate id
                _msdepthRBID = buffer.get(0);

                // set up renderbuffer properties
                GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, _mscolorRBID);
                GL30C.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, _samples, GL11C.GL_RGBA, _width, _height);

                if (_depthBits > 0) {
                    GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, _msdepthRBID);
                    GL30C.glRenderbufferStorageMultisample(GL30C.GL_RENDERBUFFER, _samples, getDepthFormat(), _width,
                            _height);
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
            ContextManager.getCurrentContext().pushEnforcedStates();
            ContextManager.getCurrentContext().clearEnforcedStates();
            ContextManager.getCurrentContext().enforceStates(_enforcedStates);
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

            if (_neededClip) {
                _parentRenderer.getScissorUtils().popClip();
            }
        }
        _active--;
    }

    public void cleanup() {
        if (_fboID != 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_fboID);
            id.rewind();
            GL30C.glDeleteFramebuffers(id);
        }

        if (_depthRBID != 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_depthRBID);
            id.rewind();
            GL30C.glDeleteRenderbuffers(id);
        }
    }
}
