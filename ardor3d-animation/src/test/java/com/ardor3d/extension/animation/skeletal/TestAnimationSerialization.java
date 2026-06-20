/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.InterpolatedDoubleChannel;
import com.ardor3d.extension.animation.skeletal.clip.InterpolatedFloatChannel;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.clip.TriggerChannel;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.export.binary.BinaryImporter;

/**
 * Round-trips the skeletal/clip Savables whose read() methods were converted from reflective
 * setAccessible/field.set into normal assignment. These guard against the de-reflection silently
 * dropping a field - the value-bearing fields must still survive a save/load.
 */
public class TestAnimationSerialization {

  static Savable roundTrip(final Savable in) throws Exception {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    new BinaryExporter().save(in, out);
    return new BinaryImporter().load(new ByteArrayInputStream(out.toByteArray()));
  }

  @Test
  public void testInterpolatedFloatChannelRoundTrip() throws Exception {
    final InterpolatedFloatChannel src =
        new InterpolatedFloatChannel("scalar", new float[] {0f, 0.5f, 1f}, new float[] {10f, 20f, 30f});

    final InterpolatedFloatChannel r = (InterpolatedFloatChannel) roundTrip(src);

    assertEquals("scalar", r.getChannelName());
    assertArrayEquals(new float[] {0f, 0.5f, 1f}, r.getTimes(), 0f);
    assertArrayEquals(new float[] {10f, 20f, 30f}, r.getValues(), 0f);
  }

  @Test
  public void testInterpolatedDoubleChannelRoundTrip() throws Exception {
    final InterpolatedDoubleChannel src =
        new InterpolatedDoubleChannel("scalarD", new float[] {0f, 1f}, new double[] {1.25, 2.5});

    final InterpolatedDoubleChannel r = (InterpolatedDoubleChannel) roundTrip(src);

    assertEquals("scalarD", r.getChannelName());
    assertArrayEquals(new float[] {0f, 1f}, r.getTimes(), 0f);
    assertArrayEquals(new double[] {1.25, 2.5}, r.getValues(), 0.0);
  }

  @Test
  public void testTriggerChannelRoundTrip() throws Exception {
    final TriggerChannel src =
        new TriggerChannel("triggers", new float[] {0f, 1f}, new String[] {"footstep", "land"});

    final TriggerChannel r = (TriggerChannel) roundTrip(src);

    assertEquals("triggers", r.getChannelName());
    assertArrayEquals(new float[] {0f, 1f}, r.getTimes(), 0f);
    assertArrayEquals(new String[] {"footstep", "land"}, r.getKeys());
  }

  @Test
  public void testJointChannelRoundTrip() throws Exception {
    final float[] times = {0f, 1f};
    final ReadOnlyQuaternion[] rot = {new Quaternion(0, 0, 0, 1), new Quaternion(0, 1, 0, 0)};
    final ReadOnlyVector3[] trans = {new Vector3(1, 2, 3), new Vector3(4, 5, 6)};
    final ReadOnlyVector3[] scale = {new Vector3(1, 1, 1), new Vector3(2, 2, 2)};
    final JointChannel src = new JointChannel("hip", 7, times, rot, trans, scale);

    final JointChannel r = (JointChannel) roundTrip(src);

    assertEquals("hip", r.getJointName());
    assertEquals(7, r.getJointIndex()); // recovered from the channel name on read
    assertArrayEquals(times, r.getTimes(), 0f);
    assertEquals(new Vector3(1, 2, 3), r.getTranslations().get(0));
    assertEquals(new Vector3(2, 2, 2), r.getScales().get(1));
  }

  @Test
  public void testAnimationClipRoundTrip() throws Exception {
    final AnimationClip src = new AnimationClip("walk");
    src.addChannel(new InterpolatedFloatChannel("c", new float[] {0f, 1f}, new float[] {1f, 2f}));

    final AnimationClip r = (AnimationClip) roundTrip(src);

    assertEquals("walk", r.getName());
    assertEquals(1, r.getChannels().size());
    assertEquals("c", r.getChannels().get(0).getChannelName());
  }

  @Test
  public void testSkeletonAndJointRoundTrip() throws Exception {
    final Joint root = new Joint("root");
    root.setIndex((short) 0);
    root.setParentIndex(Joint.NO_PARENT);
    final Joint child = new Joint("spine");
    child.setIndex((short) 1);
    child.setParentIndex((short) 0);
    final Skeleton src = new Skeleton("rig", new Joint[] {root, child});

    final Skeleton r = (Skeleton) roundTrip(src);

    assertEquals("rig", r.getName());
    assertEquals(2, r.getJoints().length);
    assertEquals("root", r.getJoints()[0].getName());
    assertEquals((short) 1, r.getJoints()[1].getIndex());
    assertEquals((short) 0, r.getJoints()[1].getParentIndex());
  }

  @Test
  public void testSkeletonPoseRoundTrip() throws Exception {
    final Joint root = new Joint("root");
    root.setIndex((short) 0);
    root.setParentIndex(Joint.NO_PARENT);
    final Skeleton skel = new Skeleton("rig", new Joint[] {root});
    final SkeletonPose src = new SkeletonPose(skel);
    src.getLocalJointTransforms()[0].setTranslation(3, 4, 5);

    final SkeletonPose r = (SkeletonPose) roundTrip(src);

    assertEquals(1, r.getSkeleton().getJoints().length);
    assertEquals(new Vector3(3, 4, 5), r.getLocalJointTransforms()[0].getTranslation());
    // transient palettes are not serialized; read() must rebuild them sized to the joint count
    assertNotNull(r.getGlobalJointTransforms());
    assertEquals(1, r.getGlobalJointTransforms().length);
    assertNotNull(r.getMatrixPalette());
    assertEquals(1, r.getMatrixPalette().length);
  }
}
