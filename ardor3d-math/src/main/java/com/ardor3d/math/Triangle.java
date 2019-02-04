/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc..
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

import com.ardor3d.math.type.ReadOnlyTriangle;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Triangle is a math class defining a three sided polygon by three points in space.
 */
public class Triangle implements Cloneable, Savable, Externalizable, ReadOnlyTriangle, Poolable {

    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Triangle> TRI_POOL = ObjectPool.create(Triangle.class,
            MathConstants.maxMathPoolSize);

    protected final Vector3 _pointA = new Vector3();
    protected final Vector3 _pointB = new Vector3();
    protected final Vector3 _pointC = new Vector3();

    protected transient Vector3 _center;
    protected transient Vector3 _normal;

    protected int _index = 0;

    private boolean _dirtyNormal = true;

    private boolean _dirtyCenter = true;

    /**
     * Construct a new, mutable triangle with all points at 0,0,0 and an index of 0.
     */
    public Triangle() {}

    /**
     * Copy constructor.
     *
     * @param source
     *            the triangle to copy from.
     */
    public Triangle(final ReadOnlyTriangle source) {
        this(source.getA(), source.getB(), source.getC(), source.getIndex());
    }

    /**
     * Construct a new, mutable triangle using the given points and an index of 0.
     *
     * @param pointA
     * @param pointB
     * @param pointC
     */
    public Triangle(final ReadOnlyVector3 pointA, final ReadOnlyVector3 pointB, final ReadOnlyVector3 pointC) {
        this(pointA, pointB, pointC, 0);
    }

    /**
     * Constructs a new triangle using the given points and index.
     *
     * @param pointA
     * @param pointB
     * @param pointC
     * @param index
     */
    public Triangle(final ReadOnlyVector3 pointA, final ReadOnlyVector3 pointB, final ReadOnlyVector3 pointC,
            final int index) {
        _pointA.set(pointA);
        _pointB.set(pointB);
        _pointC.set(pointC);
        _index = index;
    }

    /**
     * Copies the values of the given source Triangle into this Triangle.
     *
     * @param source
     * @return this Triangle for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Triangle set(final ReadOnlyTriangle source) {
        _pointA.set(source.getA());
        _pointB.set(source.getB());
        _pointC.set(source.getC());
        _index = source.getIndex();
        return this;
    }

    @Override
    public int getIndex() {
        return _index;
    }

    @Override
    public ReadOnlyVector3 get(final int index) {
        switch (index) {
            case 0:
                return getA();
            case 1:
                return getB();
            case 2:
                return getC();
        }
        throw new IllegalArgumentException("invalid index: " + index);
    }

    @Override
    public ReadOnlyVector3 getA() {
        return _pointA;
    }

    @Override
    public ReadOnlyVector3 getB() {
        return _pointB;
    }

    @Override
    public ReadOnlyVector3 getC() {
        return _pointC;
    }

    /**
     * Obtains the unit length normal vector of this triangle... Will create and recalculate this normal vector if this
     * is the first request, or if one of the points on the triangle has changed since the last request.
     *
     * @return the normal vector
     * @throws NullPointerException
     *             if store is null.
     */
    @Override
    public ReadOnlyVector3 getNormal() {
        if (_dirtyNormal) {
            calculateNormal();
        }
        return _normal;
    }

    /**
     * Obtains the center point of this triangle... Will create and recalculate this point if this is the first request,
     * or if one of the points on the triangle has changed since the last request.
     */
    @Override
    public ReadOnlyVector3 getCenter() {
        if (_dirtyCenter) {
            calculateCenter();
        }
        return _center;
    }

    /**
     * Sets the index value of this triangle to the given int value.
     *
     * @param index
     */
    public void setIndex(final int index) {
        _index = index;
    }

    /**
     * Sets the first point of this triangle to the values of the given vector.
     *
     * @param pointA
     */
    public void setA(final ReadOnlyVector3 pointA) {
        _pointA.set(pointA);
        _dirtyCenter = _dirtyNormal = true;
    }

    /**
     * Sets the second point of this triangle to the values of the given vector.
     *
     * @param pointB
     */
    public void setB(final ReadOnlyVector3 pointB) {
        _pointB.set(pointB);
        _dirtyCenter = _dirtyNormal = true;
    }

    /**
     * Sets the third point of this triangle to the values of the given vector.
     *
     * @param pointC
     */
    public void setC(final ReadOnlyVector3 pointC) {
        _pointC.set(pointC);
        _dirtyCenter = _dirtyNormal = true;
    }

    /**
     * Sets a point to a new value.
     *
     * @param index
     *            the index of the point to set (0-2, corresponding to A-C)
     * @param point
     *            the new value
     * @throws IllegalArgumentException
     *             if index is not in [0, 2]
     */
    public void set(final int index, final ReadOnlyVector3 point) {
        switch (index) {
            case 0:
                setA(point);
                return;
            case 1:
                setB(point);
                return;
            case 2:
                setC(point);
                return;
        }
        throw new IllegalArgumentException("index must be 0, 1 or 2 (corresponding to A, B or C.)");
    }

    /**
     * Recalculates the center point of this triangle by averaging the triangle's three points.
     */
    protected void calculateCenter() {
        if (_center == null) {
            _center = _pointA.clone();
        } else {
            _center.set(_pointA);
        }
        _center.addLocal(_pointB).addLocal(_pointC).multiplyLocal(MathUtils.ONE_THIRD);
        _dirtyCenter = false;
    }

    /**
     * Recalculates the surface normal of the triangle by crossing the vectors formed by BA and CA.
     */
    protected void calculateNormal() {
        if (_normal == null) {
            _normal = _pointB.clone();
        } else {
            _normal.set(_pointB);
        }
        _normal.subtractLocal(_pointA).crossLocal(_pointC.getX() - _pointA.getX(), _pointC.getY() - _pointA.getY(),
                _pointC.getZ() - _pointA.getZ());
        _normal.normalizeLocal();
        _dirtyNormal = false;
    }

    /**
     * Check a triangle... if it is null or its points are invalid, return false. Else return true.
     *
     * @param triangle
     *            the triangle to check
     * @return true or false as stated above.
     */
    public static boolean isValid(final Triangle triangle) {
        if (triangle == null) {
            return false;
        }
        if (!Vector3.isValid(triangle._pointA) || !Vector3.isValid(triangle._pointB)
                || !Vector3.isValid(triangle._pointC)) {
            return false;
        }

        return true;
    }

    /**
     * @return the string representation of this triangle.
     */
    @Override
    public String toString() {
        return "com.ardor3d.math.Triangle [A: " + _pointA + " - B: " + _pointB + " - C: " + _pointC + " - Index: "
                + _index + "]";
    }

    /**
     * @return returns a unique code for this triangle object based on its values. If two triangles have the same points
     *         and index, they will return the same hash code value.
     */
    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + _pointA.hashCode();
        result += 31 * result + _pointB.hashCode();
        result += 31 * result + _pointC.hashCode();
        result += 31 * result + _index;

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this triangle and the provided triangle have the same index and point values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyTriangle)) {
            return false;
        }
        final ReadOnlyTriangle comp = (ReadOnlyTriangle) o;
        return _index == comp.getIndex() && _pointA.equals(comp.getA()) && _pointB.equals(comp.getB())
                && _pointC.equals(comp.getC());
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Triangle clone() {
        return new Triangle(this);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends Triangle> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_pointA, "a", (Vector3) Vector3.ZERO);
        capsule.write(_pointB, "b", (Vector3) Vector3.ZERO);
        capsule.write(_pointC, "c", (Vector3) Vector3.ZERO);
        capsule.write(_index, "index", 0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _pointA.set(capsule.readSavable("a", (Vector3) Vector3.ZERO));
        _pointB.set(capsule.readSavable("b", (Vector3) Vector3.ZERO));
        _pointC.set(capsule.readSavable("c", (Vector3) Vector3.ZERO));
        _index = capsule.readInt("index", 0);
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
        setA((Vector3) in.readObject());
        setB((Vector3) in.readObject());
        setC((Vector3) in.readObject());
        setIndex(in.readInt());
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
        out.writeObject(_pointA);
        out.writeObject(_pointB);
        out.writeObject(_pointC);
        out.writeInt(getIndex());
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Triangle that is intended for temporary use in calculations and so forth. Multiple calls
     *         to the method should return instances of this class that are not currently in use.
     */
    public final static Triangle fetchTempInstance() {
        if (MathConstants.useMathPools) {
            return Triangle.TRI_POOL.fetch();
        } else {
            return new Triangle();
        }
    }

    /**
     * Releases a Triangle back to be used by a future call to fetchTempInstance. TAKE CARE: this Triangle object should
     * no longer have other classes referencing it or "Bad Things" will happen.
     *
     * @param tri
     *            the Triangle to release.
     */
    public final static void releaseTempInstance(final Triangle tri) {
        if (MathConstants.useMathPools) {
            Triangle.TRI_POOL.release(tri);
        }
    }
}
