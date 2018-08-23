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

import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.google.common.collect.Lists;

public class MaterialTechnique implements Savable {

    protected String _name;

    protected final List<TechniquePass> _passes = Lists.newArrayList();

    public List<TechniquePass> getPasses() {
        return _passes;
    }

    public void setName(final String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public int getScore(final Mesh mesh) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String toString() {
        return "MaterialTechnique: " + getName();
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends MaterialTechnique> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_name, "name", null);
        capsule.writeSavableList(_passes, "passes", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _name = capsule.readString("name", null);

        final List<TechniquePass> pList = capsule.readSavableList("passes", null);
        _passes.clear();
        if (pList != null) {
            _passes.addAll(pList);
        }
    }
}
