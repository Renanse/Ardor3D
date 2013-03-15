/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Simple data class storing a buffer of bytes
 */
public class ByteBufferData extends IndexBufferData<ByteBuffer> implements Savable {

    /**
     * Instantiates a new ByteBufferData.
     */
    public ByteBufferData() {}

    /**
     * Instantiates a new ByteBufferData with a buffer of the given size.
     */
    public ByteBufferData(final int size) {
        this(BufferUtils.createByteBuffer(size));
    }

    /**
     * Creates a new ByteBufferData.
     * 
     * @param buffer
     *            Buffer holding the data. Must not be null.
     */
    public ByteBufferData(final ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("Buffer can not be null!");
        }

        _buffer = buffer;
    }

    public Class<? extends ByteBufferData> getClassTag() {
        return getClass();
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _buffer = capsule.readByteBuffer("buffer", null);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_buffer, "buffer", null);
    }

    @Override
    public int get() {
        return _buffer.get() & 0xFF;
    }

    @Override
    public int get(final int index) {
        return _buffer.get(index) & 0xFF;
    }

    @Override
    public ByteBufferData put(final int value) {
        if (value < 0 || value >= 256) {
            throw new IllegalArgumentException("Invalid value passed to byte buffer: " + value);
        }
        _buffer.put((byte) value);
        return this;
    }

    @Override
    public ByteBufferData put(final int index, final int value) {
        if (value < 0 || value >= 256) {
            throw new IllegalArgumentException("Invalid value passed to byte buffer: " + value);
        }
        _buffer.put(index, (byte) value);
        return this;
    }

    @Override
    public void put(final IndexBufferData<?> buf) {
        if (buf instanceof ByteBufferData) {
            _buffer.put((ByteBuffer) buf.getBuffer());
        } else {
            while (buf.getBuffer().hasRemaining()) {
                put(buf.get());
            }
        }
    }

    @Override
    public void put(final int[] array) {
        for (int i = 0; i < array.length; i++) {
            put(array[i]);
        }
    }

    @Override
    public void put(final int[] array, final int offset, final int length) {
        for (int i = offset, max = offset + length; i < max; i++) {
            put(array[i]);
        }
    }

    @Override
    public int getByteCount() {
        return 1;
    }

    @Override
    public ByteBuffer getBuffer() {
        return _buffer;
    }

    @Override
    public IntBuffer asIntBuffer() {
        final ByteBuffer source = getBuffer().duplicate();
        source.rewind();
        final IntBuffer buff = BufferUtils.createIntBufferOnHeap(source.limit());
        for (int i = 0, max = source.limit(); i < max; i++) {
            buff.put(source.get() & 0xFF);
        }
        buff.flip();
        return buff;
    }

    @Override
    public ByteBufferData makeCopy() {
        final ByteBufferData copy = new ByteBufferData();
        copy._buffer = BufferUtils.clone(_buffer);
        copy._vboAccessMode = _vboAccessMode;
        return copy;
    }
}