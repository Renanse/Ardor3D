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

import java.util.ArrayList;
import java.util.List;

/**
 * Transient class that maintains the current triggers and armed status for a TriggerChannel.
 */
public class TriggerData {

    /** The current trigger name. */
    private final List<String> _currentTriggers = new ArrayList<>();

    /**
     * The current channel sample index. We keep this to make sure we don't miss two channels in a row with the same
     * trigger name.
     */
    private int _currentIndex = -1;

    /** If true, we are armed - we have had a trigger set and have not executed it. */
    private boolean _armed = false;

    public List<String> getCurrentTriggers() {
        return _currentTriggers;
    }

    public String getCurrentTrigger() {
        return _currentTriggers.isEmpty() ? null : _currentTriggers.get(_currentTriggers.size() - 1);
    }

    public int getCurrentIndex() {
        return _currentIndex;
    }

    public void setArmed(final boolean armed) {
        _armed = armed;
    }

    public boolean isArmed() {
        return _armed;
    }

    /**
     * Try to set a given trigger/index as armed. If we already have this trigger and index set, we don't change the
     * state of armed.
     * 
     * @param trigger
     *            our trigger name
     * @param index
     *            our sample index
     */
    public synchronized void arm(final int index, final String... triggers) {
        if (triggers == null || triggers.length == 0) {
            _currentTriggers.clear();
            _armed = false;
        } else if (index != _currentIndex) {
            _currentTriggers.clear();
            for (final String t : triggers) {
                _currentTriggers.add(t);
            }
            _armed = true;
        }
        _currentIndex = index;
    }
}
