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

import java.util.List;

import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.util.shader.ShaderVariable;
import com.google.common.collect.Lists;

public class ShaderStateRecord extends StateRecord {
    ShaderState reference = null;

    public List<ShaderVariable> enabledAttributes = Lists.newArrayList();

    public int programId = -1;

    @Override
    public void invalidate() {
        super.invalidate();

        programId = -1;
        enabledAttributes.clear();
    }
}
