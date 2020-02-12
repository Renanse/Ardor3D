/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client;

import java.util.Map;

import com.ardor3d.image.TextureStoreFormat;

/**
 * Terrain Configuration data for a specific map.
 */
public class TextureConfiguration {
    /** Total number of clipmap levels in this map */
    private final int totalNrClipmapLevels;
    /** Mapping of sourceIDs to texture format */
    private final Map<Integer, TextureStoreFormat> textureDataTypes;
    /** "Tile size" for each tile in the cache */
    private final int cacheGridSize;
    /** Texture density in relation to the terrain */
    private final float textureDensity;
    /** True if tiles are only valid in positive coordinates */
    private final boolean onlyPositiveQuadrant;
    /** True if destination data should render alpha */
    private final boolean useAlpha;

    public TextureConfiguration(final int totalNrClipmapLevels,
            final Map<Integer, TextureStoreFormat> textureDataTypes, final int cacheGridSize,
            final float textureDensity, final boolean onlyPositiveQuadrant, final boolean useAlpha) {
        this.totalNrClipmapLevels = totalNrClipmapLevels;
        this.textureDataTypes = textureDataTypes;
        this.cacheGridSize = cacheGridSize;
        this.textureDensity = textureDensity;
        this.onlyPositiveQuadrant = onlyPositiveQuadrant;
        this.useAlpha = useAlpha;
    }

    public int getTotalNrClipmapLevels() {
        return totalNrClipmapLevels;
    }

    public Map<Integer, TextureStoreFormat> getTextureDataTypes() {
        return textureDataTypes;
    }

    public int getCacheGridSize() {
        return cacheGridSize;
    }

    public float getTextureDensity() {
        return textureDensity;
    }

    public boolean isOnlyPositiveQuadrant() {
        return onlyPositiveQuadrant;
    }

    public TextureStoreFormat getTextureDataType(final int sourceId) {
        return textureDataTypes.get(sourceId);
    }

    public boolean isUseAlpha() {
        return useAlpha;
    }

    @Override
    public String toString() {
        return "TextureConfiguration [cacheGridSize=" + cacheGridSize + ", onlyPositiveQuadrant="
                + onlyPositiveQuadrant + ", textureDataTypes=" + textureDataTypes + ", textureDensity="
                + textureDensity + ", totalNrClipmapLevels=" + totalNrClipmapLevels + ", useAlpha=" + useAlpha + "]";
    }
}
