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
 * Represents a node in a blend tree.
 */
public interface BlendTreeSource {

    /**
     * @param manager
     *            the manager this is being called from.
     * @return a map of source information from the blend tree node.
     */
    Map<String, ? extends Object> getSourceData(AnimationManager manager);

    /**
     * Move any clips or animation information to the given global time.
     * 
     * @param globalTime
     *            our new "global" timeline time.
     * @param manager
     *            the manager this is being called from.
     * @return true if we found at least one active clip in the tree
     */
    boolean setTime(double globalTime, AnimationManager manager);

    /**
     * Reset any clips in this tree. This sets the start time to the given time and sets it active.
     * 
     * @param manager
     *            the manager to use in resetting the clips.
     * @param globalStartTime
     *            the new start time to use.
     */
    void resetClips(AnimationManager manager, double globalStartTime);

    /**
     * Check if there are still active clips in the tree.
     * 
     * @param manager
     *            the manager this is being called from.
     * @return true if we found at least one active clip in the tree
     */
    boolean isActive(AnimationManager manager);

    public boolean isInEndingWindow(final double window);

}
