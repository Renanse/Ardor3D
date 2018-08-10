/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.shader.uniformtypes;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.shader.ShaderVariable;

/** ShaderVariableMatrix4 */
public class ShaderVariableMatrix4 extends ShaderVariable {
    public FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    public boolean rowMajor;

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(matrixBuffer, "matrixBuffer", null);
        capsule.write(rowMajor, "rowMajor", false);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        matrixBuffer = capsule.readFloatBuffer("matrixBuffer", null);
        rowMajor = capsule.readBoolean("rowMajor", false);
    }
}
