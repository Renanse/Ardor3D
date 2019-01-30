/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.material;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.scenegraph.Renderable;

public class MaterialTechnique {

    protected String _name;

    protected final List<TechniquePass> _passes = new ArrayList<>();

    public List<TechniquePass> getPasses() {
        return _passes;
    }

    public void addPass(final TechniquePass pass) {
        _passes.add(pass);
    }

    public void setName(final String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public int getScore(final Renderable renderable) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String toString() {
        return "MaterialTechnique: " + getName();
    }
}
