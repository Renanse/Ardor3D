/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.light.shadow;

import com.ardor3d.light.PointLight;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.SceneIndexer;

public class PointShadowData extends AbstractShadowData {

  // Tracked light
  protected final PointLight _light;

  public PointShadowData(final PointLight pointLight) {
    super();
    _light = pointLight;
  }

  @Override
  public void updateShadows(final Renderer renderer, final SceneIndexer indexer) {
    // FIXME: render to cube, but as 6 slices in texture array
    final var shadowRenderer = getShadowRenderer(_light, 6, renderer);
    shadowRenderer.render(indexer, _texture, Renderer.BUFFER_DEPTH);
  }
}
