/**
 * Copyright (c) 2008-2010 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.StencilState;
import com.ardor3d.renderer.state.StencilState.StencilFunction;
import com.ardor3d.renderer.state.StencilState.StencilOperation;
import com.ardor3d.renderer.state.record.StencilStateRecord;

public abstract class JoglStencilStateUtil {

    public static void apply(final JoglRenderer renderer, final StencilState state) {
        final GL2 gl = GLContext.getCurrentGL().getGL2();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final StencilStateRecord record = (StencilStateRecord) context.getStateRecord(StateType.Stencil);
        context.setCurrentState(StateType.Stencil, state);

        setEnabled(state.isEnabled(), record);
        if (state.isEnabled()) {
            if (state.isUseTwoSided()) {
                gl.glStencilMaskSeparate(GL.GL_BACK, state.getStencilWriteMaskBack());

                gl.glStencilFuncSeparate(GL.GL_BACK, //
                        getGLStencilFunction(state.getStencilFunctionBack()), //
                        state.getStencilReferenceBack(), //
                        state.getStencilFuncMaskBack());
                gl.glStencilOpSeparate(GL.GL_BACK, //
                        getGLStencilOp(state.getStencilOpFailBack()), //
                        getGLStencilOp(state.getStencilOpZFailBack()), //
                        getGLStencilOp(state.getStencilOpZPassBack()));

                gl.glStencilMaskSeparate(GL.GL_FRONT, state.getStencilWriteMaskFront());
                gl.glStencilFuncSeparate(GL.GL_FRONT, //
                        getGLStencilFunction(state.getStencilFunctionFront()), //
                        state.getStencilReferenceFront(), //
                        state.getStencilFuncMaskFront());
                gl.glStencilOpSeparate(GL.GL_FRONT, //
                        getGLStencilOp(state.getStencilOpFailFront()), //
                        getGLStencilOp(state.getStencilOpZFailFront()), //
                        getGLStencilOp(state.getStencilOpZPassFront()));
            } else {
                gl.glStencilMask(state.getStencilWriteMaskFront());
                gl.glStencilFunc( //
                        getGLStencilFunction(state.getStencilFunctionFront()), //
                        state.getStencilReferenceFront(), //
                        state.getStencilFuncMaskFront());
                gl.glStencilOp( //
                        getGLStencilOp(state.getStencilOpFailFront()), //
                        getGLStencilOp(state.getStencilOpZFailFront()), //
                        getGLStencilOp(state.getStencilOpZPassFront()));
            }
        }

        if (!record.isValid()) {
            record.validate();
        }
    }

    private static int getGLStencilFunction(final StencilFunction function) {
        switch (function) {
            case Always:
                return GL.GL_ALWAYS;
            case Never:
                return GL.GL_NEVER;
            case EqualTo:
                return GL.GL_EQUAL;
            case NotEqualTo:
                return GL.GL_NOTEQUAL;
            case GreaterThan:
                return GL.GL_GREATER;
            case GreaterThanOrEqualTo:
                return GL.GL_GEQUAL;
            case LessThan:
                return GL.GL_LESS;
            case LessThanOrEqualTo:
                return GL.GL_LEQUAL;
        }
        throw new IllegalArgumentException("unknown function: " + function);
    }

    private static int getGLStencilOp(final StencilOperation operation) {
        switch (operation) {
            case Keep:
                return GL.GL_KEEP;
            case DecrementWrap:
                return GL.GL_DECR_WRAP;
            case Decrement:
                return GL.GL_DECR;
            case IncrementWrap:
                return GL.GL_INCR_WRAP;
            case Increment:
                return GL.GL_INCR;
            case Invert:
                return GL.GL_INVERT;
            case Replace:
                return GL.GL_REPLACE;
            case Zero:
                return GL.GL_ZERO;
        }
        throw new IllegalArgumentException("unknown operation: " + operation);
    }

    private static void setEnabled(final boolean enable, final StencilStateRecord record) {
        final GL gl = GLContext.getCurrentGL();

        if (enable && (!record.isValid() || !record.enabled)) {
            gl.glEnable(GL.GL_STENCIL_TEST);
        } else if (!enable && (!record.isValid() || record.enabled)) {
            gl.glDisable(GL.GL_STENCIL_TEST);
        }

        record.enabled = enable;
    }
}
