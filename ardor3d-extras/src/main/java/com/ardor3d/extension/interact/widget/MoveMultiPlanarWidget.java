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

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.MouseCursor;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PrimitiveKey;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.ZBufferState.TestFunction;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.MaterialUtil;

public class MoveMultiPlanarWidget extends AbstractInteractWidget {

  public static double DEFAULT_SCALE = 1.0;
  public static double MOUSEOVER_SCALE = 1.1;

  public static MouseCursor DEFAULT_CURSOR = null;

  public MoveMultiPlanarWidget() {
    this(0.5);
  }

  public MoveMultiPlanarWidget(final double extent) {
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

    createDefaultHandle(extent);

    if (MoveMultiPlanarWidget.DEFAULT_CURSOR != null) {
      setMouseOverCallback(new SetCursorCallback(MoveMultiPlanarWidget.DEFAULT_CURSOR));
    }
  }

  protected void createDefaultHandle(final double extent) {
    final Box grip = new Box("grip", Vector3.ZERO, extent, extent, extent);
    grip.setSolidColor(ColorRGBA.WHITE);
    grip.updateModelBound();
    _handle.attachChild(grip);
    MaterialUtil.autoMaterials(grip);

    // setup some colors, just at the corner of the primitives since we will use flat shading.
    final FloatBuffer colors = grip.getMeshData().getColorBuffer();
    BufferUtils.setInBuffer(ColorRGBA.MAGENTA, colors, 0);
    BufferUtils.setInBuffer(ColorRGBA.CYAN, colors, 4);
    BufferUtils.setInBuffer(ColorRGBA.MAGENTA, colors, 8);
    BufferUtils.setInBuffer(ColorRGBA.CYAN, colors, 12);
    BufferUtils.setInBuffer(ColorRGBA.YELLOW, colors, 16);
    BufferUtils.setInBuffer(ColorRGBA.YELLOW, colors, 20);
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
        * (_mouseOver ? MoveMultiPlanarWidget.MOUSEOVER_SCALE : MoveMultiPlanarWidget.DEFAULT_SCALE);
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
    final PickData pickData = _results.getNumber() > 0 ? _results.getPickData(0) : null;
    if (pickData != null && _lastDragSpatial instanceof Mesh
        && pickData.getIntersectionRecord().getNumberOfIntersections() > 0) {
      final PrimitiveKey key = pickData.getIntersectionRecord().getIntersectionPrimitive(0);
      ((Mesh) _lastDragSpatial).getMeshData().getPrimitiveVertices(key.getPrimitiveIndex(), key.getSection(),
          new Vector3[] {_calcVec3A, _calcVec3B, _calcVec3C});
      _lastDragSpatial.localToWorld(_calcVec3A, _calcVec3A);
      _lastDragSpatial.localToWorld(_calcVec3B, _calcVec3B);
      _lastDragSpatial.localToWorld(_calcVec3C, _calcVec3C);
      final Vector2 oldMouse = new Vector2(previous.getX(), previous.getY());
      final Vector3 loc = getNewOffset(oldMouse, current, camera, manager);
      final Transform transform = manager.getSpatialState().getTransform();
      transform.setTranslation(loc.addLocal(transform.getTranslation()));

      // apply our filters, if any, now that we've made updates.
      applyFilters(manager);
    }
  }

  protected Vector3 getNewOffset(final Vector2 oldMouse, final MouseState current, final Camera camera,
      final InteractManager manager) {

    // make plane object
    final Plane pickPlane = new Plane().setPlanePoints(_calcVec3A, _calcVec3B, _calcVec3C);

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
