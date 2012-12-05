/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.shader.uniformtypes;

import java.io.IOException;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.shader.ShaderVariable;

/** ShaderVariableInt4 */
public class ShaderVariableInt4 extends ShaderVariable {
    public int value1;
    public int value2;
    public int value3;
    public int value4;

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(value1, "value1", 0);
        capsule.write(value2, "value2", 0);
        capsule.write(value3, "value3", 0);
        capsule.write(value4, "value4", 0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        value1 = capsule.readInt("value1", 0);
        value2 = capsule.readInt("value2", 0);
        value3 = capsule.readInt("value3", 0);
        value4 = capsule.readInt("value4", 0);
    }
}
