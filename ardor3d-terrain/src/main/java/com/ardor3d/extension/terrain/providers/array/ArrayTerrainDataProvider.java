/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.providers.array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.extension.terrain.providers.image.ImageTextureSource;
import com.ardor3d.extension.terrain.util.NormalMapUtil;
import com.ardor3d.image.Image;
import com.ardor3d.math.type.ReadOnlyVector3;

public class ArrayTerrainDataProvider implements TerrainDataProvider {
    private static final int tileSize = 128;

    private final List<float[]> heightMaps;
    private final List<Integer> heightMapSizes;
    private final ReadOnlyVector3 scale;

    private float heightMax = 1.0f;
    private float heightMin = 0.0f;

    private boolean generateNormalMap;

    public ArrayTerrainDataProvider(final float[] data, final int size, final ReadOnlyVector3 scale) {
        this(data, size, scale, false);
    }

    public ArrayTerrainDataProvider(final float[] data, final int size, final ReadOnlyVector3 scale,
            final boolean generateNormalMap) {
        this.scale = scale;
        this.generateNormalMap = generateNormalMap;

        // TODO: calculate clipLevelCount through size and tileSize
        final int clipLevelCount = 6;

        int currentSize = size;
        heightMaps = new ArrayList<>();
        heightMapSizes = new ArrayList<>();
        heightMaps.add(data);
        heightMapSizes.add(currentSize);
        float[] parentHeightMap = data;
        for (int i = 0; i < clipLevelCount; i++) {
            currentSize /= 2;
            final float[] heightMapMip = new float[currentSize * currentSize];
            heightMaps.add(heightMapMip);
            heightMapSizes.add(currentSize);
            for (int x = 0; x < currentSize; x++) {
                for (int z = 0; z < currentSize; z++) {
                    heightMapMip[z * currentSize + x] = parentHeightMap[z * currentSize * 4 + x * 2];
                }
            }
            parentHeightMap = heightMapMip;
        }

        Collections.reverse(heightMaps);
        Collections.reverse(heightMapSizes);
    }

    @Override
    public Map<Integer, String> getAvailableMaps() throws Exception {
        final Map<Integer, String> maps = new HashMap<>();
        maps.put(0, "ArrayBasedMap");

        return maps;
    }

    @Override
    public TerrainSource getTerrainSource(final int mapId) {
        return new ArrayTerrainSource(tileSize, heightMaps, heightMapSizes, scale, heightMin, heightMax);
    }

    @Override
    public TextureSource getTextureSource(final int mapId) {
        return new ArrayTextureSource(tileSize, heightMaps, heightMapSizes);
    }

    @Override
    public TextureSource getNormalMapSource(final int mapId) {
        if (generateNormalMap) {
            try {
                final float[] data = heightMaps.get(heightMaps.size() - 1);
                final int size = heightMapSizes.get(heightMapSizes.size() - 1);
                final Image normalImage = NormalMapUtil.constructNormalMap(data, size, scale.getY() / heightMax,
                        scale.getX(), scale.getZ());
                return new ImageTextureSource(tileSize, normalImage, heightMapSizes);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public float getHeightMin() {
        return heightMin;
    }

    public void setHeightMin(final float heightMin) {
        this.heightMin = heightMin;
    }

    public float getHeightMax() {
        return heightMax;
    }

    public void setHeightMax(final float heightMax) {
        this.heightMax = heightMax;
    }

    public List<Integer> getHeightMapSizes() {
        return heightMapSizes;
    }

    public boolean isGenerateNormalMap() {
        return generateNormalMap;
    }

    public void setGenerateNormalMap(final boolean generateNormalMap) {
        this.generateNormalMap = generateNormalMap;
    }
}
