/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLRunnable;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.SwingUtilities;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;

/**
 * Ardor3D JOGL Swing lightweight canvas, Swing component for the OpenGL rendering of Ardor3D with JOGL that supports
 * the AWT input system directly and its abstraction in Ardor3D (com.ardor3d.input.awt). As this canvas is generally
 * slower and heavier (in term of memory footprint) than JoglAwtCanvas, use it if and only if you have some problems
 * when mixing heavyweight and lightweight components
 * 
 */
public class JoglSwingCanvas extends GLJPanel implements Canvas {

    private static final long serialVersionUID = 1L;

    private final JoglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;

    private final DisplaySettings _settings;

    private final JoglDrawerRunnable _drawerGLRunnable;

    private final JoglSwingInitializerRunnable _initializerRunnable;

    public JoglSwingCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer) {
        this(settings, canvasRenderer, new CapsUtil());
    }

    public JoglSwingCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer,
            final CapsUtil capsUtil) {
        super(capsUtil.getCapsForSettings(settings));
        _drawerGLRunnable = new JoglDrawerRunnable(canvasRenderer);
        _initializerRunnable = new JoglSwingInitializerRunnable(this, settings);
        _settings = settings;
        _canvasRenderer = canvasRenderer;

        setFocusable(true);
        requestFocus();
        setSize(_settings.getWidth(), _settings.getHeight());
        setIgnoreRepaint(true);
        setAutoSwapBufferMode(false);
    }

    @Override
    @MainThread
    public void init() {
        if (_inited) {
            return;
        }

        // Calling setVisible(true) on the GLCanvas not from the AWT-EDT can freeze the Intel GPU under Windows
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(_initializerRunnable);
            } catch (final InterruptedException ex) {
                ex.printStackTrace();
            } catch (final InvocationTargetException ex) {
                ex.printStackTrace();
            }
        } else {
            _initializerRunnable.run();
        }

        _inited = isRealized();
    }

    @Override
    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            init();
        }

        if (isShowing()) {
            invoke(true, _drawerGLRunnable);
        }
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public JoglCanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    public void setVSyncEnabled(final boolean enabled) {
        invoke(true, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable glAutoDrawable) {
                glAutoDrawable.getGL().setSwapInterval(enabled ? 1 : 0);
                return false;
            }
        });
    }
}
