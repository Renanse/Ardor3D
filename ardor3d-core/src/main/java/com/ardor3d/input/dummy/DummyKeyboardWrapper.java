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

import com.ardor3d.input.keyboard.KeyEvent;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.google.common.collect.PeekingIterator;

/**
 * A "do-nothing" implementation of KeyboardWrapper useful when you want to ignore (or do not need)
 * key events.
 */
public class DummyKeyboardWrapper implements KeyboardWrapper {
  public static final DummyKeyboardWrapper INSTANCE = new DummyKeyboardWrapper();

  PeekingIterator<KeyEvent> empty = new PeekingIterator<>() {

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public void remove() {}

    @Override
    public KeyEvent peek() {
      return null;
    }

    @Override
    public KeyEvent next() {
      return null;
    }
  };

  @Override
  public PeekingIterator<KeyEvent> getKeyEvents() { return empty; }

  @Override
  public void init() {
    // ignore, does nothing.
  }

}
