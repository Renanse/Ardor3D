/**
 * Copyright (c) 2008-2010 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl.state.record;

import java.nio.FloatBuffer;

import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLContext;

public class JoglRealMatrixBackend implements JoglMatrixBackend {

    public JoglRealMatrixBackend() {
        super();
        if (!GLContext.getCurrentGL().isGL2ES1()) {
            throw new UnsupportedOperationException("The current GL interface doesn't implement GL2ES1");
        }
    }

    @Override
    public void setMatrixMode(final int matrixMode) {
        final GL2ES1 gl = GLContext.getCurrentGL().getGL2ES1();
        gl.glMatrixMode(matrixMode);
    }

    @Override
    public void pushMatrix() {
        final GL2ES1 gl = GLContext.getCurrentGL().getGL2ES1();
        gl.glPushMatrix();
    }

    @Override
    public void popMatrix() {
        final GL2ES1 gl = GLContext.getCurrentGL().getGL2ES1();
        gl.glPopMatrix();
    }

    @Override
    public void multMatrix(final FloatBuffer fb) {
        final GL2ES1 gl = GLContext.getCurrentGL().getGL2ES1();
        gl.glMultMatrixf(fb);
    }

    @Override
    public void loadMatrix(final FloatBuffer fb) {
        final GL2ES1 gl = GLContext.getCurrentGL().getGL2ES1();
        gl.glLoadMatrixf(fb);
    }

    @Override
    public FloatBuffer getMatrix(final int matrixType, final FloatBuffer store) {
        final GL2ES1 gl = GLContext.getCurrentGL().getGL2ES1();
        gl.glGetFloatv(matrixType, store);
        return store;
    }

    @Override
    public void loadIdentity() {
        final GL2ES1 gl = GLContext.getCurrentGL().getGL2ES1();
        gl.glLoadIdentity();
    }

    @Override
    public void setOrtho(final double left, final double right, final double bottom, final double top,
            final double near, final double far) {
        final GL2ES1 gl = GLContext.getCurrentGL().getGL2ES1();
        gl.glOrtho(left, right, bottom, top, near, far);
    }

}
