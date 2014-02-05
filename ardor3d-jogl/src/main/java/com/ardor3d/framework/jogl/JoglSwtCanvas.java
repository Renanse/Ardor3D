/**
 * Copyright (c) 2008-2014 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import java.util.concurrent.CountDownLatch;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLRunnable;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.jogamp.opengl.swt.GLCanvas;

/**
 * Ardor3D JOGL SWT heavyweight canvas, SWT control for the OpenGL rendering of Ardor3D with JOGL that supports the SWT
 * input system directly and its abstraction in Ardor3D (com.ardor3d.input.swt)
 */
public class JoglSwtCanvas extends GLCanvas implements Canvas {

    private final JoglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;

    private final DisplaySettings _settings;

    private final JoglDrawerRunnable _drawerGLRunnable;

    private final JoglSwtInitializerRunnable _initializerRunnable;

    public JoglSwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer,
            final Composite composite, final int style) {
        this(settings, canvasRenderer, new CapsUtil(), composite, style);
    }

    public JoglSwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer,
            final CapsUtil capsUtil, final Composite composite, final int style) {
        super(composite, style, capsUtil.getCapsForSettings(settings), null);
        _drawerGLRunnable = new JoglDrawerRunnable(canvasRenderer);
        _initializerRunnable = new JoglSwtInitializerRunnable(this, settings);
        _settings = settings;
        _canvasRenderer = canvasRenderer;

        setFocus();
        setSize(_settings.getWidth(), _settings.getHeight());
        setAutoSwapBufferMode(false);
    }

    @Override
    @MainThread
    public void init() {
        if (_inited) {
            return;
        }

        // if we aren't on SWT user interface thread
        if (Display.getCurrent() == null) {
            Display.getDefault().syncExec(_initializerRunnable);
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

        if (isVisible()) {
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
