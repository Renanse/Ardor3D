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

import org.lwjgl.opengl.ARBFragmentProgram;
import org.lwjgl.opengl.ARBProgram;
import org.lwjgl.opengl.GL11;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.FragmentProgramState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.FragmentProgramStateRecord;
import com.ardor3d.util.geom.BufferUtils;

public final class LwjglFragmentProgramStateUtil {
    private static final Logger logger = Logger.getLogger(LwjglFragmentProgramStateUtil.class.getName());

    /**
     * Queries OpenGL for errors in the fragment program. Errors are logged as SEVERE, noting both the line number and
     * message.
     */
    private static void checkProgramError() {
        if (GL11.glGetError() == GL11.GL_INVALID_OPERATION) {
            // retrieve the error position
            final IntBuffer errorloc = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(ARBProgram.GL_PROGRAM_ERROR_POSITION_ARB, errorloc);

            logger.severe("Error " + GL11.glGetString(ARBProgram.GL_PROGRAM_ERROR_STRING_ARB)
                    + " in fragment program on line " + errorloc.get(0));
        }
    }

    private static int create(final ByteBuffer program) {

        final IntBuffer buf = BufferUtils.createIntBuffer(1);

        ARBProgram.glGenProgramsARB(buf);
        ARBProgram.glBindProgramARB(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB, buf.get(0));
        ARBProgram.glProgramStringARB(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB,
                ARBProgram.GL_PROGRAM_FORMAT_ASCII_ARB, program);

        checkProgramError();

        return buf.get(0);
    }

    public static void apply(final FragmentProgramState state) {
        final RenderContext context = ContextManager.getCurrentContext();
        if (context.getCapabilities().isFragmentProgramSupported()) {
            final FragmentProgramStateRecord record = (FragmentProgramStateRecord) context
                    .getStateRecord(StateType.FragmentProgram);
            context.setCurrentState(StateType.FragmentProgram, state);

            if (!record.isValid() || record.getReference() != state) {
                record.setReference(state);
                if (state.isEnabled()) {
                    // Fragment program not yet loaded
                    if (state._getProgramID() == -1) {
                        if (state.getProgramAsBuffer() != null) {
                            final int id = create(state.getProgramAsBuffer());
                            state._setProgramID(id);
                        } else {
                            return;
                        }
                    }

                    GL11.glEnable(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB);
                    ARBProgram.glBindProgramARB(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB, state._getProgramID());

                    // load environmental parameters...
                    // TODO: Reevaluate how this is done.
                    /*
                     * for (int i = 0; i < envparameters.length; i++) if (envparameters[i] != null)
                     * ARBFragmentProgram.glProgramEnvParameter4fARB( ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB, i,
                     * envparameters[i][0], envparameters[i][1], envparameters[i][2], envparameters[i][3]);
                     */

                    // load local parameters...
                    if (state.isUsingParameters()) {
                        // no parameters are used
                        for (int i = 0; i < state._getParameters().length; i++) {
                            if (state._getParameters()[i] != null) {
                                ARBProgram.glProgramLocalParameter4fARB(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB, i,
                                        state._getParameters()[i][0], state._getParameters()[i][1],
                                        state._getParameters()[i][2], state._getParameters()[i][3]);
                            }
                        }
                    }

                } else {
                    GL11.glDisable(ARBFragmentProgram.GL_FRAGMENT_PROGRAM_ARB);
                }
            }

            if (!record.isValid()) {
                record.validate();
            }
        }
    }
}
