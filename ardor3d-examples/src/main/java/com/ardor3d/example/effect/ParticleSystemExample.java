/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.effect;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.effect.EffectUtils;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * A demonstration of the ParticleSystem and TextureState classes; which controls an emitter's properties (e.g. size,
 * color) change over time.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.effect.ParticleSystemExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_ParticleSystemExample.jpg", //
        maxHeapMemory = 64)
public class ParticleSystemExample extends ExampleBase {

    private ParticleSystem particles;
    private final Vector3 currentPos = new Vector3(), newPos = new Vector3();

    public static void main(final String[] args) {
        start(ParticleSystemExample.class);
    }

    int ignore = 10;

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
            System.out.printf("%7.1f FPS\n", fps);
        }

        // We'll ignore the first 10 iterations because our timer is going to be unstable.
        if (ignore > 0) {
            ignore--;
            return;
        }
        if ((int) currentPos.getX() == (int) newPos.getX() && (int) currentPos.getY() == (int) newPos.getY()
                && (int) currentPos.getZ() == (int) newPos.getZ()) {
            newPos.setX(MathUtils.nextRandomDouble() * 50 - 25);
            newPos.setY(MathUtils.nextRandomDouble() * 50 - 25);
            newPos.setZ(MathUtils.nextRandomDouble() * 50 - 150);
        }
        final double tpf = timer.getTimePerFrame();
        currentPos.setX(currentPos.getX() - (currentPos.getX() - newPos.getX()) * tpf);
        currentPos.setY(currentPos.getY() - (currentPos.getY() - newPos.getY()) * tpf);
        currentPos.setZ(currentPos.getZ() - (currentPos.getZ() - newPos.getZ()) * tpf);
        _root.setTranslation(currentPos);
    }

    @Override
    protected void initExample() {
        EffectUtils.addDefaultResourceLocators();

        _canvas.setTitle("Particle System - Example");
        _lightState.setEnabled(false);

        // test particles + VBO
        // _root.getSceneHints().setDataMode(DataMode.VBO);

        particles = ParticleFactory.buildParticles("particles", 300);
        particles.setEmissionDirection(new Vector3(0, 1, 0));
        particles.setInitialVelocity(.006);
        particles.setStartSize(2.5);
        particles.setEndSize(.5);
        particles.setMinimumLifeTime(100);
        particles.setMaximumLifeTime(1500);
        particles.setStartColor(new ColorRGBA(1, 0, 0, 1));
        particles.setEndColor(new ColorRGBA(0, 1, 0, 0));
        particles.setMaximumAngle(360 * MathUtils.DEG_TO_RAD);
        particles.getParticleController().setControlFlow(false);
        particles.setParticlesInWorldCoords(true);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.One);
        particles.setRenderState(blend);

        final TextureState ts = new TextureState();
        ts.setTexture(TextureManager.load("images/flaresmall.jpg", Texture.MinificationFilter.Trilinear,
                TextureStoreFormat.GuessCompressedFormat, true));
        ts.getTexture().setWrap(WrapMode.BorderClamp);
        ts.setEnabled(true);
        particles.setRenderState(ts);

        final ZBufferState zstate = new ZBufferState();
        zstate.setWritable(false);
        particles.setRenderState(zstate);

        particles.getParticleGeometry().setModelBound(new BoundingBox());

        _root.attachChild(particles);
        // kick things off by setting our start and end
        newPos.setX(MathUtils.nextRandomDouble() * 50 - 25);
        newPos.setY(MathUtils.nextRandomDouble() * 50 - 25);
        newPos.setZ(MathUtils.nextRandomDouble() * 50 - 150);

        currentPos.setX(MathUtils.nextRandomDouble() * 50 - 25);
        currentPos.setY(MathUtils.nextRandomDouble() * 50 - 25);
        currentPos.setZ(MathUtils.nextRandomDouble() * 50 - 150);
        _root.setTranslation(currentPos);

        // update our world transforms so the the particles will be in the right spot when we warm things up
        _root.updateWorldTransform(true);

        particles.warmUp(60);
    }
}
