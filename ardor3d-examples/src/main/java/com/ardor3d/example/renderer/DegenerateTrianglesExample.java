/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer;

import java.nio.FloatBuffer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

/**
 * Illustrates two techniques for creating a triangle strip (i.e. series of connected triangles).
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.renderer.DegenerateTrianglesExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_DegenerateTrianglesExample.jpg", //
    maxHeapMemory = 64)
public class DegenerateTrianglesExample extends ExampleBase {

  private BasicText t;
  private boolean showDegenerateMesh = false;

  private final int xSize = 200;
  private final int ySize = 200;
  private final int totalSize = xSize * ySize;

  public static void main(final String[] args) {
    start(DegenerateTrianglesExample.class);
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
      System.out.printf("%7.1f FPS\n", fps);
    }
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Degenerate vs MultiStrip Example");

    t = BasicText.createDefaultTextLabel("Text", "[SPACE] MultiStrip Mesh");
    t.getSceneHints().setLightCombineMode(LightCombineMode.Off);
    t.setTranslation(new Vector3(0, 20, 0));
    _orthoRoot.attachChild(t);

    final TextureState ts = new TextureState();
    ts.setEnabled(true);
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear,
        TextureStoreFormat.GuessCompressedFormat, true));

    final double maxSize = Math.max(xSize, ySize);
    final double requiredDistance =
        (maxSize / 2) / Math.tan(_canvas.getCanvasRenderer().getCamera().getFovY() * MathUtils.DEG_TO_RAD * 0.5);

    final Mesh multiStripMesh = createMultiStripMesh();
    multiStripMesh.setRenderState(ts);
    multiStripMesh.updateModelBound();
    multiStripMesh.setTranslation(-xSize * 0.5, -ySize * 0.5, -requiredDistance);
    _root.attachChild(multiStripMesh);

    final Mesh degenerateStripMesh = createDegenerateStripMesh();
    degenerateStripMesh.setRenderState(ts);
    degenerateStripMesh.updateModelBound();
    degenerateStripMesh.setTranslation(-xSize * 0.5, -ySize * 0.5, -requiredDistance);
    _root.attachChild(degenerateStripMesh);

    degenerateStripMesh.getSceneHints().setCullHint(CullHint.Always);

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), (source, inputStates, tpf) -> {
      showDegenerateMesh = !showDegenerateMesh;
      if (showDegenerateMesh) {
        t.setText("[SPACE] Degenerate Mesh");
        multiStripMesh.getSceneHints().setCullHint(CullHint.Always);
        degenerateStripMesh.getSceneHints().setCullHint(CullHint.Inherit);
      } else {
        t.setText("[SPACE] MultiStrip Mesh");
        multiStripMesh.getSceneHints().setCullHint(CullHint.Inherit);
        degenerateStripMesh.getSceneHints().setCullHint(CullHint.Always);
      }
    }));
  }

  private Mesh createMultiStripMesh() {
    final Mesh mesh = new Mesh();
    final MeshData meshData = mesh.getMeshData();

    final FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(totalSize);
    final FloatBuffer normalBuffer = BufferUtils.createVector3Buffer(totalSize);
    final FloatBuffer textureBuffer = BufferUtils.createVector2Buffer(totalSize);

    final IndexBufferData<?> indices =
        BufferUtils.createIndexBufferData((ySize - 1) * xSize * 2, vertexBuffer.capacity() - 1);
    final int[] indexLengths = new int[ySize - 1];

    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        vertexBuffer.put(x).put(y).put(0);
        normalBuffer.put(0).put(0).put(1);
        textureBuffer.put(x).put(y);
      }
    }

    for (int y = 0; y < ySize - 1; y++) {
      for (int x = 0; x < xSize; x++) {
        final int index = y * xSize + x;
        indices.put(index);
        indices.put(index + xSize);
      }
      indexLengths[y] = xSize * 2;
    }

    meshData.setVertexBuffer(vertexBuffer);
    meshData.setNormalBuffer(normalBuffer);
    meshData.setTextureBuffer(textureBuffer, 0);

    meshData.setIndices(indices);
    meshData.setIndexLengths(indexLengths);
    meshData.setIndexMode(IndexMode.TriangleStrip);

    return mesh;
  }

  private Mesh createDegenerateStripMesh() {
    final Mesh mesh = new Mesh();
    final MeshData meshData = mesh.getMeshData();

    final FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(totalSize);
    final FloatBuffer normalBuffer = BufferUtils.createVector3Buffer(totalSize);
    final FloatBuffer textureBuffer = BufferUtils.createVector2Buffer(totalSize);

    final IndexBufferData<?> indices =
        BufferUtils.createIndexBufferData((ySize - 1) * xSize * 2 + (ySize - 1) * 2, vertexBuffer.capacity() - 1);

    for (int y = 0; y < ySize; y++) {
      for (int x = 0; x < xSize; x++) {
        vertexBuffer.put(x).put(y).put(0);
        normalBuffer.put(0).put(0).put(1);
        textureBuffer.put(x).put(y);
      }
    }

    for (int y = 0; y < ySize - 1; y++) {
      for (int x = 0; x < xSize; x++) {
        final int index = y * xSize + x;
        indices.put(index);
        indices.put(index + xSize);
      }

      final int index = (y + 1) * xSize;
      indices.put(index + xSize - 1);
      indices.put(index);
    }

    meshData.setVertexBuffer(vertexBuffer);
    meshData.setNormalBuffer(normalBuffer);
    meshData.setTextureBuffer(textureBuffer, 0);

    meshData.setIndices(indices);
    meshData.setIndexMode(IndexMode.TriangleStrip);

    return mesh;
  }
}
