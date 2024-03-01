/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.bounding;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.intersection.IntersectionRecord;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyPlane;
import com.ardor3d.math.type.ReadOnlyPlane.Side;
import com.ardor3d.math.type.ReadOnlyRay3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

public class OrientedBoundingBox extends BoundingVolume {

  private static final long serialVersionUID = 1L;

  /** X axis of the Oriented Box. */
  protected final Vector3 _xAxis = new Vector3(1, 0, 0);

  /** Y axis of the Oriented Box. */
  protected final Vector3 _yAxis = new Vector3(0, 1, 0);

  /** Z axis of the Oriented Box. */
  protected final Vector3 _zAxis = new Vector3(0, 0, 1);

  /** Extents of the box along the x,y,z axis. */
  protected final Vector3 _extent = new Vector3(0, 0, 0);

  /** Vector array used to store the array of 8 corners the box has. */
  protected final Vector3[] _vectorStore = new Vector3[8];

  /**
   * If true, the box's vectorStore array correctly represents the box's corners.
   */
  public boolean correctCorners = false;

  protected final Vector3 _compVect3 = new Vector3();

  public OrientedBoundingBox() {
    for (int x = 0; x < 8; x++) {
      _vectorStore[x] = new Vector3();
    }
  }

  @Override
  public Type getType() { return Type.OBB; }

  @Override
  // XXX: HACK, revisit.
  public BoundingVolume transform(final ReadOnlyTransform transform, BoundingVolume store) {
    if (store == null || store.getType() != Type.OBB) {
      store = new OrientedBoundingBox();
    }
    final OrientedBoundingBox toReturn = (OrientedBoundingBox) store;
    final Vector3 helper = new Vector3();
    helper.set(1, 0, 0);
    final double scaleX = transform.applyForwardVector(helper).length();
    helper.set(0, 1, 0);
    final double scaleY = transform.applyForwardVector(helper).length();
    helper.set(0, 0, 1);
    final double scaleZ = transform.applyForwardVector(helper).length();
    toReturn._extent.set(Math.abs(_extent.getX() * scaleX), Math.abs(_extent.getY() * scaleY),
        Math.abs(_extent.getZ() * scaleZ));

    transform.getMatrix().applyPost(_xAxis, toReturn._xAxis);
    transform.getMatrix().applyPost(_yAxis, toReturn._yAxis);
    transform.getMatrix().applyPost(_zAxis, toReturn._zAxis);
    if (!transform.isRotationMatrix()) {
      toReturn._xAxis.normalizeLocal();
      toReturn._yAxis.normalizeLocal();
      toReturn._zAxis.normalizeLocal();
    }

    transform.applyForward(_center, toReturn._center);
    toReturn.correctCorners = false;
    toReturn.computeCorners();
    return toReturn;
  }

  @Override
  public Side whichSide(final ReadOnlyPlane plane) {
    final ReadOnlyVector3 planeNormal = plane.getNormal();
    final double fRadius = Math.abs(_extent.getX() * (planeNormal.dot(_xAxis)))
        + Math.abs(_extent.getY() * (planeNormal.dot(_yAxis))) + Math.abs(_extent.getZ() * (planeNormal.dot(_zAxis)));
    final double fDistance = plane.pseudoDistance(_center);
    if (fDistance <= -fRadius) {
      return Plane.Side.Inside;
    } else if (fDistance >= fRadius) {
      return Plane.Side.Outside;
    } else {
      return Plane.Side.Neither;
    }
  }

  @Override
  public void computeFromPoints(final FloatBuffer points) {
    containAABB(points);
  }

  /**
   * Calculates an AABB of the given point values for this OBB.
   *
   * @param points
   *          The points this OBB should contain.
   */
  private void containAABB(final FloatBuffer points) {
    if (points == null || points.limit() <= 2) { // we need at least a 3
      // double vector
      return;
    }

    BufferUtils.populateFromBuffer(_compVect1, points, 0);
    double minX = _compVect1.getX(), minY = _compVect1.getY(), minZ = _compVect1.getZ();
    double maxX = _compVect1.getX(), maxY = _compVect1.getY(), maxZ = _compVect1.getZ();

    for (int i = 1, len = points.limit() / 3; i < len; i++) {
      BufferUtils.populateFromBuffer(_compVect1, points, i);

      minX = Math.min(_compVect1.getX(), minX);
      maxX = Math.max(_compVect1.getX(), maxX);

      minY = Math.min(_compVect1.getY(), minY);
      maxY = Math.max(_compVect1.getY(), maxY);

      minZ = Math.min(_compVect1.getZ(), minZ);
      maxZ = Math.max(_compVect1.getZ(), maxZ);
    }

    _center.set(minX + maxX, minY + maxY, minZ + maxZ);
    _center.multiplyLocal(0.5);

    _extent.set(maxX - _center.getX(), maxY - _center.getY(), maxZ - _center.getZ());

    _xAxis.set(1, 0, 0);
    _yAxis.set(0, 1, 0);
    _zAxis.set(0, 0, 1);

    correctCorners = false;
  }

  @Override
  public BoundingVolume merge(final BoundingVolume volume) {
    // clone ourselves into a new bounding volume, then merge.
    return clone(new OrientedBoundingBox()).mergeLocal(volume);
  }

  @Override
  public BoundingVolume mergeLocal(final BoundingVolume volume) {
    if (volume == null) {
      return this;
    }

    switch (volume.getType()) {

      case OBB: {
        return mergeOBB((OrientedBoundingBox) volume);
      }

      case AABB: {
        return mergeAABB((BoundingBox) volume);
      }

      case Sphere: {
        return mergeSphere((BoundingSphere) volume);
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
        final BoundingBox box = new BoundingBox(_center, 0, 0, 0);
        return box.merge(this);
      }

      case Sphere: {
        final BoundingSphere sphere = new BoundingSphere(0, _center);
        return sphere.merge(this);
      }

      case OBB: {
        return this.clone(null);
      }

      default:
        return null;
    }
  }

  private BoundingVolume mergeSphere(final BoundingSphere volume) {
    // check for infinite bounds to prevent NaN values
    if (Vector3.isInfinite(getExtent()) || Double.isInfinite(volume.getRadius())) {
      setCenter(Vector3.ZERO);
      _extent.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      return this;
    }

    final BoundingSphere mergeSphere = volume;
    if (!correctCorners) {
      computeCorners();
    }

    final FloatBuffer mergeBuf = BufferUtils.createFloatBufferOnHeap(16 * 3);

    mergeBuf.rewind();
    for (int i = 0; i < 8; i++) {
      mergeBuf.put((float) _vectorStore[i].getX());
      mergeBuf.put((float) _vectorStore[i].getY());
      mergeBuf.put((float) _vectorStore[i].getZ());
    }
    mergeBuf.put((float) (mergeSphere._center.getX() + mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getY() + mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getZ() + mergeSphere.getRadius()));
    mergeBuf.put((float) (mergeSphere._center.getX() - mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getY() + mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getZ() + mergeSphere.getRadius()));
    mergeBuf.put((float) (mergeSphere._center.getX() + mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getY() - mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getZ() + mergeSphere.getRadius()));
    mergeBuf.put((float) (mergeSphere._center.getX() + mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getY() + mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getZ() - mergeSphere.getRadius()));
    mergeBuf.put((float) (mergeSphere._center.getX() - mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getY() - mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getZ() + mergeSphere.getRadius()));
    mergeBuf.put((float) (mergeSphere._center.getX() - mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getY() + mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getZ() - mergeSphere.getRadius()));
    mergeBuf.put((float) (mergeSphere._center.getX() + mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getY() - mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getZ() - mergeSphere.getRadius()));
    mergeBuf.put((float) (mergeSphere._center.getX() - mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getY() - mergeSphere.getRadius()))
        .put((float) (mergeSphere._center.getZ() - mergeSphere.getRadius()));
    containAABB(mergeBuf);
    correctCorners = false;
    return this;
  }

  private BoundingVolume mergeAABB(final BoundingBox volume) {
    // check for infinite bounds to prevent NaN values
    if (Vector3.isInfinite(getExtent()) || Double.isInfinite(volume.getXExtent())
        || Double.isInfinite(volume.getYExtent()) || Double.isInfinite(volume.getZExtent())) {
      setCenter(Vector3.ZERO);
      _extent.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      return this;
    }

    final BoundingBox mergeBox = volume;
    if (!correctCorners) {
      computeCorners();
    }

    final FloatBuffer mergeBuf = BufferUtils.createFloatBufferOnHeap(16 * 3);

    mergeBuf.rewind();
    for (int i = 0; i < 8; i++) {
      mergeBuf.put((float) _vectorStore[i].getX());
      mergeBuf.put((float) _vectorStore[i].getY());
      mergeBuf.put((float) _vectorStore[i].getZ());
    }
    mergeBuf.put((float) (mergeBox._center.getX() + mergeBox.getXExtent()))
        .put((float) (mergeBox._center.getY() + mergeBox.getYExtent()))
        .put((float) (mergeBox._center.getZ() + mergeBox.getZExtent()));
    mergeBuf.put((float) (mergeBox._center.getX() - mergeBox.getXExtent()))
        .put((float) (mergeBox._center.getY() + mergeBox.getYExtent()))
        .put((float) (mergeBox._center.getZ() + mergeBox.getZExtent()));
    mergeBuf.put((float) (mergeBox._center.getX() + mergeBox.getXExtent()))
        .put((float) (mergeBox._center.getY() - mergeBox.getYExtent()))
        .put((float) (mergeBox._center.getZ() + mergeBox.getZExtent()));
    mergeBuf.put((float) (mergeBox._center.getX() + mergeBox.getXExtent()))
        .put((float) (mergeBox._center.getY() + mergeBox.getYExtent()))
        .put((float) (mergeBox._center.getZ() - mergeBox.getZExtent()));
    mergeBuf.put((float) (mergeBox._center.getX() - mergeBox.getXExtent()))
        .put((float) (mergeBox._center.getY() - mergeBox.getYExtent()))
        .put((float) (mergeBox._center.getZ() + mergeBox.getZExtent()));
    mergeBuf.put((float) (mergeBox._center.getX() - mergeBox.getXExtent()))
        .put((float) (mergeBox._center.getY() + mergeBox.getYExtent()))
        .put((float) (mergeBox._center.getZ() - mergeBox.getZExtent()));
    mergeBuf.put((float) (mergeBox._center.getX() + mergeBox.getXExtent()))
        .put((float) (mergeBox._center.getY() - mergeBox.getYExtent()))
        .put((float) (mergeBox._center.getZ() - mergeBox.getZExtent()));
    mergeBuf.put((float) (mergeBox._center.getX() - mergeBox.getXExtent()))
        .put((float) (mergeBox._center.getY() - mergeBox.getYExtent()))
        .put((float) (mergeBox._center.getZ() - mergeBox.getZExtent()));
    containAABB(mergeBuf);
    correctCorners = false;
    return this;
  }

  private BoundingVolume mergeOBB(final OrientedBoundingBox volume) {
    // check for infinite bounds to prevent NaN values
    if (Vector3.isInfinite(getExtent()) || Vector3.isInfinite(volume.getExtent())) {
      setCenter(Vector3.ZERO);
      _extent.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
      return this;
    }

    // OrientedBoundingBox mergeBox=(OrientedBoundingBox) volume;
    // if (!correctCorners) this.computeCorners();
    // if (!mergeBox.correctCorners) mergeBox.computeCorners();
    // Vector3[] mergeArray=new Vector3[16];
    // for (int i=0;i<vectorStore.length;i++){
    // mergeArray[i*2+0]=this .vectorStore[i];
    // mergeArray[i*2+1]=mergeBox.vectorStore[i];
    // }
    // containAABB(mergeArray);
    // correctCorners=false;
    // return this;
    // construct a box that contains the input boxes
    // Box3<Real> kBox;
    final OrientedBoundingBox rkBox0 = this;
    final OrientedBoundingBox rkBox1 = volume;

    // The first guess at the box center. This value will be updated later
    // after the input box vertices are projected onto axes determined by an
    // average of box axes.
    final Vector3 kBoxCenter = (rkBox0._center.add(rkBox1._center, Vector3.fetchTempInstance())).multiplyLocal(.5);

    // A box's axes, when viewed as the columns of a matrix, form a rotation
    // matrix. The input box axes are converted to quaternions. The average
    // quaternion is computed, then normalized to unit length. The result is
    // the slerp of the two input quaternions with t-value of 1/2. The
    // result is converted back to a rotation matrix and its columns are
    // selected as the merged box axes.
    final Quaternion kQ0 = Quaternion.fetchTempInstance(), kQ1 = Quaternion.fetchTempInstance();
    kQ0.fromAxes(rkBox0._xAxis, rkBox0._yAxis, rkBox0._zAxis);
    kQ1.fromAxes(rkBox1._xAxis, rkBox1._yAxis, rkBox1._zAxis);

    if (kQ0.dot(kQ1) < 0.0) {
      kQ1.multiplyLocal(-1.0);
    }

    final Quaternion kQ = kQ0.addLocal(kQ1);
    kQ.normalizeLocal();

    final Matrix3 kBoxaxis = kQ.toRotationMatrix(Matrix3.fetchTempInstance());
    final Vector3 newXaxis = kBoxaxis.getColumn(0, Vector3.fetchTempInstance());
    final Vector3 newYaxis = kBoxaxis.getColumn(1, Vector3.fetchTempInstance());
    final Vector3 newZaxis = kBoxaxis.getColumn(2, Vector3.fetchTempInstance());

    // Project the input box vertices onto the merged-box axes. Each axis
    // D[i] containing the current center C has a minimum projected value
    // pmin[i] and a maximum projected value pmax[i]. The corresponding end
    // points on the axes are C+pmin[i]*D[i] and C+pmax[i]*D[i]. The point C
    // is not necessarily the midpoint for any of the intervals. The actual
    // box center will be adjusted from C to a point C' that is the midpoint
    // of each interval,
    // C' = C + sum_{i=0}^1 0.5*(pmin[i]+pmax[i])*D[i]
    // The box extents are
    // e[i] = 0.5*(pmax[i]-pmin[i])

    int i;
    double fDot;
    final Vector3 kDiff = Vector3.fetchTempInstance();
    final Vector3 kMin = Vector3.fetchTempInstance();
    final Vector3 kMax = Vector3.fetchTempInstance();

    if (!rkBox0.correctCorners) {
      rkBox0.computeCorners();
    }
    for (i = 0; i < 8; i++) {
      rkBox0._vectorStore[i].subtract(kBoxCenter, kDiff);

      fDot = kDiff.dot(newXaxis);
      if (fDot > kMax.getX()) {
        kMax.setX(fDot);
      } else if (fDot < kMin.getX()) {
        kMin.setX(fDot);
      }

      fDot = kDiff.dot(newYaxis);
      if (fDot > kMax.getY()) {
        kMax.setY(fDot);
      } else if (fDot < kMin.getY()) {
        kMin.setY(fDot);
      }

      fDot = kDiff.dot(newZaxis);
      if (fDot > kMax.getZ()) {
        kMax.setZ(fDot);
      } else if (fDot < kMin.getZ()) {
        kMin.setZ(fDot);
      }

    }

    if (!rkBox1.correctCorners) {
      rkBox1.computeCorners();
    }
    for (i = 0; i < 8; i++) {
      rkBox1._vectorStore[i].subtract(kBoxCenter, kDiff);

      fDot = kDiff.dot(newXaxis);
      if (fDot > kMax.getX()) {
        kMax.setX(fDot);
      } else if (fDot < kMin.getX()) {
        kMin.setX(fDot);
      }

      fDot = kDiff.dot(newYaxis);
      if (fDot > kMax.getY()) {
        kMax.setY(fDot);
      } else if (fDot < kMin.getY()) {
        kMin.setY(fDot);
      }

      fDot = kDiff.dot(newZaxis);
      if (fDot > kMax.getZ()) {
        kMax.setZ(fDot);
      } else if (fDot < kMin.getZ()) {
        kMin.setZ(fDot);
      }
    }

    _xAxis.set(newXaxis);
    _yAxis.set(newYaxis);
    _zAxis.set(newZaxis);

    final Vector3 tempVec = Vector3.fetchTempInstance();
    _extent.setX(.5 * (kMax.getX() - kMin.getX()));
    kBoxCenter.addLocal(_xAxis.multiply(.5 * (kMax.getX() + kMin.getX()), tempVec));

    _extent.setY(.5 * (kMax.getY() - kMin.getY()));
    kBoxCenter.addLocal(_yAxis.multiply(.5 * (kMax.getY() + kMin.getY()), tempVec));

    _extent.setZ(.5 * (kMax.getZ() - kMin.getZ()));
    kBoxCenter.addLocal(_zAxis.multiply(.5 * (kMax.getZ() + kMin.getZ()), tempVec));

    _center.set(kBoxCenter);

    correctCorners = false;

    Quaternion.releaseTempInstance(kQ0);
    Quaternion.releaseTempInstance(kQ1);
    Matrix3.releaseTempInstance(kBoxaxis);
    Vector3.releaseTempInstance(kBoxCenter);
    Vector3.releaseTempInstance(newXaxis);
    Vector3.releaseTempInstance(newYaxis);
    Vector3.releaseTempInstance(newZaxis);
    Vector3.releaseTempInstance(kDiff);
    Vector3.releaseTempInstance(kMin);
    Vector3.releaseTempInstance(kMax);
    Vector3.releaseTempInstance(tempVec);

    return this;
  }

  @Override
  public BoundingVolume clone(final BoundingVolume store) {
    OrientedBoundingBox toReturn;
    if (store instanceof OrientedBoundingBox) {
      toReturn = (OrientedBoundingBox) store;
    } else {
      toReturn = new OrientedBoundingBox();
    }
    toReturn._extent.set(_extent);
    toReturn._xAxis.set(_xAxis);
    toReturn._yAxis.set(_yAxis);
    toReturn._zAxis.set(_zAxis);
    toReturn._center.set(_center);
    toReturn._checkPlane = _checkPlane;
    for (int x = _vectorStore.length; --x >= 0;) {
      toReturn._vectorStore[x].set(_vectorStore[x]);
    }
    toReturn.correctCorners = correctCorners;
    return toReturn;
  }

  @Override
  public double getRadius() {
    double radius = 0.0;
    radius = Math.max(radius, _xAxis.multiply(_extent.getX(), _compVect1).length());
    radius = Math.max(radius, _yAxis.multiply(_extent.getY(), _compVect1).length());
    radius = Math.max(radius, _zAxis.multiply(_extent.getZ(), _compVect1).length());

    return radius;
  }

  /**
   * Sets the vectorStore information to the 8 corners of the box.
   */
  public void computeCorners() {
    final Vector3 tempAxis0 = _xAxis.multiply(_extent.getX(), _compVect1);
    final Vector3 tempAxis1 = _yAxis.multiply(_extent.getY(), _compVect2);
    final Vector3 tempAxis2 = _zAxis.multiply(_extent.getZ(), _compVect3);

    _vectorStore[0].set(_center).subtractLocal(tempAxis0).subtractLocal(tempAxis1).subtractLocal(tempAxis2);
    _vectorStore[1].set(_center).addLocal(tempAxis0).subtractLocal(tempAxis1).subtractLocal(tempAxis2);
    _vectorStore[2].set(_center).addLocal(tempAxis0).addLocal(tempAxis1).subtractLocal(tempAxis2);
    _vectorStore[3].set(_center).subtractLocal(tempAxis0).addLocal(tempAxis1).subtractLocal(tempAxis2);
    _vectorStore[4].set(_center).subtractLocal(tempAxis0).subtractLocal(tempAxis1).addLocal(tempAxis2);
    _vectorStore[5].set(_center).addLocal(tempAxis0).subtractLocal(tempAxis1).addLocal(tempAxis2);
    _vectorStore[6].set(_center).addLocal(tempAxis0).addLocal(tempAxis1).addLocal(tempAxis2);
    _vectorStore[7].set(_center).subtractLocal(tempAxis0).addLocal(tempAxis1).addLocal(tempAxis2);

    correctCorners = true;
  }

  @Override
  public void computeFromPrimitives(final MeshData data, final int section, final int[] indices, final int start,
      final int end) {
    if (end - start <= 0) {
      return;
    }

    final int vertsPerPrimitive = data.getIndexMode(section).getVertexCount();
    Vector3[] store = new Vector3[vertsPerPrimitive];

    final Vector3 min = _compVect1.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    final Vector3 max = _compVect2.set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);

    Vector3 point;
    for (int i = start; i < end; i++) {
      store = data.getPrimitiveVertices(indices[i], section, store);
      for (int j = 0; j < vertsPerPrimitive; j++) {
        point = store[j];
        if (point.getX() < min.getX()) {
          min.setX(point.getX());
        } else if (point.getX() > max.getX()) {
          max.setX(point.getX());
        }
        if (point.getY() < min.getY()) {
          min.setY(point.getY());
        } else if (point.getY() > max.getY()) {
          max.setY(point.getY());
        }
        if (point.getZ() < min.getZ()) {
          min.setZ(point.getZ());
        } else if (point.getZ() > max.getZ()) {
          max.setZ(point.getZ());
        }
      }
    }

    _center.set(min.addLocal(max));
    _center.multiplyLocal(0.5);

    _extent.set(max.getX() - _center.getX(), max.getY() - _center.getY(), max.getZ() - _center.getZ());

    _xAxis.set(1, 0, 0);
    _yAxis.set(0, 1, 0);
    _zAxis.set(0, 0, 1);

    correctCorners = false;
  }

  public boolean intersection(final OrientedBoundingBox box1) {
    // Cutoff for cosine of angles between box axes. This is used to catch the cases when at least one
    // pair of axes
    // are parallel. If this happens, there is no need to test for separation along the Cross(A[i],B[j])
    // directions.
    final OrientedBoundingBox box0 = this;
    final double cutoff = 0.999999;
    boolean parallelPairExists = false;
    int i;

    // convenience variables
    final ReadOnlyVector3[] akA = new ReadOnlyVector3[] {box0.getXAxis(), box0.getYAxis(), box0.getZAxis()};
    final ReadOnlyVector3[] akB = new ReadOnlyVector3[] {box1.getXAxis(), box1.getYAxis(), box1.getZAxis()};
    final ReadOnlyVector3 afEA = box0._extent;
    final ReadOnlyVector3 afEB = box1._extent;

    // compute difference of box centers, D = C1-C0
    final Vector3 kD = box1._center.subtract(box0._center, _compVect1);

    final double[][] aafC = {new double[3], new double[3], new double[3]};

    final double[][] aafAbsC = {new double[3], new double[3], new double[3]};

    final double[] afAD = new double[3];
    double fR0, fR1, fR; // interval radii and distance between centers
    double fR01; // = R0 + R1

    // axis C0+t*A0
    for (i = 0; i < 3; i++) {
      aafC[0][i] = akA[0].dot(akB[i]);
      aafAbsC[0][i] = Math.abs(aafC[0][i]);
      if (aafAbsC[0][i] > cutoff) {
        parallelPairExists = true;
      }
    }
    afAD[0] = akA[0].dot(kD);
    fR = Math.abs(afAD[0]);
    fR1 = afEB.getX() * aafAbsC[0][0] + afEB.getY() * aafAbsC[0][1] + afEB.getZ() * aafAbsC[0][2];
    fR01 = afEA.getX() + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A1
    for (i = 0; i < 3; i++) {
      aafC[1][i] = akA[1].dot(akB[i]);
      aafAbsC[1][i] = Math.abs(aafC[1][i]);
      if (aafAbsC[1][i] > cutoff) {
        parallelPairExists = true;
      }
    }
    afAD[1] = akA[1].dot(kD);
    fR = Math.abs(afAD[1]);
    fR1 = afEB.getX() * aafAbsC[1][0] + afEB.getY() * aafAbsC[1][1] + afEB.getZ() * aafAbsC[1][2];
    fR01 = afEA.getY() + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A2
    for (i = 0; i < 3; i++) {
      aafC[2][i] = akA[2].dot(akB[i]);
      aafAbsC[2][i] = Math.abs(aafC[2][i]);
      if (aafAbsC[2][i] > cutoff) {
        parallelPairExists = true;
      }
    }
    afAD[2] = akA[2].dot(kD);
    fR = Math.abs(afAD[2]);
    fR1 = afEB.getX() * aafAbsC[2][0] + afEB.getY() * aafAbsC[2][1] + afEB.getZ() * aafAbsC[2][2];
    fR01 = afEA.getZ() + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*B0
    fR = Math.abs(akB[0].dot(kD));
    fR0 = afEA.getX() * aafAbsC[0][0] + afEA.getY() * aafAbsC[1][0] + afEA.getZ() * aafAbsC[2][0];
    fR01 = fR0 + afEB.getX();
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*B1
    fR = Math.abs(akB[1].dot(kD));
    fR0 = afEA.getX() * aafAbsC[0][1] + afEA.getY() * aafAbsC[1][1] + afEA.getZ() * aafAbsC[2][1];
    fR01 = fR0 + afEB.getY();
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*B2
    fR = Math.abs(akB[2].dot(kD));
    fR0 = afEA.getX() * aafAbsC[0][2] + afEA.getY() * aafAbsC[1][2] + afEA.getZ() * aafAbsC[2][2];
    fR01 = fR0 + afEB.getZ();
    if (fR > fR01) {
      return false;
    }

    // At least one pair of box axes was parallel, so the separation is
    // effectively in 2D where checking the "edge" normals is sufficient for
    // the separation of the boxes.
    if (parallelPairExists) {
      return true;
    }

    // axis C0+t*A0xB0
    fR = Math.abs(afAD[2] * aafC[1][0] - afAD[1] * aafC[2][0]);
    fR0 = afEA.getY() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[1][0];
    fR1 = afEB.getY() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][1];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A0xB1
    fR = Math.abs(afAD[2] * aafC[1][1] - afAD[1] * aafC[2][1]);
    fR0 = afEA.getY() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[1][1];
    fR1 = afEB.getX() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A0xB2
    fR = Math.abs(afAD[2] * aafC[1][2] - afAD[1] * aafC[2][2]);
    fR0 = afEA.getY() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[1][2];
    fR1 = afEB.getX() * aafAbsC[0][1] + afEB.getY() * aafAbsC[0][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A1xB0
    fR = Math.abs(afAD[0] * aafC[2][0] - afAD[2] * aafC[0][0]);
    fR0 = afEA.getX() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[0][0];
    fR1 = afEB.getY() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][1];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A1xB1
    fR = Math.abs(afAD[0] * aafC[2][1] - afAD[2] * aafC[0][1]);
    fR0 = afEA.getX() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[0][1];
    fR1 = afEB.getX() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A1xB2
    fR = Math.abs(afAD[0] * aafC[2][2] - afAD[2] * aafC[0][2]);
    fR0 = afEA.getX() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[0][2];
    fR1 = afEB.getX() * aafAbsC[1][1] + afEB.getY() * aafAbsC[1][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A2xB0
    fR = Math.abs(afAD[1] * aafC[0][0] - afAD[0] * aafC[1][0]);
    fR0 = afEA.getX() * aafAbsC[1][0] + afEA.getY() * aafAbsC[0][0];
    fR1 = afEB.getY() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][1];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A2xB1
    fR = Math.abs(afAD[1] * aafC[0][1] - afAD[0] * aafC[1][1]);
    fR0 = afEA.getX() * aafAbsC[1][1] + afEA.getY() * aafAbsC[0][1];
    fR1 = afEB.getX() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A2xB2
    fR = Math.abs(afAD[1] * aafC[0][2] - afAD[0] * aafC[1][2]);
    fR0 = afEA.getX() * aafAbsC[1][2] + afEA.getY() * aafAbsC[0][2];
    fR1 = afEB.getX() * aafAbsC[2][1] + afEB.getY() * aafAbsC[2][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    return true;
  }

  @Override
  public boolean intersects(final BoundingVolume bv) {
    if (bv == null) {
      return false;
    }

    return bv.intersectsOrientedBoundingBox(this);
  }

  @Override
  public boolean intersectsSphere(final BoundingSphere bs) {
    if (!Vector3.isFinite(_center) || !Vector3.isFinite(bs._center)) {
      return false;
    }

    _compVect1.set(bs.getCenter()).subtractLocal(_center);
    final Matrix3 tempMa = Matrix3.fetchTempInstance().fromAxes(_xAxis, _yAxis, _zAxis);

    tempMa.applyPost(_compVect1, _compVect1);

    boolean result = false;
    if (Math.abs(_compVect1.getX()) < bs.getRadius() + _extent.getX()
        && Math.abs(_compVect1.getY()) < bs.getRadius() + _extent.getY()
        && Math.abs(_compVect1.getZ()) < bs.getRadius() + _extent.getZ()) {
      result = true;
    }

    Matrix3.releaseTempInstance(tempMa);
    return result;
  }

  @Override
  public boolean intersectsBoundingBox(final BoundingBox bb) {
    if (!Vector3.isFinite(_center) || !Vector3.isFinite(bb._center)) {
      return false;
    }

    // Cutoff for cosine of angles between box axes. This is used to catch
    // the cases when at least one pair of axes are parallel. If this
    // happens,
    // there is no need to test for separation along the Cross(A[i],B[j])
    // directions.
    final double cutoff = 0.999999f;
    boolean parallelPairExists = false;
    int i;

    // convenience variables
    final Vector3[] akA = new Vector3[] {_xAxis, _yAxis, _zAxis};
    final Vector3[] akB =
        new Vector3[] {Vector3.fetchTempInstance(), Vector3.fetchTempInstance(), Vector3.fetchTempInstance()};
    final Vector3 afEA = _extent;
    final Vector3 afEB = Vector3.fetchTempInstance().set(bb.getXExtent(), bb.getYExtent(), bb.getZExtent());

    // compute difference of box centers, D = C1-C0
    final Vector3 kD = bb.getCenter().subtract(_center, Vector3.fetchTempInstance());

    final double[][] aafC = {new double[3], new double[3], new double[3]};

    final double[][] aafAbsC = {new double[3], new double[3], new double[3]};

    final double[] afAD = new double[3];
    double fR0, fR1, fR; // interval radii and distance between centers
    double fR01; // = R0 + R1

    try {

      // axis C0+t*A0
      for (i = 0; i < 3; i++) {
        aafC[0][i] = akA[0].dot(akB[i]);
        aafAbsC[0][i] = Math.abs(aafC[0][i]);
        if (aafAbsC[0][i] > cutoff) {
          parallelPairExists = true;
        }
      }
      afAD[0] = akA[0].dot(kD);
      fR = Math.abs(afAD[0]);
      fR1 = afEB.getX() * aafAbsC[0][0] + afEB.getY() * aafAbsC[0][1] + afEB.getZ() * aafAbsC[0][2];
      fR01 = afEA.getX() + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A1
      for (i = 0; i < 3; i++) {
        aafC[1][i] = akA[1].dot(akB[i]);
        aafAbsC[1][i] = Math.abs(aafC[1][i]);
        if (aafAbsC[1][i] > cutoff) {
          parallelPairExists = true;
        }
      }
      afAD[1] = akA[1].dot(kD);
      fR = Math.abs(afAD[1]);
      fR1 = afEB.getX() * aafAbsC[1][0] + afEB.getY() * aafAbsC[1][1] + afEB.getZ() * aafAbsC[1][2];
      fR01 = afEA.getY() + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A2
      for (i = 0; i < 3; i++) {
        aafC[2][i] = akA[2].dot(akB[i]);
        aafAbsC[2][i] = Math.abs(aafC[2][i]);
        if (aafAbsC[2][i] > cutoff) {
          parallelPairExists = true;
        }
      }
      afAD[2] = akA[2].dot(kD);
      fR = Math.abs(afAD[2]);
      fR1 = afEB.getX() * aafAbsC[2][0] + afEB.getY() * aafAbsC[2][1] + afEB.getZ() * aafAbsC[2][2];
      fR01 = afEA.getZ() + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*B0
      fR = Math.abs(akB[0].dot(kD));
      fR0 = afEA.getX() * aafAbsC[0][0] + afEA.getY() * aafAbsC[1][0] + afEA.getZ() * aafAbsC[2][0];
      fR01 = fR0 + afEB.getX();
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*B1
      fR = Math.abs(akB[1].dot(kD));
      fR0 = afEA.getX() * aafAbsC[0][1] + afEA.getY() * aafAbsC[1][1] + afEA.getZ() * aafAbsC[2][1];
      fR01 = fR0 + afEB.getY();
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*B2
      fR = Math.abs(akB[2].dot(kD));
      fR0 = afEA.getX() * aafAbsC[0][2] + afEA.getY() * aafAbsC[1][2] + afEA.getZ() * aafAbsC[2][2];
      fR01 = fR0 + afEB.getZ();
      if (fR > fR01) {
        return false;
      }

      // At least one pair of box axes was parallel, so the separation is
      // effectively in 2D where checking the "edge" normals is sufficient for
      // the separation of the boxes.
      if (parallelPairExists) {
        return true;
      }

      // axis C0+t*A0xB0
      fR = Math.abs(afAD[2] * aafC[1][0] - afAD[1] * aafC[2][0]);
      fR0 = afEA.getY() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[1][0];
      fR1 = afEB.getY() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][1];
      fR01 = fR0 + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A0xB1
      fR = Math.abs(afAD[2] * aafC[1][1] - afAD[1] * aafC[2][1]);
      fR0 = afEA.getY() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[1][1];
      fR1 = afEB.getX() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][0];
      fR01 = fR0 + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A0xB2
      fR = Math.abs(afAD[2] * aafC[1][2] - afAD[1] * aafC[2][2]);
      fR0 = afEA.getY() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[1][2];
      fR1 = afEB.getX() * aafAbsC[0][1] + afEB.getY() * aafAbsC[0][0];
      fR01 = fR0 + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A1xB0
      fR = Math.abs(afAD[0] * aafC[2][0] - afAD[2] * aafC[0][0]);
      fR0 = afEA.getX() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[0][0];
      fR1 = afEB.getY() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][1];
      fR01 = fR0 + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A1xB1
      fR = Math.abs(afAD[0] * aafC[2][1] - afAD[2] * aafC[0][1]);
      fR0 = afEA.getX() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[0][1];
      fR1 = afEB.getX() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][0];
      fR01 = fR0 + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A1xB2
      fR = Math.abs(afAD[0] * aafC[2][2] - afAD[2] * aafC[0][2]);
      fR0 = afEA.getX() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[0][2];
      fR1 = afEB.getX() * aafAbsC[1][1] + afEB.getY() * aafAbsC[1][0];
      fR01 = fR0 + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A2xB0
      fR = Math.abs(afAD[1] * aafC[0][0] - afAD[0] * aafC[1][0]);
      fR0 = afEA.getX() * aafAbsC[1][0] + afEA.getY() * aafAbsC[0][0];
      fR1 = afEB.getY() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][1];
      fR01 = fR0 + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A2xB1
      fR = Math.abs(afAD[1] * aafC[0][1] - afAD[0] * aafC[1][1]);
      fR0 = afEA.getX() * aafAbsC[1][1] + afEA.getY() * aafAbsC[0][1];
      fR1 = afEB.getX() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][0];
      fR01 = fR0 + fR1;
      if (fR > fR01) {
        return false;
      }

      // axis C0+t*A2xB2
      fR = Math.abs(afAD[1] * aafC[0][2] - afAD[0] * aafC[1][2]);
      fR0 = afEA.getX() * aafAbsC[1][2] + afEA.getY() * aafAbsC[0][2];
      fR1 = afEB.getX() * aafAbsC[2][1] + afEB.getY() * aafAbsC[2][0];
      fR01 = fR0 + fR1;
      if (fR > fR01) {
        return false;
      }

      return true;
    } finally {
      // Make sure we release the temp vars
      Vector3.releaseTempInstance(kD);
      Vector3.releaseTempInstance(afEB);
      for (final Vector3 vec : akB) {
        Vector3.releaseTempInstance(vec);
      }

    }
  }

  @Override
  public boolean intersectsOrientedBoundingBox(final OrientedBoundingBox obb) {
    if (!Vector3.isFinite(_center) || !Vector3.isFinite(obb._center)) {
      return false;
    }

    // Cutoff for cosine of angles between box axes. This is used to catch
    // the cases when at least one pair of axes are parallel. If this
    // happens,
    // there is no need to test for separation along the Cross(A[i],B[j])
    // directions.
    final double cutoff = 0.999999f;
    boolean parallelPairExists = false;
    int i;

    // convenience variables
    final Vector3[] akA = new Vector3[] {_xAxis, _yAxis, _zAxis};
    final Vector3[] akB = new Vector3[] {obb._xAxis, obb._yAxis, obb._zAxis};
    final Vector3 afEA = _extent;
    final Vector3 afEB = obb._extent;

    // compute difference of box centers, D = C1-C0
    final Vector3 kD = obb._center.subtract(_center, _compVect1);

    final double[][] aafC = {new double[3], new double[3], new double[3]};

    final double[][] aafAbsC = {new double[3], new double[3], new double[3]};

    final double[] afAD = new double[3];
    double fR0, fR1, fR; // interval radii and distance between centers
    double fR01; // = R0 + R1

    // axis C0+t*A0
    for (i = 0; i < 3; i++) {
      aafC[0][i] = akA[0].dot(akB[i]);
      aafAbsC[0][i] = Math.abs(aafC[0][i]);
      if (aafAbsC[0][i] > cutoff) {
        parallelPairExists = true;
      }
    }
    afAD[0] = akA[0].dot(kD);
    fR = Math.abs(afAD[0]);
    fR1 = afEB.getX() * aafAbsC[0][0] + afEB.getY() * aafAbsC[0][1] + afEB.getZ() * aafAbsC[0][2];
    fR01 = afEA.getX() + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A1
    for (i = 0; i < 3; i++) {
      aafC[1][i] = akA[1].dot(akB[i]);
      aafAbsC[1][i] = Math.abs(aafC[1][i]);
      if (aafAbsC[1][i] > cutoff) {
        parallelPairExists = true;
      }
    }
    afAD[1] = akA[1].dot(kD);
    fR = Math.abs(afAD[1]);
    fR1 = afEB.getX() * aafAbsC[1][0] + afEB.getY() * aafAbsC[1][1] + afEB.getZ() * aafAbsC[1][2];
    fR01 = afEA.getY() + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A2
    for (i = 0; i < 3; i++) {
      aafC[2][i] = akA[2].dot(akB[i]);
      aafAbsC[2][i] = Math.abs(aafC[2][i]);
      if (aafAbsC[2][i] > cutoff) {
        parallelPairExists = true;
      }
    }
    afAD[2] = akA[2].dot(kD);
    fR = Math.abs(afAD[2]);
    fR1 = afEB.getX() * aafAbsC[2][0] + afEB.getY() * aafAbsC[2][1] + afEB.getZ() * aafAbsC[2][2];
    fR01 = afEA.getZ() + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*B0
    fR = Math.abs(akB[0].dot(kD));
    fR0 = afEA.getX() * aafAbsC[0][0] + afEA.getY() * aafAbsC[1][0] + afEA.getZ() * aafAbsC[2][0];
    fR01 = fR0 + afEB.getX();
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*B1
    fR = Math.abs(akB[1].dot(kD));
    fR0 = afEA.getX() * aafAbsC[0][1] + afEA.getY() * aafAbsC[1][1] + afEA.getZ() * aafAbsC[2][1];
    fR01 = fR0 + afEB.getY();
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*B2
    fR = Math.abs(akB[2].dot(kD));
    fR0 = afEA.getX() * aafAbsC[0][2] + afEA.getY() * aafAbsC[1][2] + afEA.getZ() * aafAbsC[2][2];
    fR01 = fR0 + afEB.getZ();
    if (fR > fR01) {
      return false;
    }

    // At least one pair of box axes was parallel, so the separation is
    // effectively in 2D where checking the "edge" normals is sufficient for
    // the separation of the boxes.
    if (parallelPairExists) {
      return true;
    }

    // axis C0+t*A0xB0
    fR = Math.abs(afAD[2] * aafC[1][0] - afAD[1] * aafC[2][0]);
    fR0 = afEA.getY() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[1][0];
    fR1 = afEB.getY() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][1];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A0xB1
    fR = Math.abs(afAD[2] * aafC[1][1] - afAD[1] * aafC[2][1]);
    fR0 = afEA.getY() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[1][1];
    fR1 = afEB.getX() * aafAbsC[0][2] + afEB.getZ() * aafAbsC[0][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A0xB2
    fR = Math.abs(afAD[2] * aafC[1][2] - afAD[1] * aafC[2][2]);
    fR0 = afEA.getY() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[1][2];
    fR1 = afEB.getX() * aafAbsC[0][1] + afEB.getY() * aafAbsC[0][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A1xB0
    fR = Math.abs(afAD[0] * aafC[2][0] - afAD[2] * aafC[0][0]);
    fR0 = afEA.getX() * aafAbsC[2][0] + afEA.getZ() * aafAbsC[0][0];
    fR1 = afEB.getY() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][1];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A1xB1
    fR = Math.abs(afAD[0] * aafC[2][1] - afAD[2] * aafC[0][1]);
    fR0 = afEA.getX() * aafAbsC[2][1] + afEA.getZ() * aafAbsC[0][1];
    fR1 = afEB.getX() * aafAbsC[1][2] + afEB.getZ() * aafAbsC[1][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A1xB2
    fR = Math.abs(afAD[0] * aafC[2][2] - afAD[2] * aafC[0][2]);
    fR0 = afEA.getX() * aafAbsC[2][2] + afEA.getZ() * aafAbsC[0][2];
    fR1 = afEB.getX() * aafAbsC[1][1] + afEB.getY() * aafAbsC[1][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A2xB0
    fR = Math.abs(afAD[1] * aafC[0][0] - afAD[0] * aafC[1][0]);
    fR0 = afEA.getX() * aafAbsC[1][0] + afEA.getY() * aafAbsC[0][0];
    fR1 = afEB.getY() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][1];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A2xB1
    fR = Math.abs(afAD[1] * aafC[0][1] - afAD[0] * aafC[1][1]);
    fR0 = afEA.getX() * aafAbsC[1][1] + afEA.getY() * aafAbsC[0][1];
    fR1 = afEB.getX() * aafAbsC[2][2] + afEB.getZ() * aafAbsC[2][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    // axis C0+t*A2xB2
    fR = Math.abs(afAD[1] * aafC[0][2] - afAD[0] * aafC[1][2]);
    fR0 = afEA.getX() * aafAbsC[1][2] + afEA.getY() * aafAbsC[0][2];
    fR1 = afEB.getX() * aafAbsC[2][1] + afEB.getY() * aafAbsC[2][0];
    fR01 = fR0 + fR1;
    if (fR > fR01) {
      return false;
    }

    return true;
  }

  @Override
  public boolean intersects(final ReadOnlyRay3 ray) {
    if (!Vector3.isFinite(_center)) {
      return false;
    }

    double rhs;
    final ReadOnlyVector3 rayDir = ray.getDirection();
    final Vector3 diff = _compVect1.set(ray.getOrigin()).subtractLocal(_center);
    final Vector3 wCrossD = _compVect2;

    final double[] fWdU = new double[3];
    final double[] fAWdU = new double[3];
    final double[] fDdU = new double[3];
    final double[] fADdU = new double[3];
    final double[] fAWxDdU = new double[3];

    fWdU[0] = rayDir.dot(_xAxis);
    fAWdU[0] = Math.abs(fWdU[0]);
    fDdU[0] = diff.dot(_xAxis);
    fADdU[0] = Math.abs(fDdU[0]);
    if (fADdU[0] > _extent.getX() && fDdU[0] * fWdU[0] >= 0.0) {
      return false;
    }

    fWdU[1] = rayDir.dot(_yAxis);
    fAWdU[1] = Math.abs(fWdU[1]);
    fDdU[1] = diff.dot(_yAxis);
    fADdU[1] = Math.abs(fDdU[1]);
    if (fADdU[1] > _extent.getY() && fDdU[1] * fWdU[1] >= 0.0) {
      return false;
    }

    fWdU[2] = rayDir.dot(_zAxis);
    fAWdU[2] = Math.abs(fWdU[2]);
    fDdU[2] = diff.dot(_zAxis);
    fADdU[2] = Math.abs(fDdU[2]);
    if (fADdU[2] > _extent.getZ() && fDdU[2] * fWdU[2] >= 0.0) {
      return false;
    }

    rayDir.cross(diff, wCrossD);

    fAWxDdU[0] = Math.abs(wCrossD.dot(_xAxis));
    rhs = _extent.getY() * fAWdU[2] + _extent.getZ() * fAWdU[1];
    if (fAWxDdU[0] > rhs) {
      return false;
    }

    fAWxDdU[1] = Math.abs(wCrossD.dot(_yAxis));
    rhs = _extent.getX() * fAWdU[2] + _extent.getZ() * fAWdU[0];
    if (fAWxDdU[1] > rhs) {
      return false;
    }

    fAWxDdU[2] = Math.abs(wCrossD.dot(_zAxis));
    rhs = _extent.getX() * fAWdU[1] + _extent.getY() * fAWdU[0];
    if (fAWxDdU[2] > rhs) {
      return false;

    }

    return true;
  }

  @Override
  public IntersectionRecord intersectsWhere(final ReadOnlyRay3 ray) {
    final ReadOnlyVector3 rayDir = ray.getDirection();
    final ReadOnlyVector3 rayOrigin = ray.getOrigin();

    // convert ray to box coordinates
    final Vector3 diff = rayOrigin.subtract(getCenter(), _compVect1);
    diff.set(_xAxis.dot(diff), _yAxis.dot(diff), _zAxis.dot(diff));
    final Vector3 direction = _compVect2.set(_xAxis.dot(rayDir), _yAxis.dot(rayDir), _zAxis.dot(rayDir));

    final double[] t = {0, Double.POSITIVE_INFINITY};

    final double saveT0 = t[0], saveT1 = t[1];
    final boolean notEntirelyClipped = clip(+direction.getX(), -diff.getX() - _extent.getX(), t)
        && clip(-direction.getX(), +diff.getX() - _extent.getX(), t)
        && clip(+direction.getY(), -diff.getY() - _extent.getY(), t)
        && clip(-direction.getY(), +diff.getY() - _extent.getY(), t)
        && clip(+direction.getZ(), -diff.getZ() - _extent.getZ(), t)
        && clip(-direction.getZ(), +diff.getZ() - _extent.getZ(), t);

    if (notEntirelyClipped && (t[0] != saveT0 || t[1] != saveT1)) {
      if (t[1] > t[0]) {
        final double[] distances = t;
        final Vector3[] points = new Vector3[] {rayDir.multiply(distances[0], new Vector3()).addLocal(rayOrigin),
            rayDir.multiply(distances[1], new Vector3()).addLocal(rayOrigin)};
        final IntersectionRecord record = new IntersectionRecord(distances, points);
        return record;
      }

      final double[] distances = new double[] {t[0]};
      final Vector3[] points = new Vector3[] {rayDir.multiply(distances[0], new Vector3()).addLocal(rayOrigin)};
      final IntersectionRecord record = new IntersectionRecord(distances, points);
      return record;
    }

    return null;

  }

  /**
   * <code>clip</code> determines if a line segment intersects the current test plane.
   *
   * @param denom
   *          the denominator of the line segment.
   * @param numer
   *          the numerator of the line segment.
   * @param t
   *          test values of the plane.
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

  public void setXAxis(final ReadOnlyVector3 axis) {
    _xAxis.set(axis);
    correctCorners = false;
  }

  public void setYAxis(final ReadOnlyVector3 axis) {
    _yAxis.set(axis);
    correctCorners = false;
  }

  public void setZAxis(final ReadOnlyVector3 axis) {
    _zAxis.set(axis);
    correctCorners = false;
  }

  public void setExtent(final ReadOnlyVector3 ext) {
    _extent.set(ext);
    correctCorners = false;
  }

  public ReadOnlyVector3 getXAxis() { return _xAxis; }

  public ReadOnlyVector3 getYAxis() { return _yAxis; }

  public ReadOnlyVector3 getZAxis() { return _zAxis; }

  public ReadOnlyVector3 getExtent() { return _extent; }

  @Override
  public boolean contains(final ReadOnlyVector3 point) {
    _compVect1.set(point).subtractLocal(_center);
    double coeff = _compVect1.dot(_xAxis);
    if (Math.abs(coeff) > _extent.getX()) {
      return false;
    }

    coeff = _compVect1.dot(_yAxis);
    if (Math.abs(coeff) > _extent.getY()) {
      return false;
    }

    coeff = _compVect1.dot(_zAxis);
    if (Math.abs(coeff) > _extent.getZ()) {
      return false;
    }

    return true;
  }

  @Override
  public double distanceToEdge(final ReadOnlyVector3 point) {
    // compute coordinates of point in box coordinate system
    final Vector3 diff = point.subtract(_center, _compVect1);
    final Vector3 closest = _compVect2.set(diff.dot(_xAxis), diff.dot(_yAxis), diff.dot(_zAxis));

    // project test point onto box
    double sqrDistance = 0.0;
    double delta;

    if (closest.getX() < -_extent.getX()) {
      delta = closest.getX() + _extent.getX();
      sqrDistance += delta * delta;
      closest.setX(-_extent.getX());
    } else if (closest.getX() > _extent.getX()) {
      delta = closest.getX() - _extent.getX();
      sqrDistance += delta * delta;
      closest.setX(_extent.getX());
    }

    if (closest.getY() < -_extent.getY()) {
      delta = closest.getY() + _extent.getY();
      sqrDistance += delta * delta;
      closest.setY(-_extent.getY());
    } else if (closest.getY() > _extent.getY()) {
      delta = closest.getY() - _extent.getY();
      sqrDistance += delta * delta;
      closest.setY(_extent.getY());
    }

    if (closest.getZ() < -_extent.getZ()) {
      delta = closest.getZ() + _extent.getZ();
      sqrDistance += delta * delta;
      closest.setZ(-_extent.getZ());
    } else if (closest.getZ() > _extent.getZ()) {
      delta = closest.getZ() - _extent.getZ();
      sqrDistance += delta * delta;
      closest.setZ(_extent.getZ());
    }

    return Math.sqrt(sqrDistance);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_xAxis, "_xAxis", (Vector3) Vector3.UNIT_X);
    capsule.write(_yAxis, "yAxis", (Vector3) Vector3.UNIT_Y);
    capsule.write(_zAxis, "zAxis", (Vector3) Vector3.UNIT_Z);
    capsule.write(_extent, "extent", (Vector3) Vector3.ZERO);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _xAxis.set(capsule.readSavable("xAxis", (Vector3) Vector3.UNIT_X));
    _yAxis.set(capsule.readSavable("yAxis", (Vector3) Vector3.UNIT_Y));
    _zAxis.set(capsule.readSavable("zAxis", (Vector3) Vector3.UNIT_Z));
    _extent.set(capsule.readSavable("extent", (Vector3) Vector3.ZERO));
    correctCorners = false;
  }

  @Override
  public double getVolume() { return (8 * _extent.getX() * _extent.getY() * _extent.getZ()); }
}
