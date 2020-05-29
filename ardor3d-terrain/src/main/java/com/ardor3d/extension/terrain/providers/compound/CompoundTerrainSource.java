/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.compound;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ardor3d.extension.terrain.client.TerrainConfiguration;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.util.Tile;

public class CompoundTerrainSource implements TerrainSource {

    protected List<Entry> _sourceList = new ArrayList<>();
    protected TerrainConfiguration _terrainConfig;

    public CompoundTerrainSource(final TerrainConfiguration config) {
        _terrainConfig = config;
    }

    @Override
    public TerrainConfiguration getConfiguration() throws Exception {
        return _terrainConfig;
    }

    public void setTerrainConfig(final TerrainConfiguration config) {
        _terrainConfig = config;
    }

    public void addEntry(final Entry entry) {
        _sourceList.add(entry);
    }

    public List<Entry> getEntries() {
        return _sourceList;
    }

    public Entry getEntry(final int index) {
        return _sourceList.get(index);
    }

    /**
     * @return the intersection of all non-null sets returned from the sources contained herein.
     */
    @Override
    public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        Set<Tile> set;
        Set<Tile> rVal = null;
        for (final Entry src : _sourceList) {
            set = src.getSource() == null ? null
                    : src.getSource().getValidTiles(clipmapLevel, tileX, tileY, numTilesX, numTilesY);
            if (set != null) {
                if (rVal == null) {
                    rVal = new HashSet<>(set);
                } else {
                    rVal.retainAll(set);
                }
            }
        }
        return rVal;
    }

    /**
     * @return the union of all non-null sets returned from the sources contained herein.
     */
    @Override
    public Set<Tile> getInvalidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
            final int numTilesY) throws Exception {
        Set<Tile> set;
        Set<Tile> rVal = null;
        for (final Entry src : _sourceList) {
            set = src.getSource() == null ? null
                    : src.getSource().getInvalidTiles(clipmapLevel, tileX, tileY, numTilesX, numTilesY);
            if (set != null) {
                if (rVal == null) {
                    rVal = new HashSet<>(set);
                } else {
                    rVal.addAll(set);
                }
            }
        }
        return rVal;
    }

    @Override
    public float[] getTile(final int clipmapLevel, final Tile tile) throws Exception {
        float[] data = null;

        for (final Entry entry : _sourceList) {
            if (entry.getSource() != null) {
                final float[] srcData = entry.getSource().getTile(clipmapLevel, tile);
                if (entry.getCombine() == null) {
                    data = srcData;
                } else {
                    data = entry.getCombine().apply(data, srcData);
                }
            }
        }

        return data;
    }

}
