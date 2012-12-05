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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Immutable
public class ControllerState {

    public static final ControllerState NOTHING = new ControllerState(0);

    protected final Map<String, Map<String, Float>> _controllerStates = Maps.newLinkedHashMap();
    protected final List<ControllerEvent> _eventsSinceLastState = Lists.newArrayList();

    protected ControllerState(final int ignore) {}

    public ControllerState() {
        ControllerState.NOTHING.duplicateStates(_controllerStates);
    }

    public ControllerState(final ControllerState previous) {
        if (previous != null) {
            previous.duplicateStates(_controllerStates);
        } else {
            ControllerState.NOTHING.duplicateStates(_controllerStates);
        }
    }

    /**
     * Sets a components state
     */
    public void set(final String controllerName, final String componentName, final float value) {
        Map<String, Float> controllerState = null;
        if (_controllerStates.containsKey(controllerName)) {
            controllerState = _controllerStates.get(controllerName);
        } else {
            controllerState = new LinkedHashMap<String, Float>();
            _controllerStates.put(controllerName, controllerState);
        }

        controllerState.put(componentName, value);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ControllerState) {
            final ControllerState other = (ControllerState) obj;
            return other._controllerStates.equals(_controllerStates);
        }

        return false;
    }

    @Override
    public String toString() {
        final StringBuilder stateString = new StringBuilder("ControllerState: ");

        for (final String controllerStateKey : _controllerStates.keySet()) {
            final Map<String, Float> state = _controllerStates.get(controllerStateKey);
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
        duplicateStates(snapshot._controllerStates);
        snapshot._eventsSinceLastState.addAll(_eventsSinceLastState);

        return snapshot;
    }

    private void duplicateStates(final Map<String, Map<String, Float>> store) {
        store.clear();
        for (final Entry<String, Map<String, Float>> entry : _controllerStates.entrySet()) {
            store.put(entry.getKey(), Maps.newLinkedHashMap(entry.getValue()));
        }
    }

    public void addEvent(final ControllerEvent event) {
        _eventsSinceLastState.add(event);
        set(event.getControllerName(), event.getComponentName(), event.getValue());
    }

    public List<ControllerEvent> getEvents() {
        Collections.sort(_eventsSinceLastState, new Comparator<ControllerEvent>() {
            public int compare(final ControllerEvent o1, final ControllerEvent o2) {
                return (int) (o2.getNanos() - o1.getNanos());
            }
        });

        return Collections.unmodifiableList(_eventsSinceLastState);
    }

    public void clearEvents() {
        _eventsSinceLastState.clear();
    }

    public List<String> getControllerNames() {
        return new ArrayList<String>(_controllerStates.keySet());
    }

    public List<String> getControllerComponentNames(final String controller) {
        return new ArrayList<String>(_controllerStates.get(controller).keySet());
    }

    public Map<String, Float> getControllerComponentValues(final String controller) {
        return _controllerStates.get(controller);
    }

    public float getComponentValue(final String controller, final String component) {
        if (!_controllerStates.containsKey(controller) || !_controllerStates.get(controller).containsKey(component)) {
            return 0;
        }
        return _controllerStates.get(controller).get(component);
    }
}