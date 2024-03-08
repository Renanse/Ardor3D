/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.intersection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ardor3d.math.Ray3;

/**
 * PickResults stores information created during ray intersection tests. The results will contain a
 * list of every {@link Pickable} element encountered in a pick test. Distance can be used to order
 * the results. If checkDistance is set to true, objects will be ordered with the first element in
 * the list being the closest picked object.
 */
public abstract class PickResults {

  private final List<PickData> _nodeList;
  private boolean _checkDistance;
  private DistanceComparator _distanceCompare;

  /**
   * Constructor instantiates a new <code>PickResults</code> object.
   */
  public PickResults() {
    _nodeList = new ArrayList<>();
  }

  /**
   * remember modification of the list to allow sorting after all picks have been added - not each
   * time.
   */
  private boolean modified = false;

  /**
   * Places a new geometry (enclosed in PickData) into the results list.
   * 
   * @param data
   *          the PickData to be placed in the results list.
   */
  public void addPickData(final PickData data) {
    _nodeList.add(data);
    modified = true;
  }

  /**
   * <code>getNumber</code> retrieves the number of geometries that have been placed in the results.
   * 
   * @return the number of Mesh objects in the list.
   */
  public int getNumber() { return _nodeList.size(); }

  /**
   * Retrieves a geometry (enclosed in PickData) from a specific index.
   * 
   * @param i
   *          the index requested.
   * @return the data at the specified index.
   */
  public PickData getPickData(final int i) {
    if (modified) {
      if (_checkDistance) {
        _nodeList.sort(_distanceCompare);
      }
      modified = false;
    }
    return _nodeList.get(i);
  }

  /**
   * <code>clear</code> clears the list of all Mesh objects.
   */
  public void clear() {
    _nodeList.clear();
  }

  /**
   * <code>addPick</code> generates an entry to be added to the list of picked objects. If
   * checkDistance is true, the implementing class should order the object.
   * 
   * @param ray
   *          the ray that was cast for the pick calculation.
   * @param p
   *          the pickable object to add to the pick data.
   */
  public abstract void addPick(Ray3 ray, Pickable p);

  /**
   * Optional method that can be implemented by sub classes to define methods for handling picked
   * objects. After calculating all pick results this method is called.
   * 
   */
  public void processPick() {}

  /**
   * Reports if these pick results will order the data by distance from the origin of the Ray.
   * 
   * @return true if objects will be ordered by distance, false otherwise.
   */
  public boolean willCheckDistance() {
    return _checkDistance;
  }

  /**
   * Sets if these pick results will order the data by distance from the origin of the Ray.
   * 
   * @param checkDistance
   *          true if objects will be ordered by distance, false otherwise.
   */
  public void setCheckDistance(final boolean checkDistance) {
    _checkDistance = checkDistance;
    if (checkDistance) {
      _distanceCompare = new DistanceComparator();
    }
  }

  /**
   * Implementation of comparator that uses the distance set in the pick data to order the objects.
   */
  private static class DistanceComparator implements Comparator<PickData> {

    @Override
    public int compare(final PickData o1, final PickData o2) {
      if (o1.getIntersectionRecord().getClosestDistance() <= o2.getIntersectionRecord().getClosestDistance()) {
        return -1;
      }

      return 1;
    }
  }

  public PickData findFirstIntersectingPickData() {
    int i = 0;
    while (getNumber() > 0 && getPickData(i).getIntersectionRecord().getNumberOfIntersections() == 0
        && ++i < getNumber()) {}
    return getNumber() > i ? getPickData(i) : null;
  }
}
