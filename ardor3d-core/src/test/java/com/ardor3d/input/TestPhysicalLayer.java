/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ardor3d.util.AbstractIterator;
import com.ardor3d.util.PeekingIterator;
import com.ardor3d.input.character.CharacterInputEvent;
import com.ardor3d.input.character.CharacterInputWrapper;
import com.ardor3d.input.controller.ControllerEvent;
import com.ardor3d.input.controller.ControllerWrapper;
import com.ardor3d.input.focus.FocusWrapper;
import com.ardor3d.input.gesture.GestureWrapper;
import com.ardor3d.input.gesture.event.AbstractGestureEvent;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyEvent;
import com.ardor3d.input.keyboard.KeyState;
import com.ardor3d.input.keyboard.KeyboardWrapper;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.input.mouse.MouseWrapper;

public class TestPhysicalLayer {
  KeyboardWrapper keyboardWrapper;
  MouseWrapper mouseWrapper;
  ControllerWrapper controllerWrapper;
  GestureWrapper gestureWrapper;
  FocusWrapper focusWrapper;
  CharacterInputWrapper characterWrapper;
  PhysicalLayer pl;

  Object[] mocks;

  List<KeyEvent> noKeys = new LinkedList<>();
  List<KeyEvent> Adown = new LinkedList<>();
  List<KeyEvent> AdownBdown = new LinkedList<>();
  List<KeyEvent> AdownAup = new LinkedList<>();

  List<ControllerEvent> nothing = new LinkedList<>();

  List<AbstractGestureEvent> noGestures = new LinkedList<>();

  List<CharacterInputEvent> noCharacters = new LinkedList<>();

  List<MouseState> buttonDown = new LinkedList<>();
  List<MouseState> noMice = new LinkedList<>();

  List<InputState> inputStates;
  InputState is;

  // @SuppressWarnings( { "unchecked" })
  @Before
  public void setup() throws Exception {
    keyboardWrapper = createMock("KeyboardWrapper", KeyboardWrapper.class);
    mouseWrapper = createMock("MouseWrapper", MouseWrapper.class);
    controllerWrapper = createMock("ControllerWrapper", ControllerWrapper.class);
    gestureWrapper = createMock("GestureWrapper", GestureWrapper.class);
    focusWrapper = createMock("FocusWrapper", FocusWrapper.class);
    characterWrapper = createMock("CharacterInputWrapper", CharacterInputWrapper.class);

    pl = new PhysicalLayer.Builder().with(keyboardWrapper).with(mouseWrapper).with(controllerWrapper)
        .with(gestureWrapper).with(focusWrapper).with(characterWrapper).build();

    mocks =
        new Object[] {keyboardWrapper, mouseWrapper, controllerWrapper, gestureWrapper, focusWrapper, characterWrapper};

    Adown.add(new KeyEvent(Key.A, KeyState.DOWN));

    AdownBdown.add(new KeyEvent(Key.A, KeyState.DOWN));
    AdownBdown.add(new KeyEvent(Key.B, KeyState.DOWN));

    AdownAup.add(new KeyEvent(Key.A, KeyState.DOWN));
    AdownAup.add(new KeyEvent(Key.A, KeyState.UP));

    buttonDown.add(
        new MouseState(0, 0, 0, 0, 0, MouseButton.makeMap(ButtonState.DOWN, ButtonState.UP, ButtonState.UP), null));
  }

  @After
  public void verifyMocks() throws Exception {
    verify(mocks);
  }

  @Test
  public void testKeyboardBasic1() throws Exception {
    keyboardWrapper.init();
    mouseWrapper.init();
    controllerWrapper.init();
    gestureWrapper.init();
    focusWrapper.init();
    characterWrapper.init();

    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(Adown.iterator()));
    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(noKeys.iterator()));
    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(noKeys.iterator()));

    expect(mouseWrapper.getMouseEvents()).andReturn(peekingIterator(noMice.iterator())).times(3);

    expect(controllerWrapper.getControllerEvents()).andReturn(peekingIterator(nothing.iterator())).times(3);

    expect(gestureWrapper.getGestureEvents()).andReturn(peekingIterator(noGestures.iterator())).times(3);

    expect(focusWrapper.getAndClearFocusLost()).andReturn(false).atLeastOnce();

    expect(characterWrapper.getCharacterEvents()).andReturn(peekingIterator(noCharacters.iterator()))
        .times(3);

    replay(mocks);

    pl.readState();
    inputStates = pl.drainAvailableStates();

    assertEquals("1 state", 1, inputStates.size());

    is = inputStates.get(0);

    assertTrue("a down", is.getKeyboardState().isDown(Key.A));
    assertFalse("b down", is.getKeyboardState().isDown(Key.B));

    pl.readState();
    inputStates = pl.drainAvailableStates();

    assertEquals("0 states", 0, inputStates.size());
  }

  @Test
  public void testKeyboardBasic2() throws Exception {
    final PeekingIterator<KeyEvent> adau = peekingIterator(AdownAup.iterator());

    keyboardWrapper.init();
    mouseWrapper.init();
    controllerWrapper.init();
    gestureWrapper.init();
    focusWrapper.init();
    characterWrapper.init();

    expect(keyboardWrapper.getKeyEvents()).andReturn(adau);
    expect(keyboardWrapper.getKeyEvents()).andReturn(adau);
    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(noKeys.iterator()));
    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(noKeys.iterator()));

    expect(mouseWrapper.getMouseEvents()).andReturn(peekingIterator(noMice.iterator())).times(4);
    expect(controllerWrapper.getControllerEvents()).andReturn(peekingIterator(nothing.iterator())).times(4);
    expect(gestureWrapper.getGestureEvents()).andReturn(peekingIterator(noGestures.iterator())).times(4);

    expect(focusWrapper.getAndClearFocusLost()).andReturn(false).times(2);

    expect(characterWrapper.getCharacterEvents()).andReturn(peekingIterator(noCharacters.iterator()))
        .times(4);

    replay(mocks);

    pl.readState();
    inputStates = pl.drainAvailableStates();

    assertEquals("2 states", 2, inputStates.size());

    is = inputStates.get(0);

    assertTrue("a down", is.getKeyboardState().isDown(Key.A));
    assertFalse("b down", is.getKeyboardState().isDown(Key.B));

    is = inputStates.get(1);

    assertFalse("a down", is.getKeyboardState().isDown(Key.A));
    assertFalse("b down", is.getKeyboardState().isDown(Key.B));

    pl.readState();
    inputStates = pl.drainAvailableStates();

    assertEquals("0 states", 0, inputStates.size());
  }

  @Test
  public void testKeyboardBasic3() throws Exception {
    keyboardWrapper.init();
    mouseWrapper.init();
    controllerWrapper.init();
    gestureWrapper.init();
    focusWrapper.init();
    characterWrapper.init();

    final PeekingIterator<KeyEvent> keyIterator = peekingIterator(AdownBdown.iterator());

    expect(keyboardWrapper.getKeyEvents()).andReturn(keyIterator).times(4);
    expect(mouseWrapper.getMouseEvents()).andReturn(peekingIterator(noMice.iterator())).times(4);
    expect(controllerWrapper.getControllerEvents()).andReturn(peekingIterator(nothing.iterator())).times(4);
    expect(gestureWrapper.getGestureEvents()).andReturn(peekingIterator(noGestures.iterator())).times(4);
    expect(focusWrapper.getAndClearFocusLost()).andReturn(false).times(2);

    expect(characterWrapper.getCharacterEvents()).andReturn(peekingIterator(noCharacters.iterator()))
        .times(4);

    replay(mocks);

    pl.readState();
    inputStates = pl.drainAvailableStates();

    assertEquals("2 states", 2, inputStates.size());

    is = inputStates.get(0);

    assertTrue("a down", is.getKeyboardState().isDown(Key.A));
    assertFalse("b down", is.getKeyboardState().isDown(Key.B));

    is = inputStates.get(1);

    assertTrue("a down", is.getKeyboardState().isDown(Key.A));
    assertTrue("b down", is.getKeyboardState().isDown(Key.B));

    pl.readState();
    inputStates = pl.drainAvailableStates();

    assertEquals("0 states", 0, inputStates.size());
  }

  @Test
  public void testTooManyChanges1() throws Exception {
    final PeekingIterator<KeyEvent> iter = new NeverEndingKeyIterator();

    keyboardWrapper.init();
    mouseWrapper.init();
    controllerWrapper.init();
    gestureWrapper.init();
    focusWrapper.init();
    characterWrapper.init();

    expect(keyboardWrapper.getKeyEvents()).andReturn(iter).atLeastOnce();
    expect(mouseWrapper.getMouseEvents()).andReturn(peekingIterator(noMice.iterator())).atLeastOnce();
    expect(controllerWrapper.getControllerEvents()).andReturn(peekingIterator(nothing.iterator()))
        .atLeastOnce();
    expect(gestureWrapper.getGestureEvents()).andReturn(peekingIterator(noGestures.iterator())).atLeastOnce();
    expect(focusWrapper.getAndClearFocusLost()).andReturn(false).atLeastOnce();

    expect(characterWrapper.getCharacterEvents()).andReturn(peekingIterator(noCharacters.iterator()))
        .atLeastOnce();

    replay(mocks);

    pl.readState();
  }

  @Test
  public void testTooManyChanges2() throws Exception {
    final PeekingIterator<MouseState> iter = new NeverEndingMouseIterator();

    keyboardWrapper.init();
    mouseWrapper.init();
    controllerWrapper.init();
    gestureWrapper.init();
    focusWrapper.init();
    characterWrapper.init();

    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(noKeys.iterator())).atLeastOnce();
    expect(mouseWrapper.getMouseEvents()).andReturn(iter).atLeastOnce();
    expect(controllerWrapper.getControllerEvents()).andReturn(peekingIterator(nothing.iterator()))
        .atLeastOnce();
    expect(gestureWrapper.getGestureEvents()).andReturn(peekingIterator(noGestures.iterator())).atLeastOnce();
    expect(focusWrapper.getAndClearFocusLost()).andReturn(false).atLeastOnce();

    expect(characterWrapper.getCharacterEvents()).andReturn(peekingIterator(noCharacters.iterator()))
        .atLeastOnce();

    replay(mocks);

    pl.readState();
  }

  @Test
  public void testLostFocus1() throws Exception {
    keyboardWrapper.init();
    mouseWrapper.init();
    controllerWrapper.init();
    gestureWrapper.init();
    focusWrapper.init();
    characterWrapper.init();

    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(Adown.iterator()));
    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(noKeys.iterator()));
    expect(mouseWrapper.getMouseEvents()).andReturn(peekingIterator(noMice.iterator())).times(2);
    expect(controllerWrapper.getControllerEvents()).andReturn(peekingIterator(nothing.iterator())).times(2);
    expect(gestureWrapper.getGestureEvents()).andReturn(peekingIterator(noGestures.iterator())).times(2);
    expect(focusWrapper.getAndClearFocusLost()).andReturn(true);

    expect(characterWrapper.getCharacterEvents()).andReturn(peekingIterator(noCharacters.iterator()))
        .times(2);

    replay(mocks);

    pl.readState();

    inputStates = pl.drainAvailableStates();

    assertEquals("2 states", 2, inputStates.size());

    is = inputStates.get(0);

    assertTrue("a down", is.getKeyboardState().isDown(Key.A));
    assertFalse("b down", is.getKeyboardState().isDown(Key.B));

    assertSame("lost focus", InputState.LOST_FOCUS, inputStates.get(1));
  }

  @Test
  public void testLostFocus2() throws Exception {
    keyboardWrapper.init();
    mouseWrapper.init();
    controllerWrapper.init();
    gestureWrapper.init();
    focusWrapper.init();
    characterWrapper.init();

    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(Adown.iterator()));
    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(noKeys.iterator()));
    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(noKeys.iterator()));
    expect(mouseWrapper.getMouseEvents()).andReturn(peekingIterator(noMice.iterator())).times(3);
    expect(controllerWrapper.getControllerEvents()).andReturn(peekingIterator(nothing.iterator())).times(3);
    expect(gestureWrapper.getGestureEvents()).andReturn(peekingIterator(noGestures.iterator())).times(3);
    expect(focusWrapper.getAndClearFocusLost()).andReturn(false);
    expect(focusWrapper.getAndClearFocusLost()).andReturn(true);
    expect(characterWrapper.getCharacterEvents()).andReturn(peekingIterator(noCharacters.iterator()))
        .times(3);

    replay(mocks);

    pl.readState();
    pl.readState();

    inputStates = pl.drainAvailableStates();

    assertEquals("2 states", 2, inputStates.size());

    is = inputStates.get(0);

    assertTrue("a down", is.getKeyboardState().isDown(Key.A));
    assertFalse("b down", is.getKeyboardState().isDown(Key.B));

    is = inputStates.get(1);

    assertEquals("lost focus", InputState.LOST_FOCUS, is);
    assertFalse("a down", is.getKeyboardState().isDown(Key.A));
    assertFalse("b down", is.getKeyboardState().isDown(Key.B));
  }

  @Test
  public void testLostFocus3() throws Exception {
    keyboardWrapper.init();
    mouseWrapper.init();
    controllerWrapper.init();
    gestureWrapper.init();
    focusWrapper.init();
    characterWrapper.init();

    expect(keyboardWrapper.getKeyEvents()).andReturn(peekingIterator(noKeys.iterator())).times(3);
    expect(mouseWrapper.getMouseEvents()).andReturn(peekingIterator(buttonDown.iterator()));
    expect(mouseWrapper.getMouseEvents()).andReturn(peekingIterator(noMice.iterator())).times(2);
    expect(controllerWrapper.getControllerEvents()).andReturn(peekingIterator(nothing.iterator())).times(3);
    expect(gestureWrapper.getGestureEvents()).andReturn(peekingIterator(noGestures.iterator())).times(3);
    expect(focusWrapper.getAndClearFocusLost()).andReturn(false);
    expect(focusWrapper.getAndClearFocusLost()).andReturn(true);
    expect(characterWrapper.getCharacterEvents()).andReturn(peekingIterator(noCharacters.iterator()))
        .times(3);

    replay(mocks);

    pl.readState();
    pl.readState();

    inputStates = pl.drainAvailableStates();

    assertEquals("2 states", 2, inputStates.size());

    is = inputStates.get(0);

    assertFalse("a down", is.getKeyboardState().isDown(Key.A));
    assertFalse("b down", is.getKeyboardState().isDown(Key.B));
    assertEquals("mb down", ButtonState.DOWN, is.getMouseState().getButtonState(MouseButton.LEFT));

    is = inputStates.get(1);

    assertEquals("lost focus", InputState.LOST_FOCUS, is);
    assertFalse("a down", is.getKeyboardState().isDown(Key.A));
    assertFalse("b down", is.getKeyboardState().isDown(Key.B));
    assertEquals("mb up", ButtonState.UP, is.getMouseState().getButtonState(MouseButton.LEFT));
  }

  private static class NeverEndingKeyIterator extends AbstractIterator<KeyEvent> implements PeekingIterator<KeyEvent> {
    final KeyEvent aUp = new KeyEvent(Key.A, KeyState.UP);
    final KeyEvent aDown = new KeyEvent(Key.A, KeyState.DOWN);

    int count = 0;

    @Override
    protected KeyEvent computeNext() {
      count++;

      if (count % 2 == 0) {
        return aUp;
      }

      return aDown;
    }
  }

  private static class NeverEndingMouseIterator extends AbstractIterator<MouseState>
      implements PeekingIterator<MouseState> {
    final MouseState m1 = new MouseState(0, 0, 0, 0, 0, null, null);
    final MouseState m2 = new MouseState(0, 1, 2, 0, 0, null, null);

    int count = 0;

    @Override
    protected MouseState computeNext() {
      count++;

      if (count % 2 == 0) {
        return m1;
      }

      return m2;
    }
  }

  private static class PeekingImpl<E> implements PeekingIterator<E> {

    private final Iterator<? extends E> iterator;
    private boolean hasPeeked;
    private E peekedElement;

    public PeekingImpl(Iterator<? extends E> iterator) {
      if (iterator == null) throw new NullPointerException();
      this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
      return hasPeeked || iterator.hasNext();
    }

    public E next() {
      if (!hasPeeked) {
        return iterator.next();
      }
      // The cast is safe because of the hasPeeked check.
      E result = peekedElement;
      hasPeeked = false;
      peekedElement = null;
      return result;
    }

    @Override
    public void remove() {
      if (hasPeeked) throw new IllegalStateException("Can't remove after you've peeked at next");
      iterator.remove();
    }

    public E peek() {
      if (!hasPeeked) {
        peekedElement = iterator.next();
        hasPeeked = true;
      }
      return peekedElement;
    }
  }

  public static <T> PeekingIterator<T> peekingIterator(Iterator<? extends T> iterator) {
    if (iterator instanceof PeekingImpl) {
      //noinspection unchecked
      return (PeekingImpl<T>) iterator;
    }
    return new PeekingImpl<>(iterator);
  }
}
