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

public class PbrSurface implements IUniformSupplier, Savable {

    public static final String DefaultPropertyKey = "surface";

    protected final ColorRGBA _albedo = new ColorRGBA(1f, 1f, 1f, 1f);
    protected float _metallic = .5f;
    protected float _roughness = .5f;
    protected float _ambientOcclusion = 1f;

    protected final List<UniformRef> cachedUniforms = new ArrayList<>();

    public PbrSurface() {
        cachedUniforms.add(new UniformRef("albedo", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyColorRGBA>) this::getAlbedo));
        cachedUniforms.add(new UniformRef("metallic", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getMetallic));
        cachedUniforms.add(new UniformRef("roughness", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getRoughness));
        cachedUniforms.add(new UniformRef("ao", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getAmbientOcclusion));
    }

    public PbrSurface(final ReadOnlyColorRGBA albedo, final float metallic, final float roughness,
            final float ambientOcclusion) {
        this();
        setAlbedo(albedo);
        setMetallic(metallic);
        setRoughness(roughness);
        setAmbientOcclusion(ambientOcclusion);
    }

    @Override
    public void applyDefaultUniformValues() {}

    public void setAlbedo(final ReadOnlyColorRGBA albedo) {
        _albedo.set(albedo);
    }

    public ReadOnlyColorRGBA getAlbedo() {
        return _albedo;
    }

    public void setMetallic(final float metallic) {
        _metallic = metallic;
    }

    public float getMetallic() {
        return _metallic;
    }

    public void setRoughness(final float roughness) {
        _roughness = roughness;
    }

    public float getRoughness() {
        return _roughness;
    }

    public void setAmbientOcclusion(final float ambientOcclusion) {
        _ambientOcclusion = ambientOcclusion;
    }

    public float getAmbientOcclusion() {
        return _ambientOcclusion;
    }

    @Override
    public List<UniformRef> getUniforms() {
        return cachedUniforms;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    /**
     * @see Savable#getClassTag()
     */
    public Class<? extends PbrSurface> getClassTag() {
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
        _albedo.set((ColorRGBA) capsule.readSavable("albedo", (ColorRGBA) ColorRGBA.WHITE));
        _metallic = capsule.readFloat("metallic", .5f);
        _roughness = capsule.readFloat("roughness", .5f);
        _ambientOcclusion = capsule.readFloat("ambientOcclusion", 1f);
    }

    /**
     * @param capsule
     *            the capsule
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @see Savable#write(OutputCapsule)
     */
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_albedo, "albedo", (ColorRGBA) ColorRGBA.WHITE);
        capsule.write(_metallic, "metallic", .5f);
        capsule.write(_roughness, "roughness", .5f);
        capsule.write(_ambientOcclusion, "ambientOcclusion", 1f);
    }
}