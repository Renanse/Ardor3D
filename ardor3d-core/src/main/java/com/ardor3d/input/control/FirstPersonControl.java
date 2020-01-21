/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.input.control;

import java.util.function.Predicate;

import com.ardor3d.framework.Canvas;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TriggerConditions;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;

public class FirstPersonControl {

    protected final Vector3 _upAxis = new Vector3();
    protected double _mouseRotateSpeed = .005;
    protected double _moveSpeed = 50;
    protected double _keyRotateSpeed = 2.25;
    protected final Matrix3 _workerMatrix = new Matrix3();
    protected final Vector3 _workerStoreA = new Vector3();
    protected InputTrigger _mouseTrigger;
    protected InputTrigger _keyTrigger;

    protected boolean _clampVerticalAngle = false;
    protected double _minVerticalAngle = -60 * MathUtils.DEG_TO_RAD;
    protected double _maxVerticalAngle = 60 * MathUtils.DEG_TO_RAD;

    public FirstPersonControl(final ReadOnlyVector3 upAxis) {
        _upAxis.set(upAxis);
    }

    public ReadOnlyVector3 getUpAxis() {
        return _upAxis;
    }

    public void setUpAxis(final ReadOnlyVector3 upAxis) {
        _upAxis.set(upAxis);
    }

    public double getMouseRotateSpeed() {
        return _mouseRotateSpeed;
    }

    public void setMouseRotateSpeed(final double speed) {
        _mouseRotateSpeed = speed;
    }

    public double getMoveSpeed() {
        return _moveSpeed;
    }

    public void setMoveSpeed(final double speed) {
        _moveSpeed = speed;
    }

    public double getKeyRotateSpeed() {
        return _keyRotateSpeed;
    }

    public void setKeyRotateSpeed(final double speed) {
        _keyRotateSpeed = speed;
    }

    protected void move(final Camera camera, final KeyboardState kb, final double tpf) {
        // MOVEMENT
        int moveFB = 0, strafeLR = 0;
        if (kb.isDown(Key.W)) {
            moveFB += 1;
        }
        if (kb.isDown(Key.S)) {
            moveFB -= 1;
        }
        if (kb.isDown(Key.A)) {
            strafeLR += 1;
        }
        if (kb.isDown(Key.D)) {
            strafeLR -= 1;
        }

        if (moveFB != 0 || strafeLR != 0) {
            final Vector3 loc = _workerStoreA.zero();
            if (moveFB == 1) {
                loc.addLocal(camera.getDirection());
            } else if (moveFB == -1) {
                loc.subtractLocal(camera.getDirection());
            }
            if (strafeLR == 1) {
                loc.addLocal(camera.getLeft());
            } else if (strafeLR == -1) {
                loc.subtractLocal(camera.getLeft());
            }
            loc.normalizeLocal().multiplyLocal(_moveSpeed * tpf).addLocal(camera.getLocation());
            camera.setLocation(loc);
        }

        // ROTATION
        int rotX = 0, rotY = 0;
        if (kb.isDown(Key.UP)) {
            rotY -= 1;
        }
        if (kb.isDown(Key.DOWN)) {
            rotY += 1;
        }
        if (kb.isDown(Key.LEFT)) {
            rotX += 1;
        }
        if (kb.isDown(Key.RIGHT)) {
            rotX -= 1;
        }
        if (rotX != 0 || rotY != 0) {
            rotate(camera, rotX * (_keyRotateSpeed / _mouseRotateSpeed) * tpf,
                    rotY * (_keyRotateSpeed / _mouseRotateSpeed) * tpf);
        }
    }

    protected void rotate(final Camera camera, final double dx, final double dy) {
        if (dx != 0) {
            applyDx(dx, camera);
        }

        if (dy != 0) {
            applyDY(dy, camera);
        }

        if (dx != 0 || dy != 0) {
            camera.normalize();
        }
    }

    private void applyDx(final double dx, final Camera camera) {
        _workerMatrix.fromAngleNormalAxis(_mouseRotateSpeed * dx, _upAxis);
        _workerMatrix.applyPost(camera.getLeft(), _workerStoreA);
        camera.setLeft(_workerStoreA);
        _workerMatrix.applyPost(camera.getDirection(), _workerStoreA);
        camera.setDirection(_workerStoreA);
        _workerMatrix.applyPost(camera.getUp(), _workerStoreA);
        camera.setUp(_workerStoreA);
    }

    private void applyDY(final double dy, final Camera camera) {
        // apply dy angle change to direction vector
        _workerMatrix.fromAngleNormalAxis(_mouseRotateSpeed * dy, camera.getLeft());
        _workerMatrix.applyPost(camera.getDirection(), _workerStoreA);
        camera.setDirection(_workerStoreA);

        // do we want to constrain our vertical angle?
        if (isClampVerticalAngle()) {
            // check if we went out of bounds and back up
            final double angleV = MathUtils.HALF_PI - _workerStoreA.smallestAngleBetween(_upAxis);
            if (angleV > getMaxVerticalAngle() || angleV < getMinVerticalAngle()) {
                // clamp the angle to our range
                final double newAngle = MathUtils.clamp(angleV, getMinVerticalAngle(), getMaxVerticalAngle());
                // take the difference in angles and back up the direction vector
                _workerMatrix.fromAngleNormalAxis(-(newAngle - angleV), camera.getLeft());
                _workerMatrix.applyPost(camera.getDirection(), _workerStoreA);
                camera.setDirection(_workerStoreA);
                // figure out new up vector by crossing direction and left.
                camera.getDirection().cross(camera.getLeft(), _workerStoreA);
                camera.setUp(_workerStoreA);
                return;
            }
        }

        // just apply to up vector
        _workerMatrix.applyPost(camera.getUp(), _workerStoreA);
        camera.setUp(_workerStoreA);
    }

    /**
     * @param layer
     *            the logical layer to register with
     * @param upAxis
     *            the up axis of the camera
     * @param dragOnly
     *            if true, mouse input will only rotate the camera if one of the mouse buttons (left, center or right)
     *            is down.
     * @return a new FirstPersonControl object
     */
    public static FirstPersonControl setupTriggers(final LogicalLayer layer, final ReadOnlyVector3 upAxis,
            final boolean dragOnly) {

        final FirstPersonControl control = new FirstPersonControl(upAxis);
        control.setupKeyboardTriggers(layer);
        control.setupMouseTriggers(layer, dragOnly);
        return control;
    }

    /**
     * Deregister the triggers of the given FirstPersonControl from the given LogicalLayer.
     *
     * @param layer
     * @param control
     */
    public static void removeTriggers(final LogicalLayer layer, final FirstPersonControl control) {
        if (control._mouseTrigger != null) {
            layer.deregisterTrigger(control._mouseTrigger);
        }
        if (control._keyTrigger != null) {
            layer.deregisterTrigger(control._keyTrigger);
        }
    }

    public void setupMouseTriggers(final LogicalLayer layer, final boolean dragOnly) {
        // Mouse look
        final Predicate<TwoInputStates> dragged = TriggerConditions.mouseMoved()
                .and(TriggerConditions.leftButtonDown());
        final TriggerAction dragAction = new TriggerAction() {

            // Test boolean to allow us to ignore first mouse event. First event can wildly vary based on platform.
            private boolean firstPing = true;

            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final MouseState mouse = inputStates.getCurrent().getMouseState();
                if (mouse.getDx() != 0 || mouse.getDy() != 0) {
                    if (!firstPing) {
                        FirstPersonControl.this.rotate(source.getCanvasRenderer().getCamera(), -mouse.getDx(),
                                -mouse.getDy());
                    } else {
                        firstPing = false;
                    }
                }
            }
        };

        _mouseTrigger = new InputTrigger(dragOnly ? dragged : TriggerConditions.mouseMoved(), dragAction);
        layer.registerTrigger(_mouseTrigger);
    }

    public Predicate<TwoInputStates> setupKeyboardTriggers(final LogicalLayer layer) {
        // WASD control
        final Predicate<TwoInputStates> keysHeld = new Predicate<TwoInputStates>() {
            Key[] keys = new Key[] { Key.W, Key.A, Key.S, Key.D, Key.LEFT, Key.RIGHT, Key.UP, Key.DOWN };

            public boolean test(final TwoInputStates states) {
                for (final Key k : keys) {
                    if (states.getCurrent() != null && states.getCurrent().getKeyboardState().isDown(k)) {
                        return true;
                    }
                }
                return false;
            }
        };

        final TriggerAction moveAction = new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                FirstPersonControl.this.move(source.getCanvasRenderer().getCamera(),
                        inputStates.getCurrent().getKeyboardState(), tpf);
            }
        };
        _keyTrigger = new InputTrigger(keysHeld, moveAction);
        layer.registerTrigger(_keyTrigger);
        return keysHeld;
    }

    public InputTrigger getKeyTrigger() {
        return _keyTrigger;
    }

    public InputTrigger getMouseTrigger() {
        return _mouseTrigger;
    }

    public boolean isClampVerticalAngle() {
        return _clampVerticalAngle;
    }

    /**
     * @param clampVerticalAngle
     *            if true, the vertical angle of the camera is locked between the minimum and maximum angles (default is
     *            [-60, 60])
     */
    public void setClampVerticalAngle(final boolean clampVerticalAngle) {
        _clampVerticalAngle = clampVerticalAngle;
    }

    public double getMinVerticalAngle() {
        return _minVerticalAngle;
    }

    /**
     * @param minVerticalAngle
     *            the new minimum angle, in radians, to clamp our vertical angle to. Defaults to -60 degrees (in
     *            radians). Must be less than the max angle. Has no effect unless clampVerticalAngle is true.
     * @see #setClampVerticalAngle(boolean)
     */
    public void setMinVerticalAngle(final double minVerticalAngle) {
        _minVerticalAngle = minVerticalAngle;
    }

    public double getMaxVerticalAngle() {
        return _maxVerticalAngle;
    }

    /**
     *
     * @param maxVerticalAngle
     *            the new maximum angle, in radians, to clamp our vertical angle to. Defaults to +60 degrees (in
     *            radians). Must be less than the max angle. Has no effect unless clampVerticalAngle is true.
     * @see #setClampVerticalAngle(boolean)
     */
    public void setMaxVerticalAngle(final double maxVerticalAngle) {
        _maxVerticalAngle = maxVerticalAngle;
    }
}
