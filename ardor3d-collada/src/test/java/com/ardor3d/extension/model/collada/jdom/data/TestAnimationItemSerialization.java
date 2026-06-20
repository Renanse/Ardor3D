/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.export.binary.BinaryImporter;

/**
 * AnimationItem.read previously reflected into AnimationClip's _name field on an AnimationItem
 * instance, which threw IllegalArgumentException on every load and was swallowed - so the name came
 * back null. This verifies the name (and the nested children/clip) now survive a round-trip.
 */
public class TestAnimationItemSerialization {

  static Savable roundTrip(final Savable in) throws Exception {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    new BinaryExporter().save(in, out);
    return new BinaryImporter().load(new ByteArrayInputStream(out.toByteArray()));
  }

  @Test
  public void testNameAndChildrenRestoredOnLoad() throws Exception {
    final AnimationItem root = new AnimationItem("root-anim");
    final AnimationItem child = new AnimationItem("child-anim");
    root.getChildren().add(child);
    root.setAnimationClip(new AnimationClip("clip1"));

    final AnimationItem r = (AnimationItem) roundTrip(root);

    assertEquals("root-anim", r.getName()); // was null before the de-reflection fix
    assertEquals(1, r.getChildren().size());
    assertEquals("child-anim", r.getChildren().get(0).getName());
    assertNotNull(r.getAnimationClip());
    assertEquals("clip1", r.getAnimationClip().getName());
  }
}
