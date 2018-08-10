/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math.functions;

/**
 * Creates a pattern in the XY plane that is defined by a user-provided grid of values. This is not really 3D, since it
 * ignores the z factor.
 */
public class GridPatternFunction3D implements Function3D {

    private final double[][] _grid;
    private final double _xScaleFactor;
    private final double _yScaleFactor;

    /**
     * Create the grid pattern function. The x-scale and y-scale will both default to 1.0.
     * 
     * @param grid
     *            A grid of values in the range of -1 to 1.
     */
    public GridPatternFunction3D(final double[][] grid) {
        this(grid, 1.0, 1.0);
    }

    /**
     * Create the grid pattern function.
     * 
     * @param grid
     *            A grid of values in the range of -1 to 1.
     * @param xScaleFactor
     *            The amount by which to scale grid cells along their x axis.
     * @param yScaleFactor
     *            The amount by which to scale grid cells along their y axis.
     */
    public GridPatternFunction3D(final double[][] grid, final double xScaleFactor, final double yScaleFactor) {
        _grid = grid;
        _xScaleFactor = xScaleFactor;
        _yScaleFactor = yScaleFactor;
    }

    /**
     * Evaluate the x and y valus (ignores z) to determine the value to return.
     * 
     * @return
     */
    public double eval(double x, double y, final double z) {
        x = Math.abs(x);
        y = Math.abs(y);

        double scaledX = x / _xScaleFactor;
        double scaledY = y / _yScaleFactor;

        final int numXCells = _grid.length;
        final int numYCells = _grid[0].length;
        scaledX -= Math.floor(scaledX / numXCells) * numXCells;
        scaledY -= Math.floor(scaledY / numYCells) * numYCells;

        final int gridX = (int) (Math.floor(scaledX) % numXCells);
        final int gridY = (int) (Math.floor(scaledY) % numYCells);

        return getCellValue(gridX, gridY, scaledX, scaledY);
    }

    /**
     * Gets a value from the user-defined grid. This is an extension point for subclasses which may need to perform a
     * calculation of some type on that value before returning it. The default implementation simply returns the value
     * from the grid at gridX/gridY.
     */
    protected double getCellValue(final int gridX, final int gridY, final double scaledX, final double scaledY) {
        return _grid[gridX][gridY];
    }
}