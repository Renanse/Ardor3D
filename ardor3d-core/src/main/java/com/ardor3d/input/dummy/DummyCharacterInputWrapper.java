/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.dummy;

import com.ardor3d.input.character.CharacterInputEvent;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.ardor3d.util.PeekingIterator;

public class DummyCharacterInputWrapper implements CharacterInputWrapper {

  public static final DummyCharacterInputWrapper INSTANCE = new DummyCharacterInputWrapper();

  final PeekingIterator<CharacterInputEvent> empty = new PeekingIterator<>() {
    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public void remove() {}

    @Override
    public CharacterInputEvent peek() {
      return null;
    }

    @Override
    public CharacterInputEvent next() {
      return null;
    }
  };

  @Override
  public PeekingIterator<CharacterInputEvent> getCharacterEvents() { return empty; }

  @Override
  public void init() {
    // ignore, does nothing
  }

}
