/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.bounding;

import java.util.List;
import java.util.Map;

import com.ardor3d.scenegraph.Mesh;

/**
 * UsageTreeController defines a CollisionTreeController implementation that removes cache elements based on the
 * frequency of usage. By default, and implementation in the CollisionTreeManager, the cache's key set will be ordered
 * with the first element being the oldest used. Therefore, UsageTreeController simply removes elements from the cache
 * starting at the first key and working up until the desired size is reached or we run out of elements.
 */
public class UsageTreeController implements CollisionTreeController {

    /**
     * removes elements from cache (that are not in the protectedList) until the desiredSize is reached. It removes
     * elements from the keyset as they are ordered.
     * 
     * @param cache
     *            the cache to clean.
     * @param protectedList
     *            the list of elements to not remove.
     * @param desiredSize
     *            the final size of the cache to attempt to reach.
     */
    public void clean(final Map<Mesh, CollisionTree> cache, final List<Mesh> protectedList, final int desiredSize) {

        // get the ordered keyset (this will be ordered with oldest to newest).
        final Object[] set = cache.keySet().toArray();
        int count = 0;
        // go through the cache removing items that are not protected until the
        // size of the cache is small enough to return.
        while (cache.size() > desiredSize && count < set.length) {
            if (protectedList == null || !protectedList.contains(set[count])) {
                cache.remove(set[count]);
            }
            count++;
        }
    }

}
