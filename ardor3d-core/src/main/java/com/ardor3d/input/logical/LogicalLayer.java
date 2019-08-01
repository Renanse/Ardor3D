/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.logical;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.ardor3d.annotation.GuardedBy;
import com.ardor3d.annotation.MainThread;
import com.ardor3d.annotation.ThreadSafe;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.InputState;
import com.ardor3d.input.PhysicalLayer;

/**
 * Implementation of a logical layer on top of the physical one, to be able to more easily trigger certain commands for
 * certain combination of user input.
 */
@ThreadSafe
public final class LogicalLayer {
    private final Set<InputSource> _inputs = new CopyOnWriteArraySet<InputSource>();
    private final Set<InputTrigger> _triggers = new CopyOnWriteArraySet<InputTrigger>();
    private LogicalTriggersApplier _applier = new BasicTriggersApplier();

    public LogicalLayer() {}

    public void registerInput(final Canvas source, final PhysicalLayer physicalLayer) {
        _inputs.add(new InputSource(source, physicalLayer));
    }

    /**
     * Register a trigger for evaluation when the {@link #checkTriggers(double)} method is called.
     * 
     * @param inputTrigger
     *            the trigger to check
     */
    public void registerTrigger(final InputTrigger inputTrigger) {
        _triggers.add(inputTrigger);
    }

    /**
     * Deregister a trigger for evaluation when the {@link #checkTriggers(double)} method is called.
     * 
     * @param inputTrigger
     *            the trigger to stop checking
     */
    public void deregisterTrigger(final InputTrigger inputTrigger) {
        _triggers.remove(inputTrigger);
    }

    /**
     * Check all registered triggers to see if their respective conditions are met. For every trigger whose condition is
     * true, perform the associated action.
     * 
     * @param tpf
     *            time per frame in seconds
     */
    @MainThread
    public synchronized void checkTriggers(final double tpf) {
        for (final InputSource is : _inputs) {
            is.physicalLayer.readState();

            final List<InputState> newStates = is.physicalLayer.drainAvailableStates();

            if (newStates.isEmpty()) {
                _applier.checkAndPerformTriggers(_triggers, is.source, new TwoInputStates(is.lastState, is.lastState),
                        tpf);
            } else {
                // used to spread tpf evenly among triggered actions
                final double time = newStates.size() > 1 ? tpf / newStates.size() : tpf;
                for (final InputState inputState : newStates) {
                    // no trigger is valid in the LOST_FOCUS state, so don't bother checking them
                    if (inputState != InputState.LOST_FOCUS) {
                        _applier.checkAndPerformTriggers(_triggers, is.source, new TwoInputStates(is.lastState,
                                inputState), time);
                    }

                    is.lastState = inputState;
                }
            }
        }
    }

    public void setApplier(final LogicalTriggersApplier applier) {
        _applier = applier;
    }

    public LogicalTriggersApplier getApplier() {
        return _applier;
    }

    private static class InputSource {
        private final Canvas source;
        private final PhysicalLayer physicalLayer;
        @GuardedBy("LogicalLayer.this")
        private InputState lastState;

        public InputSource(final Canvas source, final PhysicalLayer physicalLayer) {
            this.source = source;
            this.physicalLayer = physicalLayer;
            lastState = InputState.EMPTY;
        }
    }

    public Set<InputTrigger> getTriggers() {
        return _triggers;
    }

    public InputTrigger findTriggerById(final String id) {
        for (final InputTrigger trigger : _triggers) {
            if (id.equals(trigger.getId())) {
                return trigger;
            }
        }
        return null;
    }
}
