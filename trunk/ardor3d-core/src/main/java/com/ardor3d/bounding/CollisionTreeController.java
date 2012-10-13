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
 * CollisionTreeController defines an interface for determining which collision tree to remove from a supplied cache.
 * The desired size is given for the controller to attempt to reduce the cache to, as well as a list of protected
 * elements that should <b>not</b> be removed from the cache.
 */
public interface CollisionTreeController {
    /**
     * clean will reduce the size of cache to the provided desiredSize. The protectedList defines elements that should
     * not be removed from the cache.
     * 
     * @param cache
     *            the cache to reduce.
     * @param protectedList
     *            the list of elements to not remove.
     * @param desiredSize
     *            the desiredSize of the final cache.
     */
    void clean(Map<Mesh, CollisionTree> cache, List<Mesh> protectedList, int desiredSize);
}
