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

/** ShaderVariableFloat4 */
public class ShaderVariableFloat4 extends ShaderVariable {
    public float value1;
    public float value2;
    public float value3;
    public float value4;

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(value1, "value1", 0.0f);
        capsule.write(value2, "value2", 0.0f);
        capsule.write(value3, "value3", 0.0f);
        capsule.write(value4, "value4", 0.0f);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        value1 = capsule.readFloat("value1", 0.0f);
        value2 = capsule.readFloat("value2", 0.0f);
        value3 = capsule.readFloat("value3", 0.0f);
        value4 = capsule.readFloat("value4", 0.0f);
    }
}
