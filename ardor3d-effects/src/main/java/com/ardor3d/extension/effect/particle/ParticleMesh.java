/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect.particle;

import java.io.IOException;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.extension.effect.particle.emitter.MeshEmitter;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * ParticleMesh is a particle system that uses Mesh as its underlying geometric data.
 */
public class ParticleMesh extends ParticleSystem {

  private boolean _useMeshTexCoords = true;
  private boolean _useTriangleNormalEmit = true;

  public ParticleMesh() {}

  public ParticleMesh(final String name, final int numParticles) {
    super(name, numParticles);
    LightProperties.setLightReceiver(this, false);
    getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
    getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
  }

  public ParticleMesh(final String name, final int numParticles, final ParticleSystem.ParticleType type) {
    super(name, numParticles, type);
    LightProperties.setLightReceiver(this, false);
    getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
    getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
  }

  public ParticleMesh(final String name, final Mesh sourceMesh) {
    super(name, 0, ParticleSystem.ParticleType.GeomMesh);
    _numParticles = sourceMesh.getMeshData().getTotalPrimitiveCount();
    setParticleEmitter(new MeshEmitter(sourceMesh, false));
    LightProperties.setLightReceiver(this, false);
    getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
    getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
    initializeParticles(_numParticles);
  }

  @Override
  protected void initializeParticles(final int numParticles) {
    setRenderMaterial("particles/particle_mesh.yaml");

    if (_particleMesh != null) {
      detachChild(_particleMesh);
    }
    final Mesh mesh = new Mesh(getName() + "_mesh") {

      @Override
      public void updateWorldTransform(final boolean recurse) {
        // Do nothing.
      }

      @Override
      public void updateWorldBound(final boolean recurse) {
        super.updateWorldTransform(recurse);
        super.updateWorldBound(recurse);
      }
    };
    _particleMesh = mesh;
    attachChild(mesh);
    _particles = new Particle[numParticles];
    if (numParticles == 0) {
      return;
    }
    Vector2 sharedTextureData[];

    // setup texture coords and index mode
    final MeshData meshData = mesh.getMeshData();
    switch (getParticleType()) {
      case GeomMesh:
      case Triangle:
        sharedTextureData = new Vector2[] {new Vector2(2.0, 0.0), new Vector2(0.0, 2.0), new Vector2(0.0, 0.0)};
        meshData.setIndexMode(IndexMode.Triangles);
        break;
      default:
        throw new IllegalStateException(
            "Particle Mesh may only have particle type of ParticleType.GeomMesh or ParticleType.Triangle");
    }

    final int verts = getVertsForParticleType(getParticleType());

    _geometryCoordinates = BufferUtils.createVector3Buffer(numParticles * verts);
    _appearanceColors = BufferUtils.createColorBuffer(numParticles * verts);

    meshData.setVertexBuffer(_geometryCoordinates);
    meshData.setColorBuffer(_appearanceColors);
    meshData.setTextureBuffer(BufferUtils.createVector2Buffer(numParticles * verts), 0);

    final Vector2 temp = Vector2.fetchTempInstance();
    for (int k = 0; k < numParticles; k++) {
      _particles[k] = new Particle(this);
      _particles[k].init();
      _particles[k].setStartIndex(k * verts);
      for (int a = verts - 1; a >= 0; a--) {
        final int ind = (k * verts) + a;
        if (_particleType == ParticleSystem.ParticleType.GeomMesh && _useMeshTexCoords) {
          final MeshEmitter source = (MeshEmitter) getParticleEmitter();
          final Mesh sourceMesh = source.getSource();
          final int index =
              sourceMesh.getMeshData().getIndices() != null ? sourceMesh.getMeshData().getIndices().get(ind) : ind;
          BufferUtils.populateFromBuffer(temp, sourceMesh.getMeshData().getTextureCoords(0).getBuffer(), index);
          BufferUtils.setInBuffer(temp, meshData.getTextureCoords(0).getBuffer(), ind);
        } else {
          BufferUtils.setInBuffer(sharedTextureData[a], meshData.getTextureCoords(0).getBuffer(), ind);
        }
        BufferUtils.setInBuffer(_particles[k].getCurrentColor(), _appearanceColors, (ind));
      }

    }
    Vector2.releaseTempInstance(temp);
    updateWorldRenderStates(true);
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
  public void resetParticleVelocity(final int i) {
    if (_particleType == ParticleSystem.ParticleType.GeomMesh && _useTriangleNormalEmit) {
      _particles[i].getVelocity().set(_particles[i].getTriangleModel().getNormal());
      _particles[i].getVelocity().multiplyLocal(_emissionDirection);
      _particles[i].getVelocity().multiplyLocal(getInitialVelocity());
    } else {
      super.resetParticleVelocity(i);
    }
  }

  public boolean isUseMeshTexCoords() { return _useMeshTexCoords; }

  public void setUseMeshTexCoords(final boolean useMeshTexCoords) { _useMeshTexCoords = useMeshTexCoords; }

  public boolean isUseTriangleNormalEmit() { return _useTriangleNormalEmit; }

  public void setUseTriangleNormalEmit(final boolean useTriangleNormalEmit) {
    _useTriangleNormalEmit = useTriangleNormalEmit;
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_useMeshTexCoords, "useMeshTexCoords", true);
    capsule.write(_useTriangleNormalEmit, "useTriangleNormalEmit", true);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _useMeshTexCoords = capsule.readBoolean("useMeshTexCoords", true);
    _useTriangleNormalEmit = capsule.readBoolean("useTriangleNormalEmit", true);
  }

  @Override
  public Mesh getParticleGeometry() { return _particleMesh; }
}
