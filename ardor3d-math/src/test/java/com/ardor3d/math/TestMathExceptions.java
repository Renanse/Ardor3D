/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestMathExceptions {
    @Test
    public void testInvalidTransformException() {
        final InvalidTransformException ex1 = new InvalidTransformException();
        final InvalidTransformException ex2 = new InvalidTransformException("ABC");
        final Exception a = new Exception();
        final InvalidTransformException ex3 = new InvalidTransformException(a);
        final InvalidTransformException ex4 = new InvalidTransformException("DEF", a);

        assertNotNull(ex1);
        assertEquals("ABC", ex2.getMessage());
        assertEquals(a, ex3.getCause());
        assertEquals("DEF", ex4.getMessage());
        assertEquals(a, ex4.getCause());
    }

    @Test
    public void testTransformException() {
        final TransformException ex1 = new TransformException();
        final TransformException ex2 = new TransformException("ABC");

        assertNotNull(ex1);
        assertEquals("ABC", ex2.getMessage());
    }
}
