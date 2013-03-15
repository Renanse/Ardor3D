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

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.Alignment;

/**
 * A data class that is used in conjunction with AnchorLayout.
 * 
 * @see AnchorLayout
 */
public class AnchorLayoutData implements UILayoutData {

    /** The point on my component to align at. */
    private Alignment _myPoint;
    /** The component or container to align to. */
    private UIComponent _parent;
    /** The point on the parent to align my point to. */
    private Alignment _parentPoint;
    /** After aligning, we will add this x offset to the component. */
    private int _xOffset;
    /** After aligning, we will add this y offset to the component. */
    private int _yOffset;

    /**
     * Construct a new AnchorLayoutData using the provided values.
     * 
     * @param myPoint
     *            the point on our component to align at
     * @param parent
     *            the component or container to align to
     * @param parentPoint
     *            the point on the parent to align my point to
     * @param xOffset
     *            a value added to our component's x position after the alignment is done.
     * @param yOffset
     *            a value added to our component's y position after the alignment is done.
     */
    public AnchorLayoutData(final Alignment myPoint, final UIComponent parent, final Alignment parentPoint,
            final int xOffset, final int yOffset) {
        _myPoint = myPoint;
        _parent = parent;
        _parentPoint = parentPoint;
        _xOffset = xOffset;
        _yOffset = yOffset;
    }

    /**
     * @return the point on our component to align at
     */
    public Alignment getMyPoint() {
        return _myPoint;
    }

    public void setMyPoint(final Alignment myPoint) {
        _myPoint = myPoint;
    }

    /**
     * @return the component or container to align to
     */
    public UIComponent getParent() {
        return _parent;
    }

    public void setParent(final UIComponent parent) {
        _parent = parent;
    }

    /**
     * @return the point on the parent to align my point to
     */
    public Alignment getParentPoint() {
        return _parentPoint;
    }

    public void setParentPoint(final Alignment parentPoint) {
        _parentPoint = parentPoint;
    }

    /**
     * @return a value added to our component's x position after the alignment is done
     */
    public int getXOffset() {
        return _xOffset;
    }

    public void setXOffset(final int offset) {
        _xOffset = offset;
    }

    /**
     * @return a value added to our component's y position after the alignment is done.
     */
    public int getYOffset() {
        return _yOffset;
    }

    public void setYOffset(final int offset) {
        _yOffset = offset;
    }
}