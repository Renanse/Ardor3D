/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.util;

import org.junit.Test;

public class TestRGB {
    @Test
    public void testLerp() throws Exception {
        final byte x1 = 0, y1 = 100, z1 = (byte) 200, a1 = 0;
        final byte x2 = 100, y2 = (byte) 255, z2 = 50, a2 = 0;

        final int col1 = IntColorUtils.getColor(x1, y1, z1, a1);
        final int col2 = IntColorUtils.getColor(x2, y2, z2, a2);
        System.out.println(IntColorUtils.toString(col1));
        System.out.println(IntColorUtils.toString(col2));

        int col = IntColorUtils.lerp(0.0, col1, col2);
        System.out.println(IntColorUtils.toString(col));
        col = IntColorUtils.lerp(0.5, col1, col2);
        System.out.println(IntColorUtils.toString(col));
        col = IntColorUtils.lerp(0.75, col1, col2);
        System.out.println(IntColorUtils.toString(col));
        col = IntColorUtils.lerp(1.0, col1, col2);
        System.out.println(IntColorUtils.toString(col));

        System.out.println();

        col = IntColorUtils.lerp(-2.0, col1, col2);
        System.out.println(IntColorUtils.toString(col));
        col = IntColorUtils.lerp(2.0, col1, col2);
        System.out.println(IntColorUtils.toString(col));

    }
}
