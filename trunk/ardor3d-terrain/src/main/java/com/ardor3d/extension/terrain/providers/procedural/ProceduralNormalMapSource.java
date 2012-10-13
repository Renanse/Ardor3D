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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.ardor3d.extension.terrain.client.TextureConfiguration;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Maps;

public class ProceduralNormalMapSource implements TextureSource {
    private final Function3D function;

    private static final int tileSize = 128;
    private static final int availableClipmapLevels = 8;

    private final ReentrantLock textureLock = new ReentrantLock();
    private final ThreadLocal<ByteBuffer> tileDataPool = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return BufferUtils.createByteBufferOnHeap(tileSize * tileSize * 3);
        }
    };

    public ProceduralNormalMapSource(final Function3D function) {
        this.function = function;
    }

    @Override
    public TextureConfiguration getConfiguration() throws Exception {
        final Map<Integer, TextureStoreFormat> textureStoreFormat = Maps.newHashMap();
        textureStoreFormat.put(0, TextureStoreFormat.RGB8);

        return new TextureConfiguration(availableClipmapLevels, textureStoreFormat, tileSize, 1f, false, false);
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
    public ByteBuffer getTile(final int clipmapLevel, final Tile tile) throws Exception {
        final ByteBuffer data = tileDataPool.get();
        final int tileX = tile.getX();
        final int tileY = tile.getY();

        final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

        final Vector3 normal = new Vector3();
        textureLock.lock();
        try {
            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {
                    if (Thread.interrupted()) {
                        return null;
                    }

                    final int heightX = tileX * tileSize + x;
                    final int heightY = tileY * tileSize + y;

                    normal.setZ(1);
                    final double eval1 = function.eval(heightX - 1 << baseClipmapLevel, heightY << baseClipmapLevel, 0);
                    final double eval2 = function.eval(heightX + 1 << baseClipmapLevel, heightY << baseClipmapLevel, 0);
                    final double eval3 = function.eval(heightX << baseClipmapLevel, heightY - 1 << baseClipmapLevel, 0);
                    final double eval4 = function.eval(heightX << baseClipmapLevel, heightY + 1 << baseClipmapLevel, 0);

                    normal.setX((eval1 - eval2) / 2.);
                    normal.setY((eval3 - eval4) / 2.);
                    normal.normalizeLocal();

                    final int index = (x + y * tileSize) * 3;
                    data.put(index, (byte) (normal.getX() * 255));
                    data.put(index + 1, (byte) (normal.getY() * 255));
                    data.put(index + 2, (byte) (normal.getZ() * 255));
                }
            }
        } finally {
            textureLock.unlock();
        }
        return data;
    }
}
