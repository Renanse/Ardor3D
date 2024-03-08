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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationListener;

/**
 * Maintains state information about an instance of a specific animation clip, such as time scaling
 * applied, active flag, start time of the instance, etc.
 */
public class AnimationClipInstance {

  /** Active flag - if true, the instance is currently playing. */
  private boolean _active = true;

  /** Number of loops this clip should play. */
  private int _loopCount = 0;

  /**
   * A scale value to apply to our timing. Values greater than 1 will speed up playback, less than 1
   * will slow down playback. Negative values can be used to reverse playback.
   */
  private double _timeScale = 1.0;

  /** The global start time of our clip instance. */
  private double _startTime = 0.0;

  /** Map of channel name -> state tracking objects. */
  private final Map<String, Object> _clipStateObjects = new HashMap<>();

  /** List of callbacks for animation events. */
  private List<AnimationListener> animationListeners = null;

  /**
   * Add an animation listener to our callback list.
   * 
   * @param animationListener
   *          the listener to add.
   */
  public void addAnimationListener(final AnimationListener animationListener) {
    if (animationListeners == null) {
      animationListeners = new ArrayList<>();
    }
    animationListeners.add(animationListener);
  }

  /**
   * Remove an animation listener from our callback list.
   * 
   * @param animationListener
   *          the listener to remove.
   * @return true if the listener was found in our list
   */
  public boolean removeAnimationListener(final AnimationListener animationListener) {
    if (animationListeners == null) {
      return false;
    }
    final boolean rVal = animationListeners.remove(animationListener);
    if (animationListeners.isEmpty()) {
      animationListeners = null;
    }
    return rVal;
  }

  /**
   * @return an immutable copy of the list of action listeners.
   */
  public List<AnimationListener> getAnimationListeners() {
    if (animationListeners == null) {
      return List.of();
    }
    return List.copyOf(animationListeners);
  }

  public boolean isActive() { return _active; }

  public void setActive(final boolean active) { _active = active; }

  public int getLoopCount() { return _loopCount; }

  public void setLoopCount(final int loopCount) { _loopCount = loopCount; }

  public double getTimeScale() { return _timeScale; }

  public void setTimeScale(final double timeScale) { _timeScale = timeScale; }

  public double getStartTime() { return _startTime; }

  public void setStartTime(final double startTime) { _startTime = startTime; }

  public Object getApplyTo(final AbstractAnimationChannel channel) {
    final String channelName = channel.getChannelName();
    Object rVal = _clipStateObjects.get(channelName);
    if (rVal == null) {
      rVal = channel.createStateDataObject(this);
      _clipStateObjects.put(channelName, rVal);
    }
    return rVal;
  }

  public Map<String, Object> getChannelData() { return _clipStateObjects; }

  /**
   * Tell any animation listeners on this instance that the associated clip has finished playing.
   */
  public void fireAnimationFinished() {
    if (animationListeners == null) {
      return;
    }

    for (final AnimationListener animationListener : animationListeners) {
      animationListener.animationFinished(this);
    }
  }
}
