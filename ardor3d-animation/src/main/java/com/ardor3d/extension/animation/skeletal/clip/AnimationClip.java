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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * AnimationClip manages a set of animation channels as a single clip entity.
 */
@SavableFactory(factoryMethod = "initSavable")
public class AnimationClip implements Savable {

  /** A referenceable name for this clip. In general, this should be unique. */
  private final String _name;

  /** A list of animation channels managed by this clip. */
  private final List<AbstractAnimationChannel> _channels;

  /** A max time value for this clip, pulled from our managed channels. */
  private transient float _maxTime = 0;

  /**
   * Construct a new animation clip with no channels.
   */
  public AnimationClip(final String name) {
    _name = name;
    _channels = new ArrayList<>();
  }

  /**
   * Construct a new animation clip, copying in a given list of channels.
   *
   * @param channels
   *          a list of channels to shallow copy locally.
   */
  public AnimationClip(final String name, final List<AbstractAnimationChannel> channels) {
    _name = name;
    _channels = new ArrayList<>(channels);
    updateMaxTimeIndex();
  }

  /**
   * @return the referenceable name for this clip. In general, this should be unique.
   */
  public String getName() { return _name; }

  /**
   * Update an instance of this clip.
   *
   * @param clockTime
   *          the current local clip time (where 0 == start of clip)
   * @param instance
   *          the instance record to update.
   */
  public void update(final double clockTime, final AnimationClipInstance instance) {
    // Go through each channel and update clipState
    for (int i = 0; i < _channels.size(); ++i) {
      final AbstractAnimationChannel channel = _channels.get(i);
      final Object applyTo = instance.getApplyTo(channel);
      channel.updateSample(clockTime, applyTo);
    }
  }

  /**
   * Add a channel to this clip.
   *
   * @param channel
   *          the channel to add.
   */
  public void addChannel(final AbstractAnimationChannel channel) {
    _channels.add(channel);
    updateMaxTimeIndex();
  }

  /**
   * Locate a channel in this clip using its channel name.
   *
   * @param channelName
   *          the name to match against.
   * @return the first channel with a name matching the given channelName, or null if no matches are
   *         found.
   */
  public AbstractAnimationChannel findChannelByName(final String channelName) {
    for (final AbstractAnimationChannel channel : _channels) {
      if (channelName.equals(channel.getChannelName())) {
        return channel;
      }
    }
    return null;
  }

  /**
   * Remove a given channel from this clip.
   *
   * @param channel
   *          the channel to remove.
   * @return true if this clip had the given channel and it was removed.
   */
  public boolean removeChannel(final AbstractAnimationChannel channel) {
    final boolean rVal = _channels.remove(channel);
    updateMaxTimeIndex();
    return rVal;
  }

  /**
   * @return an immutable copy of the channels in this clip.
   */
  public List<AbstractAnimationChannel> getChannels() { return List.copyOf(_channels); }

  /**
   * @return the maximum (local) time value of this clip, as described by the channels it manages.
   */
  public float getMaxTimeIndex() { return _maxTime; }

  /**
   * Update our max time value to match the max time in our managed animation channels.
   */
  private void updateMaxTimeIndex() {
    _maxTime = 0;
    float max;
    for (final AbstractAnimationChannel channel : _channels) {
      max = channel.getMaxTime();
      if (max > _maxTime) {
        _maxTime = max;
      }
    }
  }

  @Override
  public String toString() {
    return "AnimationClip [channel count=" + _channels.size() + ", max time=" + _maxTime + "]";
  }

  // /////////////////
  // Methods for Savable
  // /////////////////

  @Override
  public Class<? extends AnimationClip> getClassTag() { return this.getClass(); }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_name, "name", null);
    capsule.writeSavableList(_channels, "channels", null);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    final String name = capsule.readString("name", null);
    try {
      final Field field1 = AnimationClip.class.getDeclaredField("_name");
      field1.setAccessible(true);
      field1.set(this, name);
    } catch (final Exception e) {
      e.printStackTrace();
    }

    _channels.clear();
    final List<Savable> channels = capsule.readSavableList("channels", null);
    for (final Savable channel : channels) {
      _channels.add((AbstractAnimationChannel) channel);
    }
    updateMaxTimeIndex();
  }

  public static AnimationClip initSavable() {
    return new AnimationClip();
  }

  private AnimationClip() {
    this(null);
  }
}
