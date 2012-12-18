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
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.clip.JointData;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * This tree source maintains its own source data, which can be modified directly using setJointXXX. This source is
 * meant to be used for controlling a particular joint or set of joints programatically.
 */
public class ManagedTransformSource implements BlendTreeSource {

    /** Our local source data. */
    private final Map<String, JointData> data = Maps.newHashMap();

    /** optional: name of source we were initialized from, if given. */
    private String sourceName;

    /**
     * Set the local source transform data for a given joint index.
     * 
     * @param jointIndex
     *            our joint index value.
     * @param jointData
     *            the joint transform data. This object is copied into the local store.
     */
    public void setJointTransformData(final int jointIndex, final JointData jointData) {
        final String key = JointChannel.JOINT_CHANNEL_NAME + jointIndex;
        // reuse TransformData object
        if (!data.containsKey(key)) {
            data.put(key, new JointData(jointData));
        } else {
            final JointData old = data.get(key);
            old.set(jointData);
        }
    }

    /**
     * Sets a translation to the local transformdata for a given joint index.
     * 
     * @param jointIndex
     *            our joint index value.
     * @param translation
     *            the translation to set
     */
    public void setJointTranslation(final int jointIndex, final ReadOnlyVector3 translation) {
        final String key = JointChannel.JOINT_CHANNEL_NAME + jointIndex;
        JointData tData = data.get(key);
        if (tData == null) {
            tData = new JointData(jointIndex);
            data.put(key, tData);
        }

        tData.setTranslation(translation);
    }

    /**
     * Sets a scale to the local transformdata for a given joint index.
     * 
     * @param jointIndex
     *            our joint index value.
     * @param scale
     *            the scale to set
     */
    public void setJointScale(final int jointIndex, final ReadOnlyVector3 scale) {
        final String key = JointChannel.JOINT_CHANNEL_NAME + jointIndex;
        JointData tData = data.get(key);
        if (tData == null) {
            tData = new JointData(jointIndex);
            data.put(key, tData);
        }

        tData.setScale(scale);
    }

    /**
     * Sets a rotation to the local transformdata for a given joint index.
     * 
     * @param jointIndex
     *            our joint index value.
     * @param rotation
     *            the rotation to set
     */
    public void setJointRotation(final int jointIndex, final ReadOnlyQuaternion rotation) {
        final String key = JointChannel.JOINT_CHANNEL_NAME + jointIndex;
        JointData tData = data.get(key);
        if (tData == null) {
            tData = new JointData(jointIndex);
            data.put(key, tData);
        }

        tData.setRotation(rotation);
    }

    /**
     * Returns an immutable COPY of our local source data.
     */
    public Map<String, JointData> getSourceData(final AnimationManager manager) {
        return ImmutableMap.copyOf(data);
    }

    /**
     * Does nothing.
     */
    public boolean setTime(final double globalTime, final AnimationManager manager) {
        return true;
    }

    /**
     * Does nothing.
     */
    public void resetClips(final AnimationManager manager, final double globalStartTime) {
        ; // ignore
    }

    /**
     * Does nothing.
     */
    @Override
    public boolean isActive(final AnimationManager manager) {
        return true;
    }

    /**
     * Setup transform data on this source, using the first frame from a specific clip and jointNames from a specific
     * pose.
     * 
     * @param pose
     *            the pose to sample joints from
     * @param clip
     *            the animation clip to pull data from
     * @param jointNames
     *            the names of the joints to find indices of.
     */
    public void initJointsByName(final SkeletonPose pose, final AnimationClip clip, final String... jointNames) {
        for (final String name : jointNames) {
            final int jointIndex = pose.getSkeleton().findJointByName(name);
            setJointTransformData(jointIndex, ((JointChannel) clip.findChannelByName(JointChannel.JOINT_CHANNEL_NAME
                    + jointIndex)).getJointData(0, new JointData(jointIndex)));
        }
    }

    /**
     * Setup transform data for specific joints on this source, using the first frame from a given clip.
     * 
     * @param clip
     *            the animation clip to pull data from
     * @param jointIndices
     *            the indices of the joints to initialize data for.
     */
    public void initJointsById(final AnimationClip clip, final int... jointIndices) {
        for (final int jointIndex : jointIndices) {
            setJointTransformData(jointIndex, ((JointChannel) clip.findChannelByName(JointChannel.JOINT_CHANNEL_NAME
                    + jointIndex)).getJointData(0, new JointData(jointIndex)));
        }
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(final String name) {
        sourceName = name;
    }
}
