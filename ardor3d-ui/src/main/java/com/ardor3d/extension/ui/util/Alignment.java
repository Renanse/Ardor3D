/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.util;

/**
 * Enumeration useful for describing positioning and alignment to the edges and corners of a rectangular area. This is
 * used in our UI system to align text, images, and handle certain layouts such as the AnchorLayout.
 */
public enum Alignment {
    /** North West */
    TOP_LEFT(0.0, 1.0),

    /** North */
    TOP(0.5, 1.0),

    /** North East */
    TOP_RIGHT(1.0, 1.0),

    /** West */
    LEFT(0.0, 0.5),

    /** Center */
    MIDDLE(0.5, 0.5),

    /** East */
    RIGHT(1.0, 0.5),

    /** South West */
    BOTTOM_LEFT(0.0, 0.0),

    /** South */
    BOTTOM(0.5, 0.0),

    /** South East */
    BOTTOM_RIGHT(1.0, 0.0);

    /**
     * How far from the left this alignment is as a percentage.
     */
    private double along;

    /**
     * How far from the bottom this alignment is as a percentage.
     */
    private double up;

    /**
     * @param along
     *            How far from the left this alignment is as a percentage.
     * @param up
     *            How far from the bottom this alignment is as a percentage.
     */
    Alignment(final double along, final double up) {
        this.along = along;
        this.up = up;
    }

    /**
     * @return a value in [0, 1] describing how far from the bottom this alignment is.
     */
    public double fromLeft() {
        return along;
    }

    /**
     * @return a value in [0, 1] describing how far from the bottom this alignment is.
     */
    public double fromBottom() {
        return up;
    }

    /**
     * Determine the correct relative X position for a component being aligned, using this alignment, to another
     * component (or a box to another box, or area... etc. - anything with a "width").
     * 
     * @param availableWidth
     *            the width of the component or area we are aligning to.
     * @param width
     *            the width of the component or area we are setting position on.
     * @return the new X position to use.
     */
    public double alignX(final double availableWidth, final double width) {
        return fromLeft() * availableWidth - fromLeft() * width;
    }

    public int alignX(final int availableWidth, final int width) {
        return (int) Math.round(fromLeft() * availableWidth - fromLeft() * width);
    }

    /**
     * Determine the correct relative Y position for a component being aligned, using this alignment, to another
     * component (or a box to another box, or area... etc. - anything with a "height").
     * 
     * @param availableHeight
     *            the height of the component or area we are aligning to.
     * @param height
     *            the height of the component or area we are setting position on.
     * @return the new Y position to use.
     */
    public double alignY(final double availableHeight, final double height) {
        return fromBottom() * availableHeight - fromBottom() * height;
    }

    public int alignY(final int availableHeight, final int height) {
        return (int) Math.round(fromBottom() * availableHeight - fromBottom() * height);
    }
}
