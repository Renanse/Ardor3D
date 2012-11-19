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
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.renderer.state.CullState.PolygonWind;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.CullStateRecord;

public abstract class JoglCullStateUtil {

    public static void apply(final JoglRenderer renderer, final CullState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final CullStateRecord record = (CullStateRecord) context.getStateRecord(StateType.Cull);
        context.setCurrentState(StateType.Cull, state);

        if (state.isEnabled()) {
            final Face useCullMode = state.getCullFace();

            switch (useCullMode) {
                case Front:
                    setCull(GL.GL_FRONT, state, record);
                    setCullEnabled(true, state, record);
                    break;
                case Back:
                    setCull(GL.GL_BACK, state, record);
                    setCullEnabled(true, state, record);
                    break;
                case FrontAndBack:
                    setCull(GL.GL_FRONT_AND_BACK, state, record);
                    setCullEnabled(true, state, record);
                    break;
                case None:
                    setCullEnabled(false, state, record);
                    break;
            }
            setGLPolygonWind(state.getPolygonWind(), state, record);
        } else {
            setCullEnabled(false, state, record);
            setGLPolygonWind(PolygonWind.CounterClockWise, state, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void setCullEnabled(final boolean enable, final CullState state, final CullStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (!record.isValid() || record.enabled != enable) {
            if (enable) {
                gl.glEnable(GL.GL_CULL_FACE);
            } else {
                gl.glDisable(GL.GL_CULL_FACE);
            }
            record.enabled = enable;
        }
    }

    private static void setCull(final int face, final CullState state, final CullStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (!record.isValid() || record.face != face) {
            gl.glCullFace(face);
            record.face = face;
        }
    }

    private static void setGLPolygonWind(final PolygonWind windOrder, final CullState state,
            final CullStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (!record.isValid() || record.windOrder != windOrder) {
            switch (windOrder) {
                case CounterClockWise:
                    gl.glFrontFace(GL.GL_CCW);
                    break;
                case ClockWise:
                    gl.glFrontFace(GL.GL_CW);
                    break;
            }
            record.windOrder = windOrder;
        }
    }

}
