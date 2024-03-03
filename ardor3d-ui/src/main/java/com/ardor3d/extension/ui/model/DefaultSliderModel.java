/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.model;

import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.math.util.MathUtils;

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
   *          lowest possible value (inclusive)
   * @param maxValue
   *          highest possible value (inclusive)
   */
  public DefaultSliderModel(final int minValue, final int maxValue) {
    setMinValue(minValue);
    setMaxValue(maxValue);
  }

  @Override
  public int getCurrentValue() { return _currentValue; }

  @Override
  public void setCurrentValue(final int currentValue) {
    setCurrentValue(currentValue, null);
  }

  @Override
  public void setCurrentValue(final int currentValue, final UISlider slider) {
    if (_currentValue == currentValue) {
      return;
    }
    _currentValue = MathUtils.clamp(currentValue, _minValue, _maxValue);
    if (slider != null) {
      slider.fireChangeEvent();
    }
  }

  @Override
  public int getMaxValue() { return _maxValue; }

  @Override
  public void setMaxValue(final int maxValue) { _maxValue = maxValue; }

  @Override
  public int getMinValue() { return _minValue; }

  @Override
  public void setMinValue(final int minValue) { _minValue = minValue; }
}
