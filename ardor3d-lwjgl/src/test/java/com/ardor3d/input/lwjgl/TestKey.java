/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.lwjgl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ardor3d.input.Key;

public class TestKey {
    @Test
    public void testFindByCode1() throws Exception {
        final Key a = LwjglKey.findByCode(LwjglKey.A.getLwjglCode());

        assertEquals("a found", Key.A, a);
    }

    @Test
    public void testFindByCode2() throws Exception {
        final Key unknown = LwjglKey.findByCode(-14);

        assertEquals("unknown found", Key.UNKNOWN, unknown);
    }
}
