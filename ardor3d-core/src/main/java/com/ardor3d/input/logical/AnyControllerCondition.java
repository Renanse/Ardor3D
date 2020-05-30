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

import com.ardor3d.input.controller.ControllerState;

public final class AnyControllerCondition implements Predicate<TwoInputStates> {

  @Override
  public boolean test(final TwoInputStates states) {
    final ControllerState oldState = states.getPrevious().getControllerState();
    final ControllerState currentState = states.getCurrent().getControllerState();

    final boolean apply = !oldState.equals(currentState);
    return apply;
  }

}
