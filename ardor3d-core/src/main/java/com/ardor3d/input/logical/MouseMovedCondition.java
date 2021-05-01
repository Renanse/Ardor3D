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

import java.util.function.Predicate;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.InputState;

/**
 * A condition that is true if the mouse has moved between the two input states.
 */
@Immutable
public final class MouseMovedCondition implements Predicate<TwoInputStates> {
  @Override
  public boolean test(final TwoInputStates states) {
    final InputState currentState = states.getCurrent();
    final InputState previousState = states.getPrevious();

    if (currentState == null) {
      return false;
    }

    if (currentState.equals(previousState)) {
      return false;
    }

    return currentState.getMouseState().getDx() != 0 || currentState.getMouseState().getDy() != 0;
  }
}
