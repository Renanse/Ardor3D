/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.layout;

/**
 * This class provides layout data for the GridLayout.
 */
public class GridLayoutData implements UILayoutData {

    /**
     * a shared GridLayoutData that just wraps the layout line after this component
     */
    public static GridLayoutData Wrap = new GridLayoutData(1, true, false);

    /**
     * a shared GridLayoutData that just wraps the layout line after this component and let it fill the space
     */
    public static GridLayoutData WrapAndGrow = new GridLayoutData(1, true, true);

    private boolean wrap;
    private boolean grow;
    private int span;

    public GridLayoutData() {
        this(1, false, false);
    }

    public GridLayoutData(final int span, final boolean wrap, final boolean grow) {
        this.span = span;
        this.wrap = wrap;
        this.grow = grow;
    }

    public boolean isWrap() {
        return wrap;
    }

    public void setWrap(final boolean wrap) {
        this.wrap = wrap;
    }

    public int getSpan() {
        return span;
    }

    public void setSpan(final int span) {
        this.span = span;
    }

    public boolean isGrow() {
        return grow;
    }

    public void setGrow(final boolean grow) {
        this.grow = grow;
    }

    /**
     * create a new GridLayoutData that specifies the number of cells the component should use horizontally
     * 
     * @param columns
     * @return
     */
    public static GridLayoutData Span(final int columns) {
        return new GridLayoutData(columns, false, false);
    }

    /**
     * create a new GridLayoutData that specifies the number of cells the component should use horizontally and that the
     * layout line should wrap after this component
     * 
     * @param columns
     * @return
     */
    public static GridLayoutData SpanAndWrap(final int columns) {
        return new GridLayoutData(columns, true, false);
    }
}
