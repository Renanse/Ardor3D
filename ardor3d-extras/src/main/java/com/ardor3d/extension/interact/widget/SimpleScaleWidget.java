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

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.ZBufferState.TestFunction;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.shape.Arrow;

public class SimpleScaleWidget extends AbstractInteractWidget {
    public static double MIN_SCALE = 0.000001;

    protected ReadOnlyVector3 _arrowDirection;

    public SimpleScaleWidget() {}

    public SimpleScaleWidget withArrow(final ReadOnlyVector3 arrowDirection) {
        return withArrow(arrowDirection, new ColorRGBA(1.0f, 0.0f, 0.0f, 0.4f), 0, 0);
    }

    public SimpleScaleWidget withArrow(final ReadOnlyVector3 arrowDirection, final ReadOnlyColorRGBA color) {
        return withArrow(arrowDirection, color, 0, 0);
    }

    public SimpleScaleWidget withArrow(final ReadOnlyVector3 arrowDirection, final ReadOnlyColorRGBA color,
            final double lengthGap, final double tipGap) {
        _arrowDirection = new Vector3(arrowDirection);
        _handle = new InteractArrow("scaleHandle", 1.0, 0.125, lengthGap, tipGap);
        if (!_arrowDirection.equals(Vector3.UNIT_Z)) {
            _handle.setRotation(new Quaternion().fromVectorToVector(Vector3.UNIT_Z, _arrowDirection));
        }

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        _handle.setRenderState(blend);

        ((Arrow) _handle).setDefaultColor(color);

        final ZBufferState zstate = new ZBufferState();
        zstate.setWritable(false);
        zstate.setFunction(TestFunction.Always);
        _handle.setRenderState(zstate);

        _handle.getSceneHints().setRenderBucketType(RenderBucketType.PostBucket);
        _handle.updateGeometricState(0);
        return this;
    }

    @Override
    public void targetChanged(final InteractManager manager) {
        if (_dragging) {
            endDrag(manager);
        }
        targetDataUpdated(manager);
    }

    @Override
    public void targetDataUpdated(final InteractManager manager) {
        final Spatial target = manager.getSpatialTarget();
        if (target == null) {
            _handle.setScale(1.0);
        } else {
            _handle.setScale(Math.max(SimpleScaleWidget.MIN_SCALE, target.getWorldBound().getRadius()));
        }
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
        // Make sure we have something to modify
        if (manager.getSpatialTarget() == null) {
            return;
        }

        // Make sure we are dragging.
        final MouseState current = inputStates.getCurrent().getMouseState();
        final MouseState previous = inputStates.getPrevious().getMouseState();
        if (current.getButtonState(_dragButton) != ButtonState.DOWN) {
            if (_dragging) {
                endDrag(manager);
            }
            return;
        }
        // if we're already dragging, make sure we only act on drags that started with a positive pick.
        else if (!current.getButtonsPressedSince(previous).contains(_dragButton) && !_dragging) {
            return;
        }

        final Camera camera = source.getCanvasRenderer().getCamera();
        final Vector2 oldMouse = new Vector2(previous.getX(), previous.getY());
        // Make sure we are dragging over the arrow
        if (!_dragging) {
            findPick(oldMouse, camera);
            final Vector3 lastPick = getLastPick();
            if (lastPick == null) {
                return;
            } else {
                beginDrag(manager);
            }
        }

        // we've established that our mouse is being held down, and started over our arrow. So consume.
        inputConsumed.set(true);

        // check if we've moved at all
        if (current == previous || current.getDx() == 0 && current.getDy() == 0) {
            return;
        }

        // act on drag
        final double scale = getNewScale(oldMouse, current, camera, manager);

        // Set new scale on spatial state
        manager.getSpatialState().getTransform().setScale(scale);

        // apply our filters, if any, now that we've made updates.
        applyFilters(manager);
    }

    protected double getNewScale(final Vector2 oldMouse, final MouseState current, final Camera camera,
            final InteractManager manager) {
        // calculate a plane running through the Arrow and facing the camera.
        _calcVec3A.set(_handle.getWorldTranslation());
        _calcVec3B.set(_calcVec3A).addLocal(camera.getLeft());
        _calcVec3C.set(_calcVec3A).addLocal(_arrowDirection);
        final Plane pickPlane = new Plane().setPlanePoints(_calcVec3A, _calcVec3B, _calcVec3C);

        // find out where we were hitting the plane before
        getPickRay(oldMouse, camera);
        _calcRay.intersectsPlane(pickPlane, _calcVec3A);
        final double oldHeight = _calcVec3A.getY();

        // find out where we are hitting the plane now
        getPickRay(new Vector2(current.getX(), current.getY()), camera);
        _calcRay.intersectsPlane(pickPlane, _calcVec3A);
        final double newHeight = _calcVec3A.getY();

        // Use distance between points against arrow length to determine how big we need to grow our bounding radius
        final double delta = newHeight - oldHeight;

        final double oldRadius = manager.getSpatialTarget().getWorldBound().getRadius();

        return manager.getSpatialTarget().getScale().getY() * (1.0 + delta / oldRadius);
    }
}
