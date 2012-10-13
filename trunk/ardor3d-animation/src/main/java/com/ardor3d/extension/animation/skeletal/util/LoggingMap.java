/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.common.collect.Maps;

/**
 * This class essentially just wraps a KEY->VALUE HashMap, providing extra logging when a VALUE is not found, or
 * duplicate VALUE objects are added. An optional callback may be provided to try to load values not present in this
 * map. These are loaded using the string representation of the key and casting the return from Object to the value
 * class. If the value is still null, a default value is returned.
 */
public class LoggingMap<KEY, VALUE> {

    /** our class logger */
    private static final Logger logger = Logger.getLogger(LoggingMap.class.getName());

    /** Our map of values. */
    protected final Map<KEY, VALUE> _wrappedMap = Maps.newHashMap();

    /** If not null, this callback is asked to load the missing value using the key. */
    private MissingCallback<KEY, VALUE> _missCallback = null;

    /** A default value to return if a key is requested that does not exist. Defaults to null. */
    private VALUE _defaultValue = null;

    /** If true, we'll log anytime we set a key/value where the key already existed in the map. Defaults to true. */
    private boolean _logOnReplace = true;

    /** If true, we'll log anytime we try to retrieve a value by a key that is not in the map. Defaults to true. */
    private boolean _logOnMissing = true;

    /**
     * Add a value to the store. Logs a warning if a value by the same key was already in the store and logOnReplace is
     * true.
     * 
     * @param key
     *            the key to add.
     * @param value
     *            the value to add.
     */
    public void put(final KEY key, final VALUE value) {
        if (_wrappedMap.put(key, value) != null) {
            if (isLogOnReplace()) {
                LoggingMap.logger.warning("Replaced value in map with same key. " + key);
            }
        }
    }

    /**
     * Retrieves a value from our store by key. Logs a warning if a value by that key is not found and logOnMissing is
     * true. If missing, defaultValue is returned.
     * 
     * @param key
     *            the key of the value to find.
     * @return the associated value, or null if none is found.
     */
    public VALUE get(final KEY key) {
        VALUE value = _wrappedMap.get(key);
        // value is null? ask callback.
        if (value == null && getMissCallback() != null) {
            value = getMissCallback().getValue(key);
            if (value != null) {
                // save for next time.
                _wrappedMap.put(key, value);
            }
        }
        // value still null...
        if (value == null) {
            if (isLogOnMissing()) {
                LoggingMap.logger.warning("Value not found with key: " + key + " Returning defaultValue: "
                        + _defaultValue);
            }
            return getDefaultValue();
        }
        return value;
    }

    /**
     * Removes the mapping for the given key.
     * 
     * @param key
     *            the key of the value to remove.
     * @return the previously associated value, or null if none was found.
     */
    public VALUE remove(final KEY key) {
        return _wrappedMap.remove(key);
    }

    /**
     * @return the number of key-value pairs stored in this object.
     */
    public int size() {
        return _wrappedMap.size();
    }

    public void setDefaultValue(final VALUE defaultValue) {
        this._defaultValue = defaultValue;
    }

    public VALUE getDefaultValue() {
        return _defaultValue;
    }

    public void setLogOnReplace(final boolean logOnReplace) {
        this._logOnReplace = logOnReplace;
    }

    public boolean isLogOnReplace() {
        return _logOnReplace;
    }

    public void setLogOnMissing(final boolean logOnMissing) {
        this._logOnMissing = logOnMissing;
    }

    public boolean isLogOnMissing() {
        return _logOnMissing;
    }

    public MissingCallback<KEY, VALUE> getMissCallback() {
        return _missCallback;
    }

    public void setMissCallback(final MissingCallback<KEY, VALUE> missCallback) {
        _missCallback = missCallback;
    }

    public Set<KEY> keySet() {
        return _wrappedMap.keySet();
    }

    public Collection<VALUE> values() {
        return _wrappedMap.values();
    }
}
