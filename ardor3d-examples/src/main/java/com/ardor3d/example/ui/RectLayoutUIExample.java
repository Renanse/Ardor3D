/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.ui;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIScrollPanel;
import com.ardor3d.extension.ui.UITabbedPane;
import com.ardor3d.extension.ui.UITabbedPane.TabPlacement;
import com.ardor3d.extension.ui.backdrop.SolidBackdrop;
import com.ardor3d.extension.ui.border.EmptyBorder;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.RectLayout;
import com.ardor3d.extension.ui.layout.RectLayoutData;
import com.ardor3d.extension.ui.text.UIPasswordField;
import com.ardor3d.extension.ui.text.UITextArea;
import com.ardor3d.extension.ui.text.UITextField;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Illustrates how to display GUI primitives (e.g. RadioButton, Label, TabbedPane) on a canvas.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.ui.RectLayoutUIExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/ui_RectLayoutUIExample.jpg", //
maxHeapMemory = 64)
public class RectLayoutUIExample extends ExampleBase {

    UIHud hud;

    public static void main(final String[] args) {
        start(RectLayoutUIExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("RectLayout UI Example");

        add3DBox();

        final UIPanel panel1 = example1();
        final UIPanel panel2 = example2();
        final UIPanel panel3 = example3();
        final UIPanel panel4 = example4();

        final UITabbedPane pane = new UITabbedPane(TabPlacement.NORTH);
        pane.setMinimumContentSize(300, 200);
        pane.add(panel1, "1");
        pane.add(panel2, "2");
        pane.add(panel3, "3");
        pane.add(panel4, "4");

        final UIFrame frame = new UIFrame("Examples");
        frame.setContentPanel(pane);
        frame.pack();

        hud = new UIHud(_canvas);
        hud.add(frame);
        hud.setupInput(_physicalLayer, _logicalLayer);
        hud.setMouseManager(_mouseManager);

        frame.centerOn(hud);
    }

    private UIPanel example1() {
        final UIPanel panel = new UIPanel(new RectLayout());

        final UIButton okButton = new UIButton("OK!");
        okButton.setLayoutData(RectLayoutData.pinCenter(100, 50, 55, 0));
        panel.add(okButton);

        final UIButton cancelButton = new UIButton("Cancel");
        cancelButton.setLayoutData(RectLayoutData.pinCenter(100, 50, -55, 0));
        panel.add(cancelButton);

        return panel;
    }

    private UIPanel example2() {
        final UIPanel panel = new UIPanel(new RectLayout());

        final UILabel lHeader = new UILabel("Welcome! Log in to server xyz");
        lHeader.setLayoutData(new RectLayoutData(0.0, 1.0, 1.0, 1.0, 5, 5, -25, 5));
        lHeader.setBackdrop(new SolidBackdrop(ColorRGBA.LIGHT_GRAY));

        final UILabel lName = new UILabel("Name");
        lName.setAlignment(Alignment.RIGHT);
        lName.setLayoutData(new RectLayoutData(0.0, 1.0, .25, 1.0, 30, 5, -50, 5));
        lName.setBackdrop(new SolidBackdrop(ColorRGBA.LIGHT_GRAY));
        final UITextField tfName = new UITextField();
        tfName.setText("player1");
        tfName.setLayoutData(new RectLayoutData(.25, 1.0, 1.0, 1.0, 30, 5, -50, 5));

        final UILabel lPassword = new UILabel("Password");
        lPassword.setAlignment(Alignment.RIGHT);
        lPassword.setLayoutData(new RectLayoutData(0.0, 1.0, .25, 1.0, 55, 5, -75, 5));
        lPassword.setBackdrop(new SolidBackdrop(ColorRGBA.LIGHT_GRAY));
        final UIPasswordField tfPassword = new UIPasswordField();
        tfPassword.setLayoutData(new RectLayoutData(.25, 1.0, 1.0, 1.0, 55, 5, -75, 5));

        final UIButton btLogin = new UIButton("login");
        btLogin.setLayoutData(new RectLayoutData(.35, 1.0, .65, 1.0, 80, 0, -110, 0));
        btLogin.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                System.out.println("login as user: " + tfName.getText() + " password: " + tfPassword.getText());
            }
        });

        panel.add(lHeader);
        panel.add(lName);
        panel.add(tfName);
        panel.add(lPassword);
        panel.add(tfPassword);
        panel.add(btLogin);

        return panel;
    }

    private UIPanel example3() {
        final UIPanel panel = new UIPanel(new RectLayout());

        final UITextArea historyArea = new UITextArea();
        historyArea.setStyledText(true);
        historyArea.setAlignment(Alignment.BOTTOM_LEFT);
        historyArea.setEditable(false);
        final UIScrollPanel scrollArea = new UIScrollPanel(historyArea);
        scrollArea.setLayoutData(new RectLayoutData(0, 0, 1, 1, 2, 2, 26, 2));
        panel.add(scrollArea);

        final UITextField chatField = new UITextField();
        chatField.setLayoutData(new RectLayoutData(0, 0, 1, 0, -20, 2, 0, 32));
        panel.add(chatField);

        final UIButton chatButton = new UIButton(">");
        chatButton.setMargin(Insets.EMPTY, true);
        chatButton.setBorder(new EmptyBorder(), true);
        chatButton.setLayoutData(new RectLayoutData(1, 0, 1, 0, -20, -30, 0, 2));
        panel.add(chatButton);

        return panel;
    }

    private UIPanel example4() {

        final UIPanel panel = new UIPanel(new BorderLayout());

        final UITextField field = new UITextField("0");
        // field.setAlignment(Alignment.RIGHT);
        field.setLayoutData(BorderLayoutData.NORTH);
        field.setMargin(new Insets(2, 2, 2, 2), true);
        panel.add(field);

        final UIPanel cPanel = new UIPanel(new RectLayout());
        cPanel.setLayoutData(BorderLayoutData.CENTER);
        panel.add(cPanel);

        cPanel.add(makeCalcButton("Bksp", 0, 0, 2));
        cPanel.add(makeCalcButton("C", 0, 2, 2));
        cPanel.add(makeCalcButton("CE", 0, 4, 2));

        cPanel.add(makeCalcButton("MC", 1, 0, 1));
        cPanel.add(makeCalcButton("7", 1, 1, 1));
        cPanel.add(makeCalcButton("8", 1, 2, 1));
        cPanel.add(makeCalcButton("9", 1, 3, 1));
        cPanel.add(makeCalcButton("\u00F7", 1, 4, 1));
        cPanel.add(makeCalcButton("Sqrt", 1, 5, 1));

        cPanel.add(makeCalcButton("MR", 2, 0, 1));
        cPanel.add(makeCalcButton("4", 2, 1, 1));
        cPanel.add(makeCalcButton("5", 2, 2, 1));
        cPanel.add(makeCalcButton("6", 2, 3, 1));
        cPanel.add(makeCalcButton("\u00D7", 2, 4, 1));
        cPanel.add(makeCalcButton("x\u00B2", 2, 5, 1));

        cPanel.add(makeCalcButton("MS", 3, 0, 1));
        cPanel.add(makeCalcButton("1", 3, 1, 1));
        cPanel.add(makeCalcButton("2", 3, 2, 1));
        cPanel.add(makeCalcButton("3", 3, 3, 1));
        cPanel.add(makeCalcButton("-", 3, 4, 1));
        cPanel.add(makeCalcButton("1/x", 3, 5, 1));

        cPanel.add(makeCalcButton("M+", 4, 0, 1));
        cPanel.add(makeCalcButton("0", 4, 1, 1));
        cPanel.add(makeCalcButton(".", 4, 2, 1));
        cPanel.add(makeCalcButton("\u00B1", 4, 3, 1));
        cPanel.add(makeCalcButton("+", 4, 4, 1));
        cPanel.add(makeCalcButton("=", 4, 5, 1));

        return panel;
    }

    private UIButton makeCalcButton(final String rawText, final int row, final int col, final int width) {
        final UIButton btn = new UIButton(rawText);
        btn.setLayoutData(new RectLayoutData(col * .167, .8 - (.2 * row), (col + width) * .167, 1 - (.2 * row), 2, 2,
                2, 2));
        btn.setMargin(Insets.EMPTY, true);
        btn.setPadding(Insets.EMPTY, true);
        // btn.setBorder(new EmptyBorder(), true);
        return btn;
    }

    private void add3DBox() {
        // Add a spinning 3D box to show behind UI.
        final Box box = new Box("Box", new Vector3(0, 0, 0), 5, 5, 5);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(0, 0, -15));
        box.addController(new SpatialController<Box>() {
            private final Matrix3 rotate = new Matrix3();
            private double angle = 0;
            private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();

            public void update(final double time, final Box caller) {
                angle += time * 50;
                angle %= 360;
                rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
                caller.setRotation(rotate);
            }
        });
        // Add a texture to the box.
        final TextureState ts = new TextureState();

        final Texture tex = TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                true);
        ts.setTexture(tex);
        box.setRenderState(ts);
        _root.attachChild(box);
    }

    @Override
    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        super.renderExample(renderer);
        renderer.renderBuckets();
        renderer.draw(hud);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        hud.updateGeometricState(timer.getTimePerFrame());
    }
}
