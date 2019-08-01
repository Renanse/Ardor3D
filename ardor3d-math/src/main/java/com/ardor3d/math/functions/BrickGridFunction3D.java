/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.functions;

import com.ardor3d.math.MathUtils;

/**
 * Creates a brick-like pattern in the XY plane. This is not really 3D, since it ignores the z factor.
 * 
 * The brick and mortar values returned by this function can (and do by default) have some amount of variation for
 * visual interest. The specified variation represents a maximum -- actual variation is determined randomly within this
 * maximum variation. Since actual variation is determined randomly, the brick and mortar values are likely to change a
 * bit for each instance of this class. In other words, if you do not want the variation to change at all, you should
 * instantiate this class once and use the same object each time.
 */
public class BrickGridFunction3D extends GridPatternFunction3D {

    private static final int DEFAULT_BRICK_LENGTH = 12;
    private static final int DEFAULT_BRICK_HEIGHT = 6;
    private static final int DEFAULT_MORTAR_THICKNESS = 1;
    private static final double DEFAULT_BRICK_VALUE = 0.0;
    private static final double DEFAULT_MORTAR_VALUE = 0.9;
    private static final double DEFAULT_BRICK_VARIATION = 0.1;
    private static final double DEFAULT_MORTAR_VARIATION = 0.05;

    /**
     * Create a brick pattern using all default values.
     */
    public BrickGridFunction3D() {
        this(DEFAULT_BRICK_LENGTH, DEFAULT_BRICK_HEIGHT, DEFAULT_MORTAR_THICKNESS, DEFAULT_BRICK_VALUE,
                DEFAULT_MORTAR_VALUE, DEFAULT_BRICK_VARIATION, DEFAULT_MORTAR_VARIATION);
    }

    /**
     * Create a brick pattern with values specified by the caller.
     * 
     * @param brickLength
     *            The number of grid cells used for the length of a brick (default 12).
     * @param brickHeight
     *            The number of grid cells used for the height of a brick (default 6).
     * @param mortarThickness
     *            The number of grid cells used for the thickness of mortar (default 1).
     * @param brickValue
     *            The value returned from the function for a brick coordinate (default 0.0).
     * @param mortarValue
     *            The value returned from the function for a mortar coordinate (default 0.9).
     * @param brickVariationAmount
     *            The amount by which the brick value can vary (default 0.1). Use 0.0 for no variation.
     * @param mortarVariationAmount
     *            The amount by which the mortar value can vary (default 0.05). Use 0.0 for no variation.
     */
    public BrickGridFunction3D(final int brickLength, final int brickHeight, final int mortarThickness,
            final double brickValue, final double mortarValue, final double brickVariationAmount,
            final double mortarVariationAmount) {
        super(createBrickGrid(brickLength, brickHeight, mortarThickness, brickValue, mortarValue, brickVariationAmount,
                mortarVariationAmount));
    }

    /**
     * A private utility method to build the grid used to represent the bricks.
     */
    private static double[][] createBrickGrid(final int brickLength, final int brickHeight, final int mortarThickness,
            final double brickValue, final double mortarValue, final double brickVariationAmount,
            final double mortarVariationAmount) {
        final int xTotal = brickLength + mortarThickness;
        final int yTotal = (brickHeight * 2) + (mortarThickness * 2);
        final double[][] grid = new double[xTotal][yTotal];

        // for simplicity, initialize all cells to the brick-value since that is by far the most
        // common value.
        for (int x = 0; x < xTotal; x++) {
            for (int y = 0; y < yTotal; y++) {
                grid[x][y] = createGridValue(brickValue, brickVariationAmount);
            }
        }

        // now put the mortar value into those cells that need it.
        // first, create the horizontal mortar lines...
        for (int x = 0; x < xTotal; x++) {
            for (int y = brickHeight; y < (brickHeight + mortarThickness); y++) {
                grid[x][y] = createGridValue(mortarValue, mortarVariationAmount);
                grid[x][y + brickHeight + 1] = createGridValue(mortarValue, mortarVariationAmount);
            }
        }

        // ... then create the vertical mortar lines.
        for (int y = 0; y < brickHeight; y++) {
            grid[xTotal / 2][y + brickHeight + 1] = createGridValue(mortarValue, mortarVariationAmount);
            grid[xTotal - 1][y] = createGridValue(mortarValue, mortarVariationAmount);
        }

        return grid;
    }

    private static double createGridValue(final double baseValue, final double variationAmount) {
        double gridValue = baseValue;

        if (variationAmount > 0.0) {
            final double variation = (MathUtils.nextRandomDouble() * (variationAmount * 2.0)) - variationAmount;
            gridValue += variation;
        }

        return gridValue;
    }
}
