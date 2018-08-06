
package com.ardor3d.renderer.lwjgl3;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.TextureRenderer;
import com.ardor3d.renderer.TextureRendererProvider;

public class Lwjgl3TextureRendererProvider implements TextureRendererProvider {

    public TextureRenderer createTextureRenderer(final int width, final int height, final Renderer renderer,
            final ContextCapabilities caps) {
        return createTextureRenderer(width, height, 0, 0, renderer, caps);
    }

    public TextureRenderer createTextureRenderer(final int width, final int height, final int depthBits,
            final int samples, final Renderer renderer, final ContextCapabilities caps) {
        return createTextureRenderer(new DisplaySettings(width, height, depthBits, samples), renderer, caps);
    }

    public TextureRenderer createTextureRenderer(final DisplaySettings settings, final Renderer renderer,
            final ContextCapabilities caps) {
        return new Lwjgl3TextureRenderer(settings.getWidth(), settings.getHeight(), settings.getDepthBits(),
                settings.getSamples(), renderer, caps);
    }
}
