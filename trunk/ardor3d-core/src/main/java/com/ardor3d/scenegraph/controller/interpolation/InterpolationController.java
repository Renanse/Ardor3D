/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.controller.interpolation;

import java.util.Arrays;
import java.util.List;

import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.ComplexSpatialController;

/**
 * InterpolationController class is an abstract class containing all the stuff common to controllers interpolating
 * things.
 * <p>
 * Implementation note: This class is comprised of quite a few protected methods, this is mainly to allow maximum
 * flexibility for overriding classes.
 * </p>
 * 
 * @param <C>
 *            The control 'points' being interpolated, for example Vectors or Quaternions.
 * @param <T>
 *            The object this controller will perform the interpolation on, for example Spatials.
 */
public abstract class InterpolationController<C, T extends Spatial> extends ComplexSpatialController<T> {

    /** Serial UID */
    private static final long serialVersionUID = 1L;

    /** The minimum allowed delta */
    public static final double DELTA_MIN = 0.0;

    /** The maximum allowed delta */
    public static final double DELTA_MAX = 1.0;

    /** @see #setControls(List) */
    private List<C> _controls = null;

    /** @see #setIndex(int) */
    private int _index = getMinimumIndex();

    /** @see #setDelta(double) */
    private double _delta = DELTA_MIN;

    /** @see #setCycleForward(boolean) */
    private boolean _cycleForward = true;

    /**
     * This method is automatically called by {@link #update(double, Spatial)} from this controller.
     * 
     * @param from
     *            The control to interpolate from.
     * @param to
     *            The control to interpolate to.
     * @param delta
     *            The distance between <code>from</code> and <code>to</code>, will be between <code>0.0</code> and
     *            <code>1.0</code> (inclusive).
     * @param caller
     *            The object to interpolate, will not be <code>null</code>.
     */
    protected abstract void interpolate(C from, C to, double delta, T caller);

    /**
     * Interpolates on the set {@link #getControls() controls}.
     * <p>
     * It will only update the given object if this controller is {@link #isActive() active}, caller isn't
     * <code>null</code> and it's {@link #getSpeed() speed} is greater than zero.
     * </p>
     * 
     * @param time
     *            The passed since this controller was last called.
     * @param caller
     *            The object to update, if this is <code>null</code> nothing will be updated.
     */
    @Override
    public void update(final double time, final T caller) {
        if (shouldUpdate(time, caller)) {

            updateDeltaAndIndex(time);

            assert (getDelta() >= DELTA_MIN) : "delta is less than " + DELTA_MIN
                    + ", updateDeltaAndIndex() has probably been overriden incorrectly";
            assert (getDelta() <= DELTA_MAX) : "delta is greater than " + DELTA_MAX
                    + ", updateDeltaAndIndex() has probably been overriden incorrectly";

            clampIndex();

            assert (getIndex() < getControls().size()) : "_index was greater than the number of controls, clampIndex() has probably been overriden incorrectly";
            assert (getIndex() >= 0) : "_index was negative, clampIndex() has probably been overriden incorrectly";

            interpolate(getControlFrom(), getControlTo(), getDelta(), caller);
        }
    }

    private boolean shouldUpdate(final double time, final T caller) {
        return isActive() && null != caller && time > 0.0 && getSpeed() > 0.0 && !isClamped();
    }

    /**
     * @return The minimum allowed index. By default it returns 0.
     */
    protected int getMinimumIndex() {
        return 0;
    }

    /**
     * @return The maximum allowed index. By default it returns the last index in the {@link #getControls() control}
     *         list.
     */
    protected int getMaximumIndex() {
        return getControls().size() - 1;
    }

    /**
     * @param controls
     *            The new controls to set, can not be <code>null</code> or empty.
     * @see #getControls()
     */
    public void setControls(final List<C> controls) {
        if (null == controls) {
            throw new IllegalArgumentException("controls can not be null!");
        }
        if (controls.isEmpty()) {
            throw new IllegalArgumentException("controls can not be empty!");
        }

        _controls = controls;
    }

    /**
     * @param controlArray
     *            The new values to set, can not be <code>null</code> or size 0.
     * @see #getControls()
     */
    public void setControls(final C... controlArray) {
        if (null == controlArray) {
            throw new IllegalArgumentException("controlArray can not be null!");
        }

        setControls(Arrays.<C> asList(controlArray));
    }

    /**
     * @return The controls getting interpolated between, will not be <code>null</code> or empty.
     * @see #setControls(List)
     */
    public List<C> getControls() {
        assert (null != _controls) : "_controls was null, it must be set before use!";
        assert (!_controls.isEmpty()) : "_controls was empty, it must contain at least 1 element for this class to work!";

        return _controls;
    }

    /**
     * Updates the {@link #getDelta() delta} and {@link #getIndex() index}.
     * 
     * @param time
     *            The raw time since this was last called.
     */
    protected void updateDeltaAndIndex(final double time) {
        incrementDelta(getSpeed() * time);

        /* If >= DELTA_MAX then we need to start interpolating between next set of points */
        while (getDelta() >= DELTA_MAX) {
            /* Adjust delta for new set of points */
            decrementDelta(DELTA_MAX);

            /* Increment/decrement current index based on whether we are cycling forward or backwards */
            if (isCycleForward()) {
                incrementIndex();
            } else {
                decrementIndex();
            }
        }
    }

    /**
     * Clamps the {@link #getIndex() index} to ensure its not out of bounds.
     * <p>
     * This is called automatically from {@link #update(double, Spatial)} and shouldn't be called manually. It only
     * really exists as a separate method to allow sub classes maximum flexibility. Also of note is the fact that if
     * this method is overridden then {@link #getControlFrom()} and {@link #getControlTo()} methods will also probably
     * need to be overridden as they rely on this method clamping the index correctly before they get called.
     * </p>
     */
    protected void clampIndex() {
        switch (getRepeatType()) {
            case CLAMP:
                if (getIndex() >= getMaximumIndex()) {
                    /* Clamp these just to be on the safe side (overflow) */
                    setIndex(getMaximumIndex());
                    setDelta(DELTA_MAX);
                }
                break;

            case CYCLE:
                if (isCycleForward()) {
                    if (getIndex() >= getMaximumIndex()) {
                        setIndex(getMaximumIndex());
                        setCycleForward(false);
                    }
                } else {
                    if (getIndex() <= getMinimumIndex()) {
                        setIndex(getMinimumIndex());
                        setCycleForward(true);
                    }
                }
                break;

            case WRAP:
                if (getIndex() >= getMaximumIndex()) {
                    setIndex(getMinimumIndex());
                }
                break;
        }
    }

    /**
     * This method assumes the {@link #getIndex() index} has already been {@link #clampIndex() clamped} correctly.
     * 
     * @return The control to interpolate from, will not be <code>null</code>.
     * @see #getControlTo()
     */
    protected C getControlFrom() {
        C from = null;

        switch (getRepeatType()) {
            case CLAMP:
                if (getIndex() > getMaximumIndex()) {
                    from = getControls().get(getMaximumIndex());
                } else {
                    from = getControls().get(getIndex());
                }
                break;

            case CYCLE:
                from = getControls().get(getIndex());
                break;

            case WRAP:
                from = getControls().get(getIndex());
                break;
        }

        return from;
    }

    /**
     * This method assumes the {@link #getIndex() index} has already been {@link #clampIndex() clamped} correctly.
     * 
     * @return The control to interpolate to, will not be <code>null</code>.
     * @see #getControlFrom()
     */
    protected C getControlTo() {
        C to = null;

        switch (getRepeatType()) {
            case CLAMP:
                if (getIndex() >= getMaximumIndex()) {
                    to = getControls().get(getMaximumIndex());
                } else {
                    to = getControls().get(getIndex() + 1);
                }
                break;

            case CYCLE:
                if (isCycleForward()) {
                    to = getControls().get(getIndex() + 1);
                } else {
                    to = getControls().get(getIndex() - 1);
                }
                break;

            case WRAP:
                to = getControls().get(getIndex() + 1);
                break;
        }

        return to;
    }

    /**
     * Increments the index by 1.
     * 
     * @return The new index value as a convenience.
     */
    protected int incrementIndex() {
        return ++_index;
    }

    /**
     * Decrements the index by 1.
     * 
     * @return The new index value as a convenience.
     */
    protected int decrementIndex() {
        return --_index;
    }

    /**
     * @param index
     *            The new index value.
     * @see #getIndex()
     */
    protected void setIndex(final int index) {
        _index = index;
    }

    /**
     * @return The index of the {@link #getControls() control} to interpolate from.
     * @see #setIndex(int)
     */
    protected int getIndex() {
        return _index;
    }

    /**
     * @param by
     *            The amount to increment by, if this is negative it will actually decrement the delta.
     * @return The new delta value as a convenience.
     * @see #decrementDelta(double)
     */
    protected double incrementDelta(final double by) {
        _delta += by;

        return _delta;
    }

    /**
     * @param by
     *            The amount to decrement by, if this is negative it will actually increment the delta.
     * @return The new delta value as a convenience.
     * @see #incrementDelta(double)
     */
    protected double decrementDelta(final double by) {
        _delta -= by;

        return _delta;
    }

    /**
     * @param delta
     *            The new distance between the {@link #getControlFrom() from control} and {@link #getControlTo() to
     *            control} , should be between <code>0.0</code> and <code>1.0</code> (inclusive).
     * @see #getDelta()
     */
    protected void setDelta(final double delta) {
        _delta = delta;
    }

    /**
     * @return The distance between the {@link #getControlFrom() from control} and {@link #getControlTo() to control} ,
     *         will be between <code>0.0</code> and <code>1.0</code> (inclusive).
     * @see #setDelta(double)
     */
    protected double getDelta() {
        return _delta;
    }

    /**
     * @param cycleForward
     *            <code>true</code> to interpolate the controls forwards (1, 2, 3 ...), <code>false</code> to
     *            interpolate the controls backwards (3, 2, 1 ...)
     * @see #isCycleForward()
     */
    protected void setCycleForward(final boolean cycleForward) {
        _cycleForward = cycleForward;
    }

    /**
     * @return <code>true</code> if interpolating the controls forwards (1, 2, 3 ...), <code>false</code> if
     *         interpolating the controls backwards (3, 2, 1 ...)
     * @see #setCycleForward(boolean)
     */
    protected boolean isCycleForward() {
        return _cycleForward;
    }

    /**
     * Also {@link #reset() resets} this controller for safety, because changing the repeat type part way through an
     * interpolation can cause problems.
     * 
     * @param repeatType
     *            The new repeat type to use.
     */
    @Override
    public void setRepeatType(final RepeatType repeatType) {
        if (getRepeatType() != repeatType) {
            /* Reset for safety */
            reset();
        }

        super.setRepeatType(repeatType);
    }

    /**
     * Resets the internal state, namely the cycle direction, delta and index variables.
     */
    public void reset() {
        setCycleForward(true);
        setDelta(DELTA_MIN);
        setIndex(getMinimumIndex());
    }

    /**
     * @return <code>true</code> if this controllers {@link #getRepeatType() repeat type} is
     *         {@link ComplexSpatialController.RepeatType#CLAMP clamp} and its currently clamped at the maximum index.
     */
    public boolean isClamped() {
        return isRepeatTypeClamp() && getIndex() == getMaximumIndex();
    }

}
