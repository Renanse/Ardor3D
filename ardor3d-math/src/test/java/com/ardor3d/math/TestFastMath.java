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

public class TestFastMath {

    @Test
    public void testSin() {
        final double angle = MathUtils.DEG_TO_RAD * 45;
        assertTrue(Math.abs(FastMath.sin(angle) - Math.sin(angle)) <= FastMath.EPSILON_SIN);
    }

    @Test
    public void testCos() {
        double angle = MathUtils.DEG_TO_RAD * 45;
        assertTrue(Math.abs(FastMath.cos(angle) - Math.cos(angle)) <= FastMath.EPSILON_COS);
        angle = MathUtils.DEG_TO_RAD * 135;
        assertTrue(Math.abs(FastMath.cos(angle) - Math.cos(angle)) <= FastMath.EPSILON_COS);
    }

    @Test
    public void testTan() {
        final double angle = MathUtils.DEG_TO_RAD * 45;
        assertTrue(Math.abs(FastMath.tan(angle) - Math.tan(angle)) <= FastMath.EPSILON_SIN2COS2);
    }

    @Test
    public void testAsin() {
        final double val = 0.5;
        assertTrue(Math.abs(FastMath.asin(val) - Math.asin(val)) <= FastMath.EPSILON_ASIN);
    }

    @Test
    public void testAcos() {
        final double val = 0.5;
        assertTrue(Math.abs(FastMath.acos(val) - Math.acos(val)) <= FastMath.EPSILON_ACOS);
    }

    @Test
    public void testAtan() {
        double val = 1;
        assertTrue(Math.abs(FastMath.atan(val) - Math.atan(val)) <= FastMath.EPSILON_ATAN);
        val = 0.5;
        assertTrue(Math.abs(FastMath.atan(val) - Math.atan(val)) <= FastMath.EPSILON_ATAN);
    }

    @Test
    public void testInverseSqrt() {
        final double val = 173;
        assertTrue(Math.abs(FastMath.inverseSqrt(val) - 1.0 / Math.sqrt(val)) <= FastMath.EPSILON_SQRT);
    }

    @Test
    public void testSqrt() {
        final double val = 173;
        assertTrue(Math.abs(FastMath.sqrt(val) - Math.sqrt(val)) <= FastMath.EPSILON_SQRT);
    }

}
