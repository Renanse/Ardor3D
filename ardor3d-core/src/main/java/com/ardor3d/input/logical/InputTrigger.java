/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.framework.Canvas;
import com.google.common.base.Predicate;

/**
 * Defines an action to be performed when a specific input condition is met.
 */
@Immutable
public final class InputTrigger {
    private final Predicate<TwoInputStates> _condition;
    private final TriggerAction _action;
    private String _id;

    /**
     * Construct a new InputTrigger with the given condition and action.
     * 
     * @param condition
     *            the predicate to test for this trigger
     * @param action
     *            the action to take if the predicate returns true.
     */
    public InputTrigger(final Predicate<TwoInputStates> condition, final TriggerAction action) {
        _condition = condition;
        _action = action;
    }

    /**
     * Construct a new InputTrigger with the given condition and action.
     * 
     * @param condition
     *            the predicate to test for this trigger
     * @param action
     *            the action to take if the predicate returns true.
     * @param id
     *            an id, useful for identifying this trigger for deregistration, etc.
     */
    public InputTrigger(final Predicate<TwoInputStates> condition, final TriggerAction action, final String id) {
        _condition = condition;
        _action = action;
        _id = id;
    }

    /**
     * Checks if the condition is applicable, and if so, performs the action.
     * 
     * @param source
     *            the Canvas that was the source of the current input
     * @param states
     *            the input states to check
     * @param tpf
     *            the time per frame in seconds
     */
    void performIfValid(final Canvas source, final TwoInputStates states, final double tpf) {
        if (_condition.apply(states)) {
            _action.perform(source, states, tpf);
        }
    }

    /**
     * @param id
     *            the id to set. This id can be used to uniquely identify a trigger.
     */
    public void setId(final String id) {
        _id = id;
    }

    /**
     * @return the id set, or null if none was set.
     */
    public String getId() {
        return _id;
    }
}
