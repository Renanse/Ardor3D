/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ardor3d.util.resource.StringResourceSource;

/**
 * The "g" group statement records one group name per token after the keyword. ObjImporter used to
 * treat {@code argCount <= 2} as "no group", silently discarding the common {@code g name} and
 * {@code g name1 name2} forms and filing their geometry under the default group instead.
 */
public class TestObjImporterGroups {

  private static final String VERTS = "v 0 0 0\nv 1 0 0\nv 0 1 0\n";

  private ObjGeometryStore load(final String obj) {
    return new ObjImporter().load(new StringResourceSource(obj, ".obj"));
  }

  @Test
  public void singleGroupNameIsRecorded() {
    final ObjGeometryStore store = load(VERTS + "g mygroup\nf 1 2 3\n");
    assertTrue("'g mygroup' should register the group 'mygroup'.", store.getGroupMap().containsKey("mygroup"));
    assertFalse("Named geometry should not fall back to the default group.",
        store.getGroupMap().containsKey("_default_"));
  }

  @Test
  public void twoGroupNamesAreRecorded() {
    final ObjGeometryStore store = load(VERTS + "g left right\nf 1 2 3\n");
    assertTrue("'g left right' should register 'left'.", store.getGroupMap().containsKey("left"));
    assertTrue("'g left right' should register 'right'.", store.getGroupMap().containsKey("right"));
  }

  @Test
  public void namelessGroupStillFallsBackToDefault() {
    final ObjGeometryStore store = load(VERTS + "g\nf 1 2 3\n");
    assertTrue("A bare 'g' with no names should map to the default group.",
        store.getGroupMap().containsKey("_default_"));
  }

  @Test
  public void groupSpanningMultipleObjectsKeepsAllSpatials() {
    // A single group can contain several committed spatials - here two "o" objects each force a
    // commit while the group name stays "mygroup". Both must be retained, not overwritten.
    final ObjGeometryStore store =
        load("v 0 0 0\nv 1 0 0\nv 0 1 0\nv 0 0 1\n" + "g mygroup\no partA\nf 1 2 3\no partB\nf 1 2 4\n");
    assertEquals("Both spatials committed under 'mygroup' should be retained.", 2,
        store.getGroupMap().get("mygroup").size());
  }
}
