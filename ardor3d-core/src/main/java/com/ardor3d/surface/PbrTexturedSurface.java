/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.surface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ardor3d.renderer.material.IUniformSupplier;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public class PbrTexturedSurface implements IUniformSupplier, Savable {

    public static final String DefaultPropertyKey = "surface";

    protected int _albedoMap = 0;
    protected int _normalMap = 1;
    protected int _metallicMap = 2;
    protected int _roughnessMap = 3;
    protected int _aoMap = 4;

    protected final List<UniformRef> cachedUniforms = new ArrayList<>();

    public PbrTexturedSurface() {
        cachedUniforms.add(new UniformRef("albedoMap", UniformType.Int1, UniformSource.Supplier,
                (Supplier<Integer>) this::getAlbedoMap));
        cachedUniforms.add(new UniformRef("normalMap", UniformType.Int1, UniformSource.Supplier,
                (Supplier<Integer>) this::getNormalMap));
        cachedUniforms.add(new UniformRef("metallicMap", UniformType.Int1, UniformSource.Supplier,
                (Supplier<Integer>) this::getMetallicMap));
        cachedUniforms.add(new UniformRef("roughnessMap", UniformType.Int1, UniformSource.Supplier,
                (Supplier<Integer>) this::getRoughnessMap));
        cachedUniforms.add(
                new UniformRef("aoMap", UniformType.Int1, UniformSource.Supplier, (Supplier<Integer>) this::getAoMap));
    }

    @Override
    public void applyDefaultUniformValues() {}

    public void setAlbedoMap(final int albedoMap) {
        _albedoMap = albedoMap;
    }

    public int getAlbedoMap() {
        return _albedoMap;
    }

    public void setNormalMap(final int normalMap) {
        _normalMap = normalMap;
    }

    public int getNormalMap() {
        return _normalMap;
    }

    public void setMetallicMap(final int metallicMap) {
        _metallicMap = metallicMap;
    }

    public int getMetallicMap() {
        return _metallicMap;
    }

    public void setRoughnessMap(final int roughnessMap) {
        _roughnessMap = roughnessMap;
    }

    public int getRoughnessMap() {
        return _roughnessMap;
    }

    public void setAoMap(final int aoMap) {
        _aoMap = aoMap;
    }

    public int getAoMap() {
        return _aoMap;
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
    public Class<? extends PbrTexturedSurface> getClassTag() {
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
        _albedoMap = capsule.readInt("albedoMap", 0);
        _normalMap = capsule.readInt("normalMap", 1);
        _metallicMap = capsule.readInt("metallicMap", 2);
        _roughnessMap = capsule.readInt("roughnessMap", 3);
        _aoMap = capsule.readInt("aoMap", 4);
    }

    /**
     * @param capsule
     *            the capsule
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     * @see Savable#write(OutputCapsule)
     */
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_albedoMap, "albedoMap", 0);
        capsule.write(_normalMap, "normalMap", 1);
        capsule.write(_metallicMap, "metallicMap", 2);
        capsule.write(_roughnessMap, "roughnessMap", 3);
        capsule.write(_aoMap, "aoMap", 4);
    }
}
