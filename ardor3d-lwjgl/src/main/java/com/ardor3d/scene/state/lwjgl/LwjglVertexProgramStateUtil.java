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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.logging.Logger;

import org.lwjgl.opengl.ARBProgram;
import org.lwjgl.opengl.ARBVertexProgram;
import org.lwjgl.opengl.GL11;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.VertexProgramState;
import com.ardor3d.renderer.state.record.VertexProgramStateRecord;
import com.ardor3d.util.geom.BufferUtils;

public abstract class LwjglVertexProgramStateUtil {
    private static final Logger logger = Logger.getLogger(LwjglVertexProgramStateUtil.class.getName());

    /**
     * Queries OpenGL for errors in the vertex program. Errors are logged as SEVERE, noting both the line number and
     * message.
     */
    private static void checkProgramError() {
        if (GL11.glGetError() == GL11.GL_INVALID_OPERATION) {
            // retrieve the error position
            final IntBuffer errorloc = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(ARBProgram.GL_PROGRAM_ERROR_POSITION_ARB, errorloc);

            logger.severe("Error " + GL11.glGetString(ARBProgram.GL_PROGRAM_ERROR_STRING_ARB)
                    + " in vertex program on line " + errorloc.get(0));
        }
    }

    protected static int create(final ByteBuffer program) {

        final IntBuffer buf = BufferUtils.createIntBuffer(1);

        ARBProgram.glGenProgramsARB(buf);
        ARBProgram.glBindProgramARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, buf.get(0));
        ARBProgram.glProgramStringARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, ARBProgram.GL_PROGRAM_FORMAT_ASCII_ARB,
                program);

        checkProgramError();

        return buf.get(0);
    }

    /**
     * Applies this vertex program to the current scene. Checks if the GL_ARB_vertex_program extension is supported
     * before attempting to enable this program.
     * 
     * @see com.ardor3d.renderer.state.RenderState#apply()
     */
    public static void apply(final VertexProgramState state) {
        final RenderContext context = ContextManager.getCurrentContext();
        if (context.getCapabilities().isVertexProgramSupported()) {
            // ask for the current state record
            final VertexProgramStateRecord record = (VertexProgramStateRecord) context
                    .getStateRecord(StateType.VertexProgram);
            context.setCurrentState(StateType.VertexProgram, state);

            if (!record.isValid() || record.getReference() != state) {
                record.setReference(state);
                if (state.isEnabled()) {
                    // Vertex program not yet loaded
                    if (state._getProgramID() == -1) {
                        if (state.getProgramAsBuffer() != null) {
                            final int id = create(state.getProgramAsBuffer());
                            state._setProgramID(id);
                        } else {
                            return;
                        }
                    }

                    GL11.glEnable(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB);
                    ARBProgram.glBindProgramARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, state._getProgramID());

                    // load environmental parameters...
                    for (int i = 0; i < VertexProgramState._getEnvParameters().length; i++) {
                        if (VertexProgramState._getEnvParameters()[i] != null) {
                            ARBProgram.glProgramEnvParameter4fARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, i,
                                    VertexProgramState._getEnvParameters()[i][0],
                                    VertexProgramState._getEnvParameters()[i][1],
                                    VertexProgramState._getEnvParameters()[i][2],
                                    VertexProgramState._getEnvParameters()[i][3]);
                        }
                    }

                    // load local parameters...
                    if (state.isUsingParameters()) {
                        // no parameters are used
                        for (int i = 0; i < state._getParameters().length; i++) {
                            if (state._getParameters()[i] != null) {
                                ARBProgram.glProgramLocalParameter4fARB(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB, i,
                                        state._getParameters()[i][0], state._getParameters()[i][1],
                                        state._getParameters()[i][2], state._getParameters()[i][3]);
                            }
                        }
                    }

                } else {
                    GL11.glDisable(ARBVertexProgram.GL_VERTEX_PROGRAM_ARB);
                }
            }

            if (!record.isValid()) {
                record.validate();
            }
        }
    }
}
