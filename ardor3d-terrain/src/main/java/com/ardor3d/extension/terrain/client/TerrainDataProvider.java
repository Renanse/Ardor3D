/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.client;

import java.util.Map;

/**
 * The TerrainDataProvider is the connection between the terrain core and external data.
 */
public interface TerrainDataProvider {
    /**
     * Request for all available maps. Returns a Map with mapIDs and map names.
     * 
     * @return Available maps
     * @throws Exception
     */
    Map<Integer, String> getAvailableMaps() throws Exception;

    /**
     * Request for a TerrainSource of valid type for this Provider.
     * 
     * @param mapId
     * @return
     */
    TerrainSource getTerrainSource(int mapId);

    /**
     * Request for a TextureSource of valid type for this Provider.
     * 
     * @param mapId
     * @return
     */
    TextureSource getTextureSource(int mapId);

    /**
     * Request for a normalmap TextureSource of valid type for this Provider.
     * 
     * @param mapId
     * @return
     */
    TextureSource getNormalMapSource(int mapId);
}
