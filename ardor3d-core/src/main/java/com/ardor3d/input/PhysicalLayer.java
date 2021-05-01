/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.ardor3d.input.character.CharacterInputEvent;
import com.ardor3d.input.character.CharacterInputState;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.ardor3d.input.controller.ControllerEvent;
import com.ardor3d.input.controller.ControllerState;
import com.ardor3d.input.controller.ControllerWrapper;
import com.ardor3d.input.dummy.DummyCharacterInputWrapper;
import com.ardor3d.input.dummy.DummyControllerWrapper;
import com.ardor3d.input.dummy.DummyFocusWrapper;
import com.ardor3d.input.dummy.DummyGestureWrapper;
import com.ardor3d.input.dummy.DummyKeyboardWrapper;
import com.ardor3d.input.dummy.DummyMouseWrapper;
import com.ardor3d.input.focus.FocusWrapper;
import com.ardor3d.input.gesture.GestureState;
import com.ardor3d.input.gesture.GestureWrapper;
import com.ardor3d.input.gesture.event.AbstractGestureEvent;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyEvent;
import com.ardor3d.input.keyboard.KeyState;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.input.mouse.MouseWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.PeekingIterator;

/**
 * Provides access to the physical layer of the input system. This is done via one method that polls
 * the input system, causing it to track which states it has been in {@link #readState()}, and one
 * method that fetches the list of states that are new {@link #drainAvailableStates()}.
 */
public class PhysicalLayer {

  private static final Logger logger = Logger.getLogger(PhysicalLayer.class.getName());

  protected final BlockingQueue<InputState> _stateQueue;
  protected final KeyboardWrapper _keyboardWrapper;
  protected final MouseWrapper _mouseWrapper;
  protected final FocusWrapper _focusWrapper;
  protected final ControllerWrapper _controllerWrapper;
  protected final GestureWrapper _gestureWrapper;
  protected final CharacterInputWrapper _characterWrapper;

  protected KeyboardState _currentKeyboardState = KeyboardState.NOTHING;
  protected MouseState _currentMouseState = MouseState.NOTHING;
  protected ControllerState _currentControllerState = ControllerState.NOTHING;
  protected GestureState _currentGestureState = GestureState.NOTHING;
  protected CharacterInputState _currentCharacterState = CharacterInputState.NOTHING;

  protected boolean _inited = false;

  protected static final long MAX_INPUT_POLL_TIME = TimeUnit.SECONDS.toNanos(2);
  protected static final List<InputState> EMPTY_LIST = ImmutableList.of();

  protected PhysicalLayer(final KeyboardWrapper keyboardWrapper, final MouseWrapper mouseWrapper,
    final ControllerWrapper controllerWrapper, final GestureWrapper gestureWrapper, final FocusWrapper focusWrapper,
    final CharacterInputWrapper characterWrapper) {
    _keyboardWrapper = keyboardWrapper;
    _mouseWrapper = mouseWrapper;
    _focusWrapper = focusWrapper;
    _controllerWrapper = controllerWrapper;
    _gestureWrapper = gestureWrapper;
    _characterWrapper = characterWrapper;

    _stateQueue = new LinkedBlockingQueue<>();
  }

  /**
   * Causes a poll of the input devices to happen, making any updates to input states available via
   * the {@link #drainAvailableStates()} method.
   *
   * @throws IllegalStateException
   *           if too many state changes have happened since the last call to this method
   */
  public void readState() {
    if (!_inited) {
      init();
    }

    KeyboardState oldKeyState = _currentKeyboardState;
    if (_currentMouseState.getDwheel() == 0 && _currentMouseState.getDx() == 0 && _currentMouseState.getDy() == 0) {
      // we can reuse - do nothing
    } else {
      // we can't reuse
      _currentMouseState = new MouseState(_currentMouseState.getX(), _currentMouseState.getY(), 0, 0, 0,
          _currentMouseState.getButtonStates(), _currentMouseState.getClickCounts());
    }
    MouseState oldMouseState = _currentMouseState;
    ControllerState oldControllerState = _currentControllerState;
    GestureState oldGestureState = _currentGestureState;
    CharacterInputState oldCharacterState = _currentCharacterState;

    final long loopExitTime = System.nanoTime() + MAX_INPUT_POLL_TIME;

    while (true) {
      readKeyboardState();
      readMouseState();
      readControllerState();
      readGestureState();
      readCharacterState();

      // if there is no new input, exit the loop. Otherwise, add a new input state to the queue, and
      // see if there is even more input to read.
      if (oldKeyState.equals(_currentKeyboardState) //
          && oldMouseState.equals(_currentMouseState) //
          && oldControllerState.equals(_currentControllerState) //
          && oldGestureState.equals(_currentGestureState) //
          && oldCharacterState.equals(_currentCharacterState)) {
        break;
      }

      _stateQueue.add(new InputState(_currentKeyboardState, _currentMouseState, _currentControllerState,
          _currentGestureState, _currentCharacterState));

      oldKeyState = _currentKeyboardState;
      oldMouseState = _currentMouseState;
      oldControllerState = _currentControllerState;
      oldGestureState = _currentGestureState;
      oldCharacterState = _currentCharacterState;

      if (System.nanoTime() > loopExitTime) {
        logger.severe("Spent too long collecting input data, this is probably an input system bug");
        break;
      }
    }

    if (_focusWrapper.getAndClearFocusLost()) {
      lostFocus();
    }
  }

  protected void readControllerState() {
    final PeekingIterator<ControllerEvent> eventIterator = _controllerWrapper.getControllerEvents();

    if (eventIterator.hasNext()) {
      _currentControllerState = new ControllerState(_currentControllerState);
      while (eventIterator.hasNext()) {
        _currentControllerState.addEvent(eventIterator.next());
      }
    }
  }

  protected void readGestureState() {
    final PeekingIterator<AbstractGestureEvent> eventIterator = _gestureWrapper.getGestureEvents();

    if (eventIterator.hasNext()) {
      _currentGestureState = new GestureState();
      while (eventIterator.hasNext()) {
        _currentGestureState.addEvent(eventIterator.next());
      }
    } else {
      _currentGestureState = GestureState.NOTHING;
    }
  }

  protected void readCharacterState() {
    final PeekingIterator<CharacterInputEvent> eventIterator = _characterWrapper.getCharacterEvents();

    if (eventIterator.hasNext()) {
      _currentCharacterState = new CharacterInputState();
      while (eventIterator.hasNext()) {
        _currentCharacterState.addEvent(eventIterator.next());
      }
    } else {
      _currentCharacterState = CharacterInputState.NOTHING;
    }
  }

  protected void readMouseState() {
    final PeekingIterator<MouseState> eventIterator = _mouseWrapper.getMouseEvents();

    if (eventIterator.hasNext()) {
      _currentMouseState = eventIterator.next();
    }
  }

  protected void readKeyboardState() {
    final PeekingIterator<KeyEvent> eventIterator = _keyboardWrapper.getKeyEvents();

    // if no new events, just leave the current state as is
    if (!eventIterator.hasNext()) {
      return;
    }

    final KeyEvent keyEvent = eventIterator.next();

    // EnumSet.copyOf fails if the collection is empty, since it needs at least one object to
    // figure out which type of enum to deal with. Hence the check below.
    final EnumSet<Key> keysDown = _currentKeyboardState.getKeysDown().isEmpty() ? EnumSet.noneOf(Key.class)
        : EnumSet.copyOf(_currentKeyboardState.getKeysDown());

    if (keyEvent.getState() == KeyState.DOWN) {
      keysDown.add(keyEvent.getKey());
    } else {
      // ignore the fact that this removal might fail - for instance, at startup, the
      // set of keys tracked as down will be empty even if somebody presses a key when the
      // app starts.
      keysDown.remove(keyEvent.getKey());
    }

    _currentKeyboardState = new KeyboardState(keysDown, keyEvent);
  }

  /**
   * Fetches any new <code>InputState</code>s since the last call to this method. If no input system
   * changes have been made since the last call (no mouse movements, no keys pressed or released), an
   * empty list is returned.
   *
   * @return the list of new <code>InputState</code>, or an empty list if there have been no changes
   *         in input
   */
  public List<InputState> drainAvailableStates() {
    // returning a reusable empty list to avoid object creation if there is no new
    // input available. There is a race condition here (input might become available right after
    // the check of isEmpty()) but that's OK, it won't do any harm if that is picked up next frame.
    if (_stateQueue.isEmpty()) {
      return EMPTY_LIST;
    }

    final LinkedList<InputState> result = new LinkedList<>();

    _stateQueue.drainTo(result);

    return result;
  }

  protected void lostFocus() {
    _stateQueue.add(InputState.LOST_FOCUS);
    _currentKeyboardState = KeyboardState.NOTHING;
    _currentMouseState = MouseState.NOTHING;
    _currentControllerState = ControllerState.NOTHING;
    _currentGestureState = GestureState.NOTHING;
    _currentCharacterState = CharacterInputState.NOTHING;
  }

  protected void init() {
    _inited = true;

    _keyboardWrapper.init();
    _mouseWrapper.init();
    _focusWrapper.init();
    _controllerWrapper.init();
    _gestureWrapper.init();
    _characterWrapper.init();
  }

  public ControllerWrapper getControllerWrapper() { return _controllerWrapper; }

  public KeyboardWrapper getKeyboardWrapper() { return _keyboardWrapper; }

  public MouseWrapper getMouseWrapper() { return _mouseWrapper; }

  public GestureWrapper getGestureWrapper() { return _gestureWrapper; }

  public static final class Builder {
    protected KeyboardWrapper _keyboardWrapper = DummyKeyboardWrapper.INSTANCE;
    protected MouseWrapper _mouseWrapper = DummyMouseWrapper.INSTANCE;
    protected FocusWrapper _focusWrapper = DummyFocusWrapper.INSTANCE;
    protected ControllerWrapper _controllerWrapper = DummyControllerWrapper.INSTANCE;
    protected GestureWrapper _gestureWrapper = DummyGestureWrapper.INSTANCE;
    protected CharacterInputWrapper _characterWrapper = DummyCharacterInputWrapper.INSTANCE;

    public Builder with(final KeyboardWrapper wrapper) {
      _keyboardWrapper = wrapper;
      return this;
    }

    public Builder with(final MouseWrapper wrapper) {
      _mouseWrapper = wrapper;
      return this;
    }

    public Builder with(final FocusWrapper wrapper) {
      _focusWrapper = wrapper;
      return this;
    }

    public Builder with(final ControllerWrapper wrapper) {
      _controllerWrapper = wrapper;
      return this;
    }

    public Builder with(final GestureWrapper wrapper) {
      _gestureWrapper = wrapper;
      return this;
    }

    public Builder with(final CharacterInputWrapper wrapper) {
      _characterWrapper = wrapper;
      return this;
    }

    public PhysicalLayer build() {
      return new PhysicalLayer(_keyboardWrapper, _mouseWrapper, _controllerWrapper, _gestureWrapper, _focusWrapper,
          _characterWrapper);
    }
  }
}
