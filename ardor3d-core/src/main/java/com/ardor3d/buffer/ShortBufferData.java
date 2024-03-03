/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.buffer;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Simple data class storing a buffer of shorts
 */
public class ShortBufferData extends IndexBufferData<ShortBuffer> implements Savable {

  /**
   * Instantiates a new ShortBufferData.
   */
  public ShortBufferData() {}

  /**
   * Instantiates a new ShortBufferData with a buffer of the given size.
   */
  public ShortBufferData(final int size) {
    this(BufferUtils.createShortBuffer(size));
  }

  /**
   * Creates a new ShortBufferData.
   *
   * @param buffer
   *          Buffer holding the data. Must not be null.
   */
  public ShortBufferData(final ShortBuffer buffer) {
    if (buffer == null) {
      throw new IllegalArgumentException("Buffer can not be null!");
    }

    _buffer = buffer;
  }

  @Override
  public Class<? extends ShortBufferData> getClassTag() { return getClass(); }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _buffer = capsule.readShortBuffer("buffer", null);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_buffer, "buffer", null);
  }

  @Override
  public int get() {
    return _buffer.get() & 0xFFFF;
  }

  @Override
  public int get(final int index) {
    return _buffer.get(index) & 0xFFFF;
  }

  @Override
  public ShortBufferData put(final int value) {
    if (value < 0 || value >= 65536) {
      throw new IllegalArgumentException("Invalid value passed to short buffer: " + value);
    }
    _buffer.put((short) value);
    return this;
  }

  @Override
  public ShortBufferData put(final int index, final int value) {
    if (value < 0 || value >= 65536) {
      throw new IllegalArgumentException("Invalid value passed to short buffer: " + value);
    }
    _buffer.put(index, (short) value);
    return this;
  }

  @Override
  public ShortBufferData put(final IndexBufferData<?> buf) {
    if (buf instanceof ShortBufferData) {
      _buffer.put((ShortBuffer) buf.getBuffer());
    } else {
      while (buf.getBuffer().hasRemaining()) {
        put(buf.get());
      }
    }
    return this;
  }

  @Override
  public ShortBufferData put(final byte[] array) {
    for (int i = 0; i < array.length; i++) {
      put(array[i]);
    }
    return this;
  }

  @Override
  public ShortBufferData put(final byte[] array, final int offset, final int length) {
    for (int i = offset, max = offset + length; i < max; i++) {
      put(array[i]);
    }
    return this;
  }

  @Override
  public ShortBufferData put(final short[] array) {
    _buffer.put(array);
    return this;
  }

  @Override
  public ShortBufferData put(final short[] array, final int offset, final int length) {
    _buffer.put(array, offset, length);
    return this;
  }

  @Override
  public ShortBufferData put(final int[] array) {
    for (int i = 0; i < array.length; i++) {
      put(array[i]);
    }
    return this;
  }

  @Override
  public ShortBufferData put(final int[] array, final int offset, final int length) {
    for (int i = offset, max = offset + length; i < max; i++) {
      put(array[i]);
    }
    return this;
  }

  @Override
  public ShortBufferData put(final IntBuffer buffer) {
    while (buffer.hasRemaining()) {
      put(buffer.get());
    }
    return this;
  }

  @Override
  public int getByteCount() { return 2; }

  @Override
  public ShortBuffer getBuffer() { return _buffer; }

  @Override
  public IntBuffer asIntBuffer() {
    final ShortBuffer source = getBuffer().duplicate();
    source.rewind();
    final IntBuffer buff = BufferUtils.createIntBufferOnHeap(source.limit());
    for (int i = 0, max = source.limit(); i < max; i++) {
      buff.put(source.get() & 0xFFFF);
    }
    buff.flip();
    return buff;
  }

  @Override
  public ShortBufferData makeCopy() {
    final ShortBufferData copy = new ShortBufferData();
    copy._buffer = BufferUtils.clone(_buffer);
    copy._vboAccessMode = _vboAccessMode;
    return copy;
  }
}
