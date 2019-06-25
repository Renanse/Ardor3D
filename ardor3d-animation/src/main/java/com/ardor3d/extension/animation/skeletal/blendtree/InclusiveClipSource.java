/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.blendtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.google.common.collect.ImmutableList;

/**
 * Similar to a ClipSource, this class samples and returns values from the channels of an AnimationClip.
 * InclusiveClipSource further filters this result set, excluding any channels whose names do not match those in our
 * enabledChannels list.
 */
public class InclusiveClipSource extends ClipSource {

    /** Our List of channels to include by name. */
    private final List<String> _enabledChannels = new ArrayList<>();

    /**
     * Construct a new source. Clip and Manager must be set separately before use.
     */
    public InclusiveClipSource() {}

    /**
     * Construct a new source using the given clip and manager.
     * 
     * @param clip
     *            our source clip.
     * @param manager
     *            the manager used to track clip state.
     */
    public InclusiveClipSource(final AnimationClip clip, final AnimationManager manager) {
        super(clip, manager);
    }

    /**
     * Clears all enabled channels/joints from this source.
     */
    public void clearEnabled() {
        _enabledChannels.clear();
    }

    /**
     * @param enabledChannels
     *            a list of channel names to include when returning clip results.
     */
    public void addEnabledChannels(final String... enabledChannels) {
        for (final String channelName : enabledChannels) {
            if (!_enabledChannels.contains(channelName)) {
                _enabledChannels.add(channelName);
            }
        }
    }

    /**
     * @param enabledJoints
     *            a list of joint indices to include when returning clip results. These are converted to channel names
     *            and stored in our enabledChannels list.
     */
    public void addEnabledJoints(final int... enabledJoints) {
        for (final int i : enabledJoints) {
            final String channelName = JointChannel.JOINT_CHANNEL_NAME + i;
            if (!_enabledChannels.contains(channelName)) {
                _enabledChannels.add(channelName);
            }
        }
    }

    /**
     * @return a COPY of the enabled channel list.
     */
    public ImmutableList<String> getEnabledChannels() {
        return ImmutableList.copyOf(_enabledChannels);
    }

    @Override
    public Map<String, ? extends Object> getSourceData(final AnimationManager manager) {
        final Map<String, ? extends Object> orig = super.getSourceData(manager);

        // make a copy, only bringing across specific channels
        final Map<String, Object> data = new HashMap<>();
        if (_enabledChannels != null) {
            for (final String key : _enabledChannels) {
                if (orig.containsKey(key)) {
                    data.put(key, orig.get(key));
                }
            }
        }

        return data;
    }
}
