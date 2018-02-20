/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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
import com.ardor3d.extension.ui.Orientation;
import com.ardor3d.extension.ui.UIButton;
import com.ardor3d.extension.ui.UICheckBox;
import com.ardor3d.extension.ui.UIComboBox;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UILabel;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.UIProgressBar;
import com.ardor3d.extension.ui.UIRadioButton;
import com.ardor3d.extension.ui.UIScrollPanel;
import com.ardor3d.extension.ui.UISlider;
import com.ardor3d.extension.ui.UITabbedPane;
import com.ardor3d.extension.ui.UITabbedPane.TabPlacement;
import com.ardor3d.extension.ui.backdrop.MultiImageBackdrop;
import com.ardor3d.extension.ui.event.ActionEvent;
import com.ardor3d.extension.ui.event.ActionListener;
import com.ardor3d.extension.ui.event.SelectionListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.GridLayout;
import com.ardor3d.extension.ui.layout.GridLayoutData;
import com.ardor3d.extension.ui.layout.RectLayout;
import com.ardor3d.extension.ui.layout.RectLayoutData;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.model.DefaultComboBoxModel;
import com.ardor3d.extension.ui.text.UIPasswordField;
import com.ardor3d.extension.ui.text.UITextArea;
import com.ardor3d.extension.ui.text.UITextField;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.extension.ui.util.ButtonGroup;
import com.ardor3d.extension.ui.util.Dimension;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.extension.ui.util.TransformedSubTex;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Illustrates how to display GUI primitives (e.g. RadioButton, Label, TabbedPane) on a canvas.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.ui.SimpleUIExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/ui_SimpleUIExample.jpg", //
maxHeapMemory = 64)
public class SimpleUIExample extends ExampleBase {
    UIHud hud;
    private UIFrame frame;

    public static void main(final String[] args) {
        start(SimpleUIExample.class);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Simple UI Example");

        UIComponent.setUseTransparency(true);

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

        final UIPanel panel = makeWidgetPanel();

        final UIPanel panel2 = makeLoginPanel();

        final UIPanel panel3 = makeChatPanel();

        final UIPanel panel4 = makeClockPanel();

        final UIPanel panel5 = makeScrollPanel();

        final UIPanel panel6 = makeSimpleRectLayout();

        final UITabbedPane pane = new UITabbedPane(TabPlacement.NORTH);
        pane.add(panel, "widgets");
        pane.add(panel2, "grid");
        pane.add(panel3, "chat");
        pane.add(panel4, "clock");
        pane.add(panel5, "picture");
        pane.add(panel6, "rect1");

        frame = new UIFrame("UI Sample");
        frame.setContentPanel(pane);
        frame.pack();

        frame.setUseStandin(false);
        frame.setOpacity(1f);
        frame.setName("sample");

        // Uncomment #1...
        // final Matrix3 rotate = new Matrix3();
        // final Vector3 axis = new Vector3(0, 0, 1).normalizeLocal();
        // rotate.fromAngleNormalAxis(45 * MathUtils.DEG_TO_RAD, axis);
        // frame.setRotation(rotate);

        // Uncomment #2... (needs 1)
        // frame.addController(new SpatialController<UIFrame>() {
        // double angle = 0;
        // public void update(final double time, final UIFrame caller) {
        // angle += time * 10;
        // angle %= 360;
        // rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
        // caller.setRotation(rotate);
        // caller.fireComponentDirty();
        // }
        // });

        hud = new UIHud(_canvas);
        hud.add(frame);
        hud.setupInput(_physicalLayer, _logicalLayer);
        hud.setMouseManager(_mouseManager);

        frame.centerOn(hud);
    }

    private UIPanel makeLoginPanel() {
        final UIPanel pLogin = new UIPanel(new GridLayout());
        final UILabel lHeader = new UILabel("Welcome! Log in to server xyz");
        lHeader.setLayoutData(new GridLayoutData(2, true, true));
        final UILabel lName = new UILabel("Name");
        final UITextField tfName = new UITextField();
        tfName.setText("player1");
        tfName.setLayoutData(GridLayoutData.WrapAndGrow);
        final UILabel lPassword = new UILabel("Password");
        final UIPasswordField tfPassword = new UIPasswordField();
        tfPassword.setLayoutData(GridLayoutData.WrapAndGrow);
        final UIButton btLogin = new UIButton("login");
        btLogin.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                System.out.println("login as user: " + tfName.getText() + " password: " + tfPassword.getText());
            }
        });
        pLogin.add(lHeader);
        pLogin.add(lName);
        pLogin.add(tfName);
        pLogin.add(lPassword);
        pLogin.add(tfPassword);
        pLogin.add(btLogin);
        return pLogin;
    }

    private UIPanel makeChatPanel() {
        final UIPanel chatPanel = new UIPanel(new BorderLayout());
        final UIPanel bottomPanel = new UIPanel(new BorderLayout());
        bottomPanel.setLayoutData(BorderLayoutData.SOUTH);
        final UILabel dirLabel = new UILabel("Sample chat.  Try using markup like [b]text[/b]:");
        dirLabel.setLayoutData(BorderLayoutData.NORTH);
        final UITextArea historyArea = new UITextArea();
        historyArea.setStyledText(true);
        historyArea.setAlignment(Alignment.BOTTOM_LEFT);
        historyArea.setEditable(false);
        final UIScrollPanel scrollArea = new UIScrollPanel(historyArea);
        scrollArea.setLayoutData(BorderLayoutData.CENTER);
        final UITextField chatField = new UITextField();
        chatField.setLayoutData(BorderLayoutData.CENTER);
        final UIButton chatButton = new UIButton("SAY");
        chatButton.setLayoutData(BorderLayoutData.EAST);

        final ActionListener actionListener = new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                applyChat(historyArea, chatField, scrollArea);
            }
        };
        chatButton.addActionListener(actionListener);
        chatField.addActionListener(actionListener);

        bottomPanel.add(chatField);
        bottomPanel.add(chatButton);

        chatPanel.add(dirLabel);
        chatPanel.add(scrollArea);
        chatPanel.add(bottomPanel);
        return chatPanel;
    }

    private void applyChat(final UITextArea historyArea, final UITextField chatField, final UIScrollPanel scrollArea) {
        final String text = chatField.getRawText();
        if (text != null && text.length() > 0) {
            final String oldText = historyArea.getRawText() != null ? historyArea.getRawText() + "\n" : "";
            historyArea.setText(oldText + text);
            chatField.setText("");
            scrollArea.layout();
        }
    }

    private UIPanel makeWidgetPanel() {

        final UIPanel panel = new UIPanel();
        panel.setForegroundColor(ColorRGBA.DARK_GRAY);
        panel.setLayout(new BorderLayout());

        final UIButton button = new UIButton("Button A");
        final Texture tex = TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
                false);
        button.setIcon(new SubTex(tex));
        button.setIconDimensions(new Dimension(26, 26));
        button.setGap(10);
        button.setLayoutData(BorderLayoutData.NORTH);
        button.setTooltipText("This is a tooltip!");
        panel.add(button);

        final RowLayout rowLay = new RowLayout(false, false, false);
        final UIPanel centerPanel = new UIPanel(rowLay);
        centerPanel.setLayoutData(BorderLayoutData.CENTER);
        panel.add(centerPanel);

        final UICheckBox check1 = new UICheckBox("Hello\n(disabled)");
        check1.setSelected(true);
        check1.setEnabled(false);
        centerPanel.add(check1);
        final UICheckBox check2 = new UICheckBox("World");
        centerPanel.add(check2);

        final ButtonGroup group = new ButtonGroup();
        final UIRadioButton radio1 = new UIRadioButton();
        radio1.setButtonText("option [i]A[/i]", true);
        radio1.setGroup(group);
        centerPanel.add(radio1);
        final UIRadioButton radio2 = new UIRadioButton();
        radio2.setButtonText("option [c=#f00]B[/c]", true);
        radio2.setGroup(group);
        centerPanel.add(radio2);

        final UISlider slider = new UISlider(Orientation.Horizontal, 0, 12, 0);
        slider.setSnapToValues(true);
        slider.setMinimumContentWidth(100);

        final UILabel lSliderValue = new UILabel("0");
        lSliderValue.setLayoutData(GridLayoutData.SpanAndWrap(2));
        slider.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent event) {
                lSliderValue.setText(String.valueOf(slider.getValue()));
            }
        });
        centerPanel.add(slider);
        centerPanel.add(lSliderValue);

        final UIComboBox combo = new UIComboBox(new DefaultComboBoxModel("alpha", "beta", "gamma", "delta"));
        combo.setLocalComponentWidth(120);
        combo.addSelectionListener(new SelectionListener<UIComboBox>() {
            @Override
            public void selectionChanged(final UIComboBox component, final Object newValue) {
                System.out.println("New combo value: " + newValue);
            }
        });
        centerPanel.add(combo);

        final UIProgressBar bar = new UIProgressBar("Loading: ", true);
        bar.setPercentFilled(0);
        bar.setLocalComponentWidth(250);
        bar.setMaximumContentWidth(bar.getContentWidth());
        bar.addController(new SpatialController<UIProgressBar>() {
            @Override
            public void update(final double time, final UIProgressBar caller) {
                caller.setPercentFilled(_timer.getTimeInSeconds() / 15);
            }
        });
        centerPanel.add(bar);
        return panel;
    }

    private UIPanel makeClockPanel() {
        final UIPanel clockPanel = new UIPanel();
        final MultiImageBackdrop multiImgBD = new MultiImageBackdrop(ColorRGBA.BLACK_NO_ALPHA);
        clockPanel.setBackdrop(multiImgBD);

        final Texture clockTex = TextureManager.load("images/clock.png", Texture.MinificationFilter.Trilinear, false);

        final TransformedSubTex clockBack = new TransformedSubTex(new SubTex(clockTex, 64, 65, 446, 446));

        final double scale = .333;
        clockBack.setPivot(new Vector2(.5, .5));
        clockBack.getTransform().setScale(scale);
        clockBack.setAlignment(Alignment.MIDDLE);
        clockBack.setPriority(0);
        multiImgBD.addImage(clockBack);

        final TransformedSubTex hour = new TransformedSubTex(new SubTex(clockTex, 27, 386, 27, 126));
        hour.setPivot(new Vector2(.5, 14 / 126f));
        hour.getTransform().setScale(scale);
        hour.setAlignment(Alignment.MIDDLE);
        hour.setPriority(1);
        multiImgBD.addImage(hour);

        final TransformedSubTex minute = new TransformedSubTex(new SubTex(clockTex, 0, 338, 27, 174));
        minute.setPivot(new Vector2(.5, 14 / 174f));
        minute.getTransform().setScale(scale);
        minute.setAlignment(Alignment.MIDDLE);
        minute.setPriority(2);
        multiImgBD.addImage(minute);

        clockPanel.addController(new SpatialController<Spatial>() {
            public void update(final double time, final Spatial caller) {
                final double angle1 = _timer.getTimeInSeconds() % MathUtils.TWO_PI;
                final double angle2 = (_timer.getTimeInSeconds() / 12.) % MathUtils.TWO_PI;

                minute.getTransform().setRotation(new Quaternion().fromAngleAxis(angle1, Vector3.NEG_UNIT_Z));
                hour.getTransform().setRotation(new Quaternion().fromAngleAxis(angle2, Vector3.NEG_UNIT_Z));
                clockPanel.fireComponentDirty();
            };
        });

        return clockPanel;
    }

    private UIPanel makeScrollPanel() {
        final Texture tex = TextureManager.load("images/clock.png", Texture.MinificationFilter.Trilinear, false);
        final UILabel comp = new UILabel("");
        comp.setIcon(new SubTex(tex));
        comp.updateIconDimensionsFromIcon();
        final UIScrollPanel panel = new UIScrollPanel(comp);
        return panel;
    }

    private UIPanel makeSimpleRectLayout() {
        final UIPanel panel = new UIPanel(new RectLayout());

        final UIButton okButton = new UIButton("OK!");
        okButton.setLayoutData(RectLayoutData.pinCenter(100, 50, 55, 0));
        panel.add(okButton);

        final UIButton cancelButton = new UIButton("Cancel");
        cancelButton.setLayoutData(RectLayoutData.pinCenter(100, 50, -55, 0));
        panel.add(cancelButton);

        return panel;
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

    private double counter = 0;
    private int frames = 0;

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            frame.setTitle("UI Sample: " + Math.round(fps) + " FPS");
        }
        hud.updateGeometricState(timer.getTimePerFrame());
    }
}
