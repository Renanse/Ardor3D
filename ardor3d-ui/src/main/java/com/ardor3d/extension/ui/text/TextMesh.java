/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.light.LightProperties;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.TextureCombineMode;

/**
 * Text spatial which uses textures generated by UIFont
 */
public class TextMesh extends Mesh {

  public TextMesh() {
    super("text");
    getMeshData().setIndexMode(IndexMode.Triangles);
    LightProperties.setLightReceiver(this, false);
    getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);

    // -- never cull
    setModelBound(null);
    getSceneHints().setCullHint(CullHint.Never);

    // -- default to non-pickable
    getSceneHints().setAllPickingHints(false);
  }
}
