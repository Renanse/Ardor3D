/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.shadow.map;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.scenegraph.Spatial;

public class ShadowCasterManager {

  public static final ShadowCasterManager INSTANCE = new ShadowCasterManager();

  protected List<WeakReference<Spatial>> _spatialRefs = new ArrayList<>();
  protected List<WeakReference<Spatial>> _cleanup = new ArrayList<>();

  public void addSpatial(final Spatial spatial) {
    for (final WeakReference<Spatial> ref : _spatialRefs) {
      final Spatial spat = ref.get();
      if (spat == null) {
        _cleanup.add(ref);
      }
      if (spat == spatial) {
        return;
      }
    }
    _spatialRefs.add(new WeakReference<>(spatial));
    _spatialRefs.removeAll(_cleanup);
    _cleanup.clear();
  }

  public void removeSpatial(final Spatial spatial) {
    for (final WeakReference<Spatial> ref : _spatialRefs) {
      final Spatial spat = ref.get();
      if (spat == spatial) {
        _cleanup.add(ref);
      }
    }
    _spatialRefs.removeAll(_cleanup);
    _cleanup.clear();
  }

  List<WeakReference<Spatial>> getSpatialRefs() { return _spatialRefs; }
}
