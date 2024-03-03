/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.state;

/**
 * Describes a class that holds onto and directly utilizes an AnimationState. This interface is
 * meant to act as a callback so that states can properly handle certain types of transitions such
 * as end transitions - setting new states into the correct place in the later/state hierarchy.
 */
public interface StateOwner {

  /**
   * Replace the given current state with the given new state
   * 
   * @param currentState
   *          the state to replace
   * @param newState
   *          the state to replace it with.
   */
  void replaceState(AbstractFiniteState currentState, AbstractFiniteState newState);

}
