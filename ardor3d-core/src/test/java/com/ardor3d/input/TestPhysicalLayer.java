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

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

public class TestPhysicalLayer {
    KeyboardWrapper keyboardWrapper;
    MouseWrapper mouseWrapper;
    FocusWrapper focusWrapper;
    ControllerWrapper controllerWrapper;
    PhysicalLayer pl;

    Object[] mocks;

    List<KeyEvent> noKeys = new LinkedList<KeyEvent>();
    List<KeyEvent> Adown = new LinkedList<KeyEvent>();
    List<KeyEvent> AdownBdown = new LinkedList<KeyEvent>();
    List<KeyEvent> AdownAup = new LinkedList<KeyEvent>();

    List<ControllerEvent> nothing = new LinkedList<ControllerEvent>();

    List<MouseState> buttonDown = new LinkedList<MouseState>();
    List<MouseState> noMice = new LinkedList<MouseState>();

    List<InputState> inputStates;
    InputState is;

    // @SuppressWarnings( { "unchecked" })
    @Before
    public void setup() throws Exception {
        keyboardWrapper = createMock("KeyboardWrapper", KeyboardWrapper.class);
        mouseWrapper = createMock("MouseWrapper", MouseWrapper.class);
        controllerWrapper = createMock("ControllerWrapper", ControllerWrapper.class);
        focusWrapper = createMock("FocusWrapper", FocusWrapper.class);

        pl = new PhysicalLayer(keyboardWrapper, mouseWrapper, controllerWrapper, focusWrapper);

        mocks = new Object[] { keyboardWrapper, mouseWrapper, controllerWrapper, focusWrapper };

        Adown.add(new KeyEvent(Key.A, KeyState.DOWN, 'a'));

        AdownBdown.add(new KeyEvent(Key.A, KeyState.DOWN, 'a'));
        AdownBdown.add(new KeyEvent(Key.B, KeyState.DOWN, 'b'));

        AdownAup.add(new KeyEvent(Key.A, KeyState.DOWN, 'a'));
        AdownAup.add(new KeyEvent(Key.A, KeyState.UP, 'a'));

        buttonDown.add(new MouseState(0, 0, 0, 0, 0, MouseButton.makeMap(ButtonState.DOWN, ButtonState.UP,
                ButtonState.UP), null));
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
        focusWrapper.init();

        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(Adown.iterator()));
        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(noKeys.iterator()));
        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(noKeys.iterator()));

        expect(mouseWrapper.getEvents()).andReturn(Iterators.peekingIterator(noMice.iterator())).times(3);

        expect(controllerWrapper.getEvents()).andReturn(Iterators.peekingIterator(nothing.iterator())).times(3);

        expect(focusWrapper.getAndClearFocusLost()).andReturn(false).atLeastOnce();

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
        final PeekingIterator<KeyEvent> adau = Iterators.peekingIterator(AdownAup.iterator());

        keyboardWrapper.init();
        mouseWrapper.init();
        controllerWrapper.init();
        focusWrapper.init();

        expect(keyboardWrapper.getEvents()).andReturn(adau);
        expect(keyboardWrapper.getEvents()).andReturn(adau);
        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(noKeys.iterator()));
        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(noKeys.iterator()));

        expect(mouseWrapper.getEvents()).andReturn(Iterators.peekingIterator(noMice.iterator())).times(4);
        expect(controllerWrapper.getEvents()).andReturn(Iterators.peekingIterator(nothing.iterator())).times(4);

        expect(focusWrapper.getAndClearFocusLost()).andReturn(false).times(2);

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
        focusWrapper.init();
        controllerWrapper.init();

        final PeekingIterator<KeyEvent> keyIterator = Iterators.peekingIterator(AdownBdown.iterator());

        expect(keyboardWrapper.getEvents()).andReturn(keyIterator).times(4);
        expect(mouseWrapper.getEvents()).andReturn(Iterators.peekingIterator(noMice.iterator())).times(4);
        expect(controllerWrapper.getEvents()).andReturn(Iterators.peekingIterator(nothing.iterator())).times(4);
        expect(focusWrapper.getAndClearFocusLost()).andReturn(false).times(2);

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
        focusWrapper.init();
        controllerWrapper.init();

        expect(keyboardWrapper.getEvents()).andReturn(iter).atLeastOnce();
        expect(mouseWrapper.getEvents()).andReturn(Iterators.peekingIterator(noMice.iterator())).atLeastOnce();
        expect(controllerWrapper.getEvents()).andReturn(Iterators.peekingIterator(nothing.iterator())).atLeastOnce();
        expect(focusWrapper.getAndClearFocusLost()).andReturn(false).atLeastOnce();

        replay(mocks);

        pl.readState();
    }

    @Test
    public void testTooManyChanges2() throws Exception {
        final PeekingIterator<MouseState> iter = new NeverEndingMouseIterator();

        keyboardWrapper.init();
        mouseWrapper.init();
        focusWrapper.init();
        controllerWrapper.init();

        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(noKeys.iterator())).atLeastOnce();
        expect(mouseWrapper.getEvents()).andReturn(iter).atLeastOnce();
        expect(controllerWrapper.getEvents()).andReturn(Iterators.peekingIterator(nothing.iterator())).atLeastOnce();
        expect(focusWrapper.getAndClearFocusLost()).andReturn(false).atLeastOnce();

        replay(mocks);

        pl.readState();
    }

    @Test
    public void testLostFocus1() throws Exception {
        keyboardWrapper.init();
        mouseWrapper.init();
        focusWrapper.init();
        controllerWrapper.init();

        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(Adown.iterator()));
        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(noKeys.iterator()));
        expect(mouseWrapper.getEvents()).andReturn(Iterators.peekingIterator(noMice.iterator())).times(2);
        expect(controllerWrapper.getEvents()).andReturn(Iterators.peekingIterator(nothing.iterator())).times(2);
        expect(focusWrapper.getAndClearFocusLost()).andReturn(true);

        replay(mocks);

        pl.readState();

        inputStates = pl.drainAvailableStates();

        assertEquals("2 states", 2, inputStates.size());

        is = inputStates.get(0);

        assertTrue("a down", is.getKeyboardState().isDown(Key.A));
        assertFalse("b down", is.getKeyboardState().isDown(Key.B));

        assertTrue("lost focus", InputState.LOST_FOCUS == inputStates.get(1));
    }

    @Test
    public void testLostFocus2() throws Exception {
        keyboardWrapper.init();
        mouseWrapper.init();
        focusWrapper.init();
        controllerWrapper.init();

        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(Adown.iterator()));
        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(noKeys.iterator()));
        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(noKeys.iterator()));
        expect(mouseWrapper.getEvents()).andReturn(Iterators.peekingIterator(noMice.iterator())).times(3);
        expect(controllerWrapper.getEvents()).andReturn(Iterators.peekingIterator(nothing.iterator())).times(3);
        expect(focusWrapper.getAndClearFocusLost()).andReturn(false);
        expect(focusWrapper.getAndClearFocusLost()).andReturn(true);

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
        focusWrapper.init();
        controllerWrapper.init();

        expect(keyboardWrapper.getEvents()).andReturn(Iterators.peekingIterator(noKeys.iterator())).times(3);
        expect(mouseWrapper.getEvents()).andReturn(Iterators.peekingIterator(buttonDown.iterator()));
        expect(mouseWrapper.getEvents()).andReturn(Iterators.peekingIterator(noMice.iterator())).times(2);
        expect(controllerWrapper.getEvents()).andReturn(Iterators.peekingIterator(nothing.iterator())).times(3);
        expect(focusWrapper.getAndClearFocusLost()).andReturn(false);
        expect(focusWrapper.getAndClearFocusLost()).andReturn(true);

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
        final KeyEvent aUp = new KeyEvent(Key.A, KeyState.UP, 'a');
        final KeyEvent aDown = new KeyEvent(Key.A, KeyState.DOWN, 'a');

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

    private static class NeverEndingMouseIterator extends AbstractIterator<MouseState> implements
            PeekingIterator<MouseState> {
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
}