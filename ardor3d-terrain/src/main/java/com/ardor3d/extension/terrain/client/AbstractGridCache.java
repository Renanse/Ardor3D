/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.client;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.PriorityExecutors.PriorityRunnable;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.util.MathUtils;
import com.google.common.collect.Sets;

public abstract class AbstractGridCache {

  protected final int cacheSize;
  protected final int tileSize;
  protected final int dataSize;

  protected final CacheData[][] cache;
  protected final int destinationSize;

  protected final Set<TileLoadingData> currentTiles = Sets.newConcurrentHashSet();
  protected Set<TileLoadingData> newThreadTiles = Sets.newConcurrentHashSet();
  protected Set<TileLoadingData> backThreadTiles = Sets.newConcurrentHashSet();

  protected final int meshClipIndex;
  protected final int dataClipIndex;
  protected final int vertexDistance;

  // Guards a
  protected final Object SWAP_LOCK = new Object();

  protected int backCurrentTileX = Integer.MAX_VALUE;
  protected int backCurrentTileY = Integer.MAX_VALUE;
  protected boolean updated = false;

  // Debug
  protected boolean enableDebug = true;
  protected final Set<TileLoadingData> debugTiles = Sets.newConcurrentHashSet();

  protected final ExecutorService tileThreadService;

  protected DoubleBufferedList<Region> mailBox;

  protected Set<Tile> validTiles;

  protected final int locatorSize = 20;
  protected Tile locatorTile = new Tile(Integer.MAX_VALUE, Integer.MAX_VALUE);

  public enum State {
    init, loading, finished, cancelled, error, requeue
  }

  protected AbstractGridCache(final int cacheSize, final int tileSize, final int destinationSize,
    final int meshClipIndex, final int dataClipIndex, final int vertexDistance,
    final ExecutorService tileThreadService) {
    this.cacheSize = cacheSize;
    this.tileSize = tileSize;
    dataSize = tileSize * cacheSize;
    this.destinationSize = destinationSize;
    this.meshClipIndex = meshClipIndex;
    this.dataClipIndex = dataClipIndex;
    this.vertexDistance = vertexDistance;

    this.tileThreadService = tileThreadService;

    cache = new CacheData[cacheSize][cacheSize];
    for (int i = 0; i < cacheSize; i++) {
      for (int j = 0; j < cacheSize; j++) {
        cache[i][j] = new CacheData();
      }
    }
  }

  public Region toRegion(final Tile sourceTile) {
    return new Region(meshClipIndex, sourceTile.getX() * tileSize * vertexDistance,
        sourceTile.getY() * tileSize * vertexDistance, tileSize * vertexDistance, tileSize * vertexDistance);
  }

  public void setCurrentPosition(final int x, final int y) {
    final int tileX = MathUtils.floor((float) x / tileSize);
    final int tileY = MathUtils.floor((float) y / tileSize);

    // if we have not moved to a new center tile, ignore the position change.
    if (tileX == backCurrentTileX && tileY == backCurrentTileY) {
      return;
    }

    backCurrentTileX = tileX;
    backCurrentTileY = tileY;

    // Gather all of the tiles in range of our new position
    final var newTiles = new HashSet<TileLoadingData>();
    for (int i = 0; i < cacheSize; i++) {
      for (int j = 0; j < cacheSize; j++) {
        final int sourceX = tileX + j - cacheSize / 2;
        final int sourceY = tileY + i - cacheSize / 2;

        newTiles.add(new TileLoadingData(this, sourceX, sourceY, cacheSize, dataClipIndex));
      }
    }

    // Walk through tiles we are currently tracking status of. We think these are valid currently.
    final var tileIterator = currentTiles.iterator();
    while (tileIterator.hasNext()) {
      final var data = tileIterator.next();

      // Is this tile NOT in the new data set?
      if (!newTiles.contains(data) || data.state == State.requeue) {
        // set that destination tile as invalid
        cache[data.destTile.getX()][data.destTile.getY()].isValid = false;

        // try to cancel the tile's loading if possible
        data.isCancelled = true;
        final Future<?> future = data.future;
        if (future != null && !future.isDone()) {
          future.cancel(false);
        }

        // remove the tile from current
        tileIterator.remove();

        // remove the tile from the back accumulator list, since we don't need it anymore.
        if (backThreadTiles.contains(data)) {
          backThreadTiles.remove(data);
        }
      }

      // If the tile IS in the new data set and doesn't need retry,
      // we don't want to add it to current or backthread Tiles again
      else {
        newTiles.remove(data);
      }
    }

    // add remaining tiles to backthread for next execution pass
    backThreadTiles.addAll(newTiles);
    updated = true;

    // add remaining tiles to our currentTiles for tracking here.
    currentTiles.addAll(newTiles);

    // Sync our debug tiles if enabled
    if (enableDebug) {
      synchronized (debugTiles) {
        debugTiles.clear();
        debugTiles.addAll(currentTiles);
      }
    }
  }

  public void checkForUpdates() {
    // check for stalled items and resubmit
    var tileIterator = currentTiles.iterator();
    while (tileIterator.hasNext()) {
      final TileLoadingData data = tileIterator.next();
      if (data.state == State.requeue) {
        data.state = State.init;
        data.future = null;
        backThreadTiles.add(data);
      }
    }

    if (!updated) {
      return;
    }

    int tileX;
    int tileY;

    final Set<TileLoadingData> toProcess = new HashSet<>();
    synchronized (SWAP_LOCK) {
      // Swap our tile sets so we work on data accumulated recently.
      final Set<TileLoadingData> tmp = newThreadTiles;
      newThreadTiles = backThreadTiles;
      backThreadTiles = tmp;
      backThreadTiles.clear();

      toProcess.addAll(newThreadTiles);
    }

    tileX = backCurrentTileX;
    tileY = backCurrentTileY;

    updated = false;

    if (shouldMoveLocator(tileX, tileY)) {
      validTiles = getValidTilesFromSource(tileX - locatorSize / 2, tileY - locatorSize / 2, locatorSize, locatorSize);
      locatorTile = new Tile(tileX, tileY);
    }

    // walk through the accumulated tile data
    tileIterator = toProcess.iterator();
    while (tileIterator.hasNext()) {
      final TileLoadingData data = tileIterator.next();

      // check if the given tile is valid and should be processed
      if (validTiles == null || validTiles.contains(data.sourceTile)) {
        cache[data.destTile.getX()][data.destTile.getY()].isValid = false;
        int priority = Math.abs(data.sourceTile.getX() - tileX) + Math.abs(data.sourceTile.getY() - tileY);
        if (priority <= 3) {
          priority = 100 * meshClipIndex;
        } else {
          priority = 2 * meshClipIndex - priority;
        }

        data.future = tileThreadService.submit(PriorityRunnable.of(data, priority));
      }
      tileIterator.remove();
    }
  }

  protected abstract State copyTileData(final Tile sourceTile, final int destX, final int destY);

  protected abstract Set<Tile> getValidTilesFromSource(final int tileX, final int tileY, int numTilesX, int numTilesY);

  protected abstract Set<Tile> getInvalidTilesFromSource(final int tileX, final int tileY, int numTilesX,
      int numTilesY);

  protected abstract AbstractGridCache getParentCache();

  /**
   * Check if a given tile coordinate is far enough away from the center of the locator area that we
   * should re-center things.
   *
   * @param tileX
   * @param tileY
   * @return
   */
  protected boolean shouldMoveLocator(final int tileX, final int tileY) {
    // is the given tile outside of our locator area?
    // our destination includes padding of 4-5 units, so we check if we are away from our old center by
    // that amount.
    final int offsetX = Math.abs(tileX - locatorTile.getX());
    final int offsetY = Math.abs(tileY - locatorTile.getY());
    return offsetX > 2 || offsetY > 2;
  }

  public Set<TileLoadingData> getDebugTiles() {
    Set<TileLoadingData> copyTiles = null;
    synchronized (debugTiles) {
      copyTiles = new HashSet<>(debugTiles);
    }
    return copyTiles;
  }

  public boolean isValid() {
    for (final TileLoadingData data : currentTiles) {
      if (cache[data.destTile.getX()][data.destTile.getY()].isValid) {
        return true;
      }
    }
    return false;
  }

  public void setMailBox(final DoubleBufferedList<Region> mailBox) { this.mailBox = mailBox; }

  public static class TileLoadingData implements Runnable {
    public final AbstractGridCache sourceCache;

    public final Tile sourceTile;
    public final Tile destTile;
    public final int index;

    public boolean isCancelled = false;
    public static long maxLoadingTime = 15 * 1000L;
    public Future<?> future;

    public State state = State.init;

    public TileLoadingData(final AbstractGridCache sourceCache, final int sourceX, final int sourceY,
      final int cacheSize, final int index) {
      this.sourceCache = sourceCache;
      sourceTile = new Tile(sourceX, sourceY);

      final int destX = MathUtils.moduloPositive(sourceX, cacheSize);
      final int destY = MathUtils.moduloPositive(sourceY, cacheSize);
      destTile = new Tile(destX, destY);

      this.index = index;
    }

    @Override
    public void run() {
      state = State.loading;

      // catch cancellation and bail immediately.
      if (isCancelled()) {
        state = State.cancelled;
        return;
      }

      // ask underlying system for data. We need to poll since the source may not be ready, or may need
      // time to load.
      final var copyState = sourceCache.copyTileData(sourceTile, destTile.getX(), destTile.getY());

      switch (copyState) {
        case init:
        case loading:
        case requeue:
          // source is not ready. Reschedule.
          state = State.requeue;
          return;
        case cancelled:
          // source was asked to cancel.
          state = State.cancelled;
          return;
        case error:
          state = State.error;
          return;
        case finished:
          state = State.finished;
          final Region region = sourceCache.toRegion(sourceTile);
          final DoubleBufferedList<Region> mailBox = sourceCache.mailBox;
          if (mailBox != null) {
            mailBox.add(region);
          }
          sourceCache.cache[destTile.getX()][destTile.getY()].isValid = true;
        default:
          return;
      }
    }

    protected boolean isCancelled() {
      if (future != null && future.isCancelled()) {
        return true;
      } else if (isCancelled) {
        return true;
      } else if (Thread.interrupted()) {
        return true;
      }
      return false;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (destTile == null ? 0 : destTile.hashCode());
      result = prime * result + (sourceTile == null ? 0 : sourceTile.hashCode());
      return result;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof TileLoadingData)) {
        return false;
      }
      final TileLoadingData other = (TileLoadingData) obj;
      if (destTile == null) {
        if (other.destTile != null) {
          return false;
        }
      } else if (!destTile.equals(other.destTile)) {
        return false;
      }
      if (sourceTile == null) {
        if (other.sourceTile != null) {
          return false;
        }
      } else if (!sourceTile.equals(other.sourceTile)) {
        return false;
      }
      return true;
    }

    @Override
    public String toString() {
      return "TileLoadingData [destTile=" + destTile + ", sourceTile=" + sourceTile + ", clipIndex=" + index + "]";
    }
  }

  public static class CacheData {
    public boolean isValid;

    public CacheData() {
      isValid = false;
    }
  }
}
