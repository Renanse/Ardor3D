/**
 * Copyright (c) 2008-2010 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import java.util.logging.Logger;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.DebugGL3;
import javax.media.opengl.DebugGL3bc;
import javax.media.opengl.DebugGL4;
import javax.media.opengl.DebugGL4bc;
import javax.media.opengl.DebugGLES1;
import javax.media.opengl.DebugGLES2;
import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.jogl.JoglContextCapabilities;
import com.ardor3d.renderer.jogl.JoglRenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.jogl.DirectNioBuffersSet;

public class JoglCanvasRenderer implements CanvasRenderer {

    private static final Logger LOGGER = Logger.getLogger(JoglCanvasRenderer.class.getName());

    protected Scene _scene;
    protected Camera _camera;
    protected boolean _doSwap;
    protected GLContext _context;
    protected JoglRenderer _renderer;
    protected int _frameClear = Renderer.BUFFER_COLOR_AND_DEPTH;

    private JoglRenderContext _currentContext;

    /**
     * <code>true</code> if debugging (checking for error codes on each GL call) is desired.
     */
    private final boolean _useDebug;

    /**
     * <code>true</code> if debugging is currently enabled for this GLContext.
     */
    private boolean _debugEnabled = false;

    protected CapsUtil _capsUtil;

    protected DirectNioBuffersSet _directNioBuffersSet;

    public JoglCanvasRenderer(final Scene scene) {
        this(scene, false, new CapsUtil());
    }

    public JoglCanvasRenderer(final Scene scene, final boolean useDebug, final CapsUtil capsUtil) {
        _scene = scene;
        _useDebug = useDebug;
        _capsUtil = capsUtil;
    }

    @Override
    public void makeCurrentContext() throws Ardor3dException {
        int value = GLContext.CONTEXT_NOT_CURRENT;
        int attempt = 0;
        do {
            try {
                value = _context.makeCurrent();
            } catch (final GLException gle) {
                gle.printStackTrace();
            } finally {
                attempt++;
                if (attempt == MAX_CONTEXT_GRAB_ATTEMPTS) {
                    // failed, throw exception
                    throw new Ardor3dException("Failed to claim OpenGL context.");
                }
            }
            try {
                Thread.sleep(5);
            } catch (final InterruptedException e1) {
                e1.printStackTrace();
            }
        } while (value == GLContext.CONTEXT_NOT_CURRENT);
        if (ContextManager.getCurrentContext() != null) {
            if (value == GLContext.CONTEXT_CURRENT_NEW) {
                ContextManager.getCurrentContext().contextLost();

                // Whenever the context is created or replaced, the GL chain
                // is lost. Debug will have to be added if desired.
                _debugEnabled = false;
            }

            if (ContextManager.getContextForKey(_context) != null) {
                ContextManager.switchContext(_context);
            }
        }
    }

    @Override
    public void releaseCurrentContext() {
        if (_context.equals(GLContext.getCurrent())) {
            try {
                _context.release();
            } catch (final GLException gle) {
                gle.printStackTrace();
            }
        }
    }

    @MainThread
    protected JoglContextCapabilities createContextCapabilities() {
        return new JoglContextCapabilities(_context.getGL(), _directNioBuffersSet);
    }

    @Override
    public JoglRenderer createRenderer() {
        return new JoglRenderer();
    }

    @Override
    @MainThread
    public void init(final DisplaySettings settings, final boolean doSwap) {
        _doSwap = doSwap;
        if (_context == null) {
            _context = GLDrawableFactory.getFactory(_capsUtil.getProfile()).createExternalGLContext();
        }

        makeCurrentContext();

        if (_directNioBuffersSet == null) {
            _directNioBuffersSet = new DirectNioBuffersSet();
        }

        try {

            // Look up a shared context, if a shared JoglCanvasRenderer is given.
            RenderContext sharedContext = null;
            if (settings.getShareContext() != null) {
                sharedContext = ContextManager
                        .getContextForKey(settings.getShareContext().getRenderContext().getContextKey());
            }

            final ContextCapabilities caps = createContextCapabilities();
            _currentContext = new JoglRenderContext(_context, caps, sharedContext, _directNioBuffersSet);

            ContextManager.addContext(_context, _currentContext);
            ContextManager.switchContext(_context);

            _renderer = createRenderer();

            if (settings.getSamples() != 0) {
                final GL gl = GLContext.getCurrentGL();
                gl.glEnable(GL.GL_MULTISAMPLE);
            }

            _renderer.setBackgroundColor(ColorRGBA.BLACK);

            if (_camera == null) {
                /** Set up how our camera sees. */
                _camera = new Camera(settings.getWidth(), settings.getHeight());
                _camera.setFrustumPerspective(45.0f, (float) settings.getWidth() / (float) settings.getHeight(), 1,
                        1000);
                _camera.setProjectionMode(ProjectionMode.Perspective);

                final Vector3 loc = new Vector3(0.0f, 0.0f, 10.0f);
                final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
                final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
                final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
                /** Move our camera to a correct place and orientation. */
                _camera.setFrame(loc, left, up, dir);
            } else {
                // use new width and height to set ratio.
                _camera.setFrustumPerspective(_camera.getFovY(),
                        (float) settings.getWidth() / (float) settings.getHeight(), _camera.getFrustumNear(),
                        _camera.getFrustumFar());
            }
        } finally {
            releaseCurrentContext();
        }
    }

    public GLContext getContext() {
        return _context;
    }

    public void setContext(final GLContext context) {
        _context = context;
    }

    public int MAX_CONTEXT_GRAB_ATTEMPTS = 10;

    @Override
    @MainThread
    public boolean draw() {

        // set up context for rendering this canvas
        makeCurrentContext();

        // Enable Debugging if requested.
        if (_useDebug != _debugEnabled) {
            if (_context.getGL().isGLES()) {
                if (_context.getGL().isGLES1()) {
                    _context.setGL(new DebugGLES1(_context.getGL().getGLES1()));
                } else {
                    if (_context.getGL().isGLES2()) {
                        _context.setGL(new DebugGLES2(_context.getGL().getGLES2()));
                    } else {
                        // TODO ES3
                    }
                }
            } else {
                if (_context.getGL().isGL4bc()) {
                    _context.setGL(new DebugGL4bc(_context.getGL().getGL4bc()));
                } else {
                    if (_context.getGL().isGL4()) {
                        _context.setGL(new DebugGL4(_context.getGL().getGL4()));
                    } else {
                        if (_context.getGL().isGL3bc()) {
                            _context.setGL(new DebugGL3bc(_context.getGL().getGL3bc()));
                        } else {
                            if (_context.getGL().isGL3()) {
                                _context.setGL(new DebugGL3(_context.getGL().getGL3()));
                            } else {
                                if (_context.getGL().isGL2()) {
                                    _context.setGL(new DebugGL2(_context.getGL().getGL2()));
                                }
                            }
                        }
                    }
                }
            }
            _debugEnabled = true;

            LOGGER.info("DebugGL Enabled");
        }

        // render stuff, first apply our camera if we have one
        if (_camera != null) {
            _camera.apply(_renderer);
        }
        _renderer.clearBuffers(_frameClear);

        final boolean drew = _scene.renderUnto(_renderer);
        _renderer.flushFrame(drew && _doSwap);

        // release the context if we're done (swapped and all)
        if (_doSwap) {
            releaseCurrentContext();
        }

        return drew;
    }

    @Override
    public Camera getCamera() {
        return _camera;
    }

    @Override
    public Scene getScene() {
        return _scene;
    }

    @Override
    public void setScene(final Scene scene) {
        _scene = scene;
    }

    @Override
    public Renderer getRenderer() {
        return _renderer;
    }

    @Override
    public void setCamera(final Camera camera) {
        _camera = camera;
    }

    @Override
    public JoglRenderContext getRenderContext() {
        return _currentContext;
    }

    @Override
    public int getFrameClear() {
        return _frameClear;
    }

    @Override
    public void setFrameClear(final int buffers) {
        _frameClear = buffers;
    }
}
