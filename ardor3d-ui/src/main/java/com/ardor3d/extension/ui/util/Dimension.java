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
 * This class is patterned after awt's {@link java.awt.Dimension Dimension} class. It describes the width and height of
 * something (in our case, generally a UI element.)
 */
public class Dimension {

    private int _width;
    private int _height;

    /**
     * Construct a new 0x0 dimension object.
     */
    public Dimension() {
        this(0, 0);
    }

    /**
     * Construct a new dimension object using the given values.
     * 
     * @param width
     * @param height
     */
    public Dimension(final int width, final int height) {
        setWidth(width);
        setHeight(height);
    }

    /**
     * Construct a new dimension object using the values of the given source.
     * 
     * @param source
     */
    public Dimension(final Dimension source) {
        this(source.getWidth(), source.getHeight());
    }

    public int getWidth() {
        return _width;
    }

    public void setWidth(final int width) {
        _width = width;
    }

    public int getHeight() {
        return _height;
    }

    public void setHeight(final int height) {
        _height = height;
    }

    public void set(final int width, final int height) {
        setWidth(width);
        setHeight(height);
    }

    public void set(final Dimension d) {
        set(d.getWidth(), d.getHeight());
    }

    public boolean contains(final double x, final double y) {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

    public boolean contains(final int x, final int y) {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Dimension) {
            final Dimension d = (Dimension) obj;
            return getWidth() == d.getWidth() && getHeight() == d.getHeight();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result += 31 * result + getWidth();
        result += 31 * result + getHeight();
        return result;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[width=" + getWidth() + ",height=" + getHeight() + "]";
    }
}
