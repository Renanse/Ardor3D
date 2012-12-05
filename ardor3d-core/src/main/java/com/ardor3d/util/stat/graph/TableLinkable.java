/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat.graph;

import com.ardor3d.scenegraph.Line;
import com.ardor3d.util.stat.StatType;

/**
 * Interface describing the ability for a class to create or update a line with values, usually to match those in
 * another graph.
 */
public interface TableLinkable {

    /**
     * Update/Create a line to reflect the color, stipple, antialias and width used in the other graph.
     * 
     * @param type
     *            the StatType the Line is associated with.
     * @param lineKey
     *            the Line we want to update values on (if null, a new Line should be created.)
     * @return the updated (or created) Line
     */
    public Line updateLineKey(StatType type, Line lineKey);

}
