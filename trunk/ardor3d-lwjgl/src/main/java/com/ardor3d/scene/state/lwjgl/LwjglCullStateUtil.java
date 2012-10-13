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
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.renderer.state.CullState.PolygonWind;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.CullStateRecord;

public abstract class LwjglCullStateUtil {

    public static void apply(final CullState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final CullStateRecord record = (CullStateRecord) context.getStateRecord(StateType.Cull);
        context.setCurrentState(StateType.Cull, state);

        if (state.isEnabled()) {
            final Face useCullMode = state.getCullFace();

            switch (useCullMode) {
                case Front:
                    setCull(GL11.GL_FRONT, record);
                    setCullEnabled(true, record);
                    break;
                case Back:
                    setCull(GL11.GL_BACK, record);
                    setCullEnabled(true, record);
                    break;
                case FrontAndBack:
                    setCull(GL11.GL_FRONT_AND_BACK, record);
                    setCullEnabled(true, record);
                    break;
                case None:
                    setCullEnabled(false, record);
                    break;
            }
            setGLPolygonWind(state.getPolygonWind(), record);
        } else {
            setCullEnabled(false, record);
            setGLPolygonWind(PolygonWind.CounterClockWise, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void setCullEnabled(final boolean enable, final CullStateRecord record) {
        if (!record.isValid() || record.enabled != enable) {
            if (enable) {
                GL11.glEnable(GL11.GL_CULL_FACE);
            } else {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
            record.enabled = enable;
        }
    }

    private static void setCull(final int face, final CullStateRecord record) {
        if (!record.isValid() || record.face != face) {
            GL11.glCullFace(face);
            record.face = face;
        }
    }

    private static void setGLPolygonWind(final PolygonWind windOrder, final CullStateRecord record) {
        if (!record.isValid() || record.windOrder != windOrder) {
            switch (windOrder) {
                case CounterClockWise:
                    GL11.glFrontFace(GL11.GL_CCW);
                    break;
                case ClockWise:
                    GL11.glFrontFace(GL11.GL_CW);
                    break;
            }
            record.windOrder = windOrder;
        }
    }
}
