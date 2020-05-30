/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.keyboard;

import com.ardor3d.annotation.Immutable;

/**
 * Describes the state of a physical key - either it has been pressed or it has been released.
 */
@Immutable
public class KeyEvent {
  public static final KeyEvent NOTHING = new KeyEvent(Key.UNKNOWN, KeyState.UP);

  private final Key _key;
  private final KeyState _state;

  public KeyEvent(final Key key, final KeyState state) {
    _key = key;
    _state = state;
  }

  public Key getKey() { return _key; }

  public KeyState getState() { return _state; }

  @Override
  public String toString() {
    return "KeyEvent{" + "_key=" + _key + ", _state=" + _state + '}';
  }
}
