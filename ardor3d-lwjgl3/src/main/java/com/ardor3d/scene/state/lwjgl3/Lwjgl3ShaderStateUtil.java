/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl3;

import java.util.logging.Logger;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.lwjgl3.Lwjgl3Renderer;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ShaderState;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.renderer.state.record.ShaderStateRecord;
import com.ardor3d.scene.state.lwjgl3.util.Lwjgl3ShaderUtil;
import com.ardor3d.util.Ardor3dException;

public abstract class Lwjgl3ShaderStateUtil {
    private static final Logger logger = Logger.getLogger(Lwjgl3ShaderStateUtil.class.getName());

    public static void apply(final Lwjgl3Renderer renderer, final ShaderState state) {
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
                Lwjgl3ShaderUtil.useShaderProgram(programId, record);

            } else {
                // set our current shader to off
                Lwjgl3ShaderUtil.useShaderProgram(0, record);
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

        final int programId = GL20C.glCreateProgram();
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
        GL20C.glAttachShader(programId, vertShaderId);
        GL20C.glAttachShader(programId, fragShaderId);

        if (geoShaderId != -1) {
            GL20C.glAttachShader(programId, geoShaderId);
        }
        if (tessCtrlShaderId != -1) {
            GL20C.glAttachShader(programId, tessCtrlShaderId);
        }
        if (tessEvalShaderId != -1) {
            GL20C.glAttachShader(programId, tessEvalShaderId);
        }

        // Link our shaders to the program
        GL20C.glLinkProgram(programId);

        // Check for link errors
        final int success = GL20C.glGetProgrami(programId, GL20C.GL_LINK_STATUS);
        if (success == GL11C.GL_FALSE) {
            final String info = GL20C.glGetProgramInfoLog(programId);

            logger.severe(info);
            throw new Ardor3dException("Error linking shaders: " + info);
        }

        // Delete our shaders - we're done with them now that they are linked
        GL20C.glDeleteShader(vertShaderId);
        GL20C.glDeleteShader(fragShaderId);

        if (geoShaderId != -1) {
            GL20C.glDeleteShader(geoShaderId);
        }
        if (tessCtrlShaderId != -1) {
            GL20C.glDeleteShader(tessCtrlShaderId);
        }
        if (tessEvalShaderId != -1) {
            GL20C.glDeleteShader(tessEvalShaderId);
        }

        // flag our shader as done
        state.setNeedsRefresh(false);

        return true;
    }

    private static int prepareShader(final ShaderState state, final ShaderType type) {
        if (!state.hasShader(type)) {
            return -1;
        }

        // generate a new shader object
        final int shaderId = GL20C.glCreateShader(Lwjgl3ShaderUtil.getGLShaderType(type));

        // provide our source code
        GL20C.glShaderSource(shaderId, state.getShader(type));

        // compile
        GL20C.glCompileShader(shaderId);

        // check for errors
        final int success = GL20C.glGetShaderi(shaderId, GL20C.GL_COMPILE_STATUS);
        if (success == GL11C.GL_FALSE) {
            final String info = GL20C.glGetShaderInfoLog(shaderId);

            logger.severe(info);
            throw new Ardor3dException("Error compiling shader [" + state.getShaderName(type) + "]: " + info);
        }

        return shaderId;
    }
}
