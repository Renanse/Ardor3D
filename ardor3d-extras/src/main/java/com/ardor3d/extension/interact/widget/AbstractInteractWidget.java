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

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.filter.UpdateFilter;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.Pickable;
import com.ardor3d.intersection.PickingUtil;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.ReadOnlyTimer;

public abstract class AbstractInteractWidget {

  public static double MIN_SCALE = 0.000001;

  protected Node _handle;
  protected boolean _flipPickRay, _mouseOver = false;
  protected DragState _dragState = DragState.NONE;
  protected MouseButton _dragButton = MouseButton.LEFT;

  protected boolean _activeInputOnly = true;
  protected boolean _activeRenderOnly = true;
  protected boolean _activeUpdateOnly = true;

  protected Ray3 _calcRay = new Ray3();
  protected final Vector3 _calcVec3A = new Vector3();
  protected final Vector3 _calcVec3B = new Vector3();
  protected final Vector3 _calcVec3C = new Vector3();
  protected final Vector3 _calcVec3D = new Vector3();
  protected PrimitivePickResults _results = new PrimitivePickResults();

  protected Spatial _lastDragSpatial = null;
  protected Spatial _lastMouseOverSpatial = null;

  protected InteractMatrix _interactMatrix = InteractMatrix.World;

  /** List of filters to modify state after applying input. */
  protected IFilterList _filters = new BasicFilterList();

  protected InteractMouseOverCallback _mouseOverCallback;

  public AbstractInteractWidget() {
    _results.setCheckDistance(true);
  }

  /**
   * Use the given input states to determine if and how to activate this widget. If the widget uses
   * the given input, inputConsumed should be set to "true" and applyFilters should be called by this
   * method.
   *
   * @param source
   *          the canvas that is our input source.
   * @param inputStates
   *          the current and previous state of our input devices.
   * @param inputConsumed
   *          an atomic boolean used to indicate back to the caller of this function that we have
   *          consumed the given inputStates. If set to true, no other widgets will be offered this
   *          input, nor will any other scene input triggers attached to the manager.
   * @param manager
   *          our interact manager.
   */
  public void processInput(final Canvas source, final TwoInputStates inputStates, final AtomicBoolean inputConsumed,
      final InteractManager manager) {
    /**/}

  protected void applyFilters(final InteractManager manager) {
    _filters.applyFilters(manager, this);
  }

  public void checkMouseOver(final Canvas source, final MouseState current, final InteractManager manager) {
    final Camera camera = source.getCanvasRenderer().getCamera();

    // If we are dragging, we're in mouseOver state.
    if (_dragState != DragState.NONE) {
      if (!_mouseOver) {
        mouseEntered(source, current, manager);
      }
      return;
    }

    // Make sure we have something to modify
    if (manager.getSpatialTarget() == null) {
      if (_mouseOver) {
        mouseDeparted(source, current, manager);
      }
      return;
    }

    final Vector2 currMouse = new Vector2(current.getX(), current.getY());
    findPick(currMouse, camera);
    final Vector3 lastPick = getLastPick();
    if (lastPick == null) {
      if (_mouseOver) {
        mouseDeparted(source, current, manager);
        return;
      }
    } else {
      if (!_mouseOver) {
        mouseEntered(source, current, manager);
      } else if (_results.getPickData(0).getTarget() != _lastMouseOverSpatial) {
        mouseDeparted(source, current, manager);
        mouseEntered(source, current, manager);
      }
    }
  }

  protected void mouseEntered(final Canvas source, final MouseState current, final InteractManager manager) {
    final PickData pickData = _results.getPickData(0);
    _lastMouseOverSpatial = (Spatial) pickData.getTarget();
    _mouseOver = true;

    if (_mouseOverCallback != null) {
      _mouseOverCallback.mouseEntered(source, current, manager);
    }

    targetDataUpdated(manager);
  }

  protected void mouseDeparted(final Canvas source, final MouseState current, final InteractManager manager) {
    _lastMouseOverSpatial = null;
    _mouseOver = false;

    if (_mouseOverCallback != null) {
      _mouseOverCallback.mouseDeparted(source, current, manager);
    }

    targetDataUpdated(manager);
  }

  public boolean checkShouldDrag(final Camera camera, final MouseState current, final MouseState previous,
      final AtomicBoolean inputConsumed, final InteractManager manager) {
    // Make sure we have something to modify
    if (manager.getSpatialTarget() == null) {
      return false;
    }

    // Make sure we have our drag button down.
    if (current.getButtonState(_dragButton) != ButtonState.DOWN) {
      if (_dragState != DragState.NONE) {
        endDrag(manager, current);
      }
      return false;
    }

    switch (_dragState) {
      case NONE:
        // if we were not dragging previously, then let's see if we can drag now...
        // ...make sure this is a new drag interaction - i.e. the mouse button was clicked in this frame
        if (previous.getButtonState(_dragButton) == ButtonState.DOWN) {
          return false;
        }
        if (!isOverHandle(camera, previous)) {
          // No pick found, so we were dragging over nothing. Ignore.
          _lastDragSpatial = null;
          return false;
        }

        // We did find a pick, so start the drag process
        beginDrag(manager, current);

        // we've established that our mouse is being held down, and started over our arrow. So consume.
        inputConsumed.set(true);

        return true;

      case DRAG:
        // we're in the process of dragging still, so mark input as consumed.
        inputConsumed.set(true);

        // return true if our mouse state has changed
        return current != previous && (current.getDx() != 0 || current.getDy() != 0);

      case START_DRAG:
        _dragState = DragState.DRAG;
        return true;

      case PREPARE_DRAG:
        if (!isOverHandle(camera, previous)) {
          // No pick found, so we were dragging over nothing. Ignore.
          endDrag(manager, current);
          return false;
        }

        // we're still preparing a drag, so mark consumed and return false (not yet dragging.)
        inputConsumed.set(true);
        _filters.beginDrag(manager, this, current);
        return false;

      default:
        throw new Ardor3dException("Unhandled drag state: " + _dragState);
    }
  }

  private boolean isOverHandle(final Camera camera, final MouseState previous) {
    // ...make sure we are dragging over a widget handle
    findPick(new Vector2(previous.getX(), previous.getY()), camera);
    return getLastPick() != null;
  }

  public void beginDrag(final InteractManager manager, final MouseState current) {
    if (_results.getNumber() > 0) {
      final PickData pickData = _results.getPickData(0);
      _lastDragSpatial = (Spatial) pickData.getTarget();
      _dragState = DragState.START_DRAG;
      _filters.beginDrag(manager, this, current);
    }
  }

  public void endDrag(final InteractManager manager, final MouseState current) {
    _dragState = DragState.NONE;
    _lastDragSpatial = null;
    _filters.endDrag(manager, this, current);
  }

  public void update(final ReadOnlyTimer timer, final InteractManager manager) {
    _handle.updateGeometricState(timer.getTimePerFrame());
  }

  protected double calculateHandleScale(final InteractManager manager) {
    final Spatial target = manager.getSpatialTarget();
    if (target != null && target.getWorldBound() != null) {
      final BoundingVolume bound = target.getWorldBound();
      final ReadOnlyVector3 trans = target.getWorldTranslation();
      return Math.max(AbstractInteractWidget.MIN_SCALE,
          bound.getRadius() + trans.subtract(bound.getCenter(), _calcVec3A).length());
    }

    return 1.0;
  }

  public void render(final Renderer renderer, final InteractManager manager) {
    /**/}

  public void targetChanged(final InteractManager manager) {
    if (_dragState != DragState.NONE) {
      endDrag(manager, null);
    }
    if (_mouseOver) {
      mouseDeparted(null, null, manager);
    }
    targetDataUpdated(manager);
  }

  public void targetDataUpdated(final InteractManager manager) {
    /**/}

  public void receivedControl(final InteractManager manager) {
    if (_dragState != DragState.NONE) {
      endDrag(manager, null);
    }
  }

  public void lostControl(final InteractManager manager) {
    if (_mouseOver) {
      mouseDeparted(null, null, manager);
    }
  }

  public boolean isActiveInputOnly() { return _activeInputOnly; }

  public void setActiveInputOnly(final boolean activeOnly) { _activeInputOnly = activeOnly; }

  public boolean isActiveRenderOnly() { return _activeRenderOnly; }

  public void setActiveRenderOnly(final boolean activeOnly) { _activeRenderOnly = activeOnly; }

  public boolean isActiveUpdateOnly() { return _activeUpdateOnly; }

  public void setActiveUpdateOnly(final boolean activeOnly) { _activeUpdateOnly = activeOnly; }

  public boolean isFlipPickRay() { return _flipPickRay; }

  public void setFlipPickRay(final boolean flip) { _flipPickRay = flip; }

  public MouseButton getDragButton() { return _dragButton; }

  public void setDragButton(final MouseButton button) { _dragButton = button; }

  public Node getHandle() { return _handle; }

  protected Vector3 getLastPick() {
    if (_results.getNumber() > 0 && _results.getPickData(0).getIntersectionRecord().getNumberOfIntersections() > 0) {
      return _results.getPickData(0).getIntersectionRecord().getIntersectionPoint(0);
    }
    return null;
  }

  protected Pickable getLastPickable() {
    if (_results.getNumber() > 0) {
      return _results.getPickData(0).getTarget();
    }
    return null;
  }

  protected void findPick(final Vector2 mouseLoc, final Camera camera) {
    getPickRay(mouseLoc, camera);
    _results.clear();
    PickingUtil.findPick(_handle, _calcRay, _results);
  }

  protected void getPickRay(final Vector2 mouseLoc, final Camera camera) {
    camera.getPickRay(mouseLoc, _flipPickRay, _calcRay);
  }

  public void setInteractMatrix(final InteractMatrix matrix) { _interactMatrix = matrix; }

  public InteractMatrix getInteractMatrix() { return _interactMatrix; }

  public void addFilter(final UpdateFilter filter) {
    _filters.add(filter);
  }

  public void removeFilter(final UpdateFilter filter) {
    _filters.remove(filter);
  }

  public void clearFilters() {
    _filters.clear();
  }

  public void setMouseOverCallback(final InteractMouseOverCallback callback) { _mouseOverCallback = callback; }

  public InteractMouseOverCallback getMouseOverCallback() { return _mouseOverCallback; }

  public DragState getDragState() { return _dragState; }

  public void setDragState(final DragState state) { _dragState = state; }
}
