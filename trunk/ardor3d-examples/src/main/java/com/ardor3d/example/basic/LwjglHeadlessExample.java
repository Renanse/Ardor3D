/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.basic;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.net.URISyntaxException;
import java.nio.IntBuffer;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl.LwjglHeadlessCanvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.util.AWTImageLoader;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.Timer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * <p>
 * A demonstration of the LwjglHeadlessCanvas class, which is canvas used to draw Scene data to an offscreen target.
 * </p>
 * 
 * <p>
 * Also of note, this example does not allow choosing of properties on launch. It also does not handle input or show any
 * special debugging. This is to simplify the example to the basic essentials.
 * </p>
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.basic.LwjglHeadlessExample", //
thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_LwjglHeadlessExample.jpg", //
maxHeapMemory = 64)
public class LwjglHeadlessExample implements Scene {

    // Our headless canvas
    private final LwjglHeadlessCanvas canvas;

    // Our timer.
    private final Timer timer = new Timer();

    // The root of our scene
    private final Node _root = new Node();

    // The settings for our canvas
    private final DisplaySettings settings;

    // A label we'll set our image into for viewing.
    private final JLabel label;

    // The frame we'll show to the user.
    private final JFrame frame;

    // The buffered image we'll reuse.
    private final BufferedImage labelImage;

    // A int array we'll reuse when transferring data between opengl and the labelImage.
    private final int[] tmpData;

    /**
     * Our main entry point to the example. News up the example and calls start.
     * 
     * @param args
     *            unused.
     */
    public static void main(final String[] args) {
        final LwjglHeadlessExample example = new LwjglHeadlessExample();
        example.start();
    }

    /**
     * Constructs the example class, creating our frame, label, bufferedimage and the headless canvas.
     */
    public LwjglHeadlessExample() {
        // Setup our headless canvas for rendering.
        settings = new DisplaySettings(800, 600, 0, 0, false);
        canvas = new LwjglHeadlessCanvas(settings, this);
        canvas.getRenderer().setBackgroundColor(ColorRGBA.BLACK_NO_ALPHA);

        // Set up an image to show our 3d content in.
        labelImage = new BufferedImage(settings.getWidth(), settings.getHeight(), BufferedImage.TYPE_INT_ARGB);
        tmpData = ((DataBufferInt) labelImage.getRaster().getDataBuffer()).getData();

        // Set up a frame and label with icon to show our image in.
        frame = new JFrame("Headless Example - close window to exit");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        label = new JLabel("View of Headless Content:");
        label.setVerticalTextPosition(SwingConstants.TOP);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setIcon(new ImageIcon(labelImage));
        frame.getContentPane().add(label);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Kicks off the example logic, first setting up the scene, then continuously updating and rendering.
     */
    private void start() {
        initExample();

        while (true) {
            updateExample();
            canvas.draw();

            final IntBuffer data = canvas.getDataBuffer();

            final int width = settings.getWidth();

            for (int x = settings.getHeight(); --x >= 0;) {
                data.get(tmpData, x * width, width);
            }

            label.setIcon(new ImageIcon(labelImage));
        }
    }

    /**
     * Initialize our scene.
     */
    private void initExample() {
        // Make a box...
        final Box _box = new Box("Box", Vector3.ZERO, 5, 5, 5);

        // Setup a bounding box for it -- updateModelBound is called automatically internally.
        _box.setModelBound(new BoundingBox());

        // Set its location in space.
        _box.setTranslation(new Vector3(0, 0, -25));

        // Add to root.
        _root.attachChild(_box);

        // set it to rotate:
        _box.addController(new SpatialController<Spatial>() {
            private final Vector3 _axis = new Vector3(1, 1, 0.5f).normalizeLocal();
            private final Matrix3 _rotate = new Matrix3();
            private double _angle = 0;

            public void update(final double time, final Spatial caller) {
                // update our rotation
                _angle = _angle + (timer.getTimePerFrame() * 25);
                if (_angle > 180) {
                    _angle = -180;
                }

                _rotate.fromAngleNormalAxis(_angle * MathUtils.DEG_TO_RAD, _axis);
                _box.setRotation(_rotate);
            }
        });

        // Add our awt based image loader.
        AWTImageLoader.registerLoader();

        // Set the location of our example resources.
        try {
            final SimpleResourceLocator srl = new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(
                    LwjglHeadlessExample.class, "com/ardor3d/example/media/"));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, srl);
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }

        // Create a ZBuffer to display pixels closest to the camera above farther ones.
        final ZBufferState buf = new ZBufferState();
        buf.setEnabled(true);
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        _root.setRenderState(buf);

        // Create a texture from the Ardor3D logo.
        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));
        _root.setRenderState(ts);

        // Set up a basic, default light.
        final PointLight light = new PointLight();
        light.setDiffuse(new ColorRGBA(0.75f, 0.75f, 0.75f, 0.75f));
        light.setAmbient(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        light.setLocation(new Vector3(100, 100, 100));
        light.setEnabled(true);

        // Attach the light to a lightState and the lightState to rootNode.
        final LightState lightState = new LightState();
        lightState.setEnabled(true);
        lightState.attach(light);
        _root.setRenderState(lightState);

    }

    // Used for calculating fps.
    private double counter = 0;
    private int frames = 0;

    /**
     * Update our scene... Update our timer, print the current fps (once per second) and finally update the geometric
     * state of the root and its children.
     */
    private void updateExample() {
        timer.update();

        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }

        // Update controllers/render states/transforms/bounds for rootNode.
        _root.updateGeometricState(timer.getTimePerFrame(), true);
    }

    // ------ Scene methods ------

    public boolean renderUnto(final Renderer renderer) {

        // Draw the root and all its children.
        renderer.draw(_root);

        return true;
    }

    public PickResults doPick(final Ray3 pickRay) {
        // Ignore
        return null;
    }
}
