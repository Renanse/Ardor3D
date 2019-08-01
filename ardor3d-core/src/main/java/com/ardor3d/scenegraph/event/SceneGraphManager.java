/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.event;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.scenegraph.Spatial;

/**
 * SceneGraphManager is a convenience class for use when you want to have multiple listeners on a particular node.
 */
public class SceneGraphManager implements DirtyEventListener {
    private static SceneGraphManager sceneGraphManagerInstance;

    private final List<DirtyEventListener> _listeners;

    private SceneGraphManager() {
        _listeners = new ArrayList<DirtyEventListener>();
    }

    public static SceneGraphManager getSceneGraphManager() {
        if (sceneGraphManagerInstance == null) {
            sceneGraphManagerInstance = new SceneGraphManager();
        }

        return sceneGraphManagerInstance;
    }

    public void listenOnSpatial(final Spatial spatial) {
        spatial.setListener(this);
    }

    public void addDirtyEventListener(final DirtyEventListener listener) {
        _listeners.add(listener);
    }

    public void removeDirtyEventListener(final DirtyEventListener listener) {
        _listeners.remove(listener);
    }

    public boolean spatialDirty(final Spatial spatial, final DirtyType dirtyType) {
        for (final DirtyEventListener listener : _listeners) {
            listener.spatialDirty(spatial, dirtyType);
        }
        return false;
    }

    public boolean spatialClean(final Spatial spatial, final DirtyType dirtyType) {
        for (final DirtyEventListener listener : _listeners) {
            listener.spatialClean(spatial, dirtyType);
        }
        return false;
    }
}
