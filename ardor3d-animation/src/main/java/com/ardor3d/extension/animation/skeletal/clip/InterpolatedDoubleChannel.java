/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import com.ardor3d.math.MathUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.google.common.collect.Lists;

/**
 * An animation source channel consisting of double value samples. These samples are interpolated between key frames.
 * Potential uses for this channel include extracting and using forward motion from walk animations, animating colors or
 * texture coordinates, etc.
 */
public class InterpolatedDoubleChannel extends AbstractAnimationChannel {

    /** Our key samples. */
    protected final double[] _values;

    /**
     * Construct a new InterpolatedDoubleChannel.
     * 
     * @param channelName
     *            the name of this channel.
     * @param times
     *            the time samples
     * @param keys
     *            our key samples. Entries may be null. Should have as many entries as the times array.
     */
    public InterpolatedDoubleChannel(final String channelName, final float[] times, final double[] values) {
        super(channelName, times);
        _values = values == null ? null : new double[values.length];
        if (_values != null) {
            System.arraycopy(values, 0, _values, 0, values.length);
        }
    }

    public double[] getValues() {
        return _values;
    }

    @Override
    public void setCurrentSample(final int sampleIndex, final double progressPercent, final Object applyTo) {
        final double[] store = (double[]) applyTo;

        // set key
        store[0] = MathUtils.lerp(progressPercent, _values[sampleIndex], _values[sampleIndex + 1]);
    }

    @Override
    public double[] createStateDataObject(final AnimationClipInstance instance) {
        return new double[1];
    }

    @Override
    public InterpolatedDoubleChannel getSubchannelBySample(final String name, final int startSample, final int endSample) {
        if (startSample > endSample) {
            throw new IllegalArgumentException("startSample > endSample");
        }
        if (endSample >= getSampleCount()) {
            throw new IllegalArgumentException("endSample >= getSampleCount()");
        }

        final int samples = endSample - startSample + 1;
        final float[] times = new float[samples];
        final double[] values = new double[samples];

        for (int i = 0; i <= samples; i++) {
            times[i] = _times[i + startSample];
            values[i] = _values[i + startSample];
        }

        return new InterpolatedDoubleChannel(name, times, values);
    }

    @Override
    public InterpolatedDoubleChannel getSubchannelByTime(final String name, final float startTime, final float endTime) {
        if (startTime > endTime) {
            throw new IllegalArgumentException("startTime > endTime");
        }
        final List<Float> times = Lists.newArrayList();
        final List<Double> keys = Lists.newArrayList();

        final double[] data = new double[1];

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
        final double[] values = new double[keys.size()];
        i = 0;
        for (final double val : keys) {
            values[i++] = val;
        }
        return new InterpolatedDoubleChannel(name, timesArray, values);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends InterpolatedDoubleChannel> getClassTag() {
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
        final double[] values = capsule.readDoubleArray("values", null);
        try {
            final Field field1 = TriggerChannel.class.getDeclaredField("_values");
            field1.setAccessible(true);
            field1.set(this, values);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static InterpolatedDoubleChannel initSavable() {
        return new InterpolatedDoubleChannel();
    }

    protected InterpolatedDoubleChannel() {
        super(null, null);
        _values = null;
    }
}
