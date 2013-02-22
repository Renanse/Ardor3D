/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.event;

/**
 * DirtyType contains the types of update that can occur on a spatial.
 */
public enum DirtyType {
    Transform, Bounding, Attached, Detached, Destroyed, RenderState
}
