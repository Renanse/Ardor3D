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

import java.util.EnumSet;

import com.ardor3d.extension.ui.UIDrawer.DrawerButtons;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.input.InputState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;

public class UIDrawerBar extends UIPanel {

    /** The title text label, and also our frame drag handle. */
    protected final UILabel _titleLabel;

    /** Close button used for removing the parent frame from its hud, hiding it. */
    protected final UIButton _closeButton;

    protected final UIDrawer _drawer;

    /**
     * Construct a new UIFrameBar, adding the buttons as specified in the given EnumSet
     *
     * @param buttons
     *            the button types we want shown
     * @param drawer
     *            our parent drawer
     */
    public UIDrawerBar(final EnumSet<DrawerButtons> buttons, final UIDrawer drawer) {
        super("frameBar");

        _drawer = drawer;

        _titleLabel = new UILabel("- untitled -");
        attachChild(_titleLabel);

        if (buttons.contains(DrawerButtons.CLOSE)) {
            _closeButton = createBarButton("x");
            _closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    _drawer.close();
                }
            });
            attachChild(_closeButton);
        } else {
            _closeButton = null;
        }

        switch (drawer.getEdge()) {
            case LEFT:
                setLayoutData(BorderLayoutData.EAST);
                setLayout(new RowLayout(false));
                _titleLabel.setRotation(new Matrix3().fromAngleNormalAxis(-MathUtils.HALF_PI, Vector3.UNIT_Z));
                break;
            case RIGHT:
                setLayoutData(BorderLayoutData.WEST);
                setLayout(new RowLayout(false));
                remove(_titleLabel);
                add(_titleLabel);
                _titleLabel.setRotation(new Matrix3().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.UNIT_Z));
                break;
            case TOP:
                setLayoutData(BorderLayoutData.SOUTH);
                break;
            default:
            case BOTTOM:
                setLayoutData(BorderLayoutData.NORTH);
                break;
        }
    }

    private UIButton createBarButton(final String string) {
        // Generate a standardized button.
        final UIButton rVal = new UIButton(string);
        rVal.refreshState();
        return rVal;
    }

    public UIButton getCloseButton() {
        return _closeButton;
    }

    public UILabel getTitleLabel() {
        return _titleLabel;
    }

    @Override
    public boolean mouseClicked(final MouseButton button, final InputState state) {
        final int mouseX = state.getMouseState().getX(), mouseY = state.getMouseState().getY();
        final UIComponent over = getUIComponent(mouseX, mouseY);
        if (over == _titleLabel || over == this) {
            _drawer.toggleExpanded();
        }
        return true;
    }
}
