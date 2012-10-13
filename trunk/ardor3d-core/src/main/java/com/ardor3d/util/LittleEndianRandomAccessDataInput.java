/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.charset.Charset;

import com.ardor3d.util.export.ByteUtils;

/**
 * Utility class useful for reading little-endian stored data in a random access fashion. All functions work as defined
 * in DataInput, but assume they come from a LittleEndian input stream.
 * 
 * <p>
 * Note: random access is implemented by reading the entire stream into memory.
 * </p>
 */
public class LittleEndianRandomAccessDataInput implements DataInput {
    private final ByteBuffer _contents;

    /**
     * Number of bytes to read when reading a char... For data meant to be read from C/C++ this is often 1, for Java and
     * C# this is usually 2.
     */
    public int CHAR_SIZE = 2;

    /**
     * Creates a new LittleEndian reader from the given input stream. Note that this stream is loaded completely into
     * memory.
     * 
     * @param in
     *            The stream to read from.
     */
    public LittleEndianRandomAccessDataInput(final InputStream in) throws IOException {
        _contents = ByteBuffer.wrap(ByteUtils.getByteContent(in));
    }

    /**
     * Creates a new LittleEndian reader from the given byte buffer. Note that this byte buffer is not cloned or copied,
     * so take care not to alter it during read. This constructor is useful for working with memory-mapped files.
     * 
     * @param contents
     *            The contents to read from.
     */
    public LittleEndianRandomAccessDataInput(final ByteBuffer contents) throws IOException {
        _contents = contents;
    }

    public final int readUnsignedShort() throws IOException {
        return (readByte() & 0xff) | ((readByte() & 0xff) << 8);
    }

    public final long readUnsignedInt() throws IOException {
        return ((readByte() & 0xff) | ((readByte() & 0xff) << 8) | ((readByte() & 0xff) << 16) | (((long) (readByte() & 0xff)) << 24));
    }

    public final boolean readBoolean() throws IOException {
        return (readByte() != 0);
    }

    public final byte readByte() throws IOException {
        return _contents.get();
    }

    public final int readUnsignedByte() throws IOException {
        return readByte() & 0xff;
    }

    public final short readShort() throws IOException {
        return (short) readUnsignedShort();
    }

    public final char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    public final int readInt() throws IOException {
        return ((readByte() & 0xff) | ((readByte() & 0xff) << 8) | ((readByte() & 0xff) << 16) | ((readByte() & 0xff) << 24));
    }

    public final long readLong() throws IOException {
        return ((readByte() & 0xff) | ((long) (readByte() & 0xff) << 8) | ((long) (readByte() & 0xff) << 16)
                | ((long) (readByte() & 0xff) << 24) | ((long) (readByte() & 0xff) << 32)
                | ((long) (readByte() & 0xff) << 40) | ((long) (readByte() & 0xff) << 48) | ((long) (readByte() & 0xff) << 56));
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final void readFully(final byte b[]) throws IOException {
        readFully(b, 0, b.length);
    }

    public final void readFully(final byte b[], final int off, final int len) throws IOException {
        if (len - off + _contents.position() > _contents.capacity()) {
            throw new EOFException("EOF reached");
        } else {
            _contents.get(b, off, len);
        }
    }

    public final int skipBytes(final int n) throws IOException {
        if (_contents.remaining() >= n) {
            _contents.position(_contents.position() + n);
            return n;
        }
        final int skipped = _contents.remaining();
        _contents.position(_contents.limit());
        return skipped;
    }

    /**
     * Sets a mark at the current position in the underlying buffer. This position can be returned to by calling reset.
     * 
     * @return this object
     */
    public final LittleEndianRandomAccessDataInput mark() {
        _contents.mark();
        return this;
    }

    /**
     * Seeks to the position of the last mark. The mark is not changed or discarded.
     * 
     * @return this object
     * @throws InvalidMarkException
     *             if mark was not previously called.
     */
    public final LittleEndianRandomAccessDataInput reset() {
        _contents.reset();
        return this;
    }

    /**
     * Unsupported.
     * 
     * @throws IOException
     *             if this method is called.
     */
    public final String readLine() throws IOException {
        throw new IOException("operation unsupported.");
    }

    /**
     * Unsupported.
     * 
     * @throws IOException
     *             if this method is called.
     */
    public final String readUTF() throws IOException {
        throw new IOException("operation unsupported.");
    }

    /**
     * Reads a specified number of bytes to form a string. The length of the string (number of characters) is required
     * to notify when reading should stop. The index is increased the number of characters read.
     * 
     * @param size
     *            the length of the string to read.
     * @param charset
     *            the charset used to convert the bytes to a string.
     * @return the string read.
     * @throws IOException
     *             if EOS/EOF is reached before "size" number of bytes are read.
     */
    public String readString(final int size, final Charset charset) throws IOException {
        final int start = position();
        final byte[] content = new byte[size];
        readFully(content);
        seek(start + size);
        int indexOfNullByte = size;
        // Look for zero terminated string from byte array
        for (int i = 0; i < size; i++) {
            if (content[i] == 0) {
                indexOfNullByte = i;
                break;
            }
        }
        final String s = new String(content, 0, indexOfNullByte, charset);
        return s;
    }

    /**
     * Reads a specified number of bytes to form a string. The length of the string (number of characters) is required
     * to notify when reading should stop. The index is increased the number of characters read. Will use the platform's
     * default Charset to convert the bytes to string.
     * 
     * @param size
     *            the length of the string to read.
     * @return the string read.
     * @throws IOException
     *             if EOS/EOF is reached before "size" number of bytes are read.
     */
    public String readString(final int size) throws IOException {
        return readString(size, Charset.defaultCharset());
    }

    public final void seek(final int pos) throws IOException {
        _contents.position(pos);
    }

    public int position() {
        return _contents.position();
    }

    public int capacity() {
        return _contents.capacity();
    }
}