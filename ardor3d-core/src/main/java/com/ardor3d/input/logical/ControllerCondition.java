/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.logical;

import java.util.List;

import com.ardor3d.input.ControllerEvent;
import com.ardor3d.input.ControllerState;
import com.google.common.base.Predicate;

public final class ControllerCondition implements Predicate<TwoInputStates> {

    private int controllerIndex = -1;
    private String controllerName = null;

    public ControllerCondition(final int controller) {
        controllerIndex = controller;
    }

    public ControllerCondition(final String controller) {
        controllerName = controller;
    }

    public boolean apply(final TwoInputStates states) {
        boolean apply = false;
        final ControllerState currentState = states.getCurrent().getControllerState();
        final ControllerState previousState = states.getPrevious().getControllerState();

        if (!previousState.equals(currentState)) {
            if (controllerName == null) {
                controllerName = currentState.getControllerNames().get(controllerIndex);
            }
            final List<ControllerEvent> events = currentState.getEvents();
            for (final ControllerEvent event : events) {
                if (event.getControllerName().equals(controllerName)) {
                    apply = true;
                }
            }
        }
        return apply;
    }

}
