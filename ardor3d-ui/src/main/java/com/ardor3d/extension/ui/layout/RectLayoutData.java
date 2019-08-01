/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.layout;

import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.math.Vector2;

/**
 * Layout data used to describe a component's positioning relative to a rectangle drawn on the component's parent. The
 * positioning uses percentage based anchor points to form a rectangle on the parent's content area and pixel insets to
 * enlarge or shrink that rectangle.
 */
public class RectLayoutData implements UILayoutData {

    protected final Vector2 _relativeMin = new Vector2(0.5, 0.5);
    protected final Vector2 _relativeMax = new Vector2(0.5, 0.5);

    protected final Insets _pixelInsets = new Insets(0, 0, 0, 0);

    /**
     * Create new layout data for use in RectLayout. Defaults to anchor to center of component with -10 insets on all
     * edges, forming a 20x20 component.
     */
    public RectLayoutData() {
        this(.5, .5, .5, .5, -10, -10, -10, -10);
    }

    /**
     * Create new layout data for use in RectLayout.
     *
     * @param minX
     *            left edge, as a percent from left to right.
     * @param minY
     *            bottom edge, as a percent from bottom to top.
     * @param maxX
     *            right edge, as a percent from left to right.
     * @param maxY
     *            top edge, as a percent from bottom to top.
     * @param top
     *            top edge inset, in pixels
     * @param left
     *            left edge inset, in pixels
     * @param bottom
     *            bottom edge inset, in pixels
     * @param right
     *            right edge inset, in pixels
     */
    public RectLayoutData(final double minX, final double minY, final double maxX, final double maxY, final int top,
            final int left, final int bottom, final int right) {
        _relativeMin.set(minX, minY);
        _relativeMax.set(maxX, maxY);
        _pixelInsets.set(top, left, bottom, right);
    }

    /**
     * Set the parent anchor points to tie this component to.
     *
     * @param minX
     *            left edge, as a percent from left to right.
     * @param minY
     *            bottom edge, as a percent from bottom to top.
     * @param maxX
     *            right edge, as a percent from left to right.
     * @param maxY
     *            top edge, as a percent from bottom to top.
     */
    public void setRelativeAnchors(final double minX, final double minY, final double maxX, final double maxY) {
        _relativeMin.set(minX, minY);
        _relativeMax.set(maxX, maxY);
    }

    /**
     * Set the inset values from the edges of the rectangle described by the min and max anchor points. Positive values
     * move the edge towards the center of the described rectangle.
     *
     * @param top
     *            top edge inset, in pixels
     * @param left
     *            left edge inset, in pixels
     * @param bottom
     *            bottom edge inset, in pixels
     * @param right
     *            right edge inset, in pixels
     */
    public void setPixelInsets(final int top, final int left, final int bottom, final int right) {
        _pixelInsets.set(top, left, bottom, right);
    }

    /**
     * @return the bottom left corner of the parent rectangle to position this component relative to. Coordinates are in
     *         percent, where X=0.0 means the left edge and X=1.0 means the right edge, Y=0.0 means the bottom edge and
     *         Y=1.0 means the top edge.
     */
    public Vector2 getRelativeMin() {
        return _relativeMin;
    }

    /**
     * @return the top right corner of the parent rectangle to position this component relative to. Coordinates are in
     *         percent, where X=0.0 means the left edge and X=1.0 means the right edge, Y=0.0 means the bottom edge and
     *         Y=1.0 means the top edge.
     */
    public Vector2 getRelativeMax() {
        return _relativeMax;
    }

    /**
     * @return the insets from the anchor rectangle we will use when calculating the size and position of this
     *         component. A positive value moves that edge closer to the center of the rectangle.
     */
    public Insets getPixelInsets() {
        return _pixelInsets;
    }

    /**
     * Create layout data that centers a component on its parent's center, with a specific width and height.
     *
     * @param childWidth
     *            desired child component width
     * @param childHeight
     *            desired child component height
     * @param offsetX
     *            child offset on X from parent's center
     * @param offsetY
     *            child offset on Y from parent's center
     * @return new RectLayoutData
     */
    public static RectLayoutData pinCenter(final int childWidth, final int childHeight, final int offsetX,
            final int offsetY) {
        final int left = -childWidth / 2 + offsetX;
        final int right = -childWidth / 2 - offsetX;
        final int bottom = -childHeight / 2 + offsetY;
        final int top = -childHeight / 2 - offsetY;
        return new RectLayoutData(0.5, 0.5, 0.5, 0.5, top, left, bottom, right);
    }

    /**
     * Create layout data that positions a component with its top left relative to its parent's top left, with a
     * specific width and height.
     *
     * @param childWidth
     *            desired child component width
     * @param childHeight
     *            desired child component height
     * @param offsetX
     *            child offset on X from parent's top left
     * @param offsetY
     *            child offset on Y from parent's top left
     * @return new RectLayoutData
     */
    public static RectLayoutData pinTopLeft(final int childWidth, final int childHeight, final int offsetX,
            final int offsetY) {
        final int left = offsetX;
        final int right = -childWidth - offsetX;
        final int bottom = -childHeight + offsetY;
        final int top = -offsetY;
        return new RectLayoutData(0, 1, 0, 1, top, left, bottom, right);
    }

    /**
     * Create layout data that positions a component with its top right relative to its parent's top right, with a
     * specific width and height.
     *
     * @param childWidth
     *            desired child component width
     * @param childHeight
     *            desired child component height
     * @param offsetX
     *            child offset on X from parent's top right
     * @param offsetY
     *            child offset on Y from parent's top right
     * @return new RectLayoutData
     */
    public static RectLayoutData pinTopRight(final int childWidth, final int childHeight, final int offsetX,
            final int offsetY) {
        final int left = -childWidth + offsetX;
        final int right = -offsetX;
        final int bottom = -childHeight + offsetY;
        final int top = -offsetY;
        return new RectLayoutData(1, 1, 1, 1, top, left, bottom, right);
    }

    /**
     * Create layout data that positions a component with its bottom left relative to its parent's bottom left, with a
     * specific width and height.
     *
     * @param childWidth
     *            desired child component width
     * @param childHeight
     *            desired child component height
     * @param offsetX
     *            child offset on X from parent's bottom left
     * @param offsetY
     *            child offset on Y from parent's bottom left
     * @return new RectLayoutData
     */
    public static RectLayoutData pinBottomLeft(final int childWidth, final int childHeight, final int offsetX,
            final int offsetY) {
        final int left = offsetX;
        final int right = -childWidth - offsetX;
        final int bottom = +offsetY;
        final int top = -childHeight - offsetY;
        return new RectLayoutData(0, 0, 0, 0, top, left, bottom, right);
    }

    /**
     * Create layout data that positions a component with its bottom right relative to its parent's bottom right, with a
     * specific width and height.
     *
     * @param childWidth
     *            desired child component width
     * @param childHeight
     *            desired child component height
     * @param offsetX
     *            child offset on X from parent's bottom right
     * @param offsetY
     *            child offset on Y from parent's bottom right
     * @return new RectLayoutData
     */
    public static RectLayoutData pinBottomRight(final int childWidth, final int childHeight, final int offsetX,
            final int offsetY) {
        final int left = -childWidth + offsetX;
        final int right = -offsetX;
        final int bottom = +offsetY;
        final int top = -childHeight - offsetY;
        return new RectLayoutData(1, 0, 1, 0, top, left, bottom, right);
    }

}
