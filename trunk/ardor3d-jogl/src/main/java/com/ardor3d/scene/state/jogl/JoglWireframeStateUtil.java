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

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.WireframeStateRecord;

public abstract class JoglWireframeStateUtil {

    public static void apply(final JoglRenderer renderer, final WireframeState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final WireframeStateRecord record = (WireframeStateRecord) context.getStateRecord(StateType.Wireframe);
        context.setCurrentState(StateType.Wireframe, state);

        if (state.isEnabled()) {
            renderer.setupLineParameters(state.getLineWidth(), 1, (short) 0xFFFF, state.isAntialiased());

            switch (state.getFace()) {
                case Front:
                    applyPolyMode(GL.GL_LINE, GL.GL_FILL, record);
                    break;
                case Back:
                    applyPolyMode(GL.GL_FILL, GL.GL_LINE, record);
                    break;
                case FrontAndBack:
                default:
                    applyPolyMode(GL.GL_LINE, GL.GL_LINE, record);
                    break;
            }
        } else {
            applyPolyMode(GL.GL_FILL, GL.GL_FILL, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void applyPolyMode(final int frontMode, final int backMode, final WireframeStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (record.isValid()) {
            if (frontMode == backMode && (record.frontMode != frontMode || record.backMode != backMode)) {
                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, frontMode);
                record.frontMode = frontMode;
                record.backMode = backMode;
            } else if (frontMode != backMode) {
                if (record.frontMode != frontMode) {
                    gl.glPolygonMode(GL.GL_FRONT, frontMode);
                    record.frontMode = frontMode;
                }
                if (record.backMode != backMode) {
                    gl.glPolygonMode(GL.GL_BACK, backMode);
                    record.backMode = backMode;
                }
            }

        } else {
            if (frontMode == backMode) {
                gl.glPolygonMode(GL.GL_FRONT_AND_BACK, frontMode);
            } else if (frontMode != backMode) {
                gl.glPolygonMode(GL.GL_FRONT, frontMode);
                gl.glPolygonMode(GL.GL_BACK, backMode);
            }
            record.frontMode = frontMode;
            record.backMode = backMode;
        }
    }
}
