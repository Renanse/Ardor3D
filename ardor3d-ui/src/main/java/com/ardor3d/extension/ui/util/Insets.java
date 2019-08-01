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

/**
 * This class is patterned after awt's {@link java.awt.Insets Insets} class. It describes the margins on four sides of a
 * rectangular area and is the foundation of our UIBorder class.
 */
public class Insets {

    /**
     * 0, 0, 0, 0
     */
    public final static Insets EMPTY = new Insets(0, 0, 0, 0);

    private int _top;
    private int _left;
    private int _bottom;
    private int _right;

    /**
     * Constructs a new insets with 0 size on each side.
     */
    public Insets() {
        set(0, 0, 0, 0);
    }

    /**
     * Constructs a new insets using the given sizes.
     *
     * @param top
     * @param left
     * @param bottom
     * @param right
     */
    public Insets(final int top, final int left, final int bottom, final int right) {
        set(top, left, bottom, right);
    }

    /**
     * Constructs a new insets using the sizes from the given source.
     *
     * @param source
     */
    public Insets(final Insets source) {
        set(source);
    }

    /**
     * Set the size of the sides to the given values.
     *
     * @param top
     * @param left
     * @param bottom
     * @param right
     */
    public void set(final int top, final int left, final int bottom, final int right) {
        _top = top;
        _left = left;
        _bottom = bottom;
        _right = right;
    }

    public void set(final Insets other) {
        _top = other.getTop();
        _left = other.getLeft();
        _bottom = other.getBottom();
        _right = other.getRight();
    }

    public int getLeft() {
        return _left;
    }

    public int getTop() {
        return _top;
    }

    public int getRight() {
        return _right;
    }

    public int getBottom() {
        return _bottom;
    }

    public void setLeft(final int size) {
        _left = size;
    }

    public void setRight(final int size) {
        _right = size;
    }

    public void setTop(final int size) {
        _top = size;
    }

    public void setBottom(final int size) {
        _bottom = size;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Insets) {
            final Insets other = (Insets) obj;
            return _top == other._top && _left == other._left && _bottom == other._bottom && _right == other._right;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 31 * result + getLeft();
        result += 31 * result + getRight();
        result += 31 * result + getTop();
        result += 31 * result + getBottom();
        return result;
    }

    @Override
    public String toString() {
        return String.format("'%1$s': top: %2$d, left: %3$d, bottom: %4$d, right: %5$d", this.getClass()
                .getSimpleName(), getTop(), getLeft(), getBottom(), getRight());
    }
}
