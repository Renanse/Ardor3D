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

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import com.ardor3d.extension.terrain.client.TextureConfiguration;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.util.geom.BufferUtils;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class SimpleArrayTextureSource implements TextureSource {
    private final int tileSize;
    private final byte[] colorData;
    private final int size;
    private final int availableClipmapLevels = 8;

    public SimpleArrayTextureSource(final int tileSize, final byte[] colorData, final int size) {
        this.tileSize = tileSize;
        this.colorData = colorData;
        this.size = size;
    }

    @Override
    public TextureConfiguration getConfiguration() throws Exception {
        final Map<Integer, TextureStoreFormat> textureStoreFormat = Maps.newHashMap();
        textureStoreFormat.put(0, TextureStoreFormat.RGBA8);

        return new TextureConfiguration(availableClipmapLevels, textureStoreFormat, tileSize, 1f, true, true);
    }

    @Override
    public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        final Set<Tile> validTiles = Sets.newHashSet();

        final int levelSize = 1 << availableClipmapLevels - clipmapLevel;

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

        final int baseClipmapLevel = availableClipmapLevels - clipmapLevel - 1;

        final int levelSize = 1 << baseClipmapLevel;

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
                    data.put(indexTile + 3, (byte) 0);
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
}
