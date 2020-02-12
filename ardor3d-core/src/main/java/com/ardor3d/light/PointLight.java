/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.light;

import java.io.IOException;
import java.util.function.Supplier;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>PointLight</code> defines a light that has a location in space and emits light in all directions evenly. This
 * would be something similar to a light bulb. Typically this light's values are attenuated based on the distance of the
 * point light and the object it illuminates.
 */
public class PointLight extends Light {

    private static final long serialVersionUID = 1L;

    // Position of the light.
    private Vector3 _location;

    private float _constant = 1;
    private float _linear;
    private float _quadratic;

    /**
     * Constructor instantiates a new <code>PointLight</code> object. The initial position of the light is (0,0,0) and
     * it's colors are white.
     *
     */
    public PointLight() {
        super();
        _location = new Vector3();

        cachedUniforms.add(new UniformRef("position", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyVector3>) this::getLocation));
        cachedUniforms.add(new UniformRef("constant", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getConstant));
        cachedUniforms.add(new UniformRef("linear", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getLinear));
        cachedUniforms.add(new UniformRef("quadratic", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getQuadratic));
    }

    /**
     * <code>getLocation</code> returns the position of this light.
     *
     * @return the position of the light.
     */
    public ReadOnlyVector3 getLocation() {
        return _location;
    }

    /**
     * <code>setLocation</code> sets the position of the light.
     *
     * @param location
     *            the position of the light.
     */
    public void setLocation(final ReadOnlyVector3 location) {
        _location.set(location);
    }

    /**
     * <code>setLocation</code> sets the position of the light.
     *
     * @param x
     *            the x position of the light.
     * @param y
     *            the y position of the light.
     * @param z
     *            the z position of the light.
     */
    public void setLocation(final double x, final double y, final double z) {
        _location.set(x, y, z);
    }

    /**
     * <code>getConstant</code> returns the value for the constant attenuation.
     *
     * @return the value for the constant attenuation.
     */
    public float getConstant() {
        return _constant;
    }

    /**
     * <code>setConstant</code> sets the value for the constant attentuation.
     *
     * @param constant
     *            the value for the constant attenuation.
     */
    public void setConstant(final float constant) {
        _constant = constant;
    }

    /**
     * <code>getLinear</code> returns the value for the linear attenuation.
     *
     * @return the value for the linear attenuation.
     */
    public float getLinear() {
        return _linear;
    }

    /**
     * <code>setLinear</code> sets the value for the linear attentuation.
     *
     * @param linear
     *            the value for the linear attenuation.
     */
    public void setLinear(final float linear) {
        _linear = linear;
    }

    /**
     * <code>getQuadratic</code> returns the value for the quadratic attentuation.
     *
     * @return the value for the quadratic attenuation.
     */
    public float getQuadratic() {
        return _quadratic;
    }

    /**
     * <code>setQuadratic</code> sets the value for the quadratic attenuation.
     *
     * @param quadratic
     *            the value for the quadratic attenuation.
     */
    public void setQuadratic(final float quadratic) {
        _quadratic = quadratic;
    }

    @Override
    public void applyDefaultUniformValues() {
        setAmbient(ColorRGBA.BLACK_NO_ALPHA);
        setDiffuse(ColorRGBA.BLACK_NO_ALPHA);
        setSpecular(ColorRGBA.BLACK_NO_ALPHA);
        setConstant(1);
        setQuadratic(0);
        setLinear(0);
    }

    /**
     * <code>getType</code> returns the type of this light (Type.Point).
     *
     * @see com.ardor3d.light.Light#getType()
     */
    @Override
    public Type getType() {
        return Type.Point;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_location, "location", new Vector3(Vector3.ZERO));
        capsule.write(_constant, "constant", 1);
        capsule.write(_linear, "linear", 0);
        capsule.write(_quadratic, "quadratic", 0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _location = capsule.readSavable("location", (Vector3) Vector3.ZERO);
        _constant = capsule.readFloat("constant", 1);
        _linear = capsule.readFloat("linear", 0);
        _quadratic = capsule.readFloat("quadratic", 0);
    }

}
