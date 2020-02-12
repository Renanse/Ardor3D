/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.UIPopupMenu.SubMenuBehavior;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.input.InputState;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.scenegraph.Node;

/**
 * A simple extension of Button to be used in pop-up menus.
 */
public class UIMenuItem extends UIButton {

    protected UIPopupMenu _subMenu;

    public UIMenuItem(final String text) {
        this(text, null);
    }

    public UIMenuItem(final String text, final SubTex icon) {
        this(text, icon, true, null);
    }

    public UIMenuItem(final String text, final SubTex icon, final boolean closeMenuOnSelect,
            final ActionListener listener) {
        super(text, icon);

        _defaultState = new DefaultState();
        _pressedState = new PressedState();
        _mouseOverState = new MouseOverState();
        applySkin();
        switchState(_defaultState);

        if (listener != null) {
            addActionListener(listener);
        }

        // Close menus when a menu item is clicked
        if (closeMenuOnSelect) {
            addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    final UIHud hud = getHud();
                    if (hud != null) {
                        hud.closePopupMenus();
                    }
                }
            });
        }
    }

    public UIMenuItem(final String text, final SubTex icon, final UIPopupMenu subMenu) {
        this(text, icon, false, null);
        _subMenu = subMenu;
        addActionListener(e -> showSubMenu());
    }

    protected void showSubMenu() {
        final UIHud hud = getHud();
        if (hud == null || _subMenu == null) {
            return;
        }

        hud.closePopupMenusAfter(getParent());
        _subMenu.updateMinimumSizeFromContents();
        _subMenu.layout();

        hud.showSubPopupMenu(_subMenu);
        _subMenu.showAt(getHudX() + getLocalComponentWidth() - 5, getHudY() + getLocalComponentHeight());
    }

    @Override
    public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
        final UIPopupMenu.SubMenuBehavior behavior = getMenuBehavior();
        if (behavior == SubMenuBehavior.HOVER_OPEN) {
            if (state != null && state.getMouseState().getButtonStates().containsValue(ButtonState.DOWN)) {
                switchState(getMouseOverState());
            }
            if (_subMenu != null) {
                showSubMenu();
            } else {
                final UIHud hud = getHud();
                if (hud != null) {
                    hud.closePopupMenusAfter(getParent());
                }
            }

        }
        super.mouseEntered(mouseX, mouseY, state);
    }

    protected SubMenuBehavior getMenuBehavior() {
        final Node parent = getParent();
        if (parent instanceof UIPopupMenu) {
            return ((UIPopupMenu) parent).getSubMenuBehavior();
        }
        return SubMenuBehavior.HOVER_OPEN;
    }

    class DefaultState extends LabelState {

        @Override
        public void mouseEntered(final int mouseX, final int mouseY, final InputState state) {
            switchState(getMouseOverState());
        }

        @Override
        public boolean mousePressed(final MouseButton button, final InputState state) {
            switchState(getPressedState());
            return true;
        }
    }

    class PressedState extends LabelState {

        @Override
        public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
            switchState(getDefaultState());
        }

        @Override
        public boolean mouseReleased(final MouseButton button, final InputState state) {
            switchState(getMouseOverState());
            fireActionEvent();
            return true;
        }
    }

    class MouseOverState extends LabelState {

        @Override
        public void mouseDeparted(final int mouseX, final int mouseY, final InputState state) {
            switchState(getDefaultState());
        }

        @Override
        public boolean mousePressed(final MouseButton button, final InputState state) {
            switchState(getPressedState());
            return true;
        }

        @Override
        public boolean mouseReleased(final MouseButton button, final InputState state) {
            fireActionEvent();
            return true;
        }
    }
}
