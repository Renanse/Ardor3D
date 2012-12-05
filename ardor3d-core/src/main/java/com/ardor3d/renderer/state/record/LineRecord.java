/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state.record;

public class LineRecord extends StateRecord {
    public boolean smoothed = false;
    public boolean stippled = false;
    public int smoothHint = -1;
    public float width = -1;
    public int stippleFactor = -1;
    public short stipplePattern = -1;

    @Override
    public void invalidate() {
        super.invalidate();

        smoothed = false;
        stippled = false;
        smoothHint = -1;
        width = -1;
        stippleFactor = -1;
        stipplePattern = -1;
    }
}
