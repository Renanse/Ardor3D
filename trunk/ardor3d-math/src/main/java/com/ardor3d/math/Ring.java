/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.math;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.ardor3d.math.type.ReadOnlyRing;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>Ring</code> defines a flat ring or disk within three dimensional space that is specified via the ring's center
 * point, an up vector, an inner radius, and an outer radius.
 */

public class Ring implements Cloneable, Savable, Externalizable, ReadOnlyRing, Poolable {
    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Ring> RING_POOL = ObjectPool.create(Ring.class, MathConstants.maxMathPoolSize);

    private final Vector3 _center = new Vector3();
    private final Vector3 _up = new Vector3(Vector3.UNIT_Y);
    private double _innerRadius, _outerRadius;

    /**
     * Constructor creates a new <code>Ring</code> lying on the XZ plane, centered at the origin, with an inner radius
     * of zero and an outer radius of one (a unit disk).
     */
    public Ring() {
        _innerRadius = 0.0;
        _outerRadius = 1.0;
    }

    /**
     * Copy constructor.
     * 
     * @param source
     *            the ring to copy from.
     */
    public Ring(final ReadOnlyRing source) {
        this(source.getCenter(), source.getUp(), source.getInnerRadius(), source.getOuterRadius());
    }

    /**
     * Constructor creates a new <code>Ring</code> with defined center point, up vector, and inner and outer radii.
     * 
     * @param center
     *            the center of the ring.
     * @param up
     *            the unit up vector defining the ring's orientation.
     * @param innerRadius
     *            the ring's inner radius.
     * @param outerRadius
     *            the ring's outer radius.
     */
    public Ring(final ReadOnlyVector3 center, final ReadOnlyVector3 up, final double innerRadius,
            final double outerRadius) {
        _center.set(center);
        _up.set(up);
        _innerRadius = innerRadius;
        _outerRadius = outerRadius;
    }

    /**
     * Copy the value of the given source Ring into this Ring.
     * 
     * @param source
     *            the source of the data to copy.
     * @return this ring for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Ring set(final ReadOnlyRing source) {
        _center.set(source.getCenter());
        _up.set(source.getUp());
        _innerRadius = source.getInnerRadius();
        _outerRadius = source.getOuterRadius();
        return this;
    }

    /**
     * <code>getCenter</code> returns the center of the ring.
     * 
     * @return the center of the ring.
     */
    @Override
    public ReadOnlyVector3 getCenter() {
        return _center;
    }

    /**
     * <code>setCenter</code> sets the center of the ring.
     * 
     * @param center
     *            the center of the ring.
     */
    public void setCenter(final ReadOnlyVector3 center) {
        _center.set(center);
    }

    /**
     * <code>getUp</code> returns the ring's up vector.
     * 
     * @return the ring's up vector.
     */
    @Override
    public ReadOnlyVector3 getUp() {
        return _up;
    }

    /**
     * <code>setUp</code> sets the ring's up vector.
     * 
     * @param up
     *            the ring's up vector.
     */
    public void setUp(final ReadOnlyVector3 up) {
        _up.set(up);
    }

    /**
     * <code>getInnerRadius</code> returns the ring's inner radius.
     * 
     * @return the ring's inner radius.
     */
    @Override
    public double getInnerRadius() {
        return _innerRadius;
    }

    /**
     * <code>setInnerRadius</code> sets the ring's inner radius.
     * 
     * @param innerRadius
     *            the ring's inner radius.
     */
    public void setInnerRadius(final double innerRadius) {
        _innerRadius = innerRadius;
    }

    /**
     * <code>getOuterRadius</code> returns the ring's outer radius.
     * 
     * @return the ring's outer radius.
     */
    @Override
    public double getOuterRadius() {
        return _outerRadius;
    }

    /**
     * <code>setOuterRadius</code> sets the ring's outer radius.
     * 
     * @param outerRadius
     *            the ring's outer radius.
     */
    public void setOuterRadius(final double outerRadius) {
        _outerRadius = outerRadius;
    }

    /**
     * 
     * <code>random</code> returns a random point within the ring.
     * 
     * @param store
     *            Vector to store result in
     * @return a random point within the ring.
     */
    @Override
    public Vector3 random(final Vector3 store) {
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }

        final Vector3 b1 = Vector3.fetchTempInstance();
        final Vector3 b2 = Vector3.fetchTempInstance();

        // compute a random radius according to the ring area distribution
        final double inner2 = _innerRadius * _innerRadius;
        final double outer2 = _outerRadius * _outerRadius;
        final double r = Math.sqrt(inner2 + MathUtils.nextRandomFloat() * (outer2 - inner2));
        final double theta = MathUtils.nextRandomFloat() * MathUtils.TWO_PI;

        _up.cross(Vector3.UNIT_X, b1);
        if (b1.lengthSquared() < MathUtils.EPSILON) {
            _up.cross(Vector3.UNIT_Y, b1);
        }
        b1.normalizeLocal();
        _up.cross(b1, b2);
        result.set(b1).multiplyLocal(r * MathUtils.cos(theta)).addLocal(_center);
        b2.scaleAdd(r * MathUtils.sin(theta), result, result);

        Vector3.releaseTempInstance(b1);
        Vector3.releaseTempInstance(b2);

        return result;
    }

    /**
     * Check a ring... if it is null or its radii, or the doubles of its center or up are NaN or infinite, return false.
     * Else return true.
     * 
     * @param ring
     *            the ring to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final ReadOnlyRing ring) {
        if (ring == null) {
            return false;
        }
        if (Double.isNaN(ring.getInnerRadius()) || Double.isInfinite(ring.getInnerRadius())) {
            return false;
        }
        if (Double.isNaN(ring.getOuterRadius()) || Double.isInfinite(ring.getOuterRadius())) {
            return false;
        }

        return Vector3.isValid(ring.getCenter()) && Vector3.isValid(ring.getUp());
    }

    /**
     * @return the string representation of this ring.
     */
    @Override
    public String toString() {
        return "com.ardor3d.math.Ring [Center: " + _center + " Up: " + _up + " - radii, outer: " + _outerRadius
                + "  inner: " + _innerRadius + "]";
    }

    /**
     * @return returns a unique code for this ring object based on its values. If two rings are numerically equal, they
     *         will return the same hash code value.
     */
    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + _center.hashCode();
        result += 31 * result + _up.hashCode();

        final long ir = Double.doubleToLongBits(getInnerRadius());
        result += 31 * result + (int) (ir ^ ir >>> 32);

        final long or = Double.doubleToLongBits(getOuterRadius());
        result += 31 * result + (int) (or ^ or >>> 32);

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this ring and the provided ring have the same constant and normal values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyRing)) {
            return false;
        }
        final ReadOnlyRing comp = (ReadOnlyRing) o;
        return getInnerRadius() == comp.getInnerRadius() && getOuterRadius() == comp.getOuterRadius()
                && _up.equals(comp.getUp()) && _center.equals(comp.getCenter());

    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Ring clone() {
        return new Ring(this);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends Ring> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_center, "center", new Vector3(Vector3.ZERO));
        capsule.write(_up, "up", new Vector3(Vector3.UNIT_Z));
        capsule.write(_innerRadius, "innerRadius", 0.0);
        capsule.write(_outerRadius, "outerRadius", 1.0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _center.set((Vector3) capsule.readSavable("center", new Vector3(Vector3.ZERO)));
        _up.set((Vector3) capsule.readSavable("up", new Vector3(Vector3.UNIT_Z)));
        _innerRadius = capsule.readDouble("innerRadius", 0.0);
        _outerRadius = capsule.readDouble("outerRadius", 1.0);
    }

    // /////////////////
    // Methods for Externalizable
    // /////////////////

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param in
     *            ObjectInput
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        setCenter((Vector3) in.readObject());
        setUp((Vector3) in.readObject());
        setInnerRadius(in.readDouble());
        setOuterRadius(in.readDouble());
    }

    /**
     * Used with serialization. Not to be called manually.
     * 
     * @param out
     *            ObjectOutput
     * @throws IOException
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeObject(_center);
        out.writeObject(_up);
        out.writeDouble(_innerRadius);
        out.writeDouble(_outerRadius);
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Ring that is intended for temporary use in calculations and so forth. Multiple calls to
     *         the method should return instances of this class that are not currently in use.
     */
    public final static Ring fetchTempInstance() {
        if (MathConstants.useMathPools) {
            return Ring.RING_POOL.fetch();
        } else {
            return new Ring();
        }
    }

    /**
     * Releases a Ring back to be used by a future call to fetchTempInstance. TAKE CARE: this Ring object should no
     * longer have other classes referencing it or "Bad Things" will happen.
     * 
     * @param ring
     *            the Ring to release.
     */
    public final static void releaseTempInstance(final Ring ring) {
        if (MathConstants.useMathPools) {
            Ring.RING_POOL.release(ring);
        }
    }
}