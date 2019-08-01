/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
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
 * Dummy transition - does not change current state.
 */
public class IgnoreTransitionState extends AbstractTransitionState {

    /**
     * Construct a new transition state.
     * 
     * @param targetState
     *            the name of the state to transition to.
     */
    public IgnoreTransitionState() {
        super(null);
    }

    @Override
    public AbstractFiniteState getTransitionState(final AbstractFiniteState callingState, final AnimationLayer layer) {
        // return calling state.
        return callingState;
    }

    /**
     * Ignored.
     */
    @Override
    public Map<String, ? extends Object> getCurrentSourceData(final AnimationManager manager) {
        return null;
    }

    /**
     * Ignored.
     */
    @Override
    public void update(final double globalTime, final AnimationLayer layer) {}

    @Override
    public void postUpdate(final AnimationLayer layer) {}
}
