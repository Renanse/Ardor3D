/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

import com.ardor3d.util.Timer;
import com.google.common.collect.Lists;

/**
 * This class acts as a centralized data store for statistics. As data is added to the collector, a sum total is kept as
 * well as the total number of data samples given for the particular stat.
 */
public abstract class StatCollector {
    private static final Logger logger = Logger.getLogger(StatCollector.class.getName());

    /**
     * How many distinct past aggregate samples are kept before the oldest one is dropped on add. You can multiply this
     * by the time sample rate to determine the total history length in time.
     */
    protected static int maxSamples = 100;

    /**
     * Our map of current stat values. Current means values that have been collected within the current time sample. For
     * example, if sampleRate = 1.0, then current will hold values collected since the last 1 second ping.
     */
    protected static HashMap<StatType, StatValue> current = new HashMap<StatType, StatValue>();

    protected static List<MultiStatSample> historical = Collections.synchronizedList(new LinkedList<MultiStatSample>());

    /**
     * How long to gather stats as a single unit before pushing them onto the historical stack.
     */
    protected static double sampleRateMS = 1000;

    protected static double lastSampleTime = 0;

    protected static double lastTimeCheckMS = 0;

    protected static List<StatListener> listeners = Lists.newArrayList();

    protected static double startOffset = 0;

    protected static boolean ignoreStats = false;

    protected static Stack<StatType> timeStatStack = new Stack<StatType>();
    protected static HashSet<StatType> timedStats = new HashSet<StatType>();

    protected static Timer timer = new Timer();

    protected static final double TO_MS = 1000.0 / timer.getResolution();

    protected static long pausedTime;

    protected static long pausedStartTime;

    /**
     * Construct a new StatCollector.
     * 
     * @param sampleRateMS
     *            The amount of time between aggregated samples in milliseconds.
     */
    public static void init(final long sampleRateMS, final int maxHistorical) {
        StatCollector.sampleRateMS = sampleRateMS;
        StatCollector.maxSamples = maxHistorical;
    }

    public static void addStat(final StatType type, final double statValue) {
        if (ignoreStats) {
            return;
        }

        synchronized (current) {
            StatValue val = current.get(type);
            if (val == null) {
                val = new StatValue();
                current.put(type, val);
            }
            val.incrementValue(statValue);
            val.incrementIterations();
        }
    }

    public static void startStat(final StatType type) {
        if (ignoreStats || !timedStats.contains(type)) {
            return;
        }

        synchronized (current) {
            final StatType top = !timeStatStack.isEmpty() ? timeStatStack.peek() : null;

            final double timeMS = timer.getTime() * TO_MS;
            if (top != null) {
                // tally timer and include in stats.
                final StatValue val = current.get(top);
                val.incrementValue(timeMS - lastTimeCheckMS);
            } else {
                StatValue val = current.get(StatType.STAT_UNSPECIFIED_TIMER);
                if (val == null) {
                    val = new StatValue();
                    val.setIterations(1);
                    current.put(StatType.STAT_UNSPECIFIED_TIMER, val);
                }
                val.incrementValue(timeMS - lastTimeCheckMS);
            }

            lastTimeCheckMS = timeMS;
            timeStatStack.push(type);

            if (type != null) {
                StatValue val = current.get(type);
                if (val == null) {
                    val = new StatValue();
                    current.put(type, val);
                }
                val.incrementIterations();
            }
        }
    }

    public static void endStat(final StatType type) {
        if (ignoreStats || !timedStats.contains(type)) {
            return;
        }

        synchronized (current) {
            // This will throw error if called out of turn.
            StatType top = timeStatStack.pop();

            final double timeMS = timer.getTime() * TO_MS;

            // tally timer and include in stats.
            final StatValue val = current.get(top);
            val.incrementValue(timeMS - lastTimeCheckMS);

            lastTimeCheckMS = timeMS;

            // Pop until we find our stat type
            while (!top.equals(type)) {
                logger.warning("Mismatched endStat, found " + top + ".  Expected '" + type + "'");
                top = timeStatStack.pop();
            }
        }
    }

    public static synchronized void update() {
        final double timeMS = timer.getTime() * TO_MS;
        final double elapsed = timeMS - lastSampleTime;

        // Only continue if we've gone past our sample time threshold
        if (elapsed < sampleRateMS) {
            return;
        }

        synchronized (current) {
            // Check if we have a timed stat currently tracking... if so, update it
            if (!timeStatStack.isEmpty()) {
                // tally timer and include in stats.
                final StatValue val = current.get(timeStatStack.peek());
                val.incrementValue(timeMS - lastTimeCheckMS - (pausedTime * TO_MS));
                lastTimeCheckMS = timeMS;
                // reset iterations of all stack to 0 to indicate we have not used it yet
                for (int x = timeStatStack.size(); --x >= 0;) {
                    final StatValue val2 = current.get(timeStatStack.get(x));
                    if (val2 != null) {
                        val2.setIterations(0);
                    }
                }

                // set current iterations to 1 to indicate we've used this stat
                val.setIterations(1);
            } else {
                final StatValue val = current.get(StatType.STAT_UNSPECIFIED_TIMER);
                if (val != null) {
                    val.incrementValue(timeMS - lastTimeCheckMS - (pausedTime * TO_MS));
                    lastTimeCheckMS = timeMS;
                    val.setIterations(1);
                }
            }

            // Add "current" hash into historical stat list
            final MultiStatSample sample = MultiStatSample.createNew(current);
            sample.setTimeElapsed(elapsed);
            historical.add(sample); // adds onto tail

            // reset the "current" hash... basically set things to 0 to decrease
            // object recreation
            for (final StatValue value : current.values()) {
                value.reset();
            }
        }

        // reset startOffset
        startOffset = 0;
        pausedTime = 0;

        // stat list should drop old stats from list when greater than a certain
        // threshold.
        while (historical.size() > maxSamples) {
            final MultiStatSample removed = historical.remove(0); // removes from head
            if (removed != null) {
                startOffset += removed.getElapsedTime();
            }
        }

        lastSampleTime = timeMS;
        fireActionEvent();
    }

    /**
     * Add a listener to the pool of listeners that are notified when a new stats aggregate is created (at the end of
     * each time sample).
     * 
     * @param listener
     *            the listener to add
     */
    public static void addStatListener(final StatListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from the pool of listeners that are notified when a new stats aggregate is created (at the end
     * of each time sample).
     * 
     * @param listener
     *            the listener to remove
     */
    public static boolean removeStatListener(final StatListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Cleans the listener pool of all listeners.
     */
    public static void removeAllListeners() {
        listeners.clear();
    }

    /**
     * Add a type to the set of stat types that are paid attention to when doing timed stat checking.
     * 
     * @param type
     *            the listener to add
     */
    public static void addTimedStat(final StatType type) {
        timedStats.add(type);
    }

    /**
     * Removes a type from the set of stat types that are paid attention to when doing timed stat checking.
     * 
     * @param type
     *            the listener to remove
     */
    public static boolean removeTimedStat(final StatType type) {
        return timedStats.remove(type);
    }

    /**
     * Cleans the set of stat types we paid attention to when doing timed stat checking.
     */
    public static void removeAllTimedStats() {
        timedStats.clear();
    }

    /**
     * Notifies all registered listeners that a new stats aggregate was created.
     */
    public static void fireActionEvent() {
        for (final StatListener l : listeners) {
            l.statsUpdated();
        }
    }

    public static double getStartOffset() {
        return startOffset;
    }

    public static double getSampleRate() {
        return sampleRateMS;
    }

    public static void setSampleRate(final long sampleRateMS) {
        StatCollector.sampleRateMS = sampleRateMS;
    }

    public static void setMaxSamples(final int samples) {
        StatCollector.maxSamples = samples;
    }

    public static int getMaxSamples() {
        return maxSamples;
    }

    public static List<MultiStatSample> getHistorical() {
        return historical;
    }

    public static MultiStatSample lastStats() {
        if (historical.size() == 0) {
            return null;
        }
        return historical.get(historical.size() - 1);
    }

    public static boolean isIgnoreStats() {
        return ignoreStats;
    }

    public static void setIgnoreStats(final boolean ignoreStats) {
        StatCollector.ignoreStats = ignoreStats;
    }

    public static boolean hasHistoricalStat(final StatType type) {
        for (final MultiStatSample mss : historical) {
            if (mss.containsStat(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Call this if you've caught an error, etc and you need to reset timed stats collecting.
     * 
     * NOTE: You must ensure you are not inside a START/END timed block, (or you recreate any necessary start calls)
     * otherwise when endStat is called a stack exception will occur.
     */
    public static void resetTimedStack() {
        timeStatStack.clear();
    }

    public static void pause() {
        setIgnoreStats(true);
        pausedStartTime = timer.getTime();
    }

    public static void resume() {
        setIgnoreStats(false);
        pausedTime += (timer.getTime() - pausedStartTime);
    }
}
