/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.intersection;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;

/**
 * Guards the live pick-sort path (used by scene mouse-picking): PickResults must order picks
 * nearest-first, including when two picks share a distance. The tie case is what previously made the
 * distance comparator violate the Comparator contract.
 */
public class TestPickResultsOrdering {

  /** A Pickable whose world-bounds intersection sits at a single, fixed distance. */
  private static PickData pickAtDistance(final double distance) {
    final Pickable target = new Pickable() {
      @Override
      public boolean supportsBoundsIntersectionRecord() { return true; }

      @Override
      public boolean supportsPrimitivesIntersectionRecord() { return false; }

      @Override
      public boolean intersectsWorldBound(final Ray3 ray) { return true; }

      @Override
      public IntersectionRecord intersectsWorldBoundsWhere(final Ray3 ray) {
        return new IntersectionRecord(new double[] {distance}, new Vector3[] {new Vector3()});
      }

      @Override
      public IntersectionRecord intersectsPrimitivesWhere(final Ray3 ray) { return null; }
    };
    return new PickData(new Ray3(), target, true);
  }

  @Test
  public void testOrdersPicksNearestFirstWithTies() {
    final PickResults results = new PickResults() {
      @Override
      public void addPick(final Ray3 ray, final Pickable p) { /* unused: data added directly */ }
    };
    results.setCheckDistance(true);

    results.addPickData(pickAtDistance(5.0));
    results.addPickData(pickAtDistance(1.0));
    results.addPickData(pickAtDistance(3.0));
    results.addPickData(pickAtDistance(1.0)); // ties with the nearest

    // First getPickData triggers the sort; the rest read the sorted list.
    assertEquals(1.0, results.getPickData(0).getIntersectionRecord().getClosestDistance(), 0.0);
    assertEquals(1.0, results.getPickData(1).getIntersectionRecord().getClosestDistance(), 0.0);
    assertEquals(3.0, results.getPickData(2).getIntersectionRecord().getClosestDistance(), 0.0);
    assertEquals(5.0, results.getPickData(3).getIntersectionRecord().getClosestDistance(), 0.0);
  }
}
