/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.providers.procedural;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.terrain.client.TerrainDataProvider;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.client.TextureSource;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.type.ReadOnlyVector3;

public class ProceduralTerrainDataProvider implements TerrainDataProvider {
    private final Function3D function;
    private final ReadOnlyVector3 scale;
    private final float minHeight;
    private final float maxHeight;

    private boolean generateNormalMap;

    public ProceduralTerrainDataProvider(final Function3D function, final ReadOnlyVector3 scale, final float minHeight,
            final float maxHeight) {
        this(function, scale, minHeight, maxHeight, false);
    }

    public ProceduralTerrainDataProvider(final Function3D function, final ReadOnlyVector3 scale, final float minHeight,
            final float maxHeight, final boolean generateNormalMap) {
        this.function = function;
        this.scale = scale;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.generateNormalMap = generateNormalMap;
    }

    @Override
    public TerrainSource getTerrainSource() {
        return new ProceduralTerrainSource(function, scale, minHeight, maxHeight);
    }

    @Override
    public List<TextureSource> getTextureSources() {
        final List<TextureSource> rVal = new ArrayList<>(1);
        rVal.add(new ProceduralTextureSource(function));
        return rVal;
    }

    @Override
    public TextureSource getNormalMapSource() {
        return new ProceduralNormalMapSource(function, scale.getY() / maxHeight, scale.getX(), scale.getZ());
    }

    public boolean isGenerateNormalMap() {
        return generateNormalMap;
    }

    public void setGenerateNormalMap(final boolean generateNormalMap) {
        this.generateNormalMap = generateNormalMap;
    }
}
