/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.filter;

import java.util.Arrays;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.data.SpatialState;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyPlane;

public class PlaneBoundaryFilter extends UpdateFilterAdapter {
  private final ReadOnlyPlane[] _planes;
  private final Vector3 _calcVectorA = new Vector3();
  private final Vector3 _calcVectorB = new Vector3();

  public PlaneBoundaryFilter(final ReadOnlyPlane... planes) {
    _planes = Arrays.copyOf(planes, planes.length);
  }

  @Override
  public void applyFilter(final InteractManager manager, final AbstractInteractWidget widget) {
    final SpatialState state = manager.getSpatialState();
    _calcVectorA.set(state.getTransform().getTranslation());
    for (final ReadOnlyPlane plane : _planes) {
      final double distance = plane.pseudoDistance(_calcVectorA);
      if (distance < 0) {
        // push us back to the plane.
        _calcVectorB.set(plane.getNormal()).multiplyLocal(-distance);
        _calcVectorA.addLocal(_calcVectorB);
      }
    }

    state.getTransform().setTranslation(_calcVectorA);
  }
}
