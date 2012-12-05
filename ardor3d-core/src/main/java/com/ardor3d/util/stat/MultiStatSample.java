/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat;

import java.util.HashMap;
import java.util.Set;

import com.google.common.collect.Maps;

public class MultiStatSample {
    private final HashMap<StatType, StatValue> _values = Maps.newHashMap();
    private double _elapsedTime = 0.0;

    public static MultiStatSample createNew(final HashMap<StatType, StatValue> current) {
        final MultiStatSample rVal = new MultiStatSample();
        for (final StatType type : current.keySet()) {
            final StatValue entry = current.get(type);
            // only count values we've seen at least 1 time from this sample set.
            if (entry.getIterations() > 0) {
                final StatValue store = new StatValue(entry);
                rVal._values.put(type, store);
            }
        }
        return rVal;
    }

    public void setTimeElapsed(final double time) {
        _elapsedTime = time;
    }

    public boolean containsStat(final StatType type) {
        return _values.containsKey(type);
    }

    public StatValue getStatValue(final StatType type) {
        return _values.get(type);
    }

    public Set<StatType> getStatTypes() {
        return _values.keySet();
    }

    public double getElapsedTime() {
        return _elapsedTime;
    }
}
