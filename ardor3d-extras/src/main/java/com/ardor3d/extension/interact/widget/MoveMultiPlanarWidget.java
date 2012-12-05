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

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.ButtonState;
import com.ardor3d.input.MouseState;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.PrimitiveKey;
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
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.ShadingState;
import com.ardor3d.renderer.state.ShadingState.ShadingMode;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.ZBufferState.TestFunction;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.geom.BufferUtils;

public class MoveMultiPlanarWidget extends AbstractInteractWidget {
    public static double MIN_SCALE = 0.000001;

    public MoveMultiPlanarWidget() {
        this(0.5);
    }

    public MoveMultiPlanarWidget(final double extent) {
        _handle = new Node("moveHandle");

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        _handle.setRenderState(blend);

        final ZBufferState zstate = new ZBufferState();
        zstate.setFunction(TestFunction.LessThanOrEqualTo);
        _handle.setRenderState(zstate);

        _handle.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
        _handle.updateGeometricState(0);

        createDefaultHandle(extent);
    }

    protected void createDefaultHandle(final double extent) {
        final Box grip = new Box("grip", Vector3.ZERO, extent, extent, extent);
        grip.updateModelBound();
        _handle.attachChild(grip);

        // setup some colors, just at the corner of the primitives since we will use flat shading.
        grip.setSolidColor(ColorRGBA.WHITE);
        final FloatBuffer colors = grip.getMeshData().getColorBuffer();
        BufferUtils.setInBuffer(ColorRGBA.MAGENTA, colors, 0);
        BufferUtils.setInBuffer(ColorRGBA.CYAN, colors, 4);
        BufferUtils.setInBuffer(ColorRGBA.MAGENTA, colors, 8);
        BufferUtils.setInBuffer(ColorRGBA.CYAN, colors, 12);
        BufferUtils.setInBuffer(ColorRGBA.YELLOW, colors, 16);
        BufferUtils.setInBuffer(ColorRGBA.YELLOW, colors, 20);

        // set flat shading
        final ShadingState shade = new ShadingState();
        shade.setShadingMode(ShadingMode.Flat);
        grip.setRenderState(shade);

        // setup a material state to use the colors from the vertices.
        final MaterialState material = new MaterialState();
        material.setColorMaterial(ColorMaterial.Diffuse);
        grip.setRenderState(material);
    }

    @Override
    public void targetChanged(final InteractManager manager) {
        if (_dragging) {
            endDrag(manager);
        }
        final Spatial target = manager.getSpatialTarget();
        if (target != null) {
            _handle.setScale(Math.max(MoveMultiPlanarWidget.MIN_SCALE, target.getWorldBound().getRadius()
                    + target.getWorldTranslation().subtract(target.getWorldBound().getCenter(), _calcVec3A).length()));
        }
        targetDataUpdated(manager);
    }

    @Override
    public void targetDataUpdated(final InteractManager manager) {
        final Spatial target = manager.getSpatialTarget();
        if (target == null) {
            _handle.setScale(1.0);
            _handle.setRotation(Matrix3.IDENTITY);
        } else {
            // update scale of widget using bounding radius
            target.updateGeometricState(0);

            // update arrow rotations from target
            if (_interactMatrix == InteractMatrix.Local) {
                _handle.setRotation(target.getWorldRotation());
            } else {
                _handle.setRotation(Matrix3.IDENTITY);
            }
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
        // Make sure we are dragging over the handle
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
        final PickData pickData = _results.getPickData(0);
        final Spatial picked = (Spatial) pickData.getTarget();
        if (picked instanceof Mesh && pickData.getIntersectionRecord().getNumberOfIntersections() > 0) {
            final PrimitiveKey key = pickData.getIntersectionRecord().getIntersectionPrimitive(0);
            ((Mesh) picked).getMeshData().getPrimitiveVertices(key.getPrimitiveIndex(), key.getSection(),
                    new Vector3[] { _calcVec3A, _calcVec3B, _calcVec3C });
            picked.localToWorld(_calcVec3A, _calcVec3A);
            picked.localToWorld(_calcVec3B, _calcVec3B);
            picked.localToWorld(_calcVec3C, _calcVec3C);
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
