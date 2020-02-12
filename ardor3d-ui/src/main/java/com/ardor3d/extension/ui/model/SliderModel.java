/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.model;

import com.ardor3d.extension.ui.UISlider;

public interface SliderModel {

    /**
     * @return the highest possible value (inclusive)
     */
    int getMaxValue();

    /**
     * @param maxValue
     *            the new highest possible value (inclusive)
     */
    void setMaxValue(int maxValue);

    /**
     * @return the current set value
     */
    int getCurrentValue();

    /**
     * Set a new current value. No change event will be fired.
     * 
     * @param currentValue
     *            the new current value
     */
    void setCurrentValue(int currentValue);

    /**
     * Set a new current value. If a slider is provided, fireChangeEvent will be called on it.
     * 
     * @param currentValue
     *            the new current value
     * @param slider
     *            the slider to call fireChangeEvent, if not null.
     */
    void setCurrentValue(int value, UISlider slider);

    /**
     * @return the lowest possible value (inclusive)
     */
    int getMinValue();

    /**
     * @param minValue
     *            the new lowest possible value (inclusive)
     */
    void setMinValue(int minValue);

}
