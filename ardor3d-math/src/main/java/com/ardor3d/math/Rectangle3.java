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

import com.ardor3d.math.type.ReadOnlyRectangle3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * Defines a finite plane within three dimensional space that is specified via three points (A, B, C). These three
 * points define a triangle with the forth point defining the rectangle ((B + C) - A.
 */

public class Rectangle3 implements Cloneable, Savable, Externalizable, ReadOnlyRectangle3, Poolable {
    private static final long serialVersionUID = 1L;

    private static final ObjectPool<Rectangle3> RECTANGLE_POOL = ObjectPool.create(Rectangle3.class,
            MathConstants.maxMathPoolSize);

    private final Vector3 _a = new Vector3();
    private final Vector3 _b = new Vector3();
    private final Vector3 _c = new Vector3();

    /**
     * Constructor creates a new Rectangle3 with corners at origin.
     */
    public Rectangle3() {}

    /**
     * Constructor creates a new Rectangle3 using the values of the provided source rectangle.
     * 
     * @param source
     *            the rectangle to copy from
     */
    public Rectangle3(final ReadOnlyRectangle3 source) {
        this(source.getA(), source.getB(), source.getC());
    }

    /**
     * Constructor creates a new Rectangle3 with defined A, B, and C points that define the area of the rectangle.
     * 
     * @param a
     *            the first corner of the rectangle.
     * @param b
     *            the second corner of the rectangle.
     * @param c
     *            the third corner of the rectangle.
     */
    public Rectangle3(final ReadOnlyVector3 a, final ReadOnlyVector3 b, final ReadOnlyVector3 c) {
        setA(a);
        setB(b);
        setC(c);
    }

    /**
     * Copy the value of the given source Rectangle3 into this Rectangle3.
     * 
     * @param source
     *            the source of the data to copy.
     * @return this rectangle for chaining
     * @throws NullPointerException
     *             if source is null.
     */
    public Rectangle3 set(final ReadOnlyRectangle3 source) {
        _a.set(source.getA());
        _b.set(source.getB());
        _c.set(source.getC());
        return this;
    }

    /**
     * getA returns the first point of the rectangle.
     * 
     * @return the first point of the rectangle.
     */
    @Override
    public ReadOnlyVector3 getA() {
        return _a;
    }

    /**
     * setA sets the first point of the rectangle.
     * 
     * @param a
     *            the first point of the rectangle.
     */
    public void setA(final ReadOnlyVector3 a) {
        _a.set(a);
    }

    /**
     * getB returns the second point of the rectangle.
     * 
     * @return the second point of the rectangle.
     */
    @Override
    public ReadOnlyVector3 getB() {
        return _b;
    }

    /**
     * setB sets the second point of the rectangle.
     * 
     * @param b
     *            the second point of the rectangle.
     */
    public void setB(final ReadOnlyVector3 b) {
        _b.set(b);
    }

    /**
     * getC returns the third point of the rectangle.
     * 
     * @return the third point of the rectangle.
     */
    @Override
    public ReadOnlyVector3 getC() {
        return _c;
    }

    /**
     * setC sets the third point of the rectangle.
     * 
     * @param c
     *            the third point of the rectangle.
     */
    public void setC(final ReadOnlyVector3 c) {
        _c.set(c);
    }

    /**
     * random returns a random point within the plane defined by: A, B, C, and (B + C) - A.
     * 
     * @param result
     *            Vector to store result in
     * @return a random point within the rectangle.
     */
    @Override
    public Vector3 random(Vector3 result) {
        if (result == null) {
            result = new Vector3();
        }

        final double s = MathUtils.nextRandomFloat();
        final double t = MathUtils.nextRandomFloat();

        final double aMod = 1.0 - s - t;
        final Vector3 temp1 = Vector3.fetchTempInstance();
        final Vector3 temp2 = Vector3.fetchTempInstance();
        final Vector3 temp3 = Vector3.fetchTempInstance();
        result.set(_a.multiply(aMod, temp1).addLocal(_b.multiply(s, temp2).addLocal(_c.multiply(t, temp3))));
        Vector3.releaseTempInstance(temp1);
        Vector3.releaseTempInstance(temp2);
        Vector3.releaseTempInstance(temp3);
        return result;
    }

    /**
     * @return the string representation of this rectangle.
     */
    @Override
    public String toString() {
        return "com.ardor3d.math.Rectangle3 [A: " + _a + " B: " + _b + " C: " + _c + "]";
    }

    /**
     * @return returns a unique code for this rectangle object based on its values. If two rectangles are numerically
     *         equal, they will return the same hash code value.
     */
    @Override
    public int hashCode() {
        int result = 17;

        result += 31 * result + _a.hashCode();
        result += 31 * result + _b.hashCode();
        result += 31 * result + _c.hashCode();

        return result;
    }

    /**
     * @param o
     *            the object to compare for equality
     * @return true if this rectangle and the provided rectangle have the same corner values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReadOnlyRectangle3)) {
            return false;
        }
        final ReadOnlyRectangle3 comp = (ReadOnlyRectangle3) o;
        return _a.equals(comp.getA()) && _b.equals(comp.getB()) && _c.equals(comp.getC());
    }

    // /////////////////
    // Method for Cloneable
    // /////////////////

    @Override
    public Rectangle3 clone() {
        return new Rectangle3(this);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_a, "a", new Vector3(Vector3.ZERO));
        capsule.write(_b, "b", new Vector3(Vector3.ZERO));
        capsule.write(_c, "c", new Vector3(Vector3.ZERO));
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _a.set((Vector3) capsule.readSavable("a", new Vector3(Vector3.ZERO)));
        _b.set((Vector3) capsule.readSavable("b", new Vector3(Vector3.ZERO)));
        _c.set((Vector3) capsule.readSavable("c", new Vector3(Vector3.ZERO)));
    }

    @Override
    public Class<? extends Rectangle3> getClassTag() {
        return this.getClass();
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
        out.writeObject(_a);
        out.writeObject(_b);
        out.writeObject(_c);
    }

    // /////////////////
    // Methods for creating temp variables (pooling)
    // /////////////////

    /**
     * @return An instance of Rectangle3 that is intended for temporary use in calculations and so forth. Multiple calls
     *         to the method should return instances of this class that are not currently in use.
     */
    public final static Rectangle3 fetchTempInstance() {
        if (MathConstants.useMathPools) {
            return Rectangle3.RECTANGLE_POOL.fetch();
        } else {
            return new Rectangle3();
        }
    }

    /**
     * Releases a Rectangle3 back to be used by a future call to fetchTempInstance. TAKE CARE: this object should no
     * longer have other classes referencing it or "Bad Things" will happen.
     * 
     * @param rectangle
     *            the Rectangle3 to release.
     */
    public final static void releaseTempInstance(final Rectangle3 rectangle) {
        if (MathConstants.useMathPools) {
            Rectangle3.RECTANGLE_POOL.release(rectangle);
        }
    }
}
