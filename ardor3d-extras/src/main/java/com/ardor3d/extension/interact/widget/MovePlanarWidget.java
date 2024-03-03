/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.MouseCursor;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.ZBufferState.TestFunction;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.shape.Cylinder;
import com.ardor3d.util.MaterialUtil;

public class MovePlanarWidget extends AbstractInteractWidget {

  public static double DEFAULT_SCALE = 1.0;
  public static double MOUSEOVER_SCALE = 1.1;

  protected MovePlane _plane = MovePlane.XZ;

  public static MouseCursor DEFAULT_CURSOR = null;

  public enum MovePlane {
    XY, XZ, YZ
  }

  public MovePlanarWidget() {
    _handle = new Node("moveHandle");
    LightProperties.setLightReceiver(_handle, false);

    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    _handle.setRenderState(blend);

    final ZBufferState zstate = new ZBufferState();
    zstate.setFunction(TestFunction.LessThanOrEqualTo);
    _handle.setRenderState(zstate);

    _handle.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
    _handle.updateGeometricState(0);

    if (MovePlanarWidget.DEFAULT_CURSOR != null) {
      setMouseOverCallback(new SetCursorCallback(MovePlanarWidget.DEFAULT_CURSOR));
    }
  }

  public MovePlanarWidget withDefaultHandle(final double radius, final double height, final ReadOnlyColorRGBA color) {
    final Cylinder handle = new Cylinder("handle", 2, 16, radius, height, true);
    handle.setDefaultColor(color);
    switch (_plane) {
      case XZ:
        handle.setRotation(new Matrix3().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.UNIT_X));
        break;
      case YZ:
        handle.setRotation(new Matrix3().fromAngleNormalAxis(MathUtils.HALF_PI, Vector3.UNIT_Y));
        break;
      default:
        // do nothing
        break;
    }
    handle.updateModelBound();
    MaterialUtil.autoMaterials(handle);
    withHandle(handle);
    return this;
  }

  public MovePlanarWidget withPlane(final MovePlane plane) {
    _plane = plane;
    return this;
  }

  public MovePlanarWidget withHandle(final Spatial handle) {
    _handle.attachChild(handle);
    return this;
  }

  @Override
  public void targetDataUpdated(final InteractManager manager) {
    final Spatial target = manager.getSpatialTarget();
    if (target == null) {
      _handle.setRotation(Matrix3.IDENTITY);
    } else {
      target.updateGeometricState(0);

      // update arrow rotations from target
      if (_interactMatrix == InteractMatrix.Local) {
        _handle.setRotation(target.getWorldRotation());
      } else {
        _handle.setRotation(Matrix3.IDENTITY);
      }
    }

    _handle.setScale(calculateHandleScale(manager));
  }

  @Override
  protected double calculateHandleScale(final InteractManager manager) {
    return super.calculateHandleScale(manager)
        * (_mouseOver ? MovePlanarWidget.MOUSEOVER_SCALE : MovePlanarWidget.DEFAULT_SCALE);
  }

  @Override
  public void render(final Renderer renderer, final InteractManager manager) {
    final Spatial spat = manager.getSpatialTarget();
    if (spat == null) {
      return;
    }

    _handle.setTranslation(spat.getWorldTranslation());
    _handle.updateGeometricState(0);

    renderer.draw(_handle);
  }

  @Override
  public void processInput(final Canvas source, final TwoInputStates inputStates, final AtomicBoolean inputConsumed,
      final InteractManager manager) {

    final Camera camera = source.getCanvasRenderer().getCamera();
    final MouseState current = inputStates.getCurrent().getMouseState();
    final MouseState previous = inputStates.getPrevious().getMouseState();

    // first process mouse over state
    checkMouseOver(source, current, manager);

    // Now check drag status
    if (!checkShouldDrag(camera, current, previous, inputConsumed, manager)) {
      return;
    }

    // act on drag
    final Vector2 oldMouse = new Vector2(previous.getX(), previous.getY());
    final Vector3 loc = getNewOffset(oldMouse, current, camera, manager);
    final Transform transform = manager.getSpatialState().getTransform();
    transform.setTranslation(loc.addLocal(transform.getTranslation()));

    // apply our filters, if any, now that we've made updates.
    applyFilters(manager);
  }

  protected Vector3 getNewOffset(final Vector2 oldMouse, final MouseState current, final Camera camera,
      final InteractManager manager) {

    // calculate a plane
    _calcVec3A.set(_handle.getWorldTranslation());
    switch (_plane) {
      case XY:
        _calcVec3B.set(Vector3.UNIT_X);
        _calcVec3C.set(Vector3.UNIT_Y);
        break;
      case XZ:
        _calcVec3B.set(Vector3.UNIT_X);
        _calcVec3C.set(Vector3.UNIT_Z);
        break;
      case YZ:
        _calcVec3B.set(Vector3.UNIT_Y);
        _calcVec3C.set(Vector3.UNIT_Z);
        break;
    }

    // rotate to arrow plane
    _handle.getRotation().applyPost(_calcVec3B, _calcVec3B);
    _handle.getRotation().applyPost(_calcVec3C, _calcVec3C);

    // make plane object
    final Plane pickPlane =
        new Plane().setPlanePoints(_calcVec3A, _calcVec3B.addLocal(_calcVec3A), _calcVec3C.addLocal(_calcVec3A));

    // find out where we were hitting the plane before
    getPickRay(oldMouse, camera);
    if (!_calcRay.intersectsPlane(pickPlane, _calcVec3A)) {
      return _calcVec3A.zero();
    }

    // find out where we are hitting the plane now
    getPickRay(new Vector2(current.getX(), current.getY()), camera);
    if (!_calcRay.intersectsPlane(pickPlane, _calcVec3B)) {
      return _calcVec3A.zero();
    }

    // convert to target coord space
    final Node parent = manager.getSpatialTarget().getParent();
    if (parent != null) {
      parent.getWorldTransform().applyInverse(_calcVec3A);
      parent.getWorldTransform().applyInverse(_calcVec3B);
    }

    return _calcVec3B.subtractLocal(_calcVec3A);
  }
}
