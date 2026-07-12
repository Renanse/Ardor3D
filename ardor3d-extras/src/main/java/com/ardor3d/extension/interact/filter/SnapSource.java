/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.filter;

/**
 * A filter that snaps a drag to fixed increments and can report that increment, so a widget can
 * visualize the snap grid (e.g. draw tick marks). The increment is in the filter's own quantity:
 * radians for a rotation snap, world units for a translation grid.
 */
public interface SnapSource {

  /**
   * @return true if this filter is currently snapping (enabled with a positive increment), so a
   *         widget should show its snap grid.
   */
  boolean isSnapping();

  /**
   * @return the snap increment, in the filter's native quantity (radians or world units). Only
   *         meaningful when {@link #isSnapping()} is true.
   */
  double getSnapIncrement();
}
