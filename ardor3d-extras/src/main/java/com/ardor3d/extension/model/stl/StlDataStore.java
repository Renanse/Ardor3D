/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.stl;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.geom.VertMap;

public class StlDataStore {

    public ColorRGBA defaultColor = new ColorRGBA(.6f, .6f, .6f, 1.0f);
    public Mesh mesh = new Mesh("scene");
    public VertMap vertMap;

    public Mesh getScene() {
        return mesh;
    }
}
