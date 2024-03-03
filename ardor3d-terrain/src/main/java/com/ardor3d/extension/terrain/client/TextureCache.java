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

import java.nio.ByteBuffer;

import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.Region;

/**
 * Fetches data from a source to the texture clipmap destination data through updateRegion.
 *
 */
public interface TextureCache {
  void setCurrentPosition(final int x, final int y);

  int getColor(final int x, final int z);

  int getSubColor(final float x, final float z);

  void updateRegion(ByteBuffer destinationData, final int sourceX, final int sourceY, final int destX, final int destY,
      final int width, final int height);

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
