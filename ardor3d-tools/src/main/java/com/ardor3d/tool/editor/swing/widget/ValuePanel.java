/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.tool.editor.swing.widget;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

public class ValuePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static Font LABEL_FONT = new Font("Arial", Font.BOLD, 13);

    public ValueSpinner _spinner;

    private JLabel _plabel, _slabel;

    public ValuePanel(final String prefix, final String suffix, final float min, final float max, final float step) {
        add(_plabel = createLabel(prefix));
        add(_spinner = new ValueSpinner(min, max, step));
        add(_slabel = createLabel(suffix));
    }

    public ValuePanel(final String prefix, final String suffix, final double min, final double max, final double step) {
        add(_plabel = createLabel(prefix));
        add(_spinner = new ValueSpinner(min, max, step));
        add(_slabel = createLabel(suffix));
    }

    public ValuePanel(final String prefix, final String suffix, final int min, final int max, final int step) {
        add(_plabel = createLabel(prefix));
        add(_spinner = new ValueSpinner(min, max, step));
        add(_slabel = createLabel(suffix));
    }

    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        _plabel.setEnabled(enabled);
        _spinner.setEnabled(enabled);
        _slabel.setEnabled(enabled);
    }

    public void setValue(final double value) {
        _spinner.setValue(Double.valueOf(value));
    }

    public void setValue(final float value) {
        _spinner.setValue(Float.valueOf(value));
    }

    public void setValue(final int value) {
        _spinner.setValue(Integer.valueOf(value));
    }

    public double getDoubleValue() {
        return ((Number) _spinner.getValue()).floatValue();
    }

    public float getFloatValue() {
        return ((Number) _spinner.getValue()).floatValue();
    }

    public int getIntValue() {
        return ((Number) _spinner.getValue()).intValue();
    }

    public void addChangeListener(final ChangeListener l) {
        _spinner.addChangeListener(l);
    }

    private JLabel createLabel(final String text) {
        final JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        return label;
    }
}
