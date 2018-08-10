/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.array;

import java.util.List;
import java.util.Set;

import com.ardor3d.extension.terrain.client.TerrainConfiguration;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.google.common.collect.Sets;

public class ArrayTerrainSource implements TerrainSource {
    private final int tileSize;
    private final List<float[]> heightMaps;
    private final List<Integer> heightMapSizes;
    private final ReadOnlyVector3 scale;
    private final float heightMin;
    private final float heightMax;

    private final ThreadLocal<float[]> tileDataPool = new ThreadLocal<float[]>() {
        @Override
        protected float[] initialValue() {
            return new float[tileSize * tileSize];
        }
    };

    public ArrayTerrainSource(final int tileSize, final List<float[]> heightMaps, final List<Integer> heightMapSizes,
            final ReadOnlyVector3 scale, final float heightMin, final float heightMax) {
        this.tileSize = tileSize;
        this.heightMaps = heightMaps;
        this.heightMapSizes = heightMapSizes;
        this.scale = scale;
        this.heightMin = heightMin;
        this.heightMax = heightMax;
    }

    @Override
    public TerrainConfiguration getConfiguration() throws Exception {
        return new TerrainConfiguration(heightMaps.size(), tileSize, scale, heightMin, heightMax, true);
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
    public float[] getTile(final int clipmapLevel, final Tile tile) throws Exception {
        final int tileX = tile.getX();
        final int tileY = tile.getY();

        final float[] heightMap = heightMaps.get(clipmapLevel);
        final int heightMapSize = heightMapSizes.get(clipmapLevel);

        final float[] data = tileDataPool.get();
        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                final int index = x + y * tileSize;

                final int heightX = tileX * tileSize + x;
                final int heightY = tileY * tileSize + y;
                data[index] = getHeight(heightMap, heightMapSize, heightX, heightY);
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
