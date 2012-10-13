/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>BillboardNode</code> defines a node that always orients towards the camera. However, it does not tilt up/down
 * as the camera rises. This keep geometry from appearing to fall over if the camera rises or lowers.
 * <code>BillboardNode</code> is useful to contain a single quad that has a image applied to it for lowest detail
 * models. This quad, with the texture, will appear to be a full model at great distances, and save on rendering and
 * memory. It is important to note that for AXIAL mode, the billboards orientation will always be up (0,1,0). This means
 * that a "standard" ardor3d camera with up (0,1,0) is the only camera setting compatible with AXIAL mode.
 */
public class BillboardNode extends Node {

    private double _lastTime;

    private final Matrix3 _orient = new Matrix3(Matrix3.IDENTITY);

    private final Vector3 _look = new Vector3(Vector3.ZERO);

    private final Vector3 _left = new Vector3(Vector3.ZERO);

    public enum BillboardAlignment {
        ScreenAligned, CameraAligned, AxialY, AxialZ
    }

    private BillboardAlignment _alignment;

    public BillboardNode() {}

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

    @Override
    public void updateWorldTransform(final boolean recurse) {
        _lastTime = 0; // time
        super.updateWorldTransform(recurse);
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
     * rotate the billboard based on the type set
     * 
     * @param cam
     *            Camera
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
            case AxialY:
                rotateAxial(new Vector3(Vector3.UNIT_Y));
                break;
            case AxialZ:
                rotateAxial(new Vector3(Vector3.UNIT_Z));
                break;
        }

        if (_children == null) {
            return;
        }

        propagateDirtyDown(ON_DIRTY_TRANSFORM);
        for (int i = 0, cSize = getNumberOfChildren(); i < cSize; i++) {
            final Spatial child = getChild(i);
            if (child != null) {
                child.updateGeometricState(_lastTime, false);
            }
        }
    }

    /**
     * Aligns this Billboard Node so that it points to the camera position.
     * 
     * @param camera
     *            Camera
     */
    private void rotateCameraAligned() {
        final Camera camera = Camera.getCurrentCamera();
        _look.set(camera.getLocation()).subtractLocal(_worldTransform.getTranslation());
        // coopt left for our own purposes.
        final Vector3 xzp = _left;
        // The xzp vector is the projection of the look vector on the xz plane
        xzp.set(_look.getX(), 0, _look.getZ());

        // check for undefined rotation...
        if (xzp.equals(Vector3.ZERO)) {
            return;
        }

        _look.normalizeLocal();
        xzp.normalizeLocal();
        final double cosp = _look.dot(xzp);

        // compute the local orientation matrix for the billboard
        _orient.setValue(0, 0, xzp.getZ());
        _orient.setValue(0, 1, xzp.getX() * -_look.getY());
        _orient.setValue(0, 2, xzp.getX() * cosp);
        _orient.setValue(1, 0, 0);
        _orient.setValue(1, 1, cosp);
        _orient.setValue(1, 2, _look.getY());
        _orient.setValue(2, 0, -xzp.getX());
        _orient.setValue(2, 1, xzp.getZ() * -_look.getY());
        _orient.setValue(2, 2, xzp.getZ() * cosp);

        // The billboard must be oriented to face the camera before it is
        // transformed into the world.
        final Matrix3 mat = Matrix3.fetchTempInstance().set(_worldTransform.getMatrix()).multiplyLocal(_orient);
        _worldTransform.setRotation(mat);
        Matrix3.releaseTempInstance(mat);
    }

    /**
     * Rotate the billboard so it points directly opposite the direction the camera's facing
     * 
     * @param camera
     *            Camera
     */
    private void rotateScreenAligned() {
        final Camera camera = Camera.getCurrentCamera();
        // coopt diff for our in direction:
        _look.set(camera.getDirection()).negateLocal();
        // coopt loc for our left direction:
        _left.set(camera.getLeft()).negateLocal();
        _orient.fromAxes(_left, camera.getUp(), _look);
        _worldTransform.setRotation(_orient);
    }

    /**
     * Rotate the billboard towards the camera, but keeping a given axis fixed.
     * 
     * @param camera
     *            Camera
     */
    private void rotateAxial(final Vector3 axis) {
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

            // compute the local orientation matrix for the billboard
            _orient.setValue(0, 0, _left.getY());
            _orient.setValue(0, 1, _left.getX());
            _orient.setValue(0, 2, 0);
            _orient.setValue(1, 0, -_left.getY());
            _orient.setValue(1, 1, _left.getX());
            _orient.setValue(1, 2, 0);
            _orient.setValue(2, 0, 0);
            _orient.setValue(2, 1, 0);
            _orient.setValue(2, 2, 1);
        }

        // The billboard must be oriented to face the camera before it is
        // transformed into the world.
        worldMatrix.multiplyLocal(_orient);
        _worldTransform.setRotation(worldMatrix);
        Matrix3.releaseTempInstance(worldMatrix);
    }

    /**
     * Returns the alignment this BillboardNode is set too.
     * 
     * @return The alignment of rotation, ScreenAligned, CameraAligned, AxialY or AxialZ.
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
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_orient, "orient", new Matrix3());
        capsule.write(_look, "look", new Vector3(Vector3.ZERO));
        capsule.write(_left, "left", new Vector3(Vector3.ZERO));
        capsule.write(_alignment, "alignment", BillboardAlignment.ScreenAligned);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _orient.set((Matrix3) capsule.readSavable("orient", new Matrix3(Matrix3.IDENTITY)));
        _look.set((Vector3) capsule.readSavable("look", new Vector3(Vector3.ZERO)));
        _left.set((Vector3) capsule.readSavable("left", new Vector3(Vector3.ZERO)));
        _alignment = capsule.readEnum("alignment", BillboardAlignment.class, BillboardAlignment.ScreenAligned);
    }
}