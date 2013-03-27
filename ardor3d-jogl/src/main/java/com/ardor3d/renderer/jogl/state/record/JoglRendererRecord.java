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

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;

import com.ardor3d.renderer.state.record.RendererRecord;

public class JoglRendererRecord extends RendererRecord {

    protected final JoglMatrixBackend _matrixBackend;

    public JoglRendererRecord() {
        final GL gl = GLContext.getCurrentGL();
        if (gl.isGL2ES1()) {
            _matrixBackend = new JoglRealMatrixBackend();
        } else {
            _matrixBackend = new JoglSimulatedMatrixBackend();
        }
    }

    public JoglMatrixBackend getMatrixBackend() {
        return _matrixBackend;
    }
}
