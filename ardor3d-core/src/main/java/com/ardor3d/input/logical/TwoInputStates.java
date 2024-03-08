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

import static com.ardor3d.util.Preconditions.checkNotNull;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.InputState;

/**
 * Wrapper class to make it possible to evaluate before/after conditions for triggering actions based on user input.
 */
@Immutable
public final class TwoInputStates {
  private final InputState _previous;
  private final InputState _current;

  /**
   * Instantiates a new TwoInputStates. It is safe for both parameters to point to the same instance,
   * but they cannot be null.
   * 
   * @param previous
   *          the previous input state
   * @param current
   *          the current input state
   * 
   * @throws NullPointerException
   *           if either parameter is null
   */
  public TwoInputStates(final InputState previous, final InputState current) {
    _previous = checkNotNull(previous, "previous");
    _current = checkNotNull(current, "current");
  }

  public InputState getPrevious() { return _previous; }

  public InputState getCurrent() { return _current; }

  @Override
  public int hashCode() {
    // we don't expect this to be used in a map.
    assert false : "hashCode not designed";
    return 42;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TwoInputStates comp)) {
      return false;
    }
    return _previous == comp._previous && _current == comp._current;
  }
}
