/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.jogl.JoglRenderContext;
import com.ardor3d.renderer.jogl.JoglRenderer;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.renderer.state.record.ShaderStateRecord;
import com.ardor3d.scene.state.jogl.shader.JoglShaderUtil;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.BufferUtils;

public abstract class JoglShaderObjectsStateUtil {
    private static final Logger logger = Logger.getLogger(JoglShaderObjectsStateUtil.class.getName());

    public static void apply(final JoglRenderer renderer, final ShaderState state) {
        final RenderContext context = ContextManager.getCurrentContext();

        // Ask for the current state record
        final ShaderStateRecord record = (ShaderStateRecord) context.getStateRecord(StateType.Shader);
        context.setCurrentState(StateType.Shader, state);

        // Check if we need to setup our shader on the card
        int programId = state.getProgramId(context);
        if (state.isEnabled() && programId <= 0) {
            if (!setupShaders(state, context)) {
                return;
            }
            programId = state.getProgramId(context);
        }

        // See if we need to turn on or off our shader
        if (!record.isValid() || record.programId != programId || state.needsRefresh()) {
            if (state.isEnabled() && programId != -1) {
                // set our current shader
                JoglShaderUtil.useShaderProgram(programId, record);

            } else {
                // set our current shader to off
                JoglShaderUtil.useShaderProgram(0, record);
            }

            if (!record.isValid()) {
                record.validate();
            }
        }
    }

    protected static boolean setupShaders(final ShaderState state, final RenderContext context) {
        if (!state.hasShader(ShaderType.Vertex) || !state.hasShader(ShaderType.Fragment)) {
            logger.severe("Invalid ShaderState - must have at least Vertex and Fragment shaders.");
            return false;
        }

        final GL3 gl = GLContext.getCurrentGL().getGL3();
        final int programId = gl.glCreateProgram();
        state.setProgramId(context.getContextKey(), programId);

        final int vertShaderId = prepareShader(state, ShaderType.Vertex);
        final int fragShaderId = prepareShader(state, ShaderType.Fragment);
        final int geoShaderId = prepareShader(state, ShaderType.Geometry);

        final ContextCapabilities caps = context.getCapabilities();
        final int tessCtrlShaderId, tessEvalShaderId;
        if (caps.isTessellationShadersSupported()) {
            tessCtrlShaderId = prepareShader(state, ShaderType.TessellationControl);
            tessEvalShaderId = prepareShader(state, ShaderType.TessellationEvaluation);
        } else {
            tessCtrlShaderId = tessEvalShaderId = -1;
        }

        // Attach any prepared shaders - (vert and frag are required)
        gl.glAttachShader(programId, vertShaderId);
        gl.glAttachShader(programId, fragShaderId);

        if (geoShaderId != -1) {
            gl.glAttachShader(programId, geoShaderId);
        }
        if (tessCtrlShaderId != -1) {
            gl.glAttachShader(programId, tessCtrlShaderId);
        }
        if (tessEvalShaderId != -1) {
            gl.glAttachShader(programId, tessEvalShaderId);
        }

        // Link our shaders to the program
        gl.glLinkProgram(programId);

        // Check for link errors
        checkLinkError(programId);

        // Delete our shaders - we're done with them now that they are linked
        gl.glDeleteShader(vertShaderId);
        gl.glDeleteShader(fragShaderId);

        if (geoShaderId != -1) {
            gl.glDeleteShader(geoShaderId);
        }
        if (tessCtrlShaderId != -1) {
            gl.glDeleteShader(tessCtrlShaderId);
        }
        if (tessEvalShaderId != -1) {
            gl.glDeleteShader(tessEvalShaderId);
        }

        // flag our shader as done
        state.setNeedsRefresh(false);

        return true;
    }

    private static int prepareShader(final ShaderState state, final ShaderType type) {
        if (!state.hasShader(type)) {
            return -1;
        }

        final GL3 gl = GLContext.getCurrentGL().getGL3();

        // generate a new shader object
        final int shaderId = gl.glCreateShader(JoglShaderUtil.getGLShaderType(type));

        // provide our source code
        final String source = state.getShader(type);
        gl.glShaderSource(shaderId, 1, new String[] { source }, new int[] { source.length() }, 0);

        // compile
        gl.glCompileShader(shaderId);

        // check for errors
        checkProgramError(shaderId, state.getShaderName(type));

        return shaderId;
    }

    private static void checkLinkError(final int programId) {
        final GL gl = GLContext.getCurrentGL();

        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
        final IntBuffer compiled = context.getDirectNioBuffersSet().getSingleIntBuffer();
        compiled.clear();
        if (gl.isGL2()) {
            gl.getGL2().glGetObjectParameterivARB(programId, GL2ES2.GL_LINK_STATUS, compiled);
        } else {
            if (gl.isGL2ES2()) {
                gl.getGL2ES2().glGetProgramiv(programId, GL2ES2.GL_LINK_STATUS, compiled);
            }
        }
        if (compiled.get(0) == GL.GL_FALSE) {
            if (gl.isGL2()) {
                gl.getGL2().glGetObjectParameterivARB(programId, GL2ES2.GL_INFO_LOG_LENGTH, compiled);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glGetProgramiv(programId, GL2ES2.GL_INFO_LOG_LENGTH, compiled);
                }
            }
            final int length = compiled.get(0);
            String out = null;
            if (length > 0) {
                final ByteBuffer infoLogBuf = context.getDirectNioBuffersSet().getInfoLogBuffer();
                final ByteBuffer infoLog;
                if (length <= infoLogBuf.capacity()) {
                    infoLog = infoLogBuf;
                    infoLogBuf.rewind().limit(length);
                } else {
                    infoLog = BufferUtils.createByteBuffer(length);
                }
                if (gl.isGL2()) {
                    gl.getGL2().glGetInfoLogARB(programId, infoLog.limit(), compiled, infoLog);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glGetProgramInfoLog(programId, infoLog.limit(), compiled, infoLog);
                    }
                }

                final byte[] infoBytes = new byte[length];
                infoLog.get(infoBytes);
                out = new String(infoBytes);
            }

            logger.severe(out);

            throw new Ardor3dException("Error linking GLSL shader: " + out);
        }
    }

    private static void checkProgramError(final int id, final String shaderName) {
        final GL gl = GLContext.getCurrentGL();
        final JoglRenderContext context = (JoglRenderContext) ContextManager.getCurrentContext();
        final IntBuffer compiled = context.getDirectNioBuffersSet().getSingleIntBuffer();
        compiled.clear();
        gl.getGL2ES2().glGetShaderiv(id, GL2ES2.GL_COMPILE_STATUS, compiled);

        if (compiled.get(0) == GL.GL_FALSE) {
            final IntBuffer iVal = context.getDirectNioBuffersSet().getSingleIntBuffer();
            iVal.clear();
            if (gl.isGL2()) {
                gl.getGL2().glGetObjectParameterivARB(id, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, iVal);
            } else {
                if (gl.isGL2ES2()) {
                    gl.getGL2ES2().glGetProgramiv(id, GL2ES2.GL_INFO_LOG_LENGTH, iVal);
                }
            }
            final int length = iVal.get(0);
            String out = null;

            if (length > 0) {
                final ByteBuffer infoLogBuf = context.getDirectNioBuffersSet().getInfoLogBuffer();
                final ByteBuffer infoLog;
                if (length <= infoLogBuf.capacity()) {
                    infoLog = infoLogBuf;
                    infoLogBuf.rewind().limit(length);
                } else {
                    infoLog = BufferUtils.createByteBuffer(length);
                }
                if (gl.isGL2()) {
                    gl.getGL2().glGetInfoLogARB(id, infoLog.limit(), iVal, infoLog);
                } else {
                    if (gl.isGL2ES2()) {
                        gl.getGL2ES2().glGetProgramInfoLog(id, infoLog.limit(), iVal, infoLog);
                    }
                }

                final byte[] infoBytes = new byte[length];
                infoLog.get(infoBytes);
                out = new String(infoBytes);
            }

            logger.severe(out);

            final String nameString = shaderName.equals("") ? "" : " [ " + shaderName + " ]";
            throw new Ardor3dException("Error compiling GLSL shader " + nameString + ": " + out);
        }
    }
}
