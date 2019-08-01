/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.filter.UpdateFilter;
import com.ardor3d.extension.interact.widget.MovePlanarWidget.MovePlane;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture2D;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.ReadOnlyTimer;

public class CompoundInteractWidget extends AbstractInteractWidget {

    private static final String MOVE_KEY = "Move";
    private static final String ROTATE_KEY = "Rotate";
    private static final String MOVE_PLANAR_KEY = "MovePlanar";
    private static final String MOVE_MULTIPLANAR_KEY = "MoveMultiPlanar";

    public static double MIN_SCALE = 0.000001;

    protected Map<String, AbstractInteractWidget> _widgets = new HashMap<>();

    protected AbstractInteractWidget _lastInputWidget = null;

    protected InteractMatrix _interactMatrix;

    public CompoundInteractWidget() {
        super(new BasicFilterList());
        _handle = new Node("handleRoot");
    }

    @Override
    public void addFilter(final UpdateFilter filter) {
        for (final AbstractInteractWidget widget : _widgets.values()) {
            widget.addFilter(filter);
        }
        super.addFilter(filter);
    }

    @Override
    public void removeFilter(final UpdateFilter filter) {
        for (final AbstractInteractWidget widget : _widgets.values()) {
            widget.removeFilter(filter);
        }
        super.removeFilter(filter);
    }

    @Override
    public void clearFilters() {
        for (final AbstractInteractWidget widget : _widgets.values()) {
            widget.clearFilters();
        }
        super.clearFilters();
    }

    public CompoundInteractWidget withMoveXAxis() {
        verifyMoveWidget().withXAxis();
        return this;
    }

    public CompoundInteractWidget withMoveXAxis(final ReadOnlyColorRGBA color) {
        verifyMoveWidget().withXAxis(color);
        return this;
    }

    public CompoundInteractWidget withMoveXAxis(final ReadOnlyColorRGBA color, final double scale, final double width,
            final double lengthGap, final double tipGap) {
        verifyMoveWidget().withXAxis(color, scale, width, lengthGap, tipGap);
        return this;
    }

    public CompoundInteractWidget withMoveYAxis() {
        verifyMoveWidget().withYAxis();
        return this;
    }

    public CompoundInteractWidget withMoveYAxis(final ReadOnlyColorRGBA color) {
        verifyMoveWidget().withYAxis(color);
        return this;
    }

    public CompoundInteractWidget withMoveYAxis(final ReadOnlyColorRGBA color, final double scale, final double width,
            final double lengthGap, final double tipGap) {
        verifyMoveWidget().withYAxis(color, scale, width, lengthGap, tipGap);
        return this;
    }

    public CompoundInteractWidget withMoveZAxis() {
        verifyMoveWidget().withZAxis();
        return this;
    }

    public CompoundInteractWidget withMoveZAxis(final ReadOnlyColorRGBA color) {
        verifyMoveWidget().withZAxis(color);
        return this;
    }

    public CompoundInteractWidget withMoveZAxis(final ReadOnlyColorRGBA color, final double scale, final double width,
            final double lengthGap, final double tipGap) {
        verifyMoveWidget().withZAxis(color, scale, width, lengthGap, tipGap);
        return this;
    }

    public CompoundInteractWidget withRotateXAxis() {
        verifyRotateWidget().withXAxis();
        return this;
    }

    public CompoundInteractWidget withRotateXAxis(final ReadOnlyColorRGBA color) {
        verifyRotateWidget().withXAxis(color);
        return this;
    }

    public CompoundInteractWidget withRotateXAxis(final ReadOnlyColorRGBA color, final float scale, final float width) {
        verifyRotateWidget().withXAxis(color, scale, width);
        return this;
    }

    public CompoundInteractWidget withRotateYAxis() {
        verifyRotateWidget().withYAxis();
        return this;
    }

    public CompoundInteractWidget withRotateYAxis(final ReadOnlyColorRGBA color) {
        verifyRotateWidget().withYAxis(color);
        return this;
    }

    public CompoundInteractWidget withRotateYAxis(final ReadOnlyColorRGBA color, final float scale, final float width) {
        verifyRotateWidget().withYAxis(color, scale, width);
        return this;
    }

    public CompoundInteractWidget withRotateZAxis() {
        verifyRotateWidget().withZAxis();
        return this;
    }

    public CompoundInteractWidget withRotateZAxis(final ReadOnlyColorRGBA color) {
        verifyRotateWidget().withZAxis(color);
        return this;
    }

    public CompoundInteractWidget withRotateZAxis(final ReadOnlyColorRGBA color, final float scale, final float width) {
        verifyRotateWidget().withZAxis(color, scale, width);
        return this;
    }

    public CompoundInteractWidget withRingTexture(final Texture2D texture) {
        verifyRotateWidget().setTexture(texture);
        return this;
    }

    public CompoundInteractWidget withMultiPlanarHandle() {
        MoveMultiPlanarWidget widget = (MoveMultiPlanarWidget) _widgets
                .get(CompoundInteractWidget.MOVE_MULTIPLANAR_KEY);
        if (widget != null) {
            widget.getHandle().removeFromParent();
        }

        widget = new MoveMultiPlanarWidget(_filters);
        _widgets.put(CompoundInteractWidget.MOVE_MULTIPLANAR_KEY, widget);
        _handle.attachChild(widget.getHandle());

        return this;
    }

    public CompoundInteractWidget withMultiPlanarHandle(final double extent) {
        MoveMultiPlanarWidget widget = (MoveMultiPlanarWidget) _widgets
                .get(CompoundInteractWidget.MOVE_MULTIPLANAR_KEY);
        if (widget != null) {
            widget.getHandle().removeFromParent();
        }

        widget = new MoveMultiPlanarWidget(_filters, extent);
        _widgets.put(CompoundInteractWidget.MOVE_MULTIPLANAR_KEY, widget);
        _handle.attachChild(widget.getHandle());

        return this;
    }

    public CompoundInteractWidget withPlanarHandle(final MovePlane plane, final ReadOnlyColorRGBA color) {
        MovePlanarWidget widget = (MovePlanarWidget) _widgets.get(CompoundInteractWidget.MOVE_PLANAR_KEY);
        if (widget != null) {
            widget.getHandle().removeFromParent();
        }

        widget = new MovePlanarWidget(_filters).withPlane(plane).withDefaultHandle(.5, .25, color);
        _widgets.put(CompoundInteractWidget.MOVE_PLANAR_KEY, widget);
        _handle.attachChild(widget.getHandle());

        return this;
    }

    public CompoundInteractWidget withPlanarHandle(final MovePlane plane, final double radius, final double height,
            final ReadOnlyColorRGBA color) {
        MovePlanarWidget widget = (MovePlanarWidget) _widgets.get(CompoundInteractWidget.MOVE_PLANAR_KEY);
        if (widget != null) {
            widget.getHandle().removeFromParent();
        }

        widget = new MovePlanarWidget(_filters).withPlane(plane).withDefaultHandle(radius, height, color);
        _widgets.put(CompoundInteractWidget.MOVE_PLANAR_KEY, widget);
        _handle.attachChild(widget.getHandle());

        return this;
    }

    private MoveWidget verifyMoveWidget() {
        MoveWidget moveWidget = (MoveWidget) _widgets.get(CompoundInteractWidget.MOVE_KEY);
        if (moveWidget == null) {
            moveWidget = new MoveWidget(_filters);
            _widgets.put(CompoundInteractWidget.MOVE_KEY, moveWidget);
            _handle.attachChild(moveWidget.getHandle());
        }
        return moveWidget;
    }

    private RotateWidget verifyRotateWidget() {
        RotateWidget rotateWidget = (RotateWidget) _widgets.get(CompoundInteractWidget.ROTATE_KEY);
        if (rotateWidget == null) {
            rotateWidget = new RotateWidget(_filters);
            _widgets.put(CompoundInteractWidget.ROTATE_KEY, rotateWidget);
            _handle.attachChild(rotateWidget.getHandle());
        }
        return rotateWidget;
    }

    @Override
    public void targetChanged(final InteractManager manager) {
        for (final AbstractInteractWidget widget : _widgets.values()) {
            widget.targetChanged(manager);
        }
    }

    @Override
    public void targetDataUpdated(final InteractManager manager) {
        for (final AbstractInteractWidget widget : _widgets.values()) {
            widget.targetDataUpdated(manager);
        }
    }

    @Override
    public void receivedControl(final InteractManager manager) {
        for (final AbstractInteractWidget widget : _widgets.values()) {
            widget.receivedControl(manager);
        }
    }

    @Override
    public void render(final Renderer renderer, final InteractManager manager) {
        for (final AbstractInteractWidget widget : _widgets.values()) {
            widget.render(renderer, manager);
        }
    }

    @Override
    public void processInput(final Canvas source, final TwoInputStates inputStates, final AtomicBoolean inputConsumed,
            final InteractManager manager) {
        // Make sure we have something to modify
        if (manager.getSpatialTarget() == null) {
            return;
        }
        // Make sure we are dragging.
        final MouseState current = inputStates.getCurrent().getMouseState();
        final MouseState previous = inputStates.getPrevious().getMouseState();
        final Camera camera = source.getCanvasRenderer().getCamera();

        if (current.getButtonState(_dragButton) != ButtonState.DOWN) {
            if (_lastInputWidget != null) {
                _lastInputWidget.processInput(source, inputStates, inputConsumed, manager);
                _lastInputWidget = null;
            } else {
                // check all of the widgets for mouseover
                for (final AbstractInteractWidget widget : _widgets.values()) {
                    widget.checkMouseOver(source, current, manager);
                }
            }
            _lastInputWidget = null;
            return;
        }

        if (_lastInputWidget == null) {
            final Vector2 oldMouse = new Vector2(previous.getX(), previous.getY());
            findPick(oldMouse, camera);
            if (_results.getNumber() <= 0) {
                return;
            }

            final Spatial picked = (Spatial) _results.getPickData(0).getTarget();
            if (picked == null) {
                return;
            }

            for (final AbstractInteractWidget widget : _widgets.values()) {
                if (picked.hasAncestor(widget.getHandle())) {
                    _lastInputWidget = widget;
                    break;
                }
            }
        }
        _lastInputWidget.processInput(source, inputStates, inputConsumed, manager);

        // apply our filters, if any, now that we've made updates.
        applyFilters(manager);
    }

    @Override
    public void setInteractMatrix(final InteractMatrix matrix) {
        _interactMatrix = matrix;
        for (final AbstractInteractWidget widget : _widgets.values()) {
            widget.setInteractMatrix(matrix);
        }
    }

    @Override
    public InteractMatrix getInteractMatrix() {
        return _interactMatrix;
    }

    @Override
    public void update(final ReadOnlyTimer timer, final InteractManager manager) {
        for (final AbstractInteractWidget widget : _widgets.values()) {
            widget.update(timer, manager);
        }
    }

}
