/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.inmemory;

import java.util.List;
import java.util.Map;

import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.providers.image.ImageTextureSource;
import com.ardor3d.extension.terrain.providers.inmemory.data.InMemoryTerrainData;
import com.ardor3d.extension.terrain.util.NormalMapUtil;
import com.ardor3d.image.Image;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class InMemoryTerrainDataProvider implements TerrainDataProvider {
    private static final int tileSize = 128;
    private final InMemoryTerrainData inMemoryTerrainData;

    private boolean generateNormalMap;

    public InMemoryTerrainDataProvider(final InMemoryTerrainData inMemoryTerrainData) {
        this(inMemoryTerrainData, false);
    }

    public InMemoryTerrainDataProvider(final InMemoryTerrainData inMemoryTerrainData, final boolean generateNormalMap) {
        this.inMemoryTerrainData = inMemoryTerrainData;
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
        return new InMemoryTerrainSource(tileSize, inMemoryTerrainData);
    }

    @Override
    public TextureSource getTextureSource(final int mapId) {
        return new InMemoryTextureSource(tileSize, inMemoryTerrainData);
    }

    @Override
    public TextureSource getNormalMapSource(final int mapId) {
        if (generateNormalMap) {
            try {
                final Image normalImage = NormalMapUtil.constructNormalMap(inMemoryTerrainData.getHeightData(),
                        inMemoryTerrainData.getSide(), inMemoryTerrainData.getMaxHeight(),
                        inMemoryTerrainData.getScale().getX(), inMemoryTerrainData.getScale().getY());

                final List<Integer> heightMapSizes = Lists.newArrayList();
                int currentSize = inMemoryTerrainData.getSide();
                heightMapSizes.add(currentSize);
                for (int i = 0; i < inMemoryTerrainData.getClipmapLevels(); i++) {
                    currentSize /= 2;
                    heightMapSizes.add(0, currentSize);
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
