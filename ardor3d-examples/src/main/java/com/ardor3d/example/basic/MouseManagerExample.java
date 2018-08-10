/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.basic;

import java.io.IOException;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.GrabbedState;
import com.ardor3d.input.Key;
import com.ardor3d.input.MouseButton;
import com.ardor3d.input.MouseCursor;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.MouseButtonPressedCondition;
import com.ardor3d.input.logical.MouseButtonReleasedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.ColorMaterial;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * A demonstration of the MouseManager class, which is used to control properties (e.g. cursor, location) of the native
 * mouse.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.basic.MouseManagerExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_MouseManagerExample.jpg", //
maxHeapMemory = 64)
public class MouseManagerExample extends ExampleBase {

    private Mesh t;
    private final Matrix3 rotate = new Matrix3();
    private double angle = 0;
    private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();

    private boolean useCursorOne = true;
    private MouseCursor _cursor1;
    private MouseCursor _cursor2;

    public static void main(final String[] args) {
        start(MouseManagerExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        if (timer.getTimePerFrame() < 1) {
            angle = angle + (timer.getTimePerFrame() * 25);
            if (angle > 360) {
                angle = 0;
            }
        }

        rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
        t.setRotation(rotate);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Mouse Manager Example");

        final Vector3 max = new Vector3(5, 5, 5);
        final Vector3 min = new Vector3(-5, -5, -5);

        t = new Box("Box", min, max);
        t.setModelBound(new BoundingBox());
        t.setTranslation(new Vector3(0, 0, -15));
        _root.attachChild(t);

        t.setRandomColors();

        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));

        final MaterialState ms = new MaterialState();
        ms.setColorMaterial(ColorMaterial.Diffuse);
        _root.setRenderState(ms);

        _root.setRenderState(ts);

        final AWTImageLoader awtImageLoader = new AWTImageLoader();

        try {
            _cursor1 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/wait_cursor.png");
            _cursor2 = createMouseCursor(awtImageLoader, "com/ardor3d/example/media/input/movedata.gif");

            _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.H), new TriggerAction() {
                public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                    if (useCursorOne) {
                        _mouseManager.setCursor(_cursor1);
                    } else {
                        _mouseManager.setCursor(_cursor2);
                    }
                    useCursorOne = !useCursorOne;
                }
            }));

            _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.J), new TriggerAction() {
                public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                    _mouseManager.setCursor(MouseCursor.SYSTEM_DEFAULT);
                }
            }));
            _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.K), new TriggerAction() {
                public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                    if (_mouseManager.isSetPositionSupported()) {
                        _mouseManager.setPosition(0, 0);
                    }
                }
            }));

            _logicalLayer.registerTrigger(new InputTrigger(new MouseButtonPressedCondition(MouseButton.LEFT),
                    new TriggerAction() {
                        public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                            if (_mouseManager.isSetGrabbedSupported()) {
                                _mouseManager.setGrabbed(GrabbedState.GRABBED);
                            }
                        }
                    }));
            _logicalLayer.registerTrigger(new InputTrigger(new MouseButtonReleasedCondition(MouseButton.LEFT),
                    new TriggerAction() {
                        public void perform(final Canvas source, final TwoInputStates inputState, final double tpf) {
                            if (_mouseManager.isSetGrabbedSupported()) {
                                _mouseManager.setGrabbed(GrabbedState.NOT_GRABBED);
                            }
                        }
                    }));

        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

    }

    private MouseCursor createMouseCursor(final AWTImageLoader awtImageLoader, final String resourceName)
            throws IOException {
        final Image image = awtImageLoader.load(ResourceLocatorTool.getClassPathResourceAsStream(
                MouseManagerExample.class, resourceName), true);

        return new MouseCursor("cursor1", image, 0, image.getHeight() - 1);
    }
}