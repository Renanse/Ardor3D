/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.lwjgl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.EXTFramebufferBlit;
import org.lwjgl.opengl.EXTFramebufferMultisample;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.lwjgl.LwjglContextCapabilities;
import com.ardor3d.renderer.lwjgl.LwjglRenderer;
import com.ardor3d.renderer.lwjgl.LwjglTextureRenderer;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <p>
 * A "canvas" class for use in drawing Scene data to an off-screen target. The data is read back after each call to draw
 * into a local IntBuffer for use.
 * </p>
 * 
 * <p>
 * Note: this class is not currently setup for use with other render contexts.
 * </p>
 */
public class LwjglHeadlessCanvas {

    protected Scene _scene;
    protected Renderer _renderer = new LwjglRenderer();
    protected final DisplaySettings _settings;
    protected Camera _camera;

    protected int _fboID, _depthRBID, _colorRBID;
    protected int _msfboID, _msdepthRBID, _mscolorRBID;
    protected boolean _useMSAA = false;
    protected IntBuffer _data;
    protected Pbuffer _buff;

    /**
     * Construct a new LwjglHeadlessCanvas. Only width, height, alpha, depth and stencil are used. Samples will be
     * applied as well but may cause issues on some platforms.
     * 
     * @param settings
     *            the settings to use.
     * @param scene
     *            the scene we will render.
     */
    public LwjglHeadlessCanvas(final DisplaySettings settings, final Scene scene) {
        _scene = scene;
        _settings = settings;
        init();
    }

    protected void init() {
        final int width = _settings.getWidth();
        final int height = _settings.getHeight();

        try {
            // Create a Pbuffer so we can have a valid gl context to work with
            final PixelFormat format = new PixelFormat(_settings.getAlphaBits(), _settings.getDepthBits(),
                    _settings.getStencilBits());
            _buff = new Pbuffer(1, 1, format, null);
            _buff.makeCurrent();
        } catch (final LWJGLException ex) {
            ex.printStackTrace();
        }

        // Set up our Ardor3D context and capabilities objects
        final LwjglContextCapabilities caps = new LwjglContextCapabilities(GLContext.getCapabilities());
        final RenderContext currentContext = new RenderContext(this, caps, null);

        if (!caps.isFBOSupported()) {
            throw new Ardor3dException("Headless requires FBO support.");
        }

        if (caps.isFBOMultisampleSupported() && caps.isFBOBlitSupported() && _settings.getSamples() > 0) {
            _useMSAA = true;
        }

        // Init our FBO.
        final IntBuffer buffer = BufferUtils.createIntBuffer(1);
        EXTFramebufferObject.glGenFramebuffersEXT(buffer); // generate id

        // Bind the FBO
        _fboID = buffer.get(0);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _fboID);

        // initialize our color renderbuffer
        EXTFramebufferObject.glGenRenderbuffersEXT(buffer); // generate id
        _colorRBID = buffer.get(0);
        EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _colorRBID);
        EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, GL11.GL_RGBA, width,
                height);

        // Attach color renderbuffer to framebuffer
        EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, _colorRBID);

        // initialize our depth renderbuffer
        EXTFramebufferObject.glGenRenderbuffersEXT(buffer); // generate id
        _depthRBID = buffer.get(0);
        EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _depthRBID);
        EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                GL11.GL_DEPTH_COMPONENT, width, height);

        // Attach depth renderbuffer to framebuffer
        EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, _depthRBID);

        // Check FBO complete
        LwjglTextureRenderer.checkFBOComplete(_fboID);

        // Now do it all again for multisample, if requested and supported
        if (_useMSAA) {

            // Init our ms FBO.
            EXTFramebufferObject.glGenFramebuffersEXT(buffer); // generate id

            // Bind the ms FBO
            _msfboID = buffer.get(0);
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _msfboID);

            // initialize our ms color renderbuffer
            EXTFramebufferObject.glGenRenderbuffersEXT(buffer); // generate id
            _mscolorRBID = buffer.get(0);
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _mscolorRBID);
            EXTFramebufferMultisample.glRenderbufferStorageMultisampleEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    _settings.getSamples(), GL11.GL_RGBA, width, height);

            // Attach ms color renderbuffer to ms framebuffer
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    _mscolorRBID);

            // initialize our ms depth renderbuffer
            EXTFramebufferObject.glGenRenderbuffersEXT(buffer); // generate id
            _msdepthRBID = buffer.get(0);
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, _msdepthRBID);
            EXTFramebufferMultisample.glRenderbufferStorageMultisampleEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    _settings.getSamples(), GL11.GL_DEPTH_COMPONENT, width, height);

            // Attach ms depth renderbuffer to ms framebuffer
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT,
                    EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT,
                    _msdepthRBID);

            // Check MS FBO complete
            LwjglTextureRenderer.checkFBOComplete(_msfboID);

            // enable multisample
            GL11.glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);
        }

        // Setup our data buffer for storing rendered image data.
        _data = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();

        // Add context to manager and set as active.
        ContextManager.addContext(this, currentContext);
        ContextManager.switchContext(this);

        // Setup a default bg color.
        _renderer.setBackgroundColor(ColorRGBA.BLACK);

        // Setup a default camera
        _camera = new Camera(width, _settings.getHeight());
        _camera.setFrustumPerspective(45.0f, (float) width / (float) _settings.getHeight(), 1, 1000);
        _camera.setProjectionMode(ProjectionMode.Perspective);

        // setup camera orientation and position.
        final Vector3 loc = new Vector3(0.0f, 0.0f, 0.0f);
        final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
        final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
        final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
        _camera.setFrame(loc, left, up, dir);

        // release our FBO(s) until used.
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
    }

    public void draw() {
        // bind correct fbo
        EXTFramebufferObject
                .glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _useMSAA ? _msfboID : _fboID);

        // Make sure this OpenGL context is current.
        ContextManager.switchContext(this);
        try {
            _buff.makeCurrent();
        } catch (final LWJGLException ex) {
            ex.printStackTrace();
        }

        // make sure camera is set
        if (Camera.getCurrentCamera() != _camera) {
            _camera.update();
        }
        _camera.apply(_renderer);

        // clear buffers
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        _renderer.clearBuffers(Renderer.BUFFER_COLOR | Renderer.BUFFER_DEPTH);

        // draw our scene
        _scene.renderUnto(_renderer);
        _renderer.flushFrame(false);

        // if we're multisampled, we need to blit to a non-multisampled fbo first
        if (_useMSAA) {
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_DRAW_FRAMEBUFFER_EXT, _fboID);
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferBlit.GL_READ_FRAMEBUFFER_EXT, _msfboID);
            EXTFramebufferBlit.glBlitFramebufferEXT(0, 0, _settings.getWidth(), _settings.getHeight(), 0, 0,
                    _settings.getWidth(), _settings.getHeight(), GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                            | GL11.GL_STENCIL_BUFFER_BIT, GL11.GL_NEAREST);

            // get ready to read non-msaa fbo
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _fboID);
        }

        // read data from our color buffer
        _data.rewind();
        GL11.glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
        GL11.glReadPixels(0, 0, _settings.getWidth(), _settings.getHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, _data);

        // release our FBO.
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
    }

    public void releaseContext() throws LWJGLException {
        _buff.releaseContext();
    }

    public void cleanup() {
        if (_fboID != 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_fboID);
            id.rewind();
            EXTFramebufferObject.glDeleteFramebuffersEXT(id);
            _fboID = 0;
        }

        if (_depthRBID != 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_depthRBID);
            id.rewind();
            EXTFramebufferObject.glDeleteRenderbuffersEXT(id);
            _depthRBID = 0;
        }

        if (_colorRBID != 0) {
            final IntBuffer id = BufferUtils.createIntBuffer(1);
            id.put(_colorRBID);
            id.rewind();
            EXTFramebufferObject.glDeleteRenderbuffersEXT(id);
            _colorRBID = 0;
        }
        ContextManager.removeContext(this);
    }

    public IntBuffer getDataBuffer() {
        return _data;
    }

    public Renderer getRenderer() {
        return _renderer;
    }

    public Camera getCamera() {
        return _camera;
    }

    public void setCamera(final Camera camera) {
        _camera = camera;
    }
}
