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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.terrain.client.functions.CacheFunctionUtil;
import com.ardor3d.extension.terrain.client.functions.SourceCacheFunction;
import com.ardor3d.extension.terrain.util.DoubleBufferedList;
import com.ardor3d.extension.terrain.util.IntColorUtils;
import com.ardor3d.extension.terrain.util.Region;
import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.MathUtils;
import com.google.common.collect.Sets;

/**
 * Special tile/grid based cache for texture data
 */
public class TextureGridCache implements TextureCache, Runnable {
    /** The Constant logger. */
    private static final Logger logger = Logger.getLogger(TextureGridCache.class.getName());

    private final TextureSource source;

    private final TextureCache parentCache;

    private final int cacheSize;
    private final int tileSize;
    private final int dataSize;

    private final byte[] data;
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

    private final TextureConfiguration textureConfiguration;
    private SourceCacheFunction function;

    private final int TILELOCATOR_SLEEP = 100;

    private final boolean useAlpha;
    private final int colorBits;

    // Debug
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

    public TextureGridCache(final TextureCache parentCache, final int cacheSize, final TextureSource source,
            final int tileSize, final int destinationSize, final TextureConfiguration textureConfiguration,
            final int clipmapLevel, final int requestedLevel, final ThreadPoolExecutor tileThreadService) {
        this.parentCache = parentCache;
        this.cacheSize = cacheSize;
        this.source = source;
        this.tileSize = tileSize;
        dataSize = tileSize * cacheSize;
        this.destinationSize = destinationSize;
        this.textureConfiguration = textureConfiguration;
        useAlpha = textureConfiguration.isUseAlpha();
        colorBits = useAlpha ? 4 : 3;
        this.clipmapLevel = clipmapLevel;
        this.requestedLevel = requestedLevel;

        this.tileThreadService = tileThreadService;
        // tileThreadService = new ThreadPoolExecutor(nrThreads, nrThreads, 0L, TimeUnit.MILLISECONDS,
        // new LinkedBlockingQueue<Runnable>(), new ThreadFactoryBuilder()
        // .setThreadFactory(Executors.defaultThreadFactory()).setDaemon(true)
        // .setNameFormat("TextureTileThread-%s").setPriority(Thread.MIN_PRIORITY).build());

        data = new byte[dataSize * dataSize * colorBits];
        for (int i = 0; i < dataSize * dataSize * colorBits; i++) {
            data[i] = (byte) 1;
        }

        cache = new CacheData[cacheSize][cacheSize];
        for (int i = 0; i < cacheSize; i++) {
            for (int j = 0; j < cacheSize; j++) {
                cache[i][j] = new CacheData();
            }
        }
    }

    private boolean started = false;
    private final int locatorSize = 20;
    private Tile locatorTile = new Tile(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private boolean exit = false;

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
            ByteBuffer sourceData;
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

            final TextureStoreFormat format = textureConfiguration.getTextureDataType(source.getContributorId(
                    requestedLevel, tile));
            CacheFunctionUtil.applyFunction(useAlpha, function, sourceData, data, destX, destY, format, tileSize,
                    dataSize);
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
                            source, cache, data, tileSize, dataSize, function, textureConfiguration, useAlpha,
                            clipmapLevel, requestedLevel));
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
            final Thread textureCacheThread = new Thread(this, "TextureGridCache-" + clipmapLevel);
            textureCacheThread.setDaemon(true);
            textureCacheThread.start();
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
    public int getColor(final int x, final int z) {
        int tileX = MathUtils.floor((float) x / tileSize);
        int tileY = MathUtils.floor((float) z / tileSize);
        tileX = MathUtils.moduloPositive(tileX, cacheSize);
        tileY = MathUtils.moduloPositive(tileY, cacheSize);
        final CacheData tileData = cache[tileX][tileY];

        if (!tileData.isValid) {
            if (parentCache != null) {
                if (x % 2 == 0 && z % 2 == 0) {
                    return parentCache.getColor(x / 2, z / 2);
                } else {
                    return parentCache.getSubColor(x / 2f, z / 2f);
                }
            } else {
                return 0;
            }
        } else {
            final int dataX = MathUtils.moduloPositive(x, dataSize);
            final int dataY = MathUtils.moduloPositive(z, dataSize);
            final int sourceIndex = (dataY * dataSize + dataX) * colorBits;

            int color = 0;
            if (useAlpha) {
                color = IntColorUtils.getColor(data[sourceIndex + 0], data[sourceIndex + 1], data[sourceIndex + 2],
                        data[sourceIndex + 3]);
            } else {
                color = IntColorUtils.getColor(data[sourceIndex + 0], data[sourceIndex + 1], data[sourceIndex + 2],
                        (byte) 0);
            }

            // if (rgbStore.r == 0 && rgbStore.g == 0 && rgbStore.b == 0) {
            // if (parentCache != null) {
            // if (x % 2 == 0 && z % 2 == 0) {
            // return parentCache.getRGB(x / 2, z / 2, rgbStore);
            // } else {
            // return parentCache.getSubRGB(x / 2f, z / 2f, rgbStore);
            // }
            // } else {
            // return minRGB;
            // }
            // }

            return color;
        }
    }

    @Override
    public int getSubColor(final float x, final float z) {
        int tileX = MathUtils.floor(x / tileSize);
        int tileY = MathUtils.floor(z / tileSize);
        tileX = MathUtils.moduloPositive(tileX, cacheSize);
        tileY = MathUtils.moduloPositive(tileY, cacheSize);
        final CacheData tileData = cache[tileX][tileY];

        if (!tileData.isValid) {
            if (parentCache != null) {
                return parentCache.getSubColor(x / 2f, z / 2f);
            } else {
                return 0;
            }
        } else {
            final int col = MathUtils.floor(x);
            final int row = MathUtils.floor(z);
            final double intOnX = x - col;
            final double intOnZ = z - row;

            final int topLeft = getColor(col, row);
            final int topRight = getColor(col + 1, row);
            final int top = IntColorUtils.lerp(intOnX, topLeft, topRight);

            final int bottomLeft = getColor(col, row + 1);
            final int bottomRight = getColor(col + 1, row + 1);
            final int bottom = IntColorUtils.lerp(intOnX, bottomLeft, bottomRight);

            return IntColorUtils.lerp(intOnZ, top, bottom);
        }
    }

    @Override
    public void updateRegion(final ByteBuffer destinationData, final int sourceX, final int sourceY, final int destX,
            final int destY, final int width, final int height) {
        final byte[] rgbArray = new byte[width * colorBits];
        for (int z = 0; z < height; z++) {
            final int currentSourceZ = sourceY + z;
            final int currentDestZ = destY + z;
            final int dataY = MathUtils.moduloPositive(currentDestZ, destinationSize);

            for (int x = 0; x < width; x++) {
                final int currentSourceX = sourceX + x;
                final int color = getColor(currentSourceX, currentSourceZ);

                final int index = x * colorBits;
                rgbArray[index + 0] = (byte) (color >> 24 & 0xFF);
                rgbArray[index + 1] = (byte) (color >> 16 & 0xFF);
                rgbArray[index + 2] = (byte) (color >> 8 & 0xFF);
                if (useAlpha) {
                    rgbArray[index + 3] = (byte) (color & 0xFF);
                }
            }

            final int dataX = MathUtils.moduloPositive(destX, destinationSize);
            if (dataX + width > destinationSize) {
                final int destIndex = dataY * destinationSize * colorBits;

                destinationData.position(destIndex + dataX * colorBits);
                destinationData.put(rgbArray, 0, (destinationSize - dataX) * colorBits);

                destinationData.position(destIndex);
                destinationData.put(rgbArray, (destinationSize - dataX) * colorBits, (dataX + width - destinationSize)
                        * colorBits);
            } else {
                final int destIndex = (dataY * destinationSize + dataX) * colorBits;
                destinationData.position(destIndex);
                destinationData.put(rgbArray);
            }
        }
    }

    public static class TileLoadingData implements Callable<Boolean> {
        private final TextureSource source;
        private final byte[] data;
        private final CacheData[][] cache;
        private final int tileSize;
        private final int dataSize;

        private final SourceCacheFunction function;
        private final TextureConfiguration textureConfiguration;
        private final boolean useAlpha;

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
                final TextureSource source, final CacheData[][] cache, final byte[] data, final int tileSize,
                final int dataSize, final SourceCacheFunction function,
                final TextureConfiguration textureConfiguration, final boolean useAlpha, final int clipmapLevel,
                final int requestedLevel) {
            this.mailBox = mailBox;

            this.sourceTile = sourceTile;
            this.destTile = destTile;

            this.source = source;
            this.cache = cache;
            this.data = data;
            this.tileSize = tileSize;
            this.dataSize = dataSize;

            this.function = function;
            this.textureConfiguration = textureConfiguration;
            this.useAlpha = useAlpha;

            this.clipmapLevel = clipmapLevel;
            this.requestedLevel = requestedLevel;
        }

        @Override
        public Boolean call() throws Exception {
            state = State.loading;

            if (isCancelled()) {
                return false;
            }

            ByteBuffer sourceData = null;
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

            final TextureStoreFormat format = textureConfiguration.getTextureDataType(source.getContributorId(
                    requestedLevel, sourceTile));
            CacheFunctionUtil.applyFunction(useAlpha, function, sourceData, data, destTile.getX(), destTile.getY(),
                    format, tileSize, dataSize);

            if (isCancelled()) {
                return false;
            }

            state = State.finished;
            cache[destTile.getX()][destTile.getY()].isValid = true;

            final Region region = new Region(clipmapLevel, sourceTile.getX() * tileSize, sourceTile.getY() * tileSize,
                    tileSize, tileSize);
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

    public SourceCacheFunction getFunction() {
        return function;
    }

    public void setFunction(final SourceCacheFunction function) {
        this.function = function;
    }

    public void shutdown() {
        exit = true;
        started = false;
    }
}
