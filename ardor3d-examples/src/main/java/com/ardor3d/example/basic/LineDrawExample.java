/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.basic;

import java.util.LinkedList;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.util.GeneratedImageFactory;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.MouseButtonClickedCondition;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.TextureKey;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.GeometryTool;

@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.basic.LineDrawExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_LineDrawExample.jpg", //
    maxHeapMemory = 64)
public class LineDrawExample extends ExampleBase {

  private final Node instructNode = new Node();
  private Line line;

  private int labelIndex;
  private final int column1X = 20;
  private final int column2X = 120;
  private final int startY = 50;
  private final double fontSize = 24.0;
  private final float spacing = 10f;

  private boolean mitered;
  private boolean vertexColors;
  private boolean textured = true;
  private int textureIndex;

  public static void main(final String[] args) {
    start(LineDrawExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Ardor3D - Line Drawing Example");
    _canvas.setBackgroundColor(ColorRGBA.DARK_GRAY);
    setupLine();
    recreateInstructions();
  }

  @Override
  protected void registerInputTriggers() {
    super.registerInputTriggers();
    // drop WASD control
    // FirstPersonControl.removeTriggers(_logicalLayer, _controlHandle);
    _controlHandle.setMoveSpeed(1);

    _logicalLayer.registerTrigger(
        new InputTrigger(new MouseButtonClickedCondition(MouseButton.LEFT), (source, inputStates, tpf) -> {
          final MouseState mouse = inputStates.getCurrent().getMouseState();
          addPointToLine(mouse.getX(), mouse.getY());
          updateLineMaterial();
        }));

    _logicalLayer
        .registerTrigger(new InputTrigger(new KeyPressedCondition(Key.LEFT_BRACKET), (source, inputStates, tpf) -> {
          line.setLineWidth(Math.max(0.5f, line.getLineWidth() - 0.5f));
          recreateInstructions();
        }));

    _logicalLayer
        .registerTrigger(new InputTrigger(new KeyPressedCondition(Key.RIGHT_BRACKET), (source, inputStates, tpf) -> {
          line.setLineWidth(Math.min(99f, line.getLineWidth() + 0.5f));
          recreateInstructions();
        }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.MINUS), (source, inputStates, tpf) -> {
      line.setMiterLimit(Math.max(-1f, line.getMiterLimit() - 0.1f));
      recreateInstructions();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.EQUAL), (source, inputStates, tpf) -> {
      line.setMiterLimit(Math.min(1f, line.getMiterLimit() + 0.1f));
      recreateInstructions();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.L), (source, inputStates, tpf) -> {
      line.setAntialiased(!line.isAntialiased());
      updateLineMaterial();
      recreateInstructions();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.V), (source, inputStates, tpf) -> {
      vertexColors = !vertexColors;

      if (vertexColors) {
        line.setRandomColors();
      } else {
        line.getMeshData().setColorCoords(null);
      }

      updateLineMaterial();
      recreateInstructions();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.M), (source, inputStates, tpf) -> {
      mitered = !mitered;
      updateLineMaterial();
      recreateInstructions();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE), (source, inputStates, tpf) -> {
      textured = false;
      updateLineMaterial();
      recreateInstructions();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO), (source, inputStates, tpf) -> {
      textured = true;
      textureIndex = 0;
      updateLineMaterial();
      recreateInstructions();
    }));

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE), (source, inputStates, tpf) -> {
      textured = true;
      textureIndex = 1;
      updateLineMaterial();
      recreateInstructions();
    }));
  }

  private final LinkedList<Vector3> points = new LinkedList<>();

  private void setupLine() {
    line = new Line("line");
    line.setLineWidth(20f);
    line.setMiterLimit(.75f);
    line.setDefaultColor(ColorRGBA.WHITE);
    line.getMeshData().setIndexMode(IndexMode.LineStripAdjacency);
    line.setRenderState(_wireframeState);
    line.setAntialiased(true);
    addPointToLine(800, 600);
    addPointToLine(0, 0);
    addPointToLine(400, 0);
    addPointToLine(400, 600);

    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    blend.setSourceFunction(SourceFunction.SourceAlpha);
    blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
    line.setRenderState(blend);

    updateLineMaterial();

    _root.attachChild(line);
  }

  private void updateLineMaterial() {

    try {
      final MeshData meshData = line.getMeshData();

      if (mitered) {
        meshData.setIndexMode(IndexMode.LineStripAdjacency);
        meshData.setIndices(GeometryTool.generateAdjacencyIndices(line.getMeshData().getIndexMode(0), points.size()));
      } else {
        meshData.setIndexMode(IndexMode.LineStrip);
        meshData.setIndices(null);
      }
      meshData.markIndicesDirty();

      // this forces some useful update - still looking into it
      meshData.markBufferDirty(MeshData.KEY_VertexCoords);

      if (!textured) {
        line.clearRenderState(StateType.Texture);
        return;
      }

      Image image;

      if (textureIndex == 0) {
        // solid line, alpha edges
        image = GeneratedImageFactory.create1DColorImage(true, new ColorRGBA(1, 1, 1, 1), new ColorRGBA(1, 1, 1, 0),
            new ColorRGBA(1, 1, 1, 1), new ColorRGBA(1, 1, 1, 0), new ColorRGBA(1, 1, 1, 1), new ColorRGBA(1, 1, 1, 0),
            new ColorRGBA(1, 1, 1, 1));
      } else if (textureIndex == 1) {
        // double line, alpha edges
        image = GeneratedImageFactory.create1DColorImage(true, new ColorRGBA(1, 1, 1, 1), new ColorRGBA(1, 1, 1, 1),
            new ColorRGBA(1, 1, 1, 1), new ColorRGBA(1, 1, 1, 0), new ColorRGBA(1, 1, 1, 0), new ColorRGBA(1, 1, 1, 1),
            new ColorRGBA(1, 1, 1, 1), new ColorRGBA(1, 1, 1, 1));

      } else {
        return;
      }

      final TextureState ts = new TextureState();
      final Texture tex = new Texture2D();
      tex.setImage(image);
      tex.setMagnificationFilter(MagnificationFilter.Bilinear);
      tex.setMinificationFilter(MinificationFilter.Trilinear);
      tex.setTextureKey(TextureKey.getRTTKey(tex.getMinificationFilter()));
      ts.setTexture(tex);
      line.setRenderState(ts);
    } finally {
      MaterialUtil.autoMaterials(line, true);
    }
  }

  protected void addPointToLine(final int x, final int y) {
    final Vector3 coords = _canvas.getCanvasRenderer().getCamera().getWorldCoordinates(new Vector2(x, y), 0.25f);
    points.add(coords);

    final MeshData meshData = line.getMeshData();
    meshData.setVertexBuffer(BufferUtils.createFloatBuffer(points.toArray(new Vector3[0])));
    meshData.markBufferDirty(MeshData.KEY_VertexCoords);

    if (vertexColors) {
      line.setRandomColors();
    }
    if (mitered) {
      meshData.setIndices(GeometryTool.generateAdjacencyIndices(line.getMeshData().getIndexMode(0), points.size()));
      meshData.markIndicesDirty();
    }
  }

  @Override
  public void onResize(final int newWidth, final int newHeight) {
    super.onResize(newWidth, newHeight);
    updateInstructionLocation();
  }

  private void recreateInstructions() {
    labelIndex = 0;
    instructNode.detachAllChildren();
    createControlText("ESC", "Quit");
    createControlText("LMB", "Add New Point");
    createControlText("T", "Wireframe");
    createControlText("[  ]", "Line Weight: " + String.format("%.1f", line.getLineWidth()));
    createControlText("L", "[" + (line.isAntialiased() ? "ON" : "OFF") + "] Toggle Antialiased");
    createControlText("V", "[" + (vertexColors ? "ON" : "OFF") + "] Toggle Random Vertex Colors");
    createControlText("M", "[" + (mitered ? "ON" : "OFF") + "] Toggle Mitering");
    if (mitered) {
      createControlText("-  =", "Miter Limit: " + String.format("%.1f", line.getMiterLimit()));
    }
    createControlText("1", "No Texture");
    createControlText("2", "Texture 1");
    createControlText("3", "Texture 2");
    updateInstructionLocation();
    _orthoRoot.attachChild(instructNode);
  }

  private void createControlText(final String key, final String label) {
    final BasicText keyText = BasicText.createDefaultTextLabel("key", key, fontSize);
    final BasicText labelText = BasicText.createDefaultTextLabel("label", label, fontSize);

    final double y = -(labelIndex * (fontSize + spacing));
    keyText.setTranslation(column1X, y, 0);
    labelText.setTranslation(column2X, y, 0);

    instructNode.attachChild(keyText);
    instructNode.attachChild(labelText);

    labelIndex++;
  }

  private void updateInstructionLocation() {
    instructNode.setTranslation(0, _canvas.getContentHeight() - startY, 0);
  }
}
