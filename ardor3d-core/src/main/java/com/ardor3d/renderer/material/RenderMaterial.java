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

import java.io.IOException;
import java.util.List;

import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.google.common.collect.Lists;

/**
 * RenderMaterial is a named collection of MaterialTechniques, intended to encapsulate different ways of rendering a
 * specific type of surface.
 */
public class RenderMaterial implements Savable {

    protected String _name;

    protected final List<MaterialTechnique> _techniques = Lists.newArrayList();

    public List<MaterialTechnique> getTechniques() {
        return _techniques;
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

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends RenderMaterial> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_name, "name", null);
        capsule.writeSavableList(_techniques, "techniques", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _name = capsule.readString("name", null);

        final List<MaterialTechnique> tList = capsule.readSavableList("techniques", null);
        _techniques.clear();
        if (tList != null) {
            _techniques.addAll(tList);
        }
    }
}
