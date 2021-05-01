/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.logical;

import java.util.EnumMap;
import java.util.function.Predicate;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.InputState;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;

/**
 * A condition that checks the state of the two most commonly used mouse buttons.
 */
@Immutable
public final class MouseButtonCondition implements Predicate<TwoInputStates> {
  private final EnumMap<MouseButton, ButtonState> _states = new EnumMap<>(MouseButton.class);

  public MouseButtonCondition(final EnumMap<MouseButton, ButtonState> states) {
    _states.putAll(states);
  }

  public MouseButtonCondition(final ButtonState left, final ButtonState right, final ButtonState middle) {
    if (left != ButtonState.UNDEFINED) {
      _states.put(MouseButton.LEFT, left);
    }
    if (right != ButtonState.UNDEFINED) {
      _states.put(MouseButton.RIGHT, right);
    }
    if (middle != ButtonState.UNDEFINED) {
      _states.put(MouseButton.MIDDLE, middle);
    }
  }

  @Override
  public boolean test(final TwoInputStates states) {
    final InputState currentState = states.getCurrent();

    if (currentState == null) {
      return false;
    }

    for (final MouseButton button : _states.keySet()) {
      final ButtonState required = _states.get(button);
      if (required != ButtonState.UNDEFINED) {
        if (currentState.getMouseState().getButtonState(button) != required) {
          return false;
        }
      }
    }
    return true;
  }
}
