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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.filter.SnapSource;
import com.ardor3d.extension.interact.filter.UpdateFilter;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.extension.interact.widget.DragState;
import com.ardor3d.extension.interact.widget.InteractMatrix;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.IDpiScaleProvider;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
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
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.ZBufferState.TestFunction;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.shape.Disk;
import com.ardor3d.ui.text.BMFont;
import com.ardor3d.ui.text.BMText;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.URLResourceSource;

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

  /**
   * Hover tint for handles whose base color is too gray to visibly brighten in-hue. Chromatic
   * handles ignore this and brighten toward white instead, keeping their axis identity - see
   * {@link #highlightColorFor(GizmoHandle)}.
   */
  public static final ReadOnlyColorRGBA DEFAULT_HIGHLIGHT_COLOR = new ColorRGBA(1.0f, 0.86f, 0.18f, 1.0f);

  /** Fraction a hovered chromatic handle's color moves toward white. */
  public static final float HIGHLIGHT_BRIGHTEN = 0.55f;

  /** Chroma (max minus min channel) below which a handle counts as gray for highlighting. */
  public static final float HIGHLIGHT_CHROMA_FLOOR = 0.15f;

  /**
   * Default time constant for easing the hover highlight in and out, in seconds. At ~0.05s the
   * handle reads as reaching full highlight in roughly 100ms - snappy but not instant.
   */
  public static final double DEFAULT_HIGHLIGHT_EASE_TAU = 0.05;

  /**
   * Default fraction a fully-highlighted handle's strokes thicken. The pop grows the stroke width
   * in place rather than scaling the handle, so the geometry stays put under the cursor - important
   * for the rings, where a growing radius would slide the line away from the mouse.
   */
  public static final double DEFAULT_HIGHLIGHT_LINE_WIDTH_POP = 0.5;

  /**
   * Default opacity the inactive handles fade to while a drag is in progress, so the handle being
   * dragged stands out. 1 disables the drag-focus dim.
   */
  public static final double DEFAULT_DRAG_FOCUS_DIM = 0.15;

  /** Half-length of the axis guide line, in gizmo-local units; large enough to span any viewport. */
  public static final double AXIS_GUIDE_HALF_LENGTH = 100.0;
  /** Axis guide stroke width, in screen pixels at 1:1 DPI scale. */
  public static final float AXIS_GUIDE_WIDTH = 1.5f;
  /** Axis guide opacity. */
  public static final float AXIS_GUIDE_ALPHA = 0.55f;

  /** Time constant for the snap pulse to ease back out after a snapped-value change, in seconds. */
  public static final double DEFAULT_SNAP_PULSE_TAU = 0.12;

  /** Neutral tint of the origin ghost marker drawn where a translate drag began. */
  public static final ReadOnlyColorRGBA DEFAULT_ORIGIN_GHOST_COLOR = new ColorRGBA(0.85f, 0.85f, 0.85f, 1.0f);
  /** Opacity of the origin ghost marker. */
  public static final float ORIGIN_GHOST_ALPHA = 0.4f;
  /** Origin ghost ring radius and center-dot radius, in gizmo-local units (constant screen size). */
  public static final double ORIGIN_GHOST_RADIUS = 0.12;
  public static final double ORIGIN_GHOST_DOT_RADIUS = 0.025;
  /** Origin ghost ring stroke width, in screen pixels at 1:1 DPI scale, and its arc sample count. */
  public static final float ORIGIN_GHOST_WIDTH = 2.5f;
  public static final int ORIGIN_GHOST_SAMPLES = 40;

  /** Point size of the drag readout text. */
  public static final double DEFAULT_READOUT_SIZE = 20;
  /** Pixels the readout floats above the top of the gizmo's on-screen footprint. */
  public static final double READOUT_MARGIN = 16;

  /**
   * Outlined font for the drag readout, lazily loaded and shared across gizmos. Its baked dark
   * glyph halo keeps the white numbers legible over any background (e.g. a bright, sun-lit floor)
   * without a separate backing panel. Falls back to the plain default font if it cannot be loaded.
   */
  protected static BMFont READOUT_FONT;

  protected static synchronized BMFont readoutFont() {
    if (AbstractGizmo.READOUT_FONT == null) {
      try {
        AbstractGizmo.READOUT_FONT = new BMFont(new URLResourceSource(ResourceLocatorTool
            .getClassPathResource(BasicText.class, "com/ardor3d/ui/text/DroidSans-20-bold-regular-outline2.fnt")), true);
      } catch (final Exception ex) {
        AbstractGizmo.READOUT_FONT = BasicText.DEFAULT_FONT;
      }
    }
    return AbstractGizmo.READOUT_FONT;
  }

  /** Default on-screen footprint of a gizmo, in pixels. */
  public static final double DEFAULT_PIXEL_SIZE = 100;

  /**
   * Alpha multiplier for the occluded (x-ray) render pass. High enough that the gizmo stays
   * assertive when buried in its target - occlusion should read as a hint, not hide the tool.
   */
  public static final float DEFAULT_GHOST_ALPHA = 0.5f;

  /** Handles are not pickable once faded below this alpha. */
  public static final double PICK_ALPHA_FLOOR = 0.2;

  protected double _pixelSize = AbstractGizmo.DEFAULT_PIXEL_SIZE;
  protected final ColorRGBA _highlightColor = new ColorRGBA(AbstractGizmo.DEFAULT_HIGHLIGHT_COLOR);
  protected float _ghostAlpha = AbstractGizmo.DEFAULT_GHOST_ALPHA;
  protected boolean _xray = true;

  /** Time constant for easing the hover highlight in and out, in seconds. */
  protected double _highlightEaseTau = AbstractGizmo.DEFAULT_HIGHLIGHT_EASE_TAU;
  /** Fraction a fully-highlighted handle's strokes thicken, in place. */
  protected double _highlightLineWidthPop = AbstractGizmo.DEFAULT_HIGHLIGHT_LINE_WIDTH_POP;
  /** Opacity the inactive handles fade to while dragging. */
  protected double _dragFocusDim = AbstractGizmo.DEFAULT_DRAG_FOCUS_DIM;
  /** Eased drag-focus amount in [0, 1]: 0 at rest, 1 while a drag is in progress. */
  protected double _dragFocus = 0.0;

  /** Time constant for the snap pulse to ease out. */
  protected double _snapPulseTau = AbstractGizmo.DEFAULT_SNAP_PULSE_TAU;
  /** Eased snap pulse in [0, 1]: set to 1 on a snapped-value change, eases back to 0. */
  protected double _snapPulse = 0.0;

  /** View angle at or below which fading handles are fully hidden, in radians. */
  protected double _fadeHideAngle = 10 * MathUtils.DEG_TO_RAD;
  /** View angle at or above which fading handles are fully visible, in radians. */
  protected double _fadeFullAngle = 20 * MathUtils.DEG_TO_RAD;

  protected final List<GizmoHandle> _gizmoHandles = new ArrayList<>();

  protected final ZBufferState _ghostZState = new ZBufferState();

  /**
   * Source of the display's DPI scale, applied to the gizmo's pixel footprint and stroke widths.
   * Captured from the interacting canvas if not set explicitly.
   */
  protected IDpiScaleProvider _dpiScaleProvider;
  /** DPI scale currently applied to stroke widths, so rescaling only happens on change. */
  protected double _appliedDpiScale = 1.0;

  // scratch objects for the drag hot path, complementing the _calcVec3* pool in the base class
  protected final Vector2 _calcVec2A = new Vector2();
  protected final Vector2 _calcVec2B = new Vector2();
  protected final Vector2 _calcVec2C = new Vector2();
  protected final Plane _calcPlane = new Plane();
  protected final Quaternion _calcQuat = new Quaternion();
  protected final ColorRGBA _calcColor = new ColorRGBA();
  protected final ColorRGBA _calcColorB = new ColorRGBA();

  /** The target's transform when the current drag began, restored if the drag is cancelled. */
  protected final Transform _dragStartTransform = new Transform();
  /** Whether {@link #_dragStartTransform} holds a snapshot for the active drag. */
  protected boolean _hasDragStartTransform = false;

  /**
   * A long stroke through the gizmo origin along the axis of an active single-axis drag, extending
   * the constraint line across the viewport. Hidden except during axis drags; its two endpoints are
   * rewritten each frame onto the active axis.
   */
  protected final Line _axisGuide;

  /**
   * Screen-space numeric readout shown while dragging (translation delta, angle, or scale factor).
   * Lives in the application's ortho/screen root - the app attaches {@link #getReadout()} there and
   * the gizmo shows, positions and fills it. Content comes from {@link #getReadoutText}.
   */
  protected final BasicText _readoutText;

  /**
   * A faint marker drawn at the world origin where the current drag began, shown once the origin has
   * moved away from it - a "you moved from here" anchor. Only translate drags move the origin, so it
   * self-hides for rotate and scale (whose origin stays put). Standalone, not a child of
   * {@link #_handle}, which tracks the moving target; positioned and sized per frame in render().
   */
  protected final Node _originGhost;
  /** Gizmo world origin captured at drag start - where the ghost is drawn. */
  protected final Vector3 _dragStartWorldTranslation = new Vector3();
  /** Whether a drag with a captured start origin is active. */
  protected boolean _hasOriginGhost = false;
  /** Whether to show the origin ghost at all. */
  protected boolean _showOriginGhost = true;

  public AbstractGizmo(final String name) {
    _handle = new Node(name);
    LightProperties.setLightReceiver(_handle, false);

    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    _handle.setRenderState(blend);

    final ZBufferState zstate = new ZBufferState();
    zstate.setFunction(TestFunction.LessThanOrEqualTo);
    _handle.setRenderState(zstate);

    // Two-sided: plane fills and camera-facing parts are viewed from either side, and stroke
    // geometry is expanded in screen space by its material, where winding is not meaningful.
    final CullState cull = new CullState();
    cull.setCullFace(CullState.Face.None);
    _handle.setRenderState(cull);

    // Axis constraint guide: an antialiased stroke along +/-X, reoriented onto the active axis and
    // shown only during single-axis drags (see updateAxisGuide). Built like the handle strokes.
    _axisGuide = GizmoGeometry.segmentStroke("axisGuide", new Vector3(-AbstractGizmo.AXIS_GUIDE_HALF_LENGTH, 0, 0),
        new Vector3(AbstractGizmo.AXIS_GUIDE_HALF_LENGTH, 0, 0), AbstractGizmo.AXIS_GUIDE_WIDTH);
    LightProperties.setLightReceiver(_axisGuide, false);
    _axisGuide.getSceneHints().setCullHint(CullHint.Always);
    _handle.attachChild(_axisGuide);
    MaterialUtil.autoMaterials(_axisGuide);

    // Drag readout: a centered, outlined-font label the app renders in its ortho root. Hidden until
    // a drag is active. Not attached to _handle - it lives in screen space, not the gizmo's frame.
    _readoutText = new BasicText("gizmoReadout", "", AbstractGizmo.readoutFont(), AbstractGizmo.DEFAULT_READOUT_SIZE);
    _readoutText.setTextColor(ColorRGBA.WHITE);
    _readoutText.setAlign(BMText.Align.Center);
    _readoutText.getSceneHints().setCullHint(CullHint.Always);

    // Origin ghost: a faint camera-facing ring plus a center dot, marking where a translate drag
    // began. Standalone (not under _handle) so it stays at the start point while the gizmo tracks
    // the moving target; drawn immediately by render() like _handle. updateOriginGhost() positions,
    // orients and shows/hides it each frame.
    _originGhost = new Node("originGhost");
    LightProperties.setLightReceiver(_originGhost, false);
    final BlendState ghostBlend = new BlendState();
    ghostBlend.setBlendEnabled(true);
    _originGhost.setRenderState(ghostBlend);
    final ReadOnlyColorRGBA gc = AbstractGizmo.DEFAULT_ORIGIN_GHOST_COLOR;
    final Line ghostRing = GizmoGeometry.arcStroke("ghostRing", AbstractGizmo.ORIGIN_GHOST_RADIUS, 0,
        MathUtils.TWO_PI, AbstractGizmo.ORIGIN_GHOST_SAMPLES, AbstractGizmo.ORIGIN_GHOST_WIDTH);
    ghostRing.setDefaultColor(gc.getRed(), gc.getGreen(), gc.getBlue(), AbstractGizmo.ORIGIN_GHOST_ALPHA);
    _originGhost.attachChild(ghostRing);
    final Disk ghostDot = new Disk("ghostDot", 2, 16, AbstractGizmo.ORIGIN_GHOST_DOT_RADIUS);
    ghostDot.updateModelBound();
    LightProperties.setLightReceiver(ghostDot, false);
    ghostDot.setDefaultColor(gc.getRed(), gc.getGreen(), gc.getBlue(), AbstractGizmo.ORIGIN_GHOST_ALPHA);
    GizmoGeometry.disableDepthWrite(ghostDot);
    _originGhost.attachChild(ghostDot);
    _originGhost.getSceneHints().setAllPickingHints(false);
    _originGhost.getSceneHints().setCullHint(CullHint.Always);
    _originGhost.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    MaterialUtil.autoMaterials(_originGhost);
    _originGhost.updateGeometricState(0);

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

  /**
   * Ease each handle's hover highlight toward its target - 1 for the handle under the mouse or
   * being dragged, 0 for the rest - before the base class refreshes geometric state. The highlight
   * drives both the color lerp and the scale pop applied in {@link #render}. Animation is advanced
   * here, so a gizmo that is rendered without being updated each frame simply sits at rest; the
   * standard {@link InteractManager} loop updates then renders every frame.
   */
  @Override
  public void update(final ReadOnlyTimer timer, final InteractManager manager) {
    final double dt = timer.getTimePerFrame();
    final GizmoHandle active = getActiveHandle();
    for (int i = _gizmoHandles.size(); --i >= 0;) {
      final GizmoHandle handle = _gizmoHandles.get(i);
      handle.setHighlight(
          GizmoMath.approach(handle.getHighlight(), handle == active ? 1.0 : 0.0, dt, _highlightEaseTau));
    }
    // Ease the drag-focus dim in while a drag is active, out when it ends.
    _dragFocus = GizmoMath.approach(_dragFocus, _dragState != DragState.NONE ? 1.0 : 0.0, dt, _highlightEaseTau);
    // The snap pulse eases back to rest; gizmos re-arm it on each snapped-value change.
    _snapPulse = GizmoMath.approach(_snapPulse, 0.0, dt, _snapPulseTau);
    super.update(timer, manager);
  }

  /**
   * @return the increment (in the gizmo's native quantity) of the first actively-snapping filter on
   *         this gizmo, or 0 if none is snapping. Used to draw the snap tick grid during a drag.
   */
  protected double activeSnapIncrement() {
    for (final UpdateFilter filter : _filters) {
      if (filter instanceof SnapSource snap && snap.isSnapping()) {
        return snap.getSnapIncrement();
      }
    }
    return 0.0;
  }

  /** Re-arm the snap pulse to full; it eases back out over {@link #_snapPulseTau}. */
  protected void triggerSnapPulse() {
    _snapPulse = 1.0;
  }

  public double getSnapPulse() { return _snapPulse; }

  @Override
  public void beginDrag(final InteractManager manager, final MouseState current) {
    super.beginDrag(manager, current);
    // super sets START_DRAG only if a handle was picked; snapshot the pre-drag transform so the
    // drag can be cancelled back to it (see cancelDrag), and the world origin so the origin ghost
    // can anchor there (see updateOriginGhost).
    if (_dragState != DragState.NONE) {
      _dragStartTransform.set(manager.getSpatialState().getTransform());
      _hasDragStartTransform = true;
      final Spatial target = manager.getSpatialTarget();
      if (target != null) {
        _dragStartWorldTranslation.set(target.getWorldTranslation());
        _hasOriginGhost = true;
      }
    }
  }

  @Override
  public void endDrag(final InteractManager manager, final MouseState current) {
    super.endDrag(manager, current);
    // The pre-drag snapshot is only valid during a drag - drop it so the readout stops showing.
    _hasDragStartTransform = false;
    _hasOriginGhost = false;
  }

  /**
   * Cancel an in-progress drag: restore the target to the transform it had when the drag began and
   * end the interaction, discarding the drag's changes. A no-op when no drag is active.
   */
  public void cancelDrag(final InteractManager manager, final MouseState current) {
    if (_dragState == DragState.NONE) {
      return;
    }
    final Spatial target = manager.getSpatialTarget();
    if (_hasDragStartTransform && target != null) {
      manager.getSpatialState().getTransform().set(_dragStartTransform);
      manager.getSpatialState().applyState(target);
      manager.fireTargetDataUpdated();
    }
    endDrag(manager, current);
    _hasDragStartTransform = false;
  }

  /**
   * If a drag is active and Escape is held, cancel it and mark the input consumed. Gizmos call this
   * at the top of processInput so Escape aborts a drag in progress; the drag stays cancelled until
   * the mouse button is released and pressed again.
   *
   * @return true if a drag was cancelled by this call.
   */
  protected boolean cancelDragIfRequested(final KeyboardState keyboard, final MouseState current,
      final AtomicBoolean inputConsumed, final InteractManager manager) {
    if (_dragState == DragState.NONE || keyboard == null || !keyboard.isDown(Key.ESCAPE)) {
      return false;
    }
    cancelDrag(manager, current);
    inputConsumed.set(true);
    return true;
  }

  @Override
  public void lostControl(final InteractManager manager) {
    super.lostControl(manager);
    // A deactivated gizmo's render() no longer runs, so its readout (which lives in the shared ortho
    // root) would otherwise stay frozen on screen if it was mid-drag. Hide it here.
    hideReadout();
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

    updateDpiScale();

    _handle.setTranslation(target.getWorldTranslation());
    _handle.setScale(Math.max(AbstractInteractWidget.MIN_SCALE,
        GizmoMath.calculateFixedScreenScale(camera, _handle.getTranslation(), _pixelSize * _appliedDpiScale)));

    updateCameraFacingHandles(camera);
    updateHandleFades(camera);
    applyHandleLineWidths();
    updateAxisGuide();
    _handle.updateGeometricState(0);

    // Origin ghost: a standalone marker at the drag's start point, drawn under the gizmo. Self-hides
    // (via its cull hint) when not applicable, so the draw call is a no-op then.
    updateOriginGhost(camera);
    renderer.draw(_originGhost);

    final RenderContext context = ContextManager.getCurrentContext();

    if (_xray) {
      // Occluded pass: only draw where the gizmo is behind scene geometry, dimmed.
      applyHandleColors(_ghostAlpha);
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
    applyHandleColors(1.0f);
    renderer.draw(_handle);

    updateReadout(manager);
  }

  /**
   * Show and position the drag readout for the frame, filling it from {@link #getReadoutText}, or
   * hide it when no drag is active. The readout floats just above the gizmo's on-screen footprint,
   * centered; it lives in the app's ortho root, so it is positioned in screen pixels.
   */
  protected void updateReadout(final InteractManager manager) {
    final String text = _dragState != DragState.NONE && manager.getSpatialTarget() != null ? getReadoutText(manager)
        : null;
    if (text == null) {
      hideReadout();
      return;
    }
    _readoutText.setText(text);
    final Vector3 screen = Camera.getCurrentCamera().getScreenCoordinates(_handle.getWorldTranslation(), _calcVec3A);
    final double above = _pixelSize * _appliedDpiScale + AbstractGizmo.READOUT_MARGIN;
    _readoutText.setTranslation(Math.round(screen.getX()), Math.round(screen.getY() + above), 0);
    _readoutText.getSceneHints().setCullHint(CullHint.Never);
  }

  protected void hideReadout() {
    if (_readoutText.getSceneHints().getCullHint() != CullHint.Always) {
      _readoutText.getSceneHints().setCullHint(CullHint.Always);
    }
  }

  /**
   * The numeric readout string for the current drag, or null to show nothing. Overridden per gizmo
   * (translation delta, rotation angle, scale factor); the base gizmo shows no readout.
   */
  protected String getReadoutText(final InteractManager manager) {
    return null;
  }

  /**
   * Set each handle's stroke widths for the frame: the display DPI scale, thickened in place by the
   * handle's current highlight amount. Growing the width rather than scaling the handle keeps the
   * geometry under the cursor - a ring pops thicker without its radius sliding away from the mouse.
   */
  protected void applyHandleLineWidths() {
    for (int i = _gizmoHandles.size(); --i >= 0;) {
      final GizmoHandle handle = _gizmoHandles.get(i);
      handle.applyLineWidthScale((float) (_appliedDpiScale * (1.0 + _highlightLineWidthPop * handle.getHighlight())));
    }
  }

  /**
   * Reorient and show the axis guide line while a single-axis drag is active, or hide it otherwise.
   * The guide runs through the gizmo origin along the dragged axis (in the gizmo's local frame) and
   * takes that axis's color, so the constraint line reads across the whole viewport.
   */
  protected void updateAxisGuide() {
    final GizmoHandle active = getActiveHandle();
    final boolean axisDrag = _dragState != DragState.NONE && active != null && switch (active.getPart()) {
      case AxisX, AxisY, AxisZ -> true;
      default -> false;
    };
    if (!axisDrag) {
      if (_axisGuide.getSceneHints().getCullHint() != CullHint.Always) {
        _axisGuide.getSceneHints().setCullHint(CullHint.Always);
      }
      return;
    }

    final ReadOnlyVector3 axis = active.getAxis();
    final float x = (float) (axis.getX() * AbstractGizmo.AXIS_GUIDE_HALF_LENGTH);
    final float y = (float) (axis.getY() * AbstractGizmo.AXIS_GUIDE_HALF_LENGTH);
    final float z = (float) (axis.getZ() * AbstractGizmo.AXIS_GUIDE_HALF_LENGTH);
    final FloatBuffer verts = _axisGuide.getMeshData().getVertexBuffer();
    verts.clear();
    verts.put(-x).put(-y).put(-z).put(x).put(y).put(z);
    verts.rewind();
    _axisGuide.getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);
    _axisGuide.updateModelBound();

    final ReadOnlyColorRGBA c = active.getBaseColor();
    _axisGuide.setDefaultColor(c.getRed(), c.getGreen(), c.getBlue(), AbstractGizmo.AXIS_GUIDE_ALPHA);
    _axisGuide.setLineWidth(AbstractGizmo.AXIS_GUIDE_WIDTH * (float) _appliedDpiScale);
    _axisGuide.getSceneHints().setCullHint(CullHint.Never);
  }

  /**
   * Position, orient and show the origin ghost for the frame, or hide it. The ghost anchors at the
   * world origin captured when the drag began and appears only once the current origin has moved
   * away from it - so it marks "you moved from here" during a translate drag and stays hidden for
   * rotate and scale, whose origin does not move. Sized to a constant screen footprint like the
   * gizmo and turned to face the camera so the ring always reads as a circle.
   */
  protected void updateOriginGhost(final Camera camera) {
    if (!_showOriginGhost || !_hasOriginGhost || _dragState == DragState.NONE) {
      hideOriginGhost();
      return;
    }
    final ReadOnlyVector3 current = _handle.getWorldTranslation();
    if (_dragStartWorldTranslation.distanceSquared(current) <= MathUtils.ZERO_TOLERANCE) {
      // Origin has not moved: rotate/scale drag, or a translate drag that has not moved yet.
      hideOriginGhost();
      return;
    }
    _originGhost.setTranslation(_dragStartWorldTranslation);
    _originGhost.setScale(Math.max(AbstractInteractWidget.MIN_SCALE, GizmoMath.calculateFixedScreenScale(camera,
        _dragStartWorldTranslation, _pixelSize * _appliedDpiScale)));
    _calcVec3A.set(camera.getDirection()).negateLocal();
    _calcQuat.fromVectorToVector(Vector3.UNIT_Z, _calcVec3A);
    _originGhost.setRotation(_calcQuat);
    _originGhost.getSceneHints().setCullHint(CullHint.Never);
    _originGhost.updateGeometricState(0);
  }

  private void hideOriginGhost() {
    if (_originGhost.getSceneHints().getCullHint() != CullHint.Always) {
      _originGhost.getSceneHints().setCullHint(CullHint.Always);
    }
  }

  /**
   * Hook for orienting camera-facing handles (view-plane disks, screen-space rings) each frame.
   * Called before fades are updated and world transforms refreshed.
   */
  protected void updateCameraFacingHandles(final Camera camera) {
    /**/}

  /**
   * Track the display's DPI scale. The gizmo's pixel footprint and per-frame stroke widths (see
   * {@link #applyHandleLineWidths}) both pick it up in render(). No-op until a scale provider is
   * known - set one directly or let processInput capture the interacting canvas.
   */
  protected void updateDpiScale() {
    final double scale = _dpiScaleProvider != null ? _dpiScaleProvider.scaleToScreenDpi(1.0) : 1.0;
    if (scale > 0) {
      _appliedDpiScale = scale;
    }
  }

  /** Capture the canvas as our DPI scale source, unless one was set explicitly. */
  protected void captureDpiScaleProvider(final Canvas source) {
    if (_dpiScaleProvider == null && source != null) {
      _dpiScaleProvider = source;
    }
  }

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

  protected void applyHandleColors(final float alphaMultiplier) {
    for (int i = _gizmoHandles.size(); --i >= 0;) {
      final GizmoHandle handle = _gizmoHandles.get(i);
      final ReadOnlyColorRGBA base = handle.getBaseColor();
      final float h = (float) handle.getHighlight();
      final ReadOnlyColorRGBA rgb;
      if (h <= 0f) {
        rgb = base;
      } else {
        // Lerp base -> its highlight color by the eased amount. highlightColorFor may hand back
        // _calcColor, so lerp into the separate _calcColorB. Alpha is carried by the alpha arg.
        final ReadOnlyColorRGBA hi = highlightColorFor(handle);
        rgb = _calcColorB.set(base.getRed() + (hi.getRed() - base.getRed()) * h,
            base.getGreen() + (hi.getGreen() - base.getGreen()) * h,
            base.getBlue() + (hi.getBlue() - base.getBlue()) * h, base.getAlpha());
      }
      // Drag-focus: dim the handles that are not the drag's focus toward _dragFocusDim while a
      // drag is active. Keyed off the eased highlight, so the dragged (highlight ~1) handle stays
      // full and the rest fade; zero effect when no drag is active (_dragFocus ~0).
      final float focus = 1f - (float) (_dragFocus * (1.0 - _dragFocusDim) * (1.0 - h));
      final float alpha = base.getAlpha() * (float) handle.getFadeAlpha() * focus * alphaMultiplier;
      handle.applyColor(rgb, alpha);
    }
  }

  /**
   * The hover color for a handle: its own color brightened toward white, so the handle stays
   * identifiable while clearly lit up. Handles too gray to visibly brighten (center disks, roll
   * rings) use the configured highlight tint instead.
   */
  protected ReadOnlyColorRGBA highlightColorFor(final GizmoHandle handle) {
    final ReadOnlyColorRGBA base = handle.getBaseColor();
    final float max = Math.max(base.getRed(), Math.max(base.getGreen(), base.getBlue()));
    final float min = Math.min(base.getRed(), Math.min(base.getGreen(), base.getBlue()));
    if (max - min < AbstractGizmo.HIGHLIGHT_CHROMA_FLOOR) {
      return _highlightColor;
    }
    final float keep = 1f - AbstractGizmo.HIGHLIGHT_BRIGHTEN;
    return _calcColor.set(1f - (1f - base.getRed()) * keep, 1f - (1f - base.getGreen()) * keep,
        1f - (1f - base.getBlue()) * keep, base.getAlpha());
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
    _calcPlane.setNormal(normal);
    _calcPlane.setConstant(normal.dot(origin));

    getPickRay(oldMouse, camera);
    if (!_calcRay.intersectsPlane(_calcPlane, _calcVec3A)) {
      return false;
    }
    getPickRay(newMouse, camera);
    if (!_calcRay.intersectsPlane(_calcPlane, _calcVec3B)) {
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

  /**
   * Set the on-screen footprint the gizmo is scaled to hold, in pixels at 1:1 DPI scale. On
   * scaled displays the footprint and stroke widths are multiplied by the DPI scale.
   */
  public void setPixelSize(final double pixels) { _pixelSize = pixels; }

  public IDpiScaleProvider getDpiScaleProvider() { return _dpiScaleProvider; }

  /**
   * Set the source of the display's DPI scale. Optional: the gizmo captures the canvas it
   * receives input from if none was set.
   */
  public void setDpiScaleProvider(final IDpiScaleProvider provider) { _dpiScaleProvider = provider; }

  public ReadOnlyColorRGBA getHighlightColor() { return _highlightColor; }

  public void setHighlightColor(final ReadOnlyColorRGBA color) { _highlightColor.set(color); }

  public double getHighlightEaseTau() { return _highlightEaseTau; }

  /** Set the time constant for easing the hover highlight in and out, in seconds; 0 to snap. */
  public void setHighlightEaseTau(final double seconds) { _highlightEaseTau = seconds; }

  public double getHighlightLineWidthPop() { return _highlightLineWidthPop; }

  /** Set the fraction a fully-highlighted handle's strokes thicken in place; 0 for no pop. */
  public void setHighlightLineWidthPop(final double fraction) { _highlightLineWidthPop = fraction; }

  public double getDragFocusDim() { return _dragFocusDim; }

  /** Set the opacity inactive handles fade to while dragging; 1 disables the drag-focus dim. */
  public void setDragFocusDim(final double opacity) { _dragFocusDim = opacity; }

  /** @return the shared axis constraint guide line, shown during single-axis drags. */
  public Line getAxisGuide() { return _axisGuide; }

  /** @return the origin ghost marker drawn at a translate drag's start point. */
  public Node getOriginGhost() { return _originGhost; }

  public boolean isShowOriginGhost() { return _showOriginGhost; }

  /** Enable or disable the faint marker drawn where a translate drag began. */
  public void setShowOriginGhost(final boolean show) { _showOriginGhost = show; }

  /**
   * @return the screen-space drag readout. Attach it to an application node rendered in ortho/screen
   *         coordinates (origin at the bottom left) to enable it; the gizmo handles visibility,
   *         position and content.
   */
  public BasicText getReadout() { return _readoutText; }

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
