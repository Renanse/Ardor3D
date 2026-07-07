/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget.gizmo;

/**
 * Identifies the individual interactive parts a gizmo can be composed of. Which parts are present,
 * and how a drag on each is interpreted, is up to the owning gizmo.
 */
public enum GizmoPart {
  /** Handle constrained to a single axis. */
  AxisX, AxisY, AxisZ,

  /** Handle constrained to a plane spanned by two axes. Named by the axes that span it. */
  PlaneXY, PlaneXZ, PlaneYZ,

  /** Center handle, interpreted against the camera's view plane. */
  Center,

  /** Ring handle rotating about a single axis. */
  RingX, RingY, RingZ,

  /** Screen-space ring handle rotating about the view direction. */
  RingView
}
