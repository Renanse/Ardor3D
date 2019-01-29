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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.material.IUniformSupplier;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public class ColorSurface implements IUniformSupplier, Savable {

    public static final String DefaultPropertyKey = "surface";

    protected final ColorRGBA _ambient = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
    protected final ColorRGBA _diffuse = new ColorRGBA(.5f, .5f, .5f, 1f);
    protected final ColorRGBA _emissive = new ColorRGBA(0f, 0f, 0f, 1f);
    protected final ColorRGBA _specular = new ColorRGBA(1f, 1f, 1f, 1f);
    protected float _shininess = 32f;

    protected final List<UniformRef> _cachedUniforms = new ArrayList<>();

    public ColorSurface() {
        _cachedUniforms.add(new UniformRef("ambient", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyColorRGBA>) this::getAmbient));
        _cachedUniforms.add(new UniformRef("diffuse", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyColorRGBA>) this::getDiffuse));
        _cachedUniforms.add(new UniformRef("emissive", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyColorRGBA>) this::getEmissive));
        _cachedUniforms.add(new UniformRef("specular", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyColorRGBA>) this::getSpecular));
        _cachedUniforms.add(new UniformRef("shininess", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getShininess));
    }

    @Override
    public void applyDefaultUniformValues() {}

    public void setAmbient(final ReadOnlyColorRGBA color) {
        _ambient.set(color);
    }

    public ReadOnlyColorRGBA getAmbient() {
        return _ambient;
    }

    public void setDiffuse(final ReadOnlyColorRGBA color) {
        _diffuse.set(color);
    }

    public ReadOnlyColorRGBA getDiffuse() {
        return _diffuse;
    }

    public void setEmissive(final ReadOnlyColorRGBA color) {
        _emissive.set(color);
    }

    public ReadOnlyColorRGBA getEmissive() {
        return _emissive;
    }

    public void setSpecular(final ReadOnlyColorRGBA color) {
        _specular.set(color);
    }

    public ReadOnlyColorRGBA getSpecular() {
        return _specular;
    }

    public void setShininess(final float shininess) {
        _shininess = shininess;
    }

    public float getShininess() {
        return _shininess;
    }

    @Override
    public List<UniformRef> getUniforms() {
        return _cachedUniforms;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    /**
     * @see Savable#getClassTag()
     */
    public Class<? extends ColorSurface> getClassTag() {
        return this.getClass();
    }

    /**
     * @param capsule
     *            the input capsule
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @see Savable#read(InputCapsule)
     */
    public void read(final InputCapsule capsule) throws IOException {
        _ambient.set((ColorRGBA) capsule.readSavable("ambient", new ColorRGBA(0.1f, 0.1f, 0.1f, 1f)));
        _diffuse.set((ColorRGBA) capsule.readSavable("diffuse", new ColorRGBA(.5f, .5f, .5f, 1f)));
        _emissive.set((ColorRGBA) capsule.readSavable("emissive", (ColorRGBA) ColorRGBA.BLACK));
        _specular.set((ColorRGBA) capsule.readSavable("specular", (ColorRGBA) ColorRGBA.WHITE));
        _shininess = capsule.readFloat("shininess", 32f);
    }

    /**
     * @param capsule
     *            the capsule
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @see Savable#write(OutputCapsule)
     */
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_ambient, "ambient", new ColorRGBA(0.1f, 0.1f, 0.1f, 1f));
        capsule.write(_diffuse, "diffuse", new ColorRGBA(.5f, .5f, .5f, 1f));
        capsule.write(_emissive, "emissive", (ColorRGBA) ColorRGBA.BLACK);
        capsule.write(_specular, "specular", (ColorRGBA) ColorRGBA.WHITE);
        capsule.write(_shininess, "shininess", 32f);
    }
}
