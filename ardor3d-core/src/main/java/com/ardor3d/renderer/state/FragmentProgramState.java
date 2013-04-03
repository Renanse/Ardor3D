/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.state.record.FragmentProgramStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

public class FragmentProgramState extends RenderState {
    private static final Logger logger = Logger.getLogger(FragmentProgramState.class.getName());

    /** If any local parameters for this FP state are set */
    protected boolean usingParameters = false;

    /** Parameters local to this fragment program */
    protected float[][] parameters;
    protected ByteBuffer program;
    protected int _programID = -1;

    /**
     * <code>setEnvParameter</code> sets an environmental fragment program parameter that is accessable by all fragment
     * programs in memory.
     * 
     * @param param
     *            four-element array of floating point numbers
     * @param paramID
     *            identity number of the parameter, ranging from 0 to 95
     */
    // TODO: Reevaluate how this is done.
    /*
     * public static void setEnvParameter(float[] param, int paramID){ if (paramID < 0 || paramID > 95) throw new
     * IllegalArgumentException("Invalid parameter ID"); if (param != null && param.length != 4) throw new
     * IllegalArgumentException("Vertex program parameters must be of type float[4]");
     * 
     * envparameters[paramID] = param; }
     */

    public FragmentProgramState() {
        parameters = new float[24][];
    }

    /**
     * <code>setParameter</code> sets a parameter for this fragment program.
     * 
     * @param paramID
     *            identity number of the parameter, ranging from 0 to 23
     * @param param
     *            four-element array of floating point numbers
     */
    public void setParameter(final float[] param, final int paramID) {
        if (paramID < 0 || paramID > 23) {
            throw new IllegalArgumentException("Invalid parameter ID");
        }
        if (param != null && param.length != 4) {
            throw new IllegalArgumentException("Fragment program parameters must be of type float[4]");
        }

        usingParameters = true;
        parameters[paramID] = param;
        setNeedsRefresh(true);
    }

    @Override
    public StateType getType() {
        return StateType.FragmentProgram;
    }

    /**
     * Loads the fragment program into a byte array.
     * 
     * @see com.ardor3d.renderer.state.FragmentProgramState#load(java.net.URL)
     */
    public void load(final java.net.URL file) {
        InputStream inputStream = null;
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(16 * 1024);
            inputStream = new BufferedInputStream(file.openStream());
            final byte[] buffer = new byte[1024];
            int byteCount = -1;

            // Read the byte content into the output stream first
            while ((byteCount = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, byteCount);
            }

            // Set data with byte content from stream
            final byte data[] = outputStream.toByteArray();

            // Release resources
            inputStream.close();
            outputStream.close();

            program = BufferUtils.createByteBuffer(data.length);
            program.put(data);
            program.rewind();
            _programID = -1;
            setNeedsRefresh(true);
        } catch (final Exception e) {
            logger.severe("Could not load fragment program: " + e);
            logger.logp(Level.SEVERE, getClass().getName(), "load(URL)", "Exception", e);
        } finally {
            // Ensure that the stream is closed, even if there is an exception.
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException closeFailure) {
                    logger.log(Level.WARNING, "Failed to close the fragment program", closeFailure);
                }
            }
        }
    }

    /**
     * Loads the fragment program into a byte array.
     * 
     * @see com.ardor3d.renderer.state.FragmentProgramState#load(java.net.URL)
     */
    public void load(final String programContents) {
        try {
            final byte[] bytes = programContents.getBytes();
            program = BufferUtils.createByteBuffer(bytes.length);
            program.put(bytes);
            program.rewind();
            _programID = -1;
            setNeedsRefresh(true);
        } catch (final Exception e) {
            logger.severe("Could not load fragment program: " + e);
            logger.logp(Level.SEVERE, getClass().getName(), "load(URL)", "Exception", e);
        }
    }

    public ByteBuffer getProgramAsBuffer() {
        return program;
    }

    public int _getProgramID() {
        return _programID;
    }

    public void _setProgramID(final int id) {
        _programID = id;
    }

    public boolean isUsingParameters() {
        return usingParameters;
    }

    public float[][] _getParameters() {
        return parameters;
    }

    /**
     * Used with Serialization. Do not call this directly.
     * 
     * @param s
     * @throws IOException
     * @see java.io.Serializable
     */
    private void writeObject(final java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (program == null) {
            s.writeInt(0);
        } else {
            s.writeInt(program.capacity());
            program.rewind();
            for (int x = 0, len = program.capacity(); x < len; x++) {
                s.writeByte(program.get());
            }
        }
    }

    /**
     * Used with Serialization. Do not call this directly.
     * 
     * @param s
     * @throws IOException
     * @throws ClassNotFoundException
     * @see java.io.Serializable
     */
    private void readObject(final java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        final int len = s.readInt();
        if (len == 0) {
            program = null;
        } else {
            program = BufferUtils.createByteBuffer(len);
            for (int x = 0; x < len; x++) {
                program.put(s.readByte());
            }
        }
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(usingParameters, "usingParameters", false);
        capsule.write(parameters, "parameters", new float[24][]);
        capsule.write(program, "program", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        usingParameters = capsule.readBoolean("usingParameters", false);
        parameters = capsule.readFloatArray2D("parameters", new float[24][]);
        program = capsule.readByteBuffer("program", null);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new FragmentProgramStateRecord();
    }
}
