/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.annotation.SavableFactory;

/**
 * An animation source channel consisting of keyword samples indicating when a specific trigger
 * condition is met. Each channel can only be in one keyword "state" at a given moment in time. This
 * channel guarantees that if we skip over a sample in this channel, we'll still arm it after that
 * fact. This channel should only be used with non-looping, forward moving clips.
 */
@SavableFactory(factoryMethod = "initSavable")
public class GuaranteedTriggerChannel extends TriggerChannel {

  /**
   * Construct a new GuaranteedTriggerChannel.
   * 
   * @param channelName
   *          the name of this channel.
   * @param times
   *          the time samples
   * @param keys
   *          our key samples. Entries may be null. Should have as many entries as the times array.
   */
  public GuaranteedTriggerChannel(final String channelName, final float[] times, final String[] keys) {
    super(channelName, times, keys);
  }

  @Override
  public void setCurrentSample(final int sampleIndex, final double progressPercent, final Object applyTo) {
    final TriggerData triggerData = (TriggerData) applyTo;

    final int oldIndex = triggerData.getCurrentIndex();

    // arm trigger
    final int newIndex = progressPercent != 1.0 ? sampleIndex : sampleIndex + 1;
    if (oldIndex == newIndex) {
      triggerData.arm(newIndex, _keys[newIndex]);
    } else {
      final List<String> triggers = new ArrayList<>();
      for (int i = oldIndex + 1; i <= newIndex; i++) {
        if (_keys[i] != null) {
          triggers.add(_keys[i]);
        }
      }
      triggerData.arm(newIndex, triggers.toArray(new String[triggers.size()]));
    }
  }

  @Override
  public AbstractAnimationChannel getSubchannelBySample(final String name, final int startSample, final int endSample) {
    if (startSample > endSample) {
      throw new IllegalArgumentException("startSample > endSample");
    }
    if (endSample >= getSampleCount()) {
      throw new IllegalArgumentException("endSample >= getSampleCount()");
    }

    final int samples = endSample - startSample + 1;
    final float[] times = new float[samples];
    final String[] keys = new String[samples];

    for (int i = 0; i <= samples; i++) {
      times[i] = _times[i + startSample];
      keys[i] = _keys[i + startSample];
    }

    return new GuaranteedTriggerChannel(name, times, keys);
  }

  @Override
  public AbstractAnimationChannel getSubchannelByTime(final String name, final float startTime, final float endTime) {
    if (startTime > endTime) {
      throw new IllegalArgumentException("startTime > endTime");
    }
    final List<Float> times = new ArrayList<>();
    final List<String> keys = new ArrayList<>();

    final TriggerData tData = new TriggerData();

    // Add start sample
    updateSample(startTime, tData);
    times.add(0f);
    keys.add(tData.getCurrentTrigger());

    // Add mid samples
    for (int i = 0; i < getSampleCount(); i++) {
      final float time = _times[i];
      updateSample(time, tData);
      if (time > startTime && time < endTime) {
        times.add(time - startTime);
        keys.add(_keys[i]);
      }
    }

    // Add end sample
    updateSample(endTime, tData);
    times.add(endTime - startTime);
    keys.add(tData.getCurrentTrigger());

    final float[] timesArray = new float[times.size()];
    int i = 0;
    for (final float time : times) {
      timesArray[i++] = time;
    }
    // return
    return new GuaranteedTriggerChannel(name, timesArray, keys.toArray(new String[keys.size()]));
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends GuaranteedTriggerChannel> getClassTag() { return this.getClass(); }

  public static GuaranteedTriggerChannel initSavable() {
    return new GuaranteedTriggerChannel();
  }

  protected GuaranteedTriggerChannel() {
    super(null, null, null);
  }
}
