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

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;

/**
 * Special tile/grid based cache for terrain data
 */
public class TerrainGridCache extends AbstractGridCache implements TerrainCache {
  /** The Constant logger. */
  private static final Logger logger = Logger.getLogger(TerrainGridCache.class.getName());

  private final TerrainSource source;
  private final TerrainCache parentCache;
  private final TerrainConfiguration terrainConfiguration;

  protected final float[] data;

  private final float heightScale;

  public TerrainGridCache(final TerrainCache parentCache, final int cacheSize, final TerrainSource source,
    final int tileSize, final int destinationSize, final TerrainConfiguration terrainConfiguration,
    final int meshClipIndex, final int dataClipIndex, final ExecutorService tileThreadService) {
    super(cacheSize, tileSize, destinationSize, meshClipIndex, dataClipIndex, MathUtils.pow2(meshClipIndex),
        tileThreadService);
    this.parentCache = parentCache;
    this.source = source;
    heightScale = terrainConfiguration.getScale().getYf();
    this.terrainConfiguration = terrainConfiguration;

    data = new float[dataSize * dataSize];
  }

  @Override
  public void checkForInvalidatedRegions() {
    final Set<Tile> invalidTiles = getInvalidTilesFromSource(backCurrentTileX - cacheSize / 2,
        backCurrentTileY - cacheSize / 2, cacheSize, cacheSize);
    if (invalidTiles == null || invalidTiles.isEmpty()) {
      return;
    }

    final var updates = new HashSet<TileLoadingData>();
    for (final Tile tile : invalidTiles) {
      updates.add(new TileLoadingData(this, tile.getX(), tile.getY(), cacheSize, dataClipIndex));
    }

    backThreadTiles.addAll(updates);
    updated = true;
  }

  @Override
  protected State copyTileData(final Tile sourceTile, final int destX, final int destY) {
    float[] sourceData = null;
    try {
      sourceData = source.getTile(dataClipIndex, sourceTile);
    } catch (final InterruptedException e) {
      // XXX: Loading can be interrupted
      return State.cancelled;
    } catch (final Throwable t) {
      t.printStackTrace();
      return State.error;
    }

    if (sourceData == null) {
      return State.loading;
    }

    final int offset = destY * tileSize * dataSize + destX * tileSize;
    for (int y = 0; y < tileSize; y++) {
      System.arraycopy(sourceData, y * tileSize, data, offset + y * dataSize, tileSize);
    }
    return State.finished;
  }

  @Override
  protected Set<Tile> getValidTilesFromSource(final int tileX, final int tileY, final int numTilesX,
      final int numTilesY) {
    try {
      return source.getValidTiles(dataClipIndex, tileX, tileY, numTilesX, numTilesY);
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Exception getting source info", e);
      return null;
    }
  }

  @Override
  protected Set<Tile> getInvalidTilesFromSource(final int tileX, final int tileY, final int numTilesX,
      final int numTilesY) {
    try {
      return source.getInvalidTiles(dataClipIndex, tileX, tileY, numTilesX, numTilesY);
    } catch (final Exception e) {
      logger.log(Level.WARNING, "Exception getting source info", e);
      return null;
    }
  }

  @Override
  public void setCurrentPosition(final int x, final int y) {
    super.setCurrentPosition(x, y);
  }

  @Override
  protected AbstractGridCache getParentCache() {
    return parentCache instanceof AbstractGridCache ? (AbstractGridCache) parentCache : null;
  }

  @Override
  public float getHeight(final int x, final int z, final boolean tryParentCache) {
    final CacheData tileData = getTileFromCache(x, z);

    if (tileData == null || !tileData.isValid) {
      if (tryParentCache && parentCache != null) {
        if (x % 2 == 0 && z % 2 == 0) {
          return parentCache.getHeight(x / 2, z / 2, tryParentCache);
        } else {
          return parentCache.getSubHeight(x / 2f, z / 2f, tryParentCache);
        }
      } else {
        return terrainConfiguration.getHeightRangeMin();
      }
    } else {
      final int dataX = MathUtils.moduloPositive(x, dataSize);
      final int dataY = MathUtils.moduloPositive(z, dataSize);

      final float min = terrainConfiguration.getHeightRangeMin();
      final float max = terrainConfiguration.getHeightRangeMax();
      final float height = data[dataY * dataSize + dataX];
      if (height < min || height > max) {
        return min;
      }

      return height * heightScale;
    }
  }

  @Override
  public float getSubHeight(final float x, final float z, final boolean tryParentCache) {
    final CacheData tileData = getTileFromCache(x, z);

    if (tileData == null || !tileData.isValid) {
      if (tryParentCache && parentCache != null) {
        return parentCache.getSubHeight(x / 2f, z / 2f, tryParentCache);
      } else {
        return terrainConfiguration.getHeightRangeMin();
      }
    } else {
      final double col = MathUtils.floor(x);
      final double row = MathUtils.floor(z);
      final double intOnX = x - col;
      final double intOnZ = z - row;

      final double col1 = col + 1;
      final double row1 = row + 1;

      final double topLeft = getHeight((int) col, (int) row, tryParentCache);
      final double topRight = getHeight((int) col1, (int) row, tryParentCache);
      final double bottomLeft = getHeight((int) col, (int) row1, tryParentCache);
      final double bottomRight = getHeight((int) col1, (int) row1, tryParentCache);

      return (float) MathUtils.lerp(intOnZ, MathUtils.lerp(intOnX, topLeft, topRight),
          MathUtils.lerp(intOnX, bottomLeft, bottomRight));
    }
  }

  @Override
  public void getEyeCoords(final float[] destinationData, final int sourceX, final int sourceY,
      final ReadOnlyVector3 eyePos) {
    for (int z = 0; z < 2; z++) {
      final int currentZ = sourceY + z;
      for (int x = 0; x < 2; x++) {
        final int currentX = sourceX + x;

        final float cacheHeight = getHeight(currentX, currentZ, true);

        final int indexDest = z * 8 + x * 4;
        destinationData[indexDest + 0] = currentX * vertexDistance - eyePos.getXf(); // x
        destinationData[indexDest + 1] = currentZ * vertexDistance - eyePos.getZf(); // z
        destinationData[indexDest + 2] = cacheHeight; // h

        if (parentCache == null) {
          destinationData[indexDest + 3] = cacheHeight; // w
        } else {
          final int coarseX1 = (currentX < 0 ? currentX - 1 : currentX) / 2;
          int coarseZ1 = (currentZ < 0 ? currentZ - 1 : currentZ) / 2;

          final boolean onGridX = currentX % 2 == 0;
          final boolean onGridZ = currentZ % 2 == 0;

          if (onGridX && onGridZ) {
            final float coarseHeight = parentCache.getHeight(coarseX1, coarseZ1, true);
            destinationData[indexDest + 3] = coarseHeight; // w
          } else {
            int coarseX2 = coarseX1;
            int coarseZ2 = coarseZ1;
            if (!onGridX && onGridZ) {
              coarseX2++;
            } else if (onGridX && !onGridZ) {
              coarseZ2++;
            } else if (!onGridX && !onGridZ) {
              coarseX2++;
              coarseZ1++;
            }

            final float coarser1 = parentCache.getHeight(coarseX1, coarseZ1, true);
            final float coarser2 = parentCache.getHeight(coarseX2, coarseZ2, true);

            // Apply the median of the coarser heightvalues to the W
            // value
            destinationData[indexDest + 3] = (coarser1 + coarser2) * 0.5f; // w
          }
        }
      }
    }
  }

  @Override
  public void updateRegion(final FloatBuffer destinationData, final int sourceX, final int sourceY, final int width,
      final int height) {
    for (int z = 0; z < height; z++) {
      final int currentZ = sourceY + z;
      for (int x = 0; x < width; x++) {
        final int currentX = sourceX + x;

        final float cacheHeight = getHeight(currentX, currentZ, true);

        final int destX = MathUtils.moduloPositive(currentX, destinationSize);
        final int destY = MathUtils.moduloPositive(currentZ, destinationSize);
        final int indexDest = (destY * destinationSize + destX) * ClipmapLevel.VERT_SIZE;

        destinationData.put(indexDest + 0, currentX * vertexDistance); // x
        destinationData.put(indexDest + 1, cacheHeight); // y
        destinationData.put(indexDest + 2, currentZ * vertexDistance); // z

        if (parentCache == null) {
          destinationData.put(indexDest + 3, cacheHeight); // w
        } else {
          final int coarseX1 = (currentX < 0 ? currentX - 1 : currentX) / 2;
          int coarseZ1 = (currentZ < 0 ? currentZ - 1 : currentZ) / 2;

          final boolean onGridX = currentX % 2 == 0;
          final boolean onGridZ = currentZ % 2 == 0;

          if (onGridX && onGridZ) {
            final float coarseHeight = parentCache.getHeight(coarseX1, coarseZ1, true);
            destinationData.put(indexDest + 3, coarseHeight); // w
          } else {
            int coarseX2 = coarseX1;
            int coarseZ2 = coarseZ1;
            if (!onGridX && onGridZ) {
              coarseX2++;
            } else if (onGridX && !onGridZ) {
              coarseZ2++;
            } else if (!onGridX && !onGridZ) {
              coarseX2++;
              coarseZ1++;
            }

            final float coarser1 = parentCache.getHeight(coarseX1, coarseZ1, true);
            final float coarser2 = parentCache.getHeight(coarseX2, coarseZ2, true);

            // Apply the median of the coarser heightvalues to the W value
            destinationData.put(indexDest + 3, (coarser1 + coarser2) * 0.5f); // w
          }
        }
      }
    }
  }
}
