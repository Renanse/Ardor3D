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
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.NativeCanvas;
import com.ardor3d.image.Image;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.input.FocusWrapper;
import com.ardor3d.input.MouseManager;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.BufferUtils;

/**
 * A canvas implementation for use with native LWJGL windows.
 */
public class LwjglCanvas implements NativeCanvas, FocusWrapper {
    private static final Logger logger = Logger.getLogger(LwjglCanvas.class.getName());

    private final LwjglCanvasRenderer _canvasRenderer;

    private final DisplaySettings _settings;
    private boolean _inited = false;

    private volatile boolean _focusLost = false;

    /**
     * If true, we will not try to drop and reclaim the context on each frame.
     */
    public static boolean SINGLE_THREADED_MODE = true;

    public LwjglCanvas(final DisplaySettings settings, final LwjglCanvasRenderer canvasRenderer) {
        _canvasRenderer = canvasRenderer;
        _canvasRenderer.setCanvasCallback(new LwjglCanvasCallback() {
            @Override
            public void makeCurrent() throws LWJGLException {
                if (!SINGLE_THREADED_MODE) {
                    Display.makeCurrent();
                }
            }

            @Override
            public void releaseContext() throws LWJGLException {
                if (!SINGLE_THREADED_MODE) {
                    Display.releaseContext();
                }
            }
        });
        _settings = settings;
    }

    public boolean getAndClearFocusLost() {
        final boolean result = _focusLost;

        _focusLost = false;

        return result;
    }

    @MainThread
    // hm, seem to have to control the order of initialization: display first, then keyboard/etc, in native windows
    public void init() {
        if (_inited) {
            return;
        }

        // create the Display.
        DisplayMode mode;
        if (_settings.isFullScreen()) {
            mode = getValidDisplayMode(_settings);
            if (null == mode) {
                throw new Ardor3dException("Bad display mode (w/h/bpp/freq): " + _settings.getWidth() + " / "
                        + _settings.getHeight() + " / " + _settings.getColorDepth() + " / " + _settings.getFrequency());
            }
        } else {
            mode = new DisplayMode(_settings.getWidth(), _settings.getHeight());
        }

        final PixelFormat format = new PixelFormat(_settings.getAlphaBits(), _settings.getDepthBits(),
                _settings.getStencilBits()).withSamples(_settings.getSamples()).withStereo(_settings.isStereo());

        try {
            Display.setDisplayMode(mode);
            Display.setFullscreen(_settings.isFullScreen());
            Display.create(format);
        } catch (final Exception e) {
            logger.severe("Cannot create window");
            logger.logp(Level.SEVERE, this.getClass().toString(), "initDisplay()", "Exception", e);
            throw new Ardor3dException("Cannot create window: " + e.getMessage());
        }

        _canvasRenderer.init(_settings, true); // true - do swap in renderer.
        _inited = true;
    }

    @MainThread
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

        //
        // final boolean newFocus =
        //
        // if (!focusLost && newFocus) {
        // // didn't use to have focus, but now we do
        // // do nothing for now, just keep track of the fact that we have focus
        // focusLost = newFocus;
        // } else if (focusLost && !newFocus) {
        // // had focus, but don't anymore - notify the physical input layer
        // physicalLayer.lostFocus();
        // focusLost = newFocus;
        // }
    }

    public CanvasRenderer getCanvasRenderer() {
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

    /**
     * @return a <code>DisplayMode</code> object that has the requested settings. If there is no mode that supports a
     *         requested resolution, null is returned.
     */
    private DisplayMode getValidDisplayMode(final DisplaySettings settings) {
        // get all the modes, and find one that matches our settings.
        DisplayMode[] modes;
        try {
            modes = Display.getAvailableDisplayModes();
        } catch (final LWJGLException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "getValidDisplayMode(width, height, bpp, freq)",
                    "Exception", e);
            return null;
        }

        // Try to find a best match.
        int best_match = -1; // looking for request size/bpp followed by exact or highest freq
        int match_freq = -1;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].getWidth() != settings.getWidth()) {
                logger.fine("DisplayMode " + modes[i] + ": Width != " + settings.getWidth());
                continue;
            }
            if (modes[i].getHeight() != settings.getHeight()) {
                logger.fine("DisplayMode " + modes[i] + ": Height != " + settings.getHeight());
                continue;
            }
            if (settings.getColorDepth() != 0 && modes[i].getBitsPerPixel() != settings.getColorDepth()) {
                // should pick based on best match here too
                logger.fine("DisplayMode " + modes[i] + ": Bits per pixel != " + settings.getColorDepth());
                continue;
            }
            if (best_match == -1) {
                logger.fine("DisplayMode " + modes[i] + ": Match! ");
                best_match = i;
                match_freq = modes[i].getFrequency();
            } else {
                final int cur_freq = modes[i].getFrequency();
                if (match_freq != settings.getFrequency() && // Previous is not a perfect match
                        (cur_freq == settings.getFrequency() || // Current is perfect match
                        match_freq < cur_freq)) // or is higher freq
                {
                    logger.fine("DisplayMode " + modes[i] + ": Better match!");
                    best_match = i;
                    match_freq = cur_freq;
                }
            }
        }

        if (best_match == -1) {
            return null; // none found;
        } else {
            logger.info("Selected DisplayMode: " + modes[best_match]);
            return modes[best_match];
        }
    }

    public void close() {
        if (Display.isCreated()) {
            Display.destroy();
        }
    }

    public boolean isActive() {
        return Display.isCreated() && Display.isActive();
    }

    @MainThread
    public boolean isClosing() {
        return Display.isCreated() && Display.isCloseRequested();
    }

    public void moveWindowTo(final int locX, final int locY) {
        if (Display.isCreated()) {
            Display.setLocation(locX, locY);
        }
    }

    public void setIcon(final Image[] iconImages) {
        final ByteBuffer[] iconData = new ByteBuffer[iconImages.length];
        for (int i = 0; i < iconData.length; i++) {
            // Image.Format.RGBA8 is the format that LWJGL requires, so try to convert if it's not.
            if (iconImages[i].getDataType() != PixelDataType.UnsignedByte) {
                throw new Ardor3dException(
                        "Your icon is in a format that could not be converted to UnsignedByte - RGBA");
            }

            if (iconImages[i].getDataFormat() != ImageDataFormat.RGBA) {
                if (iconImages[i].getDataFormat() != ImageDataFormat.RGB) {
                    throw new Ardor3dException(
                            "Your icon is in a format that could not be converted to UnsignedByte - RGBA");
                }
                iconImages[i] = _RGB888_to_RGBA8888(iconImages[i]);
            }

            iconData[i] = iconImages[i].getData(0);
            iconData[i].rewind();
        }
        Display.setIcon(iconData);
    }

    private static Image _RGB888_to_RGBA8888(final Image rgb888) {
        final int size = rgb888.getWidth() * rgb888.getHeight() * 4;

        final ByteBuffer rgb = rgb888.getData(0);

        final ByteBuffer rgba8888 = BufferUtils.createByteBuffer(size);
        rgb.rewind();
        for (int j = 0; j < size; j++) {
            if ((j + 1) % 4 == 0) {
                rgba8888.put((byte) 0xFF);
            } else {
                rgba8888.put(rgb.get());
            }
        }
        return new Image(ImageDataFormat.RGBA, PixelDataType.UnsignedByte, rgb888.getWidth(), rgb888.getHeight(),
                rgba8888, null);
    }

    public void setTitle(final String title) {
        Display.setTitle(title);
    }

    public void setVSyncEnabled(final boolean enabled) {
        Display.setVSyncEnabled(enabled);
    }
}
