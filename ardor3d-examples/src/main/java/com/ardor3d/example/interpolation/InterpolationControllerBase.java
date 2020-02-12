/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.interpolation;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.controller.ComplexSpatialController.RepeatType;
import com.ardor3d.scenegraph.controller.interpolation.CurveInterpolationController;
import com.ardor3d.scenegraph.controller.interpolation.CurveLookAtController;
import com.ardor3d.scenegraph.controller.interpolation.InterpolationController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.TextureManager;

public abstract class InterpolationControllerBase<C extends InterpolationController<?, ?>> extends ExampleBase {

    /** Keep a reference to the box to be able to rotate it each frame. */
    private Mesh box;

    @Override
    protected void initExample() {
        _canvas.setTitle("Interpolation Controller Example");

        // Create a new box to interpolate (see BoxExample for more info)
        box = new Box("Box", new Vector3(0, 0, 0), 5, 5, 5);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(0, 0, -15));
        _root.attachChild(box);

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        box.setRenderState(ts);

        // Create our controller
        final C controller = createController();
        controller.setRepeatType(RepeatType.WRAP);

        box.addController(controller);

        // If it's a curve interpolation controller also add a look at controller
        if (controller instanceof CurveInterpolationController) {
            box.addController(new CurveLookAtController((CurveInterpolationController) controller));
        }

        // Add some text for informational purposes
        final BasicText speedText = BasicText.createDefaultTextLabel("text", getSpeedText(controller));
        speedText.setTranslation(5, 5, 0);
        _root.attachChild(speedText);

        final BasicText repeatText = BasicText.createDefaultTextLabel("text", getWrapText(controller));
        repeatText.setTranslation(5, 10 + speedText.getHeight(), 0);
        _root.attachChild(repeatText);

        // Add a trigger to change the repeat type on the controller
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                switch (controller.getRepeatType()) {
                    case CLAMP:
                        controller.setRepeatType(RepeatType.CYCLE);
                        break;
                    case CYCLE:
                        controller.setRepeatType(RepeatType.WRAP);
                        break;
                    case WRAP:
                        controller.setRepeatType(RepeatType.CLAMP);
                        break;
                }
                repeatText.setText(getWrapText(controller));
            }
        }));

        // Add a slow down command
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.LEFT_BRACKET), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                controller.setSpeed(getNewSpeed(false, controller));
                speedText.setText(getSpeedText(controller));
            }
        }));

        // Add a speed up command
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.RIGHT_BRACKET), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                controller.setSpeed(getNewSpeed(true, controller));
                speedText.setText(getSpeedText(controller));
            }
        }));
    }

    private double getNewSpeed(final boolean dir, final C controller) {
        double newSpeed = Math.round((controller.getSpeed() + (dir ? .1 : -.1)) * 10) / 10.;
        if (newSpeed < 0) {
            newSpeed = 0;
        }
        return newSpeed;
    }

    private String getWrapText(final C controller) {
        return "Repeat type = " + controller.getRepeatType() + " (change with 'R')";
    }

    private String getSpeedText(final C controller) {
        return "Current speed: " + controller.getSpeed() + " (change with '[' or ']')";
    }

    /**
     * Implemented by sub classes to return a concrete controller.
     *
     * @return The controller to test, can not be null (otherwise a null pointer exception will be thrown).
     */
    protected abstract C createController();
}
