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

public class ZBufferStateRecord extends StateRecord {
    public boolean depthTest = false;
    public boolean writable = false;
    public int depthFunc = -1;

    @Override
    public void invalidate() {
        super.invalidate();
        depthTest = false;
        writable = false;
        depthFunc = -1;
    }
}
