/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.export.binary;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.util.export.ByteUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;

public class BinaryInputCapsule implements InputCapsule {
    private static final Logger logger = Logger.getLogger(BinaryInputCapsule.class.getName());

    protected BinaryImporter _importer;
    protected BinaryClassObject _cObj;
    protected HashMap<Byte, Object> _fieldData;

    protected int _index = 0;

    public BinaryInputCapsule(final BinaryImporter importer, final BinaryClassObject bco) {
        _importer = importer;
        _cObj = bco;
    }

    public void setContent(final byte[] content, final int start, final int limit) {
        _fieldData = new HashMap<Byte, Object>();
        for (_index = start; _index < limit;) {
            final byte alias = content[_index];

            _index++;

            try {
                final byte type = _cObj._aliasFields.get(alias)._type;
                final AtomicReference<Object> reference = new AtomicReference<>(null);
                if (!readContentOfType(content, type, reference)) {
                    continue;
                }

                _fieldData.put(alias, reference.get());

            } catch (final IOException e) {
                logger.logp(Level.SEVERE, this.getClass().toString(), "setContent(byte[] content)", "Exception", e);
            }
        }
    }

    public BitSet readBitSet(final String name, final BitSet defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (BitSet) _fieldData.get(field._alias);
    }

    public boolean readBoolean(final String name, final boolean defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return ((Boolean) _fieldData.get(field._alias)).booleanValue();
    }

    public boolean[] readBooleanArray(final String name, final boolean[] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (boolean[]) _fieldData.get(field._alias);
    }

    public boolean[][] readBooleanArray2D(final String name, final boolean[][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (boolean[][]) _fieldData.get(field._alias);
    }

    public byte readByte(final String name, final byte defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return ((Byte) _fieldData.get(field._alias)).byteValue();
    }

    public byte[] readByteArray(final String name, final byte[] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (byte[]) _fieldData.get(field._alias);
    }

    public byte[][] readByteArray2D(final String name, final byte[][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (byte[][]) _fieldData.get(field._alias);
    }

    public ByteBuffer readByteBuffer(final String name, final ByteBuffer defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (ByteBuffer) _fieldData.get(field._alias);
    }

    @SuppressWarnings("unchecked")
    public List<ByteBuffer> readByteBufferList(final String name, final List<ByteBuffer> defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (List<ByteBuffer>) _fieldData.get(field._alias);
    }

    public double readDouble(final String name, final double defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return ((Double) _fieldData.get(field._alias)).doubleValue();
    }

    public double[] readDoubleArray(final String name, final double[] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (double[]) _fieldData.get(field._alias);
    }

    public double[][] readDoubleArray2D(final String name, final double[][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (double[][]) _fieldData.get(field._alias);
    }

    public float readFloat(final String name, final float defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return ((Float) _fieldData.get(field._alias)).floatValue();
    }

    public float[] readFloatArray(final String name, final float[] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (float[]) _fieldData.get(field._alias);
    }

    public float[][] readFloatArray2D(final String name, final float[][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (float[][]) _fieldData.get(field._alias);
    }

    public FloatBuffer readFloatBuffer(final String name, final FloatBuffer defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (FloatBuffer) _fieldData.get(field._alias);
    }

    @SuppressWarnings("unchecked")
    public List<FloatBuffer> readFloatBufferList(final String name, final List<FloatBuffer> defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (List<FloatBuffer>) _fieldData.get(field._alias);
    }

    public int readInt(final String name, final int defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return ((Integer) _fieldData.get(field._alias)).intValue();
    }

    public int[] readIntArray(final String name, final int[] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (int[]) _fieldData.get(field._alias);
    }

    public int[][] readIntArray2D(final String name, final int[][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (int[][]) _fieldData.get(field._alias);
    }

    public IntBuffer readIntBuffer(final String name, final IntBuffer defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (IntBuffer) _fieldData.get(field._alias);
    }

    public long readLong(final String name, final long defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return ((Long) _fieldData.get(field._alias)).longValue();
    }

    public long[] readLongArray(final String name, final long[] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (long[]) _fieldData.get(field._alias);
    }

    public long[][] readLongArray2D(final String name, final long[][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (long[][]) _fieldData.get(field._alias);
    }

    public Savable readSavable(final String name, final Savable defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        Object value = _fieldData.get(field._alias);
        if (value == null) {
            return null;
        } else if (value instanceof ID) {
            value = _importer.readObject(((ID) value).id);
            _fieldData.put(field._alias, value);
            return (Savable) value;
        } else {
            return defVal;
        }
    }

    public Savable[] readSavableArray(final String name, final Savable[] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        Object[] values = (Object[]) _fieldData.get(field._alias);
        if (values instanceof ID[]) {
            values = resolveIDs(values);
            _fieldData.put(field._alias, values);
            return (Savable[]) values;
        } else {
            return defVal;
        }
    }

    private Savable[] resolveIDs(final Object[] values) {
        if (values != null) {
            final Savable[] savables = new Savable[values.length];
            for (int i = 0; i < values.length; i++) {
                final ID id = (ID) values[i];
                savables[i] = id != null ? _importer.readObject(id.id) : null;
            }
            return savables;
        } else {
            return null;
        }
    }

    public Savable[][] readSavableArray2D(final String name, final Savable[][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        Object[][] values = (Object[][]) _fieldData.get(field._alias);
        if (values instanceof ID[][]) {
            final Savable[][] savables = new Savable[values.length][];
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    savables[i] = resolveIDs(values[i]);
                } else {
                    savables[i] = null;
                }
            }
            values = savables;
            _fieldData.put(field._alias, values);
        }
        return (Savable[][]) values;
    }

    public Savable[][][] readSavableArray3D(final String name, final Savable[][][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        final Object[][][] values = (Object[][][]) _fieldData.get(field._alias);
        if (values instanceof ID[][][]) {
            final Savable[][][] savables = new Savable[values.length][][];
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    savables[i] = new Savable[values[i].length][];
                    for (int j = 0; j < values[i].length; j++) {
                        savables[i][j] = resolveIDs(values[i][j]);
                    }
                } else {
                    savables[i] = null;
                }
            }
            _fieldData.put(field._alias, savables);
            return savables;
        } else {
            return defVal;
        }
    }

    private List<Savable> savableArrayListFromArray(final Savable[] savables) {
        if (savables == null) {
            return null;
        }
        final List<Savable> list = new ArrayList<>(savables.length);
        for (int x = 0; x < savables.length; x++) {
            list.add(savables[x]);
        }
        return list;
    }

    // Assumes array of size 2 arrays where pos 0 is key and pos 1 is value.
    private Map<Savable, Savable> savableMapFrom2DArray(final Savable[][] savables) {
        if (savables == null) {
            return null;
        }
        final Map<Savable, Savable> map = new HashMap<>(savables.length);
        for (int x = 0; x < savables.length; x++) {
            map.put(savables[x][0], savables[x][1]);
        }
        return map;
    }

    private Map<String, Savable> stringSavableMapFromKV(final String[] keys, final Savable[] values) {
        if (keys == null || values == null) {
            return null;
        }

        final Map<String, Savable> map = new HashMap<String, Savable>(keys.length);
        for (int x = 0; x < keys.length; x++) {
            map.put(keys[x], values[x]);
        }

        return map;
    }

    private Map<String, Object> stringObjectMapFromKV(final StringObjectMap mapItems) {
        if (mapItems == null || mapItems.keys == null || mapItems.values == null) {
            return null;
        }

        final Map<String, Object> map = new HashMap<>(mapItems.keys.length);
        for (int x = 0; x < mapItems.keys.length; x++) {
            Object value = mapItems.values[x];
            if (value instanceof ID) {
                value = _importer.readObject(((ID) value).id);
            } else if (value instanceof ID[]) {
                value = resolveIDs((ID[]) value);
            }
            map.put(mapItems.keys[x], value);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    public <E extends Savable> List<E> readSavableList(final String name, final List<E> defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        Object value = _fieldData.get(field._alias);
        if (value instanceof ID[]) {
            // read Savable array and convert to ArrayList
            final Savable[] savables = readSavableArray(name, null);
            value = savableArrayListFromArray(savables);
            _fieldData.put(field._alias, value);
        }
        return (List<E>) value;
    }

    @SuppressWarnings("unchecked")
    public <E extends Savable> List<E>[] readSavableListArray(final String name, final List<E>[] defVal)
            throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        Object value = _fieldData.get(field._alias);
        if (value instanceof ID[][]) {
            // read 2D Savable array and convert to ArrayList array
            final Savable[][] savables = readSavableArray2D(name, null);
            if (savables != null) {
                final List<Savable>[] arrayLists = new ArrayList[savables.length];
                for (int i = 0; i < savables.length; i++) {
                    arrayLists[i] = savableArrayListFromArray(savables[i]);
                }
                value = arrayLists;
            } else {
                value = defVal;
            }
            _fieldData.put(field._alias, value);
        }
        return (List<E>[]) value;
    }

    @SuppressWarnings("unchecked")
    public <E extends Savable> List<E>[][] readSavableListArray2D(final String name, final List<E>[][] defVal)
            throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        Object value = _fieldData.get(field._alias);
        if (value instanceof ID[][][]) {
            // read 3D Savable array and convert to 2D ArrayList array
            final Savable[][][] savables = readSavableArray3D(name, null);
            if (savables != null && savables.length > 0) {
                final List<Savable>[][] arrayLists = new ArrayList[savables.length][];
                for (int i = 0; i < savables.length; i++) {
                    arrayLists[i] = new ArrayList[savables[i].length];
                    for (int j = 0; j < savables[i].length; j++) {
                        arrayLists[i][j] = savableArrayListFromArray(savables[i][j]);
                    }
                }
                value = arrayLists;
            } else {
                value = defVal;
            }
            _fieldData.put(field._alias, value);
        }
        return (List<E>[][]) value;
    }

    @SuppressWarnings("unchecked")
    public <K extends Savable, V extends Savable> Map<K, V> readSavableMap(final String name, final Map<K, V> defVal)
            throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        Object value = _fieldData.get(field._alias);
        if (value instanceof ID[][]) {
            // read Savable array and convert to Map
            final Savable[][] savables = readSavableArray2D(name, null);
            value = savableMapFrom2DArray(savables);
            _fieldData.put(field._alias, value);
        }
        return (Map<K, V>) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends Savable> Map<String, V> readStringSavableMap(final String name, final Map<String, V> defVal)
            throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        Object value = _fieldData.get(field._alias);
        if (value instanceof StringIDMap) {
            // read Savable array and convert to Map values
            final StringIDMap in = (StringIDMap) value;
            final Savable[] values = resolveIDs(in.values);
            value = stringSavableMapFromKV(in.keys, values);
            _fieldData.put(field._alias, value);
        }
        return (Map<String, V>) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> readStringObjectMap(final String name, final Map<String, Object> defVal)
            throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        Object value = _fieldData.get(field._alias);
        if (value instanceof StringObjectMap) {
            // read and convert to Map values
            value = stringObjectMapFromKV((StringObjectMap) value);
            _fieldData.put(field._alias, value);
        }
        return (Map<String, Object>) value;
    }

    public short readShort(final String name, final short defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return ((Short) _fieldData.get(field._alias)).shortValue();
    }

    public short[] readShortArray(final String name, final short[] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (short[]) _fieldData.get(field._alias);
    }

    public short[][] readShortArray2D(final String name, final short[][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (short[][]) _fieldData.get(field._alias);
    }

    public ShortBuffer readShortBuffer(final String name, final ShortBuffer defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (ShortBuffer) _fieldData.get(field._alias);
    }

    public String readString(final String name, final String defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (String) _fieldData.get(field._alias);
    }

    public String[] readStringArray(final String name, final String[] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (String[]) _fieldData.get(field._alias);
    }

    public String[][] readStringArray2D(final String name, final String[][] defVal) throws IOException {
        final BinaryClassField field = _cObj._nameFields.get(name);
        if (field == null || !_fieldData.containsKey(field._alias)) {
            return defVal;
        }
        return (String[][]) _fieldData.get(field._alias);
    }

    // byte primitive

    protected byte readByte(final byte[] content) throws IOException {
        final byte value = content[_index];
        _index++;
        return value;
    }

    protected byte[] readByteArray(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final byte[] value = new byte[length];
        for (int x = 0; x < length; x++) {
            value[x] = readByte(content);
        }
        return value;
    }

    protected byte[][] readByteArray2D(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final byte[][] value = new byte[length][];
        for (int x = 0; x < length; x++) {
            value[x] = readByteArray(content);
        }
        return value;
    }

    // int primitive

    protected int readInt(final byte[] content) throws IOException {
        byte[] bytes = inflateFrom(content, _index);
        _index += 1 + bytes.length;
        bytes = ByteUtils.rightAlignBytes(bytes, 4);
        final int value = ByteUtils.convertIntFromBytes(bytes);
        if (value == BinaryOutputCapsule.NULL_OBJECT || value == BinaryOutputCapsule.DEFAULT_OBJECT) {
            _index -= 4;
        }
        return value;
    }

    protected int[] readIntArray(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final int[] value = new int[length];
        for (int x = 0; x < length; x++) {
            value[x] = readInt(content);
        }
        return value;
    }

    protected int[][] readIntArray2D(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final int[][] value = new int[length][];
        for (int x = 0; x < length; x++) {
            value[x] = readIntArray(content);
        }
        return value;
    }

    // float primitive

    protected float readFloat(final byte[] content) throws IOException {
        final float value = ByteUtils.convertFloatFromBytes(content, _index);
        _index += 4;
        return value;
    }

    protected float[] readFloatArray(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final float[] value = new float[length];
        for (int x = 0; x < length; x++) {
            value[x] = readFloat(content);
        }
        return value;
    }

    protected float[][] readFloatArray2D(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final float[][] value = new float[length][];
        for (int x = 0; x < length; x++) {
            value[x] = readFloatArray(content);
        }
        return value;
    }

    // double primitive

    protected double readDouble(final byte[] content) throws IOException {
        final double value = ByteUtils.convertDoubleFromBytes(content, _index);
        _index += 8;
        return value;
    }

    protected double[] readDoubleArray(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final double[] value = new double[length];
        for (int x = 0; x < length; x++) {
            value[x] = readDouble(content);
        }
        return value;
    }

    protected double[][] readDoubleArray2D(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final double[][] value = new double[length][];
        for (int x = 0; x < length; x++) {
            value[x] = readDoubleArray(content);
        }
        return value;
    }

    // long primitive

    protected long readLong(final byte[] content) throws IOException {
        byte[] bytes = inflateFrom(content, _index);
        _index += 1 + bytes.length;
        bytes = ByteUtils.rightAlignBytes(bytes, 8);
        final long value = ByteUtils.convertLongFromBytes(bytes);
        return value;
    }

    protected long[] readLongArray(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final long[] value = new long[length];
        for (int x = 0; x < length; x++) {
            value[x] = readLong(content);
        }
        return value;
    }

    protected long[][] readLongArray2D(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final long[][] value = new long[length][];
        for (int x = 0; x < length; x++) {
            value[x] = readLongArray(content);
        }
        return value;
    }

    // short primitive

    protected short readShort(final byte[] content) throws IOException {
        final short value = ByteUtils.convertShortFromBytes(content, _index);
        _index += 2;
        return value;
    }

    protected short[] readShortArray(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final short[] value = new short[length];
        for (int x = 0; x < length; x++) {
            value[x] = readShort(content);
        }
        return value;
    }

    protected short[][] readShortArray2D(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final short[][] value = new short[length][];
        for (int x = 0; x < length; x++) {
            value[x] = readShortArray(content);
        }
        return value;
    }

    // boolean primitive

    protected boolean readBoolean(final byte[] content) throws IOException {
        final boolean value = ByteUtils.convertBooleanFromBytes(content, _index);
        _index += 1;
        return value;
    }

    protected boolean[] readBooleanArray(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final boolean[] value = new boolean[length];
        for (int x = 0; x < length; x++) {
            value[x] = readBoolean(content);
        }
        return value;
    }

    protected boolean[][] readBooleanArray2D(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final boolean[][] value = new boolean[length][];
        for (int x = 0; x < length; x++) {
            value[x] = readBooleanArray(content);
        }
        return value;
    }

    /*
     * UTF-8 crash course:
     *
     * UTF-8 codepoints map to UTF-16 codepoints and vv, which is what Java uses for it's Strings. (so a UTF-8 codepoint
     * can contain all possible values for a Java char)
     *
     * A UTF-8 codepoint can be 1, 2 or 3 bytes long. How long a codepoint is can be told by reading the first byte: b <
     * 0x80, 1 byte (b & 0xC0) == 0xC0, 2 bytes (b & 0xE0) == 0xE0, 3 bytes
     *
     * However there is an additional restriction to UTF-8, to enable you to find the start of a UTF-8 codepoint, if you
     * start reading at a random point in a UTF-8 byte stream. That's why UTF-8 requires for the second and third byte
     * of a multibyte codepoint: (b & 0x80) == 0x80 (in other words, first bit must be 1)
     */
    private final static int UTF8_START = 0; // next byte should be the start of a new
    private final static int UTF8_2BYTE = 2; // next byte should be the second byte of a 2 byte codepoint
    private final static int UTF8_3BYTE_1 = 3; // next byte should be the second byte of a 3 byte codepoint
    private final static int UTF8_3BYTE_2 = 4; // next byte should be the third byte of a 3 byte codepoint
    private final static int UTF8_ILLEGAL = 10; // not an UTF8 string

    // String
    protected String readString(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }

        /*
         * We'll transfer the bytes into a separate byte array. While we do that we'll take the opportunity to check if
         * the byte data is valid UTF-8.
         *
         * If it is not UTF-8 it is most likely saved with the BinaryOutputCapsule bug, that saves Strings using their
         * native encoding. Unfortunatly there is no way to know what encoding was used, so we'll parse using the most
         * common one in that case; latin-1 aka ISO8859_1
         *
         * Encoding of "low" ASCII codepoint (in plain speak: when no special characters are used) will usually look the
         * same for UTF-8 and the other 1 byte codepoint encodings (espc true for numbers and regular letters of the
         * alphabet). So these are valid UTF-8 and will give the same result (at most a few charakters will appear
         * different, such as the euro sign).
         *
         * However, when "high" codepoints are used (any codepoint that over 0x7F, in other words where the first bit is
         * a 1) it's a different matter and UTF-8 and the 1 byte encoding greatly will differ, as well as most 1 byte
         * encodings relative to each other.
         *
         * It is impossible to detect which one-byte encoding is used. Since UTF8 and practically all 1-byte encodings
         * share the most used characters (the "none-high" ones) parsing them will give the same result. However, not
         * all byte sequences are legal in UTF-8 (see explantion above). If not UTF-8 encoded content is detected we
         * therefor fallback on latin1. We also log a warning.
         *
         * By this method we detect all use of 1 byte encoding if they: - use a "high" codepoint after a "low" codepoint
         * or a sequence of codepoints that is valid as UTF-8 bytes, that starts with 1000 - use a "low" codepoint after
         * a "high" codepoint - use a "low" codepoint after "high" codepoint, after a "high" codepoint that starts with
         * 1110
         *
         * In practice this means that unless 2 or 3 "high" codepoints are used after each other in proper order, we'll
         * detect the string was not originally UTF-8 encoded.
         */
        final byte[] bytes = new byte[length];
        int utf8State = UTF8_START;
        int b;
        for (int x = 0; x < length; x++) {
            bytes[x] = content[_index++];
            b = bytes[x] & 0xFF; // unsign our byte

            switch (utf8State) {
                case UTF8_START:
                    if (b < 0x80) {
                        // good
                    } else if ((b & 0xC0) == 0xC0) {
                        utf8State = UTF8_2BYTE;
                    } else if ((b & 0xE0) == 0xE0) {
                        utf8State = UTF8_3BYTE_1;
                    } else {
                        utf8State = UTF8_ILLEGAL;
                    }
                    break;
                case UTF8_3BYTE_1:
                case UTF8_3BYTE_2:
                case UTF8_2BYTE:
                    if ((b & 0x80) == 0x80) {
                        utf8State = utf8State == UTF8_3BYTE_1 ? UTF8_3BYTE_2 : UTF8_START;
                    } else {
                        utf8State = UTF8_ILLEGAL;
                    }
                    break;
            }
        }

        try {
            // even though so far the parsing might have been a legal UTF-8 sequence, only if a codepoint is fully given
            // is it correct UTF-8
            if (utf8State == UTF8_START) {
                // Java misspells UTF-8 as UTF8 for official use in java.lang
                return new String(bytes, "UTF8");
            } else {
                logger.log(Level.WARNING,
                        "Your export has been saved with an incorrect encoding for it's String fields which means it might not load correctly "
                                + "due to encoding issues.");
                // We use ISO8859_1 to be consistent across platforms. We could default to native encoding, but this
                // would lead to inconsistent
                // behaviour across platforms!
                // Developers that have previously saved their exports using the old exporter (wich uses native
                // encoding), can temporarly
                // remove the ""ISO8859_1" parameter, and change the above if statement to "if (false)".
                // They should then import and re-export their models using the same enviroment they were orginally
                // created in.
                return new String(bytes, "ISO8859_1");
            }
        } catch (final UnsupportedEncodingException uee) {
            // as a last resort fall back to platform native.
            // JavaDoc is vague about what happens when a decoding a String that contains un undecodable sequence
            // it also doesn't specify which encodings have to be supported (though UTF-8 and ISO8859 have been in the
            // SUN JRE since at least 1.1)
            logger.log(Level.SEVERE,
                    "Your export has been saved with an incorrect encoding or your version of Java is unable to decode the stored string. "
                            + "While your export may load correctly by falling back, using it on different platforms or java versions might lead to "
                            + "very strange inconsitenties. You should probably re-export your work.");
            return new String(bytes);
        }
    }

    protected String[] readStringArray(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final String[] value = new String[length];
        for (int x = 0; x < length; x++) {
            value[x] = readString(content);
        }
        return value;
    }

    protected String[][] readStringArray2D(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final String[][] value = new String[length][];
        for (int x = 0; x < length; x++) {
            value[x] = readStringArray(content);
        }
        return value;
    }

    // BitSet

    protected BitSet readBitSet(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final BitSet value = new BitSet(length);
        for (int x = 0; x < length; x++) {
            value.set(x, readBoolean(content));
        }
        return value;
    }

    // INFLATOR for int and long

    protected static byte[] inflateFrom(final byte[] contents, final int index) {
        final byte firstByte = contents[index];
        if (firstByte == BinaryOutputCapsule.NULL_OBJECT) {
            return ByteUtils.convertToBytes(BinaryOutputCapsule.NULL_OBJECT);
        } else if (firstByte == BinaryOutputCapsule.DEFAULT_OBJECT) {
            return ByteUtils.convertToBytes(BinaryOutputCapsule.DEFAULT_OBJECT);
        } else if (firstByte == 0) {
            return new byte[0];
        } else {
            final byte[] rVal = new byte[firstByte];
            for (int x = 0; x < rVal.length; x++) {
                rVal[x] = contents[x + 1 + index];
            }
            return rVal;
        }
    }

    // BinarySavable

    protected ID readSavable(final byte[] content) throws IOException {
        final int id = readInt(content);
        if (id == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }

        return new ID(id);
    }

    // BinarySavable array

    protected ID[] readSavableArray(final byte[] content) throws IOException {
        final int elements = readInt(content);
        if (elements == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final ID[] rVal = new ID[elements];
        for (int x = 0; x < elements; x++) {
            rVal[x] = readSavable(content);
        }
        return rVal;
    }

    protected ID[][] readSavableArray2D(final byte[] content) throws IOException {
        final int elements = readInt(content);
        if (elements == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final ID[][] rVal = new ID[elements][];
        for (int x = 0; x < elements; x++) {
            rVal[x] = readSavableArray(content);
        }
        return rVal;
    }

    protected ID[][][] readSavableArray3D(final byte[] content) throws IOException {
        final int elements = readInt(content);
        if (elements == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final ID[][][] rVal = new ID[elements][][];
        for (int x = 0; x < elements; x++) {
            rVal[x] = readSavableArray2D(content);
        }
        return rVal;
    }

    // BinarySavable map

    protected ID[][] readSavableMap(final byte[] content) throws IOException {
        final int elements = readInt(content);
        if (elements == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final ID[][] rVal = new ID[elements][];
        for (int x = 0; x < elements; x++) {
            rVal[x] = readSavableArray(content);
        }
        return rVal;
    }

    protected StringIDMap readStringSavableMap(final byte[] content) throws IOException {
        final int elements = readInt(content);
        if (elements == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final String[] keys = readStringArray(content);
        final ID[] values = readSavableArray(content);
        final StringIDMap rVal = new StringIDMap();
        rVal.keys = keys;
        rVal.values = values;
        return rVal;
    }

    protected StringObjectMap readStringObjectMap(final byte[] content) throws IOException {
        final int elements = readInt(content);
        if (elements == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final String[] keys = new String[elements];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = readString(content);
        }

        final Object[] values = new Object[elements];
        final AtomicReference<Object> reference = new AtomicReference<>(null);
        for (int i = 0; i < values.length; i++) {
            reference.set(null);
            final byte type = readByte(content);
            readContentOfType(content, type, reference);
            values[i] = reference.get();
        }

        final StringObjectMap rVal = new StringObjectMap();
        rVal.keys = keys;
        rVal.values = values;
        return rVal;
    }

    protected boolean readContentOfType(final byte[] content, final byte type, final AtomicReference<Object> reference)
            throws IOException {
        switch (type) {
            case BinaryClassField.BITSET: {
                reference.set(readBitSet(content));
                return true;
            }
            case BinaryClassField.BOOLEAN: {
                reference.set(readBoolean(content));
                return true;
            }
            case BinaryClassField.BOOLEAN_1D: {
                reference.set(readBooleanArray(content));
                return true;
            }
            case BinaryClassField.BOOLEAN_2D: {
                reference.set(readBooleanArray2D(content));
                return true;
            }
            case BinaryClassField.BYTE: {
                reference.set(readByte(content));
                return true;
            }
            case BinaryClassField.BYTE_1D: {
                reference.set(readByteArray(content));
                return true;
            }
            case BinaryClassField.BYTE_2D: {
                reference.set(readByteArray2D(content));
                return true;
            }
            case BinaryClassField.BYTEBUFFER: {
                reference.set(readByteBuffer(content));
                return true;
            }
            case BinaryClassField.DOUBLE: {
                reference.set(readDouble(content));
                return true;
            }
            case BinaryClassField.DOUBLE_1D: {
                reference.set(readDoubleArray(content));
                return true;
            }
            case BinaryClassField.DOUBLE_2D: {
                reference.set(readDoubleArray2D(content));
                return true;
            }
            case BinaryClassField.FLOAT: {
                reference.set(readFloat(content));
                return true;
            }
            case BinaryClassField.FLOAT_1D: {
                reference.set(readFloatArray(content));
                return true;
            }
            case BinaryClassField.FLOAT_2D: {
                reference.set(readFloatArray2D(content));
                return true;
            }
            case BinaryClassField.FLOATBUFFER: {
                reference.set(readFloatBuffer(content));
                return true;
            }
            case BinaryClassField.FLOATBUFFER_ARRAYLIST: {
                reference.set(readFloatBufferArrayList(content));
                return true;
            }
            case BinaryClassField.BYTEBUFFER_ARRAYLIST: {
                reference.set(readByteBufferArrayList(content));
                return true;
            }
            case BinaryClassField.INT: {
                reference.set(readInt(content));
                return true;
            }
            case BinaryClassField.INT_1D: {
                reference.set(readIntArray(content));
                return true;
            }
            case BinaryClassField.INT_2D: {
                reference.set(readIntArray2D(content));
                return true;
            }
            case BinaryClassField.INTBUFFER: {
                reference.set(readIntBuffer(content));
                return true;
            }
            case BinaryClassField.LONG: {
                reference.set(readLong(content));
                return true;
            }
            case BinaryClassField.LONG_1D: {
                reference.set(readLongArray(content));
                return true;
            }
            case BinaryClassField.LONG_2D: {
                reference.set(readLongArray2D(content));
                return true;
            }
            case BinaryClassField.SAVABLE: {
                reference.set(readSavable(content));
                return true;
            }
            case BinaryClassField.SAVABLE_1D: {
                reference.set(readSavableArray(content));
                return true;
            }
            case BinaryClassField.SAVABLE_2D: {
                reference.set(readSavableArray2D(content));
                return true;
            }
            case BinaryClassField.SAVABLE_ARRAYLIST: {
                reference.set(readSavableArray(content));
                return true;
            }
            case BinaryClassField.SAVABLE_ARRAYLIST_1D: {
                reference.set(readSavableArray2D(content));
                return true;
            }
            case BinaryClassField.SAVABLE_ARRAYLIST_2D: {
                reference.set(readSavableArray3D(content));
                return true;
            }
            case BinaryClassField.SAVABLE_MAP: {
                reference.set(readSavableMap(content));
                return true;
            }
            case BinaryClassField.STRING_SAVABLE_MAP: {
                reference.set(readStringSavableMap(content));
                return true;
            }
            case BinaryClassField.STRING_OBJECT_MAP: {
                reference.set(readStringObjectMap(content));
                return true;
            }
            case BinaryClassField.SHORT: {
                reference.set(readShort(content));
                return true;
            }
            case BinaryClassField.SHORT_1D: {
                reference.set(readShortArray(content));
                return true;
            }
            case BinaryClassField.SHORT_2D: {
                reference.set(readShortArray2D(content));
                return true;
            }
            case BinaryClassField.SHORTBUFFER: {
                reference.set(readShortBuffer(content));
                return true;
            }
            case BinaryClassField.STRING: {
                reference.set(readString(content));
                return true;
            }
            case BinaryClassField.STRING_1D: {
                reference.set(readStringArray(content));
                return true;
            }
            case BinaryClassField.STRING_2D: {
                reference.set(readStringArray2D(content));
                return true;
            }
        }
        return false;
    }

    // ArrayList<FloatBuffer>

    protected List<FloatBuffer> readFloatBufferArrayList(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final List<FloatBuffer> rVal = new ArrayList<FloatBuffer>(length);
        for (int x = 0; x < length; x++) {
            rVal.add(readFloatBuffer(content));
        }
        return rVal;
    }

    // ArrayList<ByteBuffer>

    protected List<ByteBuffer> readByteBufferArrayList(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }
        final List<ByteBuffer> rVal = new ArrayList<ByteBuffer>(length);
        for (int x = 0; x < length; x++) {
            rVal.add(readByteBuffer(content));
        }
        return rVal;
    }

    // NIO BUFFERS

    // float buffer
    protected FloatBuffer readFloatBuffer(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }

        final boolean direct = readBoolean(content);

        // Pull data in as a little endian byte buffer.
        final ByteBuffer buf = ByteBuffer.allocateDirect(length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(content, _index, length * 4).rewind();

        // increment index
        _index += length * 4;

        // Convert to float buffer.
        final FloatBuffer value;
        final boolean contentCopyRequired;
        if (direct) {
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                value = buf.asFloatBuffer();
                contentCopyRequired = false;
            } else {
                value = BufferUtils.createFloatBuffer(length);
                contentCopyRequired = true;
            }
        } else {
            value = BufferUtils.createFloatBufferOnHeap(length);
            contentCopyRequired = true;
        }
        if (contentCopyRequired) {
            value.put(buf.asFloatBuffer());
            value.rewind();
        }

        return value;
    }

    // int buffer
    protected IntBuffer readIntBuffer(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }

        final boolean direct = readBoolean(content);

        // Pull data in as a little endian byte buffer.
        final ByteBuffer buf = ByteBuffer.allocateDirect(length * 4).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(content, _index, length * 4).rewind();

        // increment index
        _index += length * 4;

        // Convert to int buffer.
        final IntBuffer value;
        final boolean contentCopyRequired;
        if (direct) {
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                value = buf.asIntBuffer();
                contentCopyRequired = false;
            } else {
                value = BufferUtils.createIntBuffer(length);
                contentCopyRequired = true;
            }
        } else {
            value = BufferUtils.createIntBufferOnHeap(length);
            contentCopyRequired = true;
        }
        if (contentCopyRequired) {
            value.put(buf.asIntBuffer());
            value.rewind();
        }
        return value;
    }

    // short buffer
    protected ShortBuffer readShortBuffer(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }

        final boolean direct = readBoolean(content);

        // Pull data in as a little endian byte buffer.
        final ByteBuffer buf = ByteBuffer.allocateDirect(length * 2).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(content, _index, length * 2).rewind();

        // increment index
        _index += length * 2;

        // Convert to short buffer.
        final ShortBuffer value;
        final boolean contentCopyRequired;
        if (direct) {
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                value = buf.asShortBuffer();
                contentCopyRequired = false;
            } else {
                value = BufferUtils.createShortBuffer(length);
                contentCopyRequired = true;
            }
        } else {
            value = BufferUtils.createShortBufferOnHeap(length);
            contentCopyRequired = true;
        }
        if (contentCopyRequired) {
            value.put(buf.asShortBuffer());
            value.rewind();
        }
        return value;
    }

    // byte buffer
    protected ByteBuffer readByteBuffer(final byte[] content) throws IOException {
        final int length = readInt(content);
        if (length == BinaryOutputCapsule.NULL_OBJECT) {
            return null;
        }

        final boolean direct = readBoolean(content);

        // Pull data in as a little endian byte buffer.
        final ByteBuffer buf = ByteBuffer.allocateDirect(length).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(content, _index, length).rewind();

        // increment index
        _index += length;

        // Convert to platform endian buffer.
        final ByteBuffer value;
        final boolean contentCopyRequired;
        if (direct) {
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                value = buf;
                contentCopyRequired = false;
            } else {
                value = BufferUtils.createByteBuffer(length);
                contentCopyRequired = true;
            }
        } else {
            value = BufferUtils.createByteBufferOnHeap(length);
            contentCopyRequired = true;
        }
        if (contentCopyRequired) {
            value.put(buf);
            value.rewind();
        }
        return value;
    }

    static private class ID {
        public int id;

        public ID(final int id) {
            this.id = id;
        }
    }

    static private class StringIDMap {
        public String[] keys;
        public ID[] values;
    }

    static private class StringObjectMap {
        public String[] keys;
        public Object[] values;
    }

    public <T extends Enum<T>> T readEnum(final String name, final Class<T> enumType, final T defVal)
            throws IOException {
        final String eVal = readString(name, defVal != null ? defVal.name() : null);
        if (eVal != null) {
            return Enum.valueOf(enumType, eVal);
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> T[] readEnumArray(final String name, final Class<T> enumType, final T[] defVal)
            throws IOException {
        final String[] eVals = readStringArray(name, null);
        if (eVals != null) {
            final T[] rVal = (T[]) Array.newInstance(enumType, eVals.length);
            int i = 0;
            for (final String eVal : eVals) {
                rVal[i++] = Enum.valueOf(enumType, eVal);
            }
            return rVal;
        } else {
            return defVal;
        }
    }
}
