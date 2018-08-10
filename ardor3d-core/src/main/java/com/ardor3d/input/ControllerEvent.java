/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input;

public class ControllerEvent {

    private final long nanos;
    private final String controllerName;
    private final String componentName;
    private final float value;

    public ControllerEvent(final long nanos, final String controllerName, final String componentName, final float value) {
        this.nanos = nanos;
        this.controllerName = controllerName;
        this.componentName = componentName;
        this.value = value;
    }

    public long getNanos() {
        return nanos;
    }

    public String getControllerName() {
        return controllerName;
    }

    public String getComponentName() {
        return componentName;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ControllerEvent: " + controllerName + ", " + componentName + ", " + value + ", " + nanos;
    }
}
