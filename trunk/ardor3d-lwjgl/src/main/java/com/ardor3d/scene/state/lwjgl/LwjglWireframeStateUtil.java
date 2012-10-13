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
import com.ardor3d.renderer.lwjgl.LwjglRenderer;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.record.WireframeStateRecord;

public abstract class LwjglWireframeStateUtil {

    public static void apply(final LwjglRenderer renderer, final WireframeState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final WireframeStateRecord record = (WireframeStateRecord) context.getStateRecord(StateType.Wireframe);
        context.setCurrentState(StateType.Wireframe, state);

        if (state.isEnabled()) {
            renderer.setupLineParameters(state.getLineWidth(), 1, (short) 0xFFFF, state.isAntialiased());

            switch (state.getFace()) {
                case Front:
                    applyPolyMode(GL11.GL_LINE, GL11.GL_FILL, record);
                    break;
                case Back:
                    applyPolyMode(GL11.GL_FILL, GL11.GL_LINE, record);
                    break;
                case FrontAndBack:
                default:
                    applyPolyMode(GL11.GL_LINE, GL11.GL_LINE, record);
                    break;
            }
        } else {
            applyPolyMode(GL11.GL_FILL, GL11.GL_FILL, record);
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void applyPolyMode(final int frontMode, final int backMode, final WireframeStateRecord record) {

        if (record.isValid()) {
            if (frontMode == backMode && (record.frontMode != frontMode || record.backMode != backMode)) {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, frontMode);
                record.frontMode = frontMode;
                record.backMode = backMode;
            } else if (frontMode != backMode) {
                if (record.frontMode != frontMode) {
                    GL11.glPolygonMode(GL11.GL_FRONT, frontMode);
                    record.frontMode = frontMode;
                }
                if (record.backMode != backMode) {
                    GL11.glPolygonMode(GL11.GL_BACK, backMode);
                    record.backMode = backMode;
                }
            }

        } else {
            if (frontMode == backMode) {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, frontMode);
            } else if (frontMode != backMode) {
                GL11.glPolygonMode(GL11.GL_FRONT, frontMode);
                GL11.glPolygonMode(GL11.GL_BACK, backMode);
            }
            record.frontMode = frontMode;
            record.backMode = backMode;
        }
    }
}
