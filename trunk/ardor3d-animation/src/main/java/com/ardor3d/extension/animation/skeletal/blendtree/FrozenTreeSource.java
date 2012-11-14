/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.blendtree;

import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;

/**
 * A blend tree node that does not update any clips or sources below it in the blend tree. This is useful for freezing
 * an animation, often for purposes of transitioning between two unrelated animations.
 */
public class FrozenTreeSource implements BlendTreeSource {

    /** Our sub source. */
    private final BlendTreeSource _source;

    /** The time we are frozen at. */
    private final double _time;

    public FrozenTreeSource(final BlendTreeSource source, final double frozenTime) {
        _source = source;
        _time = frozenTime;
    }

    public Map<String, ? extends Object> getSourceData(final AnimationManager manager) {
        return _source.getSourceData(manager);
    }

    /**
     * Ignores the command to reset our subtree.
     */
    public void resetClips(final AnimationManager manager, final double globalStartTime) {
        _source.resetClips(manager, 0);
    }

    /**
     * Ignores the command to set time on our subtree
     */
    public boolean setTime(final double globalTime, final AnimationManager manager) {
        _source.setTime(_time, manager);
        return true;
    }

    @Override
    public boolean isActive(final AnimationManager manager) {
        return true;
    }

    @Override
    public boolean isInEndingWindow(double window) {
        return false;
}
}
