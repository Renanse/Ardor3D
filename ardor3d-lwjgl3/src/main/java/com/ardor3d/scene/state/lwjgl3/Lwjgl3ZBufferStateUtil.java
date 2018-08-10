/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl3;

import org.lwjgl.opengl.GL11C;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.record.ZBufferStateRecord;

public abstract class Lwjgl3ZBufferStateUtil {

    public static void apply(final ZBufferState state) {
        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ZBufferStateRecord record = (ZBufferStateRecord) context.getStateRecord(StateType.ZBuffer);
        context.setCurrentState(StateType.ZBuffer, state);

        enableDepthTest(state.isEnabled(), record);
        if (state.isEnabled()) {
            int depthFunc = 0;
            switch (state.getFunction()) {
                case Never:
                    depthFunc = GL11C.GL_NEVER;
                    break;
                case LessThan:
                    depthFunc = GL11C.GL_LESS;
                    break;
                case EqualTo:
                    depthFunc = GL11C.GL_EQUAL;
                    break;
                case LessThanOrEqualTo:
                    depthFunc = GL11C.GL_LEQUAL;
                    break;
                case GreaterThan:
                    depthFunc = GL11C.GL_GREATER;
                    break;
                case NotEqualTo:
                    depthFunc = GL11C.GL_NOTEQUAL;
                    break;
                case GreaterThanOrEqualTo:
                    depthFunc = GL11C.GL_GEQUAL;
                    break;
                case Always:
                    depthFunc = GL11C.GL_ALWAYS;
            }
            applyFunction(depthFunc, record);
        }

        enableWrite(state.isWritable(), record);

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static void enableDepthTest(final boolean enable, final ZBufferStateRecord record) {
        if (enable && (!record.depthTest || !record.isValid())) {
            GL11C.glEnable(GL11C.GL_DEPTH_TEST);
            record.depthTest = true;
        } else if (!enable && (record.depthTest || !record.isValid())) {
            GL11C.glDisable(GL11C.GL_DEPTH_TEST);
            record.depthTest = false;
        }
    }

    private static void applyFunction(final int depthFunc, final ZBufferStateRecord record) {
        if (depthFunc != record.depthFunc || !record.isValid()) {
            GL11C.glDepthFunc(depthFunc);
            record.depthFunc = depthFunc;
        }
    }

    private static void enableWrite(final boolean enable, final ZBufferStateRecord record) {
        if (enable != record.writable || !record.isValid()) {
            GL11C.glDepthMask(enable);
            record.writable = enable;
        }
    }
}
