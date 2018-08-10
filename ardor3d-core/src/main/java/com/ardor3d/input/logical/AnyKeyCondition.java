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

import com.ardor3d.input.InputState;
import com.google.common.base.Predicate;

/**
 * Applicable whenever 'any' key has been pressed.
 */
public class AnyKeyCondition implements Predicate<TwoInputStates> {
    public boolean apply(final TwoInputStates twoInputStates) {
        final InputState currentState = twoInputStates.getCurrent();
        final InputState previousState = twoInputStates.getPrevious();

        return !currentState.getKeyboardState().getKeysPressedSince(previousState.getKeyboardState()).isEmpty();
    }
}
