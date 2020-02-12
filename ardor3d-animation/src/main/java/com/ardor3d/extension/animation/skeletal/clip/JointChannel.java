/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

import java.io.IOException;
import java.lang.reflect.Field;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * Transform animation channel, specifically geared towards describing the motion of skeleton joints.
 */
@SavableFactory(factoryMethod = "initSavable")
public class JointChannel extends TransformChannel {

    /** A name prepended to joint indices to identify them as joint channels. */
    public static final String JOINT_CHANNEL_NAME = "_jnt";

    /** The human readable version of the name. */
    private final String _jointName;

    /** The joint index. */
    private int _jointIndex;

    /**
     * Construct a new JointChannel.
     * 
     * @param joint
     *            the joint to pull name and index from.
     * @param times
     *            our time offset values.
     * @param rotations
     *            the rotations to set on this channel at each time offset.
     * @param translations
     *            the translations to set on this channel at each time offset.
     * @param scales
     *            the scales to set on this channel at each time offset.
     */
    public JointChannel(final Joint joint, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        super(JointChannel.JOINT_CHANNEL_NAME + joint.getIndex(), times, rotations, translations, scales);
        _jointName = joint.getName();
        _jointIndex = joint.getIndex();
    }

    /**
     * Construct a new JointChannel.
     * 
     * @param jointName
     *            the human readable name of the joint
     * @param jointIndex
     *            the index of the joint.
     * @param times
     *            our time offset values.
     * @param rotations
     *            the rotations to set on this channel at each time offset.
     * @param translations
     *            the translations to set on this channel at each time offset.
     * @param scales
     *            the scales to set on this channel at each time offset.
     */
    public JointChannel(final String jointName, final int jointIndex, final float[] times,
            final ReadOnlyQuaternion[] rotations, final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        super(JointChannel.JOINT_CHANNEL_NAME + jointIndex, times, rotations, translations, scales);
        _jointName = jointName;
        _jointIndex = jointIndex;
    }

    /**
     * Construct a new JointChannel.
     * 
     * @param joint
     *            the index of the joint.
     * @param times
     *            our time offset values.
     * @param transforms
     *            the transform to set on this channel at each time offset.
     */
    public JointChannel(final Joint joint, final float[] times, final ReadOnlyTransform[] transforms) {
        super(JointChannel.JOINT_CHANNEL_NAME + joint.getIndex(), times, transforms);
        _jointName = joint.getName();
        _jointIndex = joint.getIndex();
    }

    /**
     * @return the human readable version of the associated joint's name.
     */
    public String getJointName() {
        return _jointName;
    }

    /**
     * @return the joint index this channel targets.
     */
    public int getJointIndex() {
        return _jointIndex;
    }

    @Override
    protected JointChannel newChannel(final String name, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        return new JointChannel(_jointName, _jointIndex, times, rotations, translations, scales);
    }

    @Override
    public void setCurrentSample(final int sampleIndex, final double progressPercent, final Object applyTo) {
        super.setCurrentSample(sampleIndex, progressPercent, applyTo);

        final JointData jointData = (JointData) applyTo;
        jointData.setJointIndex(_jointIndex);
    }

    @Override
    public JointData createStateDataObject(final AnimationClipInstance instance) {
        return new JointData();
    }

    public JointData getJointData(final int index, final JointData store) {
        JointData rVal = store;
        if (rVal == null) {
            rVal = new JointData();
        }
        super.getTransformData(index, rVal);
        rVal.setJointIndex(_jointIndex);
        return rVal;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends JointChannel> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_jointName, "jointName", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        final String jointName = capsule.readString("jointName", null);
        try {
            final Field field1 = JointChannel.class.getDeclaredField("_jointName");
            field1.setAccessible(true);
            field1.set(this, jointName);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (_channelName.startsWith(JointChannel.JOINT_CHANNEL_NAME)) {
            _jointIndex = Integer.parseInt(_channelName.substring(JointChannel.JOINT_CHANNEL_NAME.length()));
        } else {
            _jointIndex = -1;
        }
    }

    public static JointChannel initSavable() {
        return new JointChannel();
    }

    protected JointChannel() {
        super();
        _jointName = null;
        _jointIndex = -1;
    }
}
