/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.character;

import com.ardor3d.annotation.Immutable;

@Immutable
public class CharacterInputEvent {
  private final char _value;

  public CharacterInputEvent(final char value) {
    _value = value;
  }

  public char getValue() { return _value; }

  @Override
  public String toString() {
    return "CharacterInputEvent{" + "_value=" + _value + '}';
  }
}
