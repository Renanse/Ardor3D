/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.surface;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.material.IUniformSupplier;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;

public class ColorSurface implements IUniformSupplier {

    public final ColorRGBA ambient = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);

    public final ColorRGBA diffuse = new ColorRGBA(.5f, .5f, .5f, 1f);

    public final ColorRGBA specular = new ColorRGBA(1f, 1f, 1f, 1f);

    public float shininess = 32f;

    protected final List<UniformRef> cachedUniforms = new ArrayList<>();

    public ColorSurface() {
        cachedUniforms.add(new UniformRef("ambient", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyColorRGBA>) this::getAmbient));
        cachedUniforms.add(new UniformRef("diffuse", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyColorRGBA>) this::getDiffuse));
        cachedUniforms.add(new UniformRef("specular", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyColorRGBA>) this::getSpecular));
        cachedUniforms.add(new UniformRef("shininess", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getShininess));
    }

    @Override
    public void applyDefaultUniformValues() {}

    public ColorRGBA getAmbient() {
        return ambient;
    }

    public ColorRGBA getDiffuse() {
        return diffuse;
    }

    public ColorRGBA getSpecular() {
        return specular;
    }

    public float getShininess() {
        return shininess;
    }

    @Override
    public List<UniformRef> getUniforms() {
        return cachedUniforms;
    }
}
