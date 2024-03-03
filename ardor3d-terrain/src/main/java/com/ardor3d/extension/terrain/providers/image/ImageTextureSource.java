/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.providers.image;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.extension.terrain.client.TextureConfiguration;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.Image;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;

public class ImageTextureSource implements TextureSource {
  private final int tileSize;
  private final List<byte[]> maps;
  private final List<Integer> heightMapSizes;

  private final ThreadLocal<ByteBuffer> tileDataPool = new ThreadLocal<>() {
    @Override
    protected ByteBuffer initialValue() {
      return BufferUtils.createByteBufferOnHeap(tileSize * tileSize * 3);
    }
  };

  public ImageTextureSource(final int tileSize, final Image map, final List<Integer> heightMapSizes) {
    this.tileSize = tileSize;
    maps = new ArrayList<>(heightMapSizes.size());
    this.heightMapSizes = new ArrayList<>(heightMapSizes);
    buildMips(map);
  }

  private void buildMips(final Image map) {
    final int max = heightMapSizes.size();
    int currentSize = heightMapSizes.get(max - 1);
    byte[] parentHeightMap = new byte[currentSize * currentSize * 3];
    // populate parentHeightMap from image
    map.getData(0).get(parentHeightMap);

    maps.add(parentHeightMap);
    // populate mips
    for (int i = 1; i < max; i++) {
      currentSize = heightMapSizes.get(max - i - 1);
      final byte[] heightMapMip = new byte[currentSize * currentSize * 3];
      for (int x = 0; x < currentSize; x++) {
        for (int z = 0; z < currentSize; z++) {
          heightMapMip[3 * (z * currentSize + x) + 0] = parentHeightMap[3 * (z * currentSize * 4 + x * 2) + 0];
          heightMapMip[3 * (z * currentSize + x) + 1] = parentHeightMap[3 * (z * currentSize * 4 + x * 2) + 1];
          heightMapMip[3 * (z * currentSize + x) + 2] = parentHeightMap[3 * (z * currentSize * 4 + x * 2) + 2];
        }
      }
      parentHeightMap = heightMapMip;
      maps.add(parentHeightMap);
    }
    Collections.reverse(maps);
  }

  @Override
  public TextureConfiguration getConfiguration() {
    final Map<Integer, TextureStoreFormat> textureStoreFormat = new HashMap<>();
    textureStoreFormat.put(0, TextureStoreFormat.RGB8);

    return new TextureConfiguration(maps.size(), textureStoreFormat, tileSize, 1f, true, false);
  }

  @Override
  public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
      final int numTilesY) throws Exception {
    final Set<Tile> validTiles = new HashSet<>();

    final int heightMapSize = heightMapSizes.get(clipmapLevel);
    for (int y = 0; y < numTilesY; y++) {
      for (int x = 0; x < numTilesX; x++) {
        final int xx = tileX + x;
        final int yy = tileY + y;
        if (xx >= 0 && xx * tileSize <= heightMapSize && yy >= 0 && yy * tileSize <= heightMapSize) {
          final Tile tile = new Tile(xx, yy);
          validTiles.add(tile);
        }
      }
    }

    return validTiles;
  }

  @Override
  public ByteBuffer getTile(final int clipmapLevel, final Tile tile) throws Exception {
    final int tileX = tile.getX();
    final int tileY = tile.getY();

    final byte[] heightMap = maps.get(clipmapLevel);
    final int heightMapSize = heightMapSizes.get(clipmapLevel);

    final ByteBuffer data = tileDataPool.get();
    for (int y = 0; y < tileSize; y++) {
      for (int x = 0; x < tileSize; x++) {
        final int index = x + y * tileSize;

        final int heightX = tileX * tileSize + x;
        final int heightY = tileY * tileSize + y;
        if (heightX < 0 || heightX >= heightMapSize || heightY < 0 || heightY >= heightMapSize) {
          data.put(index * 3 + 0, (byte) 0);
          data.put(index * 3 + 1, (byte) 0);
          data.put(index * 3 + 2, (byte) 0);
        } else {
          data.put(index * 3 + 0, heightMap[3 * (heightY * heightMapSize + heightX) + 0]);
          data.put(index * 3 + 1, heightMap[3 * (heightY * heightMapSize + heightX) + 1]);
          data.put(index * 3 + 2, heightMap[3 * (heightY * heightMapSize + heightX) + 2]);
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
