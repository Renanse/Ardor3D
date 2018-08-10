/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.intersection;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.scenegraph.Mesh;

/**
 * <code>CollisionResults</code> stores the results of a collision test by storing an ArrayList of CollisionData.
 */
public abstract class CollisionResults {

    private final List<CollisionData> _nodeList;

    /**
     * Constructor instantiates a new <code>PickResults</code> object.
     */
    public CollisionResults() {
        _nodeList = new ArrayList<CollisionData>();
    }

    /**
     * <code>addCollisionData</code> places a new <code>CollisionData</code> object into the results list.
     * 
     * @param col
     *            The collision data to be placed in the results list.
     */
    public void addCollisionData(final CollisionData col) {
        _nodeList.add(col);
    }

    /**
     * <code>getNumber</code> retrieves the number of collisions that have been placed in the results.
     * 
     * @return the number of collisions in the list.
     */
    public int getNumber() {
        return _nodeList.size();
    }

    /**
     * <code>getCollisionData</code> retrieves a CollisionData from a specific index.
     * 
     * @param i
     *            the index requested.
     * @return the CollisionData at the specified index.
     */
    public CollisionData getCollisionData(final int i) {
        return _nodeList.get(i);
    }

    /**
     * <code>clear</code> clears the list of all CollisionData.
     */
    public void clear() {
        _nodeList.clear();
    }

    /**
     * 
     * <code>addCollision</code> is an abstract method whose intent is the subclass determines what to do when two Mesh
     * object's bounding volumes are determined to intersect.
     * 
     * @param s
     *            the first Mesh that intersects.
     * @param t
     *            the second Mesh that intersects.
     */
    public abstract void addCollision(Mesh s, Mesh t);

    /**
     * 
     * <code>processCollisions</code> is an abstract method whose intent is the subclass defines how to process the
     * collision data that has been collected since the last clear.
     * 
     * 
     */
    public abstract void processCollisions();

}