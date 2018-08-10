/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.effect;

/**
 * A specific instruction in a RenderEffect.
 */
public interface EffectStep {

    /**
     * Apply this step.
     * 
     * @param manager
     */
    public void apply(final EffectManager manager);

}
