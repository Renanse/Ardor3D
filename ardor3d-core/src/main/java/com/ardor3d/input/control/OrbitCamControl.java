/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.control;

import java.util.function.Predicate;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.gesture.event.PinchGestureEvent;
import com.ardor3d.input.logical.GestureEventCondition;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.MouseWheelMovedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TriggerConditions;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Spatial;

/**
 * <p>
 * Orbital type Camera controller. Basically, this class references a camera and provides methods
 * for moving that camera around a target position or spatial using spherical polar coordinates.
 * </p>
 * <p>
 * To use, create a new instance of OrbitCamController. Update the controller in your update loop.
 * </p>
 * <p>
 * Example: Creates a new control, adds mouse triggers to a Logical Layer, and sets the default
 * location (15 units away, 0 degrees ascent, 0 degrees azimuth).
 * </p>
 *
 * <pre>
 * // ... in init
 * control = new OrbitCamControl(myCamera, targetLocation);
 * control.setupMouseTriggers(myLogicalLayer, true);
 * control.setSphereCoords(15, 0, 0);
 *
 * // ...in update loop
 * control.update(timer.getTimePerFrame());
 * </pre>
 */
public class OrbitCamControl {

  /**
   * Our absolute min/max ascent (pitch) angle, in radians. This is set at 89.95 degrees to prevent
   * the camera's direction from becoming parallel to the world up vector.
   */
  public static final double ABSOLUTE_MAXASCENT = 89.95 * MathUtils.DEG_TO_RAD;

  /**
   * The camera we are modifying.
   */
  protected Camera _camera;
  protected Vector3 _worldUpVec = new Vector3(Vector3.UNIT_Y);

  protected Vector3 _sphereCoords = new Vector3();
  protected Vector3 _camPosition = new Vector3();

  protected Vector3 _lookAtPoint = new Vector3();
  protected Spatial _lookAtSpatial = null;
  protected TargetType _targetType;

  protected boolean _invertedX = false;
  protected boolean _invertedY = false;
  protected boolean _invertedWheel = true;
  protected boolean _updateCameraFromInput = false;

  protected double _zoomSpeed = 0.01;
  protected double _baseDistance = 15;
  protected double _minZoomDistance = 1;
  protected double _maxZoomDistance = 100;

  protected double _minAscent = -ABSOLUTE_MAXASCENT;
  protected double _maxAscent = ABSOLUTE_MAXASCENT;
  protected double _xSpeed = 0.01;
  protected double _ySpeed = 0.01;

  protected boolean _dirty = true;

  protected InputTrigger _mouseTrigger;

  public enum TargetType {
    Point, Spatial
  }

  /**
   * Construct a new orbit controller
   *
   * @param cam
   *          the camera to control
   * @param target
   *          a world location to lock our sights on.
   */
  public OrbitCamControl(final Camera cam, final ReadOnlyVector3 target) {
    _camera = cam;
    _targetType = TargetType.Point;
    _lookAtPoint.set(target);
    _updateCameraFromInput = _camera == null;
  }

  /**
   * Construct a new orbit controller. The camera controlled will be based on the source canvas the
   * input is triggered from.
   *
   * @param target
   *          a world location to lock our sights on.
   */
  public OrbitCamControl(final ReadOnlyVector3 target) {
    this(null, target);
  }

  /**
   * Construct a new orbit controller
   *
   * @param cam
   *          the camera to control
   * @param target
   *          a spatial whose world location we'll lock our sights on.
   */
  public OrbitCamControl(final Camera cam, final Spatial target) {
    _camera = cam;
    _targetType = TargetType.Spatial;
    _lookAtSpatial = target;
    _updateCameraFromInput = _camera == null;
  }

  /**
   * Construct a new orbit controller. The camera controlled will be based on the source canvas the
   * input is triggered
   *
   * @param target
   *          a spatial whose world location we'll lock our sights on.
   */
  public OrbitCamControl(final Spatial target) {
    this(null, target);
  }

  public Camera getCamera() { return _camera; }

  public void setCamera(final Camera camera) {
    _camera = camera;
    _updateCameraFromInput = _camera == null;
  }

  public ReadOnlyVector3 getWorldUpVec() { return _worldUpVec; }

  public void setWorldUpVec(final ReadOnlyVector3 worldUpVec) {
    _worldUpVec.set(worldUpVec);
    _dirty = true;
  }

  public void setInvertedWheel(final boolean invertedWheel) { _invertedWheel = invertedWheel; }

  public boolean isInvertedWheel() { return _invertedWheel; }

  public void setInvertedX(final boolean invertedX) { _invertedX = invertedX; }

  public boolean isInvertedX() { return _invertedX; }

  public void setInvertedY(final boolean invertedY) { _invertedY = invertedY; }

  public boolean isInvertedY() { return _invertedY; }

  public Vector3 getLookAtPoint() { return _lookAtPoint; }

  /**
   * Sets a specific world location for the camera to point at and circle around.
   *
   * @param point
   */
  public void setLookAtPoint(final Vector3 point) {
    _dirty = !point.equals(_lookAtPoint);
    _lookAtPoint = point;
    _targetType = TargetType.Point;
  }

  public Spatial getLookAtSpatial() { return _lookAtSpatial; }

  /**
   * Sets a spatial to look at. We'll use the world transform of the spatial, so its transform needs
   * to be up to date.
   *
   * @param spatial
   */
  public void setLookAtSpatial(final Spatial spatial) {
    _dirty = spatial != _lookAtSpatial; // identity equality
    _lookAtSpatial = spatial;
    _targetType = TargetType.Spatial;
  }

  public TargetType getTargetType() { return _targetType; }

  public double getZoomSpeed() { return _zoomSpeed; }

  public void setZoomSpeed(final double zoomSpeed) { _zoomSpeed = zoomSpeed; }

  public double getBaseDistance() { return _baseDistance; }

  public void setBaseDistance(final double baseDistance) {
    _baseDistance = baseDistance;
    zoom(0);
  }

  public double getMaxAscent() { return _maxAscent; }

  public void setMaxAscent(final double maxAscent) {
    _maxAscent = Math.min(maxAscent, ABSOLUTE_MAXASCENT);
    move(0, 0);
  }

  public double getMinAscent() { return _minAscent; }

  public void setMinAscent(final double minAscent) {
    _minAscent = Math.max(minAscent, -ABSOLUTE_MAXASCENT);
    move(0, 0);
  }

  public double getMaxZoomDistance() { return _maxZoomDistance; }

  public void setMaxZoomDistance(final double maxZoomDistance) {
    _maxZoomDistance = maxZoomDistance;
    zoom(0);
  }

  public double getMinZoomDistance() { return _minZoomDistance; }

  public void setMinZoomDistance(final double minZoomDistance) {
    _minZoomDistance = minZoomDistance;
    zoom(0);
  }

  public double getXSpeed() { return _xSpeed; }

  public void setXSpeed(final double speed) { _xSpeed = speed; }

  public double getYSpeed() { return _ySpeed; }

  public void setYSpeed(final double speed) { _ySpeed = speed; }

  public void setSphereCoords(final ReadOnlyVector3 sphereCoords) {
    _sphereCoords.set(sphereCoords);
    makeDirty();
  }

  public void setSphereCoords(final double x, final double y, final double z) {
    _sphereCoords.set(x, y, z);
    makeDirty();
  }

  public boolean isUpdateCameraFromInput() { return _updateCameraFromInput; }

  public void setUpdateCameraFromInput(final boolean updateCameraFromInput) {
    _updateCameraFromInput = updateCameraFromInput;
    makeDirty();
  }

  protected void updateTargetPos() {
    if (_targetType == TargetType.Spatial) {
      final double x = _lookAtPoint.getX();
      final double y = _lookAtPoint.getY();
      final double z = _lookAtPoint.getZ();
      _lookAtSpatial.getWorldTransform().applyForward(Vector3.ZERO, _lookAtPoint);
      if (x != _lookAtPoint.getX() || y != _lookAtPoint.getY() || z != _lookAtPoint.getZ()) {
        makeDirty();
      }
    }
  }

  public void makeDirty() {
    _dirty = true;
  }

  /**
   * Zoom camera in/out from the target point.
   *
   * @param percent
   *          a value applied to the baseDistance to determine how far in/out to zoom. Inverted if
   *          {@link #isInvertedWheel()} is true.
   */
  public void zoom(final double percent) {
    final double amount = (_invertedWheel ? -1 : 1) * percent * _baseDistance;
    _sphereCoords.setX(MathUtils.clamp(_sphereCoords.getX() + amount, _minZoomDistance, _maxZoomDistance));
    makeDirty();
  }

  /**
   *
   * @param xDif
   *          a value applied to the azimuth value of our spherical coordinates. Inverted if
   *          {@link #isInvertedX()} is true.
   * @param yDif
   *          a value applied to the theta value of our spherical coordinates. Inverted if
   *          {@link #isInvertedY()} is true.
   */
  public void move(final double xDif, final double yDif) {
    final double azimuthAccel = _invertedX ? -xDif : xDif;
    final double thetaAccel = _invertedY ? -yDif : yDif;

    // update our master spherical coords, using x and y movement
    _sphereCoords.setY(MathUtils.moduloPositive(_sphereCoords.getY() - azimuthAccel, MathUtils.TWO_PI));
    _sphereCoords.setZ(MathUtils.clamp(_sphereCoords.getZ() + thetaAccel, _minAscent, _maxAscent));
    makeDirty();
  }

  /**
   * Update the position of the Camera controlled by this object.
   *
   * @param time
   *          a delta time, in seconds. Not used currently, but might be useful for doing "ease-in" of
   *          camera movements.
   */
  public void update(final double time) {
    updateTargetPos();

    if (!_dirty) {
      return;
    }
    if (_worldUpVec.getY() == 1) {
      MathUtils.sphericalToCartesian(_sphereCoords, _camPosition);
    } else if (_worldUpVec.getZ() == 1) {
      MathUtils.sphericalToCartesianZ(_sphereCoords, _camPosition);
    }

    if (_camera != null) {
      _camera.setLocation(_camPosition.addLocal(_lookAtPoint));
      _camera.lookAt(_lookAtPoint, _worldUpVec);
    }
    _dirty = false;
  }

  public void setupMouseTriggers(final LogicalLayer layer, final boolean dragOnly) {
    // Mouse look
    final Predicate<TwoInputStates> someMouseDown = TriggerConditions.leftButtonDown()
        .or(TriggerConditions.rightButtonDown()).or(TriggerConditions.middleButtonDown());
    final Predicate<TwoInputStates> scrollWheelMoved = new MouseWheelMovedCondition();
    final Predicate<TwoInputStates> dragged = TriggerConditions.mouseMoved().and(someMouseDown);
    final TriggerAction mouseAction = new TriggerAction() {

      // Test boolean to allow us to ignore first mouse event. First event can wildly vary based on
      // platform.
      private boolean firstPing = true;

      @Override
      public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
        final MouseState mouse = inputStates.getCurrent().getMouseState();
        if (mouse.getDx() != 0 || mouse.getDy() != 0) {
          if (!firstPing) {
            move(_xSpeed * mouse.getDx(), _ySpeed * mouse.getDy());
          } else {
            firstPing = false;
          }
        }

        if (mouse.getDwheel() != 0) {
          zoom(_zoomSpeed * mouse.getDwheel());
        }

        if (_updateCameraFromInput) {
          _camera = source.getCanvasRenderer().getCamera();
        }
      }
    };

    final Predicate<TwoInputStates> predicate =
        scrollWheelMoved.or(dragOnly ? dragged : TriggerConditions.mouseMoved());
    _mouseTrigger = new InputTrigger(predicate, mouseAction);
    layer.registerTrigger(_mouseTrigger);
  }

  public void setupGestureTriggers(final LogicalLayer layer) {
    // pinch - zoom
    layer.registerTrigger(new InputTrigger(new GestureEventCondition(PinchGestureEvent.class), new TriggerAction() {
      double initialZoom = 1.0;

      @Override
      public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
        final PinchGestureEvent event = inputStates.getCurrent().getGestureState().first(PinchGestureEvent.class);

        if (event.isStartOfGesture()) {
          initialZoom = _sphereCoords.getX();
        }
        if (event.getScale() != 0.0) {
          _sphereCoords
              .setX(MathUtils.clamp((1.0 / event.getScale()) * initialZoom, _minZoomDistance, _maxZoomDistance));
          makeDirty();
        }

        if (_updateCameraFromInput) {
          _camera = source.getCanvasRenderer().getCamera();
        }
      }
    }));
  }
}
