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

import com.ardor3d.math.MathUtils;

public class UIDoubleField extends UITextField {

    protected double _minValue = Double.NEGATIVE_INFINITY;
    protected double _maxValue = Double.POSITIVE_INFINITY;
    protected double _value;

    protected int _decimalPlaces = 2;
    protected boolean _displayScientific = false;

    public UIDoubleField() {}

    public UIDoubleField(final double value) {
        setValue(value);
    }

    enum ReadState {
        CoeffSign, CoeffInteger, CoeffFraction, ExponentSign, ExponentInteger
    }

    @Override
    protected String validateInputText(final String inputText, final String oldText) {
        final String valText = super.validateInputText(inputText, oldText);
        if (valText == null) {
            return null;
        }

        ReadState state = ReadState.CoeffSign;
        final StringBuilder rVal = new StringBuilder();

        for (int i = 0, maxI = valText.length(); i < maxI; i++) {
            // walk through our text
            final char c = valText.charAt(i);
            switch (state) {
                case CoeffSign:
                    if (Character.isDigit(c) || c == '-' || c == '+') {
                        rVal.append(c);
                        state = ReadState.CoeffInteger;
                        continue;
                    } else if (c == '.') {
                        rVal.append(c);
                        state = ReadState.CoeffFraction;
                        continue;
                    }
                    // invalid, so return oldText
                    return oldText;
                case CoeffInteger:
                    if (Character.isDigit(c)) {
                        rVal.append(c);
                        continue;
                    } else if (c == '.') {
                        rVal.append(c);
                        state = ReadState.CoeffFraction;
                        continue;
                    } else if (c == 'e' || c == 'E') {
                        rVal.append(c);
                        state = ReadState.ExponentSign;
                        continue;
                    }
                    // invalid, so return oldText
                    return oldText;
                case CoeffFraction:
                    if (Character.isDigit(c)) {
                        rVal.append(c);
                        continue;
                    } else if (c == 'e' || c == 'E') {
                        rVal.append(c);
                        state = ReadState.ExponentSign;
                        continue;
                    }
                    // invalid, so return oldText
                    return oldText;
                case ExponentSign:
                    if (Character.isDigit(c) || c == '-' || c == '+') {
                        rVal.append(c);
                        state = ReadState.ExponentInteger;
                        continue;
                    }
                    // invalid, so return oldText
                    return oldText;
                case ExponentInteger:
                    if (Character.isDigit(c)) {
                        rVal.append(c);
                        continue;
                    }
                    // invalid, so return oldText
                    return oldText;
                default:
                    // invalid, so return oldText
                    return oldText;
            }
        }
        final boolean editing = isFocused();
        if (!editing) {
            while (rVal.length() > 0 && !Character.isDigit(rVal.charAt(rVal.length() - 1))) {
                rVal.setLength(rVal.length() - 1);
            }
        }
        if (rVal.length() == 0) {
            return null;
        } else {
            return rVal.toString();
        }
    }

    @Override
    protected String formatRawText(final String rawText) {
        if (isFocused()) {
            // don't apply formatting if we're focused
            return rawText;
        }
        if (_displayScientific) {
            return String.format("%." + (_decimalPlaces + 1) + "g", _value);
        } else {
            return String.format("%." + _decimalPlaces + "f", _value);
        }
    }

    public void setValue(final double value) {
        _value = MathUtils.clamp(value, _minValue, _maxValue);
        setText(Double.toString(_value));
    }

    public double getValue() {
        return _value;
    }

    public void setMinimumValue(final double value) {
        _minValue = value;
    }

    public double getMinimumValue() {
        return _minValue;
    }

    public void setMaximumValue(final double value) {
        _maxValue = value;
    }

    public double getMaximumValue() {
        return _maxValue;
    }

    public void setDecimalPlaces(final int places) {
        _decimalPlaces = places;
    }

    public int getDecimalPlaces() {
        return _decimalPlaces;
    }

    public boolean isDisplayScientific() {
        return _displayScientific;
    }

    public void setDisplayScientific(final boolean displayScientific) {
        _displayScientific = displayScientific;
    }

    @Override
    public void lostFocus() {
        // force a clamp to [min, max] range
        final String text = getText();
        _value = text.trim() == "" ? 0.0 : MathUtils.clamp(Double.parseDouble(text), _minValue, _maxValue);

        super.lostFocus();
    }
}
