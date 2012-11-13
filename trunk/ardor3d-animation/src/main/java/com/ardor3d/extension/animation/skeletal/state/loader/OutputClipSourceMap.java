/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.state.loader;

import java.util.logging.Logger;

import com.ardor3d.extension.animation.skeletal.blendtree.ClipSource;
import com.ardor3d.extension.animation.skeletal.util.LoggingMap;

/**
 * This class essentially just wraps a String->ClipSource HashMap, providing extra logging when a ClipSource is not
 * found, or duplicate ClipSources are added.
 */
public class OutputClipSourceMap extends LoggingMap<String, ClipSource> {

    /** our class logger */
    private static final Logger logger = Logger.getLogger(OutputClipSourceMap.class.getName());

    /**
     * Add a ClipSource to the store. Logs a warning if a source by the same name was already in the store.
     * 
     * @param source
     *            the clip source to add.
     */
    public void put(final ClipSource source) {
        final String key = source.getClip().getName();
        if (_wrappedMap.put(key, source) != null) {
            OutputClipSourceMap.logger.warning("Replaced clip source in OutputClipSourceMap with same name. " + key);
        }
    }
    
    public static Logger getLogger() {
        return logger;
    }
}
