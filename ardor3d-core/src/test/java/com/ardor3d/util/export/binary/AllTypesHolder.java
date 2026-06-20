/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.export.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.BitSet;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Exhaustive test Savable carrying one field of every primitive scalar, 1D array, 2D array, buffer,
 * BitSet, Enum and String type the capsule API supports. Used to prove the binary format round-trips
 * each leaf type, and that no field desynchronises the ones written after it.
 */
public class AllTypesHolder implements Savable {

  public enum Flavor {
    ALPHA, BETA, GAMMA
  }

  public boolean z;
  public byte b;
  public short s;
  public int i;
  public long l;
  public float f;
  public double d;
  public String str;
  public Flavor en;

  public boolean[] zArr;
  public byte[] bArr;
  public short[] sArr;
  public int[] iArr;
  public long[] lArr;
  public float[] fArr;
  public double[] dArr;
  public String[] strArr;
  public Flavor[] enArr;

  public boolean[][] zArr2;
  public byte[][] bArr2;
  public short[][] sArr2;
  public int[][] iArr2;
  public long[][] lArr2;
  public float[][] fArr2;
  public double[][] dArr2;
  public String[][] strArr2;

  public ByteBuffer byteBuf;
  public FloatBuffer floatBuf;
  public IntBuffer intBuf;
  public ShortBuffer shortBuf;

  public BitSet bits;

  public AllTypesHolder() {}

  @Override
  public Class<? extends AllTypesHolder> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    // scalar defaults are deliberately chosen to differ from the populated values so every field is
    // actually emitted (the format omits fields equal to their default).
    capsule.write(z, "z", false);
    capsule.write(b, "b", (byte) 0);
    capsule.write(s, "s", (short) 0);
    capsule.write(i, "i", 0);
    capsule.write(l, "l", 0L);
    capsule.write(f, "f", 0f);
    capsule.write(d, "d", 0.0);
    capsule.write(str, "str", null);
    capsule.write(en, "en", null);

    capsule.write(zArr, "zArr", null);
    capsule.write(bArr, "bArr", null);
    capsule.write(sArr, "sArr", null);
    capsule.write(iArr, "iArr", null);
    capsule.write(lArr, "lArr", null);
    capsule.write(fArr, "fArr", null);
    capsule.write(dArr, "dArr", null);
    capsule.write(strArr, "strArr", null);
    capsule.write(enArr, "enArr");

    capsule.write(zArr2, "zArr2", null);
    capsule.write(bArr2, "bArr2", null);
    capsule.write(sArr2, "sArr2", null);
    capsule.write(iArr2, "iArr2", null);
    capsule.write(lArr2, "lArr2", null);
    capsule.write(fArr2, "fArr2", null);
    capsule.write(dArr2, "dArr2", null);
    capsule.write(strArr2, "strArr2", null);

    capsule.write(byteBuf, "byteBuf", null);
    capsule.write(floatBuf, "floatBuf", null);
    capsule.write(intBuf, "intBuf", null);
    capsule.write(shortBuf, "shortBuf", null);

    capsule.write(bits, "bits", null);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    z = capsule.readBoolean("z", false);
    b = capsule.readByte("b", (byte) 0);
    s = capsule.readShort("s", (short) 0);
    i = capsule.readInt("i", 0);
    l = capsule.readLong("l", 0L);
    f = capsule.readFloat("f", 0f);
    d = capsule.readDouble("d", 0.0);
    str = capsule.readString("str", null);
    en = capsule.readEnum("en", Flavor.class, null);

    zArr = capsule.readBooleanArray("zArr", null);
    bArr = capsule.readByteArray("bArr", null);
    sArr = capsule.readShortArray("sArr", null);
    iArr = capsule.readIntArray("iArr", null);
    lArr = capsule.readLongArray("lArr", null);
    fArr = capsule.readFloatArray("fArr", null);
    dArr = capsule.readDoubleArray("dArr", null);
    strArr = capsule.readStringArray("strArr", null);
    enArr = capsule.readEnumArray("enArr", Flavor.class, null);

    zArr2 = capsule.readBooleanArray2D("zArr2", null);
    bArr2 = capsule.readByteArray2D("bArr2", null);
    sArr2 = capsule.readShortArray2D("sArr2", null);
    iArr2 = capsule.readIntArray2D("iArr2", null);
    lArr2 = capsule.readLongArray2D("lArr2", null);
    fArr2 = capsule.readFloatArray2D("fArr2", null);
    dArr2 = capsule.readDoubleArray2D("dArr2", null);
    strArr2 = capsule.readStringArray2D("strArr2", null);

    byteBuf = capsule.readByteBuffer("byteBuf", null);
    floatBuf = capsule.readFloatBuffer("floatBuf", null);
    intBuf = capsule.readIntBuffer("intBuf", null);
    shortBuf = capsule.readShortBuffer("shortBuf", null);

    bits = capsule.readBitSet("bits", null);
  }
}
