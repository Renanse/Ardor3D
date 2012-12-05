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

import java.util.EnumMap;

import com.google.common.collect.Maps;

public enum MouseButton {
    LEFT, RIGHT, MIDDLE;

    public static EnumMap<MouseButton, ButtonState> makeMap(final ButtonState left, final ButtonState right,
            final ButtonState middle) {
        if (left == null) {
            throw new NullPointerException("left");
        }
        if (right == null) {
            throw new NullPointerException("right");
        }
        if (middle == null) {
            throw new NullPointerException("middle");
        }
        final EnumMap<MouseButton, ButtonState> map = Maps.newEnumMap(MouseButton.class);
        map.put(LEFT, left);
        map.put(RIGHT, right);
        map.put(MIDDLE, middle);
        return map;
    }
}
