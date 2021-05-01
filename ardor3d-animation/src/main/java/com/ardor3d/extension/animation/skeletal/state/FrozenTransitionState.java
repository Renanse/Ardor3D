/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
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
 * <p>
 * A two state transition that freezes the starting state at its current position and blends that
 * over time with a target state. The target state moves forward in time during the blend as normal.
 * </p>
 *
 * XXX: Might be able to make this more efficient by capturing the getCurrentSourceData of stateA
 * and reusing.
 */
public class FrozenTransitionState extends AbstractTwoStateLerpTransition {

  /**
   * Construct a new FrozenTransitionState.
   * 
   * @param the
   *          name of the steady state we want the Animation Layer to be in at the end of the
   *          transition.
   * @param fadeTime
   *          the amount of time we should take to do the transition.
   * @param type
   *          the way we should interpolate the weighting during the transition.
   */
  public FrozenTransitionState(final String targetState, final double fadeTime, final BlendType type) {
    super(targetState, fadeTime, type);
  }

  @Override
  public AbstractFiniteState getTransitionState(final AbstractFiniteState callingState, final AnimationLayer layer) {
    // grab current time as our start
    setStart(layer.getManager().getCurrentGlobalTime());
    // set "frozen" start state
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

    // update only the B state - the first is frozen
    if (getStateB() != null) {
      getStateB().update(globalTime, layer);
    }
  }

  @Override
  public void postUpdate(final AnimationLayer layer) {
    // update only the B state - the first is frozen
    if (getStateB() != null) {
      getStateB().postUpdate(layer);
    }
  }
}
