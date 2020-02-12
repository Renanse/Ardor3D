/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material.fog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.material.IUniformSupplier;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;

public class FogParams implements IUniformSupplier {

    public static final String DefaultPropertyKey = "fogParams";

    public enum DensityFunction {
        /**
         * The fog blending function defined as: (end - z) / (end - start).
         */
        Linear,
        /**
         * The fog blending function defined as: e^-(density*z)
         */
        Exponential,
        /**
         * The fog blending function defined as: e^((-density*z)^2)
         */
        ExponentialSquared;
    }

    protected ColorRGBA _fogColor = new ColorRGBA(ColorRGBA.WHITE);
    protected float _start = 0f;
    protected float _end = 1f;
    protected float _density = 0f;
    protected DensityFunction _function = DensityFunction.Exponential;

    protected final List<UniformRef> _cachedUniforms = new ArrayList<>();

    public FogParams() {
        _cachedUniforms.add(new UniformRef("color", UniformType.Float4, UniformSource.Supplier,
                (Supplier<ReadOnlyColorRGBA>) this::getColor));
        _cachedUniforms.add(
                new UniformRef("start", UniformType.Float1, UniformSource.Supplier, (Supplier<Float>) this::getStart));
        _cachedUniforms
                .add(new UniformRef("end", UniformType.Float1, UniformSource.Supplier, (Supplier<Float>) this::getEnd));
        _cachedUniforms.add(new UniformRef("density", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getDensity));
        _cachedUniforms.add(new UniformRef("function", UniformType.Int1, UniformSource.Supplier,
                (Supplier<Integer>) this::getFunctionOrdinal));
    }

    public FogParams(final float start, final float end) {
        this();
        setStart(start);
        setEnd(end);
        setFunction(DensityFunction.Linear);
    }

    public FogParams(final float density, final DensityFunction function) {
        this();
        setDensity(density);
        setFunction(function);
    }

    @Override
    public void applyDefaultUniformValues() {
        setColor(ColorRGBA.WHITE);
    }

    public void setColor(final ReadOnlyColorRGBA color) {
        _fogColor.set(color);
    }

    public ReadOnlyColorRGBA getColor() {
        return _fogColor;
    }

    public void setStart(final float start) {
        _start = start;
    }

    public float getStart() {
        return _start;
    }

    public void setEnd(final float end) {
        _end = end;
    }

    public float getEnd() {
        return _end;
    }

    public void setDensity(final float density) {
        _density = density;
    }

    public float getDensity() {
        return _density;
    }

    public void setFunction(final DensityFunction function) {
        _function = function;
    }

    public DensityFunction getFunction() {
        return _function;
    }

    public int getFunctionOrdinal() {
        return _function.ordinal();
    }

    @Override
    public List<UniformRef> getUniforms() {
        return _cachedUniforms;
    }
}
