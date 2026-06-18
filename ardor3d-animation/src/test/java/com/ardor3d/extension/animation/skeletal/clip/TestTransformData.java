/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.export.binary.BinaryImporter;

public class TestTransformData {

  /**
   * Round-trips a TransformData through the binary Savable format. Translation, rotation and scale
   * are all distinct and non-default so a mixed-up capsule key cannot be masked by coincidental
   * equality with another field or with a default value.
   */
  @Test
  public void testBinaryRoundTrip() throws Exception {
    final TransformData source = new TransformData();
    source.setTranslation(new Vector3(1, 2, 3));
    source.setScale(new Vector3(4, 5, 6));
    source.setRotation(new Quaternion(0.5, 0.5, 0.5, 0.5)); // already unit-length

    final TransformData restored = (TransformData) roundTrip(source);

    assertEquals("translation must survive serialization", new Vector3(1, 2, 3), restored.getTranslation());
    assertEquals("rotation must survive serialization", new Quaternion(0.5, 0.5, 0.5, 0.5), restored.getRotation());
    assertEquals("scale must survive serialization", new Vector3(4, 5, 6), restored.getScale());
  }

  /**
   * JointData extends TransformData and delegates to super.read()/super.write(), so it must
   * round-trip the inherited transform fields as well as its own joint index.
   */
  @Test
  public void testJointDataBinaryRoundTrip() throws Exception {
    final JointData source = new JointData(7);
    source.setTranslation(new Vector3(1, 2, 3));
    source.setScale(new Vector3(4, 5, 6));
    source.setRotation(new Quaternion(0.5, 0.5, 0.5, 0.5));

    final JointData restored = (JointData) roundTrip(source);

    assertEquals("joint index must survive serialization", 7, restored.getJointIndex());
    assertEquals("translation must survive serialization", new Vector3(1, 2, 3), restored.getTranslation());
    assertEquals("rotation must survive serialization", new Quaternion(0.5, 0.5, 0.5, 0.5), restored.getRotation());
    assertEquals("scale must survive serialization", new Vector3(4, 5, 6), restored.getScale());
  }

  private static TransformData roundTrip(final TransformData source) throws Exception {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    new BinaryExporter().save(source, bytes);
    return (TransformData) new BinaryImporter().load(new ByteArrayInputStream(bytes.toByteArray()));
  }
}
