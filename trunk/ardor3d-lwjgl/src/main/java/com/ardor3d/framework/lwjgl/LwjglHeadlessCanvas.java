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
import com.ardor3d.util.Constants;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <p>
 * A "canvas" class for use in drawing Scene data to an offscreen target. The data is read back after each call to draw
 * into a local IntBuffer for use.
 * </p>
 * 
 * <p>
 * Note: this class is not currently setup for use with other render contexts.
 * </p>
 */
public class LwjglHeadlessCanvas {

    private final Scene _scene;
    private final Renderer _renderer = new LwjglRenderer();
    protected final DisplaySettings _settings;
    protected Camera _camera;

    private final int _fboID, _depthRBID, _colorRBID;
    private final IntBuffer _data;
    private Pbuffer _buff;

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
        final int width = _settings.getWidth();
        final int height = _settings.getHeight();

        try {
            // Create a Pbuffer so we can have a valid gl context to work with
            final PixelFormat format = new PixelFormat(_settings.getAlphaBits(), _settings.getDepthBits(),
                    _settings.getStencilBits()).withSamples(_settings.getSamples());
            _buff = new Pbuffer(1, 1, format, null);
            _buff.makeCurrent();
        } catch (final LWJGLException ex) {
            try {
                // try again without samples
                final PixelFormat format = new PixelFormat(_settings.getAlphaBits(), _settings.getDepthBits(),
                        _settings.getStencilBits());
                _buff = new Pbuffer(1, 1, format, null);
                _buff.makeCurrent();
            } catch (final LWJGLException ex2) {
                ex2.printStackTrace();
            }
        }

        // Set up our Ardor3D context and capabilities objects
        final LwjglContextCapabilities caps = new LwjglContextCapabilities(GLContext.getCapabilities());
        final RenderContext currentContext = new RenderContext(this, caps, null);

        if (!caps.isFBOSupported()) {
            throw new Ardor3dException("Headless requires FBO support.");
        }

        // Init our FBO.
        final IntBuffer buffer = BufferUtils.createIntBuffer(1);
        EXTFramebufferObject.glGenFramebuffersEXT(buffer); // generate id

        // Bind the FBO
        _fboID = buffer.get(0);
        EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _fboID);

        // initial our color renderbuffer
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

        // Setup our data buffer for storing rendered image data.
        _data = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();

        // Add context to manager and set as active.
        ContextManager.addContext(this, currentContext);
        ContextManager.switchContext(this);

        // Turn on multisample if requested...
        if (_settings.getSamples() != 0 && caps.isMultisampleSupported()) {
            GL11.glEnable(ARBMultisample.GL_MULTISAMPLE_ARB);
        }

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

        if (Constants.useMultipleContexts) {
            // release our FBO until used.
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
        }
    }

    public void draw() {
        if (Constants.useMultipleContexts) {
            // activate FBO
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, _fboID);
        }

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

        // read data from our color buffer
        _data.rewind();
        GL11.glReadBuffer(EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT);
        GL11.glReadPixels(0, 0, _settings.getWidth(), _settings.getHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, _data);

        if (Constants.useMultipleContexts) {
            // release our FBO.
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, 0);
        }
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
