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

import java.util.List;

import com.ardor3d.input.ControllerEvent;
import com.ardor3d.input.ControllerState;
import com.google.common.base.Predicate;

public final class ControllerComponentCondition implements Predicate<TwoInputStates> {

    private int controllerIndex = -1;
    private int componentIndex = -1;
    private String controllerName = null;
    private String componentName = null;

    public ControllerComponentCondition(final int controller, final int component) {
        controllerIndex = controller;
        componentIndex = component;
    }

    public ControllerComponentCondition(final String controller, final String component) {
        controllerName = controller;
        componentName = component;
    }

    public boolean apply(final TwoInputStates states) {
        boolean apply = false;
        final ControllerState currentState = states.getCurrent().getControllerState();
        final ControllerState previousState = states.getPrevious().getControllerState();

        if (!previousState.equals(currentState)) {

            if (controllerName == null) {
                controllerName = currentState.getControllerNames().get(controllerIndex);
            }
            if (componentName == null) {
                componentName = currentState.getControllerComponentNames(controllerName).get(componentIndex);
            }

            final List<ControllerEvent> events = currentState.getEvents();
            for (final ControllerEvent event : events) {
                if (event.getControllerName().equals(controllerName) && event.getComponentName().equals(componentName)) {
                    apply = true;
                }
            }
        }
        return apply;
    }

}
