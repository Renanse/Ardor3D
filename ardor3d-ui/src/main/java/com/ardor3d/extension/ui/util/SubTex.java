/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.util;

import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyRectangle2;

/**
 * Describes a sub-portion of a full Ardor3D Texture object. This is useful for taking a large image with lots of
 * smaller images in it and reusing that same texture in multiple locations.
 */
public class SubTex {

    private final Texture _texture;

    /** The x location, in pixels, of our upper left corner */
    private int _x;
    /** The y location, in pixels, of our upper left corner */
    private int _y;

    /** The width, in pixels, of our part of the texture */
    private int _width;
    /** The height, in pixels, of our part of the texture */
    private int _height;

    /** The color to tint with when rendering this SubTex */
    private ColorRGBA _tint = null;

    /**
     * The number of pixels of the inside-top of this subtex that is considered a border. Used by some UIBorders and
     * UIBackdrops in 9-slicing.
     */
    private int _borderTop;
    /**
     * The number of pixels of the inside-left of this subtex that is considered a border. Used by some UIBorders and
     * UIBackdrops in 9-slicing.
     */
    private int _borderLeft;
    /**
     * The number of pixels of the inside-bottom of this subtex that is considered a border. Used by some UIBorders and
     * UIBackdrops in 9-slicing.
     */
    private int _borderBottom;
    /**
     * The number of pixels of the inside-right of this subtex that is considered a border. Used by some UIBorders and
     * UIBackdrops in 9-slicing.
     */
    private int _borderRight;

    /**
     * Construct a new SubTex that covers the entire width and height of the supplied Texture.
     *
     * @param texture
     *            the base texture. Must already have a supplied image with a valid width and height.
     */
    public SubTex(final Texture texture) {
        this(texture, 0, 0, texture.getImage().getWidth(), texture.getImage().getHeight());
    }

    /**
     * Construct a new SubTex using the specified dimensions and location.
     *
     * @param texture
     *            the master texture we are a part of
     * @param x
     *            the x location, in pixels, of our upper left corner.
     * @param y
     *            the y location, in pixels, of our upper left corner.
     * @param width
     *            the width, in pixels, of our part of the texture.
     * @param height
     *            the height, in pixels, of our part of the texture.
     */
    public SubTex(final Texture texture, final int x, final int y, final int width, final int height) {
        _texture = texture;
        _x = x;
        _y = y;
        _width = width;
        _height = height;
    }

    /**
     * Construct a new SubTex using the specified dimensions and location.
     *
     * @param texture
     *            the master texture we are a part of
     * @param x
     *            the x location, in pixels, of our upper left corner.
     * @param y
     *            the y location, in pixels, of our upper left corner.
     * @param width
     *            the width, in pixels, of our part of the texture.
     * @param height
     *            the height, in pixels, of our part of the texture.
     * @param borderTop
     *            border, in pixels, of top.
     * @param borderLeft
     *            border, in pixels, of left.
     * @param borderBottom
     *            border, in pixels, of bottom.
     * @param borderRight
     *            border, in pixels, of right.
     */
    public SubTex(final Texture texture, final int x, final int y, final int width, final int height,
            final int borderTop, final int borderLeft, final int borderBottom, final int borderRight) {
        _texture = texture;
        _x = x;
        _y = y;
        _width = width;
        _height = height;

        _borderTop = borderTop;
        _borderLeft = borderLeft;
        _borderBottom = borderBottom;
        _borderRight = borderRight;
    }

    public Texture getTexture() {
        return _texture;
    }

    public int getX() {
        return _x;
    }

    public int getY() {
        return _y;
    }

    public int getHeight() {
        return _height;
    }

    public int getWidth() {
        return _width;
    }

    public ReadOnlyColorRGBA getTint() {
        return _tint;
    }

    public int getBorderTop() {
        return _borderTop;
    }

    public int getBorderLeft() {
        return _borderLeft;
    }

    public int getBorderBottom() {
        return _borderBottom;
    }

    public int getBorderRight() {
        return _borderRight;
    }

    public void setBorders(final int top, final int left, final int bottom, final int right) {
        _borderTop = top;
        _borderLeft = left;
        _borderBottom = bottom;
        _borderRight = right;
    }

    /**
     *
     * @param color
     *            a color to blend our texture with when rendering SubTex objects. The default is null (which is
     *            interpreted as pure white and gives an unaltered version of the texture.)
     */
    public void setTint(final ReadOnlyColorRGBA color) {
        if (color == null) {
            _tint = null;
        } else if (_tint == null) {
            _tint = new ColorRGBA(color);
        } else {
            _tint.set(color);
        }
    }

    @Override
    public String toString() {
        return String.format("SubTex of %0$s: %1$d, %2$d  dims: %3$dx%4$d  tint: %5$s  border: %6$d,%7$d,%8$d,%9$d",
                _texture, _x, _y, _width, _height, _tint, _borderTop, _borderLeft, _borderBottom, _borderRight);
    }

    public void setHeight(final int height) {
        _height = height;
    }

    public void setWidth(final int width) {
        _width = width;
    }

    public void setX(final int x) {
        _x = x;
    }

    public void setY(final int y) {
        _y = y;
    }

    public void set(final ReadOnlyRectangle2 viewport) {
        _x = viewport.getX();
        _y = viewport.getY();
        _width = viewport.getWidth();
        _height = viewport.getHeight();
    }

    /**
     * @return the uv coordinate, in [0, 1], of the right edge of this SubTex as relates to its master Texture.
     */
    public float getEndX() {
        return (_width + _x) / (float) _texture.getImage().getWidth();
    }

    /**
     * @return the uv coordinate, in [0, 1], of the top edge of this SubTex as relates to its master Texture.
     */
    public float getEndY() {
        return (_height + _y) / (float) _texture.getImage().getHeight();
    }

    /**
     * @return the uv coordinate, in [0, 1], of the left edge of this SubTex as relates to its master Texture.
     */
    public float getStartX() {
        return _x / (float) _texture.getImage().getWidth();
    }

    /**
     * @return the uv coordinate, in [0, 1], of the bottom edge of this SubTex as relates to its master Texture.
     */
    public float getStartY() {
        return _y / (float) _texture.getImage().getHeight();
    }
}
