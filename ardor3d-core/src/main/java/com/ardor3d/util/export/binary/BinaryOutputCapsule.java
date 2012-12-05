/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ardor3d.util.export.ByteUtils;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public class BinaryOutputCapsule implements OutputCapsule {

    public static final int NULL_OBJECT = -1;
    public static final int DEFAULT_OBJECT = -2;

    public static byte[] NULL_BYTES = new byte[] { (byte) -1 };
    public static byte[] DEFAULT_BYTES = new byte[] { (byte) -2 };

    protected ByteArrayOutputStream _baos;
    protected byte[] _bytes;
    protected BinaryExporter _exporter;
    protected BinaryClassObject _cObj;
    protected boolean _forceDirectNioBuffers;

    public BinaryOutputCapsule(final BinaryExporter exporter, final BinaryClassObject bco) {
        this(exporter, bco, false);
    }

    public BinaryOutputCapsule(final BinaryExporter exporter, final BinaryClassObject bco,
            final boolean forceDirectNioBuffers) {
        _baos = new ByteArrayOutputStream();
        _exporter = exporter;
        _cObj = bco;
        _forceDirectNioBuffers = forceDirectNioBuffers;
    }

    public void write(final byte value, final String name, final byte defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.BYTE);
        write(value);
    }

    public void write(final byte[] value, final String name, final byte[] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.BYTE_1D);
        write(value);
    }

    public void write(final byte[][] value, final String name, final byte[][] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.BYTE_2D);
        write(value);
    }

    public void write(final int value, final String name, final int defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.INT);
        write(value);
    }

    public void write(final int[] value, final String name, final int[] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.INT_1D);
        write(value);
    }

    public void write(final int[][] value, final String name, final int[][] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.INT_2D);
        write(value);
    }

    public void write(final float value, final String name, final float defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.FLOAT);
        write(value);
    }

    public void write(final float[] value, final String name, final float[] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.FLOAT_1D);
        write(value);
    }

    public void write(final float[][] value, final String name, final float[][] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.FLOAT_2D);
        write(value);
    }

    public void write(final double value, final String name, final double defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.DOUBLE);
        write(value);
    }

    public void write(final double[] value, final String name, final double[] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.DOUBLE_1D);
        write(value);
    }

    public void write(final double[][] value, final String name, final double[][] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.DOUBLE_2D);
        write(value);
    }

    public void write(final long value, final String name, final long defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.LONG);
        write(value);
    }

    public void write(final long[] value, final String name, final long[] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.LONG_1D);
        write(value);
    }

    public void write(final long[][] value, final String name, final long[][] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.LONG_2D);
        write(value);
    }

    public void write(final short value, final String name, final short defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SHORT);
        write(value);
    }

    public void write(final short[] value, final String name, final short[] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SHORT_1D);
        write(value);
    }

    public void write(final short[][] value, final String name, final short[][] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SHORT_2D);
        write(value);
    }

    public void write(final boolean value, final String name, final boolean defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.BOOLEAN);
        write(value);
    }

    public void write(final boolean[] value, final String name, final boolean[] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.BOOLEAN_1D);
        write(value);
    }

    public void write(final boolean[][] value, final String name, final boolean[][] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.BOOLEAN_2D);
        write(value);
    }

    public void write(final String value, final String name, final String defVal) throws IOException {
        if (value == null ? defVal == null : value.equals(defVal)) {
            return;
        }
        writeAlias(name, BinaryClassField.STRING);
        write(value);
    }

    public void write(final String[] value, final String name, final String[] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.STRING_1D);
        write(value);
    }

    public void write(final String[][] value, final String name, final String[][] defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.STRING_2D);
        write(value);
    }

    public void write(final BitSet value, final String name, final BitSet defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.BITSET);
        write(value);
    }

    public void write(final Savable object, final String name, final Savable defVal) throws IOException {
        if (object == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SAVABLE);
        write(object);
    }

    public void write(final Savable[] objects, final String name, final Savable[] defVal) throws IOException {
        if (objects == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SAVABLE_1D);
        write(objects);
    }

    public void write(final Savable[][] objects, final String name, final Savable[][] defVal) throws IOException {
        if (objects == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SAVABLE_2D);
        write(objects);
    }

    public void write(final FloatBuffer value, final String name, final FloatBuffer defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.FLOATBUFFER);
        write(value);
    }

    public void write(final IntBuffer value, final String name, final IntBuffer defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.INTBUFFER);
        write(value);
    }

    public void write(final ByteBuffer value, final String name, final ByteBuffer defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.BYTEBUFFER);
        write(value);
    }

    public void write(final ShortBuffer value, final String name, final ShortBuffer defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SHORTBUFFER);
        write(value);
    }

    public void writeFloatBufferList(final List<FloatBuffer> array, final String name, final List<FloatBuffer> defVal)
            throws IOException {
        if (array == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.FLOATBUFFER_ARRAYLIST);
        writeFloatBufferArrayList(array);
    }

    public void writeByteBufferList(final List<ByteBuffer> array, final String name, final List<ByteBuffer> defVal)
            throws IOException {
        if (array == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.BYTEBUFFER_ARRAYLIST);
        writeByteBufferArrayList(array);
    }

    public void writeSavableList(final List<? extends Savable> array, final String name,
            final List<? extends Savable> defVal) throws IOException {
        if (array == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SAVABLE_ARRAYLIST);
        writeSavableArrayList(array);
    }

    public void writeSavableListArray(final List<? extends Savable>[] array, final String name,
            final List<? extends Savable>[] defVal) throws IOException {
        if (array == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SAVABLE_ARRAYLIST_1D);
        writeSavableArrayListArray(array);
    }

    public void writeSavableListArray2D(final List<? extends Savable>[][] array, final String name,
            final List<? extends Savable>[][] defVal) throws IOException {
        if (array == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SAVABLE_ARRAYLIST_2D);
        writeSavableArrayListArray2D(array);
    }

    public void writeSavableMap(final Map<? extends Savable, ? extends Savable> map, final String name,
            final Map<? extends Savable, ? extends Savable> defVal) throws IOException {
        if (map == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.SAVABLE_MAP);
        writeSavableMap(map);
    }

    public void writeStringSavableMap(final Map<String, ? extends Savable> map, final String name,
            final Map<String, ? extends Savable> defVal) throws IOException {
        if (map == defVal) {
            return;
        }
        writeAlias(name, BinaryClassField.STRING_SAVABLE_MAP);
        writeStringSavableMap(map);
    }

    protected void writeAlias(final String name, final byte fieldType) throws IOException {
        if (_cObj._nameFields.get(name) == null) {
            generateAlias(name, fieldType);
        }

        final byte alias = _cObj._nameFields.get(name)._alias;
        write(alias);
    }

    // XXX: The generation of aliases is limited to 256 possible values.
    // If we run into classes with more than 256 fields, we need to expand this.
    // But I mean, come on...
    protected void generateAlias(final String name, final byte type) {
        final byte alias = (byte) _cObj._nameFields.size();
        _cObj._nameFields.put(name, new BinaryClassField(name, alias, type));
    }

    @Override
    public boolean equals(final Object arg0) {
        if (!(arg0 instanceof BinaryOutputCapsule)) {
            return false;
        }

        final byte[] other = ((BinaryOutputCapsule) arg0)._bytes;
        if (_bytes.length != other.length) {
            return false;
        }
        return Arrays.equals(_bytes, other);
    }

    public void finish() {
        // renamed to finish as 'finalize' in java.lang.Object should not be
        // overridden like this
        // - finalize should not be called directly but is called by garbage
        // collection!!!
        _bytes = _baos.toByteArray();
        _baos = null;
    }

    // byte primitive

    protected void write(final byte value) throws IOException {
        _baos.write(value);
    }

    protected void write(final byte[] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        _baos.write(value);
    }

    protected void write(final byte[][] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    // int primitive

    protected void write(final int value) throws IOException {
        _baos.write(deflate(ByteUtils.convertToBytes(value)));
    }

    protected void write(final int[] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    protected void write(final int[][] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    // float primitive

    protected void write(final float value) throws IOException {
        _baos.write(ByteUtils.convertToBytes(value));
    }

    protected void write(final float[] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    protected void write(final float[][] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    // double primitive

    protected void write(final double value) throws IOException {
        _baos.write(ByteUtils.convertToBytes(value));
    }

    protected void write(final double[] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    protected void write(final double[][] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    // long primitive

    protected void write(final long value) throws IOException {
        _baos.write(deflate(ByteUtils.convertToBytes(value)));
    }

    protected void write(final long[] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    protected void write(final long[][] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    // short primitive

    protected void write(final short value) throws IOException {
        _baos.write(ByteUtils.convertToBytes(value));
    }

    protected void write(final short[] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    protected void write(final short[][] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    // boolean primitive

    protected void write(final boolean value) throws IOException {
        _baos.write(ByteUtils.convertToBytes(value));
    }

    protected void write(final boolean[] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    protected void write(final boolean[][] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    // String

    protected void write(final String value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        // write our output as UTF-8. Java misspells UTF-8 as UTF8 for official use in java.lang
        final byte[] bytes = value.getBytes("UTF8");
        write(bytes.length);
        _baos.write(bytes);
    }

    protected void write(final String[] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    protected void write(final String[][] value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.length);
        for (int x = 0; x < value.length; x++) {
            write(value[x]);
        }
    }

    // BitSet

    protected void write(final BitSet value) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
            return;
        }
        write(value.size());
        // TODO: MAKE THIS SMALLER
        for (int x = 0, max = value.size(); x < max; x++) {
            write(value.get(x));
        }
    }

    // DEFLATOR for int and long

    protected static byte[] deflate(final byte[] bytes) {
        int size = bytes.length;
        if (size == 4) {
            final int possibleMagic = ByteUtils.convertIntFromBytes(bytes);
            if (possibleMagic == NULL_OBJECT) {
                return NULL_BYTES;
            } else if (possibleMagic == DEFAULT_OBJECT) {
                return DEFAULT_BYTES;
            }
        }
        for (int x = 0; x < bytes.length; x++) {
            if (bytes[x] != 0) {
                break;
            }
            size--;
        }
        if (size == 0) {
            return new byte[1];
        }

        final byte[] rVal = new byte[1 + size];
        rVal[0] = (byte) size;
        for (int x = 1; x < rVal.length; x++) {
            rVal[x] = bytes[bytes.length - size - 1 + x];
        }

        return rVal;
    }

    // BinarySavable

    protected void write(final Savable object) throws IOException {
        if (object == null) {
            write(NULL_OBJECT);
            return;
        }
        final int id = _exporter.processBinarySavable(object);
        write(id);
    }

    // BinarySavable array

    protected void write(final Savable[] objects) throws IOException {
        if (objects == null) {
            write(NULL_OBJECT);
            return;
        }
        write(objects.length);
        for (int x = 0; x < objects.length; x++) {
            write(objects[x]);
        }
    }

    protected void write(final Savable[][] objects) throws IOException {
        if (objects == null) {
            write(NULL_OBJECT);
            return;
        }
        write(objects.length);
        for (int x = 0; x < objects.length; x++) {
            write(objects[x]);
        }
    }

    // List<BinarySavable>

    protected void writeSavableArrayList(final List<? extends Savable> array) throws IOException {
        if (array == null) {
            write(NULL_OBJECT);
            return;
        }
        write(array.size());
        for (final Savable bs : array) {
            write(bs);
        }
    }

    protected void writeSavableArrayListArray(final List<? extends Savable>[] array) throws IOException {
        if (array == null) {
            write(NULL_OBJECT);
            return;
        }
        write(array.length);
        for (final List<? extends Savable> bs : array) {
            writeSavableArrayList(bs);
        }
    }

    protected void writeSavableArrayListArray2D(final List<? extends Savable>[][] array) throws IOException {
        if (array == null) {
            write(NULL_OBJECT);
            return;
        }
        write(array.length);
        for (final List<? extends Savable>[] bs : array) {
            writeSavableArrayListArray(bs);
        }
    }

    // Map<BinarySavable, BinarySavable>

    protected void writeSavableMap(final Map<? extends Savable, ? extends Savable> array) throws IOException {
        if (array == null) {
            write(NULL_OBJECT);
            return;
        }
        write(array.size());
        for (final Entry<? extends Savable, ? extends Savable> entry : array.entrySet()) {
            write(new Savable[] { entry.getKey(), entry.getValue() });
        }
    }

    protected void writeStringSavableMap(final Map<String, ? extends Savable> array) throws IOException {
        if (array == null) {
            write(NULL_OBJECT);
            return;
        }
        write(array.size());

        // write String array for keys
        final String[] keys = array.keySet().toArray(new String[array.keySet().size()]);
        write(keys);

        // write Savable array for values
        final Savable[] values = array.values().toArray(new Savable[array.values().size()]);
        write(values);
    }

    // List<FloatBuffer>

    protected void writeFloatBufferArrayList(final List<FloatBuffer> array) throws IOException {
        if (array == null) {
            write(NULL_OBJECT);
            return;
        }
        write(array.size());
        for (final FloatBuffer buf : array) {
            write(buf);
        }
    }

    // List<FloatBuffer>

    protected void writeByteBufferArrayList(final List<ByteBuffer> array) throws IOException {
        if (array == null) {
            write(NULL_OBJECT);
            return;
        }
        write(array.size());
        for (final ByteBuffer buf : array) {
            write(buf);
        }
    }

    // NIO BUFFERS

    // float buffer
    protected void write(final FloatBuffer source) throws IOException {
        if (source == null) {
            write(NULL_OBJECT);
            return;
        }

        final int sizeof = 4;

        // write length
        final int length = source.limit();
        write(length);

        // write boolean for directness
        write(_forceDirectNioBuffers || source.isDirect());

        final byte[] array = new byte[length * sizeof];
        if (source.hasArray()) {
            // get the backing array of the source buffer
            final float[] backingArray = source.array();

            // create a tiny store only to perform the conversion into little endian
            final ByteBuffer buf = ByteBuffer.allocate(sizeof).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < backingArray.length; i++) {
                buf.putFloat(backingArray[i]).rewind();
                buf.get(array, i * sizeof, sizeof).rewind();
            }
        } else {
            // duplicate buffer to allow modification of limit/position without changing original.
            final FloatBuffer value = source.duplicate();

            // create little endian store
            final ByteBuffer buf = ByteBuffer.allocate(array.length).order(ByteOrder.LITTLE_ENDIAN);

            // place buffer into store.
            value.rewind();
            buf.asFloatBuffer().put(value);
            buf.rewind();

            // Pull out store as array
            buf.get(array);
        }

        // write to stream
        _baos.write(array);
    }

    // int buffer
    protected void write(final IntBuffer source) throws IOException {
        if (source == null) {
            write(NULL_OBJECT);
            return;
        }

        final int sizeof = 4;

        // write length
        final int length = source.limit();
        write(length);

        // write boolean for directness
        write(_forceDirectNioBuffers || source.isDirect());

        final byte[] array = new byte[length * sizeof];
        if (source.hasArray()) {
            // get the backing array of the source buffer
            final int[] backingArray = source.array();

            // create a tiny store only to perform the conversion into little endian
            final ByteBuffer buf = ByteBuffer.allocate(sizeof).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < backingArray.length; i++) {
                buf.putInt(backingArray[i]).rewind();
                buf.get(array, i * sizeof, sizeof).rewind();
            }
        } else {
            // duplicate buffer to allow modification of limit/position without changing original.
            final IntBuffer value = source.duplicate();

            // create little endian store
            final ByteBuffer buf = ByteBuffer.allocate(array.length).order(ByteOrder.LITTLE_ENDIAN);

            // place buffer into store. Rewind buffers
            value.rewind();
            buf.asIntBuffer().put(value);
            buf.rewind();

            // Pull out store as array
            buf.get(array);
        }

        // write to stream
        _baos.write(array);
    }

    // short buffer
    protected void write(final ShortBuffer source) throws IOException {
        if (source == null) {
            write(NULL_OBJECT);
            return;
        }

        final int sizeof = 2;

        // write length
        final int length = source.limit();
        write(length);

        // write boolean for directness
        write(_forceDirectNioBuffers || source.isDirect());

        final byte[] array = new byte[length * sizeof];
        if (source.hasArray()) {
            // get the backing array of the source buffer
            final short[] backingArray = source.array();

            // create a tiny store only to perform the conversion into little endian
            final ByteBuffer buf = ByteBuffer.allocate(sizeof).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < backingArray.length; i++) {
                buf.putShort(backingArray[i]).rewind();
                buf.get(array, i * sizeof, sizeof).rewind();
            }
        } else {
            // duplicate buffer to allow modification of limit/position without changing original.
            final ShortBuffer value = source.duplicate();

            // create little endian store
            final ByteBuffer buf = ByteBuffer.allocate(array.length).order(ByteOrder.LITTLE_ENDIAN);

            // place buffer into store. Rewind buffers
            value.rewind();
            buf.asShortBuffer().put(value);
            buf.rewind();

            // Pull out store as array
            buf.get(array);
        }

        // write to stream
        _baos.write(array);
    }

    // byte buffer
    protected void write(final ByteBuffer source) throws IOException {
        if (source == null) {
            write(NULL_OBJECT);
            return;
        }

        // write length
        final int length = source.limit();
        write(length);

        // write boolean for directness
        write(_forceDirectNioBuffers || source.isDirect());

        final byte[] array;
        if (source.hasArray()) {
            array = source.array();
        } else {
            // duplicate buffer to allow modification of limit/position without changing original.
            final ByteBuffer value = source.duplicate();

            // Pull out value as array
            array = new byte[length];
            value.rewind();
            value.get(array);
        }

        // write to stream
        _baos.write(array);
    }

    public void write(final Enum<?> value, final String name, final Enum<?> defVal) throws IOException {
        if (value == defVal) {
            return;
        }
        if (value == null) {
            write(NULL_OBJECT);
        } else {
            write(value.name(), name, null);
        }
    }

    public void write(final Enum<?>[] value, final String name) throws IOException {
        if (value == null) {
            write(NULL_OBJECT);
        } else {
            final String[] toWrite = new String[value.length];
            int i = 0;
            for (final Enum<?> val : value) {
                toWrite[i++] = val.name();
            }
            write(toWrite, name, null);
        }
    }
}
