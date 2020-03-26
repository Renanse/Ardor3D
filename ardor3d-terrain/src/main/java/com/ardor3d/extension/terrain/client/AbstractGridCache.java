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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.PriorityExecutors.PriorityRunnable;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.MathUtils;

public abstract class AbstractGridCache {

    protected final int cacheSize;
    protected final int tileSize;
    protected final int dataSize;

    protected final CacheData[][] cache;
    protected final int destinationSize;

    protected final Set<TileLoadingData> currentTiles = new HashSet<>();
    protected Set<TileLoadingData> newThreadTiles = new HashSet<>();
    protected Set<TileLoadingData> backThreadTiles = new HashSet<>();

    protected final int meshClipIndex;
    protected final int dataClipIndex;
    protected final int vertexDistance;

    protected final Object SWAP_LOCK = new Object();
    protected int backCurrentTileX = Integer.MAX_VALUE;
    protected int backCurrentTileY = Integer.MAX_VALUE;
    protected boolean updated = false;

    // Debug
    protected boolean enableDebug = true;
    protected final Set<TileLoadingData> debugTiles = new HashSet<>();

    protected final ExecutorService tileThreadService;

    protected DoubleBufferedList<Region> mailBox;

    protected Set<Tile> validTiles;

    protected final int locatorSize = 20;
    protected Tile locatorTile = new Tile(Integer.MAX_VALUE, Integer.MAX_VALUE);

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

        synchronized (SWAP_LOCK) {
            backCurrentTileX = tileX;
            backCurrentTileY = tileY;

            // Gather all of the tiles in range of our new position
            final Set<TileLoadingData> newTiles = new HashSet<>();
            for (int i = 0; i < cacheSize; i++) {
                for (int j = 0; j < cacheSize; j++) {
                    final int sourceX = tileX + j - cacheSize / 2;
                    final int sourceY = tileY + i - cacheSize / 2;

                    final int destX = MathUtils.moduloPositive(sourceX, cacheSize);
                    final int destY = MathUtils.moduloPositive(sourceY, cacheSize);

                    newTiles.add(new TileLoadingData(this, new Tile(sourceX, sourceY), new Tile(destX, destY),
                            dataClipIndex));
                }
            }

            // Walk through tiles we are currently tracking status of. We think these are valid currently.
            final Iterator<TileLoadingData> tileIterator = currentTiles.iterator();
            while (tileIterator.hasNext()) {
                final TileLoadingData data = tileIterator.next();

                // Is this tile NOT in the new data set?
                if (!newTiles.contains(data)) {
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

                // If the tile IS in the new data set, we don't want to add it to current or backthread Tiles again
                else {
                    newTiles.remove(data);
                }
            }

            // add remaining tiles to backthread for next execution pass
            backThreadTiles.addAll(newTiles);
            updated = true;

            // add remaining tiles to our currentTiles for tracking here.
            currentTiles.addAll(newTiles);
        }

        // Sync our debug tiles if enabled
        if (enableDebug) {
            synchronized (debugTiles) {
                debugTiles.clear();
                debugTiles.addAll(currentTiles);
            }
        }
    }

    public void checkForUpdates() {
        if (!updated) {
            return;
        }

        int tileX;
        int tileY;
        synchronized (SWAP_LOCK) {
            // Swap our tile sets so we work on data accumulated recently.
            final Set<TileLoadingData> tmp = newThreadTiles;
            newThreadTiles = backThreadTiles;
            backThreadTiles = tmp;
            backThreadTiles.clear();

            tileX = backCurrentTileX;
            tileY = backCurrentTileY;

            updated = false;
        }

        if (isTileOutsideLocatorArea(tileX, tileY)) {
            validTiles = getValidTilesFromSource(tileX - locatorSize / 2, tileY - locatorSize / 2, locatorSize,
                    locatorSize);
            locatorTile = new Tile(tileX, tileY);
        }

        // walk through the accumulated tile data
        final Iterator<TileLoadingData> tileIterator = newThreadTiles.iterator();
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

    protected abstract boolean copyTileData(final Tile sourceTile, final int destX, final int destY);

    protected abstract Set<Tile> getValidTilesFromSource(final int tileX, final int tileY, int numTilesX,
            int numTilesY);

    protected abstract Set<Tile> getInvalidTilesFromSource(final int tileX, final int tileY, int numTilesX,
            int numTilesY);

    protected abstract AbstractGridCache getParentCache();

    protected boolean isTileOutsideLocatorArea(final int tileX, final int tileY) {
        final int locX = tileX - locatorTile.getX();
        final int locY = tileY - locatorTile.getY();
        final int halfSize = locatorSize / 2;
        return locX <= -halfSize + 1 || //
                locX >= +halfSize - 2 || //
                locY <= -halfSize + 1 || //
                locY >= +halfSize - 2;
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

    public void setMailBox(final DoubleBufferedList<Region> mailBox) {
        this.mailBox = mailBox;
    }

    public static class TileLoadingData implements Runnable {
        public final AbstractGridCache sourceCache;

        public final Tile sourceTile;
        public final Tile destTile;
        public final int index;

        public boolean isCancelled = false;
        public Future<?> future;

        public enum State {
            init, loading, finished, cancelled, error
        }

        public State state = State.init;

        public TileLoadingData(final AbstractGridCache sourceCache, final Tile sourceTile, final Tile destTile,
                final int index) {
            this.sourceCache = sourceCache;
            this.sourceTile = sourceTile;
            this.destTile = destTile;
            this.index = index;
        }

        @Override
        public void run() {
            state = State.loading;

            if (isCancelled()) {
                state = State.cancelled;
                return;
            }

            if (!sourceCache.copyTileData(sourceTile, destTile.getX(), destTile.getY())) {
                state = State.error;
                return;
            }

            state = State.finished;
            sourceCache.cache[destTile.getX()][destTile.getY()].isValid = true;

            final Region region = sourceCache.toRegion(sourceTile);
            final DoubleBufferedList<Region> mailBox = sourceCache.mailBox;
            if (mailBox != null) {
                mailBox.add(region);
            }

            return;
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
            return "TileLoadingData [destTile=" + destTile + ", sourceTile=" + sourceTile + ", clipIndex=" + index
                    + "]";
        }
    }

    public static class CacheData {
        public boolean isValid;

        public CacheData() {
            isValid = false;
        }
    }
}
