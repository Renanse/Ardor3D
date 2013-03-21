/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.jogl;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.record.RendererRecord;

public class JoglRenderContext extends RenderContext {

    public JoglRenderContext(final Object key, final ContextCapabilities caps) {
        this(key, caps, null);
    }

    public JoglRenderContext(final Object key, final ContextCapabilities caps, final RenderContext shared) {
        super(key, caps, shared);
    }

    @Override
    protected RendererRecord createRendererRecord() {
        // TODO create a renderer record that performs glMatrixMode, glOrtho, glPushMatrix, glLoadMatrix, glPopMatrix,
        // glMultMatrixf and glLoadMatrixf. Use PMVMatrix in a delegate
        final RendererRecord rendererRecord = new RendererRecord();
        return rendererRecord;
    }

}
