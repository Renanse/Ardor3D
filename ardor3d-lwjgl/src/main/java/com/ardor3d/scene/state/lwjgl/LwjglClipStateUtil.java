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

import java.nio.DoubleBuffer;

import org.lwjgl.opengl.GL11;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.ClipState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.ClipStateRecord;

public abstract class LwjglClipStateUtil {

    public static void apply(final ClipState state) {
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
        if (enable) {
            if (!record.isValid() || !record.planeEnabled[planeIndex]) {
                GL11.glEnable(GL11.GL_CLIP_PLANE0 + planeIndex);
                record.planeEnabled[planeIndex] = true;
            }

            record.buf.rewind();
            ((DoubleBuffer) record.buf).put(state.getPlaneEquations(planeIndex));
            record.buf.flip();
            GL11.glClipPlane(GL11.GL_CLIP_PLANE0 + planeIndex, (DoubleBuffer) record.buf);

        } else {
            if (!record.isValid() || record.planeEnabled[planeIndex]) {
                GL11.glDisable(GL11.GL_CLIP_PLANE0 + planeIndex);
                record.planeEnabled[planeIndex] = false;
            }
        }
    }
}
