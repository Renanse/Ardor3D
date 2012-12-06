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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.google.common.collect.Sets;

/**
 * Special tile/grid based cache for terrain data
 */
public class TerrainGridCache implements TerrainCache, Runnable {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TerrainGridCache.class.getName());

    private final TerrainSource source;

    private final TerrainCache parentCache;

    private final int cacheSize;
    private final int tileSize;
    private final int dataSize;

    private final float[] data;
    private final CacheData[][] cache;
    private final int destinationSize;

    private final int clipmapLevel;
    private final int requestedLevel;

    private final Set<TileLoadingData> currentTiles = Sets.newHashSet();
    private Set<TileLoadingData> newThreadTiles = Sets.newHashSet();
    private Set<TileLoadingData> backThreadTiles = Sets.newHashSet();
    private final Object SWAP_LOCK = new Object();
    private int backCurrentTileX = Integer.MAX_VALUE;
    private int backCurrentTileY = Integer.MAX_VALUE;
    private boolean updated = false;

    private final TerrainConfiguration terrainConfiguration;

    private final int vertexDistance;
    private final float heightScale;

    private final int TILELOCATOR_SLEEP = 100;

    private boolean exit = false;

    private final boolean enableDebug = true;
    private final Set<TileLoadingData> debugTiles = Sets.newHashSet();

    public Set<TileLoadingData> getDebugTiles() {
        Set<TileLoadingData> copyTiles = null;
        synchronized (debugTiles) {
            copyTiles = Sets.newHashSet(debugTiles);
        }
        return copyTiles;
    }

    private final ThreadPoolExecutor tileThreadService;

    private DoubleBufferedList<Region> mailBox;

    private Set<Tile> validTiles;

    public TerrainGridCache(final TerrainCache parentCache, final int cacheSize, final TerrainSource source,
            final int tileSize, final int destinationSize, final TerrainConfiguration terrainConfiguration,
            final int clipmapLevel, final int requestedLevel, final ThreadPoolExecutor tileThreadService) {
        this.parentCache = parentCache;
        this.cacheSize = cacheSize;
        this.source = source;
        this.tileSize = tileSize;
        dataSize = tileSize * cacheSize;
        this.destinationSize = destinationSize;
        heightScale = terrainConfiguration.getScale().getYf();
        this.terrainConfiguration = terrainConfiguration;
        this.clipmapLevel = clipmapLevel;
        this.requestedLevel = requestedLevel;

        this.tileThreadService = tileThreadService;
        // tileThreadService = new ThreadPoolExecutor(nrThreads, nrThreads, 0L, TimeUnit.MILLISECONDS,
        // new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder()
        // .setThreadFactory(Executors.defaultThreadFactory()).setDaemon(true)
        // .setNameFormat("TerrainTileThread-%s").setPriority(Thread.MIN_PRIORITY).build());

        data = new float[dataSize * dataSize];

        cache = new CacheData[cacheSize][cacheSize];
        for (int i = 0; i < cacheSize; i++) {
            for (int j = 0; j < cacheSize; j++) {
                cache[i][j] = new CacheData();
            }
        }

        vertexDistance = (int) Math.pow(2, clipmapLevel);
    }

    private boolean started = false;
    private final int locatorSize = 20;
    private Tile locatorTile = new Tile(Integer.MAX_VALUE, Integer.MAX_VALUE);

    @Override
    public Set<Tile> handleUpdateRequests() {
        Set<Tile> updateTiles;
        try {
            updateTiles = source.getInvalidTiles(requestedLevel, backCurrentTileX - cacheSize / 2, backCurrentTileY
                    - cacheSize / 2, cacheSize, cacheSize);
            if (updateTiles == null || updateTiles.isEmpty()) {
                return null;
            }
        } catch (final Exception e) {
            logger.log(Level.WARNING, "Exception processing updates", e);
            return null;
        }

        for (final Tile tile : updateTiles) {
            float[] sourceData;
            try {
                sourceData = source.getTile(requestedLevel, tile);
            } catch (final InterruptedException e) {
                // XXX: Loading can be interrupted
                return null;
            } catch (final Exception e) {
                logger.log(Level.WARNING, "Exception getting tile", e);
                return null;
            }

            if (sourceData == null) {
                continue;
            }

            final int destX = MathUtils.moduloPositive(tile.getX(), cacheSize);
            final int destY = MathUtils.moduloPositive(tile.getY(), cacheSize);

            final int offset = destY * tileSize * dataSize + destX * tileSize;
            for (int y = 0; y < tileSize; y++) {
                System.arraycopy(sourceData, y * tileSize, data, offset + y * dataSize, tileSize);
            }
        }

        return updateTiles;
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

                    newTiles.add(new TileLoadingData(mailBox, new Tile(sourceX, sourceY), new Tile(destX, destY),
                            source, cache, data, tileSize, dataSize, clipmapLevel, requestedLevel));
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

        tileThreadService.purge();

        if (enableDebug) {
            synchronized (debugTiles) {
                debugTiles.clear();
                debugTiles.addAll(currentTiles);
            }
        }

        if (!started) {
            final Thread terrainCacheThread = new Thread(this, "TerrainGridCache-" + clipmapLevel);
            terrainCacheThread.setDaemon(true);
            terrainCacheThread.start();
            started = true;
        }
    }

    @Override
    public void run() {
        while (!exit) {
            try {
                Thread.sleep(TILELOCATOR_SLEEP);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }

            int tileX;
            int tileY;
            boolean needsUpdate = false;
            synchronized (SWAP_LOCK) {
                final Set<TileLoadingData> tmp = newThreadTiles;
                newThreadTiles = backThreadTiles;
                backThreadTiles = tmp;
                backThreadTiles.clear();

                tileX = backCurrentTileX;
                tileY = backCurrentTileY;

                needsUpdate = updated;
                updated = false;
            }

            if (needsUpdate) {
                if (tileX <= locatorTile.getX() - locatorSize / 2 + 1
                        || tileX >= locatorTile.getX() + locatorSize / 2 - 2
                        || tileY <= locatorTile.getY() - locatorSize / 2 + 1
                        || tileY >= locatorTile.getY() + locatorSize / 2 - 2) {
                    try {
                        validTiles = source.getValidTiles(requestedLevel, tileX - locatorSize / 2, tileY - locatorSize
                                / 2, locatorSize, locatorSize);
                    } catch (final Exception e) {
                        logger.log(Level.WARNING, "Exception getting source info", e);
                    }
                    locatorTile = new Tile(tileX, tileY);
                }

                threadedUpdateTiles();
            }
        }
    }

    private void threadedUpdateTiles() {
        final Iterator<TileLoadingData> tileIterator = newThreadTiles.iterator();
        while (tileIterator.hasNext()) {
            final TileLoadingData data = tileIterator.next();
            if (validTiles == null || validTiles.contains(data.sourceTile)) {
                cache[data.destTile.getX()][data.destTile.getY()].isValid = false;

                final Future<Boolean> future = tileThreadService.submit(data);
                data.future = future;
            }
            tileIterator.remove();
        }
    }

    @Override
    public float getHeight(final int x, final int z) {
        int tileX = MathUtils.floor((float) x / tileSize);
        int tileY = MathUtils.floor((float) z / tileSize);
        final CacheData tileData;
        final int cacheMin = -cacheSize / 2, cacheMax = cacheSize / 2 - (cacheSize % 2 == 0 ? 1 : 0);
        if (tileX < cacheMin || tileX > cacheMax || tileY < cacheMin || tileY > cacheMax) {
            tileData = null;
        } else {
            tileX = MathUtils.moduloPositive(tileX, cacheSize);
            tileY = MathUtils.moduloPositive(tileY, cacheSize);
            tileData = cache[tileX][tileY];
        }

        if (tileData == null || !tileData.isValid) {
            if (parentCache != null) {
                if (x % 2 == 0 && z % 2 == 0) {
                    return parentCache.getHeight(x / 2, z / 2);
                } else {
                    return parentCache.getSubHeight(x / 2f, z / 2f);
                }
            } else {
                return terrainConfiguration.getHeightRangeMin();
            }
        } else {
            final int dataX = MathUtils.moduloPositive(x, dataSize);
            final int dataY = MathUtils.moduloPositive(z, dataSize);

            final float min = terrainConfiguration.getHeightRangeMin();
            final float max = terrainConfiguration.getHeightRangeMax();
            final float height = data[dataY * dataSize + dataX];
            if (height < min || height > max) {
                return min;
            }

            return height * heightScale;
        }
    }

    @Override
    public float getSubHeight(final float x, final float z) {
        int tileX = MathUtils.floor(x / tileSize);
        int tileY = MathUtils.floor(z / tileSize);
        final CacheData tileData;
        final int min = -cacheSize / 2, max = cacheSize / 2 - (cacheSize % 2 == 0 ? 1 : 0);
        if (tileX < min || tileX > max || tileY < min || tileY > max) {
            tileData = null;
        } else {
            tileX = MathUtils.moduloPositive(tileX, cacheSize);
            tileY = MathUtils.moduloPositive(tileY, cacheSize);
            tileData = cache[tileX][tileY];
        }

        if (tileData == null || !tileData.isValid) {
            if (parentCache != null) {
                return parentCache.getSubHeight(x / 2f, z / 2f);
            } else {
                return terrainConfiguration.getHeightRangeMin();
            }
        } else {
            final double col = MathUtils.floor(x);
            final double row = MathUtils.floor(z);

            final double intOnX = x - col, intOnZ = z - row;

            final double col1 = col + 1;
            final double row1 = row + 1;

            final double topLeft = getHeight((int) col, (int) row);
            final double topRight = getHeight((int) col1, (int) row);
            final double bottomLeft = getHeight((int) col, (int) row1);
            final double bottomRight = getHeight((int) col1, (int) row1);

            return (float) MathUtils.lerp(intOnZ, MathUtils.lerp(intOnX, topLeft, topRight),
                    MathUtils.lerp(intOnX, bottomLeft, bottomRight));
        }
    }

    @Override
    public void updateRegion(final FloatBuffer destinationData, final int sourceX, final int sourceY, final int width,
            final int height) {
        for (int z = 0; z < height; z++) {
            final int currentZ = sourceY + z;
            for (int x = 0; x < width; x++) {
                final int currentX = sourceX + x;

                final float cacheHeight = getHeight(currentX, currentZ);

                final int destX = MathUtils.moduloPositive(currentX, destinationSize);
                final int destY = MathUtils.moduloPositive(currentZ, destinationSize);
                final int indexDest = (destY * destinationSize + destX) * ClipmapLevel.VERT_SIZE;

                destinationData.put(indexDest + 0, currentX * vertexDistance); // x
                destinationData.put(indexDest + 1, cacheHeight); // y
                destinationData.put(indexDest + 2, currentZ * vertexDistance); // z

                if (parentCache == null) {
                    destinationData.put(indexDest + 3, cacheHeight); // w
                } else {
                    final int coarseX1 = (currentX < 0 ? currentX - 1 : currentX) / 2;
                    int coarseZ1 = (currentZ < 0 ? currentZ - 1 : currentZ) / 2;

                    final boolean onGridX = currentX % 2 == 0;
                    final boolean onGridZ = currentZ % 2 == 0;

                    if (onGridX && onGridZ) {
                        final float coarseHeight = parentCache.getHeight(coarseX1, coarseZ1);
                        destinationData.put(indexDest + 3, coarseHeight); // w
                    } else {
                        int coarseX2 = coarseX1;
                        int coarseZ2 = coarseZ1;
                        if (!onGridX && onGridZ) {
                            coarseX2++;
                        } else if (onGridX && !onGridZ) {
                            coarseZ2++;
                        } else if (!onGridX && !onGridZ) {
                            coarseX2++;
                            coarseZ1++;
                        }

                        final float coarser1 = parentCache.getHeight(coarseX1, coarseZ1);
                        final float coarser2 = parentCache.getHeight(coarseX2, coarseZ2);

                        // Apply the median of the coarser heightvalues to the W
                        // value
                        destinationData.put(indexDest + 3, (coarser1 + coarser2) * 0.5f); // w
                    }
                }
            }
        }
    }

    @Override
    public void getEyeCoords(final float[] destinationData, final int sourceX, final int sourceY,
            final ReadOnlyVector3 eyePos) {
        for (int z = 0; z < 2; z++) {
            final int currentZ = sourceY + z;
            for (int x = 0; x < 2; x++) {
                final int currentX = sourceX + x;

                final float cacheHeight = getHeight(currentX, currentZ);

                final int indexDest = z * 8 + x * 4;
                destinationData[indexDest + 0] = currentX * vertexDistance - eyePos.getXf(); // x
                destinationData[indexDest + 1] = currentZ * vertexDistance - eyePos.getZf(); // z
                destinationData[indexDest + 2] = cacheHeight; // h

                if (parentCache == null) {
                    destinationData[indexDest + 3] = cacheHeight; // w
                } else {
                    final int coarseX1 = (currentX < 0 ? currentX - 1 : currentX) / 2;
                    int coarseZ1 = (currentZ < 0 ? currentZ - 1 : currentZ) / 2;

                    final boolean onGridX = currentX % 2 == 0;
                    final boolean onGridZ = currentZ % 2 == 0;

                    if (onGridX && onGridZ) {
                        final float coarseHeight = parentCache.getHeight(coarseX1, coarseZ1);
                        destinationData[indexDest + 3] = coarseHeight; // w
                    } else {
                        int coarseX2 = coarseX1;
                        int coarseZ2 = coarseZ1;
                        if (!onGridX && onGridZ) {
                            coarseX2++;
                        } else if (onGridX && !onGridZ) {
                            coarseZ2++;
                        } else if (!onGridX && !onGridZ) {
                            coarseX2++;
                            coarseZ1++;
                        }

                        final float coarser1 = parentCache.getHeight(coarseX1, coarseZ1);
                        final float coarser2 = parentCache.getHeight(coarseX2, coarseZ2);

                        // Apply the median of the coarser heightvalues to the W
                        // value
                        destinationData[indexDest + 3] = (coarser1 + coarser2) * 0.5f; // w
                    }
                }
            }
        }
    }

    public static class TileLoadingData implements Callable<Boolean> {
        private final TerrainSource source;
        private final float[] data;
        private final CacheData[][] cache;
        private final int tileSize;
        private final int dataSize;

        private final int clipmapLevel;
        private final int requestedLevel;

        private final DoubleBufferedList<Region> mailBox;

        public final Tile sourceTile;
        public final Tile destTile;

        public boolean isCancelled = false;
        public Future<Boolean> future;

        public enum State {
            init, loading, finished
        }

        public State state = State.init;

        public TileLoadingData(final DoubleBufferedList<Region> mailBox, final Tile sourceTile, final Tile destTile,
                final TerrainSource source, final CacheData[][] cache, final float[] data, final int tileSize,
                final int dataSize, final int clipmapLevel, final int requestedLevel) {
            this.mailBox = mailBox;

            this.sourceTile = sourceTile;
            this.destTile = destTile;

            this.source = source;
            this.cache = cache;
            this.data = data;
            this.tileSize = tileSize;
            this.dataSize = dataSize;

            this.clipmapLevel = clipmapLevel;
            this.requestedLevel = requestedLevel;
        }

        @Override
        public Boolean call() throws Exception {
            state = State.loading;

            if (isCancelled()) {
                return false;
            }

            float[] sourceData = null;
            try {
                sourceData = source.getTile(requestedLevel, sourceTile);
            } catch (final InterruptedException e) {
                // XXX: Loading can be interrupted
                return false;
            } catch (final Throwable t) {
                t.printStackTrace();
            }

            if (sourceData == null || isCancelled()) {
                return false;
            }

            final int offset = destTile.getY() * tileSize * dataSize + destTile.getX() * tileSize;
            for (int y = 0; y < tileSize; y++) {
                System.arraycopy(sourceData, y * tileSize, data, offset + y * dataSize, tileSize);
            }

            if (isCancelled()) {
                return false;
            }

            state = State.finished;
            cache[destTile.getX()][destTile.getY()].isValid = true;

            final int vertexDistance = MathUtils.pow2(clipmapLevel);
            final Region region = new Region(clipmapLevel, sourceTile.getX() * tileSize * vertexDistance,
                    sourceTile.getY() * tileSize * vertexDistance, tileSize * vertexDistance, tileSize * vertexDistance);
            if (mailBox != null) {
                mailBox.add(region);
            }

            return true;
        }

        private boolean isCancelled() {
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
}
