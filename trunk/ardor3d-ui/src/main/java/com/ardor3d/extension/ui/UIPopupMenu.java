/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 * 
 * This file is part of Ardor3D.
 * 
 * Ardor3D is free software: you can redistribute it and/or modify it under the terms of its license which may be found
 * in the accompanying LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import java.util.EnumSet;

import com.ardor3d.extension.ui.layout.RowLayout;

/**
 * A special frame meant to display menu items.
 */
public class UIPopupMenu extends UIFrame {

    public UIPopupMenu() {
        super(null, EnumSet.noneOf(UIFrame.FrameButtons.class));
        getContentPanel().setLayout(new RowLayout(false));
        applySkin();
    }

    public void showAt(final int x, int y) {
        final int width = getLocalComponentWidth();
        final int height = getLocalComponentHeight();

        final int displayW = getHud().getWidth();
        final int displayH = getHud().getHeight();

        if (x + width > displayW) {
            setHudX(displayW - width);
        } else {
            setHudX(x - getBorder().getLeft());
        }
        y = y - height;
        if (y < 0) {
            y = 0;
        }
        if (y + height > displayH) {
            y = displayH - height;
        }
        setHudY(y);
        updateGeometricState(0, true);
    }

    public void setHud(final UIHud hud) {
        _parent = hud;
        attachedToHud();
    }

    public void addItem(final UIMenuItem item) {
        getContentPanel().add(item);
    }

    public void removeItem(final UIMenuItem item) {
        getContentPanel().remove(item);
    }

    public void clearItems() {
        getContentPanel().removeAllComponents();
    }
}
