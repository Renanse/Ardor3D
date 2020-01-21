/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it under the terms of its license which may be found
 * in the accompanying LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

/**
 * A special frame meant to display menu items.
 */
public class UIPopupMenu extends UIContainer implements IPopOver {

    public enum SubMenuBehavior {
        INHERIT, HOVER_OPEN, CLICK_TO_OPEN
    }

    private SubMenuBehavior _subMenuBehavior = SubMenuBehavior.INHERIT;

    public UIPopupMenu() {
        super();
        setLayout(new RowLayout(false));
        applySkin();
    }

    @Override
    public void showAt(final int x, int y) {
        final int width = getLocalComponentWidth();
        final int height = getLocalComponentHeight();

        final int displayW = getHud().getWidth();
        final int displayH = getHud().getHeight();

        setHudX(x + width > displayW ? displayW - width : x);

        y = Math.max(0, y - height);
        if (y + height > displayH) {
            y = displayH - height;
        }
        setHudY(y);
        updateGeometricState(0, true);
    }

    @Override
    public void setHud(final UIHud hud) {
        _parent = hud;
        attachedToHud();
    }

    public void addItem(final UIMenuItem item) {
        add(item);
    }

    public void removeItem(final UIMenuItem item) {
        remove(item);
    }

    public void clearItems() {
        removeAllComponents();
    }

    @Override
    public void close() {
        final UIHud hud = getHud();
        if (hud == null) {
            throw new IllegalStateException("UIPopupMenu is not attached to a hud.");
        }

        // Close any open tooltip
        hud.getTooltip().setVisible(false);

        // clear any resources for standin
        clearStandin();

        // clean up any state
        acceptVisitor((final Spatial spatial) -> {
            if (spatial instanceof StateBasedUIComponent) {
                final StateBasedUIComponent comp = (StateBasedUIComponent) spatial;
                comp.switchState(comp.getDefaultState());
            }
        }, true);

        hud.remove(this);
        _parent = null;
    }

    public SubMenuBehavior getSubMenuBehavior() {
        if (_subMenuBehavior == SubMenuBehavior.INHERIT) {
            final Node parent = getParent();
            if (parent instanceof UIPopupMenu) {
                return ((UIPopupMenu) parent).getSubMenuBehavior();
            } else {
                return SubMenuBehavior.HOVER_OPEN;
            }
        }
        return _subMenuBehavior;
    }

    public SubMenuBehavior getLocalSubMenuBehavior() {
        return _subMenuBehavior;
    }

    public void setSubMenuBehavior(final SubMenuBehavior behavior) {
        _subMenuBehavior = behavior;
    }
}
