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
 * Base class for transition states - states responsible for moving between other finite states.
 */
public abstract class AbstractTransitionState extends AbstractFiniteState {

    /**
     * @see {@link AbstractTransitionState#setStartWindow(double)}
     */
    private double _startWindow = -1;

    /**
     * @see {@link AbstractTransitionState#setEndWindow(double)}
     */
    private double _endWindow = -1;

    /**
     * The name of the steady state we want the Animation Layer to be in at the end of the transition.
     */
    private final String _targetState;

    /**
     * Construct a new transition state.
     * 
     * @param targetState
     *            the name of the steady state we want the Animation Layer to be in at the end of the transition.
     */
    protected AbstractTransitionState(final String targetState) {
        _targetState = targetState;
    }

    /**
     * @return the name of the steady state we want the Animation Layer to be in at the end of the transition.
     */
    public String getTargetState() {
        return _targetState;
    }

    /**
     * @param startWindow
     *            our new start window value. If greater than 0, this transition is only valid if the current time is >=
     *            startWindow. Note that animations are separate from states, so time scaling an animation will not
     *            affect transition windows directly and must be factored into the start/end values.
     */
    public void setStartWindow(final double startWindow) {
        _startWindow = startWindow;
    }

    /**
     * @return our start window value.
     * @see #setStartWindow(double)
     */
    public double getStartWindow() {
        return _startWindow;
    }

    /**
     * @param endWindow
     *            our new end window value. If greater than 0, this transition is only valid if the current time is <=
     *            endWindow. Note that animations are separate from states, so time scaling an animation will not affect
     *            transition windows directly and must be factored into the start/end values.
     */
    public void setEndWindow(final double endWindow) {
        _endWindow = endWindow;
    }

    /**
     * @return our end window value.
     * @see #setEndWindow(double)
     */
    public double getEndWindow() {
        return _endWindow;
    }

    /**
     * Request that this state perform a transition to another.
     * 
     * @param callingState
     *            the state calling for this transition.
     * @param layer
     *            the layer our state belongs to.
     * @return the new state to transition to. May be null if the transition was not possible or was ignored for some
     *         reason.
     */
    public final AbstractFiniteState doTransition(final AbstractFiniteState callingState, final AnimationLayer layer) {
        if (layer.getCurrentState() == null) {
            return null;
        }
        final double time = layer.getManager().getCurrentGlobalTime() - layer.getCurrentState().getGlobalStartTime();
        if (isInTimeWindow(time)) {
            return getTransitionState(callingState, layer);
        } else {
            return null;
        }
    }

    /**
     * @param localTime
     *            the state's local time
     * @return true if the given time lands within our window.
     */
    private boolean isInTimeWindow(final double localTime) {
        if (getStartWindow() <= 0) {
            if (getEndWindow() <= 0) {
                // no window, so true
                return true;
            } else {
                // just check end
                return localTime <= getEndWindow();
            }
        } else {
            if (getEndWindow() <= 0) {
                // just check start
                return localTime >= getStartWindow();
            } else if (getStartWindow() <= getEndWindow()) {
                // check between start and end
                return getStartWindow() <= localTime && localTime <= getEndWindow();
            } else {
                // start is greater than end, so there are two windows.
                return localTime >= getStartWindow() || localTime <= getEndWindow();
            }
        }
    }

    /**
     * Do the transition logic for this transition state.
     * 
     * @param callingState
     *            the state calling for this transition.
     * @param layer
     *            the layer our state belongs to.
     * @return the state to transition to. Often ourselves.
     */
    abstract AbstractFiniteState getTransitionState(AbstractFiniteState callingState, AnimationLayer layer);
}