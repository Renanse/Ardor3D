/**
 * Copyright (c) 2008-2022 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.compound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.extension.terrain.client.TerrainConfiguration;
import com.ardor3d.extension.terrain.client.TerrainSource;
import com.ardor3d.extension.terrain.util.Tile;

public class CompoundTerrainSource implements TerrainSource {

  protected final List<Entry> _sourceList = new ArrayList<>();
  protected TerrainConfiguration _terrainConfig;

  protected ReadWriteLock _sourceLock = new ReentrantReadWriteLock();

  public CompoundTerrainSource(final TerrainConfiguration config) {
    _terrainConfig = config;
  }

  @Override
  public TerrainConfiguration getConfiguration() throws Exception { return _terrainConfig; }

  public void setTerrainConfig(final TerrainConfiguration config) { _terrainConfig = config; }

  @MainThread
  public void addEntry(final Entry entry) {
    _sourceLock.writeLock().lock();
    try {
      _sourceList.add(entry);
    } finally {
      _sourceLock.writeLock().unlock();
    }
  }

  @MainThread
  public void addEntry(final int index, final Entry entry) {
    _sourceLock.writeLock().lock();
    try {
      _sourceList.add(index, entry);
    } finally {
      _sourceLock.writeLock().unlock();
    }
  }

  @MainThread
  public void removeEntry(final Entry entry) {
    _sourceLock.writeLock().lock();
    try {
      _sourceList.remove(entry);
    } finally {
      _sourceLock.writeLock().unlock();
    }
  }

  @MainThread
  public void removeEntry(final int index) {
    _sourceLock.writeLock().lock();
    try {
      _sourceList.remove(index);
    } finally {
      _sourceLock.writeLock().unlock();
    }
  }

  /**
   * @return a non-modifiable list of entries used in this source.
   */
  public List<Entry> getEntries() { return Collections.unmodifiableList(_sourceList); }

  public Entry getEntry(final int index) {

    _sourceLock.readLock().lock();
    try {
      if (index >= _sourceList.size() || index < 0) {
        return null;
      }
      return _sourceList.get(index);
    } finally {
      _sourceLock.readLock().unlock();
    }
  }

  /**
   * @return the intersection of all non-null sets returned from the sources contained herein.
   */
  @Override
  public Set<Tile> getValidTiles(final int clipmapLevel, final int tileX, final int tileY, final int numTilesX,
      final int numTilesY) throws Exception {
    Set<Tile> set;
    Set<Tile> rVal = null;

    _sourceLock.readLock().lock();
    try {
      for (final Entry src : _sourceList) {
        set = src.getSource() == null ? null
            : src.getSource().getValidTiles(clipmapLevel, tileX, tileY, numTilesX, numTilesY);
        if (set == null) {
          continue;
        }

        if (rVal == null) {
          rVal = new HashSet<>(set);
        } else {
          rVal.retainAll(set);
        }
      }
    } finally {
      _sourceLock.readLock().unlock();
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
    _sourceLock.readLock().lock();
    try {
      for (final Entry src : _sourceList) {
        set = src.getSource() == null ? null
            : src.getSource().getInvalidTiles(clipmapLevel, tileX, tileY, numTilesX, numTilesY);
        if (set == null) {
          continue;
        }

        if (rVal == null) {
          rVal = new HashSet<>(set);
        } else {
          rVal.addAll(set);
        }
      }
    } finally {
      _sourceLock.readLock().unlock();
    }
    return rVal;
  }

  @Override
  public float[] getTile(final int clipmapLevel, final Tile tile) throws Exception {
    float[] data = null;

    _sourceLock.readLock().lock();
    try {
      for (final Entry src : _sourceList) {
        if (src.getSource() == null) {
          continue;
        }

        final float[] srcData = src.getSource().getTile(clipmapLevel, tile);
        if (src.getCombine() == null) {
          data = srcData;
        } else {
          data = src.getCombine().apply(data, srcData);
        }
      }
    } finally {
      _sourceLock.readLock().unlock();
    }

    return data;
  }

}
