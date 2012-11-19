/**
 * Copyright (c) 2008-2010 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.ColorMaskState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.ColorMaskStateRecord;

public abstract class JoglColorMaskStateUtil {

    public static void apply(final JoglRenderer renderer, final ColorMaskState state) {
        final GL gl = GLU.getCurrentGL();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ColorMaskStateRecord record = (ColorMaskStateRecord) context.getStateRecord(StateType.ColorMask);
        context.setCurrentState(StateType.ColorMask, state);

        if (state.isEnabled()) {
            if (!record.isValid() || !record.is(state.getRed(), state.getGreen(), state.getBlue(), state.getAlpha())) {
                gl.glColorMask(state.getRed(), state.getGreen(), state.getBlue(), state.getAlpha());
                record.set(state.getRed(), state.getGreen(), state.getBlue(), state.getAlpha());
            }
        } else if (!record.isValid() || !record.is(true, true, true, true)) {
            gl.glColorMask(true, true, true, true);
            record.set(true, true, true, true);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }
}
