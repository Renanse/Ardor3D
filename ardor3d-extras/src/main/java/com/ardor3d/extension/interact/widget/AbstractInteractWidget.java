/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.interact.widget;

import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.filter.UpdateFilter;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.logical.TwoInputStates;
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
import com.ardor3d.util.ReadOnlyTimer;

public abstract class AbstractInteractWidget {

    protected Node _handle;
    protected boolean _flipPickRay, _dragging = false;
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

    protected InteractMatrix _interactMatrix = InteractMatrix.World;

    /** List of filters to modify state after applying input. */
    protected IFilterList _filters;

    public AbstractInteractWidget(final IFilterList filterList) {
        _results.setCheckDistance(true);
        _filters = filterList;
    }

    /**
     * Use the given inputstates to determine if and how to activate this widget. If the widget uses the given input,
     * inputConsumed should be set to "true" and applyFilters should be called by this method.
     *
     * @param source
     *            the canvas that is our input source.
     * @param inputStates
     *            the current and previous state of our input devices.
     * @param inputConsumed
     *            an atomic boolean used to indicate back to the caller of this function that we have consumed the given
     *            inputStates. If set to true, no other widgets will be offered this input, nor will any other scene
     *            input triggers attached to the manager.
     * @param manager
     *            our interact manager.
     */
    public void processInput(final Canvas source, final TwoInputStates inputStates, final AtomicBoolean inputConsumed,
            final InteractManager manager) { /**/}

    protected void applyFilters(final InteractManager manager) {
        _filters.applyFilters(manager);
    }

    public void beginDrag(final InteractManager manager) {
        _dragging = true;
        _filters.beginDrag(manager);
    }

    public void endDrag(final InteractManager manager) {
        _dragging = false;
        _filters.endDrag(manager);
    }

    public void update(final ReadOnlyTimer timer, final InteractManager manager) {
        _handle.updateGeometricState(timer.getTimePerFrame());
    }

    protected double calculateHandleScale(final InteractManager manager) {
        final Spatial target = manager.getSpatialTarget();
        if (target != null && target.getWorldBound() != null) {
            final BoundingVolume bound = target.getWorldBound();
            final ReadOnlyVector3 trans = target.getWorldTranslation();
            return Math.max(MoveWidget.MIN_SCALE, bound.getRadius()
                    + trans.subtract(bound.getCenter(), _calcVec3A).length());
        }

        return 1.0;
    }

    public void render(final Renderer renderer, final InteractManager manager) { /**/}

    public void targetChanged(final InteractManager manager) { /**/}

    public void targetDataUpdated(final InteractManager manager) { /**/}

    public void receivedControl(final InteractManager manager) {
        if (_dragging) {
            endDrag(manager);
        }
    }

    public void lostControl(final InteractManager manager) { /**/}

    public boolean isActiveInputOnly() {
        return _activeInputOnly;
    }

    public void setActiveInputOnly(final boolean activeOnly) {
        _activeInputOnly = activeOnly;
    }

    public boolean isActiveRenderOnly() {
        return _activeRenderOnly;
    }

    public void setActiveRenderOnly(final boolean activeOnly) {
        _activeRenderOnly = activeOnly;
    }

    public boolean isActiveUpdateOnly() {
        return _activeUpdateOnly;
    }

    public void setActiveUpdateOnly(final boolean activeOnly) {
        _activeUpdateOnly = activeOnly;
    }

    public boolean isFlipPickRay() {
        return _flipPickRay;
    }

    public void setFlipPickRay(final boolean flip) {
        _flipPickRay = flip;
    }

    public MouseButton getDragButton() {
        return _dragButton;
    }

    public void setDragButton(final MouseButton button) {
        _dragButton = button;
    }

    public Node getHandle() {
        return _handle;
    }

    protected Vector3 getLastPick() {
        if (_results.getNumber() > 0 && _results.getPickData(0).getIntersectionRecord().getNumberOfIntersections() > 0) {
            return _results.getPickData(0).getIntersectionRecord().getIntersectionPoint(0);
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

    public void setInteractMatrix(final InteractMatrix matrix) {
        _interactMatrix = matrix;
    }

    public InteractMatrix getInteractMatrix() {
        return _interactMatrix;
    }

    public void addFilter(final UpdateFilter filter) {
        _filters.add(filter);
    }

    public void removeFilter(final UpdateFilter filter) {
        _filters.remove(filter);
    }

    public void clearFilters() {
        _filters.clear();
    }
}
