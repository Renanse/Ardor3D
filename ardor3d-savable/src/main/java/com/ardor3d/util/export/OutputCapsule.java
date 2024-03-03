/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public interface OutputCapsule {

  // byte primitive

  void write(byte value, String name, byte defVal) throws IOException;

  void write(byte[] value, String name, byte[] defVal) throws IOException;

  void write(byte[][] value, String name, byte[][] defVal) throws IOException;

  // int primitive

  void write(int value, String name, int defVal) throws IOException;

  void write(int[] value, String name, int[] defVal) throws IOException;

  void write(int[][] value, String name, int[][] defVal) throws IOException;

  // float primitive

  void write(float value, String name, float defVal) throws IOException;

  void write(float[] value, String name, float[] defVal) throws IOException;

  void write(float[][] value, String name, float[][] defVal) throws IOException;

  // double primitive

  void write(double value, String name, double defVal) throws IOException;

  void write(double[] value, String name, double[] defVal) throws IOException;

  void write(double[][] value, String name, double[][] defVal) throws IOException;

  // long primitive

  void write(long value, String name, long defVal) throws IOException;

  void write(long[] value, String name, long[] defVal) throws IOException;

  void write(long[][] value, String name, long[][] defVal) throws IOException;

  // short primitive

  void write(short value, String name, short defVal) throws IOException;

  void write(short[] value, String name, short[] defVal) throws IOException;

  void write(short[][] value, String name, short[][] defVal) throws IOException;

  // boolean primitive

  void write(boolean value, String name, boolean defVal) throws IOException;

  void write(boolean[] value, String name, boolean[] defVal) throws IOException;

  void write(boolean[][] value, String name, boolean[][] defVal) throws IOException;

  // String

  void write(String value, String name, String defVal) throws IOException;

  void write(String[] value, String name, String[] defVal) throws IOException;

  void write(String[][] value, String name, String[][] defVal) throws IOException;

  // BitSet

  void write(BitSet value, String name, BitSet defVal) throws IOException;

  // BinarySavable

  void write(Savable object, String name, Savable defVal) throws IOException;

  void write(Savable[] objects, String name, Savable[] defVal) throws IOException;

  void write(Savable[][] objects, String name, Savable[][] defVal) throws IOException;

  // Lists

  void writeSavableList(List<? extends Savable> array, String name, List<? extends Savable> defVal) throws IOException;

  void writeSavableListArray(List<? extends Savable>[] array, String name, List<? extends Savable>[] defVal)
      throws IOException;

  void writeSavableListArray2D(List<? extends Savable>[][] array, String name, List<? extends Savable>[][] defVal)
      throws IOException;

  void writeFloatBufferList(List<FloatBuffer> array, String name, List<FloatBuffer> defVal) throws IOException;

  void writeByteBufferList(List<ByteBuffer> array, String name, List<ByteBuffer> defVal) throws IOException;

  // Maps

  void writeSavableMap(Map<? extends Savable, ? extends Savable> map, String name,
      Map<? extends Savable, ? extends Savable> defVal) throws IOException;

  void writeStringSavableMap(Map<String, ? extends Savable> map, String name, Map<String, ? extends Savable> defVal)
      throws IOException;

  void writeStringObjectMap(Map<String, Object> map, String name, Map<String, Object> defVal) throws IOException;

  // NIO BUFFERS
  // float buffer

  void write(FloatBuffer value, String name, FloatBuffer defVal) throws IOException;

  // int buffer

  void write(IntBuffer value, String name, IntBuffer defVal) throws IOException;

  // byte buffer

  void write(ByteBuffer value, String name, ByteBuffer defVal) throws IOException;

  // short buffer

  void write(ShortBuffer value, String name, ShortBuffer defVal) throws IOException;

  // enums

  void write(Enum<?> value, String name, Enum<?> defVal) throws IOException;

  void write(Enum<?>[] value, String name) throws IOException;
}
