/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget.gizmo;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.extension.interact.widget.DragState;
import com.ardor3d.extension.interact.widget.InteractMatrix;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.ZBufferState.TestFunction;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.PickingHint;

/**
 * Base class for the v2 interact gizmos. Compared to the older widgets in the parent package,
 * gizmos:
 * <ul>
 * <li>hold a constant on-screen size regardless of target size or camera distance,</li>
 * <li>highlight the individual handle under the mouse rather than scaling the whole widget,</li>
 * <li>draw on top of the scene in two passes: a dimmed "x-ray" pass where the gizmo is occluded
 * and a full-strength pass where it is visible, and</li>
 * <li>fade out handles whose drag math is ill-conditioned at the current view angle.</li>
 * </ul>
 * Gizmos render immediately (their scenegraph uses the Skip bucket), so
 * {@link InteractManager#render(Renderer)} must be called after the main scene has been drawn and
 * its render buckets flushed - which is the usual place to call it.
 */
public abstract class AbstractGizmo extends AbstractInteractWidget {

  public static final ReadOnlyColorRGBA DEFAULT_X_COLOR = new ColorRGBA(0.91f, 0.23f, 0.29f, 1.0f);
  public static final ReadOnlyColorRGBA DEFAULT_Y_COLOR = new ColorRGBA(0.44f, 0.79f, 0.16f, 1.0f);
  public static final ReadOnlyColorRGBA DEFAULT_Z_COLOR = new ColorRGBA(0.20f, 0.51f, 0.92f, 1.0f);
  public static final ReadOnlyColorRGBA DEFAULT_CENTER_COLOR = new ColorRGBA(0.85f, 0.85f, 0.85f, 0.8f);
  public static final ReadOnlyColorRGBA DEFAULT_HIGHLIGHT_COLOR = new ColorRGBA(1.0f, 0.86f, 0.18f, 1.0f);

  /** Default on-screen footprint of a gizmo, in pixels. */
  public static final double DEFAULT_PIXEL_SIZE = 90;

  /** Alpha multiplier for the occluded (x-ray) render pass. */
  public static final float DEFAULT_GHOST_ALPHA = 0.28f;

  /** Handles are not pickable once faded below this alpha. */
  public static final double PICK_ALPHA_FLOOR = 0.2;

  protected double _pixelSize = AbstractGizmo.DEFAULT_PIXEL_SIZE;
  protected final ColorRGBA _highlightColor = new ColorRGBA(AbstractGizmo.DEFAULT_HIGHLIGHT_COLOR);
  protected float _ghostAlpha = AbstractGizmo.DEFAULT_GHOST_ALPHA;
  protected boolean _xray = true;

  /** View angle at or below which fading handles are fully hidden, in radians. */
  protected double _fadeHideAngle = 10 * MathUtils.DEG_TO_RAD;
  /** View angle at or above which fading handles are fully visible, in radians. */
  protected double _fadeFullAngle = 20 * MathUtils.DEG_TO_RAD;

  protected final List<GizmoHandle> _gizmoHandles = new ArrayList<>();

  protected final ZBufferState _ghostZState = new ZBufferState();

  public AbstractGizmo(final String name) {
    _handle = new Node(name);
    LightProperties.setLightReceiver(_handle, false);

    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    _handle.setRenderState(blend);

    final ZBufferState zstate = new ZBufferState();
    zstate.setFunction(TestFunction.LessThanOrEqualTo);
    _handle.setRenderState(zstate);

    // Render immediately when drawn rather than queueing - the gizmo is drawn once per pass by
    // render(), after the scene buckets have flushed.
    _handle.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    _handle.updateGeometricState(0);

    _ghostZState.setFunction(TestFunction.GreaterThan);
    _ghostZState.setWritable(false);
  }

  protected void addGizmoHandle(final GizmoHandle handle) {
    _gizmoHandles.add(handle);
    _handle.attachChild(handle.getRoot());
  }

  protected GizmoHandle findGizmoHandle(final Spatial spatial) {
    if (spatial == null) {
      return null;
    }
    for (int i = _gizmoHandles.size(); --i >= 0;) {
      final GizmoHandle handle = _gizmoHandles.get(i);
      if (handle.contains(spatial)) {
        return handle;
      }
    }
    return null;
  }

  /**
   * @return the handle currently being dragged, or hovered if no drag is active, or null.
   */
  public GizmoHandle getActiveHandle() {
    if (_dragState != DragState.NONE && _lastDragSpatial != null) {
      return findGizmoHandle(_lastDragSpatial);
    }
    if (_mouseOver && _lastMouseOverSpatial != null) {
      return findGizmoHandle(_lastMouseOverSpatial);
    }
    return null;
  }

  @Override
  public void targetDataUpdated(final InteractManager manager) {
    final Spatial target = manager.getSpatialTarget();
    if (target == null) {
      _handle.setRotation(Matrix3.IDENTITY);
    } else {
      target.updateGeometricState(0);

      if (_interactMatrix == InteractMatrix.Local) {
        _handle.setRotation(target.getWorldRotation());
      } else {
        _handle.setRotation(Matrix3.IDENTITY);
      }
    }
    // Note: scale is not set here. Gizmos are sized per-frame in render() to hold a constant
    // screen footprint.
  }

  @Override
  public void render(final Renderer renderer, final InteractManager manager) {
    final Spatial target = manager.getSpatialTarget();
    if (target == null) {
      return;
    }

    final Camera camera = Camera.getCurrentCamera();

    _handle.setTranslation(target.getWorldTranslation());
    _handle.setScale(Math.max(AbstractInteractWidget.MIN_SCALE,
        GizmoMath.calculateFixedScreenScale(camera, _handle.getTranslation(), _pixelSize)));

    updateCameraFacingHandles(camera);
    updateHandleFades(camera);
    _handle.updateGeometricState(0);

    final GizmoHandle active = getActiveHandle();
    final RenderContext context = ContextManager.getCurrentContext();

    if (_xray) {
      // Occluded pass: only draw where the gizmo is behind scene geometry, dimmed.
      applyHandleColors(active, _ghostAlpha);
      final RenderState previousZ = context.getEnforcedState(StateType.ZBuffer);
      context.enforceState(_ghostZState);
      renderer.draw(_handle);
      if (previousZ != null) {
        context.enforceState(previousZ);
      } else {
        context.clearEnforcedState(StateType.ZBuffer);
      }
    }

    // Visible pass: full strength wherever the gizmo passes the normal depth test.
    applyHandleColors(active, 1.0f);
    renderer.draw(_handle);
  }

  /**
   * Hook for orienting camera-facing handles (view-plane disks, screen-space rings) each frame.
   * Called before fades are updated and world transforms refreshed.
   */
  protected void updateCameraFacingHandles(final Camera camera) {
    /**/}

  protected void updateHandleFades(final Camera camera) {
    for (int i = _gizmoHandles.size(); --i >= 0;) {
      final GizmoHandle handle = _gizmoHandles.get(i);
      double alpha = 1.0;
      switch (handle.getFadeMode()) {
        case Axis: {
          _handle.getRotation().applyPost(handle.getAxis(), _calcVec3A);
          final double angle = GizmoMath.axisViewAngle(_calcVec3A, camera.getDirection());
          alpha = GizmoMath.fadeAlpha(angle, _fadeHideAngle, _fadeFullAngle);
          break;
        }
        case Plane: {
          _handle.getRotation().applyPost(handle.getAxis(), _calcVec3A);
          final double angle = GizmoMath.planeViewAngle(_calcVec3A, camera.getDirection());
          alpha = GizmoMath.fadeAlpha(angle, _fadeHideAngle, _fadeFullAngle);
          break;
        }
        default:
          break;
      }
      handle.setFadeAlpha(alpha);
      handle.getRoot().getSceneHints().setPickingHint(PickingHint.Pickable,
          alpha > AbstractGizmo.PICK_ALPHA_FLOOR);
    }
  }

  protected void applyHandleColors(final GizmoHandle active, final float alphaMultiplier) {
    for (int i = _gizmoHandles.size(); --i >= 0;) {
      final GizmoHandle handle = _gizmoHandles.get(i);
      final ReadOnlyColorRGBA rgb = handle == active ? _highlightColor : handle.getBaseColor();
      final float alpha = handle.getBaseColor().getAlpha() * (float) handle.getFadeAlpha() * alphaMultiplier;
      handle.applyColor(rgb, alpha);
    }
  }

  /**
   * Project the pick-ray hits for two mouse positions onto a world-space axis line through the
   * gizmo origin. The hits are taken against the plane containing the axis that most directly
   * faces the camera - the standard construction for single-axis drags.
   *
   * @param axis
   *          unit direction of the axis, in world space.
   * @param oldMouse
   *          previous mouse position.
   * @param newMouse
   *          current mouse position.
   * @param camera
   *          the viewing camera.
   * @param store
   *          on success, x holds the old hit's signed distance along the axis from the gizmo
   *          origin and y the new hit's.
   * @return false if the axis is aligned with the view direction (ill-conditioned) or a pick ray
   *         missed the plane.
   */
  protected boolean projectOnAxis(final ReadOnlyVector3 axis, final Vector2 oldMouse, final Vector2 newMouse,
      final Camera camera, final Vector2 store) {
    final ReadOnlyVector3 origin = _handle.getWorldTranslation();

    // The plane's normal is the component of the view direction perpendicular to the axis.
    final Vector3 normal = _calcVec3C.set(camera.getDirection())
        .subtractLocal(axis.multiply(camera.getDirection().dot(axis), _calcVec3A));
    if (normal.lengthSquared() < 1e-10) {
      return false;
    }
    normal.normalizeLocal();
    final Plane pickPlane = new Plane(normal, normal.dot(origin));

    getPickRay(oldMouse, camera);
    if (!_calcRay.intersectsPlane(pickPlane, _calcVec3A)) {
      return false;
    }
    getPickRay(newMouse, camera);
    if (!_calcRay.intersectsPlane(pickPlane, _calcVec3B)) {
      return false;
    }

    store.set(_calcVec3A.subtractLocal(origin).dot(axis), _calcVec3B.subtractLocal(origin).dot(axis));
    return true;
  }

  @Override
  protected void findPick(final Vector2 mouseLoc, final Camera camera) {
    getPickRay(mouseLoc, camera);
    _results.clear();
    // Unlike the base widget, pick against culled spatials too: thin handles carry invisible,
    // fatter pick-proxy meshes hidden with CullHint.Always.
    PickingUtil.findPick(_handle, _calcRay, _results, false);
  }

  public double getPixelSize() { return _pixelSize; }

  /** Set the on-screen footprint the gizmo is scaled to hold, in pixels. */
  public void setPixelSize(final double pixels) { _pixelSize = pixels; }

  public ReadOnlyColorRGBA getHighlightColor() { return _highlightColor; }

  public void setHighlightColor(final ReadOnlyColorRGBA color) { _highlightColor.set(color); }

  public boolean isXRay() { return _xray; }

  /** Enable or disable the dimmed render pass showing the gizmo where it is occluded. */
  public void setXRay(final boolean xray) { _xray = xray; }

  public float getGhostAlpha() { return _ghostAlpha; }

  public void setGhostAlpha(final float alpha) { _ghostAlpha = alpha; }

  public double getFadeHideAngle() { return _fadeHideAngle; }

  public double getFadeFullAngle() { return _fadeFullAngle; }

  /** Set the view-angle band over which ill-conditioned handles fade out, in radians. */
  public void setFadeAngles(final double hideBelow, final double fullAbove) {
    _fadeHideAngle = hideBelow;
    _fadeFullAngle = fullAbove;
  }

  public List<GizmoHandle> getGizmoHandles() { return _gizmoHandles; }
}
