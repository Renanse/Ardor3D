/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.assimp;

import java.util.HashMap;
import java.util.Map;

import com.ardor3d.image.Texture;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.surface.ColorSurface;

public class ModelDataStore {
  private Node _scene;

  public Map<Integer, ColorSurface> materialSurfaces = new HashMap<>();
  public Map<Integer, Texture> materialDiffuseTexs = new HashMap<>();
  public Map<Integer, Map<RenderState.StateType, RenderState>> renderStateMap = new HashMap<>();

  public Node getScene() { return _scene; }

  public void setScene(final Node scene) { _scene = scene; }

  public void addRenderState(final int materialIndex, final RenderState toAdd) {
    Map<StateType, RenderState> stateMap = renderStateMap.getOrDefault(materialIndex, null);
    if (stateMap == null) {
      stateMap = new HashMap<>();
      renderStateMap.put(materialIndex, stateMap);
    }

    stateMap.put(toAdd.getType(), toAdd);
  }
}
