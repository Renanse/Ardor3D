/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import java.util.EnumSet;

import com.ardor3d.extension.ui.UIFrame.FrameButtons;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;

/**
 * This panel extension defines a standard frame title bar with optional buttons you might find in a typical windowing
 * system (such as minimize, close, etc.)
 */
public class UIFrameBar extends UIPanel {

    /** The title text label, and also our frame drag handle. */
    private final UILabel _titleLabel;

    /** Help button. */
    private final UIButton _helpButton;
    /** Minimize button. */
    private final UIButton _minimizeButton;
    /** Maximize button. */
    private final UIButton _maximizeButton;
    /** Close button used for removing the parent frame from its hud, hiding it. */
    private final UIButton _closeButton;

    /**
     * Construct a new UIFrameBar, adding the buttons as specified in the given EnumSet
     *
     * @param buttons
     *            the button types we want shown
     */
    public UIFrameBar(final EnumSet<FrameButtons> buttons) {
        super("frameBar");

        _titleLabel = new UILabel("- untitled -");
        attachChild(_titleLabel);

        if (buttons.contains(FrameButtons.HELP)) {
            _helpButton = createFrameButton("?");
            _helpButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    // TODO: Implement
                }
            });
            attachChild(_helpButton);
        } else {
            _helpButton = null;
        }

        if (buttons.contains(FrameButtons.MINIMIZE)) {
            _minimizeButton = createFrameButton("_");
            _minimizeButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    // TODO: Implement
                }
            });
            attachChild(_minimizeButton);
        } else {
            _minimizeButton = null;
        }

        if (buttons.contains(FrameButtons.MAXIMIZE)) {
            _maximizeButton = createFrameButton("^");
            _maximizeButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    // XXX: Should this also update the button img?
                    final UIFrame frame = (UIFrame) getParent();
                    if (frame.isMaximized()) {
                        frame.restore();
                    } else {
                        frame.maximize();
                    }
                }
            });
            attachChild(_maximizeButton);
        } else {
            _maximizeButton = null;
        }

        if (buttons.contains(FrameButtons.CLOSE)) {
            _closeButton = createFrameButton("x");
            _closeButton.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent event) {
                    ((UIFrame) getParent()).close();
                }
            });
            attachChild(_closeButton);
        } else {
            _closeButton = null;
        }
    }

    private UIButton createFrameButton(final String string) {
        // Generate a standardized button.
        final UIButton rVal = new UIButton(string);
        rVal.refreshState();
        return rVal;
    }

    public UIButton getCloseButton() {
        return _closeButton;
    }

    public UIButton getExpandButton() {
        return _maximizeButton;
    }

    public UIButton getHelpButton() {
        return _helpButton;
    }

    public UIButton getMinimizeButton() {
        return _minimizeButton;
    }

    public UILabel getTitleLabel() {
        return _titleLabel;
    }
}
