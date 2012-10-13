/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ardor3d.annotation.Immutable;
import com.google.common.collect.Maps;

@Immutable
public class ControllerState {

    public static final ControllerState NOTHING = new ControllerState();

    private final Map<String, Map<String, Float>> controllerStates = new LinkedHashMap<String, Map<String, Float>>();
    private final List<ControllerEvent> eventsSinceLastState = new ArrayList<ControllerEvent>();

    /**
     * Sets a components state
     */
    public void set(final String controllerName, final String componentName, final float value) {
        Map<String, Float> controllerState = null;
        if (controllerStates.containsKey(controllerName)) {
            controllerState = controllerStates.get(controllerName);
        } else {
            controllerState = new LinkedHashMap<String, Float>();
            controllerStates.put(controllerName, controllerState);
        }

        controllerState.put(componentName, value);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ControllerState) {
            final ControllerState other = (ControllerState) obj;
            return other.controllerStates.equals(controllerStates);
        }

        return false;
    }

    @Override
    public String toString() {
        final StringBuilder stateString = new StringBuilder("ControllerState: ");

        for (final String controllerStateKey : controllerStates.keySet()) {
            final Map<String, Float> state = controllerStates.get(controllerStateKey);
            stateString.append("[").append(controllerStateKey);
            for (final String stateKey : state.keySet()) {
                stateString.append("[").append(stateKey).append(":").append(state.get(stateKey)).append("]");
            }
            stateString.append("]");
        }

        return stateString.toString();
    }

    public ControllerState snapshot() {
        final ControllerState snapshot = new ControllerState();
        duplicateStates(snapshot.controllerStates);
        snapshot.eventsSinceLastState.addAll(eventsSinceLastState);

        return snapshot;
    }

    private void duplicateStates(final Map<String, Map<String, Float>> store) {
        store.clear();
        for (final Entry<String, Map<String, Float>> entry : controllerStates.entrySet()) {
            store.put(entry.getKey(), Maps.newLinkedHashMap(entry.getValue()));
        }
    }

    public void addEvent(final ControllerEvent event) {
        eventsSinceLastState.add(event);
        set(event.getControllerName(), event.getComponentName(), event.getValue());
    }

    public List<ControllerEvent> getEvents() {
        Collections.sort(eventsSinceLastState, new Comparator<ControllerEvent>() {
            public int compare(final ControllerEvent o1, final ControllerEvent o2) {
                return (int) (o2.getNanos() - o1.getNanos());
            }
        });

        return Collections.unmodifiableList(eventsSinceLastState);
    }

    public void clearEvents() {
        eventsSinceLastState.clear();
    }

    public List<String> getControllerNames() {
        return new ArrayList<String>(controllerStates.keySet());
    }

    public List<String> getControllerComponentNames(final String controller) {
        return new ArrayList<String>(controllerStates.get(controller).keySet());
    }
}
