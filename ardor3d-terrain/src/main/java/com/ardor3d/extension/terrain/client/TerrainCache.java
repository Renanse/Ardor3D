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

import java.nio.FloatBuffer;
import java.util.Set;

import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * Fetches data from a source to the clipmap destination data through updateRegion.
 *
 */
public interface TerrainCache {
    /**
     * Tell the cache the current position so that it can start loading affected tiles
     *
     * @param x
     * @param y
     */
    void setCurrentPosition(int x, int y);

    float getHeight(int x, int z);

    float getSubHeight(float x, float z);

    /**
     * Update destinationData from cache in specified region
     *
     * @param destinationData
     * @param sourceX
     * @param sourceY
     * @param width
     * @param height
     */
    void updateRegion(FloatBuffer destinationData, int sourceX, int sourceY, int width, int height);

    void getEyeCoords(float[] destinationData, int sourceX, int sourceY, ReadOnlyVector3 eyePos);

    boolean isValid();

    void setMailBox(final DoubleBufferedList<Region> mailBox);

    Set<Tile> handleUpdateRequests();

    void checkForUpdates();
}
