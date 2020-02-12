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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.CapsuleUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.google.common.collect.ImmutableList;

/**
 * An animation channel consisting of a series of transforms interpolated over time.
 */
@SavableFactory(factoryMethod = "initSavable")
public class TransformChannel extends AbstractAnimationChannel {

    private static final Logger logger = Logger.getLogger(TransformChannel.class.getName());

    // XXX: Perhaps we could optimize memory by reusing sample objects that are the same from one index to the next.
    // XXX: Could then also optimize execution time by checking object equality (==) and skipping (s)lerps.

    /** Our rotation samples. */
    private final ReadOnlyQuaternion[] _rotations;

    /** Our translation samples. */
    private final ReadOnlyVector3[] _translations;

    /** Our scale samples. */
    private final ReadOnlyVector3[] _scales;

    private final Quaternion _compQuat1 = new Quaternion();
    private final Quaternion _compQuat2 = new Quaternion();
    private final Vector3 _compVect1 = new Vector3();

    /**
     * Construct a new TransformChannel.
     * 
     * @param channelName
     *            our name.
     * @param times
     *            our time offset values.
     * @param rotations
     *            the rotations to set on this channel at each time offset.
     * @param translations
     *            the translations to set on this channel at each time offset.
     * @param scales
     *            the scales to set on this channel at each time offset.
     */
    public TransformChannel(final String channelName, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        super(channelName, times);

        if (rotations.length != times.length || translations.length != times.length || scales.length != times.length) {
            throw new IllegalArgumentException("All provided arrays must be the same length! Channel: " + channelName);
        }

        // Construct our data
        _rotations = new ReadOnlyQuaternion[rotations.length];
        int i = 0;
        for (final ReadOnlyQuaternion q : rotations) {
            _rotations[i++] = new Quaternion(q);
        }
        _translations = new ReadOnlyVector3[translations.length];
        i = 0;
        for (final ReadOnlyVector3 v : translations) {
            _translations[i++] = new Vector3(v);
        }
        _scales = new ReadOnlyVector3[scales.length];
        i = 0;
        for (final ReadOnlyVector3 v : scales) {
            _scales[i++] = new Vector3(v);
        }
    }

    /**
     * Construct a new TransformChannel.
     * 
     * @param channelName
     *            our name.
     * @param times
     *            our time offset values.
     * @param transforms
     *            the transform to set on this channel at each time offset. These are separated into rotation, scale and
     *            translation components. Note that supplying transforms with non-rotational matrices (with built in
     *            shear, scale.) will produce a warning and may not give you the expected result.
     */
    public TransformChannel(final String channelName, final float[] times, final ReadOnlyTransform[] transforms) {
        super(channelName, times);

        // Construct our data
        _rotations = new ReadOnlyQuaternion[transforms.length];
        _translations = new ReadOnlyVector3[transforms.length];
        _scales = new ReadOnlyVector3[transforms.length];

        for (int i = 0; i < transforms.length; i++) {
            final ReadOnlyTransform transform = transforms[i];
            if (!transform.isRotationMatrix()) {
                TransformChannel.logger.warning("TransformChannel '" + channelName
                        + "' supplied transform with non-rotational matrices.  May have unexpected results.");
            }
            _rotations[i] = new Quaternion().fromRotationMatrix(transform.getMatrix()).normalizeLocal();
            _translations[i] = new Vector3(transform.getTranslation());
            _scales[i] = new Vector3(transform.getScale());
        }
    }

    @Override
    public void setCurrentSample(final int sampleIndex, final double progressPercent, final Object applyTo) {
        final TransformData transformData = (TransformData) applyTo;

        // shortcut if we are fully on one sample or the next
        if (progressPercent == 0.0f) {
            transformData.setRotation(_rotations[sampleIndex]);
            transformData.setTranslation(_translations[sampleIndex]);
            transformData.setScale(_scales[sampleIndex]);
            return;
        } else if (progressPercent == 1.0f) {
            transformData.setRotation(_rotations[sampleIndex + 1]);
            transformData.setTranslation(_translations[sampleIndex + 1]);
            transformData.setScale(_scales[sampleIndex + 1]);
            return;
        }

        // Apply (s)lerp and set in transform
        _compQuat1.slerpLocal(_rotations[sampleIndex], _rotations[sampleIndex + 1], progressPercent, _compQuat2);
        transformData.setRotation(_compQuat1);

        _compVect1.lerpLocal(_translations[sampleIndex], _translations[sampleIndex + 1], progressPercent);
        transformData.setTranslation(_compVect1);
        _compVect1.lerpLocal(_scales[sampleIndex], _scales[sampleIndex + 1], progressPercent);
        transformData.setScale(_compVect1);
    }

    /**
     * Apply a specific index of this channel to a TransformData object.
     * 
     * @param index
     *            the index to grab.
     * @param store
     *            the TransformData to store in. If null, a new one is created.
     * @return our resulting TransformData.
     */
    public TransformData getTransformData(final int index, final TransformData store) {
        TransformData rVal = store;
        if (rVal == null) {
            rVal = new TransformData();
        }
        rVal.setRotation(_rotations[index]);
        rVal.setScale(_scales[index]);
        rVal.setTranslation(_translations[index]);
        return rVal;
    }

    @Override
    public TransformChannel getSubchannelBySample(final String name, final int startSample, final int endSample) {
        if (startSample > endSample) {
            throw new IllegalArgumentException("startSample > endSample");
        }
        if (endSample >= getSampleCount()) {
            throw new IllegalArgumentException("endSample >= getSampleCount()");
        }

        final int samples = endSample - startSample + 1;
        final float[] times = new float[samples];
        final ReadOnlyQuaternion[] rotations = new ReadOnlyQuaternion[samples];
        final ReadOnlyVector3[] translations = new ReadOnlyVector3[samples];
        final ReadOnlyVector3[] scales = new ReadOnlyVector3[samples];

        for (int i = 0; i < samples; i++) {
            times[i] = _times[i + startSample];
            rotations[i] = _rotations[i + startSample];
            translations[i] = _translations[i + startSample];
            scales[i] = _scales[i + startSample];
        }

        return newChannel(name, times, rotations, translations, scales);
    }

    @Override
    public AbstractAnimationChannel getSubchannelByTime(final String name, final float startTime, final float endTime) {
        if (startTime > endTime) {
            throw new IllegalArgumentException("startTime > endTime");
        }
        final List<Float> times = new ArrayList<>();
        final List<ReadOnlyQuaternion> rotations = new ArrayList<>();
        final List<ReadOnlyVector3> translations = new ArrayList<>();
        final List<ReadOnlyVector3> scales = new ArrayList<>();

        final TransformData tData = new TransformData();

        // Add start sample
        updateSample(startTime, tData);
        times.add(0f);
        rotations.add(tData.getRotation());
        translations.add(tData.getTranslation());
        scales.add(tData.getScale());

        // Add mid samples
        for (int i = 0; i < getSampleCount(); i++) {
            final float time = _times[i];
            if (time > startTime && time < endTime) {
                times.add(time - startTime);
                rotations.add(_rotations[i]);
                translations.add(_translations[i]);
                scales.add(_scales[i]);
            }
        }

        // Add end sample
        updateSample(endTime, tData);
        times.add(endTime - startTime);
        rotations.add(tData.getRotation());
        translations.add(tData.getTranslation());
        scales.add(tData.getScale());

        final float[] timesArray = new float[times.size()];
        int i = 0;
        for (final float time : times) {
            timesArray[i++] = time;
        }
        // return
        return newChannel(name, timesArray, rotations.toArray(new ReadOnlyQuaternion[rotations.size()]),
                translations.toArray(new ReadOnlyVector3[translations.size()]),
                scales.toArray(new ReadOnlyVector3[scales.size()]));
    }

    public ImmutableList<ReadOnlyVector3> getTranslations() {
        return ImmutableList.copyOf(_translations);
    }

    public ImmutableList<ReadOnlyVector3> getScales() {
        return ImmutableList.copyOf(_scales);
    }

    public ImmutableList<ReadOnlyQuaternion> getRotations() {
        return ImmutableList.copyOf(_rotations);
    }

    protected TransformChannel newChannel(final String name, final float[] times, final ReadOnlyQuaternion[] rotations,
            final ReadOnlyVector3[] translations, final ReadOnlyVector3[] scales) {
        return new TransformChannel(name, times, rotations, translations, scales);
    }

    @Override
    public TransformData createStateDataObject(final AnimationClipInstance instance) {
        return new TransformData();
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends TransformChannel> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(CapsuleUtils.asSavableArray(_rotations), "rotations", null);
        capsule.write(CapsuleUtils.asSavableArray(_scales), "scales", null);
        capsule.write(CapsuleUtils.asSavableArray(_translations), "translations", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        final ReadOnlyQuaternion[] rotations = CapsuleUtils.asArray(capsule.readSavableArray("rotations", null),
                ReadOnlyQuaternion.class);
        final ReadOnlyVector3[] scales = CapsuleUtils.asArray(capsule.readSavableArray("scales", null),
                ReadOnlyVector3.class);
        final ReadOnlyVector3[] translations = CapsuleUtils.asArray(capsule.readSavableArray("translations", null),
                ReadOnlyVector3.class);
        try {
            final Field field1 = TransformChannel.class.getDeclaredField("_rotations");
            field1.setAccessible(true);
            field1.set(this, rotations);

            final Field field2 = TransformChannel.class.getDeclaredField("_scales");
            field2.setAccessible(true);
            field2.set(this, scales);

            final Field field3 = TransformChannel.class.getDeclaredField("_translations");
            field3.setAccessible(true);
            field3.set(this, translations);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static TransformChannel initSavable() {
        return new TransformChannel();
    }

    protected TransformChannel() {
        super(null, null);
        _rotations = null;
        _translations = null;
        _scales = null;
    }
}