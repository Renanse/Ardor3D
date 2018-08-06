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

public interface TextureRendererProvider {

    /**
     * @see TextureRendererFactory#createTextureRenderer(int, int, Renderer, ContextCapabilities)
     */
    TextureRenderer createTextureRenderer(int width, int height, Renderer renderer, ContextCapabilities caps);

    /**
     * @see TextureRendererFactory#createTextureRenderer(int, int, int, int, Renderer, ContextCapabilities)
     */
    TextureRenderer createTextureRenderer(int width, int height, int depthBits, int samples, Renderer renderer,
            ContextCapabilities caps);

    /**
     * @see TextureRendererFactory#createTextureRenderer(DisplaySettings, Renderer, ContextCapabilities)
     */
    TextureRenderer createTextureRenderer(DisplaySettings settings, Renderer renderer, ContextCapabilities caps);

}
