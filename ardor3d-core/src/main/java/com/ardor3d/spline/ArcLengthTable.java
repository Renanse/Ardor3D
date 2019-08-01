/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

/**
 * 
 */

package com.ardor3d.spline;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.controller.ComplexSpatialController;
import com.ardor3d.scenegraph.controller.interpolation.InterpolationController;

/**
 * ArcLengthTable class contains methods for generating and storing arc lengths of a curve. Arc Lengths are used to get
 * constant speed interpolation over a curve.
 * <p>
 * This class does not automatically generate the look up tables, you must manually call {@link #generate(int, boolean)}
 * to generate the table.
 * </p>
 */
public class ArcLengthTable {

    /** Classes logger */
    private static final Logger LOGGER = Logger.getLogger(ArcLengthTable.class.getName());

    /** Table containing arc lengths for look up */
    private Map<Integer, List<ArcLengthEntry>> _lookupTable;

    /** The curve who's values to cache */
    private final Curve _curve;

    /**
     * Creates a new instance of <code>ArcLengthTable</code>.
     * 
     * @param curve
     *            The curve to create the table for, can not be <code>null</code>.
     */
    public ArcLengthTable(final Curve curve) {
        super();

        if (null == curve) {
            throw new IllegalArgumentException("curve was null!");
        }

        _curve = curve;
    }

    /**
     * @param index
     *            The index of the control point you want the length for.
     * @return The total approximate length of the segment starting at the given index.
     */
    public double getLength(final int index) {
        if (null == _lookupTable) {
            throw new IllegalStateException(
                    "You must generate the look up table before calling this method! see generate()");
        }

        final List<ArcLengthEntry> entries = _lookupTable.get(index);

        if (null == entries) {
            throw new IllegalArgumentException("entries was null, the index parameter was invalid. index=" + index);
        }

        final ArcLengthEntry arcLength = entries.get(entries.size() - 1);

        return arcLength.getLength();
    }

    /**
     * @param index
     *            The index of the first control point you are interpolating from.
     * @param distance
     *            The distance you want the spatial to travel.
     * @return The delta you should use to travel the specified distance from the control point given, will not be
     *         negative but may be greater than 1.0 if the distance is greater than the length of the segment.
     */
    public double getDelta(final int index, final double distance) {
        if (null == _lookupTable) {
            throw new IllegalStateException(
                    "You must generate the look up table before calling this method! see generate()");
        }

        ArcLengthEntry previous = null;
        ArcLengthEntry next = null;

        final List<ArcLengthEntry> entries = _lookupTable.get(index);

        if (null == entries) {
            throw new IllegalArgumentException("entries was null, the index parameter was invalid. index=" + index);
        }

        for (final ArcLengthEntry entry : entries) {
            if (entry.getLength() <= distance) {
                previous = entry;
            }
            if (entry.getLength() >= distance) {
                next = entry;
                break;
            }
        }

        if (null == previous) {
            throw new IllegalArgumentException(
                    "previous was null, either the index or distance parameters were invalid. index=" + index
                            + ", distance=" + distance);
        }

        final double delta;

        /*
         * If next is null then the length we need to travel is longer than the length of the segment, in this case we
         * work out the delta required to travel from the start of the segment minus what we've already travelled. We
         * then add that delta to the delta required to traverse the next segment and return that value, it's up to the
         * controller to handle this value correctly (update indices etc)
         * 
         * We need to be careful about wrapping around the end of the curve.
         */
        if (null == next) {
            final int newIndex = (index + 1 >= _lookupTable.size()) ? 1 : index + 1;

            delta = getDelta(newIndex, distance - previous.getLength()) + previous.getDelta();

        } else {
            if (previous.equals(next)) {
                delta = previous.getDelta();

            } else {
                final double d0 = previous.getDelta();
                final double d1 = next.getDelta();
                final double l0 = previous.getLength();
                final double l1 = next.getLength();

                delta = (d0 + ((distance - l0) / (l1 - l0)) * (d1 - d0));
            }
        }

        return delta;
    }

    /**
     * Actually generates the arc length table, this needs to be called before this class can actually perform any
     * useful functions.
     * 
     * @param step
     *            The larger the step value used the more accurate the resulting table will be and thus the smoother the
     *            motion will be, must be greater than zero.
     * @param reverse
     *            <code>true</code> to generate the table while stepping from the end of the curve to the beginning,
     *            <code>false</code> to generate the table from the beginning of the curve. You only need to generate a
     *            reverse table if you are using the {@link ComplexSpatialController.RepeatType#CYCLE cycle} repeat
     *            type.
     */
    public void generate(final int step, final boolean reverse) {
        if (step <= 0) {
            throw new IllegalArgumentException("step must be > 0! step=" + step);
        }

        _lookupTable = new HashMap<Integer, List<ArcLengthEntry>>();

        final Vector3 target = Vector3.fetchTempInstance();
        final Vector3 previous = Vector3.fetchTempInstance();

        final int loopStart = reverse ? (_curve.getControlPointCount() - 2) : 1;
        final double tStep = InterpolationController.DELTA_MAX / step;

        for (int i = loopStart; continueLoop(i, reverse); i = updateCounter(i, reverse)) {
            final int startIndex = i;
            double t = 0f;
            double length = 0;

            previous.set(_curve.getControlPoints().get(i));

            final ArrayList<ArcLengthEntry> entries = new ArrayList<ArcLengthEntry>();
            entries.add(new ArcLengthEntry(0f, 0));

            final int endIndex = reverse ? startIndex - 1 : startIndex + 1;

            while (true) {
                t += tStep;

                /*
                 * If we are over delta max force to 1. We need to do this to avoid precision issues causing errors
                 * later on (e.g. if last entry for an index had a delta of 0.996 and later during an update we passed
                 * 0.998 for that index we'd fail to find a valid entry and error out)
                 */
                if (t > InterpolationController.DELTA_MAX) {
                    t = InterpolationController.DELTA_MAX;
                }

                _curve.interpolate(startIndex, endIndex, t, target);

                length += previous.distance(target);

                previous.set(target);

                entries.add(new ArcLengthEntry(t, length));

                if (t == InterpolationController.DELTA_MAX) {
                    break;
                }
            }

            _lookupTable.put(i, entries);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("look up table = " + _lookupTable);
        }

        Vector3.releaseTempInstance(target);
        Vector3.releaseTempInstance(previous);
    }

    private boolean continueLoop(final int i, final boolean reverse) {
        return (reverse ? i > 0 : i < _curve.getControlPointCount() - 2);
    }

    private int updateCounter(final int i, final boolean reverse) {
        return (reverse ? i - 1 : i + 1);
    }

    /**
     * A private inner class used to store the required arc length variables for the table
     */
    private static class ArcLengthEntry implements Serializable {
        /** Serial UID */
        private static final long serialVersionUID = 1L;

        private final double _delta;
        private final double _length;

        public ArcLengthEntry(final double delta, final double length) {
            super();

            _delta = delta;
            _length = length;
        }

        public double getDelta() {
            return _delta;
        }

        public double getLength() {
            return _length;
        }

        @Override
        public String toString() {
            return "ArcLengthEntry[length=" + _length + ", delta=" + _delta + ']';
        }
    }

}
