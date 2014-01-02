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

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * Defines a node that manages a {@link Camera} object, allowing it to be part of a scenegraph. The updateWorldTransform
 * method is overridden to adjust the managed camera's location and orientation using this Node's world translation and
 * the world rotation. The column 0 of the world rotation matrix is used for the camera left vector, column 1 is used
 * for the camera up vector, column 2 is used for the camera direction vector.
 */
public class CameraNode extends Node {

    private Camera _camera;

    public CameraNode() {}

    /**
     * Constructor instantiates a new <code>CameraNode</code> object setting the camera to use for the frame reference.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparison purposes.
     * @param camera
     *            the camera this node controls.
     */
    public CameraNode(final String name, final Camera camera) {
        super(name);
        _camera = camera;
    }

    /**
     * Forces rotation and translation of this node to be sync'd with the attached camera. (Assumes the node is in world
     * space.)
     */
    public void updateFromCamera() {
        final ReadOnlyVector3 camLeft = _camera.getLeft();
        final ReadOnlyVector3 camUp = _camera.getUp();
        final ReadOnlyVector3 camDir = _camera.getDirection();
        final ReadOnlyVector3 camLoc = _camera.getLocation();

        final Matrix3 rotation = Matrix3.fetchTempInstance();
        rotation.fromAxes(camLeft, camUp, camDir);

        setRotation(rotation);
        setTranslation(camLoc);

        Matrix3.releaseTempInstance(rotation);
    }

    /**
     * <code>setCamera</code> sets the camera that this node controls.
     * 
     * @param camera
     *            the camera that this node controls.
     */
    public void setCamera(final Camera camera) {
        _camera = camera;
    }

    /**
     * <code>getCamera</code> retrieves the camera object that this node controls.
     * 
     * @return the camera this node controls.
     */
    public Camera getCamera() {
        return _camera;
    }

    /**
     * <code>updateWorldTransform</code> updates the rotation and translation of this node, and sets the camera's frame
     * buffer to reflect the current view.
     */
    @Override
    public void updateWorldTransform(final boolean recurse) {
        super.updateWorldTransform(recurse);
        if (_camera != null) {
            final ReadOnlyVector3 worldTranslation = getWorldTranslation();
            final ReadOnlyMatrix3 worldRotation = getWorldRotation();
            _camera.setFrame(worldTranslation, worldRotation);
        }
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_camera, "camera", null);

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _camera = (Camera) capsule.readSavable("camera", null);

    }
}