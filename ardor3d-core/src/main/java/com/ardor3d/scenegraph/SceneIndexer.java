/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph;

import com.ardor3d.light.LightManager;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.event.DirtyEventListener;
import com.ardor3d.scenegraph.event.DirtyType;

public class SceneIndexer implements DirtyEventListener {

  protected LightManager _lightManager = new LightManager();

  public static SceneIndexer getCurrent() {
    final RenderContext context = ContextManager.getCurrentContext();
    if (context == null) {
      return null;
    }
    return context.getSceneIndexer();
  }

  public LightManager getLightManager() { return _lightManager; }

  public void setLightManager(final LightManager manager) { _lightManager = manager; }

  public void onRender(final Renderer renderer) {
    if (_lightManager != null) {
      _lightManager.cleanLights();
      _lightManager.renderShadowMaps(renderer);
    }

  }

  public void addSceneRoot(final Spatial spat) {
    spat.addListener(this);
    addToIndex(spat);
  }

  public void removeSceneRoot(final Spatial spat) {
    spat.removeListener(this);
    removeFromIndex(spat);
  }

  protected void addToIndex(final Spatial spat) {
    if (_lightManager != null) {
      _lightManager.addLights(spat);
    }
  }

  protected void removeFromIndex(final Spatial spat) {
    if (_lightManager != null) {
      _lightManager.removeLights(spat);
    }
  }

  @Override
  public boolean spatialClean(final Spatial spatial, final DirtyType dirtyType) {
    // nothing to do here - we only care about dirty
    return false;
  }

  @Override
  public boolean spatialDirty(final Spatial caller, final DirtyType dirtyType) {
    if (dirtyType == DirtyType.Attached) {
      addToIndex(caller);
    } else if (dirtyType == DirtyType.Detached) {
      removeFromIndex(caller);
    }
    return false;
  }

}
