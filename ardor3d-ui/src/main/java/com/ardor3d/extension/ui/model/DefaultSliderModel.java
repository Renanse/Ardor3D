/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.model;

import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.math.MathUtils;

/**
 * A backing data model for UISlider.
 */
public class DefaultSliderModel implements SliderModel {

    /** Minimum range value */
    private int _minValue;

    /** Maximum range value */
    private int _maxValue;

    /** Current value */
    private int _currentValue;

    /**
     * Construct a new model
     * 
     * @param minValue
     *            lowest possible value (inclusive)
     * @param maxValue
     *            highest possible value (inclusive)
     */
    public DefaultSliderModel(final int minValue, final int maxValue) {
        setMinValue(minValue);
        setMaxValue(maxValue);
    }

    public int getCurrentValue() {
        return _currentValue;
    }

    public void setCurrentValue(final int currentValue) {
        setCurrentValue(currentValue, null);
    }

    public void setCurrentValue(final int currentValue, final UISlider slider) {
        if (_currentValue == currentValue) {
            return;
        }
        _currentValue = MathUtils.clamp(currentValue, _minValue, _maxValue);
        if (slider != null) {
            slider.fireChangeEvent();
        }
    }

    public int getMaxValue() {
        return _maxValue;
    }

    public void setMaxValue(final int maxValue) {
        _maxValue = maxValue;
    }

    public int getMinValue() {
        return _minValue;
    }

    public void setMinValue(final int minValue) {
        _minValue = minValue;
    }
}