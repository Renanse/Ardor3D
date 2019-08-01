/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.terrain.providers.inmemory.data;

import java.util.HashSet;
import java.util.Set;

import com.ardor3d.extension.terrain.util.Tile;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.math.functions.FbmFunction3D;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.functions.Functions;
import com.ardor3d.math.type.ReadOnlyVector3;

public class InMemoryTerrainData {

    protected final float[] heightData;
    protected final byte[] colorData;
    protected final int side;

    protected boolean running = false;
    protected float minHeight = 0.0f;
    protected float maxHeight = 10.0f;
    protected float updateDelta = .5f;

    protected final int tileSize;
    protected final int clipmapLevels;

    protected final Set<Tile>[] updatedTerrainTiles;
    protected final Set<Tile>[] updatedTextureTiles;

    protected ReadOnlyVector3 scale;

    /**
     *
     * @param totalSide
     *            must be greater than 10.
     */
    @SuppressWarnings("unchecked")
    public InMemoryTerrainData(final int totalSide, final int clipmapLevels, final int tileSize,
            final ReadOnlyVector3 scale) {
        if (totalSide < 10) {
            throw new IllegalArgumentException("totalSide must be at least 10.");
        }
        side = totalSide;
        heightData = new float[side * side];
        colorData = new byte[side * side * 4]; // rgba
        this.tileSize = tileSize;
        this.clipmapLevels = clipmapLevels;
        this.scale = scale;

        updatedTerrainTiles = new Set[clipmapLevels];
        updatedTextureTiles = new Set[clipmapLevels];
        for (int i = 0; i < clipmapLevels; i++) {
            updatedTerrainTiles[i] = new HashSet<>();
            updatedTextureTiles[i] = new HashSet<>();
        }

        final double procScale = 1.0 / 4000.0;
        final Function3D functionTmp = new FbmFunction3D(Functions.simplexNoise(), 9, 0.5, 0.5, 3.14);
        final Function3D function = Functions.scaleInput(functionTmp, procScale, procScale, 1);

        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                final int index = y * side + x;

                final float h = (float) function.eval(x, y, 0);
                heightData[index] = h;

                int col = (int) (h * h * 255 * 0.6);
                col = MathUtils.clamp(col, 0, 255);
                colorData[index * 4 + 0] = (byte) (col * 1.0);
                colorData[index * 4 + 1] = (byte) (col * 0.8);
                colorData[index * 4 + 2] = (byte) (col * 0.7);
                colorData[index * 4 + 3] = (byte) 1;
            }
        }
    }

    public void startUpdates() {
        if (running) {
            return;
        }

        running = true;
        final Thread t = new Thread() {
            @Override
            public void run() {
                while (running) {
                    // sleep some random amount of time (0 - 2 secs)
                    try {
                        Thread.sleep(MathUtils.nextRandomInt(0, 2000));
                    } catch (final InterruptedException e) {
                        running = false;
                        continue;
                    }

                    // pick a place to modify the terrain
                    final int x = MathUtils.nextRandomInt(0, side - 1);
                    final int y = MathUtils.nextRandomInt(0, side - 1);

                    // pick a color
                    final ColorRGBA paint = ColorRGBA.randomColor(null);

                    // pick a random radius 0 - tenth side
                    final int radius = MathUtils.nextRandomInt(0, side / 10);

                    // pick an offset amount
                    final float offset = MathUtils.nextRandomFloat() * updateDelta
                            * (MathUtils.nextRandomInt(0, 2) != 0 ? 1 : -1);

                    // modify the terrain!
                    updateTerrain(x, y, radius, paint, offset);

                    // queue up an update alert for the rectangle updated
                    final int minY = Math.max(0, y - radius), maxY = Math.min(y + radius, side - 1);
                    final int minX = Math.max(0, x - radius), maxX = Math.min(x + radius, side - 1);
                    final Rectangle2 region = new Rectangle2(minX, minY, maxX - minX, maxY - minY);

                    // break up by clipmaplevel
                    // add to two queues since these updates are called in different threads potentially
                    for (int i = 0; i < clipmapLevels; i++) {
                        addTiles(region, updatedTerrainTiles[i]);
                        addTiles(region, updatedTextureTiles[i]);
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    protected void addTiles(final Rectangle2 bounds, final Set<Tile> store) {
        for (int i = 0; i <= clipmapLevels; i++) {
            final double scale = 1.0 / (tileSize * MathUtils.pow2(i));
            final int minX = (int) MathUtils.floor(bounds.getX() * scale);
            final int minY = (int) MathUtils.floor(bounds.getY() * scale);
            final int maxX = (int) MathUtils.floor((bounds.getX() + bounds.getWidth() - 1) * scale);
            final int maxY = (int) MathUtils.floor((bounds.getY() + bounds.getHeight() - 1) * scale);

            Tile tile;
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    tile = new Tile(x, y);
                    synchronized (store) {
                        store.add(tile);
                    }
                }
            }
        }
    }

    protected void updateTerrain(final int x, final int y, final int radius, final ColorRGBA paint,
            final float offset) {
        float r, dr;
        int dx, dy, index;
        byte red, green, blue, alpha;
        final int minY = Math.max(0, y - radius), maxY = Math.min(y + radius, side - 1);
        final int minX = Math.max(0, x - radius), maxX = Math.min(x + radius, side - 1);
        for (int i = minY; i <= maxY; i++) {
            dy = Math.abs(y - i);
            for (int j = minX; j <= maxX; j++) {
                dx = Math.abs(x - j);
                r = (float) MathUtils.sqrt(dx * dx + dy * dy);
                if (r <= radius) {
                    dr = (radius - r) / radius;
                    index = i * side + j;
                    heightData[index] = Math.max(minHeight, Math.min(heightData[index] + dr * offset, maxHeight));
                    red = (byte) ((int) MathUtils.lerp(dr, colorData[index * 4 + 0] & 0xff, paint.getRed() * 255)
                            & 0xff);
                    green = (byte) ((int) MathUtils.lerp(dr, colorData[index * 4 + 1] & 0xff, paint.getGreen() * 255)
                            & 0xff);
                    blue = (byte) ((int) MathUtils.lerp(dr, colorData[index * 4 + 2] & 0xff, paint.getBlue() * 255)
                            & 0xff);
                    alpha = (byte) ((int) MathUtils.lerp(dr, colorData[index * 4 + 3] & 0xff, paint.getAlpha() * 255)
                            & 0xff);
                    colorData[index * 4 + 0] = red;
                    colorData[index * 4 + 1] = green;
                    colorData[index * 4 + 2] = blue;
                    colorData[index * 4 + 3] = alpha;
                }
            }
        }
    }

    public void stopUpdates() {
        running = false;
    }

    public float getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(final float minHeight) {
        this.minHeight = minHeight;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(final float maxHeight) {
        this.maxHeight = maxHeight;
    }

    public float getUpdateDelta() {
        return updateDelta;
    }

    public void setUpdateDelta(final float updateDelta) {
        this.updateDelta = updateDelta;
    }

    public byte[] getColorData() {
        return colorData;
    }

    public float[] getHeightData() {
        return heightData;
    }

    public int getSide() {
        return side;
    }

    public Set<Tile>[] getUpdatedTerrainTiles() {
        return updatedTerrainTiles;
    }

    public Set<Tile>[] getUpdatedTextureTiles() {
        return updatedTextureTiles;
    }

    public boolean isRunning() {
        return running;
    }

    public ReadOnlyVector3 getScale() {
        return scale;
    }

    public int getClipmapLevels() {
        return clipmapLevels;
    }
}
