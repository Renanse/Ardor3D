/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.text;

import com.ardor3d.math.MathUtils;

public class UIIntegerField extends UITextField {

    protected int _minValue = Integer.MIN_VALUE;
    protected int _maxValue = Integer.MAX_VALUE;
    protected int _value;

    @Override
    protected String validateInputText(final String inputText, final String oldText) {
        String valText = super.validateInputText(inputText, oldText);
        if (valText == null) {
            return null;
        }

        // remove any non integer items
        valText = valText.replaceAll("[^\\d-]", "");
        final boolean neg = valText.charAt(0) == '-';
        valText = (neg ? "-" : "") + valText.replaceAll("[^\\d]", "");
        return valText;
    }

    public void setValue(final int value) {
        _value = MathUtils.clamp(value, _minValue, _maxValue);
        setText(Integer.toString(_value));
    }

    public int getValue() {
        return _value;
    }

    public void setMinimumValue(final int value) {
        _minValue = value;
    }

    public int getMinimumValue() {
        return _minValue;
    }

    public void setMaximumValue(final int value) {
        _maxValue = value;
    }

    public int getMaximumValue() {
        return _maxValue;
    }

    @Override
    public void lostFocus() {
        // force a clamp to [min, max] range
        final String text = getText();
        _value = text == "" ? 0 : MathUtils.clamp(Integer.parseInt(text), _minValue, _maxValue);

        super.lostFocus();
    }
}
