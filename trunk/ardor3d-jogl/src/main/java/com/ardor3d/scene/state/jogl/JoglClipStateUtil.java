/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.ClipStateRecord;

public abstract class JoglClipStateUtil {

    public static void apply(final JoglRenderer renderer, final ClipState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ClipStateRecord record = (ClipStateRecord) context.getStateRecord(StateType.Clip);
        context.setCurrentState(StateType.Clip, state);

        final ContextCapabilities caps = context.getCapabilities();
        final int max = Math.min(ClipState.MAX_CLIP_PLANES, caps.getMaxUserClipPlanes());

        if (state.isEnabled()) {
            for (int i = 0; i < max; i++) {
                enableClipPlane(i, state.getPlaneEnabled(i), state, record);
            }
        } else {
            for (int i = 0; i < max; i++) {
                enableClipPlane(i, false, state, record);
            }
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void enableClipPlane(final int planeIndex, final boolean enable, final ClipState state,
            final ClipStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (enable) {
            if (!record.isValid() || !record.planeEnabled[planeIndex]) {
                gl.glEnable(GL.GL_CLIP_PLANE0 + planeIndex);
                record.planeEnabled[planeIndex] = true;
            }

            record.buf.rewind();
            record.buf.put(state.getPlaneEquations(planeIndex));
            record.buf.flip();
            gl.glClipPlane(GL.GL_CLIP_PLANE0 + planeIndex, record.buf);

        } else {
            if (!record.isValid() || record.planeEnabled[planeIndex]) {
                gl.glDisable(GL.GL_CLIP_PLANE0 + planeIndex);
                record.planeEnabled[planeIndex] = false;
            }
        }
    }
}
