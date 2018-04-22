/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.array;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ardor3d.extension.terrain.client.TextureConfiguration;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ArrayTextureSource implements TextureSource {
    private final int tileSize;
    private final List<float[]> heightMaps;
    private final List<Integer> heightMapSizes;

    private final ThreadLocal<ByteBuffer> tileDataPool = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return BufferUtils.createByteBufferOnHeap(tileSize * tileSize);
        }
    };

    public ArrayTextureSource(final int tileSize, final List<float[]> heightMaps, final List<Integer> heightMapSizes) {
        this.tileSize = tileSize;
        this.heightMaps = heightMaps;
        this.heightMapSizes = heightMapSizes;
    }

    @Override
    public TextureConfiguration getConfiguration() throws Exception {
        final Map<Integer, TextureStoreFormat> textureStoreFormat = Maps.newHashMap();
        textureStoreFormat.put(0, TextureStoreFormat.Luminance8);

        return new TextureConfiguration(heightMaps.size(), textureStoreFormat, tileSize, 1f, true, false);
    }

    @Override
    public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        final Set<Tile> validTiles = Sets.newHashSet();

        final int heightMapSize = heightMapSizes.get(clipmapLevel);
        for (int y = 0; y < numTilesY; y++) {
            for (int x = 0; x < numTilesX; x++) {
                final int xx = tileX + x;
                final int yy = tileY + y;
                if (xx >= 0 && xx * tileSize <= heightMapSize && yy >= 0 && yy * tileSize <= heightMapSize) {
                    validTiles.add(new Tile(xx, yy));
                }
            }
        }

        return validTiles;
    }

    @Override
    public Set<Tile> getInvalidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        return null;
    }

    @Override
    public int getContributorId(final int clipmapLevel, final Tile tile) {
        return 0;
    }

    @Override
    public ByteBuffer getTile(final int clipmapLevel, final Tile tile) throws Exception {
        final int tileX = tile.getX();
        final int tileY = tile.getY();

        final float[] heightMap = heightMaps.get(clipmapLevel);
        final int heightMapSize = heightMapSizes.get(clipmapLevel);

        final ByteBuffer data = tileDataPool.get();
        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                final int index = (x + y * tileSize) * 1;

                final int heightX = tileX * tileSize + x;
                final int heightY = tileY * tileSize + y;
                final float height = getHeight(heightMap, heightMapSize, heightX, heightY);
                final byte byteHeight = (byte) (height * 255 * 3);
                data.put(index, byteHeight);
            }
        }
        return data;
    }

    private float getHeight(final float[] heightMap, final int heightMapSize, final int x, final int y) {
        if (x < 0 || x >= heightMapSize || y < 0 || y >= heightMapSize) {
            return 0;
        }

        return heightMap[y * heightMapSize + x];
    }
}
