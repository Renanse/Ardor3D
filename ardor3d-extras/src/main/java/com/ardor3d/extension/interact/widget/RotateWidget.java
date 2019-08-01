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

import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture2D;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.MouseCursor;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.ZBufferState.TestFunction;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.MaterialUtil;

public class RotateWidget extends AbstractInteractWidget {

    protected Matrix3 _calcMat3 = new Matrix3();
    protected Matrix3 _rotateStore = new Matrix3();

    protected InteractRing _lastRing = null;

    protected InteractRing _xRing = null;
    protected InteractRing _yRing = null;
    protected InteractRing _zRing = null;

    public static MouseCursor DEFAULT_CURSOR = null;

    public RotateWidget(final IFilterList filterList) {
        super(filterList);
        _handle = new Node("rotationHandle");

        final ZBufferState zstate = new ZBufferState();
        zstate.setFunction(TestFunction.LessThanOrEqualTo);
        _handle.setRenderState(zstate);

        _handle.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
        _handle.updateGeometricState(0);

        if (RotateWidget.DEFAULT_CURSOR != null) {
            setMouseOverCallback(new SetCursorCallback(RotateWidget.DEFAULT_CURSOR));
        }
    }

    /**
     * Call this after creating the rings you want to use.
     *
     * @param texture
     * @return
     */
    public void setTexture(final Texture2D texture) {
        if (_xRing != null) {
            _xRing.setTexture(texture);
        }
        if (_yRing != null) {
            _yRing.setTexture(texture);
        }
        if (_zRing != null) {
            _zRing.setTexture(texture);
        }
    }

    @Override
    protected void mouseEntered(final Canvas source, final MouseState current, final InteractManager manager) {
        super.mouseEntered(source, current, manager);
        if (_lastMouseOverSpatial != null) {
            updateBlendStates(_lastMouseOverSpatial);
        }
    }

    @Override
    protected void mouseDeparted(final Canvas source, final MouseState current, final InteractManager manager) {
        super.mouseDeparted(source, current, manager);
        updateBlendStates(null);
    }

    protected void updateBlendStates(final Spatial highlight) {
        _handle.acceptVisitor((final Spatial spatial) -> {
            final BlendState bs = (BlendState) spatial.getLocalRenderState(StateType.Blend);
            if (bs != null) {
                bs.setSourceFunction(spatial == highlight ? SourceFunction.One : SourceFunction.SourceAlpha);
            }
        }, true);
        _handle.updateGeometricState(0);
    }

    public RotateWidget withXAxis() {
        return withXAxis(new ColorRGBA(1, 0, 0, .65f));
    }

    public RotateWidget withXAxis(final ReadOnlyColorRGBA color) {
        return withXAxis(color, 1.0f, 0.15f);
    }

    public RotateWidget withXAxis(final ReadOnlyColorRGBA color, final float scale, final float width) {
        if (_xRing != null) {
            _xRing.removeFromParent();
        }
        _xRing = new InteractRing("xRotRing", 4, 32, scale, width);
        _xRing.setDefaultColor(color);
        final Quaternion rotate = new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.UNIT_Y);
        _xRing.getMeshData().rotatePoints(rotate);
        _xRing.getMeshData().rotateNormals(rotate);
        _handle.attachChild(_xRing);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        _xRing.setRenderState(blend);
        MaterialUtil.autoMaterials(_xRing);

        return this;
    }

    public RotateWidget withYAxis() {
        return withYAxis(new ColorRGBA(0, 1, 0, .65f));
    }

    public RotateWidget withYAxis(final ReadOnlyColorRGBA color) {
        return withYAxis(color, 1.0f, 0.15f);
    }

    public RotateWidget withYAxis(final ReadOnlyColorRGBA color, final float scale, final float width) {
        if (_yRing != null) {
            _yRing.removeFromParent();
        }
        _yRing = new InteractRing("yRotRing", 4, 32, scale, width);
        _yRing.setDefaultColor(color);
        final Quaternion rotate = new Quaternion().fromAngleAxis(MathUtils.HALF_PI, Vector3.NEG_UNIT_X);
        _yRing.getMeshData().rotatePoints(rotate);
        _yRing.getMeshData().rotateNormals(rotate);
        _handle.attachChild(_yRing);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        _yRing.setRenderState(blend);
        MaterialUtil.autoMaterials(_yRing);

        return this;
    }

    public RotateWidget withZAxis() {
        return withZAxis(new ColorRGBA(0, 0, 1, .65f));
    }

    public RotateWidget withZAxis(final ReadOnlyColorRGBA color) {
        return withZAxis(color, 1.0f, 0.15f);
    }

    public RotateWidget withZAxis(final ReadOnlyColorRGBA color, final float scale, final float width) {
        if (_zRing != null) {
            _zRing.removeFromParent();
        }
        _zRing = new InteractRing("zRotRing", 4, 32, scale, width);
        _zRing.setDefaultColor(color);
        _handle.attachChild(_zRing);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        _zRing.setRenderState(blend);
        MaterialUtil.autoMaterials(_zRing);

        return this;
    }

    protected void setRingRotations(final ReadOnlyMatrix3 rot) {
        if (_xRing != null) {
            _xRing.setRotation(rot);
        }
        if (_yRing != null) {
            _yRing.setRotation(rot);
        }
        if (_zRing != null) {
            _zRing.setRotation(rot);
        }
    }

    @Override
    public void targetDataUpdated(final InteractManager manager) {
        final Spatial target = manager.getSpatialTarget();
        if (target == null) {
            setRingRotations(Matrix3.IDENTITY);
        } else {
            target.updateGeometricState(0);

            // update ring rotations from target
            if (_interactMatrix == InteractMatrix.Local) {
                setRingRotations(target.getWorldRotation());
            } else {
                setRingRotations(Matrix3.IDENTITY);
                if (_lastRing != null) {
                    _lastRing.setRotation(_rotateStore);
                }
            }
        }

        _handle.setScale(calculateHandleScale(manager));
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

        if (current.getButtonsReleasedSince(previous).contains(_dragButton)) {
            _rotateStore.setIdentity();
            if (_interactMatrix != InteractMatrix.Local) {
                setRingRotations(Matrix3.IDENTITY);
            }
        }

        // Now check drag status
        if (!checkShouldDrag(camera, current, previous, inputConsumed, manager)) {
            return;
        }

        // act on drag
        if (_lastDragSpatial instanceof InteractRing) {
            _lastRing = (InteractRing) _lastDragSpatial;

            final Vector2 oldMouse = new Vector2(previous.getX(), previous.getY());
            final ReadOnlyQuaternion rot = getNewAxisRotation(_lastRing, oldMouse, current, camera, manager);
            final Transform transform = manager.getSpatialState().getTransform();
            rot.toRotationMatrix(_calcMat3).multiply(transform.getMatrix(), _calcMat3);
            transform.setRotation(_calcMat3);

            // apply our filters, if any, now that we've made updates.
            applyFilters(manager);
        }
    }

    protected ReadOnlyQuaternion getNewAxisRotation(final InteractRing ring, final Vector2 oldMouse,
            final MouseState current, final Camera camera, final InteractManager manager) {
        // calculate a plane running through the ring we picked
        _calcVec3A.set(_handle.getWorldTranslation());
        if (ring == _zRing || ring == _yRing) {
            _calcVec3B.set(Vector3.UNIT_X);
        } else {
            _calcVec3B.set(Vector3.UNIT_Z);
        }

        if (ring == _zRing || ring == _xRing) {
            _calcVec3C.set(Vector3.UNIT_Y);
        } else {
            _calcVec3C.set(Vector3.UNIT_Z);
        }

        // rotate to ring plane
        ring.getRotation().applyPost(_calcVec3B, _calcVec3B);
        ring.getRotation().applyPost(_calcVec3C, _calcVec3C);

        // make plane object
        final Plane pickPlane = new Plane().setPlanePoints(_calcVec3A, _calcVec3B.addLocal(_calcVec3A),
                _calcVec3C.addLocal(_calcVec3A));

        // find out where we were hitting the plane before
        getPickRay(oldMouse, camera);
        if (!_calcRay.intersectsPlane(pickPlane, _calcVec3A)) {
            return Quaternion.IDENTITY;
        }

        // find out where we are hitting the plane now
        getPickRay(new Vector2(current.getX(), current.getY()), camera);
        if (!_calcRay.intersectsPlane(pickPlane, _calcVec3B)) {
            return Quaternion.IDENTITY;
        }

        // convert to vectors
        _calcVec3A.subtractLocal(_handle.getWorldTranslation());
        _calcVec3B.subtractLocal(_handle.getWorldTranslation());

        // apply to our interact matrix if used
        if (_interactMatrix == InteractMatrix.World) {
            _rotateStore.multiplyLocal(
                    new Quaternion().fromVectorToVector(_calcVec3A, _calcVec3B).toRotationMatrix(_calcMat3));
        }

        // convert to target coord space
        final Node parent = manager.getSpatialTarget().getParent();
        if (parent != null) {
            parent.getWorldTransform().applyInverseVector(_calcVec3A);
            parent.getWorldTransform().applyInverseVector(_calcVec3B);
        }

        // return a rotation to take us to the new rotation
        return new Quaternion().fromVectorToVector(_calcVec3A, _calcVec3B);
    }

    @Override
    public void setInteractMatrix(final InteractMatrix matrix) {
        if (_interactMatrix != matrix) {
            _lastRing = null;
            _interactMatrix = matrix;
        }
    }

    public InteractRing getXRing() {
        return _xRing;
    }

    public InteractRing getYRing() {
        return _yRing;
    }

    public InteractRing getZRing() {
        return _zRing;
    }
}
