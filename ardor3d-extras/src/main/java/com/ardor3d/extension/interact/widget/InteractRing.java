/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.widget;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.AbstractBufferData.VBOAccessMode;
import com.ardor3d.image.Texture2D;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * Textured ring geometry, intended for use as a rotational handle.
 */
public class InteractRing extends Mesh {
  protected float _innerRadius, _outerRadius;
  protected int _tessRings = 2;
  protected int _tessSteps = 32;
  protected float _texMul = 4.0f;
  protected float _concaveValue = 0;

  public InteractRing() {}

  public InteractRing(final String name, final int tessRings, final int tessSteps, final float radius,
    final float width) {
    this(name, tessRings, tessSteps, radius, width, 0);
  }

  public InteractRing(final String name, final int tessRings, final int tessSteps, final float radius,
    final float width, final float concaveValue) {
    super(name);
    _tessRings = tessRings;
    _tessSteps = tessSteps;
    _concaveValue = concaveValue;
    setRadius(radius, width);
  }

  public void setRadius(final float radius, final float width) {
    _innerRadius = radius;
    _outerRadius = radius + width;
    updateGeometry();
  }

  /**
   * @param vMult
   *          new multiplier for v direction of texture coords (around ring)
   */
  public void setTextureMultiplier(final float vMult) {
    _texMul = vMult;
    updateGeometry();
  }

  public void setConcaveValue(final float value) {
    _concaveValue = value;
    updateGeometry();
  }

  /**
   * Convenience method for setting texture without managing TextureState.
   *
   * @param texture
   *          the new texture to set on unit 0.
   */
  public void setTexture(final Texture2D texture) {
    TextureState ts = (TextureState) getLocalRenderState(RenderState.StateType.Texture);
    if (ts == null) {
      ts = new TextureState();
      ts.setEnabled(true);
      setRenderState(ts);
    }
    ts.setTexture(texture, 0);
  }

  /**
   *
   */
  public void updateGeometry() {
    final int numPairs = _tessSteps + 1;
    final int totalVerts = _tessRings * numPairs * 2;

    final MeshData meshData = getMeshData();
    FloatBuffer crdBuf = meshData.getVertexBuffer();
    if (crdBuf == null || totalVerts != crdBuf.limit() / 3) { // allocate new buffers
      meshData.setVertexBuffer(BufferUtils.createVector3Buffer(totalVerts));
      meshData.setNormalBuffer(BufferUtils.createVector3Buffer(totalVerts));
      meshData.setTextureBuffer(BufferUtils.createVector2Buffer(totalVerts), 0);
      crdBuf = meshData.getVertexBuffer();
      meshData.getVertexCoords().setVboAccessMode(VBOAccessMode.DynamicDraw);
      meshData.getNormalCoords().setVboAccessMode(VBOAccessMode.DynamicDraw);
      meshData.getTextureCoords(0).setVboAccessMode(VBOAccessMode.DynamicDraw);
    }
    final FloatBuffer nrmBuf = meshData.getNormalBuffer();
    final FloatBuffer txcBuf = meshData.getTextureBuffer(0);
    calculateVertexData(_tessRings, numPairs, totalVerts, crdBuf, nrmBuf, txcBuf);

    // mark our data as needing updates
    meshData.markBufferDirty(MeshData.KEY_VertexCoords);
    meshData.markBufferDirty(MeshData.KEY_NormalCoords);
    meshData.markBufferDirty(MeshData.KEY_TextureCoords0);

    updateModelBound();
  }

  protected void normalize(final int i, final float[] nrm) {
    final float length = (float) Math.sqrt(nrm[i] * nrm[i] + nrm[i + 1] * nrm[i + 1] + nrm[i + 2] * nrm[i + 2]);
    nrm[i] /= length;
    nrm[i + 1] /= length;
    nrm[i + 2] /= length;
  }

  protected void calculateVertexData(final int numStrips, final int numPairs, final int totalVerts,
      final FloatBuffer crdBuf, final FloatBuffer nrmBuf, final FloatBuffer txcBuf) {
    // we are generating strips
    getMeshData().setIndexMode(IndexMode.TriangleStrip);

    final float astep = (float) (Math.PI * 2 / _tessSteps);
    final float sstep = 1.0f / numStrips;
    final float rrange = _outerRadius - _innerRadius;
    final float rstep = rrange / numStrips;
    float xa, ya;
    float r0, r1;
    float nadd0, nadd1;
    float tc;
    final float up = 1;
    final float[] nrm = new float[6];
    crdBuf.rewind();
    nrmBuf.rewind();
    txcBuf.rewind();
    for (int s = 0; s < numStrips; s++) {
      nadd0 = _concaveValue * (s + 0 - numStrips * 0.5f) / numStrips;
      nadd1 = _concaveValue * (s + 1 - numStrips * 0.5f) / numStrips;
      for (int a = 0; a < numPairs; a++) {
        xa = (float) Math.cos(a * astep);
        ya = (float) Math.sin(a * astep);
        r0 = _innerRadius + (s + 0) * rstep;
        r1 = _innerRadius + (s + 1) * rstep;

        crdBuf.put(xa * r0).put(ya * r0).put(0);
        crdBuf.put(xa * r1).put(ya * r1).put(0);

        nrm[0] = nadd0 * xa;
        nrm[1] = nadd0 * ya;
        nrm[2] = up;
        nrm[3] = nadd1 * xa;
        nrm[4] = nadd1 * ya;
        nrm[5] = up;
        normalize(0, nrm);
        normalize(3, nrm);
        nrmBuf.put(nrm[0]).put(nrm[1]).put(nrm[2]);
        nrmBuf.put(nrm[3]).put(nrm[4]).put(nrm[5]);

        tc = a * _texMul / _tessSteps;
        txcBuf.put((s + 0) * sstep).put(tc);
        txcBuf.put((s + 1) * sstep).put(tc);
      }
    }
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(_innerRadius, "innerRadius", 0f);
    capsule.write(_outerRadius, "outerRadius", 0f);
    capsule.write(_tessRings, "tessRings", 2);
    capsule.write(_tessSteps, "tessSteps", 16);
    capsule.write(_texMul, "texMul", 1f);
    capsule.write(_concaveValue, "concaveValue", 0f);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    _innerRadius = capsule.readFloat("innerRadius", 0f);
    _outerRadius = capsule.readFloat("outerRadius", 0f);
    _tessRings = capsule.readInt("tessRings", 2);
    _tessSteps = capsule.readInt("tessSteps", 16);
    _texMul = capsule.readFloat("texMul", 1f);
    _concaveValue = capsule.readFloat("concaveValue", 0f);
  }
}
