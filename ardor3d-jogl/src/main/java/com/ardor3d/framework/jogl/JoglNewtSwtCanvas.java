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

import java.util.concurrent.CountDownLatch;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLRunnable;

import org.eclipse.swt.widgets.Composite;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.input.MouseManager;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;

/**
 * Ardor3D JOGL SWT lightweight canvas, SWT control for the OpenGL rendering of Ardor3D with JOGL that supports both
 * NEWT and SWT input systems directly and their abstractions in Ardor3D (com.ardor3d.input.jogl and
 * com.ardor3d.input.swt)
 */
public class JoglNewtSwtCanvas extends NewtCanvasSWT implements Canvas, NewtWindowContainer {

    private final JoglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;

    private final DisplaySettings _settings;

    private final JoglDrawerRunnable _drawerGLRunnable;

    public JoglNewtSwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer,
            final Composite composite, final int style) {
        this(settings, canvasRenderer, new CapsUtil(), composite, style);
    }

    public JoglNewtSwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer,
            final CapsUtil capsUtil, final Composite composite, final int style) {
        super(composite, style, GLWindow.create(capsUtil.getCapsForSettings(settings)));
        _drawerGLRunnable = new JoglDrawerRunnable(canvasRenderer);
        getNewtWindow().setUndecorated(true);
        _settings = settings;
        _canvasRenderer = canvasRenderer;

        setSize(_settings.getWidth(), _settings.getHeight());
        getNewtWindow().setAutoSwapBufferMode(false);
    }

    @Override
    @MainThread
    public void init() {
        if (_inited) {
            return;
        }

        // Make the window visible to realize the OpenGL surface.
        setVisible(true);
        if (getNewtWindow().isRealized()) {
            _canvasRenderer.setContext(getNewtWindow().getContext());
            getNewtWindow().invoke(true, new GLRunnable() {
                @Override
                public boolean run(final GLAutoDrawable glAutoDrawable) {
                    _canvasRenderer.init(_settings, true);// true - do swap in renderer.
                    return true;
                }
            });
            _inited = true;
        }
    }

    @Override
    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            init();
        }

        if (isVisible()) {
            getNewtWindow().invoke(true, _drawerGLRunnable);
        }
        if (latch != null) {
            latch.countDown();
        }
    }

    @Override
    public JoglCanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    protected MouseManager _manager;

    @Override
    public MouseManager getMouseManager() {
        return _manager;
    }

    @Override
    public void setMouseManager(final MouseManager manager) {
        _manager = manager;
    }

    @Override
    public GLWindow getNewtWindow() {
        return (GLWindow) getNEWTChild();
    }

    public void setVSyncEnabled(final boolean enabled) {
        getNewtWindow().invoke(true, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable glAutoDrawable) {
                glAutoDrawable.getGL().setSwapInterval(enabled ? 1 : 0);
                return false;
            }
        });
    }

}
