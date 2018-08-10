/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

public class StencilStateRecord extends StateRecord {
    public boolean enabled = false;

    // public int[] func = { -1, -1, -1 };
    // public int[] ref = { -1, -1, -1 };
    // public int[] writeMask = { -1, -1, -1 };
    // public int[] funcMask = { -1, -1, -1 };
    // public int[] fail = { -1, -1, -1 };
    // public int[] zfail = { -1, -1, -1 };
    // public int[] zpass = { -1, -1, -1 };

    public boolean useTwoSided = false;

    @Override
    public void invalidate() {
        super.invalidate();

        enabled = false;
        useTwoSided = false;
    }
}
