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

public interface JoglMatrixBackend {

    public void setMatrixMode(int matrixMode);

    public void pushMatrix();

    public void popMatrix();

    public void multMatrix(FloatBuffer fb);

    public void loadMatrix(FloatBuffer fb);

    public FloatBuffer getMatrix(final int matrixType, final FloatBuffer store);

    public void loadIdentity();

    public void setOrtho(double left, double right, double bottom, double top, double near, double far);
}
