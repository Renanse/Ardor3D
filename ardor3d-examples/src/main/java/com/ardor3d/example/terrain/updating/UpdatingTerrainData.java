/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.terrain.updating;

import com.ardor3d.extension.terrain.providers.inmemory.data.InMemoryTerrainData;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.functions.FbmFunction3D;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;

public class UpdatingTerrainData extends InMemoryTerrainData {

  public UpdatingTerrainData(final int totalSide, final int clipmapLevels, final int tileSize,
    final ReadOnlyVector3 scale, final FbmFunction3D sourceFunc) {
    super(totalSide, clipmapLevels, tileSize, scale, sourceFunc);
  }

  void updateRegion(final Vector3 center, final double size) {
    // modify the terrain!
    int index;
    final double halfSize = size / 2.0;
    final int minY = (int) Math.max(0, center.getZ() - halfSize),
        maxY = (int) Math.min(center.getZ() + halfSize, side - 1);
    final int minX = (int) Math.max(0, center.getX() - halfSize),
        maxX = (int) Math.min(center.getX() + halfSize, side - 1);
    final double scale = (maxHeight - minHeight) * .25 * MathUtils.nextRandomDouble() + .25;
    for (int i = minY; i <= maxY; i++) {
      for (int j = minX; j <= maxX; j++) {
        index = i * side + j;
        heightData[index] = Math.max(minHeight, Math.min((float) (Math.sin(i / 75.0) * scale + scale), maxHeight));
      }
    }

    // queue up an update alert for the rectangle updated
    final Rectangle2 region = new Rectangle2(minX, minY, maxX - minX, maxY - minY);

    // break up by clipmaplevel
    // add to two queues since these updates are called in different threads potentially
    for (int i = 0; i < clipmapLevels; i++) {
      addTiles(region, updatedTerrainTiles[i]);
    }
  }

}
