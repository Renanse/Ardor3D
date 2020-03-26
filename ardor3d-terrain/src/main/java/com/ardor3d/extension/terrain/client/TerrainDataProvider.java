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

import java.util.List;

/**
 * The TerrainDataProvider is the connection between the terrain core and external data.
 */
public interface TerrainDataProvider {
    /**
     * @return this provider's TerrainSource
     */
    TerrainSource getTerrainSource();

    /**
     * @return a list of TextureSources for this Provider.
     */
    List<TextureSource> getTextureSources();

    /**
     * @return the normalmap TextureSource for this Provider, or null if none is provided.
     */
    TextureSource getNormalMapSource();
}
