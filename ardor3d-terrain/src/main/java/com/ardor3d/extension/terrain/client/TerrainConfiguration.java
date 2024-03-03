/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client;

import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Terrain Configuration data for a specific map.
 */
public class TerrainConfiguration {
  /** Total number of clipmap levels in this map */
  private final int totalNrClipmapLevels;
  /** "Tile size" for each tile in the cache */
  private final int cacheGridSize;
  /** Scale of one unit of terrain in meters */
  private final ReadOnlyVector3 scale;
  /** Minimum height value in the map */
  private final float heightRangeMin;
  /** Maximum height value in the map */
  private final float heightRangeMax;
  /** True if tiles are only valid in positive coordinates */
  private final boolean onlyPositiveQuadrant;

  public TerrainConfiguration(final int totalNrClipmapLevels, final int cacheGridSize, final ReadOnlyVector3 scale,
    final float heightRangeMin, final float heightRangeMax, final boolean onlyPositiveQuadrant) {
    this.totalNrClipmapLevels = totalNrClipmapLevels;
    this.cacheGridSize = cacheGridSize;
    this.scale = new Vector3(scale);
    this.heightRangeMin = heightRangeMin;
    this.heightRangeMax = heightRangeMax;
    this.onlyPositiveQuadrant = onlyPositiveQuadrant;
  }

  public int getCacheGridSize() { return cacheGridSize; }

  public float getHeightRangeMin() { return heightRangeMin; }

  public float getHeightRangeMax() { return heightRangeMax; }

  public ReadOnlyVector3 getScale() { return scale; }

  public int getTotalNrClipmapLevels() { return totalNrClipmapLevels; }

  public boolean isOnlyPositiveQuadrant() { return onlyPositiveQuadrant; }

  @Override
  public String toString() {
    return "TerrainConfiguration [cacheGridSize=" + cacheGridSize + ", heightRangeMax=" + heightRangeMax
        + ", heightRangeMin=" + heightRangeMin + ", onlyPositiveQuadrant=" + onlyPositiveQuadrant + ", scale=" + scale
        + ", totalNrClipmapLevels=" + totalNrClipmapLevels + "]";
  }
}
