/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scene.state.lwjgl3;

import org.lwjgl.opengl.GL11C;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.state.record.ZBufferStateRecord;

public abstract class Lwjgl3ZBufferStateUtil {

  public static void apply(final ZBufferState state) {
    // ask for the current state record
    final RenderContext context = ContextManager.getCurrentContext();
    final ZBufferStateRecord record = (ZBufferStateRecord) context.getStateRecord(StateType.ZBuffer);
    context.setCurrentState(StateType.ZBuffer, state);

    enableDepthTest(state.isEnabled(), record);
    if (state.isEnabled()) {
      int depthFunc = switch (state.getFunction()) {
        case Never -> GL11C.GL_NEVER;
        case LessThan -> GL11C.GL_LESS;
        case EqualTo -> GL11C.GL_EQUAL;
        case LessThanOrEqualTo -> GL11C.GL_LEQUAL;
        case GreaterThan -> GL11C.GL_GREATER;
        case NotEqualTo -> GL11C.GL_NOTEQUAL;
        case GreaterThanOrEqualTo -> GL11C.GL_GEQUAL;
        case Always -> GL11C.GL_ALWAYS;
        default -> throw new IllegalStateException("Unexpected value: " + state.getFunction());
      };
      applyFunction(depthFunc, record);
    }

    enableWrite(state.isWritable(), record);

    if (!record.isValid()) {
      record.validate();
    }
  }

  private static void enableDepthTest(final boolean enable, final ZBufferStateRecord record) {
    if (enable && (!record.depthTest || !record.isValid())) {
      GL11C.glEnable(GL11C.GL_DEPTH_TEST);
      record.depthTest = true;
    } else if (!enable && (record.depthTest || !record.isValid())) {
      GL11C.glDisable(GL11C.GL_DEPTH_TEST);
      record.depthTest = false;
    }
  }

  private static void applyFunction(final int depthFunc, final ZBufferStateRecord record) {
    if (depthFunc != record.depthFunc || !record.isValid()) {
      GL11C.glDepthFunc(depthFunc);
      record.depthFunc = depthFunc;
    }
  }

  private static void enableWrite(final boolean enable, final ZBufferStateRecord record) {
    if (enable != record.writable || !record.isValid()) {
      GL11C.glDepthMask(enable);
      record.writable = enable;
    }
  }
}
