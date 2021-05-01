/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.framework;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * The purpose of this class is to own the update phase and separate update logic from the view.
 */
public interface Updater {
  @MainThread
  void init();

  @MainThread
  void update(final ReadOnlyTimer timer);
}
