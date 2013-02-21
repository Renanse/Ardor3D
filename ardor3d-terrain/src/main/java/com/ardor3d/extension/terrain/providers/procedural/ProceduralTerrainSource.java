/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.procedural;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.ardor3d.extension.terrain.client.TerrainConfiguration;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.type.ReadOnlyVector3;

public class ProceduralTerrainSource implements TerrainSource {
    private final Function3D function;
    private final ReadOnlyVector3 scale;
    private final float minHeight;
    private final float maxHeight;

    private static final int tileSize = 128;
    private static final int availableClipmapLevels = 8;

    private final ReentrantLock terrainLock = new ReentrantLock();
    private final ThreadLocal<float[]> tileDataPool = new ThreadLocal<float[]>() {
        @Override
        protected float[] initialValue() {
            return new float[tileSize * tileSize];
        }
    };

    public ProceduralTerrainSource(final Function3D function, final ReadOnlyVector3 scale, final float minHeight,
            final float maxHeight) {
        this.function = function;
        this.scale = scale;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    @Override
    public TerrainConfiguration getConfiguration() throws Exception {
        return new TerrainConfiguration(availableClipmapLevels, tileSize, scale, minHeight, maxHeight, false);
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
        final float[] data = tileDataPool.get();
        final int tileX = tile.getX();
        final int tileY = tile.getY();

        final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

        terrainLock.lock();
        try {
            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {
                    if (Thread.interrupted()) {
                        return null;
                    }

                    final int heightX = tileX * tileSize + x;
                    final int heightY = tileY * tileSize + y;

                    final int index = x + y * tileSize;
                    data[index] = (float) function.eval(heightX << baseClipmapLevel, heightY << baseClipmapLevel, 0);
                }
            }
        } finally {
            terrainLock.unlock();
        }
        return data;
    }
}
