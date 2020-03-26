/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.terrain.providers.compound.function;

public interface ICombineFunction {

    public static final ICombineFunction MAX = (prev, inc) -> {
        if (prev == null) {
            return inc;
        } else if (inc == null) {
            return prev;
        }

        assert prev.length == inc.length;

        final float[] rVal = new float[prev.length];
        for (int i = 0; i < rVal.length; i++) {
            if (!Double.isFinite(inc[i])) {
                rVal[i] = prev[i];
            } else if (!Double.isFinite(prev[i])) {
                rVal[i] = inc[i];
            } else {
                rVal[i] = Math.max(prev[i], inc[i]);
            }
        }
        return rVal;
    };

    public static final ICombineFunction MIN = (prev, inc) -> {
        if (prev == null) {
            return inc;
        } else if (inc == null) {
            return prev;
        }

        assert prev.length == inc.length;

        final float[] rVal = new float[prev.length];
        for (int i = 0; i < rVal.length; i++) {
            if (!Double.isFinite(inc[i])) {
                rVal[i] = prev[i];
            } else if (!Double.isFinite(prev[i])) {
                rVal[i] = inc[i];
            } else {
                rVal[i] = Math.min(prev[i], inc[i]);
            }
        }
        return rVal;
    };

    public static final ICombineFunction IGNORE_PREVIOUS = (prev, inc) -> {
        return inc;
    };

    public static final ICombineFunction IGNORE_INCOMING = (prev, inc) -> {
        return prev;
    };

    public static final ICombineFunction WHERE_INCOMING_VALID = (prev, inc) -> {
        if (prev == null) {
            return inc;
        } else if (inc == null) {
            return prev;
        }

        assert prev.length == inc.length;

        final float[] rVal = new float[prev.length];
        for (int i = 0; i < rVal.length; i++) {
            if (!Double.isFinite(inc[i])) {
                rVal[i] = prev[i];
            } else {
                rVal[i] = inc[i];
            }
        }
        return rVal;
    };

    float[] apply(float[] previousData, float[] incomingData);

}
