/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.framework.Canvas;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * This class represents a view into a 3d scene and how that view should map to a 2D rendering
 * surface.
 */
public class Camera implements Savable, Externalizable {

  private static final long serialVersionUID = 1L;

  private static final Logger _logger = Logger.getLogger(Camera.class.getName());

  public enum FrustumIntersect {
    /**
     * Object being compared to the frustum is completely outside of the frustum.
     */
    Outside,

    /**
     * Object being compared to the frustum is completely inside of the frustum.
     */
    Inside,

    /**
     * Object being compared to the frustum intersects one of the frustum planes and is thus both inside
     * and outside of the frustum.
     */
    Intersects;
  }

  // planes of the frustum
  /**
   * LEFT_PLANE represents the left plane of the camera frustum.
   */
  public static final int LEFT_PLANE = 0;

  /**
   * RIGHT_PLANE represents the right plane of the camera frustum.
   */
  public static final int RIGHT_PLANE = 1;

  /**
   * BOTTOM_PLANE represents the bottom plane of the camera frustum.
   */
  public static final int BOTTOM_PLANE = 2;

  /**
   * TOP_PLANE represents the top plane of the camera frustum.
   */
  public static final int TOP_PLANE = 3;

  /**
   * FAR_PLANE represents the far plane of the camera frustum.
   */
  public static final int FAR_PLANE = 4;

  /**
   * NEAR_PLANE represents the near plane of the camera frustum.
   */
  public static final int NEAR_PLANE = 5;

  /**
   * FRUSTUM_PLANES represents the number of planes of the camera frustum.
   */
  public static final int FRUSTUM_PLANES = 6;

  /**
   * MAX_WORLD_PLANES holds the maximum planes allowed by the system.
   */
  public static final int MAX_WORLD_PLANES = 32;

  // the location and orientation of the camera.
  /**
   * Camera's location
   */
  protected final Vector3 _location = new Vector3();

  /**
   * Direction of camera's 'left'
   */
  protected final Vector3 _left = new Vector3();

  /**
   * Direction of 'up' for camera.
   */
  protected final Vector3 _up = new Vector3();

  /**
   * Direction the camera is facing.
   */
  protected final Vector3 _direction = new Vector3();

  /**
   * The near range for mapping depth values from normalized device coordinates to window coordinates.
   */
  protected double _depthRangeNear;

  /**
   * The far range for mapping depth values from normalized device coordinates to window coordinates.
   */
  protected double _depthRangeFar;

  /**
   * Distance from camera to near frustum plane.
   */
  protected double _frustumNear;

  /**
   * Distance from camera to far frustum plane.
   */
  protected double _frustumFar;

  /**
   * Distance from camera to left frustum plane.
   */
  protected double _frustumLeft;

  /**
   * Distance from camera to right frustum plane.
   */
  protected double _frustumRight;

  /**
   * Distance from camera to top frustum plane.
   */
  protected double _frustumTop;

  /**
   * Distance from camera to bottom frustum plane.
   */
  protected double _frustumBottom;

  /**
   * Convenience store for fovY. Only set during setFrustumPerspective and never used. Retrieve by
   * getFovY(). Default is NaN.
   */
  protected double _fovY = Double.NaN;

  // Temporary values computed in onFrustumChange that are needed if a
  // call is made to onFrameChange.
  protected double _coeffLeft[];
  protected double _coeffRight[];
  protected double _coeffBottom[];
  protected double _coeffTop[];

  /** Number of camera planes used by this camera. Default is 6. */
  protected int _planeQuantity;

  // view port coordinates
  /**
   * Percent value on display where horizontal viewing starts for this camera. Default is 0.
   */
  protected double _viewPortLeft;

  /**
   * Percent value on display where horizontal viewing ends for this camera. Default is 1.
   */
  protected double _viewPortRight;

  /**
   * Percent value on display where vertical viewing ends for this camera. Default is 1.
   */
  protected double _viewPortTop;

  /**
   * Percent value on display where vertical viewing begins for this camera. Default is 0.
   */
  protected double _viewPortBottom;

  /**
   * Array holding the planes that this camera will check for culling.
   */
  protected Plane[] _worldPlane;

  protected final FloatBuffer _matrixBuffer = BufferUtils.createFloatBuffer(16);

  /**
   * Computation vector used in various operations.
   */
  protected final Vector3 _tempVector = new Vector3();

  /**
   * Projection mode used by the camera.
   */
  public enum ProjectionMode {
    Perspective, Orthographic, Custom
  }

  /**
   * Current projection mode.
   */
  private ProjectionMode _projectionMode = ProjectionMode.Perspective;

  private boolean _updateMVMatrix = true;
  private boolean _updatePMatrix = true;
  private boolean _updateMVPMatrix = true;
  private boolean _updateInverseMVPMatrix = true;

  // NB: These matrices are column-major.
  protected final Matrix4 _view = new Matrix4();
  protected final Matrix4 _projection = new Matrix4();
  private final Matrix4 _modelViewProjection = new Matrix4();
  private final Matrix4 _modelViewProjectionInverse = new Matrix4();

  protected boolean _depthRangeDirty;
  protected boolean _frustumDirty;
  protected boolean _viewPortDirty;
  protected boolean _frameDirty;

  /**
   * A set of layers to use for filtering Spatials from queuing and rendering. See
   * {@link #_exclusiveLayers} for more details.
   */
  protected Set<Integer> _layers = new HashSet<>();
  /**
   * If true, the _layers set is exclusive - meaning values that appear in the set are excluded from
   * rendering when this Camera is the current active camera. If false, the set is instead inclusive,
   * meaning that ONLY layers that appear in the set are allowed to queue and render.
   */
  protected boolean _exclusiveLayers = true;

  /**
   * A mask value set during contains() that allows fast culling of a Node's children.
   */
  private int _planeState;

  protected int _width;
  protected int _height;

  /**
   * Construct a new Camera with a width and height of 100.
   */
  public Camera() {
    this(100, 100);
  }

  /**
   * Construct a new Camera with the given frame width and height.
   *
   * @param width
   * @param height
   */
  public Camera(final int width, final int height) {
    _width = width;
    _height = height;

    _location.set(0, 0, 0);
    _left.set(-1, 0, 0);
    _up.set(0, 1, 0);
    _direction.set(0, 0, -1);

    _depthRangeNear = 0.0;
    _depthRangeFar = 1.0;
    _depthRangeDirty = true;

    _frustumNear = 1.0;
    _frustumFar = 2.0;
    _frustumLeft = -0.5;
    _frustumRight = 0.5;
    _frustumTop = 0.5;
    _frustumBottom = -0.5;

    _coeffLeft = new double[2];
    _coeffRight = new double[2];
    _coeffBottom = new double[2];
    _coeffTop = new double[2];

    _viewPortLeft = 0.0;
    _viewPortRight = 1.0;
    _viewPortTop = 1.0;
    _viewPortBottom = 0.0;

    _planeQuantity = 6;

    _worldPlane = new Plane[Camera.MAX_WORLD_PLANES];
    for (int i = 0; i < Camera.MAX_WORLD_PLANES; i++) {
      _worldPlane[i] = new Plane();
    }

    onFrustumChange();
    onViewPortChange();
    onFrameChange();

    Camera._logger.fine("Camera created. W: " + width + "  H: " + height);
  }

  /**
   * Construct a new camera, using the given source camera's values.
   *
   * @param source
   */
  public Camera(final Camera source) {

    _coeffLeft = new double[2];
    _coeffRight = new double[2];
    _coeffBottom = new double[2];
    _coeffTop = new double[2];

    _worldPlane = new Plane[Camera.MAX_WORLD_PLANES];
    for (int i = 0; i < Camera.MAX_WORLD_PLANES; i++) {
      _worldPlane[i] = new Plane();
    }

    set(source);

    Camera._logger.fine("Camera created. W: " + getWidth() + "  H: " + getHeight());
  }

  /**
   * Copy the source camera's fields to this camera
   *
   * @param source
   *          the camera to copy from
   */
  public void set(final Camera source) {
    _width = source.getWidth();
    _height = source.getHeight();

    _location.set(source.getLocation());
    _left.set(source.getLeft());
    _up.set(source.getUp());
    _direction.set(source.getDirection());
    _fovY = source.getFovY();

    _depthRangeNear = source.getDepthRangeNear();
    _depthRangeFar = source.getDepthRangeFar();
    _depthRangeDirty = true;

    _frustumNear = source.getFrustumNear();
    _frustumFar = source.getFrustumFar();
    _frustumLeft = source.getFrustumLeft();
    _frustumRight = source.getFrustumRight();
    _frustumTop = source.getFrustumTop();
    _frustumBottom = source.getFrustumBottom();

    _viewPortLeft = source.getViewPortLeft();
    _viewPortRight = source.getViewPortRight();
    _viewPortTop = source.getViewPortTop();
    _viewPortBottom = source.getViewPortBottom();

    _planeQuantity = 6;

    _projectionMode = source.getProjectionMode();

    onFrustumChange();
    onViewPortChange();
    onFrameChange();

  }

  public double getDepthRangeFar() { return _depthRangeFar; }

  /**
   * @param depthRangeNear
   *          the far clipping plane for window coordinates. Should be in the range [0, 1]. Default is
   *          1.
   */
  public void setDepthRangeFar(final double depthRangeFar) {
    _depthRangeFar = depthRangeFar;
    _depthRangeDirty = true;
  }

  public double getDepthRangeNear() { return _depthRangeNear; }

  /**
   * @param depthRangeNear
   *          the near clipping plane for window coordinates. Should be in the range [0, 1]. Default
   *          is 0.
   */
  public void setDepthRangeNear(final double depthRangeNear) {
    _depthRangeNear = depthRangeNear;
    _depthRangeDirty = true;
  }

  /**
   * @return the value of the bottom frustum plane.
   */
  public double getFrustumBottom() { return _frustumBottom; }

  /**
   * @param frustumBottom
   *          the new value of the bottom frustum plane.
   */
  public void setFrustumBottom(final double frustumBottom) {
    _frustumBottom = frustumBottom;
    onFrustumChange();
  }

  /**
   * = * @return the value of the far frustum plane.
   */
  public double getFrustumFar() { return _frustumFar; }

  /**
   * @param frustumFar
   *          the new value of the far frustum plane.
   */
  public void setFrustumFar(final double frustumFar) {
    _frustumFar = frustumFar;
    onFrustumChange();
  }

  /**
   * @return the value of the left frustum plane.
   */
  public double getFrustumLeft() { return _frustumLeft; }

  /**
   * @param frustumLeft
   *          the new value of the left frustum plane.
   */
  public void setFrustumLeft(final double frustumLeft) {
    _frustumLeft = frustumLeft;
    onFrustumChange();
  }

  /**
   * @return the value of the near frustum plane.
   */
  public double getFrustumNear() { return _frustumNear; }

  /**
   * @param frustumNear
   *          the new value of the near frustum plane.
   */
  public void setFrustumNear(final double frustumNear) {
    _frustumNear = frustumNear;
    onFrustumChange();
  }

  /**
   * @return frustumRight the value of the right frustum plane.
   */
  public double getFrustumRight() { return _frustumRight; }

  /**
   * @param frustumRight
   *          the new value of the right frustum plane.
   */
  public void setFrustumRight(final double frustumRight) {
    _frustumRight = frustumRight;
    onFrustumChange();
  }

  /**
   * @return the value of the top frustum plane.
   */
  public double getFrustumTop() { return _frustumTop; }

  /**
   * @param frustumTop
   *          the new value of the top frustum plane.
   */
  public void setFrustumTop(final double frustumTop) {
    _frustumTop = frustumTop;
    onFrustumChange();
  }

  /**
   * @return the current position of the camera.
   */
  public ReadOnlyVector3 getLocation() { return _location; }

  /**
   * @return the current direction the camera is facing.
   */
  public ReadOnlyVector3 getDirection() { return _direction; }

  /**
   * @return the left axis of the camera.
   */
  public ReadOnlyVector3 getLeft() { return _left; }

  /**
   * @return the up axis of the camera.
   */
  public ReadOnlyVector3 getUp() { return _up; }

  /**
   * @param location
   *          the new location or position of the camera.
   */
  public void setLocation(final ReadOnlyVector3 location) {
    _location.set(location);
    onFrameChange();
  }

  /**
   * @param location
   *          the new location or position of the camera.
   */
  public void setLocation(final double x, final double y, final double z) {
    _location.set(x, y, z);
    onFrameChange();
  }

  /**
   * Sets the new direction this camera is facing. This does not change left or up axes, so make sure
   * those vectors are properly set as well.
   *
   * @param direction
   *          the new direction this camera is facing.
   */
  public void setDirection(final ReadOnlyVector3 direction) {
    _direction.set(direction);
    onFrameChange();
  }

  /**
   * Sets the new left axis of this camera. This does not change direction or up axis, so make sure
   * those vectors are properly set as well.
   *
   * @param left
   *          the new left axis of this camera.
   */
  public void setLeft(final ReadOnlyVector3 left) {
    _left.set(left);
    onFrameChange();
  }

  /**
   * Sets the new up axis of this camera. This does not change direction or left axis, so make sure
   * those vectors are properly set as well.
   *
   * @param up
   *          the new up axis of this camera.
   */
  public void setUp(final ReadOnlyVector3 up) {
    _up.set(up);
    onFrameChange();
  }

  /**
   * @param left
   *          the new left axis of the camera.
   * @param up
   *          the new up axis of the camera.
   * @param direction
   *          the new direction the camera is facing.
   */
  public void setAxes(final ReadOnlyVector3 left, final ReadOnlyVector3 up, final ReadOnlyVector3 direction) {
    _left.set(left);
    _up.set(up);
    _direction.set(direction);
    onFrameChange();
  }

  public Matrix3 getAxes(final Matrix3 store) {
    final var rVal = store != null ? store : new Matrix3();

    rVal.setColumn(0, _left);
    rVal.setColumn(1, _up);
    rVal.setColumn(2, _direction);

    return rVal;
  }

  /**
   * Sets our left, up and direction values from the given rotation matrix.
   *
   * @param axes
   *          the matrix that defines the orientation of the camera.
   */
  public void setAxes(final ReadOnlyMatrix3 axes) {
    axes.getColumn(0, _left);
    axes.getColumn(1, _up);
    axes.getColumn(2, _direction);
    onFrameChange();
  }

  /**
   * Ensure our up, left and direction are unit-length vectors.
   */
  public void normalize() {
    _left.normalizeLocal();
    _up.normalizeLocal();
    _direction.normalizeLocal();
    onFrameChange();
  }

  /**
   * Sets the frustum plane values of this camera using the given values.
   *
   * @param near
   * @param far
   * @param left
   * @param right
   * @param top
   * @param bottom
   */
  public void setFrustum(final double near, final double far, final double left, final double right, final double top,
      final double bottom) {
    _frustumNear = near;
    _frustumFar = far;
    _frustumLeft = left;
    _frustumRight = right;
    _frustumTop = top;
    _frustumBottom = bottom;
    onFrustumChange();
  }

  /**
   * Sets the frustum plane values of this camera using those of a given source camera
   *
   * @param source
   *          a source camera.
   */
  public void setFrustum(final Camera source) {
    _frustumNear = source.getFrustumNear();
    _frustumFar = source.getFrustumFar();
    _frustumLeft = source.getFrustumLeft();
    _frustumRight = source.getFrustumRight();
    _frustumTop = source.getFrustumTop();
    _frustumBottom = source.getFrustumBottom();
    onFrustumChange();
  }

  /**
   * Sets the frustum plane values of this camera using the given perspective values.
   *
   * @param fovY
   *          the full angle of view on the Y axis, in degrees.
   * @param aspect
   *          the aspect ratio of our view (generally in [0,1]). Often this is canvas width / canvas
   *          height.
   * @param near
   *          our near plane value
   * @param far
   *          our far plane value
   */
  public void setFrustumPerspective(final double fovY, final double aspect, final double near, final double far) {
    if (Double.isNaN(aspect) || Double.isInfinite(aspect)) {
      // ignore.
      Camera._logger.warning("Invalid aspect given to setFrustumPerspective: " + aspect);
      return;
    }

    _projectionMode = ProjectionMode.Perspective;
    _fovY = fovY;
    final double h = Math.tan(_fovY * MathUtils.DEG_TO_RAD * .5) * near;
    final double w = h * aspect;
    _frustumLeft = -w;
    _frustumRight = w;
    _frustumBottom = -h;
    _frustumTop = h;
    _frustumNear = near;
    _frustumFar = far;
    onFrustumChange();
  }

  /**
   * Accessor for the fovY value. Note that this value is only present if setFrustumPerspective was
   * previously called.
   *
   * @return the fovY value
   */
  public double getFovY() { return _fovY; }

  /**
   * Sets the axes and location of the camera. Similar to
   * {@link #setAxes(ReadOnlyVector3, ReadOnlyVector3, ReadOnlyVector3)}, but sets camera location as
   * well.
   *
   * @param location
   * @param left
   * @param up
   * @param direction
   */
  public void setFrame(final ReadOnlyVector3 location, final ReadOnlyVector3 left, final ReadOnlyVector3 up,
      final ReadOnlyVector3 direction) {
    _left.set(left);
    _up.set(up);
    _direction.set(direction);
    _location.set(location);
    onFrameChange();
  }

  /**
   * Sets the axes and location of the camera. Similar to {@link #setAxes(ReadOnlyMatrix3)}, but sets
   * camera location as well.
   *
   * @param location
   *          the point position of the camera.
   * @param axes
   *          the orientation of the camera.
   */
  public void setFrame(final ReadOnlyVector3 location, final ReadOnlyMatrix3 axes) {
    axes.getColumn(0, _left);
    axes.getColumn(1, _up);
    axes.getColumn(2, _direction);
    _location.set(location);
    onFrameChange();
  }

  /**
   * Sets the axes and location of the camera using those of a given source camera
   *
   * @param source
   *          a source camera.
   */
  public void setFrame(final Camera source) {
    _left.set(source.getLeft());
    _up.set(source.getUp());
    _direction.set(source.getDirection());
    _location.set(source.getLocation());
    onFrameChange();
  }

  /**
   * A convenience method for auto-setting the frame based on a world position the user desires the
   * camera to look at. It points the camera towards the given position using the difference between
   * that position and the current camera location as a direction vector and the general worldUpVector
   * to compute up and left camera vectors.
   *
   * @param pos
   *          where to look at in terms of world coordinates
   * @param worldUpVector
   *          a normalized vector indicating the up direction of the world. (often
   *          {@link Vector3#UNIT_Y} or {@link Vector3#UNIT_Z})
   */
  public void lookAt(final ReadOnlyVector3 pos, final ReadOnlyVector3 worldUpVector) {
    lookAt(pos.getX(), pos.getY(), pos.getZ(), worldUpVector);
  }

  /**
   * A convenience method for auto-setting the frame based on a world position the user desires the
   * camera to look at. It points the camera towards the given position using the difference between
   * that position and the current camera location as a direction vector and the general worldUpVector
   * to compute up and left camera vectors.
   *
   * @param x
   *          where to look at in terms of world coordinates (x)
   * @param y
   *          where to look at in terms of world coordinates (y)
   * @param z
   *          where to look at in terms of world coordinates (z)
   * @param worldUpVector
   *          a normalized vector indicating the up direction of the world. (often
   *          {@link Vector3#UNIT_Y} or {@link Vector3#UNIT_Z})
   */
  public void lookAt(final double x, final double y, final double z, final ReadOnlyVector3 worldUpVector) {
    final Vector3 newDirection = _tempVector;
    newDirection.set(x, y, z).subtractLocal(_location).normalizeLocal();

    // check to see if we haven't really updated camera -- no need to call sets.
    if (newDirection.equals(_direction)) {
      return;
    }
    _direction.set(newDirection);

    _up.set(worldUpVector).normalizeLocal();
    if (_up.equals(Vector3.ZERO)) {
      _up.set(Vector3.UNIT_Y);
    }
    _left.set(_up).crossLocal(_direction).normalizeLocal();
    if (_left.equals(Vector3.ZERO)) {
      if (_direction.getX() != 0.0) {
        _left.set(_direction.getY(), -_direction.getX(), 0);
      } else {
        _left.set(0, _direction.getZ(), -_direction.getY());
      }
    }
    _up.set(_direction).crossLocal(_left).normalizeLocal();
    onFrameChange();
  }

  /**
   * Forces all aspect of the camera to be updated from internal values, and sets all dirty flags to
   * true so that the next apply() call will fully set this camera to the render context.
   */
  public void update() {
    _depthRangeDirty = true;
    onFrustumChange();
    onViewPortChange();
    onFrameChange();
  }

  /**
   * @return an internally used bitmask describing what frustum planes have been examined for culling
   *         thus far.
   */
  public int getPlaneState() { return _planeState; }

  /**
   * @param planeState
   *          a new value for planeState.
   * @see #getPlaneState()
   */
  public void setPlaneState(final int planeState) { _planeState = planeState; }

  /**
   * @return the left boundary of the viewport
   */
  public double getViewPortLeft() { return _viewPortLeft; }

  /**
   * @param left
   *          the new left boundary of the viewport
   */
  public void setViewPortLeft(final double left) {
    _viewPortLeft = left;
    onViewPortChange();
  }

  /**
   * @return the right boundary of the viewport
   */
  public double getViewPortRight() { return _viewPortRight; }

  /**
   * @param right
   *          the new right boundary of the viewport
   */
  public void setViewPortRight(final double right) {
    _viewPortRight = right;
    onViewPortChange();
  }

  /**
   * @return the top boundary of the viewport
   */
  public double getViewPortTop() { return _viewPortTop; }

  /**
   * @param top
   *          the new top boundary of the viewport
   */
  public void setViewPortTop(final double top) {
    _viewPortTop = top;
    onViewPortChange();
  }

  /**
   * @return the bottom boundary of the viewport
   */
  public double getViewPortBottom() { return _viewPortBottom; }

  /**
   * @param bottom
   *          the new bottom boundary of the viewport
   */
  public void setViewPortBottom(final double bottom) {
    _viewPortBottom = bottom;
    onViewPortChange();
  }

  /**
   * Sets the boundaries of this camera's viewport to the given values
   *
   * @param left
   * @param right
   * @param bottom
   * @param top
   */
  public void setViewPort(final double left, final double right, final double bottom, final double top) {
    setViewPortLeft(left);
    setViewPortRight(right);
    setViewPortBottom(bottom);
    setViewPortTop(top);
  }

  public Set<Integer> getLayers() { return _layers; }

  public void setLayers(final Set<Integer> layers) {
    _layers.clear();
    _layers.addAll(layers);
  }

  public boolean addLayer(final int layer) {
    return _layers.add(layer);
  }

  public boolean removeLayer(final int layer) {
    return _layers.remove(layer);
  }

  public boolean checkLayerPasses(final int layer) {
    final boolean found = _layers.contains(layer);
    if (_exclusiveLayers) {
      return !found;
    } else {
      return found;
    }
  }

  public boolean isExclusiveLayers() { return _exclusiveLayers; }

  public void setExclusiveLayers(final boolean exclusiveLayers) { _exclusiveLayers = exclusiveLayers; }

  /**
   * Checks a bounding volume against the planes of this camera's frustum and returns if it is
   * completely inside of, outside of, or intersecting.
   *
   * @param bound
   *          the bound to check for culling
   * @return intersection type
   */
  public Camera.FrustumIntersect contains(final BoundingVolume bound) {
    if (bound == null) {
      return FrustumIntersect.Inside;
    }

    int mask;
    FrustumIntersect rVal = FrustumIntersect.Inside;

    for (int planeCounter = Camera.FRUSTUM_PLANES; planeCounter >= 0; planeCounter--) {
      if (planeCounter == bound.getCheckPlane()) {
        continue; // we have already checked this plane at first iteration
      }
      final int planeId = (planeCounter == Camera.FRUSTUM_PLANES) ? bound.getCheckPlane() : planeCounter;

      mask = 1 << (planeId);
      if ((_planeState & mask) == 0) {
        switch (bound.whichSide(_worldPlane[planeId])) {
          case Inside:
            // object is outside of frustum
            bound.setCheckPlane(planeId);
            return FrustumIntersect.Outside;
          case Outside:
            // object is visible on *this* plane, so mark this plane
            // so that we don't check it for sub nodes.
            _planeState |= mask;
            break;
          case Neither:
            rVal = FrustumIntersect.Intersects;
            break;
        }
      }
    }

    return rVal;
  }

  /**
   * Resizes this camera's view with the given width and height.
   *
   * @param width
   *          the view width
   * @param height
   *          the view height
   */
  public void resize(final int width, final int height) {
    if (_width == width && _height == height) {
      return;
    }

    _width = width;
    _height = height;
    onViewPortChange();
  }

  /**
   * Updates internal frustum coefficient values to reflect the current frustum plane values.
   */
  public void onFrustumChange() {
    if (getProjectionMode() == ProjectionMode.Perspective) {
      final double nearSquared = _frustumNear * _frustumNear;
      final double leftSquared = _frustumLeft * _frustumLeft;
      final double rightSquared = _frustumRight * _frustumRight;
      final double bottomSquared = _frustumBottom * _frustumBottom;
      final double topSquared = _frustumTop * _frustumTop;

      double inverseLength = 1.0 / Math.sqrt(nearSquared + leftSquared);
      _coeffLeft[0] = _frustumNear * inverseLength;
      _coeffLeft[1] = -_frustumLeft * inverseLength;

      inverseLength = 1.0 / Math.sqrt(nearSquared + rightSquared);
      _coeffRight[0] = -_frustumNear * inverseLength;
      _coeffRight[1] = _frustumRight * inverseLength;

      inverseLength = 1.0 / Math.sqrt(nearSquared + bottomSquared);
      _coeffBottom[0] = _frustumNear * inverseLength;
      _coeffBottom[1] = -_frustumBottom * inverseLength;

      inverseLength = 1.0 / Math.sqrt(nearSquared + topSquared);
      _coeffTop[0] = -_frustumNear * inverseLength;
      _coeffTop[1] = _frustumTop * inverseLength;
    } else if (getProjectionMode() == ProjectionMode.Orthographic) {
      if (_frustumRight > _frustumLeft) {
        _coeffLeft[0] = -1;
        _coeffLeft[1] = 0;

        _coeffRight[0] = 1;
        _coeffRight[1] = 0;
      } else {
        _coeffLeft[0] = 1;
        _coeffLeft[1] = 0;

        _coeffRight[0] = -1;
        _coeffRight[1] = 0;
      }

      if (_frustumTop > _frustumBottom) {
        _coeffBottom[0] = -1;
        _coeffBottom[1] = 0;

        _coeffTop[0] = 1;
        _coeffTop[1] = 0;
      } else {
        _coeffBottom[0] = 1;
        _coeffBottom[1] = 0;

        _coeffTop[0] = -1;
        _coeffTop[1] = 0;
      }
    }

    _updatePMatrix = true;
    _updateMVPMatrix = true;
    _updateInverseMVPMatrix = true;

    markFrustumDirty();
  }

  public void markFrustumDirty() {
    _frustumDirty = true;
  }

  /**
   * Updates the values of the world planes associated with this camera.
   */
  public void onFrameChange() {
    final double dirDotLocation = _direction.dot(_location);

    final Vector3 planeNormal = Vector3.fetchTempInstance();

    // left plane
    planeNormal.setX(_left.getX() * _coeffLeft[0]);
    planeNormal.setY(_left.getY() * _coeffLeft[0]);
    planeNormal.setZ(_left.getZ() * _coeffLeft[0]);
    planeNormal.addLocal(_direction.getX() * _coeffLeft[1], _direction.getY() * _coeffLeft[1],
        _direction.getZ() * _coeffLeft[1]);
    _worldPlane[Camera.LEFT_PLANE].setNormal(planeNormal);
    _worldPlane[Camera.LEFT_PLANE].setConstant(_location.dot(planeNormal));

    // right plane
    planeNormal.setX(_left.getX() * _coeffRight[0]);
    planeNormal.setY(_left.getY() * _coeffRight[0]);
    planeNormal.setZ(_left.getZ() * _coeffRight[0]);
    planeNormal.addLocal(_direction.getX() * _coeffRight[1], _direction.getY() * _coeffRight[1],
        _direction.getZ() * _coeffRight[1]);
    _worldPlane[Camera.RIGHT_PLANE].setNormal(planeNormal);
    _worldPlane[Camera.RIGHT_PLANE].setConstant(_location.dot(planeNormal));

    // bottom plane
    planeNormal.setX(_up.getX() * _coeffBottom[0]);
    planeNormal.setY(_up.getY() * _coeffBottom[0]);
    planeNormal.setZ(_up.getZ() * _coeffBottom[0]);
    planeNormal.addLocal(_direction.getX() * _coeffBottom[1], _direction.getY() * _coeffBottom[1],
        _direction.getZ() * _coeffBottom[1]);
    _worldPlane[Camera.BOTTOM_PLANE].setNormal(planeNormal);
    _worldPlane[Camera.BOTTOM_PLANE].setConstant(_location.dot(planeNormal));

    // top plane
    planeNormal.setX(_up.getX() * _coeffTop[0]);
    planeNormal.setY(_up.getY() * _coeffTop[0]);
    planeNormal.setZ(_up.getZ() * _coeffTop[0]);
    planeNormal.addLocal(_direction.getX() * _coeffTop[1], _direction.getY() * _coeffTop[1],
        _direction.getZ() * _coeffTop[1]);
    _worldPlane[Camera.TOP_PLANE].setNormal(planeNormal);
    _worldPlane[Camera.TOP_PLANE].setConstant(_location.dot(planeNormal));

    if (getProjectionMode() == ProjectionMode.Orthographic) {
      if (_frustumRight > _frustumLeft) {
        _worldPlane[Camera.LEFT_PLANE].setConstant(_worldPlane[Camera.LEFT_PLANE].getConstant() + _frustumLeft);
        _worldPlane[Camera.RIGHT_PLANE].setConstant(_worldPlane[Camera.RIGHT_PLANE].getConstant() - _frustumRight);
      } else {
        _worldPlane[Camera.LEFT_PLANE].setConstant(_worldPlane[Camera.LEFT_PLANE].getConstant() - _frustumLeft);
        _worldPlane[Camera.RIGHT_PLANE].setConstant(_worldPlane[Camera.RIGHT_PLANE].getConstant() + _frustumRight);
      }

      if (_frustumBottom > _frustumTop) {
        _worldPlane[Camera.TOP_PLANE].setConstant(_worldPlane[Camera.TOP_PLANE].getConstant() + _frustumTop);
        _worldPlane[Camera.BOTTOM_PLANE].setConstant(_worldPlane[Camera.BOTTOM_PLANE].getConstant() - _frustumBottom);
      } else {
        _worldPlane[Camera.TOP_PLANE].setConstant(_worldPlane[Camera.TOP_PLANE].getConstant() - _frustumTop);
        _worldPlane[Camera.BOTTOM_PLANE].setConstant(_worldPlane[Camera.BOTTOM_PLANE].getConstant() + _frustumBottom);
      }
    }

    // far plane
    planeNormal.set(_direction).negateLocal();
    _worldPlane[Camera.FAR_PLANE].setNormal(planeNormal);
    _worldPlane[Camera.FAR_PLANE].setConstant(-(dirDotLocation + _frustumFar));

    // near plane
    _worldPlane[Camera.NEAR_PLANE].setNormal(_direction);
    _worldPlane[Camera.NEAR_PLANE].setConstant(dirDotLocation + _frustumNear);

    Vector3.releaseTempInstance(planeNormal);

    _updateMVMatrix = true;
    _updateMVPMatrix = true;
    _updateInverseMVPMatrix = true;

    markFrameDirty();
  }

  public void markFrameDirty() {
    _frameDirty = true;
  }

  /**
   * Updates the value of our projection matrix.
   */
  protected void updateProjectionMatrix() {
    if (getProjectionMode() == ProjectionMode.Orthographic) {
      _projection.setIdentity();
      _projection.setM00(2.0 / (_frustumRight - _frustumLeft));
      _projection.setM11(2.0 / (_frustumTop - _frustumBottom));
      _projection.setM22(-2.0 / (_frustumFar - _frustumNear));
      _projection.setM30(-(_frustumRight + _frustumLeft) / (_frustumRight - _frustumLeft));
      _projection.setM31(-(_frustumTop + _frustumBottom) / (_frustumTop - _frustumBottom));
      _projection.setM32(-(_frustumFar + _frustumNear) / (_frustumFar - _frustumNear));
    } else if (getProjectionMode() == ProjectionMode.Perspective) {
      _projection.setIdentity();
      _projection.setM00((2.0 * _frustumNear) / (_frustumRight - _frustumLeft));
      _projection.setM11((2.0 * _frustumNear) / (_frustumTop - _frustumBottom));
      _projection.setM20((_frustumRight + _frustumLeft) / (_frustumRight - _frustumLeft));
      _projection.setM21((_frustumTop + _frustumBottom) / (_frustumTop - _frustumBottom));
      _projection.setM22(-(_frustumFar + _frustumNear) / (_frustumFar - _frustumNear));
      _projection.setM23(-1.0);
      _projection.setM32(-(2.0 * _frustumFar * _frustumNear) / (_frustumFar - _frustumNear));
      _projection.setM33(-0.0);
    }

    _updatePMatrix = false;
  }

  public void setProjectionMatrix(final ReadOnlyMatrix4 projection) {
    _projection.set(projection);
    _frustumDirty = true;
  }

  /**
   * @return this camera's 4x4 projection matrix.
   */
  public ReadOnlyMatrix4 getProjectionMatrix() {
    checkProjection();

    return _projection;
  }

  /**
   * Updates the value of our view matrix.
   */
  protected void updateViewMatrix() {
    _view.setIdentity();
    _view.setM00(-_left.getX());
    _view.setM10(-_left.getY());
    _view.setM20(-_left.getZ());

    _view.setM01(_up.getX());
    _view.setM11(_up.getY());
    _view.setM21(_up.getZ());

    _view.setM02(-_direction.getX());
    _view.setM12(-_direction.getY());
    _view.setM22(-_direction.getZ());

    _view.setM30(_left.dot(_location));
    _view.setM31(-_up.dot(_location));
    _view.setM32(_direction.dot(_location));
  }

  /**
   * @return this camera's 4x4 view matrix.
   */
  public ReadOnlyMatrix4 getViewMatrix() {
    checkModelView();

    return _view;
  }

  /**
   * @return this camera's 4x4 model view X projection matrix.
   */
  public ReadOnlyMatrix4 getModelViewProjectionMatrix() {
    checkModelViewProjection();

    return _modelViewProjection;
  }

  /**
   * @return the inverse of this camera's 4x4 model view X projection matrix.
   */
  public ReadOnlyMatrix4 getModelViewProjectionInverseMatrix() {
    checkInverseModelViewProjection();

    return _modelViewProjectionInverse;
  }

  /**
   * Calculate a Pick Ray using the given screen position at the near plane of this camera and the
   * camera's position in space.
   *
   * @param screenPosition
   *          the x, y position on the near space to pass the ray through.
   * @param flipVertical
   *          if true, we'll flip the screenPosition on the y axis. This is useful when you are
   *          dealing with non-opengl coordinate systems.
   * @param store
   *          the Ray to store the result in. If false, a new Ray is created and returned.
   * @return the resulting Ray.
   */
  public Ray3 getPickRay(final ReadOnlyVector2 screenPosition, final boolean flipVertical, final Ray3 store) {
    final Vector2 pos = Vector2.fetchTempInstance().set(screenPosition);
    if (flipVertical) {
      pos.setY(getHeight() - screenPosition.getY());
    }

    Ray3 result = store;
    if (result == null) {
      result = new Ray3();
    }
    final Vector3 origin = Vector3.fetchTempInstance();
    final Vector3 direction = Vector3.fetchTempInstance();
    getWorldCoordinates(pos, 0, origin);
    getWorldCoordinates(pos, 0.3, direction).subtractLocal(origin).normalizeLocal();
    result.setOrigin(origin);
    result.setDirection(direction);
    Vector2.releaseTempInstance(pos);
    Vector3.releaseTempInstance(origin);
    Vector3.releaseTempInstance(direction);
    return result;
  }

  /**
   * Converts a local x,y screen position and depth value to world coordinates based on the current
   * settings of this camera.
   *
   * @param screenPosition
   *          the x,y coordinates of the screen position
   * @param zDepth
   *          the depth into the camera view to take our point. 0 indicates the near plane of the
   *          camera and 1 indicates the far plane.
   * @return a new vector containing the world coordinates.
   */
  public Vector3 getWorldCoordinates(final ReadOnlyVector2 screenPosition, final double zDepth) {
    return getWorldCoordinates(screenPosition, zDepth, null);
  }

  /**
   * Converts a local x,y screen position and depth value to world coordinates based on the current
   * settings of this camera.
   *
   * @param screenPosition
   *          the x,y coordinates of the screen position
   * @param zDepth
   *          the depth into the camera view to take our point. 0 indicates the near plane of the
   *          camera and 1 indicates the far plane.
   * @param store
   *          Use to avoid object creation. if not null, the results are stored in the given vector
   *          and returned. Otherwise, a new vector is created.
   * @return a vector containing the world coordinates.
   */
  public Vector3 getWorldCoordinates(final ReadOnlyVector2 screenPosition, final double zDepth, Vector3 store) {
    if (store == null) {
      store = new Vector3();
    }
    checkInverseModelViewProjection();
    final Vector4 position = Vector4.fetchTempInstance();
    position.set((screenPosition.getX() / getWidth() - _viewPortLeft) / (_viewPortRight - _viewPortLeft) * 2 - 1,
        (screenPosition.getY() / getHeight() - _viewPortBottom) / (_viewPortTop - _viewPortBottom) * 2 - 1,
        zDepth * 2 - 1, 1);
    _modelViewProjectionInverse.applyPre(position, position);
    position.multiplyLocal(1.0 / position.getW());
    store.setX(position.getX());
    store.setY(position.getY());
    store.setZ(position.getZ());

    Vector4.releaseTempInstance(position);
    return store;
  }

  /**
   * Converts a position in world coordinate space to an x,y screen position and depth value using the
   * current settings of this camera.
   *
   * @param worldPos
   *          the position in space to retrieve screen coordinates for.
   * @return a new vector containing the screen coordinates as x and y and the distance as a percent
   *         between near and far planes.
   */
  public Vector3 getScreenCoordinates(final ReadOnlyVector3 worldPos) {
    return getScreenCoordinates(worldPos, null);
  }

  /**
   * Converts a position in world coordinate space to an x,y screen position and depth value using the
   * current settings of this camera.
   *
   * @param worldPos
   *          the position in space to retrieve screen coordinates for.
   * @param store
   *          Use to avoid object creation. if not null, the results are stored in the given vector
   *          and returned. Otherwise, a new vector is created.
   * @return a vector containing the screen coordinates as x and y and the distance as a percent
   *         between near and far planes.
   */
  public Vector3 getScreenCoordinates(final ReadOnlyVector3 worldPosition, Vector3 store) {
    store = getNormalizedDeviceCoordinates(worldPosition, store);

    store.setX(((store.getX() + 1) * (_viewPortRight - _viewPortLeft) / 2) * getWidth());
    store.setY(((store.getY() + 1) * (_viewPortTop - _viewPortBottom) / 2) * getHeight());
    store.setZ((store.getZ() + 1) / 2);

    return store;
  }

  /**
   * Converts a position in world coordinate space to a x,y,z frustum position using the current
   * settings of this camera.
   *
   * @param worldPos
   *          the position in space to retrieve frustum coordinates for.
   * @return a new vector containing the x,y,z frustum position
   */
  public Vector3 getFrustumCoordinates(final ReadOnlyVector3 worldPos) {
    return getFrustumCoordinates(worldPos, null);
  }

  /**
   * Converts a position in world coordinate space to a x,y,z frustum position using the current
   * settings of this camera.
   *
   * @param worldPos
   *          the position in space to retrieve frustum coordinates for.
   * @param store
   *          Use to avoid object creation. if not null, the results are stored in the given vector
   *          and returned. Otherwise, a new vector is created.
   * @return a vector containing the x,y,z frustum position
   */
  public Vector3 getFrustumCoordinates(final ReadOnlyVector3 worldPosition, Vector3 store) {
    store = getNormalizedDeviceCoordinates(worldPosition, store);

    store.setX(((store.getX() + 1) * (_frustumRight - _frustumLeft) / 2) + _frustumLeft);
    store.setY(((store.getY() + 1) * (_frustumTop - _frustumBottom) / 2) + _frustumBottom);
    store.setZ(((store.getZ() + 1) * (_frustumFar - _frustumNear) / 2) + _frustumNear);

    return store;
  }

  public Vector3 getNormalizedDeviceCoordinates(final ReadOnlyVector3 worldPos) {
    return getNormalizedDeviceCoordinates(worldPos, null);
  }

  public Vector3 getNormalizedDeviceCoordinates(final ReadOnlyVector3 worldPosition, Vector3 store) {
    if (store == null) {
      store = new Vector3();
    }
    checkModelViewProjection();
    final Vector4 position = Vector4.fetchTempInstance();
    position.set(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 1);
    _modelViewProjection.applyPre(position, position);
    position.multiplyLocal(1.0 / position.getW());
    store.setX(position.getX());
    store.setY(position.getY());
    store.setZ(position.getZ());
    Vector4.releaseTempInstance(position);

    return store;
  }

  /**
   * update modelView if necessary.
   */
  private void checkModelView() {
    if (_updateMVMatrix) {
      updateViewMatrix();
      _updateMVMatrix = false;
    }
  }

  /**
   * update projection if necessary.
   */
  private void checkProjection() {
    if (_updatePMatrix) {
      updateProjectionMatrix();
      _updatePMatrix = false;
    }
  }

  /**
   * update modelViewProjection if necessary.
   */
  private void checkModelViewProjection() {
    if (_updateMVPMatrix) {
      _modelViewProjection.set(getViewMatrix()).multiplyLocal(getProjectionMatrix());
      _updateMVPMatrix = false;
    }
  }

  /**
   * update inverse modelViewProjection if necessary.
   */
  private void checkInverseModelViewProjection() {
    if (_updateInverseMVPMatrix) {
      checkModelViewProjection();
      _modelViewProjection.invert(_modelViewProjectionInverse);
      _updateInverseMVPMatrix = false;
    }
  }

  /**
   * @return the height of the display.
   */
  public int getHeight() { return _height; }

  public ProjectionMode getProjectionMode() { return _projectionMode; }

  public void setProjectionMode(final ProjectionMode projectionMode) { _projectionMode = projectionMode; }

  /**
   * @return the width of the display.
   */
  public int getWidth() { return _width; }

  /**
   * Apply this camera's values to the given Renderer. Only values determined to be dirty (via
   * updates, setters, etc.) will be applied.
   *
   * @param renderer
   *          the Renderer to use.
   */
  public void apply(final Renderer renderer) {
    if (Camera.getCurrentCamera() != this) {
      _frustumDirty = _viewPortDirty = _frameDirty = true;
      ContextManager.getCurrentContext().setCurrentCamera(this);
    }
    if (_depthRangeDirty) {
      renderer.setDepthRange(_depthRangeNear, _depthRangeFar);
      _depthRangeDirty = false;
    }
    if (_frustumDirty) {
      applyProjectionMatrix(renderer);
      _frustumDirty = false;
    }
    if (_viewPortDirty) {
      applyViewport(renderer);
      _viewPortDirty = false;
    }
    if (_frameDirty) {
      applyViewMatrix(renderer);
      _frameDirty = false;
    }
  }

  /**
   * Mark view port dirty.
   */
  protected void onViewPortChange() {
    markViewPortDirty();
  }

  public void markViewPortDirty() {
    _viewPortDirty = true;
  }

  /**
   * Apply the camera's projection matrix using the given Renderer.
   *
   * @param renderer
   *          the Renderer to use.
   */
  protected void applyProjectionMatrix(final Renderer renderer) {
    _matrixBuffer.rewind();
    getProjectionMatrix().toFloatBuffer(_matrixBuffer);
    _matrixBuffer.rewind();
    renderer.setMatrix(RenderMatrixType.Projection, _matrixBuffer);
  }

  /**
   * Apply the camera's viewport using the given Renderer.
   *
   * @param renderer
   *          the Renderer to use.
   */
  protected void applyViewport(final Renderer renderer) {
    final int x = getViewportOffsetX();
    final int y = getViewportOffsetY();
    final int w = getViewportWidth();
    final int h = getViewportHeight();
    renderer.setViewport(x, y, w, h);
  }

  public int getViewportOffsetX() { return (int) (_viewPortLeft * _width); }

  public int getViewportOffsetY() { return (int) (_viewPortBottom * _height); }

  public int getViewportWidth() { return (int) ((_viewPortRight - _viewPortLeft) * _width); }

  public int getViewportHeight() { return (int) ((_viewPortTop - _viewPortBottom) * _height); }

  /**
   * Apply the camera's view matrix using the given Renderer.
   *
   * @param renderer
   *          the Renderer to use.
   */
  protected void applyViewMatrix(final Renderer renderer) {
    _matrixBuffer.rewind();
    getViewMatrix().toFloatBuffer(_matrixBuffer);
    _matrixBuffer.rewind();
    renderer.setMatrix(RenderMatrixType.View, _matrixBuffer);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_location, "location", new Vector3(Vector3.ZERO));
    capsule.write(_left, "left", new Vector3(Vector3.UNIT_X));
    capsule.write(_up, "up", new Vector3(Vector3.UNIT_Y));
    capsule.write(_direction, "direction", new Vector3(Vector3.UNIT_Z));
    capsule.write(_frustumNear, "frustumNear", 1);
    capsule.write(_frustumFar, "frustumFar", 2);
    capsule.write(_frustumLeft, "frustumLeft", -0.5);
    capsule.write(_frustumRight, "frustumRight", 0.5);
    capsule.write(_frustumTop, "frustumTop", 0.5);
    capsule.write(_frustumBottom, "frustumBottom", -0.5);
    capsule.write(_coeffLeft, "coeffLeft", new double[2]);
    capsule.write(_coeffRight, "coeffRight", new double[2]);
    capsule.write(_coeffBottom, "coeffBottom", new double[2]);
    capsule.write(_coeffTop, "coeffTop", new double[2]);
    capsule.write(_planeQuantity, "planeQuantity", 6);
    capsule.write(_viewPortLeft, "viewPortLeft", 0);
    capsule.write(_viewPortRight, "viewPortRight", 1);
    capsule.write(_viewPortTop, "viewPortTop", 1);
    capsule.write(_viewPortBottom, "viewPortBottom", 0);
    capsule.write(_width, "width", 0);
    capsule.write(_height, "height", 0);
    capsule.write(_depthRangeNear, "depthRangeNear", 0.0);
    capsule.write(_depthRangeFar, "depthRangeFar", 1.0);
    capsule.write(_projectionMode, "projectionMode", ProjectionMode.Perspective);
    if (_layers.size() > 0) {
      final int[] layers = new int[_layers.size()];
      int i = 0;
      for (final int n : _layers) {
        layers[i++] = n;
      }
      capsule.write(layers, "layers", new int[0]);
    }
    capsule.write(_exclusiveLayers, "exclusiveLayers", true);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _location.set(capsule.readSavable("location", (Vector3) Vector3.ZERO));
    _left.set(capsule.readSavable("left", (Vector3) Vector3.UNIT_X));
    _up.set(capsule.readSavable("up", (Vector3) Vector3.UNIT_Y));
    _direction.set(capsule.readSavable("direction", (Vector3) Vector3.UNIT_Z));
    _frustumNear = capsule.readDouble("frustumNear", 1);
    _frustumFar = capsule.readDouble("frustumFar", 2);
    _frustumLeft = capsule.readDouble("frustumLeft", -0.5);
    _frustumRight = capsule.readDouble("frustumRight", 0.5);
    _frustumTop = capsule.readDouble("frustumTop", 0.5);
    _frustumBottom = capsule.readDouble("frustumBottom", -0.5);
    _coeffLeft = capsule.readDoubleArray("coeffLeft", new double[2]);
    _coeffRight = capsule.readDoubleArray("coeffRight", new double[2]);
    _coeffBottom = capsule.readDoubleArray("coeffBottom", new double[2]);
    _coeffTop = capsule.readDoubleArray("coeffTop", new double[2]);
    _planeQuantity = capsule.readInt("planeQuantity", 6);
    _viewPortLeft = capsule.readDouble("viewPortLeft", 0);
    _viewPortRight = capsule.readDouble("viewPortRight", 1);
    _viewPortTop = capsule.readDouble("viewPortTop", 1);
    _viewPortBottom = capsule.readDouble("viewPortBottom", 0);
    _width = capsule.readInt("width", 0);
    _height = capsule.readInt("height", 0);
    _depthRangeNear = capsule.readDouble("depthRangeNear", 0.0);
    _depthRangeFar = capsule.readDouble("depthRangeFar", 1.0);
    _projectionMode = capsule.readEnum("projectionMode", ProjectionMode.class, ProjectionMode.Perspective);
    _layers.clear();
    final int[] layers = capsule.readIntArray("layers", new int[0]);
    for (int i = 0; i < layers.length; i++) {
      _layers.add(layers[i]);
    }
    _exclusiveLayers = capsule.readBoolean("exclusiveLayers", true);
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    _location.set((Vector3) in.readObject());
    _left.set((Vector3) in.readObject());
    _up.set((Vector3) in.readObject());
    _direction.set((Vector3) in.readObject());
    _frustumNear = in.readDouble();
    _frustumFar = in.readDouble();
    _frustumLeft = in.readDouble();
    _frustumRight = in.readDouble();
    _frustumTop = in.readDouble();
    _frustumBottom = in.readDouble();
    _coeffLeft = (double[]) in.readObject();
    _coeffRight = (double[]) in.readObject();
    _coeffBottom = (double[]) in.readObject();
    _coeffTop = (double[]) in.readObject();
    _planeQuantity = in.readInt();
    _viewPortLeft = in.readDouble();
    _viewPortRight = in.readDouble();
    _viewPortTop = in.readDouble();
    _viewPortBottom = in.readDouble();
    _width = in.readInt();
    _height = in.readInt();
    _depthRangeNear = in.readDouble();
    _depthRangeFar = in.readDouble();
    _projectionMode = ProjectionMode.valueOf(in.readUTF());
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeObject(_location);
    out.writeObject(_left);
    out.writeObject(_up);
    out.writeObject(_direction);
    out.writeDouble(_frustumNear);
    out.writeDouble(_frustumFar);
    out.writeDouble(_frustumLeft);
    out.writeDouble(_frustumRight);
    out.writeDouble(_frustumTop);
    out.writeDouble(_frustumBottom);
    out.writeObject(_coeffLeft);
    out.writeObject(_coeffRight);
    out.writeObject(_coeffBottom);
    out.writeObject(_coeffTop);
    out.writeInt(_planeQuantity);
    out.writeDouble(_viewPortLeft);
    out.writeDouble(_viewPortRight);
    out.writeDouble(_viewPortTop);
    out.writeDouble(_viewPortBottom);
    out.writeInt(_width);
    out.writeInt(_height);
    out.writeDouble(_depthRangeNear);
    out.writeDouble(_depthRangeFar);
    out.writeUTF(_projectionMode.name());
  }

  @Override
  public String toString() {
    return "com.ardor3d.renderer.Camera: loc - " + Arrays.toString(getLocation().toArray(null)) + " dir - "
        + Arrays.toString(getDirection().toArray(null)) + " up - " + Arrays.toString(getUp().toArray(null)) + " left - "
        + Arrays.toString(getLeft().toArray(null));
  }

  @Override
  public Class<? extends Camera> getClassTag() { return getClass(); }

  /**
   * Convenience method for retrieving the Camera set on the current RenderContext. Similar to
   * ContextManager.getCurrentContext().getCurrentCamera() but with null checks for current context.
   *
   * @return the Camera on the current RenderContext.
   */
  public static Camera getCurrentCamera() {
    RenderContext context = ContextManager.getCurrentContext();
    if (context == null) {
      return null;
    }
    return context.getCurrentCamera();
  }

  public boolean isFrameDirty() { return _frameDirty; }

  public static Camera newOrthoCamera(final Canvas canvas) {
    final int width = canvas.getContentWidth();
    final int height = canvas.getContentHeight();

    final Camera camera = new Camera(width, height);
    camera.setFrustum(-1, 1, 0, width, height, 0);
    camera.setProjectionMode(ProjectionMode.Orthographic);

    final Vector3 loc = new Vector3(0.0f, 0.0f, 0.0f);
    final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
    final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
    final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
    /** Move our camera to a correct place and orientation. */
    camera.setFrame(loc, left, up, dir);

    canvas.addListener((newWidth, newHeight) -> {
      camera.resize(newWidth, newHeight);
      camera.setFrustumRight(newWidth);
      camera.setFrustumTop(newHeight);
    });
    return camera;
  }
}
