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

import java.nio.Buffer;
import java.nio.IntBuffer;

public abstract class IndexBufferData<T extends Buffer> extends AbstractBufferData<T> {

    /**
     * @return the next value from this object, as an int, incrementing our position by 1 entry. Buffer types smaller
     *         than an int should return unsigned values.
     */
    public abstract int get();

    /**
     * @param index
     *            the absolute position to get our value from. This is in entries, not bytes, and is 0 based. So for a
     *            ShortBuffer, 2 would be the 3rd short from the beginning, etc.
     * @return the value from this object, as an int, at the given absolute entry position. Buffer types smaller than an
     *         int should return unsigned values.
     */
    public abstract int get(int index);

    /**
     * @return a new, non-direct IntBuffer containing a snapshot of the contents of this buffer.
     */
    public abstract IntBuffer asIntBuffer();

    /**
     * Sets the value of this buffer at the current position, incrementing our position by 1 entry.
     * 
     * @param value
     *            the value to place into this object at the current position.
     * @return this object, for chaining.
     */
    public abstract IndexBufferData<T> put(int value);

    /**
     * Sets the value of this buffer at the given index.
     * 
     * @param index
     *            the absolute position to put our value at. This is in entries, not bytes, and is 0 based. So for a
     *            ShortBuffer, 2 would be the 3rd short from the beginning, etc.
     * @param value
     *            the value to place into this object
     * @return
     */
    public abstract IndexBufferData<T> put(int index, int value);

    /**
     * Write the contents of the given IndexBufferData into this one. Note that data conversion is handled using the
     * get/put methods in IndexBufferData.
     * 
     * @param buf
     *            the source buffer object.
     */
    public abstract void put(IndexBufferData<?> buf);

    /**
     * Get the underlying nio buffer.
     */
    @Override
    public abstract T getBuffer();

    /**
     * @see Buffer#remaining();
     */
    public int remaining() {
        return getBuffer().remaining();
    }

    /**
     * @see Buffer#position();
     */
    public int position() {
        return getBuffer().position();
    }

    /**
     * @see Buffer#position(int);
     */
    public void position(final int position) {
        getBuffer().position(position);
    }

    /**
     * @see Buffer#limit();
     */
    public int limit() {
        return getBuffer().limit();
    }

    /**
     * @see Buffer#limit(int);
     */
    public void limit(final int limit) {
        getBuffer().limit(limit);
    }

    /**
     * @see Buffer#capacity();
     */
    public int capacity() {
        return getBuffer().capacity();
    }

    /**
     * @see Buffer#rewind();
     */
    public void rewind() {
        getBuffer().rewind();
    }

    /**
     * @see Buffer#reset();
     */
    public void reset() {
        getBuffer().reset();
    }

    @Override
    public abstract IndexBufferData<T> makeCopy();
}
