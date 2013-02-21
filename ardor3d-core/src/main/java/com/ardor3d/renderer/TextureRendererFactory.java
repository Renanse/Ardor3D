/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import com.ardor3d.framework.DisplaySettings;

public enum TextureRendererFactory {

    INSTANCE;

    private TextureRendererProvider _provider = null;

    public void setProvider(final TextureRendererProvider provider) {
        _provider = provider;
    }

    /**
     * Create a TextureRenderer of the given width and height. All other params are considered undefined. We will
     * attempt to make an FBO based renderer if supported, or a Pbuffer based renderer if supported, or null if neither
     * are supported.
     * 
     * @param width
     *            the width of our off screen rendering target
     * @param height
     *            the height of our off screen rendering target
     * @param renderer
     *            the renderer to use when rendering to this off screen target.
     * @param caps
     *            the context capabilities, used for testing.
     * @return a TextureRenderer
     * @throws IllegalStateException
     *             if provider has not been set prior to calling this method.
     */
    public TextureRenderer createTextureRenderer(final int width, final int height, final Renderer renderer,
            final ContextCapabilities caps) {
        if (_provider == null) {
            throw new IllegalStateException("No provider has been set on TextureRendererFactory.");
        }
        return _provider.createTextureRenderer(width, height, renderer, caps);
    }

    /**
     * Create a TextureRenderer using params that are meaningful regardless of whether a Pbuffer or FBO renderer are
     * used. We will attempt to make an FBO based renderer if supported, or a Pbuffer based renderer if supported, or
     * null if neither are supported.
     * 
     * @param width
     *            the width of our off screen rendering target
     * @param height
     *            the height of our off screen rendering target
     * @param depthBits
     *            the desired depth buffer size of our off screen rendering target
     * @param samples
     *            the number of samples for our off screen rendering target
     * @param renderer
     *            the renderer to use when rendering to this off screen target.
     * @param caps
     *            the context capabilities, used for testing.
     * @return a TextureRenderer
     * @throws IllegalStateException
     *             if provider has not been set prior to calling this method.
     */
    public TextureRenderer createTextureRenderer(final int width, final int height, final int depthBits,
            final int samples, final Renderer renderer, final ContextCapabilities caps) {
        if (_provider == null) {
            throw new IllegalStateException("No provider has been set on TextureRendererFactory.");
        }
        return _provider.createTextureRenderer(width, height, depthBits, samples, renderer, caps);
    }

    /**
     * Create a TextureRenderer using as many of the given DisplaySettings that are meaningful for the chosen type.
     * Unless forcePbuffer is true, we will attempt to make an FBO based renderer if supported, or a Pbuffer based
     * renderer if supported, or null if neither are supported.
     * 
     * @param settings
     *            a complete set of possible display settings to use. Some will only be valid if Pbuffer is used.
     * @param forcePbuffer
     *            if true, we will return a pbuffer or null if pbuffers are not supported.
     * @param renderer
     *            the renderer to use when rendering to this off screen target.
     * @param caps
     *            the context capabilities, used for testing.
     * @return a TextureRenderer
     * @throws IllegalStateException
     *             if provider has not been set prior to calling this method.
     */
    public TextureRenderer createTextureRenderer(final DisplaySettings settings, final boolean forcePbuffer,
            final Renderer renderer, final ContextCapabilities caps) {
        if (_provider == null) {
            throw new IllegalStateException("No provider has been set on TextureRendererFactory.");
        }
        return _provider.createTextureRenderer(settings, forcePbuffer, renderer, caps);
    }
}
