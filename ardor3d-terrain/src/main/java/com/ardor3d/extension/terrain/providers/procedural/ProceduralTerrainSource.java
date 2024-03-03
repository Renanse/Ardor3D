/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.providers.procedural;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.ardor3d.extension.terrain.client.TerrainConfiguration;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.type.ReadOnlyVector3;

public class ProceduralTerrainSource implements TerrainSource {
  private final Function3D function;
  private final ReadOnlyVector3 scale;
  private final float minHeight;
  private final float maxHeight;
  private final boolean[] _invalidLevels;

  private static final int tileSize = 128;
  private static final int availableClipmapLevels = 8;

  private final ReentrantLock terrainLock = new ReentrantLock();
  private final ThreadLocal<float[]> tileDataPool = ThreadLocal.withInitial(() -> new float[tileSize * tileSize]);

  public ProceduralTerrainSource(final Function3D function, final ReadOnlyVector3 scale, final float minHeight,
    final float maxHeight) {
    this.function = function;
    this.scale = scale;
    this.minHeight = minHeight;
    this.maxHeight = maxHeight;
    _invalidLevels = new boolean[availableClipmapLevels];
  }

  @Override
  public TerrainConfiguration getConfiguration() throws Exception {
    return new TerrainConfiguration(availableClipmapLevels, tileSize, getScale(), getMinHeight(), getMaxHeight(),
        false);
  }

  @Override
  public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
      final int numTilesY) throws Exception {
    return null;
  }

  @Override
  public Set<Tile> getInvalidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
      final int numTilesY) throws Exception {
    if (clipmapLevel < 0 || clipmapLevel >= availableClipmapLevels) {
      return null;
    }

    synchronized (_invalidLevels) {
      if (_invalidLevels[clipmapLevel]) {
        _invalidLevels[clipmapLevel] = false;
        final Set<Tile> rVal = new HashSet<>();
        for (int y = 0; y < numTilesY; y++) {
          for (int x = 0; x < numTilesX; x++) {
            rVal.add(new Tile(tileX + x, tileY + y));
          }
        }
        return rVal;
      }
      return null;
    }
  }

  @Override
  public float[] getTile(final int clipmapLevel, final Tile tile) throws Exception {
    final float[] data = tileDataPool.get();
    final int tileX = tile.getX();
    final int tileY = tile.getY();

    final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

    terrainLock.lock();
    try {
      for (int y = 0; y < tileSize; y++) {
        for (int x = 0; x < tileSize; x++) {
          if (Thread.interrupted()) {
            return null;
          }

          final int heightX = tileX * tileSize + x;
          final int heightY = tileY * tileSize + y;

          final int index = x + y * tileSize;
          data[index] = (float) getFunction().eval(heightX << baseClipmapLevel, heightY << baseClipmapLevel, 0);
        }
      }
    } finally {
      terrainLock.unlock();
    }
    return data;
  }

  public void markInvalid() {
    synchronized (_invalidLevels) {
      Arrays.fill(_invalidLevels, true);
    }
  }

  public Function3D getFunction() { return function; }

  public ReadOnlyVector3 getScale() { return scale; }

  public float getMinHeight() { return minHeight; }

  public float getMaxHeight() { return maxHeight; }
}
