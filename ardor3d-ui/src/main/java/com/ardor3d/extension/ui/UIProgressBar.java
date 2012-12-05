/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui;

import com.ardor3d.extension.ui.border.EmptyBorder;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.util.Insets;

/**
 * This component shows progress as "percent complete" via a proportionally sized bar and an optional text label.
 */
public class UIProgressBar extends UIPanel {

    /** Text label shown on top of the progress bar itself. */
    private final UILabel _barLabel;
    /** Text label shown to the side of the progress bar. */
    private final UILabel _textLabel;

    /** The panel representing our percentage progress bar. */
    private final UIPanel _bar;
    /** The panel shown behind our progress bar. */
    private final UIPanel _mainPanel;

    /** How much progress to show, as a percentage. */
    private double _percentFilled = .5;
    /** Whether to show the progress bar horizontally or vertically. */
    private boolean _horizontal = true;

    /**
     * Construct a new, horizontal progress bar.
     */
    public UIProgressBar() {
        this(null, true);
    }

    /**
     * Construct a new progress bar with the given attributes.
     * 
     * @param labelText
     *            text to show next to the progress bar
     * @param horizontal
     *            true for horizontal bar, false for vertical.
     */
    public UIProgressBar(final String labelText, final boolean horizontal) {
        super(new BorderLayout());
        setHorizontal(horizontal);

        _textLabel = new UILabel("");
        _textLabel.setBorder(new EmptyBorder());
        _textLabel.setMargin(new Insets(0, 0, 0, 0));
        _textLabel.setPadding(new Insets(0, 0, 0, 0));
        _textLabel.setLayoutData(BorderLayoutData.WEST);
        setLabelText(labelText);
        add(_textLabel);

        _mainPanel = new UIPanel(new BorderLayout());
        _mainPanel.setLayoutData(BorderLayoutData.CENTER);
        add(_mainPanel);

        _bar = new UIPanel(new BorderLayout());
        _bar.setLayoutData(BorderLayoutData.CENTER);
        _mainPanel.add(_bar);

        _barLabel = new UILabel("");
        _barLabel.setLayoutData(BorderLayoutData.CENTER);
        _mainPanel.add(_barLabel);

        applySkin();

        updateMinimumSizeFromContents();
        compact();

        layout();
    }

    @Override
    public void layout() {
        super.layout();

        if (isHorizontal()) {
            _bar.setLocalComponentWidth((int) (_percentFilled * _bar.getLocalComponentWidth()));
        } else {
            _bar.setLocalComponentHeight((int) (_percentFilled * _bar.getLocalComponentHeight()));
        }
    }

    public boolean isHorizontal() {
        return _horizontal;
    }

    /**
     * Takes affect on next call to layout()
     * 
     * @param horizontal
     *            true for horizontal bar, false for vertical.
     */
    public void setHorizontal(final boolean horizontal) {
        _horizontal = horizontal;
    }

    public double getPercentFilled() {
        return _percentFilled;
    }

    /**
     * Triggers layout if value is not same as current value.
     * 
     * @param value
     */
    public void setPercentFilled(final double value) {
        final double old = _percentFilled;
        _percentFilled = Math.min(value, 1.0);
        if (old != _percentFilled) {
            layout();
        }
    }

    public void setLabelText(final String text) {
        _textLabel.setText(text);
        _textLabel.updateMinimumSizeFromContents();
    }

    public void setBarText(final String text) {
        _barLabel.setText(text);
    }

    public UIPanel getBar() {
        return _bar;
    }

    public UILabel getTextLabel() {
        return _textLabel;
    }

    public UILabel getBarLabel() {
        return _barLabel;
    }

    public UIPanel getMainPanel() {
        return _mainPanel;
    }
}
