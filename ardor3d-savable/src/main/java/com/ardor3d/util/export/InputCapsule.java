/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
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

    public byte readByte(String name, byte defVal) throws IOException;

    public byte[] readByteArray(String name, byte[] defVal) throws IOException;

    public byte[][] readByteArray2D(String name, byte[][] defVal) throws IOException;

    // int primitive

    public int readInt(String name, int defVal) throws IOException;

    public int[] readIntArray(String name, int[] defVal) throws IOException;

    public int[][] readIntArray2D(String name, int[][] defVal) throws IOException;

    // float primitive

    public float readFloat(String name, float defVal) throws IOException;

    public float[] readFloatArray(String name, float[] defVal) throws IOException;

    public float[][] readFloatArray2D(String name, float[][] defVal) throws IOException;

    // double primitive

    public double readDouble(String name, double defVal) throws IOException;

    public double[] readDoubleArray(String name, double[] defVal) throws IOException;

    public double[][] readDoubleArray2D(String name, double[][] defVal) throws IOException;

    // long primitive

    public long readLong(String name, long defVal) throws IOException;

    public long[] readLongArray(String name, long[] defVal) throws IOException;

    public long[][] readLongArray2D(String name, long[][] defVal) throws IOException;

    // short primitive

    public short readShort(String name, short defVal) throws IOException;

    public short[] readShortArray(String name, short[] defVal) throws IOException;

    public short[][] readShortArray2D(String name, short[][] defVal) throws IOException;

    // boolean primitive

    public boolean readBoolean(String name, boolean defVal) throws IOException;

    public boolean[] readBooleanArray(String name, boolean[] defVal) throws IOException;

    public boolean[][] readBooleanArray2D(String name, boolean[][] defVal) throws IOException;

    // String

    public String readString(String name, String defVal) throws IOException;

    public String[] readStringArray(String name, String[] defVal) throws IOException;

    public String[][] readStringArray2D(String name, String[][] defVal) throws IOException;

    // BitSet

    public BitSet readBitSet(String name, BitSet defVal) throws IOException;

    // BinarySavable

    public <E extends Savable> E readSavable(String name, E defVal) throws IOException;

    public <E extends Savable> E[] readSavableArray(String name, E[] defVal) throws IOException;

    public <E extends Savable> E[][] readSavableArray2D(String name, E[][] defVal) throws IOException;

    // Lists

    public <E extends Savable> List<E> readSavableList(String name, List<E> defVal) throws IOException;

    public <E extends Savable> List<E>[] readSavableListArray(String name, List<E>[] defVal) throws IOException;

    public <E extends Savable> List<E>[][] readSavableListArray2D(String name, List<E>[][] defVal) throws IOException;

    public List<FloatBuffer> readFloatBufferList(String name, List<FloatBuffer> defVal) throws IOException;

    public List<ByteBuffer> readByteBufferList(String name, List<ByteBuffer> defVal) throws IOException;

    // Maps

    public <K extends Savable, V extends Savable> Map<K, V> readSavableMap(String name, Map<K, V> defVal)
            throws IOException;

    public <V extends Savable> Map<String, V> readStringSavableMap(String name, Map<String, V> defVal)
            throws IOException;

    public Map<String, Object> readStringObjectMap(String name, Map<String, Object> defVal) throws IOException;

    // NIO BUFFERS
    // float buffer

    public FloatBuffer readFloatBuffer(String name, FloatBuffer defVal) throws IOException;

    // int buffer

    public IntBuffer readIntBuffer(String name, IntBuffer defVal) throws IOException;

    // byte buffer

    public ByteBuffer readByteBuffer(String name, ByteBuffer defVal) throws IOException;

    // short buffer

    public ShortBuffer readShortBuffer(String name, ShortBuffer defVal) throws IOException;

    // enums

    public <T extends Enum<T>> T readEnum(String name, Class<T> enumType, T defVal) throws IOException;

    public <T extends Enum<T>> T[] readEnumArray(final String name, final Class<T> enumType, final T[] defVal)
            throws IOException;

}
