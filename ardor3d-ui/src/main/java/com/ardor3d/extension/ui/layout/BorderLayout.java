/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.layout;

import java.util.List;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.scenegraph.Spatial;

/**
 * This layout places components on the edges or in the center of a container, depending on the value of the layout data
 * object they hold. The behavior is meant to be similar to awt's {@link java.awt.BorderLayout BorderLayout}.
 *
 * @see BorderLayoutData
 */
public class BorderLayout extends UILayout {

    @Override
    public void layoutContents(final UIContainer container) {
        if (container.getNumberOfChildren() < 1) {
            return;
        }
        int widthWest = 0;
        int widthEast = 0;

        int heightNorth = 0;
        int heightSouth = 0;
        final List<Spatial> content = container.getChildren();

        // Go through each component in the given container and determine the width and height of our edges.
        final Rectangle2 store = new Rectangle2();
        for (final Spatial s : content) {
            if (!(s instanceof UIComponent)) {
                continue;
            }
            final UIComponent comp = (UIComponent) s;
            comp.getRelativeMinComponentBounds(store);

            final BorderLayoutData data = (BorderLayoutData) comp.getLayoutData();
            if (data != null) {
                switch (data) {
                    case NORTH:
                        heightNorth = store.getHeight();
                        break;
                    case SOUTH:
                        heightSouth = store.getHeight();
                        break;
                    case EAST:
                        widthEast = store.getWidth();
                        break;
                    case WEST:
                        widthWest = store.getWidth();
                        break;
                    case CENTER:
                        // nothing to do
                        break;
                }
            }
        }

        // Using the information from the last pass, set the position and size of each component in the container.
        for (final Spatial s : content) {
            if (!(s instanceof UIComponent)) {
                continue;
            }
            final UIComponent comp = (UIComponent) s;
            comp.getRelativeMinComponentBounds(store);

            final BorderLayoutData data = (BorderLayoutData) comp.getLayoutData();

            if (data != null) {
                switch (data) {
                    case NORTH:
                        comp.fitComponentIn(container.getContentWidth(), store.getHeight());
                        comp.getRelativeComponentBounds(store);
                        comp.setLocalXY(-store.getX(), container.getContentHeight() - heightNorth - store.getY());
                        break;
                    case SOUTH:
                        comp.fitComponentIn(container.getContentWidth(), store.getHeight());
                        comp.getRelativeComponentBounds(store);
                        comp.setLocalXY(-store.getX(), -store.getY());
                        break;
                    case EAST:
                        comp.fitComponentIn(store.getWidth(), container.getContentHeight() - heightNorth - heightSouth);
                        comp.getRelativeComponentBounds(store);
                        comp.setLocalXY(container.getContentWidth() - store.getWidth() - 1 - store.getX(), heightSouth
                                - store.getY());
                        break;
                    case WEST:
                        comp.fitComponentIn(store.getWidth(), container.getContentHeight() - heightNorth - heightSouth);
                        comp.getRelativeComponentBounds(store);
                        comp.setLocalXY(-store.getX(), heightSouth - store.getY());
                        break;
                    case CENTER:
                        comp.fitComponentIn(container.getContentWidth() - widthEast - widthWest,
                                container.getContentHeight() - heightSouth - heightNorth);
                        comp.getRelativeComponentBounds(store);
                        comp.setLocalXY(widthWest - store.getX(), heightSouth - store.getY());
                }
            }
        }
    }

    @Override
    public void updateMinimumSizeFromContents(final UIContainer container) {
        container.setLayoutMinimumContentSize(getMinimumWidth(container.getChildren()),
                getMinimumHeight(container.getChildren()));
    }

    private int getMinimumHeight(final List<Spatial> content) {
        int minH = 0;
        int maxEWCH = 0;
        if (content != null) {
            final Rectangle2 store = new Rectangle2();
            for (final Spatial s : content) {
                if (!(s instanceof UIComponent)) {
                    continue;
                }
                final UIComponent comp = (UIComponent) s;
                comp.getRelativeMinComponentBounds(store);
                final BorderLayoutData bld = (BorderLayoutData) comp.getLayoutData();
                if (bld == null) {
                    continue;
                }
                if (bld == BorderLayoutData.SOUTH || bld == BorderLayoutData.NORTH) {
                    minH += store.getHeight();
                } else {
                    final int h = store.getHeight();
                    if (h > maxEWCH) {
                        maxEWCH = h;
                    }
                }
            }
        }

        return minH + maxEWCH;
    }

    private int getMinimumWidth(final List<Spatial> content) {
        int minWidth = 0;
        int maxNSWidth = 0;
        if (content != null) {
            final Rectangle2 store = new Rectangle2();
            for (final Spatial s : content) {
                if (!(s instanceof UIComponent)) {
                    continue;
                }
                final UIComponent comp = (UIComponent) s;
                comp.getRelativeMinComponentBounds(store);
                final BorderLayoutData data = (BorderLayoutData) comp.getLayoutData();
                if (data == BorderLayoutData.EAST || data == BorderLayoutData.WEST || data == BorderLayoutData.CENTER
                        || data == null) {
                    minWidth += store.getWidth();
                } else {
                    final int width = store.getWidth();
                    if (width > maxNSWidth) {
                        maxNSWidth = width;
                    }
                }

            }
        }
        return Math.max(minWidth, maxNSWidth);
    }
}
