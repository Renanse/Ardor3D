/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.pass;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import com.ardor3d.image.Texture;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.Spatial;

/**
 * <code>Pass</code> encapsulates logic necessary for rendering one or more steps in a multipass
 * technique.
 *
 * Rendering:
 *
 * When renderPass is called, a check is first made to see if the pass isEnabled(). Then any states
 * set on this pass are enforced via Spatial.enforceState(RenderState). This is useful for doing
 * things such as causing this pass to be blended to a previous pass via enforcing an BlendState,
 * etc. Next, doRender(Renderer) is called to do the actual rendering work. Finally, any enforced
 * states set before this pass was run are restored.
 */
public abstract class Pass implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /** list of Spatial objects registered with this pass. */
  protected List<Spatial> _spatials = new ArrayList<>();

  /** if false, pass will not be updated or rendered. */
  protected boolean _enabled = true;

  /**
   * RenderStates registered with this pass - if a given state is not null it overrides the
   * corresponding state set during rendering.
   */
  protected final EnumMap<RenderState.StateType, RenderState> _passStates =
      new EnumMap<>(RenderState.StateType.class);

  protected RenderContext _context = null;

  /** if enabled, set the states for this pass and then render. */
  public final void renderPass(final Renderer r) {
    if (!_enabled) {
      return;
    }
    _context = ContextManager.getCurrentContext();
    _context.pushEnforcedStates();
    _context.enforceStates(_passStates);
    doRender(r);
    _context.popEnforcedStates();
    _context = null;
  }

  /** if enabled, set the states for this pass and then render. */
  public final void renderPass(final TextureRenderer r, final int clear, final List<Texture> texs) {
    if (!_enabled) {
      return;
    }
    _context = ContextManager.getCurrentContext();
    _context.pushEnforcedStates();
    _context.enforceStates(_passStates);
    doRender(r, clear, texs);
    _context.popEnforcedStates();
    _context = null;
  }

  /**
   * Enforce a particular state. In other words, the given state will override any state of the same
   * type set on a scene object. Remember to clear the state when done enforcing. Very useful for
   * multipass techniques where multiple sets of states need to be applied to a scenegraph drawn
   * multiple times.
   * 
   * @param state
   *          state to enforce
   */
  public void setPassState(final RenderState state) {
    _passStates.put(state.getType(), state);
  }

  /**
   * Clears an enforced render state index by setting it to null. This allows object specific states
   * to be used.
   * 
   * @param type
   *          The type of RenderState to clear enforcement on.
   */
  public void clearPassState(final RenderState.StateType type) {
    _passStates.remove(type);
  }

  /**
   * sets all enforced states to null.
   * 
   * @see RenderContext#clearEnforcedState(int)
   */
  public void clearPassStates() {
    _passStates.clear();
  }

  protected abstract void doRender(Renderer r);

  protected void doRender(final TextureRenderer r, final int clear, final List<Texture> texs) {
    throw new UnsupportedOperationException("This pass type does not support RTT use.");
  }

  /** if enabled, call doUpdate to update information for this pass. */
  public final void updatePass(final double tpf) {
    if (!_enabled) {
      return;
    }
    doUpdate(tpf);
  }

  protected void doUpdate(final double tpf) {}

  public void add(final Spatial toAdd) {
    _spatials.add(toAdd);
  }

  public Spatial get(final int index) {
    return _spatials.get(index);
  }

  public boolean contains(final Spatial s) {
    return _spatials.contains(s);
  }

  public boolean remove(final Spatial toRemove) {
    return _spatials.remove(toRemove);
  }

  public int size() {
    return _spatials.size();
  }

  /**
   * @return Returns the enabled.
   */
  public boolean isEnabled() { return _enabled; }

  /**
   * @param enabled
   *          The enabled to set.
   */
  public void setEnabled(final boolean enabled) { _enabled = enabled; }

  public void cleanUp() {}

}
