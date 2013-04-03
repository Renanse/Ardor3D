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
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.renderer.state.record.VertexProgramStateRecord;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Implementation of the GL_ARB_vertex_program extension.
 */
public class VertexProgramState extends RenderState {
    private static final Logger logger = Logger.getLogger(VertexProgramState.class.getName());

    /** Environmental parameters applied to all vertex programs */
    protected static float[][] _envparameters = new float[96][];

    /** If any local parameters for this VP state are set */
    protected boolean _usingParameters = false;

    /** Parameters local to this vertex program */
    protected float[][] _parameters;
    protected ByteBuffer _program;

    protected int _programID = -1;

    /**
     * <code>setEnvParameter</code> sets an environmental vertex program parameter that is accessible by all vertex
     * programs in memory.
     * 
     * @param param
     *            four-element array of floating point numbers
     * @param paramID
     *            identity number of the parameter, ranging from 0 to 95
     */
    public static void setEnvParameter(final float[] param, final int paramID) {
        if (paramID < 0 || paramID > 95) {
            throw new IllegalArgumentException("Invalid parameter ID");
        }
        if (param != null && param.length != 4) {
            throw new IllegalArgumentException("Vertex program parameters must be of type float[4]");
        }

        _envparameters[paramID] = param;
    }

    /**
     * Creates a new VertexProgramState. <code>load(URL)</code> must be called before the state can be used.
     */
    public VertexProgramState() {
        _parameters = new float[96][];
    }

    /**
     * <code>setParameter</code> sets a parameter for this vertex program.
     * 
     * @param paramID
     *            identity number of the parameter, ranging from 0 to 95
     * @param param
     *            four-element array of floating point numbers
     */
    public void setParameter(final float[] param, final int paramID) {
        if (paramID < 0 || paramID > 95) {
            throw new IllegalArgumentException("Invalid parameter ID");
        }
        if (param != null && param.length != 4) {
            throw new IllegalArgumentException("Vertex program parameters must be of type float[4]");
        }

        _usingParameters = true;
        _parameters[paramID] = param;
        setNeedsRefresh(true);
    }

    @Override
    public StateType getType() {
        return StateType.VertexProgram;
    }

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
            final byte[] data = outputStream.toByteArray();

            // Release resources
            inputStream.close();
            outputStream.close();

            _program = BufferUtils.createByteBuffer(data.length);
            _program.put(data);
            _program.rewind();
            _programID = -1;
            setNeedsRefresh(true);

        } catch (final Exception e) {
            logger.severe("Could not load vertex program: " + e);
            logger.logp(Level.SEVERE, getClass().getName(), "load(URL)", "Exception", e);
        } finally {
            // Ensure that the stream is closed, even if there is an exception.
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (final IOException closeFailure) {
                    logger.log(Level.WARNING, "Failed to close the vertex program", closeFailure);
                }
            }

        }
    }

    /**
     * Loads the vertex program into a byte array.
     * 
     * @see com.ardor3d.renderer.state.VertexProgramState#load(java.net.URL)
     */
    public void load(final String programContents) {
        try {
            final byte[] bytes = programContents.getBytes();
            _program = BufferUtils.createByteBuffer(bytes.length);
            _program.put(bytes);
            _program.rewind();
            _programID = -1;
            setNeedsRefresh(true);

        } catch (final Exception e) {
            logger.severe("Could not load vertex program: " + e);
            logger.logp(Level.SEVERE, getClass().getName(), "load(URL)", "Exception", e);
        }
    }

    public ByteBuffer getProgramAsBuffer() {
        return _program;
    }

    public int _getProgramID() {
        return _programID;
    }

    public void _setProgramID(final int id) {
        _programID = id;
    }

    public boolean isUsingParameters() {
        return _usingParameters;
    }

    public float[][] _getParameters() {
        return _parameters;
    }

    public static float[][] _getEnvParameters() {
        return _envparameters;
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
        if (_program == null) {
            s.writeInt(0);
        } else {
            s.writeInt(_program.capacity());
            _program.rewind();
            for (int x = 0, len = _program.capacity(); x < len; x++) {
                s.writeByte(_program.get());
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
            _program = null;
        } else {
            _program = BufferUtils.createByteBuffer(len);
            for (int x = 0; x < len; x++) {
                _program.put(s.readByte());
            }
        }
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_usingParameters, "usingParameters", false);
        capsule.write(_parameters, "parameters", new float[96][]);
        capsule.write(_program, "program", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _usingParameters = capsule.readBoolean("usingParameters", false);
        _parameters = capsule.readFloatArray2D("parameters", new float[96][]);
        _program = capsule.readByteBuffer("program", null);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new VertexProgramStateRecord();
    }

}
