/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.extension;

import java.io.IOException;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <p>
 * <code>BillboardNode</code> defines a node that will attempt to orient itself in relation to the current camera. The
 * way it does this depends on the alignment type set via {@link #setAlignment(BillboardAlignment)}.
 * </p>
 * <p>
 * <code>BillboardNode</code> is often a useful way fake complex distant geometry using a single quad that has an image
 * applied to it. This quad, with the texture, will appear to be a full model at great distances, and save on rendering
 * and memory.
 * </p>
 * <p>
 * It is also worth noting that you can use any geometry with this node, not just quads.
 * </p>
 */
public class BillboardNode extends Node {

    private final Matrix3 _orient = new Matrix3(Matrix3.IDENTITY);
    private Matrix3 _localRot = null;

    private final Vector3 _look = new Vector3(Vector3.ZERO);

    private final Vector3 _left = new Vector3(Vector3.ZERO);

    // Used to denote whether our billboard updates should also request bounds updates.
    private boolean _updateBounds = true;

    /**
     * Method of alignment to use. See individual enum values for details.
     */
    public enum BillboardAlignment {
        /**
         * Do not change the node's rotation. Useful for situations where you can not remove a billboard node from a
         * hierarchy, but do not want the rotation to change.
         */
        None,

        /**
         * Rotate the billboard so it points directly opposite the direction the camera's facing.
         */
        ScreenAligned,

        /**
         * Rotate the billboard so it points directly towards the camera's direction.
         */
        CameraAligned,

        /**
         * Rotate the billboard to face in the camera's direction by rotating around the X axis.
         */
        AxialX,

        /**
         * Rotate the billboard to face in the camera's direction by rotating around the Y axis.
         */
        AxialY,

        /**
         * Rotate the billboard to face in the camera's direction by rotating around the Z axis.
         */
        AxialZ
    }

    private BillboardAlignment _alignment;

    public BillboardNode() { /**/}

    /**
     * Constructor instantiates a new <code>BillboardNode</code>. The name of the node is supplied during construction.
     *
     * @param name
     *            the name of the node.
     */
    public BillboardNode(final String name) {
        super(name);
        _alignment = BillboardAlignment.ScreenAligned;
    }

    /**
     * <code>draw</code> updates the billboards orientation then renders the billboard's children.
     *
     * @param r
     *            the renderer used to draw.
     * @see com.ardor3d.scenegraph.Spatial#draw(com.ardor3d.renderer.Renderer)
     */
    @Override
    public void draw(final Renderer r) {
        rotateBillboard();

        super.draw(r);
    }

    /**
     * Normally a billboard triggers transform and bounds updates on its children. Setting this to false will only
     * trigger dirty flags for transform, potentially saving some expensive bounds calculations if those are deemed
     * unnecessary (for example, when using sphere bounds and quad billboards. Setting to false may cause odd culling
     * behavior.
     *
     * @param doUpdate
     *            true (the default) if we should request bounds updates for children, false if not.
     */
    public void setUpdateBounds(final boolean doUpdate) {
        _updateBounds = doUpdate;
    }

    /**
     * @return true if bounds are dirtied for children each frame.
     * @see #setUpdateBounds(boolean)
     */
    public boolean isUpdateBounds() {
        return _updateBounds;
    }

    public void setLocalRotation(final Matrix3 rot) {
        _localRot = rot;
    }

    public ReadOnlyMatrix3 getLocalRotation() {
        return _localRot;
    }

    /**
     * Rotate the billboard based on the type set and the currently set camera.
     *
     * @see Camera#getCurrentCamera()
     */
    public void rotateBillboard() {
        // get the scale, translation and rotation of the node in world space
        updateWorldTransform(false);

        switch (_alignment) {
            case ScreenAligned:
                rotateScreenAligned();
                break;
            case CameraAligned:
                rotateCameraAligned();
                break;
            case AxialX:
                rotateAxial(Vector3.UNIT_X);
                break;
            case AxialY:
                rotateAxial(Vector3.UNIT_Y);
                break;
            case AxialZ:
                rotateAxial(Vector3.UNIT_Z);
                break;
            case None:
                rotateNone();
                break;
        }

        if (_children == null) {
            return;
        }

        if (_updateBounds) {
            propagateDirtyDown(ON_DIRTY_TRANSFORM);
        } else {
            propagateDirtyDown(ON_DIRTY_TRANSFORM_ONLY);
        }

        for (int i = 0, cSize = getNumberOfChildren(); i < cSize; i++) {
            final Spatial child = getChild(i);
            if (child != null) {
                child.updateGeometricState(0, false);
            }
        }
    }

    private void rotateNone() {
        if (_localRot != null) {
            _orient.set(getRotation());
            _orient.multiplyLocal(_localRot);
            _worldTransform.setRotation(_orient);
        }
    }

    /**
     * Aligns this Billboard Node so that it points to the camera position.
     */
    private void rotateCameraAligned() {
        final Camera camera = Camera.getCurrentCamera();
        _look.set(camera.getLocation()).subtractLocal(_worldTransform.getTranslation()).normalizeLocal();
        _left.set(camera.getUp()).crossLocal(_look);
        final Vector3 up = Vector3.fetchTempInstance();
        up.set(_look).crossLocal(_left);
        _orient.fromAxes(_left, up, _look);
        if (_localRot != null) {
            _orient.multiplyLocal(_localRot);
        }
        _worldTransform.setRotation(_orient);
        Vector3.releaseTempInstance(up);
    }

    /**
     * Rotate the billboard so it points directly opposite the direction the camera's facing
     */
    private void rotateScreenAligned() {
        final Camera camera = Camera.getCurrentCamera();
        _look.set(camera.getDirection()).negateLocal();
        _left.set(camera.getLeft()).negateLocal();
        _orient.fromAxes(_left, camera.getUp(), _look);
        if (_localRot != null) {
            _orient.multiplyLocal(_localRot);
        }
        _worldTransform.setRotation(_orient);
    }

    /**
     * Rotate the billboard towards the current camera, but keeping a given axis fixed.
     */
    private void rotateAxial(final ReadOnlyVector3 axis) {
        final Camera camera = Camera.getCurrentCamera();

        // Compute the additional rotation required for the billboard to face
        // the camera. To do this, the camera must be inverse-transformed into
        // the model space of the billboard.
        _look.set(camera.getLocation()).subtractLocal(_worldTransform.getTranslation());
        final Matrix3 worldMatrix = Matrix3.fetchTempInstance().set(_worldTransform.getMatrix());
        worldMatrix.applyPost(_look, _left); // coopt left for our own purposes.
        final ReadOnlyVector3 scale = _worldTransform.getScale();
        _left.divideLocal(scale);

        // squared length of the camera projection in the xz-plane
        final double lengthSquared = _left.getX() * _left.getX() + _left.getZ() * _left.getZ();
        if (lengthSquared < MathUtils.EPSILON) {
            // camera on the billboard axis, rotation not defined
            return;
        }

        // unitize the projection
        final double invLength = 1.0 / Math.sqrt(lengthSquared);
        if (axis.getY() == 1) {
            _left.setX(_left.getX() * invLength);
            _left.setY(0.0);
            _left.setZ(_left.getZ() * invLength);

            // compute the local orientation matrix for the billboard
            _orient.setValue(0, 0, _left.getZ());
            _orient.setValue(0, 1, 0);
            _orient.setValue(0, 2, _left.getX());
            _orient.setValue(1, 0, 0);
            _orient.setValue(1, 1, 1);
            _orient.setValue(1, 2, 0);
            _orient.setValue(2, 0, -_left.getX());
            _orient.setValue(2, 1, 0);
            _orient.setValue(2, 2, _left.getZ());
        } else if (axis.getZ() == 1) {
            _left.setX(_left.getX() * invLength);
            _left.setY(_left.getY() * invLength);
            _left.setZ(0.0);
            _left.normalizeLocal();

            // compute the local orientation matrix for the billboard
            _orient.setValue(0, 0, -_left.getY());
            _orient.setValue(0, 1, -_left.getX());
            _orient.setValue(0, 2, 0);
            _orient.setValue(1, 0, _left.getX());
            _orient.setValue(1, 1, -_left.getY());
            _orient.setValue(1, 2, 0);
            _orient.setValue(2, 0, 0);
            _orient.setValue(2, 1, 0);
            _orient.setValue(2, 2, 1);
        } else if (axis.getX() == 1) {
            _left.setX(0.0);
            _left.setY(_left.getY() * invLength);
            _left.setZ(_left.getZ() * invLength);
            _left.normalizeLocal();

            // compute the local orientation matrix for the billboard
            _orient.setValue(0, 0, 1);
            _orient.setValue(0, 1, 0);
            _orient.setValue(0, 2, 0);
            _orient.setValue(1, 0, 0);
            _orient.setValue(1, 1, _left.getZ());
            _orient.setValue(1, 2, _left.getY());
            _orient.setValue(2, 0, 0);
            _orient.setValue(2, 1, -_left.getY());
            _orient.setValue(2, 2, _left.getZ());
        }

        // The billboard must be oriented to face the camera before it is
        // transformed into the world.
        if (_localRot != null) {
            _orient.multiplyLocal(_localRot);
        }
        worldMatrix.multiplyLocal(_orient);
        _worldTransform.setRotation(worldMatrix);
        Matrix3.releaseTempInstance(worldMatrix);
    }

    /**
     * Returns the alignment this BillboardNode is set to.
     *
     * @return The alignment of rotation.
     */
    public BillboardAlignment getAlignment() {
        return _alignment;
    }

    /**
     * Sets the type of rotation this BillboardNode will have. The alignment can be ScreenAligned, CameraAligned, AxialY
     * or AxialZ. Invalid alignments will assume no billboard rotation.
     */
    public void setAlignment(final BillboardAlignment alignment) {
        _alignment = alignment;
        _worldTransform.setRotation(Matrix3.IDENTITY);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_orient, "orient", new Matrix3());
        capsule.write(_look, "look", (Vector3) Vector3.ZERO);
        capsule.write(_left, "left", (Vector3) Vector3.ZERO);
        capsule.write(_alignment, "alignment", BillboardAlignment.ScreenAligned);
        capsule.write(_updateBounds, "updateBounds", true);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _orient.set(capsule.readSavable("orient", (Matrix3) Matrix3.IDENTITY));
        _look.set(capsule.readSavable("look", (Vector3) Vector3.ZERO));
        _left.set(capsule.readSavable("left", (Vector3) Vector3.ZERO));
        _alignment = capsule.readEnum("alignment", BillboardAlignment.class, BillboardAlignment.ScreenAligned);
        _updateBounds = capsule.readBoolean("updateBounds", true);
    }
}