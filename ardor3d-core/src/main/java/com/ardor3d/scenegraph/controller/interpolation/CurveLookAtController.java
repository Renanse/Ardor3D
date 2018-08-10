/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

/**
 * 
 */

package com.ardor3d.scenegraph.controller.interpolation;

import java.io.Serializable;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;

/**
 * CurveLookAtController class rotates a spatial to 'look at' a curve.
 * <p>
 * This class assumes the given delegate curve interpolation controller is already added to a spatial and is getting
 * automatically updated as part of the main loop. Therefore this class doesn't call update on the delegate controller.
 * </p>
 */
public class CurveLookAtController implements SpatialController<Spatial>, Serializable {

    /** Serial UID */
    private static final long serialVersionUID = 1L;

    /** The world up vector to use in matrix look at */
    private ReadOnlyVector3 _worldUp;

    /** The curve interpolation controller that does the work of finding the correct position to look at */
    private final CurveInterpolationController _curveController;

    /** The previous location of the spatial on the curve */
    private final Vector3 _previous;

    /** @see #setLocalRotation(boolean) */
    private boolean _localRotation = true;

    /**
     * Creates a new instance of <code>CurveLookAtController</code>, with {@link Vector3#UNIT_Y} as the world up vector.
     * 
     * @param curveController
     *            The curve interpolation controller that does the work of finding the correct position to look at, can
     *            not be <code>null</code>.
     */
    public CurveLookAtController(final CurveInterpolationController curveController) {
        this(curveController, Vector3.UNIT_Y);
    }

    /**
     * Creates a new instance of <code>CurveLookAtController</code>.
     * 
     * @param curveController
     *            The curve interpolation controller that does the work of finding the correct position to look at, can
     *            not be <code>null</code>.
     * @param worldUp
     *            The world up vector, can not be <code>null</code>.
     */
    public CurveLookAtController(final CurveInterpolationController curveController, final ReadOnlyVector3 worldUp) {
        super();

        if (null == curveController) {
            throw new IllegalArgumentException("curveController can not be null!");
        }

        _curveController = curveController;

        _previous = new Vector3(_curveController.getControlFrom());

        setWorldUp(worldUp);
    }

    @Override
    public void update(final double time, final Spatial caller) {
        if (null == caller) {
            throw new IllegalArgumentException("caller can not be null!");
        }

        final Vector3 interpolated = Vector3.fetchTempInstance();
        final Matrix3 rotation = Matrix3.fetchTempInstance();

        _curveController.interpolateVectors(_curveController.getControlFrom(), _curveController.getControlTo(),
                _curveController.getDelta(), interpolated);

        MathUtils.matrixLookAt(_previous, interpolated, _worldUp, rotation);

        if (isLocalRotation()) {
            caller.setRotation(rotation);
        } else {
            caller.setWorldRotation(rotation);
        }

        _previous.set(interpolated);

        Matrix3.releaseTempInstance(rotation);
        Vector3.releaseTempInstance(interpolated);
    }

    /**
     * @param worldUp
     *            The world up vector, can not be <code>null</code>.
     */
    public void setWorldUp(final ReadOnlyVector3 worldUp) {
        if (null == worldUp) {
            throw new IllegalArgumentException("worldUp can not be null!");
        }

        _worldUp = worldUp;
    }

    /**
     * @param localRotation
     *            <code>true</code> to update local rotation, <code>false</code> to update world rotation.
     * @see #isLocalRotation()
     */
    public void setLocalRotation(final boolean localRotation) {
        _localRotation = localRotation;
    }

    /**
     * @return <code>true</code> if the local rotation is being updated, <code>false</code> if the world rotation is.
     * @see #setLocalRotation(boolean)
     */
    public boolean isLocalRotation() {
        return _localRotation;
    }

}
