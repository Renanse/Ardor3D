/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.geom;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

/**
 * @deprecated use {@link Spatial#makeCopy(boolean)} instead.
 */
@Deprecated
public class SharedCopyLogic implements CopyLogic {
  private static final Logger logger = Logger.getLogger(SharedCopyLogic.class.getName());

  @Override
  public Spatial copy(final Spatial source, final AtomicBoolean recurse) {
    recurse.set(false);
    if (source instanceof Node) {
      recurse.set(true);
      return clone((Node) source);
    } else if (source instanceof Mesh) {
      final Mesh result = clone((Mesh) source);
      result.setMeshData(((Mesh) source).getMeshData());
      result.updateModelBound();
      return result;
    }
    return null;
  }

  protected Mesh clone(final Mesh original) {
    Mesh copy = null;
    try {
      copy = original.getClass().newInstance();
    } catch (final InstantiationException e) {
      logger.log(Level.SEVERE, "Could not access final constructor of class " + original.getClass().getCanonicalName(),
          e);
      throw new RuntimeException(e);
    } catch (final IllegalAccessException e) {
      logger.log(Level.SEVERE, "Could not access final constructor of class " + original.getClass().getCanonicalName(),
          e);
      throw new RuntimeException(e);
    }
    copy.setName(original.getName() + "_copy");
    copy.getSceneHints().set(original.getSceneHints());
    copy.setTransform(original.getTransform());
    copy.setDefaultColor(original.getDefaultColor());

    for (final StateType type : StateType.values()) {
      final RenderState state = original.getLocalRenderState(type);
      if (state != null) {
        copy.setRenderState(state);
      }
    }
    return copy;
  }

  protected Node clone(final Node original) {
    Node copy = null;
    try {
      copy = original.getClass().newInstance();
    } catch (final InstantiationException e) {
      logger.log(Level.SEVERE, "Could not access final constructor of class " + original.getClass().getCanonicalName(),
          e);
      throw new RuntimeException(e);
    } catch (final IllegalAccessException e) {
      logger.log(Level.SEVERE, "Could not access final constructor of class " + original.getClass().getCanonicalName(),
          e);
      throw new RuntimeException(e);
    }
    copy.setName(original.getName() + "_copy");
    copy.getSceneHints().set(original.getSceneHints());
    copy.setTransform(original.getTransform());

    for (final StateType type : StateType.values()) {
      final RenderState state = original.getLocalRenderState(type);
      if (state != null) {
        copy.setRenderState(state);
      }
    }
    return copy;
  }
}
