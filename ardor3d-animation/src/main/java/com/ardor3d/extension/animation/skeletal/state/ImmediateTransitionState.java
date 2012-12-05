/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.state;

import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.layer.AnimationLayer;
import com.google.common.collect.Maps;

/**
 * Cuts directly to the set target state, without any intermediate transition action.
 */
public class ImmediateTransitionState extends AbstractTransitionState {

    /**
     * Construct a new transition state.
     * 
     * @param targetState
     *            the name of the state to transition to.
     */
    public ImmediateTransitionState(final String targetState) {
        super(targetState);
    }

    @Override
    public AbstractFiniteState getTransitionState(final AbstractFiniteState callingState, final AnimationLayer layer) {
        // Pull our state from the layer
        final AbstractFiniteState state = layer.getSteadyState(getTargetState());
        if (state == null) {
            return null;
        }
        // Reset to start
        state.resetClips(layer.getManager());
        // return state.
        return state;
    }

    /**
     * Ignored.
     */
    @Override
    public Map<String, ? extends Object> getCurrentSourceData(final AnimationManager manager) {
        return Maps.newHashMap();
    }

    /**
     * Ignored.
     */
    @Override
    public void update(final double globalTime, final AnimationLayer layer) {}

    @Override
    public void postUpdate(final AnimationLayer layer) {}
}
