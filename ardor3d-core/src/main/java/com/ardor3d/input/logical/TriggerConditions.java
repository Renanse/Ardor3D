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

import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.util.Timer;

/**
 * Utility methods for getting standard TriggerConditions. To reduce object creation, it may be a
 * good idea to use utility methods here to get immutable conditions that meet common criteria.
 */
public final class TriggerConditions {
  private static final MouseMovedCondition MOUSE_MOVED_CONDITION = new MouseMovedCondition();
  private static final MouseButtonCondition LEFT_DOWN_CONDITION = makeCondition(MouseButton.LEFT, ButtonState.DOWN);
  private static final MouseButtonCondition RIGHT_DOWN_CONDITION = makeCondition(MouseButton.RIGHT, ButtonState.DOWN);
  private static final MouseButtonCondition MIDDLE_DOWN_CONDITION = makeCondition(MouseButton.MIDDLE, ButtonState.DOWN);

  private static final Predicate<TwoInputStates> ALWAYS_TRUE = (final TwoInputStates arg0) -> true;

  private static final Predicate<TwoInputStates> ALWAYS_FALSE = (final TwoInputStates arg0) -> false;

  private static MouseButtonCondition makeCondition(final MouseButton button, final ButtonState state) {
    final EnumMap<MouseButton, ButtonState> map = new EnumMap<>(MouseButton.class);
    for (final MouseButton b : MouseButton.values()) {
      map.put(b, button != b ? ButtonState.UNDEFINED : state);
    }
    return new MouseButtonCondition(map);
  }

  // prevent instantiation
  private TriggerConditions() {

  }

  /**
   * @return a condition that evaluates to true if the mouse has moved
   */
  public static MouseMovedCondition mouseMoved() {
    return MOUSE_MOVED_CONDITION;
  }

  /**
   *
   * @return a condition that is true if the left button is down
   */
  public static MouseButtonCondition leftButtonDown() {
    return LEFT_DOWN_CONDITION;
  }

  /**
   *
   * @return a condition that is true if the right button is down
   */
  public static MouseButtonCondition rightButtonDown() {
    return RIGHT_DOWN_CONDITION;
  }

  /**
   *
   * @return a condition that is true if the middle button is down
   */
  public static MouseButtonCondition middleButtonDown() {
    return MIDDLE_DOWN_CONDITION;
  }

  /**
   * @return a condition that is always true.
   */
  public static Predicate<TwoInputStates> alwaysTrue() {
    return ALWAYS_TRUE;
  }

  /**
   * @return a condition that is always false.
   */
  public static Predicate<TwoInputStates> alwaysFalse() {
    return ALWAYS_FALSE;
  }

  /**
   * @return a condition that is true only every throttleTime seconds.
   */
  public static Predicate<TwoInputStates> passedThrottle(final double throttleTime, final Timer timer) {
    return new Predicate<>() {
      private double lastPass = 0;

      @Override
      public boolean test(final TwoInputStates arg0) {
        final double now = timer.getTimeInSeconds();
        if (now - lastPass >= throttleTime) {
          lastPass = now;
          return true;
        }
        return false;
      }
    };
  }
}
