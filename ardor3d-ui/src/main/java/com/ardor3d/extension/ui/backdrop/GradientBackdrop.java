/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.backdrop;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.AbstractBufferData.VBOAccessMode;
import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;

/**
 * A gradient four color backdrop. Each corner can be a different color and it will be blended
 * across the background.
 */
public class GradientBackdrop extends UIBackdrop {

  private final ColorRGBA _topLeft = new ColorRGBA(ColorRGBA.LIGHT_GRAY);
  private final ColorRGBA _topRight = new ColorRGBA(ColorRGBA.GRAY);
  private final ColorRGBA _bottomLeft = new ColorRGBA(ColorRGBA.GRAY);
  private final ColorRGBA _bottomRight = new ColorRGBA(ColorRGBA.DARK_GRAY);

  private static Mesh _mesh = GradientBackdrop.createMesh();
  private static final float[] _vals = new float[12];
  private static final float[] _cVals = new float[16];

  /**
   * Construct this back drop, using default colors.
   */
  public GradientBackdrop() {}

  /**
   * Construct this back drop, using the given corner colors.
   *
   * @param topLeft
   * @param topRight
   * @param bottomLeft
   * @param bottomRight
   */
  public GradientBackdrop(final ReadOnlyColorRGBA topLeft, final ReadOnlyColorRGBA topRight,
    final ReadOnlyColorRGBA bottomLeft, final ReadOnlyColorRGBA bottomRight) {
    setTopLeft(topLeft);
    setTopRight(topRight);
    setBottomLeft(bottomLeft);
    setBottomRight(bottomRight);
  }

  public ReadOnlyColorRGBA getBottomLeft() { return _bottomLeft; }

  public void setBottomLeft(final ReadOnlyColorRGBA color) {
    _bottomLeft.set(color);
  }

  public ReadOnlyColorRGBA getBottomRight() { return _bottomRight; }

  public void setBottomRight(final ReadOnlyColorRGBA color) {
    _bottomRight.set(color);
  }

  public ReadOnlyColorRGBA getTopLeft() { return _topLeft; }

  public void setTopLeft(final ReadOnlyColorRGBA color) {
    _topLeft.set(color);
  }

  public ReadOnlyColorRGBA getTopRight() { return _topRight; }

  public void setTopRight(final ReadOnlyColorRGBA color) {
    _topRight.set(color);
  }

  @Override
  public void draw(final Renderer renderer, final UIComponent comp) {

    final float pAlpha = UIComponent.getCurrentOpacity();

    final Vector3 v = Vector3.fetchTempInstance();
    final Insets margin = comp.getMargin() != null ? comp.getMargin() : Insets.EMPTY;
    final Insets border = comp.getBorder() != null ? comp.getBorder() : Insets.EMPTY;
    v.set(margin.getLeft() + border.getLeft(), margin.getBottom() + border.getBottom(), 0);

    final Transform t = Transform.fetchTempInstance();
    t.set(comp.getWorldTransform());
    t.applyForwardVector(v);
    t.translate(v);
    Vector3.releaseTempInstance(v);

    GradientBackdrop._mesh.setWorldTransform(t);
    Transform.releaseTempInstance(t);

    final int width = UIBackdrop.getBackdropWidth(comp);
    final int height = UIBackdrop.getBackdropHeight(comp);

    GradientBackdrop._vals[0] = 0;
    GradientBackdrop._vals[1] = 0;
    GradientBackdrop._vals[3] = width;
    GradientBackdrop._vals[4] = 0;
    GradientBackdrop._vals[6] = width;
    GradientBackdrop._vals[7] = height;
    GradientBackdrop._vals[9] = 0;
    GradientBackdrop._vals[10] = height;

    GradientBackdrop._cVals[0] = _bottomLeft.getRed();
    GradientBackdrop._cVals[1] = _bottomLeft.getGreen();
    GradientBackdrop._cVals[2] = _bottomLeft.getBlue();
    GradientBackdrop._cVals[3] = _bottomLeft.getAlpha() * pAlpha;
    GradientBackdrop._cVals[4] = _bottomRight.getRed();
    GradientBackdrop._cVals[5] = _bottomRight.getGreen();
    GradientBackdrop._cVals[6] = _bottomRight.getBlue();
    GradientBackdrop._cVals[7] = _bottomRight.getAlpha() * pAlpha;
    GradientBackdrop._cVals[8] = _topRight.getRed();
    GradientBackdrop._cVals[9] = _topRight.getGreen();
    GradientBackdrop._cVals[10] = _topRight.getBlue();
    GradientBackdrop._cVals[11] = _topRight.getAlpha() * pAlpha;
    GradientBackdrop._cVals[12] = _topLeft.getRed();
    GradientBackdrop._cVals[13] = _topLeft.getGreen();
    GradientBackdrop._cVals[14] = _topLeft.getBlue();
    GradientBackdrop._cVals[15] = _topLeft.getAlpha() * pAlpha;

    final MeshData meshData = GradientBackdrop._mesh.getMeshData();
    meshData.getVertexBuffer().rewind();
    meshData.getVertexBuffer().put(GradientBackdrop._vals);

    meshData.getColorBuffer().rewind();
    meshData.getColorBuffer().put(GradientBackdrop._cVals);

    meshData.markBufferDirty(MeshData.KEY_VertexCoords);
    meshData.markBufferDirty(MeshData.KEY_ColorCoords);
    GradientBackdrop._mesh.render(renderer);
  }

  private static Mesh createMesh() {
    final Mesh mesh = new Mesh();
    mesh.setRenderMaterial("ui/untextured/vertex_color.yaml");

    final MeshData meshData = mesh.getMeshData();
    meshData.setVertexBuffer(BufferUtils.createVector3Buffer(4));
    meshData.getVertexCoords().setVboAccessMode(VBOAccessMode.DynamicDraw);
    meshData.setColorBuffer(BufferUtils.createColorBuffer(4));
    meshData.getColorCoords().setVboAccessMode(VBOAccessMode.DynamicDraw);
    meshData.setIndexMode(IndexMode.TriangleFan);

    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    blend.setSourceFunction(SourceFunction.SourceAlpha);
    blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
    mesh.setRenderState(blend);
    mesh.updateWorldRenderStates(false);

    return mesh;
  }
}
