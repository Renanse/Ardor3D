/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import java.util.Random;

import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * A demonstration of randomly placed spheres being illuminated by numerious PointLight sources.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.ManyLightsExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_ManyLightsExample.jpg", //
        maxHeapMemory = 64)
public class ManyLightsExample extends ExampleBase {
    private final Random rand = new Random();
    private Node colornode;
    private final double worldsize = 20;

    public static void main(final String[] args) {
        start(ManyLightsExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        _root.sortLights();
    }

    /**
     * Create a randomly positioned light and lightsphere with a controller
     *
     * @param i
     *            Index of the light
     */
    void randomLight(final int i) {
        // Chose the color for the lights.
        final ColorRGBA lightColor = ColorRGBA.randomColor(new ColorRGBA());

        // Create a sphere to show where the light is in the demo.
        final Sphere lightSphere = new Sphere("lightSphere" + i, 9, 9, .1f);
        lightSphere.setModelBound(new BoundingSphere());
        lightSphere.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        lightSphere.setDefaultColor(lightColor);

        // Create a new point light and fill out the properties
        final PointLight pointLight = new PointLight();
        pointLight.setAttenuate(true);
        pointLight.setConstant(.01f);
        pointLight.setLinear(.001f);
        pointLight.setQuadratic(.1f);
        pointLight.setEnabled(true);
        pointLight.setDiffuse(lightColor);
        pointLight.setAmbient(new ColorRGBA(.1f, .1f, .1f, .1f));

        _lightState.attach(pointLight);

        lightSphere.addController(new SpatialController<Spatial>() {

            double timeX = rand.nextDouble() * Math.PI * 8;
            double timeY = rand.nextDouble() * Math.PI * 8;
            double timeZ = rand.nextDouble() * Math.PI * 8;
            double speed = MathUtils.nextRandomDouble();

            public void update(final double tpf, final Spatial caller) {
                timeX += tpf * speed;
                timeY += tpf * speed;
                timeZ += tpf * speed;

                final double xPos = Math.sin(timeX * 0.4) * worldsize;
                final double yPos = Math.cos(timeY * 0.5) * worldsize;
                final double zPos = Math.sin(timeZ * 0.6) * worldsize;

                caller.setTranslation(xPos, yPos, zPos);
                pointLight.setLocation(xPos, yPos, zPos);
            }
        });

        // Add the light to the world part 2.
        colornode.attachChild(lightSphere);
    }

    /**
     * Create a random sphere to be affected by the light
     *
     * @param i
     *            Index of the sphere
     */
    void randomSphere(final int i) {
        // Crate a sphere and position it.
        final Sphere sphere = new Sphere("sphere" + i, 18, 18, MathUtils.nextRandomDouble() * 1.75 + 1.25);
        sphere.updateModelBound();
        sphere.setTranslation(new Vector3(rand.nextDouble() * worldsize * 2 - worldsize,
                rand.nextDouble() * worldsize * 2 - worldsize, rand.nextDouble() * worldsize * 2 - worldsize));

        _root.attachChild(sphere);
    }

    @Override
    protected void initExample() {
        _lightState.detachAll();

        rand.setSeed(1337);

        // Now add all the lights.
        colornode = new Node("LightNode");

        for (int i = 0; i < 40; i++) {
            randomLight(i);
        }
        // Add the spheres.
        for (int i = 0; i < 30; i++) {
            randomSphere(i);
        }

        _root.attachChild(colornode);
    }
}
