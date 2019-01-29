/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
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

public interface OutputCapsule {

    // byte primitive

    public void write(byte value, String name, byte defVal) throws IOException;

    public void write(byte[] value, String name, byte[] defVal) throws IOException;

    public void write(byte[][] value, String name, byte[][] defVal) throws IOException;

    // int primitive

    public void write(int value, String name, int defVal) throws IOException;

    public void write(int[] value, String name, int[] defVal) throws IOException;

    public void write(int[][] value, String name, int[][] defVal) throws IOException;

    // float primitive

    public void write(float value, String name, float defVal) throws IOException;

    public void write(float[] value, String name, float[] defVal) throws IOException;

    public void write(float[][] value, String name, float[][] defVal) throws IOException;

    // double primitive

    public void write(double value, String name, double defVal) throws IOException;

    public void write(double[] value, String name, double[] defVal) throws IOException;

    public void write(double[][] value, String name, double[][] defVal) throws IOException;

    // long primitive

    public void write(long value, String name, long defVal) throws IOException;

    public void write(long[] value, String name, long[] defVal) throws IOException;

    public void write(long[][] value, String name, long[][] defVal) throws IOException;

    // short primitive

    public void write(short value, String name, short defVal) throws IOException;

    public void write(short[] value, String name, short[] defVal) throws IOException;

    public void write(short[][] value, String name, short[][] defVal) throws IOException;

    // boolean primitive

    public void write(boolean value, String name, boolean defVal) throws IOException;

    public void write(boolean[] value, String name, boolean[] defVal) throws IOException;

    public void write(boolean[][] value, String name, boolean[][] defVal) throws IOException;

    // String

    public void write(String value, String name, String defVal) throws IOException;

    public void write(String[] value, String name, String[] defVal) throws IOException;

    public void write(String[][] value, String name, String[][] defVal) throws IOException;

    // BitSet

    public void write(BitSet value, String name, BitSet defVal) throws IOException;

    // BinarySavable

    public void write(Savable object, String name, Savable defVal) throws IOException;

    public void write(Savable[] objects, String name, Savable[] defVal) throws IOException;

    public void write(Savable[][] objects, String name, Savable[][] defVal) throws IOException;

    // Lists

    public void writeSavableList(List<? extends Savable> array, String name, List<? extends Savable> defVal)
            throws IOException;

    public void writeSavableListArray(List<? extends Savable>[] array, String name, List<? extends Savable>[] defVal)
            throws IOException;

    public void writeSavableListArray2D(List<? extends Savable>[][] array, String name,
            List<? extends Savable>[][] defVal) throws IOException;

    public void writeFloatBufferList(List<FloatBuffer> array, String name, List<FloatBuffer> defVal) throws IOException;

    public void writeByteBufferList(List<ByteBuffer> array, String name, List<ByteBuffer> defVal) throws IOException;

    // Maps

    public void writeSavableMap(Map<? extends Savable, ? extends Savable> map, String name,
            Map<? extends Savable, ? extends Savable> defVal) throws IOException;

    public void writeStringSavableMap(Map<String, ? extends Savable> map, String name,
            Map<String, ? extends Savable> defVal) throws IOException;

    public void writeStringObjectMap(Map<String, Object> map, String name, Map<String, Object> defVal)
            throws IOException;

    // NIO BUFFERS
    // float buffer

    public void write(FloatBuffer value, String name, FloatBuffer defVal) throws IOException;

    // int buffer

    public void write(IntBuffer value, String name, IntBuffer defVal) throws IOException;

    // byte buffer

    public void write(ByteBuffer value, String name, ByteBuffer defVal) throws IOException;

    // short buffer

    public void write(ShortBuffer value, String name, ShortBuffer defVal) throws IOException;

    // enums

    public void write(Enum<?> value, String name, Enum<?> defVal) throws IOException;

    public void write(Enum<?>[] value, String name) throws IOException;
}
