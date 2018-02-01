/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.RowLayout;

public class UIIntegerRollerField extends UIContainer {

    protected UIIntegerField _field;
    protected UIButton _rollUp;
    protected UIButton _rollDown;

    protected int _interval = 1;
    private UIPanel _buttonPanel;

    public UIIntegerRollerField() {
        setLayout(new BorderLayout());
        initComponents();
        applySkin();
    }

    protected void initComponents() {
        _field = new UIIntegerField();
        _field.setLayoutData(BorderLayoutData.CENTER);
        add(_field);

        _rollUp = new UIButton("^");
        _rollUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                setValue(getValue() + _interval);
            }
        });

        _rollDown = new UIButton("\\/");
        _rollDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                setValue(getValue() - _interval);
            }
        });

        _buttonPanel = new UIPanel(new RowLayout(false));
        _buttonPanel.setLayoutData(BorderLayoutData.EAST);
        _buttonPanel.add(_rollUp);
        _buttonPanel.add(_rollDown);
        add(_buttonPanel);
    }

    public void setValue(final int value) {
        _field.setValue(value);
    }

    public int getValue() {
        return _field.getValue();
    }

    public void setMinimumValue(final int value) {
        _field.setMinimumValue(value);
    }

    public int getMinimumValue() {
        return _field.getMinimumValue();
    }

    public void setMaximumValue(final int value) {
        _field.setMaximumValue(value);
    }

    public int getMaximumValue() {
        return _field.getMaximumValue();
    }

    public UIIntegerField getField() {
        return _field;
    }

    public UIButton getRollUpButton() {
        return _rollUp;
    }

    public UIButton getRollDownButton() {
        return _rollDown;
    }

    public UIPanel getButtonPanel() {
        return _buttonPanel;
    }
}
