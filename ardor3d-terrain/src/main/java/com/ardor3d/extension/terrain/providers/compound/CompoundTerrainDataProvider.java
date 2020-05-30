/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.compound;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.terrain.client.TerrainConfiguration;
import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TextureSource;

public class CompoundTerrainDataProvider implements TerrainDataProvider {

  protected CompoundTerrainSource _source;
  protected List<TextureSource> _texSources = new ArrayList<>();

  public CompoundTerrainDataProvider(final TerrainConfiguration config) {
    _source = new CompoundTerrainSource(config);
  }

  @Override
  public CompoundTerrainSource getTerrainSource() { return _source; }

  @Override
  public List<TextureSource> getTextureSources() { return _texSources; }

  @Override
  public TextureSource getNormalMapSource() {
    // TODO Add support in the future
    return null;
  }

}
