/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal;

import com.ardor3d.extension.animation.skeletal.clip.TriggerCallback;
import com.ardor3d.scenegraph.Spatial;

/**
 * Describes a class that can take information from a manager and its current layers and state and apply it to a given
 * SkeletonPose. The class should not update or modify the manager, but should merely request current state (usually via
 * <i>manager.getCurrentSourceData();</i>)
 */
public interface AnimationApplier {

    /**
     * Apply the current status of the manager to our SkeletonPose.
     * 
     * @param applyToPose
     *            the pose to apply to
     * @param manager
     *            the animation manager to pull state from.
     */
    void applyTo(SkeletonPose applyToPose, AnimationManager manager);

    /**
     * Apply the current status of the manager to non-skeletal assets.
     * 
     * @param root
     *            the root of the scene graph we will apply to.
     * @param manager
     *            the animation manager to pull state from.
     */
    void apply(Spatial root, AnimationManager manager);

    /**
     * Add a trigger callback to our callback list.
     * 
     * @param key
     *            the key to add a callback to
     * @param callback
     *            the callback logic to add.
     */
    void addTriggerCallback(final String key, final TriggerCallback callback);

    /**
     * Remove a trigger callback from our callback list for a specific key.
     * 
     * @param key
     *            the key to remove from
     * @param callback
     *            the callback logic to remove.
     * @return true if the callback was found to remove
     */
    boolean removeTriggerCallback(final String key, final TriggerCallback callback);

}
