/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.state.loader;

import java.util.logging.Logger;

import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.util.LoggingMap;

/**
 * This class essentially just wraps a String->Animation HashMap, providing extra logging when a
 * clip is not found, or duplicate clips are added.
 */
public class ImportClipMap extends LoggingMap<String, AnimationClip> {

  /** our class logger */
  private static final Logger logger = Logger.getLogger(ImportClipMap.class.getName());

  /**
   * Add a clip to the store. Logs a warning if a clip by the same name was already in the store.
   * 
   * @param clip
   *          the clip to add.
   */
  public void put(final AnimationClip clip) {
    if (_wrappedMap.put(clip.getName(), clip) != null && isLogOnReplace()) {
      ImportClipMap.logger.warning("Replaced clip in ImportClipStore with same name. " + clip.getName());
    }
  }
}
