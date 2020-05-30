/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.queue;

import java.util.Comparator;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.TransparencyType;

public class TransparentRenderBucket extends AbstractRenderBucket {
  /** CullState for two pass transparency rendering. */
  private final CullState _tranparentCull;

  /** ZBufferState for two pass transparency rendering. */
  private final ZBufferState _transparentZBuff;

  public TransparentRenderBucket() {
    super();

    _tranparentCull = new CullState();
    _transparentZBuff = new ZBufferState();
    _transparentZBuff.setWritable(false);
    _transparentZBuff.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);

    _comparator = new TransparentComparator();
  }

  @Override
  public void render(final Renderer renderer) {
    // Grab our render context - used to enforce renderstates
    final RenderContext context = ContextManager.getCurrentContext();

    // go through our bucket contents
    Spatial spatial;
    for (int i = 0; i < _currentListSize; i++) {
      spatial = _currentList[i];

      // make sure we have a real spatial
      if (spatial == null) {
        continue;
      }

      // we only care about altering the Mesh... perhaps could be altered later to some interface shared
      // by Mesh
      // and other leaf nodes.
      if (spatial instanceof Mesh) {

        // get our transparency rendering type.
        final TransparencyType renderType = spatial.getSceneHints().getTransparencyType();

        // check for one of the two pass types...
        if (renderType != TransparencyType.OnePass) {

          // get handle to Mesh
          final Mesh mesh = (Mesh) spatial;

          // check if we have a Cull state set or enforced. If one is explicitly set and is not Face.None,
          // we'll not do two-pass transparency.
          RenderState setState = context.hasEnforcedStates() ? context.getEnforcedState(StateType.Cull) : null;
          if (setState == null) {
            setState = mesh.getWorldRenderState(RenderState.StateType.Cull);
          }

          // Do the described check.
          if (setState == null || ((CullState) setState).getCullFace() == Face.None) {

            // pull any currently enforced cull or zstate. We'll put them back afterwards
            final RenderState oldCullState = context.getEnforcedState(StateType.Cull);
            final RenderState oldZState = context.getEnforcedState(StateType.ZBuffer);

            // enforce our cull and zstate. The zstate is setup to respect depth, but not write to it.
            context.enforceState(_tranparentCull);
            context.enforceState(_transparentZBuff);

            // first render back-facing tris only
            _tranparentCull.setCullFace(CullState.Face.Front);
            mesh.draw(renderer);

            // revert z state
            context.clearEnforcedState(StateType.ZBuffer);
            if (oldZState != null) {
              context.enforceState(oldZState);
            }

            // render front-facing tris
            _tranparentCull.setCullFace(CullState.Face.Back);
            mesh.draw(renderer);

            // revert cull state
            if (oldCullState != null) {
              context.enforceState(oldCullState);
            } else {
              context.clearEnforcedState(StateType.Cull);
            }
            continue;
          }
        }
      }

      // go ahead and draw as usual
      spatial.draw(renderer);
    }
  }

  private class TransparentComparator implements Comparator<Spatial> {
    @Override
    public int compare(final Spatial o1, final Spatial o2) {
      final double d1 = distanceToCam(o1);
      final double d2 = distanceToCam(o2);
      if (d1 > d2) {
        return -1;
      } else if (d1 < d2) {
        return 1;
      } else {
        return 0;
      }
    }
  }

}
