/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.controller;

import java.util.ArrayList;
import java.util.List;

public class ControllerInfo {

    private final String _controllerName;
    private final ArrayList<String> _axisNames;
    private final ArrayList<String> _buttonNames;

    public ControllerInfo(final String controllerName, final List<String> axisNames, final List<String> buttonNames) {
        _controllerName = controllerName;
        _axisNames = new ArrayList<>(axisNames);
        _buttonNames = new ArrayList<>(buttonNames);
    }

    public String getControllerName() {
        return _controllerName;
    }

    public ArrayList<String> getAxisNames() {
        return _axisNames;
    }

    public ArrayList<String> getButtonNames() {
        return _buttonNames;
    }

    public int getAxisCount() {
        return _axisNames.size();
    }

    public int getButtonCount() {
        return _buttonNames.size();
    }

    @Override
    public String toString() {
        return "Controller '" + _controllerName + "' with Axis " + _axisNames + " and Buttons " + _buttonNames;
    }
}