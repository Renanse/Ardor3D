/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.client;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.MathUtils;
import com.google.common.collect.Sets;

public abstract class AbstractGridCache implements Runnable {

    protected final int cacheSize;
    protected final int tileSize;
    protected final int dataSize;

    protected final CacheData[][] cache;
    protected final int destinationSize;

    protected final Set<TileLoadingData> currentTiles = Sets.newHashSet();
    protected Set<TileLoadingData> newThreadTiles = Sets.newHashSet();
    protected Set<TileLoadingData> backThreadTiles = Sets.newHashSet();

    protected final int clipmapLevel;
    protected final int requestedLevel;

    protected final Object SWAP_LOCK = new Object();
    protected int backCurrentTileX = Integer.MAX_VALUE;
    protected int backCurrentTileY = Integer.MAX_VALUE;
    protected boolean updated = false;

    protected final int TILELOCATOR_SLEEP = 250;

    // Debug
    protected boolean enableDebug = true;
    protected final Set<TileLoadingData> debugTiles = Sets.newHashSet();

    protected final ThreadPoolExecutor tileThreadService;

    protected DoubleBufferedList<Region> mailBox;

    protected Set<Tile> validTiles;

    protected boolean started = false;
    protected final int locatorSize = 20;
    protected Tile locatorTile = new Tile(Integer.MAX_VALUE, Integer.MAX_VALUE);

    protected boolean exit = false;

    protected AbstractGridCache(final int cacheSize, final int tileSize, final int destinationSize,
            final int clipmapLevel, final int requestedLevel, final ThreadPoolExecutor tileThreadService) {
        this.cacheSize = cacheSize;
        this.tileSize = tileSize;
        dataSize = tileSize * cacheSize;
        this.destinationSize = destinationSize;
        this.clipmapLevel = clipmapLevel;
        this.requestedLevel = requestedLevel;

        this.tileThreadService = tileThreadService;

        cache = new CacheData[cacheSize][cacheSize];
        for (int i = 0; i < cacheSize; i++) {
            for (int j = 0; j < cacheSize; j++) {
                cache[i][j] = new CacheData();
            }
        }
    }

    public void setCurrentPosition(final int x, final int y) {
        final int tileX = MathUtils.floor((float) x / tileSize);
        final int tileY = MathUtils.floor((float) y / tileSize);

        final int diffX = tileX - backCurrentTileX;
        final int diffY = tileY - backCurrentTileY;

        if (diffX == 0 && diffY == 0) {
            return;
        }

        synchronized (SWAP_LOCK) {
            backCurrentTileX = tileX;
            backCurrentTileY = tileY;

            final Set<TileLoadingData> newTiles = Sets.newHashSet();
            for (int i = 0; i < cacheSize; i++) {
                for (int j = 0; j < cacheSize; j++) {
                    final int sourceX = tileX + j - cacheSize / 2;
                    final int sourceY = tileY + i - cacheSize / 2;

                    final int destX = MathUtils.moduloPositive(sourceX, cacheSize);
                    final int destY = MathUtils.moduloPositive(sourceY, cacheSize);

                    newTiles.add(new TileLoadingData(this, new Tile(sourceX, sourceY), new Tile(destX, destY)));
                }
            }

            final Iterator<TileLoadingData> tileIterator = currentTiles.iterator();
            while (tileIterator.hasNext()) {
                final TileLoadingData data = tileIterator.next();

                if (!newTiles.contains(data)) {
                    cache[data.destTile.getX()][data.destTile.getY()].isValid = false;

                    data.isCancelled = true;
                    final Future<Boolean> future = data.future;
                    if (future != null && !future.isDone()) {
                        future.cancel(true);
                    }
                    tileIterator.remove();
                    if (backThreadTiles.contains(data)) {
                        backThreadTiles.remove(data);
                    }
                } else {
                    newTiles.remove(data);
                }
            }

            backThreadTiles.addAll(newTiles);
            updated = true;

            currentTiles.addAll(newTiles);
        }

        // Housekeeping - Wipe out future tasks that have already been cancelled.
        tileThreadService.purge();

        if (enableDebug) {
            synchronized (debugTiles) {
                debugTiles.clear();
                debugTiles.addAll(currentTiles);
            }
        }

        if (!started) {
            final Thread cacheThread = new Thread(this, "GridCacheUpdater-" + clipmapLevel);
            cacheThread.setDaemon(true);
            cacheThread.start();
            started = true;
        }
    }

    @Override
    public void run() {
        while (!exit) {
            while (!updated) {
                try {
                    TimeUnit.MILLISECONDS.sleep(TILELOCATOR_SLEEP);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }

            int tileX;
            int tileY;
            synchronized (SWAP_LOCK) {
                final Set<TileLoadingData> tmp = newThreadTiles;
                newThreadTiles = backThreadTiles;
                backThreadTiles = tmp;
                backThreadTiles.clear();

                tileX = backCurrentTileX;
                tileY = backCurrentTileY;

                updated = false;
            }

            if (isTileOutsideLocatorArea(tileX, tileY)) {
                validTiles = getValidTilesFromSource(requestedLevel, tileX - locatorSize / 2, tileY - locatorSize / 2,
                        locatorSize, locatorSize);
                locatorTile = new Tile(tileX, tileY);
            }

            final Iterator<TileLoadingData> tileIterator = newThreadTiles.iterator();
            while (tileIterator.hasNext()) {
                final TileLoadingData data = tileIterator.next();
                if (validTiles == null || validTiles.contains(data.sourceTile)) {
                    cache[data.destTile.getX()][data.destTile.getY()].isValid = false;
                    data.future = tileThreadService.submit(data);
                }
                tileIterator.remove();
            }
        }
    }

    public Set<Tile> handleUpdateRequests() {
        final Set<Tile> updateTiles = getInvalidTilesFromSource(requestedLevel, backCurrentTileX - cacheSize / 2,
                backCurrentTileY - cacheSize / 2, cacheSize, cacheSize);
        if (updateTiles == null || updateTiles.isEmpty()) {
            return null;
        }

        for (final Tile tile : updateTiles) {
            final int destX = MathUtils.moduloPositive(tile.getX(), cacheSize);
            final int destY = MathUtils.moduloPositive(tile.getY(), cacheSize);
            copyTileData(tile, destX, destY);
        }

        return updateTiles;
    }

    protected abstract boolean copyTileData(final Tile sourceTile, final int destX, final int destY);

    protected abstract Set<Tile> getValidTilesFromSource(final int clipmapLevel, final int tileX, final int tileY,
            int numTilesX, int numTilesY);

    protected abstract Set<Tile> getInvalidTilesFromSource(final int clipmapLevel, final int tileX, final int tileY,
            int numTilesX, int numTilesY);

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
            copyTiles = Sets.newHashSet(debugTiles);
        }
        return copyTiles;
    }

    public boolean isValid() {
        int nrValid = 0;
        for (final TileLoadingData data : currentTiles) {
            if (cache[data.destTile.getX()][data.destTile.getY()].isValid) {
                nrValid++;
            }
        }
        return nrValid != 0;
    }

    public void setMailBox(final DoubleBufferedList<Region> mailBox) {
        this.mailBox = mailBox;
    }

    public void shutdown() {
        exit = true;
        started = false;
    }

    public static class TileLoadingData implements Callable<Boolean> {
        private final AbstractGridCache sourceCache;

        public final Tile sourceTile;
        public final Tile destTile;

        public boolean isCancelled = false;
        public Future<Boolean> future;

        public enum State {
            init, loading, finished, cancelled, error
        }

        public State state = State.init;

        public TileLoadingData(final AbstractGridCache sourceCache, final Tile sourceTile, final Tile destTile) {
            this.sourceCache = sourceCache;
            this.sourceTile = sourceTile;
            this.destTile = destTile;
        }

        @Override
        public Boolean call() throws Exception {
            state = State.loading;

            if (isCancelled()) {
                state = State.cancelled;
                return false;
            }

            if (!sourceCache.copyTileData(sourceTile, destTile.getX(), destTile.getY())) {
                state = State.error;
                return false;
            }

            state = State.finished;
            sourceCache.cache[destTile.getX()][destTile.getY()].isValid = true;

            final int clipmapLevel = sourceCache.clipmapLevel;
            final int vertexDistance = MathUtils.pow2(clipmapLevel);
            final int tileSize = sourceCache.tileSize;
            final Region region = new Region(clipmapLevel, sourceTile.getX() * tileSize * vertexDistance,
                    sourceTile.getY() * tileSize * vertexDistance, tileSize * vertexDistance, tileSize * vertexDistance);

            final DoubleBufferedList<Region> mailBox = sourceCache.mailBox;
            if (mailBox != null) {
                mailBox.add(region);
            }

            return true;
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
            return "TileLoadingData [destTile=" + destTile + ", sourceTile=" + sourceTile + "]";
        }
    }

    public static class CacheData {
        public boolean isValid;

        public CacheData() {
            isValid = false;
        }
    }
}
