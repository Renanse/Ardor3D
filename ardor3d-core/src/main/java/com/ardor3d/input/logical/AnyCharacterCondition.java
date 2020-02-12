/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.logical;

import java.util.function.Predicate;

import com.ardor3d.input.InputState;

public class AnyCharacterCondition implements Predicate<TwoInputStates> {
    public boolean test(final TwoInputStates twoInputStates) {
        final InputState currentState = twoInputStates.getCurrent();

        return !currentState.getCharacterState().getEvents().isEmpty();
    }
}
