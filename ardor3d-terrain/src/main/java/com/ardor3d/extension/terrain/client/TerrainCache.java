/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client;

import java.nio.FloatBuffer;

import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Fetches data from a source to the clipmap destination data through updateRegion.
 *
 */
public interface TerrainCache {
  /**
   * Tell the cache the current position so that it can start loading affected tiles
   *
   * @param x
   * @param y
   */
  void setCurrentPosition(int x, int y);

  /**
   * Returns the height at a given grid position. If the cache does not have a valid tile at this
   * position, we'll try our parent level cache.
   *
   * @param x
   *          local x position to get height from
   * @param z
   *          local z position to get height from
   * @return height at position
   */
  float getHeight(int x, int z, boolean tryParentCache);

  /**
   * Calculates, via LERP, the height at a given grid position. If the cache does not have a valid
   * tile at this position, we'll try our parent level cache.
   *
   * @param x
   *          local, fractional x position to interpolate data from.
   * @param z
   *          local, fractional z position to interpolate data from.
   * @param tryParentCache
   * @return height at position, linearly interpolated from the surrounding 4 grid points as pulled by
   *         getHeight.
   */
  float getSubHeight(float x, float z, boolean tryParentCache);

  /**
   * Update destinationData from cache in specified region
   *
   * @param destinationData
   * @param sourceX
   * @param sourceY
   * @param width
   * @param height
   */
  void updateRegion(FloatBuffer destinationData, int sourceX, int sourceY, int width, int height);

  void getEyeCoords(float[] destinationData, int sourceX, int sourceY, ReadOnlyVector3 eyePos);

  boolean isValid();

  void setMailBox(final DoubleBufferedList<Region> mailBox);

  /**
   * Ask our cache to check for tiles that have gone bad. These should get submitted to the mailbox as
   * regions.
   */
  void checkForInvalidatedRegions();

  void checkForUpdates();

  void regenerate();

}
