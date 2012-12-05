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

import com.ardor3d.renderer.state.CullState.PolygonWind;

public class CullStateRecord extends StateRecord {
    public boolean enabled = false;
    public int face = -1;
    public PolygonWind windOrder = null;

    @Override
    public void invalidate() {
        super.invalidate();

        enabled = false;
        face = -1;
        windOrder = null;
    }
}
