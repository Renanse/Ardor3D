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
import java.util.List;
import java.util.Map;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Test Savable exercising the Savable-graph collection writers/readers: object arrays, lists, list
 * arrays, buffer lists, and the three map flavours (Savable->Savable, String->Savable,
 * String->Object). Object arrays come back as runtime {@code Savable[]} so those fields are typed as
 * such on purpose.
 */
public class CollectionsHolder implements Savable {

  public Savable[] savArr;
  public Savable[][] savArr2;
  public List<SavableLeaf> savList;
  public List<Savable>[] savListArr;
  public List<FloatBuffer> floatBufList;
  public List<ByteBuffer> byteBufList;
  public Map<SavableLeaf, SavableLeaf> savMap;
  public Map<String, SavableLeaf> strSavMap;
  public Map<String, Object> strObjMap;

  public CollectionsHolder() {}

  @Override
  public Class<? extends CollectionsHolder> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(savArr, "savArr", null);
    capsule.write(savArr2, "savArr2", null);
    capsule.writeSavableList(savList, "savList", null);
    capsule.writeSavableListArray(savListArr, "savListArr", null);
    capsule.writeFloatBufferList(floatBufList, "floatBufList", null);
    capsule.writeByteBufferList(byteBufList, "byteBufList", null);
    capsule.writeSavableMap(savMap, "savMap", null);
    capsule.writeStringSavableMap(strSavMap, "strSavMap", null);
    capsule.writeStringObjectMap(strObjMap, "strObjMap", null);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    savArr = capsule.readSavableArray("savArr", null);
    savArr2 = capsule.readSavableArray2D("savArr2", null);
    savList = capsule.readSavableList("savList", null);
    savListArr = capsule.readSavableListArray("savListArr", null);
    floatBufList = capsule.readFloatBufferList("floatBufList", null);
    byteBufList = capsule.readByteBufferList("byteBufList", null);
    savMap = capsule.readSavableMap("savMap", null);
    strSavMap = capsule.readStringSavableMap("strSavMap", null);
    strObjMap = capsule.readStringObjectMap("strObjMap", null);
  }
}
