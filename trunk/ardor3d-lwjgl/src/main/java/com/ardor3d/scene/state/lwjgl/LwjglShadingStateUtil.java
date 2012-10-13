/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl;

import org.lwjgl.opengl.GL11;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ShadingState;
import com.ardor3d.renderer.state.ShadingState.ShadingMode;
import com.ardor3d.renderer.state.record.ShadingStateRecord;

public abstract class LwjglShadingStateUtil {

    public static void apply(final ShadingState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ShadingStateRecord record = (ShadingStateRecord) context.getStateRecord(StateType.Shading);
        context.setCurrentState(StateType.Shading, state);

        // If not enabled, we'll use smooth
        final int toApply = state.isEnabled() ? getGLShade(state.getShadingMode()) : GL11.GL_SMOOTH;
        // only apply if we're different. Update record to reflect any changes.
        if (!record.isValid() || toApply != record.lastShade) {
            GL11.glShadeModel(toApply);
            record.lastShade = toApply;
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static int getGLShade(final ShadingMode shadeMode) {
        switch (shadeMode) {
            case Flat:
                return GL11.GL_FLAT;
            case Smooth:
                return GL11.GL_SMOOTH;
        }
        throw new IllegalStateException("unknown shade mode: " + shadeMode);
    }
}
