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
import javax.media.opengl.awt.GLCanvas;
import javax.swing.SwingUtilities;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;

/**
 * FIXME there is still a deadlock when using several instances of this class in the same container, see JOGL bug 572
 * Rather use JoglNewtAwtCanvas in this case.
 * 
 */
public class JoglAwtCanvas extends GLCanvas implements Canvas {

    private static final long serialVersionUID = 1L;

    private final JoglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;

    private final DisplaySettings _settings;

    private final JoglDrawerRunnable _drawerGLRunnable;

    public JoglAwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer) {
        super(CapsUtil.getCapsForSettings(settings));
        _drawerGLRunnable = new JoglDrawerRunnable(canvasRenderer);
        _settings = settings;
        _canvasRenderer = canvasRenderer;

        setFocusable(true);
        requestFocus();
        setSize(_settings.getWidth(), _settings.getHeight());
        setIgnoreRepaint(true);
        setAutoSwapBufferMode(false);
    }

    @MainThread
    public void init() {
        if (_inited) {
            return;
        }

        // Calling setVisible(true) on the GLCanvas not from the AWT-EDT can freeze the Intel GPU under Windows
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        // Make the window visible to realize the OpenGL surface.
                        setVisible(true);
                    }
                });
            } catch (final InterruptedException ex) {
                ex.printStackTrace();
            } catch (final InvocationTargetException ex) {
                ex.printStackTrace();
            }
        } else {
            // Make the window visible to realize the OpenGL surface.
            setVisible(true);
        }

        // Request the focus here as it cannot work when the window is not visible
        requestFocus();

        _canvasRenderer.setContext(getContext());

        invoke(true, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable glAutoDrawable) {
                _canvasRenderer.init(_settings, true);// true - do swap in renderer.
                return true;
            }
        });
        _inited = true;
    }

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

    public JoglCanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }
}
