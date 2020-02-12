/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.binary;

public class BinaryClassField {

    public static final byte UNHANDLED = -1;

    public static final byte BYTE = 0;
    public static final byte BYTE_1D = 1;
    public static final byte BYTE_2D = 2;

    public static final byte INT = 10;
    public static final byte INT_1D = 11;
    public static final byte INT_2D = 12;

    public static final byte FLOAT = 20;
    public static final byte FLOAT_1D = 21;
    public static final byte FLOAT_2D = 22;

    public static final byte DOUBLE = 30;
    public static final byte DOUBLE_1D = 31;
    public static final byte DOUBLE_2D = 32;

    public static final byte LONG = 40;
    public static final byte LONG_1D = 41;
    public static final byte LONG_2D = 42;

    public static final byte SHORT = 50;
    public static final byte SHORT_1D = 51;
    public static final byte SHORT_2D = 52;

    public static final byte BOOLEAN = 60;
    public static final byte BOOLEAN_1D = 61;
    public static final byte BOOLEAN_2D = 62;

    public static final byte STRING = 70;
    public static final byte STRING_1D = 71;
    public static final byte STRING_2D = 72;

    public static final byte BITSET = 80;

    public static final byte SAVABLE = 90;
    public static final byte SAVABLE_1D = 91;
    public static final byte SAVABLE_2D = 92;

    public static final byte SAVABLE_ARRAYLIST = 100;
    public static final byte SAVABLE_ARRAYLIST_1D = 101;
    public static final byte SAVABLE_ARRAYLIST_2D = 102;

    public static final byte SAVABLE_MAP = 105;
    public static final byte STRING_SAVABLE_MAP = 106;
    public static final byte STRING_OBJECT_MAP = 107;

    public static final byte FLOATBUFFER_ARRAYLIST = 110;
    public static final byte BYTEBUFFER_ARRAYLIST = 111;

    public static final byte FLOATBUFFER = 120;
    public static final byte INTBUFFER = 121;
    public static final byte BYTEBUFFER = 122;
    public static final byte SHORTBUFFER = 123;

    public byte _type;
    public String _name;
    public byte _alias;

    public BinaryClassField(final String name, final byte alias, final byte type) {
        _name = name;
        _alias = alias;
        _type = type;
    }
}
