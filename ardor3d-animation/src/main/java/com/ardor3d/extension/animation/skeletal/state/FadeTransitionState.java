/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.state;

import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;

/**
 * A transition that blends over a given time from one animation state to another, beginning the
 * target clip from local time 0 at the start of the transition. This is best used with two clips
 * that have similar motions.
 */
public class FadeTransitionState extends AbstractTwoStateLerpTransition {

  /**
   * Construct a new FadeTransitionState.
   * 
   * @param targetState
   *          the name of the steady state we want the Animation Layer to be in at the end of the
   *          transition.
   * @param fadeTime
   *          the amount of time we should take to do the transition.
   * @param type
   *          the way we should interpolate the weighting during the transition.
   */
  public FadeTransitionState(final String targetState, final double fadeTime, final BlendType type) {
    super(targetState, fadeTime, type);
  }

  @Override
  public AbstractFiniteState getTransitionState(final AbstractFiniteState callingState, final AnimationLayer layer) {
    // grab current time as our start
    setStart(layer.getManager().getCurrentGlobalTime());
    // set "current" start state
    setStateA(callingState);
    // set "target" end state
    setStateB(layer.getSteadyState(getTargetState()));
    if (getStateB() == null) {
      return null;
    }
    // restart end state.
    getStateB().resetClips(layer.getManager(), getStart());
    return this;
  }

  @Override
  public void update(final double globalTime, final AnimationLayer layer) {
    super.update(globalTime, layer);

    // update both of our states
    if (getStateA() != null) {
      getStateA().update(globalTime, layer);
    }
    if (getStateB() != null) {
      getStateB().update(globalTime, layer);
    }
  }

  @Override
  public void postUpdate(final AnimationLayer layer) {
    // post update both of our states
    if (getStateA() != null) {
      getStateA().postUpdate(layer);
    }
    if (getStateB() != null) {
      getStateB().postUpdate(layer);
    }
  }
}
