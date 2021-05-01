/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.blendtree;

import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClipInstance;
import com.ardor3d.math.util.MathUtils;

/**
 * A blend tree leaf node that samples and returns values from the channels of an AnimationClip.
 */
public class ClipSource implements BlendTreeSource {

  /** Our clip to sample from. This may be shared with other clip sources, etc. */
  protected AnimationClip _clip;

  /**
   * Construct a new ClipSource. Clip and Manager must be set separately before use.
   */
  public ClipSource() {}

  /**
   * Construct a new ClipSource using the given data.
   * 
   * @param clip
   *          the clip to use.
   * @param manager
   *          the manager to track clip state with.
   */
  public ClipSource(final AnimationClip clip, final AnimationManager manager) {
    setClip(clip);

    // init instance
    manager.getClipInstance(clip);
  }

  public AnimationClip getClip() { return _clip; }

  public void setClip(final AnimationClip clip) { _clip = clip; }

  @Override
  public Map<String, ? extends Object> getSourceData(final AnimationManager manager) {
    return manager.getClipInstance(getClip()).getChannelData();
  }

  /**
   * Sets the current time on our AnimationClip instance, accounting for looping and time scaling.
   */
  @Override
  public boolean setTime(final double globalTime, final AnimationManager manager) {
    final AnimationClipInstance instance = manager.getClipInstance(_clip);
    if (instance.isActive()) {
      double clockTime = instance.getTimeScale() * (globalTime - instance.getStartTime());

      final double maxTime = _clip.getMaxTimeIndex();
      if (maxTime <= 0) {
        return false;
      }

      // Check for looping.
      if (instance.getLoopCount() == Integer.MAX_VALUE
          || instance.getLoopCount() > 1 && maxTime * instance.getLoopCount() >= Math.abs(clockTime)) {
        if (clockTime < 0) {
          clockTime = maxTime + clockTime % maxTime;
        } else {
          clockTime %= maxTime;
        }
      } else if (clockTime < 0) {
        clockTime = maxTime + clockTime;
      }

      // Check for past max time
      if (clockTime > maxTime || clockTime < 0) {
        clockTime = MathUtils.clamp(clockTime, 0, maxTime);
        // signal to any listeners that we have ended our animation.
        instance.fireAnimationFinished();
        // deactivate this instance of the clip
        instance.setActive(false);
      }

      // update the clip with the correct clip local time.
      _clip.update(clockTime, instance);
    }
    return instance.isActive();
  }

  @Override
  public void resetClips(final AnimationManager manager, final double globalStartTime) {
    manager.resetClipInstance(_clip, globalStartTime);
  }

  @Override
  public boolean isActive(final AnimationManager manager) {
    final AnimationClipInstance instance = manager.getClipInstance(_clip);
    return instance.isActive() && _clip.getMaxTimeIndex() > 0;
  }
}
