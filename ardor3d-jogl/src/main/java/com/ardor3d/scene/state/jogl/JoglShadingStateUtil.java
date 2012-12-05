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
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.glu.GLU;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.ShadingState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ShadingState.ShadingMode;
import com.ardor3d.renderer.state.record.ShadingStateRecord;

public abstract class JoglShadingStateUtil {

    public static void apply(final JoglRenderer renderer, final ShadingState state) {
        final GL gl = GLU.getCurrentGL();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ShadingStateRecord record = (ShadingStateRecord) context.getStateRecord(StateType.Shading);
        context.setCurrentState(StateType.Shading, state);

        // If not enabled, we'll use smooth
        final int toApply = state.isEnabled() ? getGLShade(state.getShadingMode()) : GLLightingFunc.GL_SMOOTH;
        // only apply if we're different. Update record to reflect any changes.
        if (!record.isValid() || toApply != record.lastShade) {
            gl.getGL2().glShadeModel(toApply);
            record.lastShade = toApply;
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static int getGLShade(final ShadingMode shadeMode) {
        switch (shadeMode) {
            case Smooth:
                return GLLightingFunc.GL_SMOOTH;
            case Flat:
                return GLLightingFunc.GL_FLAT;
        }
        throw new IllegalStateException("unknown shade mode: " + shadeMode);
    }
}
