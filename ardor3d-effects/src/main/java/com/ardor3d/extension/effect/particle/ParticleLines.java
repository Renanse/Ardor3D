/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.particle;

import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.geom.BufferUtils;

/**
 * ParticleLines is a particle system that uses Line as its underlying geometric data.
 */
public class ParticleLines extends ParticleSystem {

    public ParticleLines() {}

    public ParticleLines(final String name, final int numParticles) {
        super(name, numParticles);
    }

    @Override
    protected void initializeParticles(final int numParticles) {

        // setup texture coords
        final Vector2[] sharedTextureData = new Vector2[] { new Vector2(0.0, 0.0), new Vector2(1.0, 1.0) };

        final int verts = getVertsForParticleType(getParticleType());

        _geometryCoordinates = BufferUtils.createVector3Buffer(numParticles * verts);

        // setup indices for PT_LINES

        _appearanceColors = BufferUtils.createColorBuffer(numParticles * verts);
        _particles = new Particle[numParticles];

        if (_particleMesh != null) {
            detachChild(_particleMesh);
        }
        final Line line = new Line(getName() + "_lines") {

            @Override
            public void updateWorldTransform(final boolean recurse) {
                ; // Do nothing.
            }

            @Override
            public void updateWorldBound(final boolean recurse) {
                super.updateWorldTransform(recurse);
                super.updateWorldBound(recurse);
            }
        };
        _particleMesh = line;
        attachChild(line);
        line.getMeshData().setVertexBuffer(_geometryCoordinates);
        line.getMeshData().setColorBuffer(_appearanceColors);
        line.getMeshData().setTextureBuffer(BufferUtils.createVector2Buffer(numParticles * 2), 0);
        getSceneHints().setRenderBucketType(RenderBucketType.Opaque);
        getSceneHints().setLightCombineMode(LightCombineMode.Off);
        getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);

        for (int k = 0; k < numParticles; k++) {
            _particles[k] = new Particle(this);
            _particles[k].init();
            _particles[k].setStartIndex(k * verts);
            for (int a = verts - 1; a >= 0; a--) {
                final int ind = (k * verts) + a;
                BufferUtils.setInBuffer(sharedTextureData[a], line.getMeshData().getTextureCoords(0).getBuffer(), ind);
                BufferUtils.setInBuffer(_particles[k].getCurrentColor(), _appearanceColors, (ind));
            }

        }
        updateWorldRenderStates(true);
        _particleMesh.getSceneHints().setCastsShadows(false);
    }

    @Override
    public ParticleType getParticleType() {
        return ParticleSystem.ParticleType.Line;
    }

    @Override
    public void draw(final Renderer r) {
        final Camera camera = Camera.getCurrentCamera();
        boolean anyAlive = false;
        for (int i = 0; i < _particles.length; i++) {
            final Particle particle = _particles[i];
            if (particle.getStatus() == Particle.Status.Alive) {
                particle.updateVerts(camera);
                anyAlive = true;
            }
        }

        // Since we've updated our verts, update the model boundary where applicable
        if (getParticleGeometry().getWorldBound() != null && anyAlive) {
            getParticleGeometry().updateModelBound();
        }

        if (!_particlesInWorldCoords) {
            getParticleGeometry().setWorldTransform(getWorldTransform());
        } else {
            getParticleGeometry().setWorldTranslation(Vector3.ZERO);
            getParticleGeometry().setWorldRotation(Matrix3.IDENTITY);
            getParticleGeometry().setWorldScale(getWorldScale());
        }

        getParticleGeometry().draw(r);
    }

    @Override
    public Line getParticleGeometry() {
        return (Line) _particleMesh;
    }
}
