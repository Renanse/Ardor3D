/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.inmemory;

import java.util.Iterator;
import java.util.Set;

import com.ardor3d.extension.terrain.client.TerrainConfiguration;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.providers.inmemory.data.InMemoryTerrainData;
import com.ardor3d.extension.terrain.util.Tile;
import com.google.common.collect.Sets;

public class InMemoryTerrainSource implements TerrainSource {
    private final int tileSize;
    private final InMemoryTerrainData inMemoryTerrainData;
    private final int availableClipmapLevels;

    public InMemoryTerrainSource(final int tileSize, final InMemoryTerrainData inMemoryTerrainData) {
        this.tileSize = tileSize;
        this.inMemoryTerrainData = inMemoryTerrainData;
        availableClipmapLevels = inMemoryTerrainData.getClipmapLevels();
    }

    @Override
    public TerrainConfiguration getConfiguration() throws Exception {
        return new TerrainConfiguration(availableClipmapLevels, tileSize, inMemoryTerrainData.getScale(),
                inMemoryTerrainData.getMinHeight(), inMemoryTerrainData.getMaxHeight(), true);
    }

    @Override
    public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

        final Set<Tile> validTiles = Sets.newHashSet();

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
        final Set<Tile> updatedTiles[] = inMemoryTerrainData.getUpdatedTerrainTiles();
        if (updatedTiles == null) {
            return null;
        }

        final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

        final Set<Tile> tiles = Sets.newHashSet();

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
    public int getContributorId(final int clipmapLevel, final Tile tile) {
        return 0;
    }

    @Override
    public float[] getTile(final int clipmapLevel, final Tile tile) throws Exception {
        final int tileX = tile.getX();
        final int tileY = tile.getY();

        final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

        final int levelSize = 1 << baseClipmapLevel;

        final int size = inMemoryTerrainData.getSide();

        final float[] heightData = inMemoryTerrainData.getHeightData();

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
