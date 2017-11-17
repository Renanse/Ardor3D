/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.ardor3d.input.gestures.AbstractGestureEvent;
import com.ardor3d.input.gestures.GestureState;
import com.ardor3d.input.gestures.GestureWrapper;
import com.ardor3d.input.logical.DummyControllerWrapper;
import com.ardor3d.input.logical.DummyFocusWrapper;
import com.ardor3d.input.logical.DummyGestureWrapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.PeekingIterator;

/**
 * Provides access to the physical layer of the input system. This is done via one method that polls the input system,
 * causing it to track which states it has been in {@link #readState()}, and one method that fetches the list of states
 * that are new {@link #drainAvailableStates()}.
 */
public class PhysicalLayer {

    private static final Logger logger = Logger.getLogger(PhysicalLayer.class.getName());

    private final BlockingQueue<InputState> _stateQueue;
    private final KeyboardWrapper _keyboardWrapper;
    private final MouseWrapper _mouseWrapper;
    private final FocusWrapper _focusWrapper;
    private final ControllerWrapper _controllerWrapper;
    private final GestureWrapper _gestureWrapper;

    private KeyboardState _currentKeyboardState;
    private MouseState _currentMouseState;
    private ControllerState _currentControllerState;
    private GestureState _currentGestureState;

    private boolean _inited = false;

    private static final long MAX_INPUT_POLL_TIME = TimeUnit.SECONDS.toNanos(2);
    private static final List<InputState> EMPTY_LIST = ImmutableList.of();

    public PhysicalLayer(final KeyboardWrapper keyboardWrapper, final MouseWrapper mouseWrapper) {
        this(keyboardWrapper, mouseWrapper, DummyControllerWrapper.INSTANCE, DummyGestureWrapper.INSTANCE,
                DummyFocusWrapper.INSTANCE);
    }

    public PhysicalLayer(final KeyboardWrapper keyboardWrapper, final MouseWrapper mouseWrapper,
            final FocusWrapper focusWrapper) {
        this(keyboardWrapper, mouseWrapper, DummyControllerWrapper.INSTANCE, DummyGestureWrapper.INSTANCE, focusWrapper);
    }

    public PhysicalLayer(final KeyboardWrapper keyboardWrapper, final MouseWrapper mouseWrapper,
            final ControllerWrapper controllerWrapper) {
        this(keyboardWrapper, mouseWrapper, controllerWrapper, DummyGestureWrapper.INSTANCE, DummyFocusWrapper.INSTANCE);
    }

    public PhysicalLayer(final KeyboardWrapper keyboardWrapper, final MouseWrapper mouseWrapper,
            final ControllerWrapper controllerWrapper, final FocusWrapper focusWrapper) {
        this(keyboardWrapper, mouseWrapper, controllerWrapper, DummyGestureWrapper.INSTANCE, focusWrapper);
    }

    public PhysicalLayer(final KeyboardWrapper keyboardWrapper, final MouseWrapper mouseWrapper,
            final GestureWrapper gestureWrapper) {
        this(keyboardWrapper, mouseWrapper, DummyControllerWrapper.INSTANCE, gestureWrapper, DummyFocusWrapper.INSTANCE);
    }

    public PhysicalLayer(final KeyboardWrapper keyboardWrapper, final MouseWrapper mouseWrapper,
            final ControllerWrapper controllerWrapper, final GestureWrapper gestureWrapper,
            final FocusWrapper focusWrapper) {
        _keyboardWrapper = keyboardWrapper;
        _mouseWrapper = mouseWrapper;
        _focusWrapper = focusWrapper;
        _controllerWrapper = controllerWrapper;
        _gestureWrapper = gestureWrapper;
        _stateQueue = new LinkedBlockingQueue<InputState>();

        _currentKeyboardState = KeyboardState.NOTHING;
        _currentMouseState = MouseState.NOTHING;
        _currentControllerState = ControllerState.NOTHING;
        _currentGestureState = GestureState.NOTHING;
    }

    /**
     * Causes a poll of the input devices to happen, making any updates to input states available via the
     * {@link #drainAvailableStates()} method.
     *
     * @throws IllegalStateException
     *             if too many state changes have happened since the last call to this method
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

        final long loopExitTime = System.nanoTime() + MAX_INPUT_POLL_TIME;

        while (true) {
            readKeyboardState();
            readMouseState();
            readControllerState();
            readGestureState();

            // if there is no new input, exit the loop. Otherwise, add a new input state to the queue, and
            // see if there is even more input to read.
            if (oldKeyState.equals(_currentKeyboardState) && oldMouseState.equals(_currentMouseState)
                    && oldControllerState.equals(_currentControllerState)
                    && oldGestureState.equals(_currentGestureState)) {
                break;
            }

            _stateQueue.add(new InputState(_currentKeyboardState, _currentMouseState, _currentControllerState,
                    _currentGestureState));

            oldKeyState = _currentKeyboardState;
            oldMouseState = _currentMouseState;
            oldControllerState = _currentControllerState;
            oldGestureState = _currentGestureState;

            if (System.nanoTime() > loopExitTime) {
                logger.severe("Spent too long collecting input data, this is probably an input system bug");
                break;
            }
        }

        if (_focusWrapper.getAndClearFocusLost()) {
            lostFocus();
        }
    }

    private void readControllerState() {
        final PeekingIterator<ControllerEvent> eventIterator = _controllerWrapper.getEvents();

        if (eventIterator.hasNext()) {
            _currentControllerState = new ControllerState(_currentControllerState);
            while (eventIterator.hasNext()) {
                _currentControllerState.addEvent(eventIterator.next());
            }
        }
    }

    private void readGestureState() {
        final PeekingIterator<AbstractGestureEvent> eventIterator = _gestureWrapper.getEvents();

        if (eventIterator.hasNext()) {
            _currentGestureState = new GestureState();
            while (eventIterator.hasNext()) {
                _currentGestureState.addEvent(eventIterator.next());
            }
        }
    }

    private void readMouseState() {
        final PeekingIterator<MouseState> eventIterator = _mouseWrapper.getEvents();

        if (eventIterator.hasNext()) {
            _currentMouseState = eventIterator.next();
        }
    }

    private void readKeyboardState() {
        final PeekingIterator<KeyEvent> eventIterator = _keyboardWrapper.getEvents();

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
     * Fetches any new <code>InputState</code>s since the last call to this method. If no input system changes have been
     * made since the last call (no mouse movements, no keys pressed or released), an empty list is returned.
     *
     * @return the list of new <code>InputState</code>, or an empty list if there have been no changes in input
     */
    public List<InputState> drainAvailableStates() {
        // returning a reusable empty list to avoid object creation if there is no new
        // input available. There is a race condition here (input might become available right after
        // the check of isEmpty()) but that's OK, it won't do any harm if that is picked up next frame.
        if (_stateQueue.isEmpty()) {
            return EMPTY_LIST;
        }

        final LinkedList<InputState> result = new LinkedList<InputState>();

        _stateQueue.drainTo(result);

        return result;
    }

    private void lostFocus() {
        _stateQueue.add(InputState.LOST_FOCUS);
        _currentKeyboardState = KeyboardState.NOTHING;
        _currentMouseState = MouseState.NOTHING;
        _currentControllerState = ControllerState.NOTHING;
        _currentGestureState = GestureState.NOTHING;
    }

    private void init() {
        _inited = true;

        _keyboardWrapper.init();
        _mouseWrapper.init();
        _focusWrapper.init();
        _controllerWrapper.init();
        _gestureWrapper.init();
    }

    public ControllerWrapper getControllerWrapper() {
        return _controllerWrapper;
    }

    public KeyboardWrapper getKeyboardWrapper() {
        return _keyboardWrapper;
    }

    public MouseWrapper getMouseWrapper() {
        return _mouseWrapper;
    }

    public GestureWrapper getGestureWrapper() {
        return _gestureWrapper;
    }
}