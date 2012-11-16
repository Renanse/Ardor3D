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
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.record.ZBufferStateRecord;

public abstract class JoglZBufferStateUtil {

    public static void apply(final JoglRenderer renderer, final ZBufferState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ZBufferStateRecord record = (ZBufferStateRecord) context.getStateRecord(RenderState.StateType.ZBuffer);
        context.setCurrentState(RenderState.StateType.ZBuffer, state);

        enableDepthTest(state.isEnabled(), record);
        if (state.isEnabled()) {
            int depthFunc = 0;
            switch (state.getFunction()) {
                case Never:
                    depthFunc = GL.GL_NEVER;
                    break;
                case LessThan:
                    depthFunc = GL.GL_LESS;
                    break;
                case EqualTo:
                    depthFunc = GL.GL_EQUAL;
                    break;
                case LessThanOrEqualTo:
                    depthFunc = GL.GL_LEQUAL;
                    break;
                case GreaterThan:
                    depthFunc = GL.GL_GREATER;
                    break;
                case NotEqualTo:
                    depthFunc = GL.GL_NOTEQUAL;
                    break;
                case GreaterThanOrEqualTo:
                    depthFunc = GL.GL_GEQUAL;
                    break;
                case Always:
                    depthFunc = GL.GL_ALWAYS;
            }
            applyFunction(depthFunc, record);
        }

        enableWrite(state.isWritable(), record);

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void enableDepthTest(final boolean enable, final ZBufferStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (enable && (!record.depthTest || !record.isValid())) {
            gl.glEnable(GL.GL_DEPTH_TEST);
            record.depthTest = true;
        } else if (!enable && (record.depthTest || !record.isValid())) {
            gl.glDisable(GL.GL_DEPTH_TEST);
            record.depthTest = false;
        }
    }

    private static void applyFunction(final int depthFunc, final ZBufferStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (depthFunc != record.depthFunc || !record.isValid()) {
            gl.glDepthFunc(depthFunc);
            record.depthFunc = depthFunc;
        }
    }

    private static void enableWrite(final boolean enable, final ZBufferStateRecord record) {
        final GL gl = GLU.getCurrentGL();

        if (enable != record.writable || !record.isValid()) {
            gl.glDepthMask(enable);
            record.writable = enable;
        }
    }
}
