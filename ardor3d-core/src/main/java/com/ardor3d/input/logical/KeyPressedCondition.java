/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
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
import com.ardor3d.input.keyboard.Key;

/**
 * A condition that is true if a given key was pressed when going from the previous input state to
 * the current one.
 */
@Immutable
public final class KeyPressedCondition implements Predicate<TwoInputStates> {
  private final Key key;

  /**
   * Construct a new KeyPressedCondition.
   *
   * @param key
   *          the key that should be held
   * @throws IllegalArgumentException
   *           if the key is null
   */
  public KeyPressedCondition(final Key key) {
    if (key == null) {
      throw new IllegalArgumentException("key was null");
    }

    this.key = key;
  }

  @Override
  public boolean test(final TwoInputStates states) {
    final InputState currentState = states.getCurrent();
    final InputState previousState = states.getPrevious();

    return currentState.getKeyboardState().getKeysPressedSince(previousState.getKeyboardState()).contains(key);
  }
}
