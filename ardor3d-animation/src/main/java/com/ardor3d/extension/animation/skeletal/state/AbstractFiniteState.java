/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.state;

import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;

/**
 * Base class for a finite state in our finite state machine.
 */
public abstract class AbstractFiniteState {

  /**
   * The global time this state was last set to use as a start reference. Meant for subclass use only.
   */
  private double _globalStartTime = 0;

  /**
   * The last holder of this state. Used when we are transitioning and need to ask someone to swap us
   * with another state.
   */
  private StateOwner _lastOwner;

  /**
   * Reset the clip instances held by this state's blend tree (or other leaf nodes in our blend tree)
   * to a start time using the current global time from the given manager.
   * 
   * @param manager
   *          our manager.
   */
  public void resetClips(final AnimationManager manager) {
    resetClips(manager, manager.getCurrentGlobalTime());
  }

  /**
   * Reset the clip instances held by this state's blend tree (or other leaf nodes in our blend tree)
   * to given start time.
   * 
   * @param manager
   *          our manager.
   * @param globalStartTime
   *          the new start time to use.
   */
  public void resetClips(final AnimationManager manager, final double globalStartTime) {
    _globalStartTime = globalStartTime;
  }

  /**
   * Update this state using the current global time.
   * 
   * @param globalTime
   *          the current global time.
   * @param layer
   *          the layer this state belongs to.
   */
  public abstract void update(final double globalTime, final AnimationLayer layer);

  /**
   * Post update. If the state has no more clips and no end transition, this will clear this state
   * from the layer.
   * 
   * @param layer
   *          the layer this state belongs to.
   */
  public abstract void postUpdate(final AnimationLayer layer);

  /**
   * @return the current map of source channel data for this layer.
   */
  public abstract Map<String, ? extends Object> getCurrentSourceData(AnimationManager manager);

  /**
   * @param owner
   *          the last holder of this state. Used when we are transitioning and need to ask someone to
   *          swap us with another state. Generally only called by the AnimationLayer or by
   *          transitioning states that reference other states.
   */
  public void setLastStateOwner(final StateOwner owner) { _lastOwner = owner; }

  /**
   * @return the last holder of this state.
   * @see #setLastStateOwner(StateOwner)
   */
  public StateOwner getLastStateOwner() { return _lastOwner; }

  /**
   * @return the global time this state was last set to use as a start reference. Meant for subclass
   *         use only.
   */
  protected double getGlobalStartTime() { return _globalStartTime; }
}
