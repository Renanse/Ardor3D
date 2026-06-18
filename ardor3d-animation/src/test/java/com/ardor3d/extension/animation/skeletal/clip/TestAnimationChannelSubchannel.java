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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Exercises {@link AbstractAnimationChannel#getSubchannelBySample} for each concrete channel type.
 * The sub-channel of samples [1,3] of a five-sample channel must contain exactly the three middle
 * samples; an off-by-one in the copy loop throws ArrayIndexOutOfBoundsException instead.
 */
public class TestAnimationChannelSubchannel {

  private static final float[] TIMES = {0f, 1f, 2f, 3f, 4f};
  private static final float[] EXPECTED_TIMES = {1f, 2f, 3f};

  @Test
  public void testTriggerChannelSubchannel() {
    final String[] keys = {"a", "b", "c", "d", "e"};
    final TriggerChannel sub =
        (TriggerChannel) new TriggerChannel("c", TIMES, keys).getSubchannelBySample("sub", 1, 3);

    assertEquals(3, sub.getSampleCount());
    assertArrayEquals(EXPECTED_TIMES, sub.getTimes(), 0f);
    assertArrayEquals(new String[] {"b", "c", "d"}, sub.getKeys());
  }

  @Test
  public void testGuaranteedTriggerChannelSubchannel() {
    final String[] keys = {"a", "b", "c", "d", "e"};
    final GuaranteedTriggerChannel sub = (GuaranteedTriggerChannel) new GuaranteedTriggerChannel("c", TIMES, keys)
        .getSubchannelBySample("sub", 1, 3);

    assertEquals(3, sub.getSampleCount());
    assertArrayEquals(EXPECTED_TIMES, sub.getTimes(), 0f);
    assertArrayEquals(new String[] {"b", "c", "d"}, sub.getKeys());
  }

  @Test
  public void testInterpolatedFloatChannelSubchannel() {
    final float[] values = {10f, 11f, 12f, 13f, 14f};
    final InterpolatedFloatChannel sub =
        new InterpolatedFloatChannel("c", TIMES, values).getSubchannelBySample("sub", 1, 3);

    assertEquals(3, sub.getSampleCount());
    assertArrayEquals(EXPECTED_TIMES, sub.getTimes(), 0f);
    assertArrayEquals(new float[] {11f, 12f, 13f}, sub.getValues(), 0f);
  }

  @Test
  public void testInterpolatedDoubleChannelSubchannel() {
    final double[] values = {10d, 11d, 12d, 13d, 14d};
    final InterpolatedDoubleChannel sub =
        new InterpolatedDoubleChannel("c", TIMES, values).getSubchannelBySample("sub", 1, 3);

    assertEquals(3, sub.getSampleCount());
    assertArrayEquals(EXPECTED_TIMES, sub.getTimes(), 0f);
    assertArrayEquals(new double[] {11d, 12d, 13d}, sub.getValues(), 0d);
  }
}
