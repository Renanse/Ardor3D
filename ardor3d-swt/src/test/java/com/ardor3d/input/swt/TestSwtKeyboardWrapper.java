/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.swt;

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.junit.Before;
import org.junit.Test;

import com.ardor3d.input.Key;
import com.ardor3d.input.KeyEvent;
import com.ardor3d.input.KeyState;

public class TestSwtKeyboardWrapper {
    SwtKeyboardWrapper kw;

    Control control;

    org.eclipse.swt.events.KeyEvent e1, e2;

    @Before
    public void setup() throws Exception {
        control = createMock("text", Text.class);

        kw = new SwtKeyboardWrapper(control);

        final Event event = new Event();
        event.widget = control;

        e1 = new org.eclipse.swt.events.KeyEvent(event);
        e2 = new org.eclipse.swt.events.KeyEvent(event);
    }

    @Test
    public void testKeys1() throws Exception {

        e1.keyCode = 'a';

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
        e1.keyCode = 'a';
        e2.keyCode = 'b';

        kw.keyPressed(e1);
        kw.keyPressed(e2);

        final Iterator<KeyEvent> events = kw.getEvents();

        final KeyEvent event1 = events.next();
        final KeyEvent event2 = events.next();
        final KeyEvent event3 = events.next();

        assertFalse("no more", events.hasNext());
        assertFalse("no more", kw.getEvents().hasNext());

        assertEquals("key a", Key.A, event1.getKey());
        assertEquals("down", KeyState.DOWN, event1.getState());

        assertEquals("key a", Key.A, event2.getKey());
        assertEquals("up", KeyState.UP, event2.getState());

        assertEquals("key b", Key.B, event3.getKey());
        assertEquals("down", KeyState.DOWN, event3.getState());
    }

    @Test
    public void testKeysRepeat() throws Exception {

        e1.keyCode = 'a';

        kw.keyPressed(e1);
        kw.keyPressed(e1);

        final Iterator<KeyEvent> events = kw.getEvents();

        final KeyEvent event1 = events.next();

        assertFalse("no more", events.hasNext());
        assertFalse("no more", kw.getEvents().hasNext());

        assertEquals("key a", Key.A, event1.getKey());
        assertEquals("down", KeyState.DOWN, event1.getState());
    }

}
