/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.simplearray;

import java.util.Set;

import com.ardor3d.extension.terrain.client.TerrainConfiguration;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.Vector3;

public class SimpleArrayTerrainSource implements TerrainSource {
    private final int tileSize;
    private final float[] heightData;
    private final int size;
    private final int availableClipmapLevels = 8;

    public SimpleArrayTerrainSource(final int tileSize, final float[] heightData, final int size) {
        this.tileSize = tileSize;
        this.heightData = heightData;
        this.size = size;
    }

    @Override
    public TerrainConfiguration getConfiguration() throws Exception {
        return new TerrainConfiguration(availableClipmapLevels, tileSize, new Vector3(1, 1, 1), 0.0f, 1.0f, true);
    }

    @Override
    public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        return null;
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

        final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

        final int levelSize = 1 << baseClipmapLevel;

        final float[] data = new float[tileSize * tileSize];
        for (int y = 0; y < tileSize; y++) {
            for (int x = 0; x < tileSize; x++) {
                final int index = x + y * tileSize;

                final int heightX = (tileX * tileSize + x) * levelSize;
                final int heightY = (tileY * tileSize + y) * levelSize;
                data[index] = getHeight(heightData, size, heightX, heightY);
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
