/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.effect;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.effect.EffectUtils;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.extension.effect.particle.SwarmInfluence;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Example showing a particle system using the particle SwarmInfluence.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.effect.ParticleSwarmExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/effect_ParticleSwarmExample.jpg", //
    maxHeapMemory = 64)
public class ParticleSwarmExample extends ExampleBase {

  private ParticleSystem particles;
  private final Vector3 currentPos = new Vector3(), newPos = new Vector3();
  private SwarmInfluence swarm;
  private Sphere sphere;

  public static void main(final String[] args) {
    start(ParticleSwarmExample.class);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    if ((int) currentPos.getX() == (int) newPos.getX() && (int) currentPos.getY() == (int) newPos.getY()
        && (int) currentPos.getZ() == (int) newPos.getZ()) {
      newPos.setX(MathUtils.nextRandomDouble() * 50 - 25);
      newPos.setY(MathUtils.nextRandomDouble() * 50 - 25);
      newPos.setZ(MathUtils.nextRandomDouble() * 50 - 150);
    }

    final double frameRate = timer.getFrameRate() / 2.0;
    currentPos.setX(currentPos.getX() - (currentPos.getX() - newPos.getX()) / frameRate);
    currentPos.setY(currentPos.getY() - (currentPos.getY() - newPos.getY()) / frameRate);
    currentPos.setZ(currentPos.getZ() - (currentPos.getZ() - newPos.getZ()) / frameRate);

    particles.setOriginOffset(currentPos);
    sphere.setTranslation(currentPos);
  }

  @Override
  protected void initExample() {
    EffectUtils.addDefaultResourceLocators();

    _canvas.setTitle("Particle System - Swarming Influence");
    _lightState.setEnabled(false);

    sphere = new Sphere("sp", 12, 12, 3);
    sphere.setModelBound(new BoundingBox());
    sphere.setDefaultColor(ColorRGBA.BLUE);
    sphere.getSceneHints().setRenderBucketType(RenderBucketType.Opaque);

    particles = ParticleFactory.buildParticles("particles", 30);
    particles.setEmissionDirection(new Vector3(0, 1, 0));
    particles.setStartSize(3);
    particles.setEndSize(1.5);
    particles.setOriginOffset(new Vector3(0, 0, 0));
    particles.setInitialVelocity(.05);
    particles.setMinimumLifeTime(5000);
    particles.setMaximumLifeTime(15000);
    particles.setStartColor(new ColorRGBA(1, 0, 0, 1));
    particles.setEndColor(new ColorRGBA(0, 1, 0, 1));
    particles.setMaximumAngle(360f * MathUtils.DEG_TO_RAD);
    particles.getParticleController().setControlFlow(false);
    particles.getParticleController().setSpeed(0.75);
    swarm = new SwarmInfluence(new Vector3(particles.getWorldTranslation()), .001);
    swarm.setMaxSpeed(.2);
    swarm.setSpeedBump(0.025);
    swarm.setTurnSpeed(MathUtils.DEG_TO_RAD * 360);
    particles.addInfluence(swarm);

    final BlendState as1 = new BlendState();
    as1.setBlendEnabled(true);
    as1.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
    as1.setDestinationFunction(BlendState.DestinationFunction.One);
    particles.setRenderState(as1);

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
    _root.attachChild(sphere);
    _root.setRenderMaterial("unlit/untextured/basic.yaml");
  }
}
