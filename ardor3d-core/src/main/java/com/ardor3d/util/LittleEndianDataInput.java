/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * LittleEndianDataInput is a class to read little-endian stored data via a InputStream. All
 * functions work as defined in DataInput, but assume they come from a LittleEndian input stream.
 */
public class LittleEndianDataInput implements DataInput {

  private final BufferedInputStream _stream;

  /**
   * Number of bytes to read when reading a char... For data meant to be read from C/C++ this is often
   * 1, for Java and C# this is usually 2.
   */
  public int CHAR_SIZE = 2;

  /**
   * Creates a new LittleEndian reader from the given input stream. The stream is wrapped in a
   * BufferedInputStream automatically.
   * 
   * @param in
   *          The input stream to read from.
   */
  public LittleEndianDataInput(final InputStream in) {
    _stream = new BufferedInputStream(in);
  }

  @Override
  public final int readUnsignedShort() throws IOException {
    return (_stream.read() & 0xff) | ((_stream.read() & 0xff) << 8);
  }

  /**
   * read an unsigned int as a long
   */
  public final long readUnsignedInt() throws IOException {
    return ((_stream.read() & 0xff) | ((_stream.read() & 0xff) << 8) | ((_stream.read() & 0xff) << 16)
        | (((long) (_stream.read() & 0xff)) << 24));
  }

  @Override
  public final boolean readBoolean() throws IOException {
    return (_stream.read() != 0);
  }

  @Override
  public final byte readByte() throws IOException {
    return (byte) _stream.read();
  }

  @Override
  public final int readUnsignedByte() throws IOException {
    return _stream.read();
  }

  @Override
  public final short readShort() throws IOException {
    return (short) readUnsignedShort();
  }

  @Override
  public final char readChar() throws IOException {
    return (char) readUnsignedShort();
  }

  @Override
  public final int readInt() throws IOException {
    return ((_stream.read() & 0xff) | ((_stream.read() & 0xff) << 8) | ((_stream.read() & 0xff) << 16)
        | ((_stream.read() & 0xff) << 24));
  }

  @Override
  public final long readLong() throws IOException {
    return ((_stream.read() & 0xff) | ((long) (_stream.read() & 0xff) << 8) | ((long) (_stream.read() & 0xff) << 16)
        | ((long) (_stream.read() & 0xff) << 24) | ((long) (_stream.read() & 0xff) << 32)
        | ((long) (_stream.read() & 0xff) << 40) | ((long) (_stream.read() & 0xff) << 48)
        | ((long) (_stream.read() & 0xff) << 56));
  }

  @Override
  public final float readFloat() throws IOException {
    return Float.intBitsToFloat(readInt());
  }

  @Override
  public final double readDouble() throws IOException {
    return Double.longBitsToDouble(readLong());
  }

  @Override
  public final void readFully(final byte[] b) throws IOException {
    readFully(b, 0, b.length);
  }

  @Override
  public final void readFully(final byte[] b, final int off, final int len) throws IOException {
    // this may look over-complicated, but the problem is that the InputStream.read() methods are
    // not guaranteed to fill up the buffer you pass to it. So we need to loop until we have filled
    // up the buffer or until we reach the end of the file.

    final int bytesRead = _stream.read(b, off, len);

    if (bytesRead == -1) {
      throw new EOFException("EOF reached");
    }

    if (bytesRead < len) {
      // we didn't get all the data we wanted, so read some more
      readFully(b, off + bytesRead, len - bytesRead);
    }
  }

  @Override
  public final int skipBytes(final int n) throws IOException {
    return (int) _stream.skip(n);
  }

  @Override
  public final String readLine() throws IOException {
    throw new IOException("Unsupported operation");
  }

  @Override
  public final String readUTF() throws IOException {
    throw new IOException("Unsupported operation");
  }

  public final void close() throws IOException {
    _stream.close();
  }

  public final int available() throws IOException {
    return _stream.available();
  }
}
