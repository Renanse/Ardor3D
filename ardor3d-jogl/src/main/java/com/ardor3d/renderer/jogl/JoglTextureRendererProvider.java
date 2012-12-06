/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import java.util.logging.Logger;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererProvider;

public class JoglTextureRendererProvider implements TextureRendererProvider {

    private static final Logger logger = Logger.getLogger(JoglTextureRendererProvider.class.getName());

    public TextureRenderer createTextureRenderer(final int width, final int height, final Renderer renderer,
            final ContextCapabilities caps) {
        return createTextureRenderer(width, height, 0, 0, renderer, caps);
    }

    public TextureRenderer createTextureRenderer(final int width, final int height, final int depthBits,
            final int samples, final Renderer renderer, final ContextCapabilities caps) {
        return createTextureRenderer(new DisplaySettings(width, height, depthBits, samples), false, renderer, caps);
    }

    public TextureRenderer createTextureRenderer(final DisplaySettings settings, final boolean forcePbuffer,
            final Renderer renderer, final ContextCapabilities caps) {
        if (!forcePbuffer && caps.isFBOSupported()) {
            return new JoglTextureRenderer(settings.getWidth(), settings.getHeight(), settings.getDepthBits(), settings
                    .getSamples(), renderer, caps);
        } else if (caps.isPbufferSupported()) {
            return new JoglPbufferTextureRenderer(settings, renderer, caps);
        } else {
            logger.severe("No texture renderer support (FBO or Pbuffer).");
            return null;
        }

    }

}
