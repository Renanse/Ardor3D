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

import java.util.List;
import java.util.Map;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Similar to a ClipSource, this class samples and returns values from the channels of an AnimationClip.
 * ExclusiveClipSource further filters this result set, excluding a given set of channels by name.
 */
public class ExclusiveClipSource extends ClipSource {

    /** Our List of channels to exclude by name. */
    private final List<String> _disabledChannels = Lists.newArrayList();

    /**
     * Construct a new source. Clip and Manager must be set separately before use.
     */
    public ExclusiveClipSource() {}

    /**
     * Construct a new source using the given clip and manager.
     * 
     * @param clip
     *            our source clip.
     * @param manager
     *            the manager used to track clip state.
     */
    public ExclusiveClipSource(final AnimationClip clip, final AnimationManager manager) {
        super(clip, manager);
    }

    /**
     * Clears all disabled channels/joints from this source.
     */
    public void clearDisabled() {
        _disabledChannels.clear();
    }

    /**
     * @param disabledChannels
     *            a list of channel names to exclude when returning clip results.
     */
    public void addDisabledChannels(final String... disabledChannels) {
        for (final String channelName : disabledChannels) {
            if (!_disabledChannels.contains(channelName)) {
                _disabledChannels.add(channelName);
            }
        }
    }

    /**
     * @param disabledJoints
     *            a list of joint indices to exclude when returning clip results. These are converted to channel names
     *            and stored in our disabledChannels list.
     */
    public void addDisabledJoints(final int... disabledJoints) {
        for (final int i : disabledJoints) {
            final String channelName = JointChannel.JOINT_CHANNEL_NAME + i;
            if (!_disabledChannels.contains(channelName)) {
                _disabledChannels.add(channelName);
            }
        }
    }

    /**
     * @return a COPY of the disabled channel list.
     */
    public ImmutableList<String> getDisabledChannels() {
        return ImmutableList.copyOf(_disabledChannels);
    }

    @Override
    public Map<String, ? extends Object> getSourceData(final AnimationManager manager) {
        final Map<String, ? extends Object> orig = super.getSourceData(manager);

        // make a copy, removing specific channels
        final Map<String, ? extends Object> data = Maps.newHashMap(orig);
        if (_disabledChannels != null) {
            for (final String key : _disabledChannels) {
                data.remove(key);
            }
        }

        return data;
    }
}
