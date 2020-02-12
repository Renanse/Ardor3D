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

import com.ardor3d.math.MathUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * An animation source channel consisting of float value samples. These samples are interpolated between key frames.
 * Potential uses for this channel include extracting and using forward motion from walk animations, animating colors or
 * texture coordinates, etc.
 */
public class InterpolatedFloatChannel extends AbstractAnimationChannel {

    /** Our key samples. */
    protected final float[] _values;

    /**
     * Construct a new InterpolatedFloatChannel.
     * 
     * @param channelName
     *            the name of this channel.
     * @param times
     *            the time samples
     * @param values
     *            our value samples. Entries may be null. Should have as many entries as the times array.
     */
    public InterpolatedFloatChannel(final String channelName, final float[] times, final float[] values) {
        super(channelName, times);
        _values = values == null ? null : new float[values.length];
        if (_values != null) {
            System.arraycopy(values, 0, _values, 0, values.length);
        }
    }

    public float[] getValues() {
        return _values;
    }

    @Override
    public void setCurrentSample(final int sampleIndex, final double progressPercent, final Object applyTo) {
        final float[] store = (float[]) applyTo;

        // set key
        store[0] = MathUtils.lerp((float) progressPercent, _values[sampleIndex], _values[sampleIndex + 1]);
    }

    @Override
    public float[] createStateDataObject(final AnimationClipInstance instance) {
        return new float[1];
    }

    @Override
    public InterpolatedFloatChannel getSubchannelBySample(final String name, final int startSample, final int endSample) {
        if (startSample > endSample) {
            throw new IllegalArgumentException("startSample > endSample");
        }
        if (endSample >= getSampleCount()) {
            throw new IllegalArgumentException("endSample >= getSampleCount()");
        }

        final int samples = endSample - startSample + 1;
        final float[] times = new float[samples];
        final float[] values = new float[samples];

        for (int i = 0; i <= samples; i++) {
            times[i] = _times[i + startSample];
            values[i] = _values[i + startSample];
        }

        return new InterpolatedFloatChannel(name, times, values);
    }

    @Override
    public InterpolatedFloatChannel getSubchannelByTime(final String name, final float startTime, final float endTime) {
        if (startTime > endTime) {
            throw new IllegalArgumentException("startTime > endTime");
        }
        final List<Float> times = new ArrayList<>();
        final List<Float> keys = new ArrayList<>();

        final float[] data = new float[1];

        // Add start sample
        updateSample(startTime, data);
        times.add(0f);
        keys.add(data[0]);

        // Add mid samples
        for (int i = 0; i < getSampleCount(); i++) {
            final float time = _times[i];
            updateSample(time, data);
            if (time > startTime && time < endTime) {
                times.add(time - startTime);
                keys.add(_values[i]);
            }
        }

        // Add end sample
        updateSample(endTime, data);
        times.add(endTime - startTime);
        keys.add(data[0]);

        final float[] timesArray = new float[times.size()];
        int i = 0;
        for (final float time : times) {
            timesArray[i++] = time;
        }
        // return
        final float[] values = new float[keys.size()];
        i = 0;
        for (final float val : keys) {
            values[i++] = val;
        }
        return new InterpolatedFloatChannel(name, timesArray, values);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends InterpolatedFloatChannel> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_values, "values", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        final float[] values = capsule.readFloatArray("values", null);
        try {
            final Field field1 = TriggerChannel.class.getDeclaredField("_values");
            field1.setAccessible(true);
            field1.set(this, values);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static InterpolatedFloatChannel initSavable() {
        return new InterpolatedFloatChannel();
    }

    protected InterpolatedFloatChannel() {
        super(null, null);
        _values = null;
    }
}
