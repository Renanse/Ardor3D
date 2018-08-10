/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal;

import com.ardor3d.extension.animation.skeletal.AnimationManager.AnimationUpdateState;

public interface AnimationUpdateStateListener {

    public void stateChanged(AnimationUpdateState oldState, AnimationUpdateState newState);

}
