/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.providers.inmemory;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.providers.image.ImageTextureSource;
import com.ardor3d.extension.terrain.providers.inmemory.data.InMemoryTerrainData;
import com.ardor3d.extension.terrain.util.NormalMapUtil;
import com.ardor3d.image.Image;

public class InMemoryTerrainDataProvider implements TerrainDataProvider {
  private static final int tileSize = 128;
  private final InMemoryTerrainData inMemoryTerrainData;

  private boolean generateNormalMap;

  public InMemoryTerrainDataProvider(final InMemoryTerrainData inMemoryTerrainData) {
    this(inMemoryTerrainData, false);
  }

  public InMemoryTerrainDataProvider(final InMemoryTerrainData inMemoryTerrainData, final boolean generateNormalMap) {
    this.inMemoryTerrainData = inMemoryTerrainData;
    this.generateNormalMap = generateNormalMap;
  }

  @Override
  public TerrainSource getTerrainSource() { return new InMemoryTerrainSource(tileSize, inMemoryTerrainData); }

  @Override
  public List<TextureSource> getTextureSources() {
    final List<TextureSource> rVal = new ArrayList<>(1);
    rVal.add(new InMemoryTextureSource(tileSize, inMemoryTerrainData));
    return rVal;
  }

  @Override
  public TextureSource getNormalMapSource() {
    if (generateNormalMap) {
      try {
        final Image normalImage = NormalMapUtil.constructNormalMap(inMemoryTerrainData.getHeightData(),
            inMemoryTerrainData.getSide(), inMemoryTerrainData.getMaxHeight(), inMemoryTerrainData.getScale().getX(),
            inMemoryTerrainData.getScale().getY());

        final List<Integer> heightMapSizes = new ArrayList<>();
        int currentSize = inMemoryTerrainData.getSide();
        heightMapSizes.add(currentSize);
        for (int i = 0; i < inMemoryTerrainData.getClipmapLevels(); i++) {
          currentSize /= 2;
          heightMapSizes.add(0, currentSize);
        }
        return new ImageTextureSource(tileSize, normalImage, heightMapSizes);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  public boolean isGenerateNormalMap() { return generateNormalMap; }

  public void setGenerateNormalMap(final boolean generateNormalMap) { this.generateNormalMap = generateNormalMap; }
}
