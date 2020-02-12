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
 * A transition that blends over a given time from one animation state to another, synchronizing the target state to the
 * initial state's start time. This is best used with two clips that have similar motions.
 */
public class SyncFadeTransitionState extends FadeTransitionState {

    /**
     * Construct a new SyncFadeTransitionState.
     * 
     * @param targetState
     *            the name of the steady state we want the Animation Layer to be in at the end of the transition.
     * @param fadeTime
     *            the amount of time we should take to do the transition.
     * @param type
     *            the way we should interpolate the weighting during the transition.
     */
    public SyncFadeTransitionState(final String targetState, final double fadeTime, final BlendType type) {
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
        // grab current state's start time and set on end state
        getStateB().resetClips(layer.getManager(), getStateA().getGlobalStartTime());
        return this;
    }
}
