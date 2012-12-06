/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.awt;

import static org.easymock.classextension.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.awt.Component;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.ardor3d.input.Key;
import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyState;

public class TestAwtKeyboardWrapper {
    AwtKeyboardWrapper kw;

    Component control;

    java.awt.event.KeyEvent e1, e2;

    @Before
    public void setup() throws Exception {
        control = createMock("component", Component.class);

        kw = new AwtKeyboardWrapper(control);
    }

    @Test
    public void testKeys1() throws Exception {

        e1 = new java.awt.event.KeyEvent(control, 0, 0, 0, AwtKey.A.getAwtCode(), 'a');

        kw.keyPressed(e1);
        kw.keyReleased(e1);

        final Iterator<KeyEvent> events = kw.getEvents();

        final KeyEvent event1 = events.next();
        final KeyEvent event2 = events.next();

        assertFalse("no more", events.hasNext());
        assertFalse("no more", kw.getEvents().hasNext());

        assertEquals("key a", Key.A, event1.getKey());
        assertEquals("down", KeyState.DOWN, event1.getState());

        assertEquals("key a", Key.A, event2.getKey());
        assertEquals("up", KeyState.UP, event2.getState());
    }

    @Test
    public void testKeys2() throws Exception {
        e1 = new java.awt.event.KeyEvent(control, 0, 0, 0, AwtKey.A.getAwtCode(), 'a');
        e2 = new java.awt.event.KeyEvent(control, 0, 0, 0, AwtKey.B.getAwtCode(), 'b');

        kw.keyPressed(e1);
        kw.keyPressed(e2);

        final Iterator<KeyEvent> events = kw.getEvents();

        final KeyEvent event1 = events.next();
        final KeyEvent event2 = events.next();

        assertFalse("no more", events.hasNext());
        assertFalse("no more", kw.getEvents().hasNext());

        assertEquals("key a", Key.A, event1.getKey());
        assertEquals("down", KeyState.DOWN, event1.getState());

        assertEquals("key b", Key.B, event2.getKey());
        assertEquals("down", KeyState.DOWN, event2.getState());
    }

    // @Test
    // public void testLostFocusKeyDown() throws Exception {
    //
    // e1 = new java.awt.event.KeyEvent(control, 0, 0, 0, AwtKey.AWT_KEY_A.getAwtCode(), 'a');
    //
    // kw.keyPressed(e1);
    //
    //
    // Iterator<KeyEvent> events = kw.getEvents();
    //
    // KeyEvent event1 = events.next();
    //
    // assertFalse("no more", events.hasNext());
    // assertFalse("no more", kw.getEvents().hasNext());
    //
    // assertEquals("key a", Key.A, event1.getKey());
    // assertEquals("down", KeyState.DOWN, event1.getState());
    // }
    //
    //
}