/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.providers.inmemory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.extension.terrain.client.TextureConfiguration;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.providers.inmemory.data.InMemoryTerrainData;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;

public class InMemoryTextureSource implements TextureSource {
  private final int tileSize;
  private final InMemoryTerrainData inMemoryTerrainData;
  private final int availableClipmapLevels;

  public InMemoryTextureSource(final int tileSize, final InMemoryTerrainData inMemoryTerrainData) {
    this.tileSize = tileSize;
    this.inMemoryTerrainData = inMemoryTerrainData;
    availableClipmapLevels = inMemoryTerrainData.getClipmapLevels();
  }

  @Override
  public TextureConfiguration getConfiguration() {
    final Map<Integer, TextureStoreFormat> textureStoreFormat = new HashMap<>();
    textureStoreFormat.put(0, TextureStoreFormat.RGBA8);

    return new TextureConfiguration(availableClipmapLevels, textureStoreFormat, tileSize, 1f, true, true);
  }

  @Override
  public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
      final int numTilesY) throws Exception {
    final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

    final Set<Tile> validTiles = new HashSet<>();

    final int levelSize = 1 << baseClipmapLevel;
    final int size = inMemoryTerrainData.getSide();

    for (int y = 0; y < numTilesY; y++) {
      for (int x = 0; x < numTilesX; x++) {
        final int xx = tileX + x;
        final int yy = tileY + y;
        if (xx >= 0 && xx * tileSize * levelSize <= size && yy >= 0 && yy * tileSize * levelSize <= size) {
          final Tile tile = new Tile(xx, yy);
          validTiles.add(tile);
        }
      }
    }

    return validTiles;
  }

  @Override
  public Set<Tile> getInvalidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
      final int numTilesY) throws Exception {
    final Set<Tile> updatedTiles[] = inMemoryTerrainData.getUpdatedTextureTiles();
    if (updatedTiles == null) {
      return null;
    }

    final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

    final Set<Tile> tiles = new HashSet<>();

    synchronized (updatedTiles[baseClipmapLevel]) {
      if (updatedTiles[baseClipmapLevel].isEmpty()) {
        return null;
      }

      int checkX, checkY;
      for (final Iterator<Tile> it = updatedTiles[baseClipmapLevel].iterator(); it.hasNext();) {
        final Tile tile = it.next();
        checkX = tile.getX();
        checkY = tile.getY();
        if (checkX >= tileX && checkX < tileX + numTilesX && checkY >= tileY && checkY < tileY + numTilesY) {
          tiles.add(tile);
          it.remove();
        }
      }
    }

    return tiles;
  }

  @Override
  public ByteBuffer getTile(final int clipmapLevel, final Tile tile) throws Exception {
    final int tileX = tile.getX();
    final int tileY = tile.getY();

    final int levelSize = 1 << availableClipmapLevels - clipmapLevel - 1;

    final int size = inMemoryTerrainData.getSide();
    final byte[] colorData = inMemoryTerrainData.getColorData();

    final ByteBuffer data = BufferUtils.createByteBufferOnHeap(tileSize * tileSize * 4);
    for (int y = 0; y < tileSize; y++) {
      for (int x = 0; x < tileSize; x++) {
        final int heightX = (tileX * tileSize + x) * levelSize;
        final int heightY = (tileY * tileSize + y) * levelSize;

        final int indexTile = (y * tileSize + x) * 4;
        final int index = heightY * size + heightX;

        if (heightX < 0 || heightX >= size || heightY < 0 || heightY >= size) {
          data.put(indexTile + 0, (byte) 0);
          data.put(indexTile + 1, (byte) 0);
          data.put(indexTile + 2, (byte) 0);
          data.put(indexTile + 3, (byte) 255);
        } else {
          data.put(indexTile + 0, colorData[index * 4 + 0]);
          data.put(indexTile + 1, colorData[index * 4 + 1]);
          data.put(indexTile + 2, colorData[index * 4 + 2]);
          data.put(indexTile + 3, colorData[index * 4 + 3]);
        }
      }
    }

    return data;
  }

  protected String name;

  @Override
  public String getName() { return name; }

  @Override
  public void setName(final String value) { name = value; }

  protected ColorRGBA tint = new ColorRGBA(ColorRGBA.WHITE);

  @Override
  public ReadOnlyColorRGBA getTintColor() { return tint; }

  @Override
  public void setTintColor(final ReadOnlyColorRGBA value) {
    tint.set(value);
  }
}
