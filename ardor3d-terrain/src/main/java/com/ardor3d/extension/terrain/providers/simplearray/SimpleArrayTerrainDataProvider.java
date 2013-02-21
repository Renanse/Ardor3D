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

import java.util.List;
import java.util.Map;

import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.providers.image.ImageTextureSource;
import com.ardor3d.extension.terrain.util.NormalMapUtil;
import com.ardor3d.image.Image;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SimpleArrayTerrainDataProvider implements TerrainDataProvider {
    private static final int tileSize = 128;

    private final float[] heightData;
    private final byte[] colorData;
    private final int side;

    private boolean generateNormalMap;

    public SimpleArrayTerrainDataProvider(final float[] heightData, final byte[] colorData, final int side) {
        this(heightData, colorData, side, false);
    }

    public SimpleArrayTerrainDataProvider(final float[] heightData, final byte[] colorData, final int side,
            final boolean generateNormalMap) {
        this.heightData = heightData;
        this.colorData = colorData;
        this.side = side;
        this.generateNormalMap = generateNormalMap;
    }

    @Override
    public Map<Integer, String> getAvailableMaps() throws Exception {
        final Map<Integer, String> maps = Maps.newHashMap();
        maps.put(0, "InMemoryData");

        return maps;
    }

    @Override
    public TerrainSource getTerrainSource(final int mapId) {
        return new SimpleArrayTerrainSource(tileSize, heightData, side);
    }

    @Override
    public TextureSource getTextureSource(final int mapId) {
        return new SimpleArrayTextureSource(tileSize, colorData, side);
    }

    @Override
    public TextureSource getNormalMapSource(final int mapId) {
        if (generateNormalMap) {
            try {
                final Image normalImage = NormalMapUtil.constructNormalMap(heightData, side, 1, 1, 1);
                final List<Integer> heightMapSizes = Lists.newArrayList();
                int currentSize = side;
                heightMapSizes.add(currentSize);
                for (int i = 0; i < 8; i++) {
                    currentSize /= 2;
                    heightMapSizes.add(currentSize);
                }
                return new ImageTextureSource(tileSize, normalImage, heightMapSizes);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isGenerateNormalMap() {
        return generateNormalMap;
    }

    public void setGenerateNormalMap(final boolean generateNormalMap) {
        this.generateNormalMap = generateNormalMap;
    }
}
