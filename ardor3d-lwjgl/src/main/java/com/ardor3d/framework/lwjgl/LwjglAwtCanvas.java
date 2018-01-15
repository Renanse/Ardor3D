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

import java.util.concurrent.CountDownLatch;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.PixelFormat;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.input.MouseManager;

public class LwjglAwtCanvas extends AWTGLCanvas implements Canvas {

    private static final long serialVersionUID = 1L;

    private final LwjglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;
    private final DisplaySettings _settings;

    private volatile boolean _updated = false;
    // _latch would have to be volatile if we are not careful with the order of reads and writes between this one and
    // '_updated'
    private CountDownLatch _latch = null;

    public LwjglAwtCanvas(final DisplaySettings settings, final LwjglCanvasRenderer canvasRenderer)
            throws LWJGLException {
        super(new PixelFormat(settings.getColorDepth(), settings.getAlphaBits(), settings.getDepthBits(),
                settings.getStencilBits(), settings.getSamples()).withStereo(settings.isStereo()));
        _settings = settings;
        _canvasRenderer = canvasRenderer;
        _canvasRenderer.setCanvasCallback(new LwjglCanvasCallback() {
            @Override
            public void makeCurrent() throws LWJGLException {
                LwjglAwtCanvas.this.makeCurrent();
            }

            @Override
            public void releaseContext() throws LWJGLException {
                LwjglAwtCanvas.this.releaseContext();
            }
        });
    }

    public void draw(final CountDownLatch latch) {
        if (!shouldDraw(latch)) {
            latch.countDown();
            return;
        }

        // need to set _latch before _updated, for memory consistency reasons
        _latch = latch;
        _updated = true;
        repaint();
    }

    private boolean shouldDraw(final CountDownLatch latch) {
        final boolean showing = isShowing();
        final boolean lastUpdateComplete = latch == null || !_updated;
        return showing && lastUpdateComplete;
    }

    @Override
    @MainThread
    protected void paintGL() {
        if (!_inited) {
            _canvasRenderer.init(_settings, false); // false - do not do back buffer swap, awt will do that.
            _canvasRenderer.getCamera().resize(getWidth(), getHeight());
            _inited = true;
        }

        if (_latch != null && !_updated) {
            return;
        }

        try {
            if (_canvasRenderer.draw()) {
                swapBuffers();
            }
        } catch (final LWJGLException e) {
            throw new RuntimeException(e);
        } finally {
            // release our context - because swap is external, we release here instead.
            _canvasRenderer.releaseCurrentContext();
        }

        if (_latch != null) {
            _updated = false;
            _latch.countDown();
        }
    }

    public void init() {
        ; // ignore - can only be inited inside our paintGL
    }

    public LwjglCanvasRenderer getCanvasRenderer() {
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

}
