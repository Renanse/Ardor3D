/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;

/**
 * Extension of Camera useful for tracking and updating a Stereo view camera. See
 * http://local.wasp.uwa.edu.au/~pbourke/miscellaneous/stereorender/ for a useful discussion on stereo viewing in
 * OpenGL.
 */
public class StereoCamera extends Camera {

    private boolean _sideBySideMode = false;

    private final Camera _leftCamera;
    private final Camera _rightCamera;

    private double _focalDistance = 100;
    private double _eyeSeparation = _focalDistance / 30;
    private double _aperture = 45 * MathUtils.DEG_TO_RAD;

    public StereoCamera() {
        this(100, 100);
    }

    /**
     * 
     * @param width
     * @param height
     * @param sideBySideMode
     */
    public StereoCamera(final int width, final int height) {
        super(width, height);
        _leftCamera = new Camera(width, height);
        _rightCamera = new Camera(width, height);
    }

    public StereoCamera(final Camera camera) {
        super(camera);
        _leftCamera = new Camera(camera);
        _rightCamera = new Camera(camera);
    }

    @Override
    public void resize(final int width, final int height) {
        super.resize(width, height);
        _leftCamera.resize(width, height);
        _rightCamera.resize(width, height);
    }

    /**
     * @return the sideBySideMode
     * @see #setSideBySideMode(boolean)
     */
    public boolean isSideBySideMode() {
        return _sideBySideMode;
    }

    /**
     * @param sideBySideMode
     *            If true, left camera will be set up to the left half of the render context and right camera to the
     *            right half. If false, the views are set up to be the full size of the render context.
     */
    public void setSideBySideMode(final boolean sideBySideMode) {
        _sideBySideMode = sideBySideMode;
        setupLeftRightCameras();
    }

    public void setupLeftRightCameras() {
        // Set viewport:
        // XXX: Could maybe make use of our current viewport?
        if (_sideBySideMode) {
            _leftCamera.setViewPort(0, .5, 0, 1);
            _rightCamera.setViewPort(.5, 1, 0, 1);
        } else {
            _leftCamera.setViewPort(0, 1, 0, 1);
            _rightCamera.setViewPort(0, 1, 0, 1);
        }

        // Set frustum:
        final double aspectRatio = (getWidth() / (double) getHeight() / (_sideBySideMode ? 2.0 : 1.0));
        final double halfView = getFrustumNear() * MathUtils.tan(_aperture / 2);

        final double top = halfView;
        final double bottom = -halfView;
        final double horizontalShift = 0.5 * _eyeSeparation * getFrustumNear() / _focalDistance;

        // LEFT:
        {
            final double left = -aspectRatio * halfView + horizontalShift;
            final double right = aspectRatio * halfView + horizontalShift;

            _leftCamera.setFrustum(getFrustumNear(), getFrustumFar(), left, right, top, bottom);
        }

        // RIGHT:
        {
            final double left = -aspectRatio * halfView - horizontalShift;
            final double right = aspectRatio * halfView - horizontalShift;

            _rightCamera.setFrustum(getFrustumNear(), getFrustumFar(), left, right, top, bottom);
        }
    }

    public void updateLeftRightCameraFrames() {
        // update camera frame
        final Vector3 rightDir = Vector3.fetchTempInstance();
        final Vector3 work = Vector3.fetchTempInstance();
        rightDir.set(getDirection()).crossLocal(getUp()).multiplyLocal(_eyeSeparation / 2.0);
        _leftCamera.setFrame(getLocation().subtract(rightDir, work), getLeft(), getUp(), getDirection());
        _rightCamera.setFrame(getLocation().add(rightDir, work), getLeft(), getUp(), getDirection());
        Vector3.releaseTempInstance(work);
        Vector3.releaseTempInstance(rightDir);
    }

    public void switchToLeftCamera(final Renderer r) {
        _leftCamera.update();
        _leftCamera.apply(r);
    }

    public void switchToRightCamera(final Renderer r) {
        _rightCamera.update();
        _rightCamera.apply(r);
    }

    /**
     * @return the leftCamera
     */
    public Camera getLeftCamera() {
        return _leftCamera;
    }

    /**
     * @return the rightCamera
     */
    public Camera getRightCamera() {
        return _rightCamera;
    }

    /**
     * @return the focalDistance
     */
    public double getFocalDistance() {
        return _focalDistance;
    }

    /**
     * @param focalDistance
     *            the focalDistance to set
     */
    public void setFocalDistance(final double focalDistance) {
        _focalDistance = focalDistance;
    }

    /**
     * @return the eyeSeparation
     */
    public double getEyeSeparation() {
        return _eyeSeparation;
    }

    /**
     * @param eyeSeparation
     *            the eyeSeparation to set
     */
    public void setEyeSeparation(final double eyeSeparation) {
        _eyeSeparation = eyeSeparation;
    }

    /**
     * @return the aperture
     */
    public double getAperture() {
        return _aperture;
    }

    /**
     * @param radians
     *            the horizontal field of view, in radians
     */
    public void setAperture(final double radians) {
        _aperture = radians;
    }
}
