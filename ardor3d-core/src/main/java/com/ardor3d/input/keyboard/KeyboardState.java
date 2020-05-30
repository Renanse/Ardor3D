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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.ardor3d.annotation.Immutable;

/**
 * A keyboard state at some point in time. Contains an EnumSet of the keys that are down, as well as
 * a KeyEvent that describes the latest event (a key being pressed or released).
 */
@Immutable
public class KeyboardState {
  public static final KeyboardState NOTHING = new KeyboardState(EnumSet.noneOf(Key.class), KeyEvent.NOTHING);

  private final EnumSet<Key> _keysDown;
  private final Set<Key> _keysDownView;
  private final KeyEvent _keyEvent;

  public KeyboardState(final EnumSet<Key> keysDown, final KeyEvent keyEvent) {
    // keeping the keysDown as an EnumSet rather than as an unmodifiableSet in order to get
    // the performance benefit of working with the fast implementations of contains(),
    // removeAll(), etc., in EnumSet. The intention is that the keysDown set should never change.
    // The reason why the performance benefits are lost when using an unmodifiableSet is that
    // methods like containsAll(), etc., are not symmetrical in the EnumSet implementations.
    // So typically, unmodifiableSet.containsAll(EnumSet) will be faster than
    // enumSet.containsAll(unmodifiableSet).
    _keysDown = keysDown;
    _keyEvent = keyEvent;
    _keysDownView = Collections.unmodifiableSet(keysDown);
  }

  public boolean isDown(final Key key) {
    return _keysDown.contains(key);
  }

  public boolean isAllDown(final Key... keys) {
    for (final Key key : keys) {
      if (!_keysDown.contains(key)) {
        return false;
      }
    }
    return true;
  }

  public boolean isAtLeastOneDown(final Key... keys) {
    for (final Key key : keys) {
      if (_keysDown.contains(key)) {
        return true;
      }
    }
    return false;
  }

  public Set<Key> getKeysDown() { return _keysDownView; }

  public KeyEvent getKeyEvent() { return _keyEvent; }

  public EnumSet<Key> getKeysReleasedSince(final KeyboardState previous) {
    final EnumSet<Key> result = EnumSet.copyOf(previous._keysDown);

    result.removeAll(_keysDown);

    return result;
  }

  public EnumSet<Key> getKeysPressedSince(final KeyboardState previous) {
    final EnumSet<Key> result = EnumSet.copyOf(_keysDown);

    result.removeAll(previous._keysDown);

    return result;

  }

  public EnumSet<Key> getKeysHeldSince(final KeyboardState previous) {
    final EnumSet<Key> result = EnumSet.copyOf(_keysDown);

    result.retainAll(previous._keysDown);

    return result;

  }

  @Override
  public String toString() {
    return "KeyboardState{_keysDown=" + _keysDown + ", _keyEvent=" + _keyEvent + '}';
  }

}
