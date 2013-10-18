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
import com.ardor3d.renderer.jogl.state.record.JoglRendererRecord;
import com.ardor3d.util.geom.jogl.DirectNioBuffersSet;

public class JoglRenderContext extends RenderContext {

    private final DirectNioBuffersSet _directNioBuffersSet;

    public JoglRenderContext(final Object key, final ContextCapabilities caps,
            final DirectNioBuffersSet directNioBuffersSet) {
        this(key, caps, null, directNioBuffersSet);
    }

    public JoglRenderContext(final Object key, final ContextCapabilities caps, final RenderContext shared,
            final DirectNioBuffersSet directNioBuffersSet) {
        super(key, caps, shared);
        _directNioBuffersSet = directNioBuffersSet;
    }

    @Override
    protected JoglRendererRecord createRendererRecord() {
        final JoglRendererRecord rendererRecord = new JoglRendererRecord();
        return rendererRecord;
    }

    public DirectNioBuffersSet getDirectNioBuffersSet() {
        return _directNioBuffersSet;
    }

    @Override
    public JoglRendererRecord getRendererRecord() {
        return (JoglRendererRecord) _rendererRecord;
    }
}
