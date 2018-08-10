/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
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
import com.ardor3d.image.util.GeneratedImageFactory;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Maps;

public class ProceduralTextureSource implements TextureSource {
    private final Function3D function;

    private static final int tileSize = 128;
    private static final int availableClipmapLevels = 8;

    private final ReadOnlyColorRGBA[] terrainColors;

    private final ReentrantLock textureLock = new ReentrantLock();
    private final ThreadLocal<ByteBuffer> tileDataPool = new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
            return BufferUtils.createByteBufferOnHeap(tileSize * tileSize * 3);
        }
    };

    public ProceduralTextureSource(final Function3D function) {
        this.function = function;

        terrainColors = new ReadOnlyColorRGBA[256];
        terrainColors[0] = new ColorRGBA(0, 0, .5f, 1);
        terrainColors[95] = new ColorRGBA(0, 0, 1, 1);
        terrainColors[127] = new ColorRGBA(0, .5f, 1, 1);
        terrainColors[137] = new ColorRGBA(240 / 255f, 240 / 255f, 64 / 255f, 1);
        terrainColors[143] = new ColorRGBA(32 / 255f, 160 / 255f, 0, 1);
        terrainColors[175] = new ColorRGBA(224 / 255f, 224 / 255f, 0, 1);
        terrainColors[223] = new ColorRGBA(128 / 255f, 128 / 255f, 128 / 255f, 1);
        terrainColors[255] = ColorRGBA.WHITE;
        GeneratedImageFactory.fillInColorTable(terrainColors);
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

        textureLock.lock();
        try {
            for (int y = 0; y < tileSize; y++) {
                for (int x = 0; x < tileSize; x++) {
                    if (Thread.interrupted()) {
                        return null;
                    }

                    final int heightX = tileX * tileSize + x;
                    final int heightY = tileY * tileSize + y;

                    final double eval = function.eval(heightX << baseClipmapLevel, heightY << baseClipmapLevel, 0) * 0.4167f + 0.5f;
                    final byte colIndex = (byte) (eval * 255);

                    final ReadOnlyColorRGBA c = terrainColors[colIndex & 0xFF];

                    final int index = (x + y * tileSize) * 3;
                    data.put(index, (byte) (c.getRed() * 255));
                    data.put(index + 1, (byte) (c.getGreen() * 255));
                    data.put(index + 2, (byte) (c.getBlue() * 255));
                }
            }
        } finally {
            textureLock.unlock();
        }
        return data;
    }
}
