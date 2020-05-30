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

import java.util.Map;
import java.util.function.Predicate;

public final class ControllerComponentActiveCondition implements Predicate<TwoInputStates> {

  private final String controllerName;
  private final String[] componentNames;

  public ControllerComponentActiveCondition(final String controller, final String... components) {
    controllerName = controller;
    componentNames = components;
  }

  @Override
  public boolean test(final TwoInputStates states) {
    final Map<String, Float> currentStates =
        states.getCurrent().getControllerState().getControllerComponentValues(controllerName);
    final Map<String, Float> previousStates =
        states.getPrevious().getControllerState().getControllerComponentValues(controllerName);

    if (currentStates == null) {
      return false;
    }

    Float prev, curr;
    for (final String component : componentNames) {
      curr = currentStates.get(component);
      if (curr == null) {
        continue;
      }
      if (curr.floatValue() != 0) {
        return true;
      }

      if (previousStates != null) {
        prev = previousStates.get(component);

        if (prev != null && curr.floatValue() != prev.floatValue()) {
          return true;
        }
      }
    }

    return false;
  }
}
