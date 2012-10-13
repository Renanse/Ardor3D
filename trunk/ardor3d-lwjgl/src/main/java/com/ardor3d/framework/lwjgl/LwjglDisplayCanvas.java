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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.PixelFormat;

import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.input.FocusWrapper;
import com.ardor3d.util.Ardor3dException;

/**
 * A Canvas implementation intended for use with an existing awt canvas.
 * <p>
 * XXX: Could/should this be merged with LwjglCanvas?
 * </p>
 */
public class LwjglDisplayCanvas implements Canvas, FocusWrapper {
    private static final Logger logger = Logger.getLogger(LwjglDisplayCanvas.class.getName());

    private final LwjglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;
    private final DisplaySettings _settings;

    private volatile boolean _focusLost = false;

    private final java.awt.Canvas _canvas;

    public LwjglDisplayCanvas(final java.awt.Canvas canvas, final DisplaySettings settings,
            final LwjglCanvasRenderer canvasRenderer) throws LWJGLException {
        _settings = settings;
        _canvasRenderer = canvasRenderer;
        _canvas = canvas;
        _canvasRenderer.setCanvasCallback(new LwjglCanvasCallback() {
            @Override
            public void makeCurrent() throws LWJGLException {
                if (!LwjglCanvas.SINGLE_THREADED_MODE) {
                    Display.makeCurrent();
                }
            }

            @Override
            public void releaseContext() throws LWJGLException {
                if (!LwjglCanvas.SINGLE_THREADED_MODE) {
                    Display.releaseContext();
                }
            }
        });
    }

    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            init();
        }

        checkFocus();

        _canvasRenderer.draw();

        if (latch != null) {
            latch.countDown();
        }
    }

    private void checkFocus() {
        // focusLost should be true if it is already true (hasn't been read/cleared yet), or
        // the display is presently not in focus
        _focusLost = _focusLost || !(Display.isActive() && Display.isVisible());
    }

    public boolean getAndClearFocusLost() {
        final boolean result = _focusLost;

        _focusLost = false;

        return result;
    }

    public void init() {
        if (_inited) {
            return;
        }

        // create the Display.
        final PixelFormat format = new PixelFormat(_settings.getAlphaBits(), _settings.getDepthBits(),
                _settings.getStencilBits()).withSamples(_settings.getSamples()).withStereo(_settings.isStereo());

        try {
            Display.setParent(_canvas);
            // NOTE: Workaround for possible lwjgl "pixel not accelerated" bug, as suggested by user "faust"
            try {
                Display.create(format);
            } catch (final LWJGLException e) {
                // failed to create Display, apply workaround (sleep for 1 second) and try again
                Thread.sleep(1000);
                Display.create(format);
            }
        } catch (final Exception e) {
            logger.severe("Cannot create window");
            logger.logp(Level.SEVERE, this.getClass().toString(), "initDisplay()", "Exception", e);
            throw new Ardor3dException("Cannot create window: " + e.getMessage());
        }

        _canvasRenderer.init(_settings, true); // true - do swap in renderer.
        _inited = true;
    }

    public LwjglCanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    public void setVSyncEnabled(final boolean enabled) {
        Display.setVSyncEnabled(enabled);
    }

    public void setFullScreen(final boolean fullScreen) throws LWJGLException {
        Display.setFullscreen(fullScreen);
    }

    public boolean isFullScreen() {
        return Display.isFullscreen();
    }
}
