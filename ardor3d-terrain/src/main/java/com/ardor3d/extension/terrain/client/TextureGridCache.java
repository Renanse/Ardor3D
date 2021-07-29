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

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.terrain.client.functions.CacheFunctionUtil;
import com.ardor3d.extension.terrain.client.functions.SourceCacheFunction;
import com.ardor3d.extension.terrain.util.IntColorUtils;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.util.MathUtils;

/**
 * Special tile/grid based cache for texture data
 */
public class TextureGridCache extends AbstractGridCache implements TextureCache {
  /** The Constant logger. */
  private static final Logger logger = Logger.getLogger(TextureGridCache.class.getName());

  private final TextureSource source;
  private final TextureCache parentCache;
  private final TextureConfiguration textureConfiguration;
  private SourceCacheFunction function;

  private final byte[] data;

  private final boolean useAlpha;
  private final int colorWidthBytes;

  public TextureGridCache(final TextureCache parentCache, final int cacheSize, final TextureSource source,
    final int tileSize, final int destinationSize, final TextureConfiguration textureConfiguration,
    final int meshClipIndex, final int dataClipIndex, final ExecutorService tileThreadService) {
    super(cacheSize, tileSize, destinationSize, meshClipIndex, dataClipIndex, 1, tileThreadService);
    this.parentCache = parentCache;
    this.source = source;
    this.textureConfiguration = textureConfiguration;
    useAlpha = textureConfiguration.isUseAlpha();
    colorWidthBytes = useAlpha ? 4 : 3;

    data = new byte[dataSize * dataSize * colorWidthBytes];
    for (int i = 0; i < dataSize * dataSize * colorWidthBytes; i++) {
      data[i] = (byte) 1;
    }
  }

  @Override
  public void checkForInvalidatedRegions() {
    final Set<Tile> invalidTiles = getInvalidTilesFromSource(backCurrentTileX - cacheSize / 2,
        backCurrentTileY - cacheSize / 2, cacheSize, cacheSize);
    if (invalidTiles == null || invalidTiles.isEmpty()) {
      return;
    }

    final Set<TileLoadingData> updates = new HashSet<>();

    for (final Tile tile : invalidTiles) {
      final int destX = MathUtils.moduloPositive(tile.getX(), cacheSize);
      final int destY = MathUtils.moduloPositive(tile.getY(), cacheSize);
      updates.add(new TileLoadingData(this, new Tile(tile.getX(), tile.getY()), new Tile(destX, destY), dataClipIndex));
    }

    backThreadTiles.addAll(updates);
    updated = true;
  }

  @Override
  protected State copyTileData(final Tile sourceTile, final int destX, final int destY) {
    ByteBuffer sourceData = null;
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

    final TextureStoreFormat format =
        textureConfiguration.getTextureDataType(source.getContributorId(dataClipIndex, sourceTile));
    CacheFunctionUtil.applyFunction(useAlpha, function, sourceData, data, destX, destY, format, tileSize, dataSize);

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
  public int getColor(final int x, final int z) {
    int tileX = MathUtils.floor((float) x / tileSize);
    int tileY = MathUtils.floor((float) z / tileSize);
    tileX = MathUtils.moduloPositive(tileX, cacheSize);
    tileY = MathUtils.moduloPositive(tileY, cacheSize);
    final CacheData tileData = cache[tileX][tileY];

    if (!tileData.isValid) {
      if (parentCache != null) {
        if (x % 2 == 0 && z % 2 == 0) {
          return parentCache.getColor(x / 2, z / 2);
        } else {
          return parentCache.getSubColor(x / 2f, z / 2f);
        }
      } else {
        return 0;
      }
    } else {
      final int dataX = MathUtils.moduloPositive(x, dataSize);
      final int dataY = MathUtils.moduloPositive(z, dataSize);
      final int sourceIndex = (dataY * dataSize + dataX) * colorWidthBytes;

      int color = 0;
      if (useAlpha) {
        color = IntColorUtils.getColor(data[sourceIndex + 0], data[sourceIndex + 1], data[sourceIndex + 2],
            data[sourceIndex + 3]);
      } else {
        color = IntColorUtils.getColor(data[sourceIndex + 0], data[sourceIndex + 1], data[sourceIndex + 2], (byte) 0);
      }

      return color;
    }
  }

  @Override
  public int getSubColor(final float x, final float z) {
    int tileX = MathUtils.floor(x / tileSize);
    int tileY = MathUtils.floor(z / tileSize);
    tileX = MathUtils.moduloPositive(tileX, cacheSize);
    tileY = MathUtils.moduloPositive(tileY, cacheSize);
    final CacheData tileData = cache[tileX][tileY];

    if (!tileData.isValid) {
      if (parentCache != null) {
        return parentCache.getSubColor(x / 2f, z / 2f);
      } else {
        return 0;
      }
    } else {
      final int col = MathUtils.floor(x);
      final int row = MathUtils.floor(z);
      final double intOnX = x - col;
      final double intOnZ = z - row;

      final int topLeft = getColor(col, row);
      final int topRight = getColor(col + 1, row);
      final int top = IntColorUtils.lerp(intOnX, topLeft, topRight);

      final int bottomLeft = getColor(col, row + 1);
      final int bottomRight = getColor(col + 1, row + 1);
      final int bottom = IntColorUtils.lerp(intOnX, bottomLeft, bottomRight);

      return IntColorUtils.lerp(intOnZ, top, bottom);
    }
  }

  public SourceCacheFunction getFunction() { return function; }

  public void setFunction(final SourceCacheFunction function) { this.function = function; }

  @Override
  public void updateRegion(final ByteBuffer destinationData, final int sourceX, final int sourceY, final int destX,
      final int destY, final int width, final int height) {
    final byte[] rgbArray = new byte[width * colorWidthBytes];
    for (int z = 0; z < height; z++) {
      final int currentSourceZ = sourceY + z;
      final int currentDestZ = destY + z;
      final int dataY = MathUtils.moduloPositive(currentDestZ, destinationSize);

      for (int x = 0; x < width; x++) {
        final int currentSourceX = sourceX + x;
        final int color = getColor(currentSourceX, currentSourceZ);

        final int index = x * colorWidthBytes;
        rgbArray[index + 0] = (byte) (color >> 24 & 0xFF);
        rgbArray[index + 1] = (byte) (color >> 16 & 0xFF);
        rgbArray[index + 2] = (byte) (color >> 8 & 0xFF);
        if (useAlpha) {
          rgbArray[index + 3] = (byte) (color & 0xFF);
        }
      }

      final int dataX = MathUtils.moduloPositive(destX, destinationSize);
      if (dataX + width > destinationSize) {
        final int destIndex = dataY * destinationSize * colorWidthBytes;

        destinationData.position(destIndex + dataX * colorWidthBytes);
        destinationData.put(rgbArray, 0, (destinationSize - dataX) * colorWidthBytes);

        destinationData.position(destIndex);
        destinationData.put(rgbArray, (destinationSize - dataX) * colorWidthBytes,
            (dataX + width - destinationSize) * colorWidthBytes);
      } else {
        final int destIndex = (dataY * destinationSize + dataX) * colorWidthBytes;
        destinationData.position(destIndex);
        destinationData.put(rgbArray);
      }
    }
  }
}
