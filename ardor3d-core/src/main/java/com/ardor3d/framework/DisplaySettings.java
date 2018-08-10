/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

public class DisplaySettings {
    private final int _width;
    private final int _height;
    private final int _colorDepth;
    private final int _frequency;
    private final int _alphaBits;
    private final int _depthBits;
    private final int _stencilBits;
    private final int _samples;
    private final boolean _fullScreen;
    private final boolean _stereo;
    private final CanvasRenderer _shareContext;

    /**
     * Convenience method equivalent to <code>DisplaySettings(width, height, 0, 0, 0, 8, 0, 0,
     * false, false, null)</code>
     *
     * @param width
     *            the canvas width
     * @param height
     *            the canvas height
     * @param depthBits
     *            the number of bits making up the z-buffer
     * @param samples
     *            the number of samples used to anti-alias
     * @see http://en.wikipedia.org/wiki/Z-buffering
     * @see http://en.wikipedia.org/wiki/Multisample_anti-aliasing
     */
    public DisplaySettings(final int width, final int height, final int depthBits, final int samples) {
        _width = width;
        _height = height;
        _colorDepth = 0;
        _frequency = 0;
        _alphaBits = 0;
        _depthBits = depthBits;
        _stencilBits = 0;
        _samples = samples;
        _fullScreen = false;
        _stereo = false;
        _shareContext = null;
    }

    /**
     * Convenience method equivalent to <code>DisplaySettings(width, height, colorDepth, frequency,
     * 0, 8, 0, 0, fullScreen, false, null)</code>
     *
     * @param width
     *            the canvas width
     * @param height
     *            the canvas height
     * @param colorDepth
     *            the number of color bits used to represent the color of a single pixel
     * @param frequency
     *            the number of times per second to repaint the canvas
     * @param fullScreen
     *            true if the canvas should assume exclusive access to the screen
     * @see http://en.wikipedia.org/wiki/Refresh_rate
     */
    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final boolean fullScreen) {
        _width = width;
        _height = height;
        _colorDepth = colorDepth;
        _frequency = frequency;
        _alphaBits = 0;
        _depthBits = 8;
        _stencilBits = 0;
        _samples = 0;
        _fullScreen = fullScreen;
        _stereo = false;
        _shareContext = null;
    }

    /**
     * Convenience method equivalent to <code>DisplaySettings(width, height, colorDepth, frequency,
     * alphaBits, depthBits, stencilBits, samples, fullScreen, stereo, null)</code>
     *
     * @param width
     *            the canvas width
     * @param height
     *            the canvas height
     * @param colorDepth
     *            the number of color bits used to represent the color of a single pixel
     * @param frequency
     *            the number of times per second to repaint the canvas
     * @param alphaBits
     *            the numner of bits used to represent the translucency of a single pixel
     * @param depthBits
     *            the number of bits making up the z-buffer
     * @param stencilBits
     *            the number of bits making up the stencil buffer
     * @param samples
     *            the number of samples used to anti-alias
     * @param fullScreen
     *            true if the canvas should assume exclusive access to the screen
     * @param stereo
     *            true if the canvas should be rendered stereoscopically (for 3D glasses)
     * @see http://en.wikipedia.org/wiki/Refresh_rate
     * @see http://en.wikipedia.org/wiki/Alpha_compositing
     * @see http://en.wikipedia.org/wiki/Stencil_buffer
     * @see http://en.wikipedia.org/wiki/Stereoscopy
     */
    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final int alphaBits, final int depthBits, final int stencilBits, final int samples,
            final boolean fullScreen, final boolean stereo) {
        _width = width;
        _height = height;
        _colorDepth = colorDepth;
        _frequency = frequency;
        _alphaBits = alphaBits;
        _depthBits = depthBits;
        _stencilBits = stencilBits;
        _samples = samples;
        _fullScreen = fullScreen;
        _stereo = stereo;
        _shareContext = null;
    }

    /**
     * Creates a new <code>DisplaySettings</code> object.
     *
     * @param width
     *            the canvas width
     * @param height
     *            the canvas height
     * @param colorDepth
     *            the number of color bits used to represent the color of a single pixel
     * @param frequency
     *            the number of times per second to repaint the canvas
     * @param alphaBits
     *            the numner of bits used to represent the translucency of a single pixel
     * @param depthBits
     *            the number of bits making up the z-buffer
     * @param stencilBits
     *            the number of bits making up the stencil buffer
     * @param samples
     *            the number of samples used to anti-alias
     * @param fullScreen
     *            true if the canvas should assume exclusive access to the screen
     * @param stereo
     *            true if the canvas should be rendered stereoscopically (for 3D glasses)
     * @param shareContext
     *            the renderer used to render the canvas (see "ardor3d.useMultipleContexts" property)
     * @see http://en.wikipedia.org/wiki/Z-buffering
     * @see http://en.wikipedia.org/wiki/Multisample_anti-aliasing
     * @see http://en.wikipedia.org/wiki/Refresh_rate
     * @see http://en.wikipedia.org/wiki/Alpha_compositing
     * @see http://en.wikipedia.org/wiki/Stencil_buffer
     * @see http://en.wikipedia.org/wiki/Stereoscopy
     * @see http://www.ardor3d.com/forums/viewtopic.php?f=13&t=318&p=2311&hilit=ardor3d.useMultipleContexts#p2311
     */
    public DisplaySettings(final int width, final int height, final int colorDepth, final int frequency,
            final int alphaBits, final int depthBits, final int stencilBits, final int samples,
            final boolean fullScreen, final boolean stereo, final CanvasRenderer shareContext) {
        _width = width;
        _height = height;
        _colorDepth = colorDepth;
        _frequency = frequency;
        _alphaBits = alphaBits;
        _depthBits = depthBits;
        _stencilBits = stencilBits;
        _samples = samples;
        _fullScreen = fullScreen;
        _stereo = stereo;
        _shareContext = shareContext;
    }

    public CanvasRenderer getShareContext() {
        return _shareContext;
    }

    public int getWidth() {
        return _width;
    }

    public int getHeight() {
        return _height;
    }

    public int getColorDepth() {
        return _colorDepth;
    }

    public int getFrequency() {
        return _frequency;
    }

    public int getAlphaBits() {
        return _alphaBits;
    }

    public int getDepthBits() {
        return _depthBits;
    }

    public int getStencilBits() {
        return _stencilBits;
    }

    public int getSamples() {
        return _samples;
    }

    public boolean isFullScreen() {
        return _fullScreen;
    }

    public boolean isStereo() {
        return _stereo;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DisplaySettings that = (DisplaySettings) o;

        return _colorDepth == that._colorDepth
                && _frequency == that._frequency
                && _fullScreen != that._fullScreen
                && _height != that._height
                && _width != that._width
                && _alphaBits != that._alphaBits
                && _depthBits != that._depthBits
                && _stencilBits != that._stencilBits
                && _samples != that._samples
                && _stereo != that._stereo
                && ((_shareContext == that._shareContext) || (_shareContext != null && _shareContext
                .equals(that._shareContext)));
    }

    @Override
    public int hashCode() {
        int result;
        result = 17;
        result = 31 * result + _height;
        result = 31 * result + _width;
        result = 31 * result + _colorDepth;
        result = 31 * result + _frequency;
        result = 31 * result + _alphaBits;
        result = 31 * result + _depthBits;
        result = 31 * result + _stencilBits;
        result = 31 * result + _samples;
        result = 31 * result + (_fullScreen ? 1 : 0);
        result = 31 * result + (_stereo ? 1 : 0);
        result = 31 * result + (_shareContext != null ? _shareContext.hashCode() : 0);
        return result;
    }

    public DisplaySettings resizedCopy(final int width, final int height) {
        return new DisplaySettings(width, height, _colorDepth, _frequency, _alphaBits, _depthBits, _stencilBits,
                _samples, _fullScreen, _stereo, _shareContext);
    }
}
