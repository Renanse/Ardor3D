/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.util;

import java.nio.FloatBuffer;

import com.ardor3d.image.Image;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector2;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.AbstractBufferData.VBOAccessMode;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;

public class UIArc extends Mesh {

  protected double _sampleRate;

  protected double _radius, _innerRadius, _arcLength;

  public UIArc() {}

  /**
   * Creates a flat disk (circle) at the origin flat along the Z. Usually, a higher sample number
   * creates a better looking cylinder, but at the cost of more vertex information.
   *
   * @param name
   *          The name of the arc.
   * @param sampleRate
   *          The number of radial samples per radian.
   * @param radius
   *          The radius of the arc.
   * @param innerRadius
   *          The innerRadius of the arc.
   */
  public UIArc(final String name, final double sampleRate, final double radius, final double innerRadius) {
    super(name);

    _sampleRate = sampleRate;
    resetGeometry(0, MathUtils.PI, radius, innerRadius, null, true);
    getMeshData().setIndexMode(IndexMode.Triangles);
  }

  public void resetGeometry(final double startAngle, final double arcLength, final double radius,
      final double innerRadius, final SubTex subTex, final boolean ignoreArcEdges) {
    _radius = radius;
    _innerRadius = innerRadius;
    _arcLength = arcLength;

    // allocate vertices
    final int radialVerts = (int) MathUtils.round(_arcLength / _sampleRate);
    final int verts = radialVerts * 4 + 4 * 4; // bg verts + edge decorations
    if (_meshData.getVertexBuffer() == null || _meshData.getVertexBuffer().capacity() < verts * 2) {
      _meshData.setVertexCoords(new FloatBufferData(verts * 2, 2));
      _meshData.getVertexCoords().setVboAccessMode(VBOAccessMode.DynamicDraw);
    } else {
      _meshData.getVertexBuffer().clear();
    }
    if (_meshData.getTextureBuffer(0) == null || _meshData.getTextureBuffer(0).capacity() < verts * 2) {
      _meshData.setTextureCoords(new FloatBufferData(verts * 2, 2), 0);
      _meshData.getTextureCoords(0).setVboAccessMode(VBOAccessMode.DynamicDraw);
    } else {
      _meshData.getTextureBuffer(0).clear();
    }

    // allocate indices
    final int tris = (radialVerts + 1) * 6; // bg tris + edge decorations
    final int indices = tris * 3;
    if (_meshData.getIndexBuffer() == null || _meshData.getIndexBuffer().capacity() < indices) {
      _meshData.setIndices(BufferUtils.createIndexBufferData(indices, verts - 1));
      _meshData.getIndices().setVboAccessMode(VBOAccessMode.DynamicDraw);
    } else {
      _meshData.getIndexBuffer().clear();
    }
    _meshData.setIndexLengths(new int[] {indices - 36, 18});

    // generate geometry
    float txOff = 0f, tyOff = 0f, txScale = 1f, tyScale = 1f;
    int topBrd = 0, leftBrd = 0, bottomBrd = 0, rightBrd = 0;
    float topOffTx = 0f, leftOffTx = 0f, bottomOffTx = 0f, rightOffTx = 0f;
    if (subTex != null && subTex.getTexture() != null && subTex.getTexture().getImage() != null) {
      txOff = subTex.getStartX();
      tyOff = subTex.getStartY();
      txScale = subTex.getEndX() - subTex.getStartX();
      tyScale = subTex.getEndY() - subTex.getStartY();

      topBrd = subTex.getBorderTop();
      leftBrd = subTex.getBorderLeft();
      bottomBrd = subTex.getBorderBottom();
      rightBrd = subTex.getBorderRight();

      final Image image = subTex.getTexture().getImage();
      topOffTx = topBrd / (float) image.getHeight();
      leftOffTx = leftBrd / (float) image.getWidth();
      bottomOffTx = bottomBrd / (float) image.getHeight();
      rightOffTx = rightBrd / (float) image.getWidth();

      if (ignoreArcEdges) {
        leftBrd = 0;
        rightBrd = 0;
      }
    }

    /***** VERT DATA *****/
    {
      final Vector2 radialOffset = new Vector2();
      final Vector2 radialEdge = new Vector2();
      final Vector2 texCoord = new Vector2();
      final FloatBuffer vertBuffer = _meshData.getVertexBuffer();
      final FloatBuffer texBuffer = _meshData.getTextureBuffer(0);

      int i = 0;
      final double actualSampleRate = _arcLength / (radialVerts - 1);
      double angle = startAngle;
      for (int x = 0; x < radialVerts; x++) {
        final float radialU = txOff + leftOffTx + (txScale - leftOffTx - rightOffTx) * ((float) x / (radialVerts - 1));
        final Vector2 radial = new Vector2(MathUtils.sin(angle), MathUtils.cos(angle));
        radialOffset.set(radial).multiplyLocal(_innerRadius);

        // inner edge
        radialEdge.zero().addLocal(radialOffset);
        BufferUtils.setInBuffer(radialEdge, vertBuffer, i);
        texCoord.set(radialU, tyOff + tyScale);
        BufferUtils.setInBuffer(texCoord, texBuffer, i++);

        // inner edge border
        radialEdge.set(radial).multiplyLocal(bottomBrd).addLocal(radialOffset);
        BufferUtils.setInBuffer(radialEdge, vertBuffer, i);
        texCoord.set(radialU, tyOff + tyScale - topOffTx);
        BufferUtils.setInBuffer(texCoord, texBuffer, i++);

        // outer edge border
        radialEdge.set(radial).multiplyLocal(_radius - _innerRadius - topBrd).addLocal(radialOffset);
        BufferUtils.setInBuffer(radialEdge, vertBuffer, i);
        texCoord.set(radialU, tyOff + bottomOffTx);
        BufferUtils.setInBuffer(texCoord, texBuffer, i++);

        // outer edge
        radialEdge.set(radial).multiplyLocal(_radius - _innerRadius).addLocal(radialOffset);
        BufferUtils.setInBuffer(radialEdge, vertBuffer, i);
        texCoord.set(radialU, tyOff);
        BufferUtils.setInBuffer(texCoord, texBuffer, i++);

        angle += actualSampleRate;
      }

      // *** edge decoration - left
      {
        angle = startAngle + MathUtils.HALF_PI;
        final float radialU1 = txOff;
        final float radialU2 = txOff + leftOffTx;
        final Vector2 radial = new Vector2(MathUtils.sin(angle), MathUtils.cos(angle));
        radialOffset.set(radial).multiplyLocal(leftBrd);

        for (int s = 0; s < 4; s++) {
          BufferUtils.copyInternalVector2(vertBuffer, s, i + s);
          BufferUtils.copyInternalVector2(vertBuffer, s, i + 4 + s);
          BufferUtils.addInBuffer(radialOffset, vertBuffer, i + 4 + s);
        }

        // inner edge
        BufferUtils.setInBuffer(texCoord.set(radialU1, tyOff + tyScale), texBuffer, i + 0);
        BufferUtils.setInBuffer(texCoord.set(radialU2, tyOff + tyScale), texBuffer, i + 4);

        // inner edge border
        BufferUtils.setInBuffer(texCoord.set(radialU1, tyOff + tyScale - topOffTx), texBuffer, i + 1);
        BufferUtils.setInBuffer(texCoord.set(radialU2, tyOff + tyScale - topOffTx), texBuffer, i + 5);

        // outer edge border
        BufferUtils.setInBuffer(texCoord.set(radialU1, tyOff + bottomOffTx), texBuffer, i + 2);
        BufferUtils.setInBuffer(texCoord.set(radialU2, tyOff + bottomOffTx), texBuffer, i + 6);

        // outer edge
        BufferUtils.setInBuffer(texCoord.set(radialU1, tyOff), texBuffer, i + 3);
        BufferUtils.setInBuffer(texCoord.set(radialU2, tyOff), texBuffer, i + 7);

        i += 8;
      }

      // *** edge decoration - right
      {
        angle = startAngle + arcLength - MathUtils.HALF_PI;
        final float radialU1 = txOff + txScale - rightOffTx;
        final float radialU2 = txOff + txScale;
        final Vector2 radial = new Vector2(MathUtils.sin(angle), MathUtils.cos(angle));
        radialOffset.set(radial).multiplyLocal(rightBrd);

        for (int s = 0; s < 4; s++) {
          BufferUtils.copyInternalVector2(vertBuffer, s, i + s);
          BufferUtils.copyInternalVector2(vertBuffer, s, i + 4 + s);
          BufferUtils.addInBuffer(radialOffset, vertBuffer, i + 4 + s);
        }

        // inner edge
        BufferUtils.setInBuffer(texCoord.set(radialU1, tyOff + tyScale), texBuffer, i + 0);
        BufferUtils.setInBuffer(texCoord.set(radialU2, tyOff + tyScale), texBuffer, i + 4);

        // inner edge border
        BufferUtils.setInBuffer(texCoord.set(radialU1, tyOff + tyScale - topOffTx), texBuffer, i + 1);
        BufferUtils.setInBuffer(texCoord.set(radialU2, tyOff + tyScale - topOffTx), texBuffer, i + 5);

        // outer edge border
        BufferUtils.setInBuffer(texCoord.set(radialU1, tyOff + bottomOffTx), texBuffer, i + 2);
        BufferUtils.setInBuffer(texCoord.set(radialU2, tyOff + bottomOffTx), texBuffer, i + 6);

        // outer edge
        BufferUtils.setInBuffer(texCoord.set(radialU1, tyOff), texBuffer, i + 3);
        BufferUtils.setInBuffer(texCoord.set(radialU2, tyOff), texBuffer, i + 7);

        i += 8;
      }

      /***** INDICE DATA *****/
      {
        final IndexBufferData<?> indBuff = _meshData.getIndices();
        for (int r = 0; r < radialVerts - 1; r++) {
          for (int iS = 0; iS < 3; iS++) {
            final int i00 = iS + 4 * r;
            final int i01 = iS + 4 * (r + 1);
            final int i10 = i00 + 1;
            final int i11 = i01 + 1;
            indBuff.put(i00).put(i10).put(i11);
            indBuff.put(i00).put(i11).put(i01);
          }
        }

        // edge decoration - left
        int e = verts - 16;
        for (int iS = 0; iS < 3; iS++) {
          final int i00 = e + iS;
          final int i01 = e + iS + 4;
          final int i10 = i00 + 1;
          final int i11 = i01 + 1;
          indBuff.put(i00).put(i10).put(i11);
          indBuff.put(i00).put(i11).put(i01);
        }

        // edge decoration - right
        e = verts - 8;
        for (int iS = 0; iS < 3; iS++) {
          final int i00 = e + iS;
          final int i01 = e + iS + 4;
          final int i10 = i00 + 1;
          final int i11 = i01 + 1;
          indBuff.put(i00).put(i10).put(i11);
          indBuff.put(i00).put(i11).put(i01);
        }
      }
    }

    _meshData.markBufferDirty(MeshData.KEY_VertexCoords);
    _meshData.markBufferDirty(MeshData.KEY_TextureCoords0);
    _meshData.markIndicesDirty();
  }

  public double getSampleRate() { return _sampleRate; }

  public double getRadius() { return _radius; }

  public double getInnerRadius() { return _innerRadius; }

}
