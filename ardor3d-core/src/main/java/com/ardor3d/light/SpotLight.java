/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
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
 * SpotLight defines a light that has a location in space and emits light within a cone. This cone is defined by an
 * angle and inner angle, where the light falls off linearly between the two. Typically this light's values are
 * attenuated based on the distance of the point light and the object it illuminates.
 */
public class SpotLight extends PointLight {

    private static final long serialVersionUID = 1L;

    private float _angle, _innerAngle;
    private final Vector3 _direction = new Vector3(Vector3.UNIT_Z);

    /**
     * Constructor instantiates a new <code>SpotLight</code> object. The initial position of the light is (0,0,0) with
     * angle 0, and colors white.
     *
     */
    public SpotLight() {
        super();
        setAmbient(new ColorRGBA(0, 0, 0, 1));

        cachedUniforms.add(
                new UniformRef("angle", UniformType.Float1, UniformSource.Supplier, (Supplier<Float>) this::getAngle));
        cachedUniforms.add(new UniformRef("innerAngle", UniformType.Float1, UniformSource.Supplier,
                (Supplier<Float>) this::getInnerAngle));
        cachedUniforms.add(new UniformRef("direction", UniformType.Float3, UniformSource.Supplier,
                (Supplier<ReadOnlyVector3>) this::getDirection));
    }

    /**
     * @return the direction the spot light is pointing.
     */
    public ReadOnlyVector3 getDirection() {
        return _direction;
    }

    /**
     * @param direction
     *            the direction the spot light is pointing.
     */
    public void setDirection(final ReadOnlyVector3 direction) {
        _direction.set(direction);
    }

    /**
     * <code>setDirection</code> sets the direction the spot light is pointing.
     *
     * @param x
     *            the x value of the direction vector.
     * @param y
     *            the y value of the direction vector.
     * @param z
     *            the z value of the direction vector.
     */
    public void setDirection(final double x, final double y, final double z) {
        _direction.set(x, y, z);
    }

    /**
     * <code>getAngle</code> returns the angle of the spot light.
     *
     * @see #setAngle(float) for more info
     * @return the angle (in radians)
     */
    public float getAngle() {
        return _angle;
    }

    /**
     * <code>setAngle</code> sets the angle of focus of the spot light measured from the direction vector. Think of this
     * as the angle of a cone. Therefore, if you specify 10 degrees, you will get a 20 degree cone (10 degrees off
     * either side of the direction vector.)
     *
     * @param angle
     *            the angle (in radians)
     */
    public void setAngle(final float angle) {
        _angle = angle;
    }

    /**
     * <code>getInnerAngle</code> returns the inner angle of the spot light.
     *
     * @see #setInnerAngle(float) for more info
     * @return the inner angle (in radians)
     */
    public float getInnerAngle() {
        return _innerAngle;
    }

    /**
     * <code>setInnerAngle</code> sets the inner angle the spot light measured from the direction vector. This is where
     * falloff begins.
     *
     * @param angle
     *            the angle (in radians)
     */
    public void setInnerAngle(final float angle) {
        _innerAngle = angle;
    }

    /**
     * <code>getType</code> returns the type of this light (Type.Spot).
     *
     * @see com.ardor3d.light.Light#getType()
     */
    @Override
    public Type getType() {
        return Type.Spot;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_direction, "direction", new Vector3(Vector3.UNIT_Z));
        capsule.write(_angle, "angle", 0);
        capsule.write(_innerAngle, "innerAngle", 0);

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _direction.set(capsule.readSavable("direction", (Vector3) Vector3.UNIT_Z));
        _angle = capsule.readFloat("angle", 0);
        _innerAngle = capsule.readFloat("innerAngle", 0);
    }

}
