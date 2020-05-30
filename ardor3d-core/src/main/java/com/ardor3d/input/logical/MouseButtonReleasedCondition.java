/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.logical;

import java.util.function.Predicate;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.InputState;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;

/**
 * A condition that is true if a given button was pressed when going from the previous input state
 * to the current one.
 */
@Immutable
public final class MouseButtonReleasedCondition implements Predicate<TwoInputStates> {
  private final MouseButton _button;

  /**
   * Construct a new MouseButtonPressedCondition.
   *
   * @param button
   *          the button that should be pressed to trigger this condition
   * @throws NullPointerException
   *           if the button is null
   */
  public MouseButtonReleasedCondition(final MouseButton button) {
    if (button == null) {
      throw new NullPointerException();
    }

    _button = button;
  }

  @Override
  public boolean test(final TwoInputStates states) {
    final InputState currentState = states.getCurrent();
    final InputState previousState = states.getPrevious();

    if (currentState == null || previousState == null
        || !previousState.getMouseState().hasButtonState(ButtonState.DOWN)) {
      return false;
    }

    return currentState.getMouseState().getButtonsReleasedSince(previousState.getMouseState()).contains(_button);
  }
}
