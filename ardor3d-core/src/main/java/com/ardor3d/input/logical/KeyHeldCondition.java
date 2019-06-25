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

import java.util.function.Predicate;

import com.ardor3d.annotation.Immutable;
import com.ardor3d.input.Key;

/**
 * A condition that is true when a key is down in the current input state.
 */
@Immutable
public final class KeyHeldCondition implements Predicate<TwoInputStates> {
    private final Key key;

    /**
     * Construct a new KeyHeldCondition.
     *
     * @param key
     *            the key that should be held
     * @throws NullPointerException
     *             if the key is null
     */
    public KeyHeldCondition(final Key key) {
        if (key == null) {
            throw new NullPointerException();
        }

        this.key = key;
    }

    public boolean test(final TwoInputStates states) {
        return states.getCurrent().getKeyboardState().isDown(key);
    }
}
