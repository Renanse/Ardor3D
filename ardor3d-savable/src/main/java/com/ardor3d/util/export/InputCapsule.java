/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
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

public interface InputCapsule {

  // byte primitive

  byte readByte(String name, byte defVal) throws IOException;

  byte[] readByteArray(String name, byte[] defVal) throws IOException;

  byte[][] readByteArray2D(String name, byte[][] defVal) throws IOException;

  // int primitive

  int readInt(String name, int defVal) throws IOException;

  int[] readIntArray(String name, int[] defVal) throws IOException;

  int[][] readIntArray2D(String name, int[][] defVal) throws IOException;

  // float primitive

  float readFloat(String name, float defVal) throws IOException;

  float[] readFloatArray(String name, float[] defVal) throws IOException;

  float[][] readFloatArray2D(String name, float[][] defVal) throws IOException;

  // double primitive

  double readDouble(String name, double defVal) throws IOException;

  double[] readDoubleArray(String name, double[] defVal) throws IOException;

  double[][] readDoubleArray2D(String name, double[][] defVal) throws IOException;

  // long primitive

  long readLong(String name, long defVal) throws IOException;

  long[] readLongArray(String name, long[] defVal) throws IOException;

  long[][] readLongArray2D(String name, long[][] defVal) throws IOException;

  // short primitive

  short readShort(String name, short defVal) throws IOException;

  short[] readShortArray(String name, short[] defVal) throws IOException;

  short[][] readShortArray2D(String name, short[][] defVal) throws IOException;

  // boolean primitive

  boolean readBoolean(String name, boolean defVal) throws IOException;

  boolean[] readBooleanArray(String name, boolean[] defVal) throws IOException;

  boolean[][] readBooleanArray2D(String name, boolean[][] defVal) throws IOException;

  // String

  String readString(String name, String defVal) throws IOException;

  String[] readStringArray(String name, String[] defVal) throws IOException;

  String[][] readStringArray2D(String name, String[][] defVal) throws IOException;

  // BitSet

  BitSet readBitSet(String name, BitSet defVal) throws IOException;

  // BinarySavable

  <E extends Savable> E readSavable(String name, E defVal) throws IOException;

  <E extends Savable> E[] readSavableArray(String name, E[] defVal) throws IOException;

  <E extends Savable> E[][] readSavableArray2D(String name, E[][] defVal) throws IOException;

  // Lists

  <E extends Savable> List<E> readSavableList(String name, List<E> defVal) throws IOException;

  <E extends Savable> List<E>[] readSavableListArray(String name, List<E>[] defVal) throws IOException;

  <E extends Savable> List<E>[][] readSavableListArray2D(String name, List<E>[][] defVal) throws IOException;

  List<FloatBuffer> readFloatBufferList(String name, List<FloatBuffer> defVal) throws IOException;

  List<ByteBuffer> readByteBufferList(String name, List<ByteBuffer> defVal) throws IOException;

  // Maps

  <K extends Savable, V extends Savable> Map<K, V> readSavableMap(String name, Map<K, V> defVal) throws IOException;

  <V extends Savable> Map<String, V> readStringSavableMap(String name, Map<String, V> defVal) throws IOException;

  Map<String, Object> readStringObjectMap(String name, Map<String, Object> defVal) throws IOException;

  // NIO BUFFERS
  // float buffer

  FloatBuffer readFloatBuffer(String name, FloatBuffer defVal) throws IOException;

  // int buffer

  IntBuffer readIntBuffer(String name, IntBuffer defVal) throws IOException;

  // byte buffer

  ByteBuffer readByteBuffer(String name, ByteBuffer defVal) throws IOException;

  // short buffer

  ShortBuffer readShortBuffer(String name, ShortBuffer defVal) throws IOException;

  // enums

  <T extends Enum<T>> T readEnum(String name, Class<T> enumType, T defVal) throws IOException;

  <T extends Enum<T>> T[] readEnumArray(final String name, final Class<T> enumType, final T[] defVal)
      throws IOException;

}
