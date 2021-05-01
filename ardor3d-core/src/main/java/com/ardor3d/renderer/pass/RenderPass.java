/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.pass;

import java.util.List;

import com.ardor3d.image.Texture;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.Spatial;

/**
 * <code>RenderPass</code> renders the spatials attached to it as normal, including rendering the
 * renderqueue at the end of the pass.
 */
public class RenderPass extends Pass {

  private static final long serialVersionUID = 1L;

  @Override
  public void doRender(final Renderer r) {
    for (int i = 0, sSize = _spatials.size(); i < sSize; i++) {
      final Spatial s = _spatials.get(i);
      r.draw(s);
    }
    r.renderBuckets();
  }

  @Override
  public void doRender(final TextureRenderer r, final int clear, final List<Texture> texs) {
    r.renderSpatials(_spatials, texs, clear);
  }
}
