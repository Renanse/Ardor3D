/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.Map;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.PixelDataType;
import com.ardor3d.image.util.ImageUtils;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.renderer.queue.RenderQueue;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.util.Constants;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

/**
 * Provides some common base level method implementations for Renderers.
 */
public abstract class AbstractRenderer implements Renderer {
  // clear color
  protected final ColorRGBA _backgroundColor = new ColorRGBA(ColorRGBA.BLACK);

  protected boolean _processingQueue;

  protected RenderQueue _queue = new RenderQueue();

  protected int _stencilClearValue;

  protected Map<RenderMatrixType, FloatBuffer> _matrixStore = new EnumMap<>(RenderMatrixType.class);

  /** List of default rendering states for this specific renderer type */
  protected final EnumMap<RenderState.StateType, RenderState> defaultStateList =
      new EnumMap<>(RenderState.StateType.class);

  public AbstractRenderer() {
    _matrixStore.put(RenderMatrixType.Model, BufferUtils.createFloatBuffer(16));
    _matrixStore.put(RenderMatrixType.View, BufferUtils.createFloatBuffer(16));
    _matrixStore.put(RenderMatrixType.Projection, BufferUtils.createFloatBuffer(16));
    _matrixStore.put(RenderMatrixType.ModelViewProjection, BufferUtils.createFloatBuffer(16));
    _matrixStore.put(RenderMatrixType.Normal, BufferUtils.createFloatBuffer(9));

    for (final RenderState.StateType type : RenderState.StateType.values()) {
      final RenderState state = RenderState.createState(type);
      state.setEnabled(false);
      defaultStateList.put(type, state);
    }
  }

  @Override
  public ReadOnlyColorRGBA getBackgroundColor() { return _backgroundColor; }

  @Override
  public RenderQueue getQueue() { return _queue; }

  @Override
  public boolean isProcessingQueue() { return _processingQueue; }

  @Override
  public RenderState applyState(final StateType type, final RenderState state) {
    if (Constants.stats) {
      StatCollector.startStat(StatType.STAT_STATES_TIMER);
    }

    final RenderState tempState = getProperRenderState(type, state);
    final RenderContext context = ContextManager.getCurrentContext();
    if (!RenderState._quickCompare.contains(type) || tempState.needsRefresh()
        || tempState != context.getCurrentState(type)) {
      doApplyState(tempState);
      tempState.setNeedsRefresh(false);
    }

    if (Constants.stats) {
      StatCollector.endStat(StatType.STAT_STATES_TIMER);
    }

    return tempState;
  }

  protected abstract void doApplyState(RenderState state);

  protected void addStats(final IndexMode indexMode, final int vertCount) {
    final int primCount = IndexMode.getPrimitiveCount(indexMode, vertCount);
    switch (indexMode) {
      case Triangles:
      case TriangleFan:
      case TriangleStrip:
      case TrianglesAdjacency:
      case TriangleStripAdjacency:
        StatCollector.addStat(StatType.STAT_TRIANGLE_COUNT, primCount);
        break;
      case Lines:
      case LineLoop:
      case LineStrip:
      case LinesAdjacency:
      case LineStripAdjacency:
        StatCollector.addStat(StatType.STAT_LINE_COUNT, primCount);
        break;
      case Points:
        StatCollector.addStat(StatType.STAT_POINT_COUNT, primCount);
        break;
    }
  }

  public int getStencilClearValue() { return _stencilClearValue; }

  public void setStencilClearValue(final int stencilClearValue) { _stencilClearValue = stencilClearValue; }

  @Override
  public RenderState getProperRenderState(final StateType type, final RenderState current) {
    final RenderContext context = ContextManager.getCurrentContext();

    // first look up in enforced states
    final RenderState state = context.hasEnforcedStates() ? context.getEnforcedState(type) : null;

    // Not there? Use the state we received
    if (state == null) {
      if (current != null) {
        return current;
      } else {
        return defaultStateList.get(type);
      }
    } else {
      return state;
    }
  }

  @Override
  public void grabScreenContents(final ByteBuffer store, final ImageDataFormat format, final int x, final int y,
      final int w, final int h) {
    grabScreenContents(store, format, PixelDataType.UnsignedByte, x, y, w, h);
  }

  @Override
  public int getExpectedBufferSizeToGrabScreenContents(final ImageDataFormat format, final PixelDataType type,
      final int w, final int h) {
    return w * h * ImageUtils.getPixelByteSize(format, type);
  }

  @Override
  public FloatBuffer getMatrix(final RenderMatrixType type) {
    return _matrixStore.get(type);
  }

  @Override
  public void setMatrix(final RenderMatrixType type, final FloatBuffer matrix) {
    final FloatBuffer dst = _matrixStore.get(type);
    dst.clear();
    dst.put(matrix);
  }

  @Override
  public void setMatrix(final RenderMatrixType type, final ReadOnlyMatrix4 matrix, final boolean rowMajor) {
    final FloatBuffer dst = _matrixStore.get(type);
    dst.clear();
    matrix.toFloatBuffer(dst, rowMajor);
  }

  @Override
  public void setMatrix(final RenderMatrixType type, final ReadOnlyTransform transform) {
    final FloatBuffer dst = _matrixStore.get(type);
    dst.clear();
    transform.getGLApplyMatrix(dst);
    dst.flip();
    if (type == RenderMatrixType.Model) {
      updateMVP();
    }
  }

  private void updateMVP() {
    final Matrix4 mvp = Matrix4.fetchTempInstance();
    final Matrix4 temp = Matrix4.fetchTempInstance();

    try {

      final FloatBuffer model = getMatrix(RenderMatrixType.Model);
      final FloatBuffer view = getMatrix(RenderMatrixType.View);
      final FloatBuffer projection = getMatrix(RenderMatrixType.Projection);

      // projection
      projection.rewind();
      mvp.fromFloatBuffer(projection, false);

      // view
      view.rewind();
      temp.fromFloatBuffer(view, false);
      mvp.multiplyLocal(temp);

      // model
      model.rewind();
      temp.fromFloatBuffer(model, false);
      mvp.multiplyLocal(temp);

      setMatrix(RenderMatrixType.ModelViewProjection, mvp, false);

    } finally {
      Matrix4.releaseTempInstance(temp);
      Matrix4.releaseTempInstance(mvp);
    }
  }

  @Override
  public void computeNormalMatrix(final boolean modelIsUniformScale) {
    final FloatBuffer dst = _matrixStore.get(RenderMatrixType.Normal);
    dst.clear();

    final Matrix3 normal = Matrix3.fetchTempInstance();
    final Matrix4 model = Matrix4.fetchTempInstance();
    try {
      final FloatBuffer modelBuff = _matrixStore.get(RenderMatrixType.Model);
      modelBuff.clear();
      model.fromFloatBuffer(modelBuff, false);

      if (modelIsUniformScale) {
        // normal matrix is just the 3x3 of the model matrix
        model.toMatrix3(normal);
      } else {
        // normal matrix is the inverse transpose of the 3x3 model matrix
        model.toMatrix3(normal);
        try {
          normal.invertLocal().transposeLocal();
        } catch (final ArithmeticException ex) {
          // silently ignore for now - non invertable
        }
      }
      normal.toFloatBuffer(dst, false);
    } finally {
      Matrix3.releaseTempInstance(normal);
      Matrix4.releaseTempInstance(model);
    }
  }
}
