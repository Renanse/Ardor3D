/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material;

import java.util.ArrayList;
import java.util.List;

/**
 * RenderMaterial is a named collection of MaterialTechniques, intended to encapsulate different ways of rendering a
 * specific type of surface.
 */
public class RenderMaterial {

    protected String _name;

    protected final List<MaterialTechnique> _techniques = new ArrayList<>();

    public List<MaterialTechnique> getTechniques() {
        return _techniques;
    }

    public void addTechnique(final MaterialTechnique technique) {
        _techniques.add(technique);
    }

    public void setName(final String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    @Override
    public String toString() {
        return "RenderMaterial: " + getName();
    }
}
