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
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

import com.ardor3d.renderer.ContextCapabilities;
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
        final GL gl = GLContext.getCurrentGL();

        // ask for the current state record
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        final StencilStateRecord record = (StencilStateRecord) context.getStateRecord(StateType.Stencil);
        context.setCurrentState(StateType.Stencil, state);

        setEnabled(state.isEnabled(), caps.isTwoSidedStencilSupported() ? state.isUseTwoSided() : false, record, caps);
        if (state.isEnabled()) {
            if (state.isUseTwoSided() && caps.isTwoSidedStencilSupported()) {
                gl.getGL2().glActiveStencilFaceEXT(GL.GL_BACK);
                applyMask(state.getStencilWriteMaskBack(), record, 2);
                applyFunc(getGLStencilFunction(state.getStencilFunctionBack()), state.getStencilReferenceBack(),
                        state.getStencilFuncMaskBack(), record, 2);
                applyOp(getGLStencilOp(state.getStencilOpFailBack(), caps),
                        getGLStencilOp(state.getStencilOpZFailBack(), caps),
                        getGLStencilOp(state.getStencilOpZPassBack(), caps), record, 2);

                gl.getGL2().glActiveStencilFaceEXT(GL.GL_FRONT);
                applyMask(state.getStencilWriteMaskFront(), record, 1);
                applyFunc(getGLStencilFunction(state.getStencilFunctionFront()), state.getStencilReferenceFront(),
                        state.getStencilFuncMaskFront(), record, 1);
                applyOp(getGLStencilOp(state.getStencilOpFailFront(), caps),
                        getGLStencilOp(state.getStencilOpZFailFront(), caps),
                        getGLStencilOp(state.getStencilOpZPassFront(), caps), record, 1);
            } else {
                applyMask(state.getStencilWriteMaskFront(), record, 0);
                applyFunc(getGLStencilFunction(state.getStencilFunctionFront()), state.getStencilReferenceFront(),
                        state.getStencilFuncMaskFront(), record, 0);
                applyOp(getGLStencilOp(state.getStencilOpFailFront(), caps),
                        getGLStencilOp(state.getStencilOpZFailFront(), caps),
                        getGLStencilOp(state.getStencilOpZPassFront(), caps), record, 0);
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

    private static int getGLStencilOp(final StencilOperation operation, final ContextCapabilities caps) {
        switch (operation) {
            case Keep:
                return GL.GL_KEEP;
            case DecrementWrap:
                if (caps.isStencilWrapSupported()) {
                    return GL.GL_DECR_WRAP;
                }
                // FALLS THROUGH
            case Decrement:
                return GL.GL_DECR;
            case IncrementWrap:
                if (caps.isStencilWrapSupported()) {
                    return GL.GL_INCR_WRAP;
                }
                // FALLS THROUGH
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

    private static void setEnabled(final boolean enable, final boolean twoSided, final StencilStateRecord record,
            final ContextCapabilities caps) {
        final GL gl = GLContext.getCurrentGL();

        if (record.isValid()) {
            if (enable && !record.enabled) {
                gl.glEnable(GL.GL_STENCIL_TEST);
            } else if (!enable && record.enabled) {
                gl.glDisable(GL.GL_STENCIL_TEST);
            }
        } else {
            if (enable) {
                gl.glEnable(GL.GL_STENCIL_TEST);
            } else {
                gl.glDisable(GL.GL_STENCIL_TEST);
            }
        }

        setTwoSidedEnabled(enable ? twoSided : false, record, caps);
        record.enabled = enable;
    }

    private static void setTwoSidedEnabled(final boolean enable, final StencilStateRecord record,
            final ContextCapabilities caps) {
        final GL gl = GLContext.getCurrentGL();

        if (caps.isTwoSidedStencilSupported()) {
            if (record.isValid()) {
                if (enable && !record.useTwoSided) {
                    gl.glEnable(GL2.GL_STENCIL_TEST_TWO_SIDE_EXT);
                } else if (!enable && record.useTwoSided) {
                    gl.glDisable(GL2.GL_STENCIL_TEST_TWO_SIDE_EXT);
                }
            } else {
                if (enable) {
                    gl.glEnable(GL2.GL_STENCIL_TEST_TWO_SIDE_EXT);
                } else {
                    gl.glDisable(GL2.GL_STENCIL_TEST_TWO_SIDE_EXT);
                }
            }
        }
        record.useTwoSided = enable;
    }

    private static void applyMask(final int writeMask, final StencilStateRecord record, final int face) {
        final GL gl = GLContext.getCurrentGL();

        // if (!record.isValid() || writeMask != record.writeMask[face]) {
        gl.glStencilMask(writeMask);
        // record.writeMask[face] = writeMask;
        // }
    }

    private static void applyFunc(final int glfunc, final int stencilRef, final int funcMask,
            final StencilStateRecord record, final int face) {
        final GL gl = GLContext.getCurrentGL();

        // if (!record.isValid() || glfunc != record.func[face] || stencilRef != record.ref[face]
        // || funcMask != record.funcMask[face]) {
        gl.glStencilFunc(glfunc, stencilRef, funcMask);
        // record.func[face] = glfunc;
        // record.ref[face] = stencilRef;
        // record.funcMask[face] = funcMask;
        // }
    }

    private static void applyOp(final int fail, final int zfail, final int zpass, final StencilStateRecord record,
            final int face) {
        final GL gl = GLContext.getCurrentGL();

        // if (!record.isValid() || fail != record.fail[face] || zfail != record.zfail[face]
        // || zpass != record.zpass[face]) {
        gl.glStencilOp(fail, zfail, zpass);
        // record.fail[face] = fail;
        // record.zfail[face] = zfail;
        // record.zpass[face] = zpass;
        // }
    }
}
