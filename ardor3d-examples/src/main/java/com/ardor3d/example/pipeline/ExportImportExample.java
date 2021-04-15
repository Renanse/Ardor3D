/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.pipeline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Teapot;
import com.ardor3d.scenegraph.shape.Torus;
import com.ardor3d.surface.ColorSurface;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.export.binary.BinaryImporter;
import com.ardor3d.util.export.xml.XMLExporter;
import com.ardor3d.util.export.xml.XMLImporter;

/**
 * A demonstration of the BinaryImporter and BinaryExporter classes; which can export/import a Node
 * to/from a data stream.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.pipeline.ExportImportExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/pipeline_ExportImportExample.jpg", //
    maxHeapMemory = 64)
public class ExportImportExample extends ExampleBase {
  private static final Logger logger = Logger.getLogger(ExportImportExample.class.getName());

  private final Matrix3 rotation = new Matrix3();

  private Node originalNode;
  private Node binaryImportedNode;
  private Node xmlImportedNode;

  public static void main(final String[] args) {
    start(ExportImportExample.class);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    final double time = timer.getTimeInSeconds() * 0.5;

    originalNode.setRotation(rotation.fromAngles(time, time, time));
    if (binaryImportedNode != null) {
      binaryImportedNode.setRotation(rotation);
    }
    if (xmlImportedNode != null) {
      xmlImportedNode.setRotation(rotation);
    }
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("TestExportImport");

    final Texture bg = TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true);
    final TextureState bgts = new TextureState();
    bgts.setTexture(bg);
    bgts.setEnabled(true);

    final TextureState ts = new TextureState();
    final Texture t0 = TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true);
    final Texture t1 = TextureManager.load("images/flaresmall.jpg", Texture.MinificationFilter.Trilinear, true);
    // t1.setEnvironmentalMapMode(Texture.EnvironmentalMapMode.SphereMap);
    ts.setTexture(t0, 0);
    ts.setTexture(t1, 1);
    ts.setEnabled(true);

    final Torus torus = new Torus("Torus", 50, 50, 10, 25);
    torus.updateModelBound();
    torus.setRenderState(ts);
    final ColorSurface surface = new ColorSurface();
    surface.setDiffuse(ColorRGBA.BLUE);
    torus.setProperty(ColorSurface.DefaultPropertyKey, surface);

    final Quad quad = new Quad("Quad");
    quad.resize(150, 120);
    quad.updateModelBound();
    quad.setRenderState(bgts);

    final Teapot teapot = new Teapot();
    teapot.setScale(20);
    teapot.updateModelBound();
    teapot.setRandomColors();

    final Mesh multiStrip = createMultiStrip();
    multiStrip.updateModelBound();
    multiStrip.setTranslation(0, 0, -30);

    originalNode = new Node("originalNode");
    originalNode.attachChild(torus);
    originalNode.attachChild(quad);
    originalNode.attachChild(multiStrip);
    originalNode.attachChild(teapot);

    // attach and set material
    _root.attachChild(originalNode);

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      new BinaryExporter().save(originalNode, bos);
    } catch (final IOException e) {
      logger.log(Level.SEVERE, "BinaryExporter failed to save file", e);
    }

    originalNode.setTranslation(new Vector3(-80, 0, -400));

    try {
      binaryImportedNode = (Node) new BinaryImporter().load(bos.toByteArray());
      binaryImportedNode.setTranslation(new Vector3(80, 80, -400));
      _root.attachChild(binaryImportedNode);
    } catch (final IOException e) {
      logger.log(Level.SEVERE, "BinaryImporter failed to load file", e);
    }

    bos.reset();
    try {
      new XMLExporter().save(originalNode, bos);
    } catch (final IOException e) {
      logger.log(Level.SEVERE, "XMLExporter failed to save file", e);
    }

    try {
      xmlImportedNode = (Node) new XMLImporter().load(bos.toByteArray());
      xmlImportedNode.setTranslation(new Vector3(80, -80, -400));
      _root.attachChild(xmlImportedNode);
    } catch (final IOException e) {
      logger.log(Level.SEVERE, "XMLImporter failed to load file", e);
    }
    MaterialUtil.autoMaterials(_root);
  }

  private Mesh createMultiStrip() {
    final Mesh mesh = new Mesh("ms");
    final MeshData meshData = mesh.getMeshData();

    final FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(12);

    vertexBuffer.put(-30).put(0).put(0);
    vertexBuffer.put(-40).put(0).put(0);
    vertexBuffer.put(-40).put(10).put(0);
    vertexBuffer.put(-30).put(10).put(0);

    vertexBuffer.put(-10).put(0).put(0);
    vertexBuffer.put(-20).put(0).put(0);
    vertexBuffer.put(-20).put(10).put(0);
    vertexBuffer.put(-10).put(10).put(0);

    vertexBuffer.put(30).put(0).put(0);
    vertexBuffer.put(40).put(0).put(0);
    vertexBuffer.put(40).put(10).put(0);
    vertexBuffer.put(30).put(10).put(0);

    meshData.setVertexBuffer(vertexBuffer);

    final IndexBufferData<?> indices = BufferUtils.createIndexBufferData(18, vertexBuffer.capacity() - 1);

    // Strips
    indices.put(0).put(3).put(1).put(2);
    indices.put(4).put(7).put(5).put(6);

    // Triangles
    indices.put(8).put(9).put(11);
    indices.put(9).put(11).put(10);

    meshData.setIndices(indices);

    // Setting sub primitive data
    final int[] indexLengths = new int[] {4, 4, 6};
    meshData.setIndexLengths(indexLengths);

    final IndexMode[] indexModes =
        new IndexMode[] {IndexMode.TriangleStrip, IndexMode.TriangleStrip, IndexMode.Triangles};
    meshData.setIndexModes(indexModes);

    final WireframeState ws = new WireframeState();
    mesh.setRenderState(ws);
    mesh.updateModelBound();

    return mesh;
  }
}
