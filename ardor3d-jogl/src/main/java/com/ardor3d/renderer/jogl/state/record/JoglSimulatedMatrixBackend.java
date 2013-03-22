/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl.state.record;

import java.nio.FloatBuffer;

import com.jogamp.opengl.util.PMVMatrix;

public class JoglSimulatedMatrixBackend implements JoglMatrixBackend {

    private final PMVMatrix _matrix;

    public JoglSimulatedMatrixBackend() {
        super();
        _matrix = new PMVMatrix();
    }

    @Override
    public void setMatrixMode(final int matrixMode) {
        _matrix.glMatrixMode(matrixMode);
    }

    @Override
    public void pushMatrix() {
        _matrix.glPushMatrix();
    }

    @Override
    public void popMatrix() {
        _matrix.glPopMatrix();
    }

    @Override
    public void multMatrix(final FloatBuffer fb) {
        _matrix.glMultMatrixf(fb);
    }

    @Override
    public void loadMatrix(final FloatBuffer fb) {
        _matrix.glLoadMatrixf(fb);
    }

    @Override
    public FloatBuffer getMatrix(final int matrixType, final FloatBuffer store) {
        _matrix.glGetFloatv(matrixType, store);
        return store;
    }

    @Override
    public void loadIdentity() {
        _matrix.glLoadIdentity();
    }

    @Override
    public void setOrtho(final double left, final double right, final double bottom, final double top,
            final double near, final double far) {
        _matrix.glOrthof((float) left, (float) right, (float) bottom, (float) top, (float) near, (float) far);
    }

}
