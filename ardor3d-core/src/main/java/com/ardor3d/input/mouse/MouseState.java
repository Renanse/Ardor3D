/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.mouse;

import java.util.EnumMap;
import java.util.EnumSet;

import com.ardor3d.annotation.Immutable;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableMultiset.Builder;
import com.google.common.collect.Multiset;

/**
 * Describes the mouse state at some point in time.
 */
@Immutable
public class MouseState {
  public static final MouseState NOTHING = new MouseState(0, 0, 0, 0, 0, null, null);

  /**
   * How many milliseconds can pass between mouse press and release before we do not count the
   * interaction as a click.
   */
  public static long CLICK_TIME_MS = 500;

  /**
   * How many pixels a mouse can drift between the position when pressed and the position when
   * released before we ignore the click. Some input platforms may not honor this value.
   */
  public static double CLICK_MAX_DELTA = 15.0;

  private final int _x;
  private final int _y;
  private final int _dx;
  private final int _dy;
  private final int _dwheel;
  private final ImmutableMap<MouseButton, ButtonState> _buttonStates;
  private final ImmutableMultiset<MouseButton> _clickCounts;

  /**
   * Constructs a new MouseState instance.
   *
   * @param x
   *          the mouse's x position
   * @param y
   *          the mouse's y position
   * @param dx
   *          the delta in the mouse's x position since the last update
   * @param dy
   *          the delta in the mouse's y position since the last update
   * @param dwheel
   *          the delta in the mouse's wheel movement since the last update
   * @param buttonStates
   *          the states of the various given buttons.
   * @param clicks
   *          the number of times each button has been clicked
   */
  public MouseState(final int x, final int y, final int dx, final int dy, final int dwheel,
    final EnumMap<MouseButton, ButtonState> buttonStates, final Multiset<MouseButton> clicks) {
    _x = x;
    _y = y;
    _dx = dx;
    _dy = dy;
    _dwheel = dwheel;
    if (buttonStates != null) {
      final com.google.common.collect.ImmutableMap.Builder<MouseButton, ButtonState> builder = ImmutableMap.builder();
      _buttonStates = builder.putAll(buttonStates).build();
    } else {
      _buttonStates = ImmutableMap.of();
    }
    if (clicks != null) {
      final Builder<MouseButton> builder = ImmutableMultiset.builder();
      _clickCounts = builder.addAll(clicks).build();
    } else {
      _clickCounts = ImmutableMultiset.of();
    }
  }

  public int getX() { return _x; }

  public int getY() { return _y; }

  public int getDx() { return _dx; }

  public int getDy() { return _dy; }

  public int getDwheel() { return _dwheel; }

  /**
   *
   * @param state
   *          the button state to look for
   * @return true if at least one mouse button is in the given button state.
   */
  public boolean hasButtonState(final ButtonState state) {
    return _buttonStates.containsValue(state);
  }

  /**
   *
   * @param state
   *          the button to look for
   * @return true if the given mouse button is currently mapped to a state.
   */
  public boolean hasButtonState(final MouseButton button) {
    return _buttonStates.containsKey(button);
  }

  /**
   * Returns all the buttons' states. It could be easier for most classes to use the
   * {@link #getButtonState(MouseButton)} methods, and that also results in less object creation.
   *
   * @return a defensive copy of the states of all the buttons at this point in time.
   */
  public EnumMap<MouseButton, ButtonState> getButtonStates() { return getButtonStates(null); }

  /**
   * Returns all the buttons' states. It could be easier for most classes to use the
   * {@link #getButtonState(MouseButton)} methods, and that also results in less object creation.
   *
   * @param store
   *          a map to store the states in... any values in store are cleared first. If store is null,
   *          a new map is created.
   * @return a defensive copy of the states of all the buttons at this point in time.
   */
  public EnumMap<MouseButton, ButtonState> getButtonStates(final EnumMap<MouseButton, ButtonState> store) {
    EnumMap<MouseButton, ButtonState> rVal = store;
    if (store == null) {
      rVal = new EnumMap<>(MouseButton.class);
    }
    rVal.clear();
    rVal.putAll(_buttonStates);
    return rVal;
  }

  /**
   * Returns the current state for the supplied button, or UP if no state for that button is
   * registered.
   *
   * @param button
   *          the mouse button to check
   * @return the button's state, or {@link ButtonState#UP} if no button state registered.
   */
  public ButtonState getButtonState(final MouseButton button) {
    if (_buttonStates.containsKey(button)) {
      return _buttonStates.get(button);
    }

    return ButtonState.UP;
  }

  public EnumSet<MouseButton> getButtonsReleasedSince(final MouseState previous) {
    final EnumSet<MouseButton> result = EnumSet.noneOf(MouseButton.class);
    for (final MouseButton button : MouseButton.values()) {
      if (previous.getButtonState(button) == ButtonState.DOWN) {
        if (getButtonState(button) != ButtonState.DOWN) {
          result.add(button);
        }
      }
    }

    return result;
  }

  public EnumSet<MouseButton> getButtonsPressedSince(final MouseState previous) {
    final EnumSet<MouseButton> result = EnumSet.noneOf(MouseButton.class);
    for (final MouseButton button : MouseButton.values()) {
      if (getButtonState(button) == ButtonState.DOWN) {
        if (previous.getButtonState(button) != ButtonState.DOWN) {
          result.add(button);
        }
      }
    }

    return result;
  }

  /**
   * Returns all the buttons' states. It could be easier for most classes to use the
   * {@link #getClickCount(MouseButton)} method, and that also results in less object creation.
   *
   * @return a defensive copy of the click counts of all the buttons at this point in time.
   */
  public Multiset<MouseButton> getClickCounts() {
    if (_clickCounts.isEmpty()) {
      return EnumMultiset.create(MouseButton.class);
    } else {
      return EnumMultiset.create(_clickCounts);
    }
  }

  public Multiset<MouseButton> getClickCounts(final EnumMultiset<MouseButton> store) {
    final EnumMultiset<MouseButton> rVal = store;
    if (store == null) {
      if (_clickCounts.isEmpty()) {
        return EnumMultiset.create(MouseButton.class);
      } else {
        return EnumMultiset.create(_clickCounts);
      }
    }
    rVal.clear();
    rVal.addAll(_clickCounts);
    return rVal;
  }

  /**
   * Returns the click count of a mouse button as of this frame. Click counts are non-zero only for
   * frames when the mouse button is released. A double-click sequence, for instance, could show up
   * like this:
   * <nl>
   * <li>Frame 1, mouse button pressed - click count == 0</li>
   * <li>Frame 2, mouse button down - click count == 0</li>
   * <li>Frame 3, mouse button released - click count == 1</li>
   * <li>Frame 4, mouse button up - click count == 0</li>
   * <li>Frame 5, mouse button pressed - click count == 0</li>
   * <li>Frame 6, mouse button down - click count == 0</li>
   * <li>Frame 7, mouse button released - click count == 2</li>
   * </nl>
   *
   * Whether or not a mouse press/release sequence counts as a click (or double-click) depends on the
   * time passed between them as well as any distance the mouse traveled. See {@link #CLICK_TIME_MS}
   * and {@link #CLICK_MAX_DELTA}.
   *
   *
   * @param button
   *          the button to check for clicks
   * @return the click count in this frame
   */
  public int getClickCount(final MouseButton button) {
    return _clickCounts.count(button);
  }

  /**
   * Returns a new EnumSet of all buttons that were clicked this frame.
   *
   * @return every mouse button whose click count this frame is > 0
   */
  public EnumSet<MouseButton> getButtonsClicked() {
    final EnumSet<MouseButton> result = EnumSet.noneOf(MouseButton.class);
    for (final MouseButton button : MouseButton.values()) {
      if (getClickCount(button) != 0) {
        result.add(button);
      }
    }

    return result;
  }

  @Override
  public String toString() {
    return "MouseState{" + "x=" + _x + ", y=" + _y + ", dx=" + _dx + ", dy=" + _dy + ", dwheel=" + _dwheel
        + ", buttonStates=" + _buttonStates.toString() + ", clickCounts=" + _clickCounts.toString() + '}';
  }
}
