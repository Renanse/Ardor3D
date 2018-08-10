/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * A Transform that checks if any of it's member values are null, NaN or Infinity, and throws an
 * InvalidTransformException if so.
 * 
 */
public class ValidatingTransform extends Transform {
    public ValidatingTransform() {}

    /**
     * Copy constructor
     * 
     * @param source
     */
    public ValidatingTransform(final ReadOnlyTransform source) {
        super(source);
        validate();
    }

    private void validate() {
        if (!Transform.isValid(this)) {
            throw new InvalidTransformException("Transform is invalid: " + this);
        }
    }

    @Override
    public Transform fromHomogeneousMatrix(final ReadOnlyMatrix4 matrix) {
        super.fromHomogeneousMatrix(matrix);
        validate();
        return this;
    }

    @Override
    public ValidatingTransform setRotation(final ReadOnlyMatrix3 rotation) {
        super.setRotation(rotation);
        validate();
        return this;
    }

    @Override
    public ValidatingTransform setRotation(final ReadOnlyQuaternion rotation) {
        super.setRotation(rotation);
        validate();
        return this;
    }

    @Override
    public ValidatingTransform setScale(final double x, final double y, final double z) {
        super.setScale(x, y, z);
        validate();
        return this;
    }

    @Override
    public ValidatingTransform setScale(final double uniformScale) {
        super.setScale(uniformScale);
        validate();
        return this;
    }

    @Override
    public ValidatingTransform setScale(final ReadOnlyVector3 scale) {
        super.setScale(scale);
        validate();
        return this;
    }

    @Override
    public ValidatingTransform setTranslation(final double x, final double y, final double z) {
        super.setTranslation(x, y, z);
        validate();
        return this;
    }

    @Override
    public ValidatingTransform setTranslation(final ReadOnlyVector3 translation) {
        super.setTranslation(translation);
        validate();
        return this;
    }

    @Override
    public Transform translate(final double x, final double y, final double z) {
        super.translate(x, y, z);
        validate();
        return this;
    }

    @Override
    public Transform translate(final ReadOnlyVector3 vec) {
        super.translate(vec);
        validate();
        return this;
    }

    @Override
    public Transform multiply(final ReadOnlyTransform transformBy, final Transform store) {
        final Transform transform = super.multiply(transformBy, store);
        if (!Transform.isValid(transform)) {
            throw new InvalidTransformException("Transform is invalid");
        }
        return transform;
    }

    @Override
    public Transform invert(final Transform store) {
        final Transform transform = super.invert(store);
        if (!Transform.isValid(transform)) {
            throw new InvalidTransformException("Transform is invalid");
        }
        return transform;
    }

    @Override
    public Transform set(final ReadOnlyTransform source) {
        super.set(source);
        validate();
        return this;
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public ValidatingTransform clone() {
        return new ValidatingTransform(this);
    }

}
