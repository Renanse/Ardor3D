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

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.InputState;
import com.ardor3d.input.Key;
import com.google.common.base.Predicate;

/**
 * A condition that is true when a key was released from the previous to the current input state.
 */
@Immutable
public final class KeyReleasedCondition implements Predicate<TwoInputStates> {
    private final Key key;

    /**
     * Construct a new KeyReleasedCondition.
     * 
     * @param key
     *            the key that should be held
     * @throws NullPointerException
     *             if the key is null
     */
    public KeyReleasedCondition(final Key key) {
        if (key == null) {
            throw new NullPointerException();
        }

        this.key = key;
    }

    public boolean apply(final TwoInputStates states) {
        final InputState currentState = states.getCurrent();
        final InputState previousState = states.getPrevious();

        return currentState.getKeyboardState().getKeysReleasedSince(previousState.getKeyboardState()).contains(key);
    }
}