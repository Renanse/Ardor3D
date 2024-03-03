/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.state.loader;

/**
 * Storage class for items required during Layer import.
 */
public class InputStore {

  private final ImportClipMap _clips = new ImportClipMap();

  public ImportClipMap getClips() { return _clips; }
}
