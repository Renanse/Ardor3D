/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.heightmap;

import java.util.Random;
import java.util.logging.Logger;

import com.ardor3d.math.MathUtils;
import com.ardor3d.util.Ardor3dException;

public class MidPointHeightMapGenerator {
    private static final Logger logger = Logger.getLogger(MidPointHeightMapGenerator.class.getName());

    private float roughness;

    /** Height data information. */
    protected float[] heightData = null;

    /** The size of the height map's width. */
    protected int size = 0;

    /** Allows scaling the Y height of the map. */
    protected float heightScale = 1.0f;

    /** The filter is used to erode the terrain. */
    protected float filter = 0.5f;

    /** The range used to normalize terrain */
    private float heightRange = 1f;

    private final Random random = new Random();
    private boolean loaded = false;

    /**
     * Constructor builds a new heightmap using the midpoint displacement algorithm. Roughness determines how chaotic
     * the terrain will be. Where 1 is perfectly self-similar, > 1 early iterations have a disproportionately large
     * effect creating smooth terrain, and < 1 late iteraions have a disproportionately large effect creating chaotic
     * terrain.
     * 
     * @param size
     *            the size of the terrain, must be a power of 2.
     * @param roughness
     *            how chaotic to make the terrain.
     * 
     * @throws Ardor3dException
     *             if size is less than or equal to zero or roughtness is less than 0.
     */
    public MidPointHeightMapGenerator(final int size, final float roughness) {
        if (!MathUtils.isPowerOfTwo(size)) {
            throw new Ardor3dException("Size must be (2^N) sized.");
        }
        if (roughness < 0 || size <= 0) {
            throw new Ardor3dException("size and roughness must be " + "greater than 0");
        }
        this.roughness = roughness;
        this.size = size;
    }

    /**
     * Constructor builds a new heightmap using the midpoint displacement algorithm. Roughness determines how chaotic
     * the terrain will be. Where 1 is perfectly self-similar, > 1 early iterations have a disproportionately large
     * effect creating smooth terrain, and < 1 late iteraions have a disproportionately large effect creating chaotic
     * terrain.
     * 
     * @param size
     *            the size of the terrain, must be a power of 2.
     * @param roughness
     *            how chaotic to make the terrain.
     * 
     * @throws Ardor3dException
     *             if size is less than or equal to zero or roughtness is less than 0.
     */
    public MidPointHeightMapGenerator(final int size, final float roughness, final long seed) {
        this(size, roughness);
        random.setSeed(seed);
    }

    /**
     * @return the heightData
     */
    public float[] getHeightData() {
        if (!loaded) {
            generateHeightData();
        }
        return heightData;
    }

    /**
     * <code>load</code> generates the heightfield using the Midpoint Displacement algorithm. <code>load</code> uses the
     * latest attributes, so a call to <code>load</code> is recommended if attributes have changed using the set
     * methods.
     */
    public boolean generateHeightData() {
        float height;
        double heightReducer;
        float[][] tempBuffer;

        // holds the points of the square.
        int ni, nj;
        int mi, mj;
        int pmi, pmj;

        height = 1f;
        heightReducer = Math.pow(2, -1 * roughness);

        heightData = new float[size * size];
        tempBuffer = new float[size][size];

        int counter = size;
        while (counter > 0) {
            // displace the center of the square.
            for (int i = 0; i < size; i += counter) {
                for (int j = 0; j < size; j += counter) {
                    // (0,0) point of the local square
                    ni = (i + counter) % size;
                    nj = (j + counter) % size;
                    // middle point of the local square
                    mi = i + counter / 2;
                    mj = j + counter / 2;

                    // displace the middle point by the average of the
                    // corners, and a random value.
                    tempBuffer[mi][mj] = (tempBuffer[i][j] + tempBuffer[ni][j] + tempBuffer[i][nj] + tempBuffer[ni][nj])
                            / 4 + random.nextFloat() * height - height / 2;
                }
            }

            // next calculate the new midpoints of the line segments.
            for (int i = 0; i < size; i += counter) {
                for (int j = 0; j < size; j += counter) {
                    // (0,0) of the local square
                    ni = (i + counter) % size;
                    nj = (j + counter) % size;

                    // middle point of the local square.
                    mi = i + counter / 2;
                    mj = j + counter / 2;

                    // middle point on the line in the x-axis direction.
                    pmi = (i - counter / 2 + size) % size;
                    // middle point on the line in the y-axis direction.
                    pmj = (j - counter / 2 + size) % size;

                    // Calculate the square value for the top side of the rectangle
                    tempBuffer[mi][j] = (tempBuffer[i][j] + tempBuffer[ni][j] + tempBuffer[mi][pmj] + tempBuffer[mi][mj])
                            / 4 + random.nextFloat() * height - height / 2;

                    // Calculate the square value for the left side of the rectangle
                    tempBuffer[i][mj] = (tempBuffer[i][j] + tempBuffer[i][nj] + tempBuffer[pmi][mj] + tempBuffer[mi][mj])
                            / 4 + random.nextFloat() * height - height / 2;

                }
            }

            counter /= 2;
            height *= heightReducer;
        }

        normalizeTerrain(tempBuffer);

        // transfer the new terrain into the height map.
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                setHeightAtPoint(tempBuffer[i][j], i, j);
            }
        }

        MidPointHeightMapGenerator.logger.info("Created Heightmap using Mid Point");

        loaded = true;

        return true;
    }

    /**
     * <code>setRoughness</code> sets the new roughness value of the heightmap. Roughness determines how chaotic the
     * terrain will be. Where 1 is perfectly self-similar, > 1 early iterations have a disproportionately large effect
     * creating smooth terrain, and < 1 late iteraions have a disproportionately large effect creating chaotic terrain.
     * 
     * @param roughness
     *            how chaotic will the heightmap be.
     */
    public void setRoughness(final float roughness) {
        if (roughness < 0) {
            throw new Ardor3dException("roughness must be greater than 0");
        }
        this.roughness = roughness;
    }

    /**
     * <code>setHeightAtPoint</code> sets the height value for a given coordinate.
     * 
     * @param height
     *            the new height for the coordinate.
     * @param x
     *            the x (east/west) coordinate.
     * @param z
     *            the z (north/south) coordinate.
     */
    protected void setHeightAtPoint(final float height, final int x, final int z) {
        heightData[x + z * size] = height;
    }

    /**
     * <code>normalizeTerrain</code> takes the current terrain data and converts it to values between 0 and
     * NORMALIZE_RANGE.
     * 
     * @param tempBuffer
     *            the terrain to normalize.
     */
    protected void normalizeTerrain(final float[][] tempBuffer) {
        float currentMin, currentMax;
        float height;

        currentMin = tempBuffer[0][0];
        currentMax = tempBuffer[0][0];

        // find the min/max values of the height fTemptemptempBuffer
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (tempBuffer[i][j] > currentMax) {
                    currentMax = tempBuffer[i][j];
                } else if (tempBuffer[i][j] < currentMin) {
                    currentMin = tempBuffer[i][j];
                }
            }
        }

        // find the range of the altitude
        if (currentMax <= currentMin) {
            return;
        }

        height = currentMax - currentMin;

        // scale the values to a range of 0-255
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                tempBuffer[i][j] = (tempBuffer[i][j] - currentMin) / height * heightRange;
            }
        }
    }

    /**
     * <code>erodeTerrain</code> is a convenience method that applies the FIR filter to a given height map. This
     * simulates water errosion.
     * 
     * @param tempBuffer
     *            the terrain to filter.
     */
    protected void erodeTerrain(final float[][] tempBuffer) {
        // erode left to right
        float v;

        for (int i = 0; i < size; i++) {
            v = tempBuffer[i][0];
            for (int j = 1; j < size; j++) {
                tempBuffer[i][j] = filter * v + (1 - filter) * tempBuffer[i][j];
                v = tempBuffer[i][j];
            }
        }

        // erode right to left
        for (int i = size - 1; i >= 0; i--) {
            v = tempBuffer[i][0];
            for (int j = 0; j < size; j++) {
                tempBuffer[i][j] = filter * v + (1 - filter) * tempBuffer[i][j];
                v = tempBuffer[i][j];
                // erodeBand(tempBuffer[size * i + size - 1], -1);
            }
        }

        // erode top to bottom
        for (int i = 0; i < size; i++) {
            v = tempBuffer[0][i];
            for (int j = 0; j < size; j++) {
                tempBuffer[j][i] = filter * v + (1 - filter) * tempBuffer[j][i];
                v = tempBuffer[j][i];
            }
        }

        // erode from bottom to top
        for (int i = size - 1; i >= 0; i--) {
            v = tempBuffer[0][i];
            for (int j = 0; j < size; j++) {
                tempBuffer[j][i] = filter * v + (1 - filter) * tempBuffer[j][i];
                v = tempBuffer[j][i];
            }
        }
    }

    public float getHeightRange() {
        return heightRange;
    }

    public void setHeightRange(final float heightRange) {
        this.heightRange = heightRange;
    }
}
