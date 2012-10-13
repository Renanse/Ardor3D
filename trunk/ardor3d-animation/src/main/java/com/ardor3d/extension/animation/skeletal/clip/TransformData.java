/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.clip;

import java.io.IOException;

import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Describes a relative transform as a Quaternion-Vector-Vector tuple. We use QVV to make it simpler to do LERP
 * blending.
 */
public class TransformData implements Savable {

    /** Our rotation. */
    private final Quaternion _rotation = new Quaternion(Quaternion.IDENTITY);

    /** Our scale. */
    private final Vector3 _scale = new Vector3(Vector3.ONE);

    /** Our translation. */
    private final Vector3 _translation = new Vector3(Vector3.ZERO);

    /**
     * Construct a new, identity transform data object.
     */
    public TransformData() {}

    /**
     * Construct a new transform data object, copying the value of the given source.
     * 
     * @param source
     *            our source to copy.
     * @throws NullPointerException
     *             if source is null.
     */
    public TransformData(final TransformData source) {
        set(source);
    }

    /**
     * Copy the source's values into this transform data object.
     * 
     * @param source
     *            our source to copy.
     * @throws NullPointerException
     *             if source is null.
     */
    public void set(final TransformData source) {
        _rotation.set(source.getRotation());
        _scale.set(source.getScale());
        _translation.set(source.getTranslation());
    }

    public Quaternion getRotation() {
        return _rotation;
    }

    public void setRotation(final ReadOnlyQuaternion rotation) {
        _rotation.set(rotation);
    }

    public void setRotation(final double x, final double y, final double z, final double w) {
        _rotation.set(x, y, z, w);
    }

    public Vector3 getScale() {
        return _scale;
    }

    public void setScale(final ReadOnlyVector3 scale) {
        _scale.set(scale);
    }

    public void setScale(final double x, final double y, final double z) {
        _scale.set(x, y, z);
    }

    public Vector3 getTranslation() {
        return _translation;
    }

    public void setTranslation(final ReadOnlyVector3 translation) {
        _translation.set(translation);
    }

    public void setTranslation(final double x, final double y, final double z) {
        _translation.set(x, y, z);
    }

    public void applyTo(final Transform transform) {
        transform.setIdentity();
        transform.setRotation(getRotation());
        transform.setScale(getScale());
        transform.setTranslation(getTranslation());
    }

    public void applyTo(final Spatial spat) {
        spat.setTransform(Transform.IDENTITY);
        spat.setRotation(getRotation());
        spat.setScale(getScale());
        spat.setTranslation(getTranslation());
    }

    /**
     * Blend this transform with the given transform.
     * 
     * @param blendTo
     *            The transform to blend to
     * @param blendWeight
     *            The blend weight
     * @param store
     *            The transform store.
     * @return The blended transform.
     */
    public TransformData blend(final TransformData blendTo, final double blendWeight, final TransformData store) {
        TransformData tData = store;
        if (tData == null) {
            tData = new TransformData();
        }

        double weight, scaleX = 0.0, scaleY = 0.0, scaleZ = 0.0, transX = 0.0, transY = 0.0, transZ = 0.0;
        Vector3 vectorData;

        weight = 1 - blendWeight;

        vectorData = getTranslation();
        transX += vectorData.getX() * weight;
        transY += vectorData.getY() * weight;
        transZ += vectorData.getZ() * weight;

        vectorData = getScale();
        scaleX += vectorData.getX() * weight;
        scaleY += vectorData.getY() * weight;
        scaleZ += vectorData.getZ() * weight;

        weight = blendWeight;

        vectorData = blendTo.getTranslation();
        transX += vectorData.getX() * weight;
        transY += vectorData.getY() * weight;
        transZ += vectorData.getZ() * weight;

        vectorData = blendTo.getScale();
        scaleX += vectorData.getX() * weight;
        scaleY += vectorData.getY() * weight;
        scaleZ += vectorData.getZ() * weight;

        tData.setScale(scaleX, scaleY, scaleZ);
        tData.setTranslation(transX, transY, transZ);
        tData.setRotation(getRotation().slerpLocal(blendTo.getRotation(), weight));
        return tData;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends TransformData> getClassTag() {
        return this.getClass();
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_rotation, "rotation", new Quaternion(Quaternion.IDENTITY));
        capsule.write(_scale, "scale", new Vector3(Vector3.ONE));
        capsule.write(_translation, "translation", new Vector3(Vector3.ZERO));
    }

    public void read(final InputCapsule capsule) throws IOException {
        setRotation((Quaternion) capsule.readSavable("rotation", new Quaternion(Quaternion.IDENTITY)));
        setScale((Vector3) capsule.readSavable("scale", new Vector3(Vector3.ONE)));
        setTranslation((Vector3) capsule.readSavable("rotation", new Vector3(Vector3.ZERO)));
    }
}
