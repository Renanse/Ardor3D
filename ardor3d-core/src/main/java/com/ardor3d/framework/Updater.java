/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * The purpose of this class is to own the update phase and separate update logic from the view.
 */
public interface Updater {
    @MainThread
    public void init();

    @MainThread
    public void update(final ReadOnlyTimer timer);
}
