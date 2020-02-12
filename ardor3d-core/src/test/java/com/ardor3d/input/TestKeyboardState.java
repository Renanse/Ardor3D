/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.junit.Test;

import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyEvent;
import com.ardor3d.input.keyboard.KeyboardState;

public class TestKeyboardState {
    KeyboardState ks1, ks2;

    @Test
    public void testKeysReleased1() throws Exception {
        ks1 = new KeyboardState(EnumSet.of(Key.A, Key.B), KeyEvent.NOTHING);
        ks2 = new KeyboardState(EnumSet.of(Key.A, Key.E), KeyEvent.NOTHING);

        final EnumSet<Key> released = ks2.getKeysReleasedSince(ks1);

        assertEquals("1 key", 1, released.size());
        assertTrue("b released", released.contains(Key.B));
    }

    @Test
    public void testKeysReleased2() throws Exception {
        ks1 = new KeyboardState(EnumSet.of(Key.A, Key.B), KeyEvent.NOTHING);
        ks2 = new KeyboardState(EnumSet.noneOf(Key.class), KeyEvent.NOTHING);

        final EnumSet<Key> released = ks2.getKeysReleasedSince(ks1);

        assertEquals("2 key", 2, released.size());
        assertTrue("a released", released.contains(Key.A));
        assertTrue("b released", released.contains(Key.B));
    }

    @Test
    public void testKeysPressed1() throws Exception {
        ks1 = new KeyboardState(EnumSet.of(Key.A, Key.B), KeyEvent.NOTHING);
        ks2 = new KeyboardState(EnumSet.of(Key.A, Key.C, Key.D), KeyEvent.NOTHING);

        final EnumSet<Key> pressed = ks2.getKeysPressedSince(ks1);

        assertEquals("2 key", 2, pressed.size());
        assertTrue("c pressed", pressed.contains(Key.C));
        assertTrue("d pressed", pressed.contains(Key.D));
    }

    @Test
    public void testKeysPressed2() throws Exception {
        ks1 = new KeyboardState(EnumSet.noneOf(Key.class), KeyEvent.NOTHING);
        ks2 = new KeyboardState(EnumSet.of(Key.A, Key.C, Key.D), KeyEvent.NOTHING);

        final EnumSet<Key> pressed = ks2.getKeysPressedSince(ks1);

        assertEquals("2 key", 3, pressed.size());
        assertTrue("a pressed", pressed.contains(Key.A));
        assertTrue("c pressed", pressed.contains(Key.C));
        assertTrue("d pressed", pressed.contains(Key.D));
    }

    @Test
    public void testKeysPressed3() throws Exception {
        ks1 = new KeyboardState(EnumSet.of(Key.A, Key.C, Key.D), KeyEvent.NOTHING);
        ks2 = new KeyboardState(EnumSet.of(Key.A), KeyEvent.NOTHING);

        final EnumSet<Key> pressed = ks2.getKeysPressedSince(ks1);

        assertEquals("0 key", 0, pressed.size());
    }

}
