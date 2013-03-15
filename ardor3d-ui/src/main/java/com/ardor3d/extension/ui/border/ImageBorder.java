/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.border;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.extension.ui.util.SubTexUtil;
import com.ardor3d.renderer.Renderer;

/**
 * This border takes a set of images and draws them around the edge of a UI component. There are eight possible border
 * images - 4 for the sides of the component and 4 for the corners. Of these, only the sides must be provided. If the
 * corners are null, the top and bottom will stretch to fill in the gaps.
 */
public class ImageBorder extends UIBorder {

    private SubTex _leftEdge = null;
    private SubTex _rightEdge = null;
    private SubTex _topEdge = null;
    private SubTex _bottomEdge = null;

    private SubTex _topLeftCorner = null;
    private SubTex _topRightCorner = null;
    private SubTex _bottomLeftCorner = null;
    private SubTex _bottomRightCorner = null;

    /**
     * Construct this border using the given edge images. The corners will not be drawn.
     * 
     * @param leftEdge
     * @param rightEdge
     * @param topEdge
     * @param bottomEdge
     */
    public ImageBorder(final SubTex leftEdge, final SubTex rightEdge, final SubTex topEdge, final SubTex bottomEdge) {
        super(topEdge.getHeight(), leftEdge.getWidth(), bottomEdge.getHeight(), rightEdge.getWidth());

        _leftEdge = leftEdge;
        _rightEdge = rightEdge;
        _topEdge = topEdge;
        _bottomEdge = bottomEdge;
    }

    /**
     * Construct this border using the given edge and side images.
     * 
     * @param leftEdge
     * @param rightEdge
     * @param topEdge
     * @param bottomEdge
     * @param topLeftCorner
     * @param topRightCorner
     * @param bottomLeftCorner
     * @param bottomRightCorner
     */
    public ImageBorder(final SubTex leftEdge, final SubTex rightEdge, final SubTex topEdge, final SubTex bottomEdge,
            final SubTex topLeftCorner, final SubTex topRightCorner, final SubTex bottomLeftCorner,
            final SubTex bottomRightCorner) {
        super(topEdge.getHeight(), leftEdge.getWidth(), bottomEdge.getHeight(), rightEdge.getWidth());

        _leftEdge = leftEdge;
        _rightEdge = rightEdge;
        _topEdge = topEdge;
        _bottomEdge = bottomEdge;
        _topLeftCorner = topLeftCorner;
        _topRightCorner = topRightCorner;
        _bottomLeftCorner = bottomLeftCorner;
        _bottomRightCorner = bottomRightCorner;
    }

    public SubTex getBottomEdge() {
        return _bottomEdge;
    }

    public void setBottomEdge(final SubTex bottomEdge) {
        _bottomEdge = bottomEdge;
    }

    public SubTex getBottomLeftCorner() {
        return _bottomLeftCorner;
    }

    public void setBottomLeftCorner(final SubTex bottomLeftCorner) {
        _bottomLeftCorner = bottomLeftCorner;
    }

    public SubTex getBottomRightCorner() {
        return _bottomRightCorner;
    }

    public void setBottomRightCorner(final SubTex bottomRightCorner) {
        _bottomRightCorner = bottomRightCorner;
    }

    public SubTex getLeftEdge() {
        return _leftEdge;
    }

    public void setLeftEdge(final SubTex leftEdge) {
        _leftEdge = leftEdge;
    }

    public SubTex getRightEdge() {
        return _rightEdge;
    }

    public void setRightEdge(final SubTex rightEdge) {
        _rightEdge = rightEdge;
    }

    public SubTex getTopEdge() {
        return _topEdge;
    }

    public void setTopEdge(final SubTex topEdge) {
        _topEdge = topEdge;
    }

    public SubTex getTopLeftCorner() {
        return _topLeftCorner;
    }

    public void setTopLeftCorner(final SubTex topLeftCorner) {
        _topLeftCorner = topLeftCorner;
    }

    public SubTex getTopRightCorner() {
        return _topRightCorner;
    }

    public void setTopRightCorner(final SubTex topRightCorner) {
        _topRightCorner = topRightCorner;
    }

    @Override
    public void draw(final Renderer renderer, final UIComponent comp) {

        // get our general width and height
        final int borderWidth = UIBorder.getBorderWidth(comp);
        final int borderHeight = UIBorder.getBorderHeight(comp);

        // Figure out our bottom left corner
        final double dX = comp.getMargin().getLeft();
        final double dY = comp.getMargin().getBottom();

        {
            // draw bottom - stretched to fit
            double leftwidth = _bottomLeftCorner != null ? _bottomLeftCorner.getWidth() : 0;
            double rightwidth = _bottomRightCorner != null ? _bottomRightCorner.getWidth() : 0;
            double x = dX + leftwidth;
            double y = dY;
            double width = borderWidth - leftwidth - rightwidth;
            double height = _bottomEdge.getHeight();
            SubTexUtil.drawSubTex(renderer, _bottomEdge, x, y, width, height, comp.getWorldTransform());

            // draw top - stretched to fit
            leftwidth = _topLeftCorner != null ? _topLeftCorner.getWidth() : 0;
            rightwidth = _topRightCorner != null ? _topRightCorner.getWidth() : 0;
            x = dX + leftwidth;
            y = dY + (borderHeight - _topEdge.getHeight());
            width = borderWidth - leftwidth - rightwidth;
            height = _topEdge.getHeight();
            SubTexUtil.drawSubTex(renderer, _topEdge, x, y, width, height, comp.getWorldTransform());
        }

        {
            // draw left - stretched to fit
            int bottomHeight = _bottomLeftCorner != null ? _bottomLeftCorner.getHeight() : _bottomEdge.getHeight();
            int topHeight = _topLeftCorner != null ? _topLeftCorner.getHeight() : _topEdge.getHeight();
            double x = dX;
            double y = dY + bottomHeight;
            double width = _leftEdge.getWidth();
            double height = borderHeight - bottomHeight - topHeight;
            SubTexUtil.drawSubTex(renderer, _leftEdge, x, y, width, height, comp.getWorldTransform());

            // draw right - stretched to fit
            bottomHeight = _bottomRightCorner != null ? _bottomRightCorner.getHeight() : _bottomEdge.getHeight();
            topHeight = _topRightCorner != null ? _topRightCorner.getHeight() : _topEdge.getHeight();
            x = dX + (borderWidth - _rightEdge.getWidth());
            y = dY + bottomHeight;
            width = _rightEdge.getWidth();
            height = borderHeight - bottomHeight - topHeight;
            SubTexUtil.drawSubTex(renderer, _rightEdge, x, y, width, height, comp.getWorldTransform());
        }

        // draw corners - not stretched
        if (_topLeftCorner != null) {
            SubTexUtil.drawSubTex(renderer, _topLeftCorner, dX, dY + (borderHeight - _topLeftCorner.getHeight()),
                    _topLeftCorner.getWidth(), _topLeftCorner.getHeight(), comp.getWorldTransform());
        }
        if (_bottomLeftCorner != null) {
            SubTexUtil.drawSubTex(renderer, _bottomLeftCorner, dX, dY, _bottomLeftCorner.getWidth(), _bottomLeftCorner
                    .getHeight(), comp.getWorldTransform());
        }
        if (_topRightCorner != null) {
            SubTexUtil.drawSubTex(renderer, _topRightCorner, dX + (borderWidth - _topRightCorner.getWidth()), dY
                    + (borderHeight - _topRightCorner.getHeight()), _topRightCorner.getWidth(), _topRightCorner
                    .getHeight(), comp.getWorldTransform());
        }
        if (_bottomRightCorner != null) {
            SubTexUtil.drawSubTex(renderer, _bottomRightCorner, dX + (borderWidth - _bottomRightCorner.getWidth()), dY,
                    _bottomRightCorner.getWidth(), _bottomRightCorner.getHeight(), comp.getWorldTransform());
        }

    }

}
