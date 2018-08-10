/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.bounding;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyPlane;
import com.ardor3d.math.type.ReadOnlyPlane.Side;
import com.ardor3d.math.type.ReadOnlyRay3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>BoundingBox</code> defines an axis-aligned cube that defines a container for a group of vertices of a
 * particular piece of geometry. This box defines a center and extents from that center along the x, y and z axis. <br>
 * <br>
 * A typical usage is to allow the class define the center and radius by calling either <code>containAABB</code> or
 * <code>averagePoints</code>. A call to <code>computeFramePoint</code> in turn calls <code>containAABB</code>.
 */
public class BoundingBox extends BoundingVolume {

    private static final long serialVersionUID = 1L;

    private double _xExtent, _yExtent, _zExtent;

    /**
     * Default constructor instantiates a new <code>BoundingBox</code> object.
     */
    public BoundingBox() {}

    /**
     * Constructor instantiates a new <code>BoundingBox</code> object with given values.
     */
    public BoundingBox(final BoundingBox other) {
        this(other.getCenter(), other.getXExtent(), other.getYExtent(), other.getZExtent());
    }

    /**
     * Constructor instantiates a new <code>BoundingBox</code> object with given values.
     */
    public BoundingBox(final ReadOnlyVector3 c, final double x, final double y, final double z) {
        _center.set(c);
        setXExtent(x);
        setYExtent(y);
        setZExtent(z);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof BoundingBox)) {
            return false;
        }
        final BoundingBox b = (BoundingBox) other;
        return _center.equals(b._center) && _xExtent == b._xExtent && _yExtent == b._yExtent && _zExtent == b._zExtent;
    }

    @Override
    public Type getType() {
        return Type.AABB;
    }

    public void setXExtent(final double xExtent) {
        _xExtent = xExtent;
    }

    public double getXExtent() {
        return _xExtent;
    }

    public void setYExtent(final double yExtent) {
        _yExtent = yExtent;
    }

    public double getYExtent() {
        return _yExtent;
    }

    public void setZExtent(final double zExtent) {
        _zExtent = zExtent;
    }

    public double getZExtent() {
        return _zExtent;
    }

    @Override
    public double getRadius() {
        return MathUtils.sqrt(_xExtent * _xExtent + _yExtent * _yExtent + _zExtent * _zExtent);
    }

    // Some transform matrices are not in decomposed form and in this
    // situation we need to use a different, more robust, algorithm
    // for computing the new bounding box.
    @Override
    public BoundingVolume transform(final ReadOnlyTransform transform, final BoundingVolume store) {

        if (transform.isRotationMatrix()) {
            return transformRotational(transform, store);
        }

        BoundingBox box;
        if (store == null || store.getType() != Type.AABB) {
            box = new BoundingBox();
        } else {
            box = (BoundingBox) store;
        }

        final Vector3[] corners = new Vector3[8];
        for (int i = 0; i < corners.length; i++) {
            corners[i] = Vector3.fetchTempInstance();
        }
        getCorners(corners);

        // Transform all of these points by the transform
        for (int i = 0; i < corners.length; i++) {
            transform.applyForward(corners[i]);
        }
        // Now compute based on these transformed points
        double minX = corners[0].getX();
        double minY = corners[0].getY();
        double minZ = corners[0].getZ();
        double maxX = minX;
        double maxY = minY;
        double maxZ = minZ;
        for (int i = 1; i < corners.length; i++) {
            final double curX = corners[i].getX();
            final double curY = corners[i].getY();
            final double curZ = corners[i].getZ();
            minX = Math.min(minX, curX);
            minY = Math.min(minY, curY);
            minZ = Math.min(minZ, curZ);
            maxX = Math.max(maxX, curX);
            maxY = Math.max(maxY, curY);
            maxZ = Math.max(maxZ, curZ);
        }

        final double ctrX = (maxX + minX) * 0.5;
        final double ctrY = (maxY + minY) * 0.5;
        final double ctrZ = (maxZ + minZ) * 0.5;

        box._center.set(ctrX, ctrY, ctrZ);
        box._xExtent = maxX - ctrX;
        box._yExtent = maxY - ctrY;
        box._zExtent = maxZ - ctrZ;

        for (int i = 0; i < corners.length; i++) {
            Vector3.releaseTempInstance(corners[i]);
        }

        return box;
    }

    public BoundingVolume transformRotational(final ReadOnlyTransform transform, final BoundingVolume store) {

        final ReadOnlyMatrix3 rotate = transform.getMatrix();
        final ReadOnlyVector3 scale = transform.getScale();
        final ReadOnlyVector3 translate = transform.getTranslation();

        BoundingBox box;
        if (store == null || store.getType() != Type.AABB) {
            box = new BoundingBox();
        } else {
            box = (BoundingBox) store;
        }

        _center.multiply(scale, box._center);
        rotate.applyPost(box._center, box._center);
        box._center.addLocal(translate);

        final Matrix3 transMatrix = Matrix3.fetchTempInstance();
        transMatrix.set(rotate);
        // Make the rotation matrix all positive to get the maximum x/y/z extent
        transMatrix.setValue(0, 0, Math.abs(transMatrix.getM00()));
        transMatrix.setValue(0, 1, Math.abs(transMatrix.getM01()));
        transMatrix.setValue(0, 2, Math.abs(transMatrix.getM02()));
        transMatrix.setValue(1, 0, Math.abs(transMatrix.getM10()));
        transMatrix.setValue(1, 1, Math.abs(transMatrix.getM11()));
        transMatrix.setValue(1, 2, Math.abs(transMatrix.getM12()));
        transMatrix.setValue(2, 0, Math.abs(transMatrix.getM20()));
        transMatrix.setValue(2, 1, Math.abs(transMatrix.getM21()));
        transMatrix.setValue(2, 2, Math.abs(transMatrix.getM22()));

        _compVect1.set(getXExtent() * scale.getX(), getYExtent() * scale.getY(), getZExtent() * scale.getZ());
        transMatrix.applyPost(_compVect1, _compVect1);
        // Assign the biggest rotations after scales.
        box.setXExtent(Math.abs(_compVect1.getX()));
        box.setYExtent(Math.abs(_compVect1.getY()));
        box.setZExtent(Math.abs(_compVect1.getZ()));

        Matrix3.releaseTempInstance(transMatrix);

        return box;
    }

    /**
     * <code>computeFromPoints</code> creates a new Bounding Box from a given set of points. It uses the
     * <code>containAABB</code> method as default.
     * 
     * @param points
     *            the points to contain.
     */
    @Override
    public void computeFromPoints(final FloatBuffer points) {
        containAABB(points);
    }

    @Override
    public void computeFromPrimitives(final MeshData data, final int section, final int[] indices, final int start,
            final int end) {
        if (end - start <= 0) {
            return;
        }

        final Vector3 min = _compVect1
                .set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        final Vector3 max = _compVect2
                .set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

        final int vertsPerPrimitive = data.getIndexMode(section).getVertexCount();
        Vector3[] store = new Vector3[vertsPerPrimitive];

        for (int i = start; i < end; i++) {
            store = data.getPrimitiveVertices(indices[i], section, store);
            for (int j = 0; j < store.length; j++) {
                checkMinMax(min, max, store[j]);
            }
        }

        _center.set(min.addLocal(max));
        _center.multiplyLocal(0.5);

        setXExtent(max.getX() - _center.getX());
        setYExtent(max.getY() - _center.getY());
        setZExtent(max.getZ() - _center.getZ());
    }

    private void checkMinMax(final Vector3 min, final Vector3 max, final ReadOnlyVector3 point) {
        if (point.getX() < min.getX()) {
            min.setX(point.getX());
        }
        if (point.getX() > max.getX()) {
            max.setX(point.getX());
        }

        if (point.getY() < min.getY()) {
            min.setY(point.getY());
        }
        if (point.getY() > max.getY()) {
            max.setY(point.getY());
        }

        if (point.getZ() < min.getZ()) {
            min.setZ(point.getZ());
        }
        if (point.getZ() > max.getZ()) {
            max.setZ(point.getZ());
        }
    }

    /**
     * <code>containAABB</code> creates a minimum-volume axis-aligned bounding box of the points, then selects the
     * smallest enclosing sphere of the box with the sphere centered at the boxes center.
     * 
     * @param points
     *            the list of points.
     */
    public void containAABB(final FloatBuffer points) {
        if (points == null) {
            return;
        }

        points.rewind();
        if (points.remaining() <= 2) {
            return;
        }

        BufferUtils.populateFromBuffer(_compVect1, points, 0);
        double minX = _compVect1.getX(), minY = _compVect1.getY(), minZ = _compVect1.getZ();
        double maxX = _compVect1.getX(), maxY = _compVect1.getY(), maxZ = _compVect1.getZ();

        for (int i = 1, len = points.remaining() / 3; i < len; i++) {
            BufferUtils.populateFromBuffer(_compVect1, points, i);

            if (_compVect1.getX() < minX) {
                minX = _compVect1.getX();
            } else if (_compVect1.getX() > maxX) {
                maxX = _compVect1.getX();
            }

            if (_compVect1.getY() < minY) {
                minY = _compVect1.getY();
            } else if (_compVect1.getY() > maxY) {
                maxY = _compVect1.getY();
            }

            if (_compVect1.getZ() < minZ) {
                minZ = _compVect1.getZ();
            } else if (_compVect1.getZ() > maxZ) {
                maxZ = _compVect1.getZ();
            }
        }

        _center.set(minX + maxX, minY + maxY, minZ + maxZ);
        _center.multiplyLocal(0.5);

        setXExtent(maxX - _center.getX());
        setYExtent(maxY - _center.getY());
        setZExtent(maxZ - _center.getZ());
    }

    /**
     * <code>whichSide</code> takes a plane (typically provided by a view frustum) to determine which side this bound is
     * on.
     * 
     * @param plane
     *            the plane to check against.
     */
    @Override
    public Side whichSide(final ReadOnlyPlane plane) {
        final ReadOnlyVector3 normal = plane.getNormal();
        final double radius = Math.abs(getXExtent() * normal.getX()) + Math.abs(getYExtent() * normal.getY())
                + Math.abs(getZExtent() * normal.getZ());

        final double distance = plane.pseudoDistance(_center);

        if (distance < -radius) {
            return Plane.Side.Inside;
        } else if (distance > radius) {
            return Plane.Side.Outside;
        } else {
            return Plane.Side.Neither;
        }
    }

    /**
     * <code>merge</code> combines this sphere with a second bounding sphere. This new sphere contains both bounding
     * spheres and is returned.
     * 
     * @param volume
     *            the sphere to combine with this sphere.
     * @return the new sphere
     */
    @Override
    public BoundingVolume merge(final BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {
            case AABB: {
                final BoundingBox vBox = (BoundingBox) volume;
                return merge(vBox._center, vBox.getXExtent(), vBox.getYExtent(), vBox.getZExtent(), new BoundingBox(
                        new Vector3(0, 0, 0), 0, 0, 0));
            }

            case Sphere: {
                final BoundingSphere vSphere = (BoundingSphere) volume;
                return merge(vSphere._center, vSphere.getRadius(), vSphere.getRadius(), vSphere.getRadius(),
                        new BoundingBox(new Vector3(0, 0, 0), 0, 0, 0));
            }

            case OBB: {
                final OrientedBoundingBox box = (OrientedBoundingBox) volume;
                final BoundingBox rVal = (BoundingBox) this.clone(null);
                return rVal.mergeOBB(box);
            }

            default:
                return null;
        }
    }

    /**
     * <code>mergeLocal</code> combines this sphere with a second bounding sphere locally. Altering this sphere to
     * contain both the original and the additional sphere volumes;
     * 
     * @param volume
     *            the sphere to combine with this sphere.
     * @return this
     */
    @Override
    public BoundingVolume mergeLocal(final BoundingVolume volume) {
        if (volume == null) {
            return this;
        }

        switch (volume.getType()) {
            case AABB: {
                final BoundingBox vBox = (BoundingBox) volume;
                return merge(vBox._center, vBox.getXExtent(), vBox.getYExtent(), vBox.getZExtent(), this);
            }

            case Sphere: {
                final BoundingSphere vSphere = (BoundingSphere) volume;
                return merge(vSphere._center, vSphere.getRadius(), vSphere.getRadius(), vSphere.getRadius(), this);
            }

            case OBB: {
                return mergeOBB((OrientedBoundingBox) volume);
            }

            default:
                return null;
        }
    }

    @Override
    public BoundingVolume asType(final Type newType) {
        if (newType == null) {
            return null;
        }

        switch (newType) {
            case AABB: {
                return this.clone(null);
            }

            case Sphere: {
                final BoundingSphere sphere = new BoundingSphere(0, _center);
                return sphere.merge(this);
            }

            case OBB: {
                final OrientedBoundingBox obb = new OrientedBoundingBox();
                obb.setCenter(_center);
                return obb.merge(this);
            }

            default:
                return null;
        }
    }

    /**
     * Merges this AABB with the given OBB.
     * 
     * @param volume
     *            the OBB to merge this AABB with.
     * @return This AABB extended to fit the given OBB.
     */
    private BoundingBox mergeOBB(final OrientedBoundingBox volume) {
        // check for infinite bounds to prevent NaN values
        if (Double.isInfinite(getXExtent()) || Double.isInfinite(getYExtent()) || Double.isInfinite(getZExtent())
                || Vector3.isInfinite(volume.getExtent())) {
            setCenter(Vector3.ZERO);
            setXExtent(Double.POSITIVE_INFINITY);
            setYExtent(Double.POSITIVE_INFINITY);
            setZExtent(Double.POSITIVE_INFINITY);
            return this;
        }

        if (!volume.correctCorners) {
            volume.computeCorners();
        }

        double minX, minY, minZ;
        double maxX, maxY, maxZ;

        minX = _center.getX() - getXExtent();
        minY = _center.getY() - getYExtent();
        minZ = _center.getZ() - getZExtent();

        maxX = _center.getX() + getXExtent();
        maxY = _center.getY() + getYExtent();
        maxZ = _center.getZ() + getZExtent();

        for (int i = 1; i < volume._vectorStore.length; i++) {
            final Vector3 temp = volume._vectorStore[i];
            if (temp.getX() < minX) {
                minX = temp.getX();
            } else if (temp.getX() > maxX) {
                maxX = temp.getX();
            }

            if (temp.getY() < minY) {
                minY = temp.getY();
            } else if (temp.getY() > maxY) {
                maxY = temp.getY();
            }

            if (temp.getZ() < minZ) {
                minZ = temp.getZ();
            } else if (temp.getZ() > maxZ) {
                maxZ = temp.getZ();
            }
        }

        _center.set(minX + maxX, minY + maxY, minZ + maxZ);
        _center.multiplyLocal(0.5);

        setXExtent(maxX - _center.getX());
        setYExtent(maxY - _center.getY());
        setZExtent(maxZ - _center.getZ());
        return this;
    }

    /**
     * <code>merge</code> combines this bounding box with another box which is defined by the center, x, y, z extents.
     * 
     * @param boxCenter
     *            the center of the box to merge with
     * @param boxX
     *            the x extent of the box to merge with.
     * @param boxY
     *            the y extent of the box to merge with.
     * @param boxZ
     *            the z extent of the box to merge with.
     * @param store
     *            the box to store our results in.
     * @return the resulting merged box.
     */
    private BoundingBox merge(final Vector3 boxCenter, final double boxX, final double boxY, final double boxZ,
            final BoundingBox store) {
        // check for infinite bounds to prevent NaN values
        if (Double.isInfinite(getXExtent()) || Double.isInfinite(getYExtent()) || Double.isInfinite(getZExtent())
                || Double.isInfinite(boxX) || Double.isInfinite(boxY) || Double.isInfinite(boxZ)) {
            store.setCenter(Vector3.ZERO);
            store.setXExtent(Double.POSITIVE_INFINITY);
            store.setYExtent(Double.POSITIVE_INFINITY);
            store.setZExtent(Double.POSITIVE_INFINITY);
            return store;
        }

        _compVect1.setX(_center.getX() - getXExtent());
        if (_compVect1.getX() > boxCenter.getX() - boxX) {
            _compVect1.setX(boxCenter.getX() - boxX);
        }
        _compVect1.setY(_center.getY() - getYExtent());
        if (_compVect1.getY() > boxCenter.getY() - boxY) {
            _compVect1.setY(boxCenter.getY() - boxY);
        }
        _compVect1.setZ(_center.getZ() - getZExtent());
        if (_compVect1.getZ() > boxCenter.getZ() - boxZ) {
            _compVect1.setZ(boxCenter.getZ() - boxZ);
        }

        _compVect2.setX(_center.getX() + getXExtent());
        if (_compVect2.getX() < boxCenter.getX() + boxX) {
            _compVect2.setX(boxCenter.getX() + boxX);
        }
        _compVect2.setY(_center.getY() + getYExtent());
        if (_compVect2.getY() < boxCenter.getY() + boxY) {
            _compVect2.setY(boxCenter.getY() + boxY);
        }
        _compVect2.setZ(_center.getZ() + getZExtent());
        if (_compVect2.getZ() < boxCenter.getZ() + boxZ) {
            _compVect2.setZ(boxCenter.getZ() + boxZ);
        }

        store._center.set(_compVect2).addLocal(_compVect1).multiplyLocal(0.5);

        store.setXExtent(_compVect2.getX() - store._center.getX());
        store.setYExtent(_compVect2.getY() - store._center.getY());
        store.setZExtent(_compVect2.getZ() - store._center.getZ());

        return store;
    }

    /**
     * <code>clone</code> creates a new BoundingBox object containing the same data as this one.
     * 
     * @param store
     *            where to store the cloned information. if null or wrong class, a new store is created.
     * @return the new BoundingBox
     */
    @Override
    public BoundingVolume clone(final BoundingVolume store) {
        if (store != null && store.getType() == Type.AABB) {
            final BoundingBox rVal = (BoundingBox) store;
            rVal._center.set(_center);
            rVal.setXExtent(_xExtent);
            rVal.setYExtent(_yExtent);
            rVal.setZExtent(_zExtent);
            rVal._checkPlane = _checkPlane;
            return rVal;
        }

        final BoundingBox rVal = new BoundingBox(_center, getXExtent(), getYExtent(), getZExtent());
        return rVal;
    }

    /**
     * <code>toString</code> returns the string representation of this object. The form is:
     * "Radius: RRR.SSSS Center: <Vector>".
     * 
     * @return the string representation of this.
     */
    @Override
    public String toString() {
        return "com.ardor3d.scene.BoundingBox [Center: " + _center + "  xExtent: " + getXExtent() + "  yExtent: "
                + getYExtent() + "  zExtent: " + getZExtent() + "]";
    }

    @Override
    public boolean intersects(final BoundingVolume bv) {
        if (bv == null) {
            return false;
        }

        return bv.intersectsBoundingBox(this);
    }

    @Override
    public boolean intersectsSphere(final BoundingSphere bs) {
        if (!Vector3.isValid(_center) || !Vector3.isValid(bs._center)) {
            return false;
        }

        if (Math.abs(_center.getX() - bs.getCenter().getX()) < bs.getRadius() + getXExtent()
                && Math.abs(_center.getY() - bs.getCenter().getY()) < bs.getRadius() + getYExtent()
                && Math.abs(_center.getZ() - bs.getCenter().getZ()) < bs.getRadius() + getZExtent()) {
            return true;
        }

        return false;
    }

    @Override
    public boolean intersectsBoundingBox(final BoundingBox bb) {
        if (!Vector3.isValid(_center) || !Vector3.isValid(bb._center)) {
            return false;
        }

        if (_center.getX() + getXExtent() < bb._center.getX() - bb.getXExtent()
                || _center.getX() - getXExtent() > bb._center.getX() + bb.getXExtent()) {
            return false;
        } else if (_center.getY() + getYExtent() < bb._center.getY() - bb.getYExtent()
                || _center.getY() - getYExtent() > bb._center.getY() + bb.getYExtent()) {
            return false;
        } else if (_center.getZ() + getZExtent() < bb._center.getZ() - bb.getZExtent()
                || _center.getZ() - getZExtent() > bb._center.getZ() + bb.getZExtent()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean intersectsOrientedBoundingBox(final OrientedBoundingBox obb) {
        return obb.intersectsBoundingBox(this);
    }

    @Override
    public boolean intersects(final ReadOnlyRay3 ray) {
        if (!Vector3.isValid(_center)) {
            return false;
        }

        final Vector3 diff = ray.getOrigin().subtract(_center, _compVect1);

        final ReadOnlyVector3 direction = ray.getDirection();

        final double[] t = { 0.0, Double.POSITIVE_INFINITY };

        // Check for degenerate cases and pad using zero tolerance. Should give close enough result.
        double x = getXExtent();
        if (x < MathUtils.ZERO_TOLERANCE && x >= 0) {
            x = MathUtils.ZERO_TOLERANCE;
        }
        double y = getYExtent();
        if (y < MathUtils.ZERO_TOLERANCE && y >= 0) {
            y = MathUtils.ZERO_TOLERANCE;
        }
        double z = getZExtent();
        if (z < MathUtils.ZERO_TOLERANCE && z >= 0) {
            z = MathUtils.ZERO_TOLERANCE;
        }

        // Special case.
        if (Double.isInfinite(x) && Double.isInfinite(y) && Double.isInfinite(z)) {
            return true;
        }

        final boolean notEntirelyClipped = clip(direction.getX(), -diff.getX() - x, t)
                && clip(-direction.getX(), diff.getX() - x, t) && clip(direction.getY(), -diff.getY() - y, t)
                && clip(-direction.getY(), diff.getY() - y, t) && clip(direction.getZ(), -diff.getZ() - z, t)
                && clip(-direction.getZ(), diff.getZ() - z, t);

        return (notEntirelyClipped && (t[0] != 0.0 || t[1] != Double.POSITIVE_INFINITY));
    }

    @Override
    public IntersectionRecord intersectsWhere(final ReadOnlyRay3 ray) {
        if (!Vector3.isValid(_center)) {
            return null;
        }

        final Vector3 diff = ray.getOrigin().subtract(_center, _compVect1);

        final ReadOnlyVector3 direction = ray.getDirection();

        final double[] t = { 0.0, Double.POSITIVE_INFINITY };

        // Check for degenerate cases and pad using zero tolerance. Should give close enough result.
        double x = getXExtent();
        if (x < MathUtils.ZERO_TOLERANCE && x >= 0) {
            x = MathUtils.ZERO_TOLERANCE;
        }
        double y = getYExtent();
        if (y < MathUtils.ZERO_TOLERANCE && y >= 0) {
            y = MathUtils.ZERO_TOLERANCE;
        }
        double z = getZExtent();
        if (z < MathUtils.ZERO_TOLERANCE && z >= 0) {
            z = MathUtils.ZERO_TOLERANCE;
        }

        final boolean notEntirelyClipped = clip(direction.getX(), -diff.getX() - x, t)
                && clip(-direction.getX(), diff.getX() - x, t) && clip(direction.getY(), -diff.getY() - y, t)
                && clip(-direction.getY(), diff.getY() - y, t) && clip(direction.getZ(), -diff.getZ() - z, t)
                && clip(-direction.getZ(), diff.getZ() - z, t);

        if (notEntirelyClipped && (t[0] != 0.0 || t[1] != Double.POSITIVE_INFINITY)) {
            if (t[1] > t[0]) {
                final double[] distances = t;
                final Vector3[] points = new Vector3[] {
                        new Vector3(ray.getDirection()).multiplyLocal(distances[0]).addLocal(ray.getOrigin()),
                        new Vector3(ray.getDirection()).multiplyLocal(distances[1]).addLocal(ray.getOrigin()) };
                return new IntersectionRecord(distances, points);
            }

            final double[] distances = new double[] { t[0] };
            final Vector3[] points = new Vector3[] { new Vector3(ray.getDirection()).multiplyLocal(distances[0])
                    .addLocal(ray.getOrigin()), };
            return new IntersectionRecord(distances, points);
        }

        return null;

    }

    @Override
    public boolean contains(final ReadOnlyVector3 point) {
        return Math.abs(_center.getX() - point.getX()) < getXExtent()
                && Math.abs(_center.getY() - point.getY()) < getYExtent()
                && Math.abs(_center.getZ() - point.getZ()) < getZExtent();
    }

    @Override
    public double distanceToEdge(final ReadOnlyVector3 point) {
        // compute coordinates of point in box coordinate system
        final Vector3 closest = point.subtract(_center, _compVect1);

        // project test point onto box
        double sqrDistance = 0.0;
        double delta;

        if (closest.getX() < -getXExtent()) {
            delta = closest.getX() + getXExtent();
            sqrDistance += delta * delta;
            closest.setX(-getXExtent());
        } else if (closest.getX() > getXExtent()) {
            delta = closest.getX() - getXExtent();
            sqrDistance += delta * delta;
            closest.setX(getXExtent());
        }

        if (closest.getY() < -getYExtent()) {
            delta = closest.getY() + getYExtent();
            sqrDistance += delta * delta;
            closest.setY(-getYExtent());
        } else if (closest.getY() > getYExtent()) {
            delta = closest.getY() - getYExtent();
            sqrDistance += delta * delta;
            closest.setY(getYExtent());
        }

        if (closest.getZ() < -getZExtent()) {
            delta = closest.getZ() + getZExtent();
            sqrDistance += delta * delta;
            closest.setZ(-getZExtent());
        } else if (closest.getZ() > getZExtent()) {
            delta = closest.getZ() - getZExtent();
            sqrDistance += delta * delta;
            closest.setZ(getZExtent());
        }

        return Math.sqrt(sqrDistance);
    }

    /**
     * Get our corners using the bounding center and extents.
     * 
     * @param store
     *            An optional store. Must be at least length of 8. If null, one will be created for you.
     * @return array filled with our corners.
     * @throws ArrayIndexOutOfBoundsException
     *             if our store is length < 8.
     */
    public Vector3[] getCorners(Vector3[] store) {
        if (store == null) {
            store = new Vector3[8];
            for (int i = 0; i < store.length; i++) {
                store[i] = new Vector3();
            }
        }
        store[0].set(_center.getX() + _xExtent, _center.getY() + _yExtent, _center.getZ() + _zExtent);
        store[1].set(_center.getX() + _xExtent, _center.getY() + _yExtent, _center.getZ() - _zExtent);
        store[2].set(_center.getX() + _xExtent, _center.getY() - _yExtent, _center.getZ() + _zExtent);
        store[3].set(_center.getX() + _xExtent, _center.getY() - _yExtent, _center.getZ() - _zExtent);
        store[4].set(_center.getX() - _xExtent, _center.getY() + _yExtent, _center.getZ() + _zExtent);
        store[5].set(_center.getX() - _xExtent, _center.getY() + _yExtent, _center.getZ() - _zExtent);
        store[6].set(_center.getX() - _xExtent, _center.getY() - _yExtent, _center.getZ() + _zExtent);
        store[7].set(_center.getX() - _xExtent, _center.getY() - _yExtent, _center.getZ() - _zExtent);
        return store;
    }

    /**
     * <code>clip</code> determines if a line segment intersects the current test plane.
     * 
     * @param denom
     *            the denominator of the line segment.
     * @param numer
     *            the numerator of the line segment.
     * @param t
     *            test values of the plane.
     * @return true if the line segment intersects the plane, false otherwise.
     */
    private boolean clip(final double denom, final double numer, final double[] t) {
        // Return value is 'true' if line segment intersects the current test
        // plane. Otherwise 'false' is returned in which case the line segment
        // is entirely clipped.
        if (denom > 0.0) {
            if (numer > denom * t[1]) {
                return false;
            }
            if (numer > denom * t[0]) {
                t[0] = numer / denom;
            }
            return true;
        } else if (denom < 0.0) {
            if (numer > denom * t[0]) {
                return false;
            }
            if (numer > denom * t[1]) {
                t[1] = numer / denom;
            }
            return true;
        } else {
            return numer <= 0.0;
        }
    }

    /**
     * Query extent.
     * 
     * @param store
     *            where extent gets stored - null to return a new vector
     * @return store / new vector
     */
    public Vector3 getExtent(Vector3 store) {
        if (store == null) {
            store = new Vector3();
        }
        store.set(getXExtent(), getYExtent(), getZExtent());
        return store;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(getXExtent(), "xExtent", 0);
        capsule.write(getYExtent(), "yExtent", 0);
        capsule.write(getZExtent(), "zExtent", 0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        setXExtent(capsule.readDouble("xExtent", 0));
        setYExtent(capsule.readDouble("yExtent", 0));
        setZExtent(capsule.readDouble("zExtent", 0));
    }

    @Override
    public double getVolume() {
        return (8 * getXExtent() * getYExtent() * getZExtent());
    }
}
