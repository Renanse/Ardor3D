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

import java.util.EmptyStackException;
import java.util.EnumMap;
import java.util.Stack;

import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.renderer.texture.AbstractFBOTextureRenderer;
import com.ardor3d.scenegraph.SceneIndexer;

/**
 * Represents the state of an individual context in OpenGL.
 */
public class RenderContext {

  public static class RenderContextRef {}

  /** List of states that override any set states on a spatial if not null. */
  protected final EnumMap<RenderState.StateType, RenderState> _enforcedStates =
      new EnumMap<>(RenderState.StateType.class);

  protected final Stack<EnumMap<StateType, RenderState>> _enforcedBackStack = new Stack<>();

  protected final Stack<AbstractFBOTextureRenderer> _textureRenderers = new Stack<>();

  /** RenderStates a Spatial contains during rendering. */
  protected final EnumMap<RenderState.StateType, RenderState> _currentStates =
      new EnumMap<>(RenderState.StateType.class);

  protected final EnumMap<RenderState.StateType, StateRecord> _stateRecords =
      new EnumMap<>(RenderState.StateType.class);

  protected RenderMaterial _enforcedMaterial = null;
  protected Stack<RenderMaterial> _materialBackStack = new Stack<>();

  protected final RendererRecord _rendererRecord = createRendererRecord();

  /** Represents the non-sharable portion of a GL context... VAO, etc. */
  protected final RenderContextRef _uniqueContextRef = new RenderContextRef();

  /** The object tied to this RenderContext, such as the Canvas, etc. */
  protected final Object _contextKey;

  /** Represents the sharable portion of a GL context... Textures, displayLists, etc. */
  protected RenderContextRef _sharableContextRef;

  protected ContextCapabilities _capabilities;

  protected Camera _currentCamera = null;

  protected CanvasRenderer _currentCanvasRenderer = null;

  protected SceneIndexer _sceneIndexer = new SceneIndexer();

  protected RenderPhase _renderPhase = RenderPhase.Scene;

  public RenderContext(final Object key) {
    this(key, null);
  }

  public RenderContext(final Object key, final RenderContext shared) {
    _contextKey = key;
    setSharedContext(shared);
  }

  public void setSharedContext(final RenderContext shared) {
    _sharableContextRef = (shared == null) ? new RenderContextRef() : shared._sharableContextRef;
  }

  public void setCapabilities(final ContextCapabilities caps) {
    _capabilities = caps;
    setupRecords();
  }

  protected RendererRecord createRendererRecord() {
    return new RendererRecord();
  }

  protected void setupRecords() {
    for (final RenderState.StateType type : RenderState.StateType.values()) {
      _stateRecords.put(type, RenderState.createState(type).createStateRecord(_capabilities));
    }
  }

  public void invalidateStates() {
    for (final RenderState.StateType type : RenderState.StateType.values()) {
      _stateRecords.get(type).invalidate();
    }
    _rendererRecord.invalidate();

    clearCurrentStates();
  }

  public ContextCapabilities getCapabilities() { return _capabilities; }

  public StateRecord getStateRecord(final RenderState.StateType type) {
    return _stateRecords.get(type);
  }

  public RendererRecord getRendererRecord() { return _rendererRecord; }

  /**
   * Enforce a particular state. In other words, the given state will override any state of the same
   * type set on a scene object. Remember to clear the state when done enforcing. Very useful for
   * multipass techniques where multiple sets of states need to be applied to a scenegraph drawn
   * multiple times.
   *
   * @param state
   *          state to enforce
   */
  public void enforceState(final RenderState state) {
    _enforcedStates.put(state.getType(), state);
  }

  /**
   * Enforces the states referenced in the given EnumMap.
   */
  public void enforceStates(final EnumMap<StateType, RenderState> states) {
    _enforcedStates.putAll(states);
  }

  /**
   * Clears an enforced render state index by setting it to null. This allows object specific states
   * to be used.
   *
   * @param type
   *          The type of RenderState to clear enforcement on.
   */
  public void clearEnforcedState(final RenderState.StateType type) {
    _enforcedStates.remove(type);
  }

  /**
   * sets all enforced states to null.
   */
  public void clearEnforcedStates() {
    _enforcedStates.clear();
  }

  /**
   * sets all current states to null, and therefore forces the use of the default states.
   */
  public void clearCurrentStates() {
    _currentStates.clear();
  }

  /**
   * @param type
   *          the state type to clear.
   */
  public void clearCurrentState(final RenderState.StateType type) {
    _currentStates.remove(type);
  }

  public boolean hasEnforcedStates() {
    return !_enforcedStates.isEmpty();
  }

  public RenderState getEnforcedState(final RenderState.StateType type) {
    return _enforcedStates.get(type);
  }

  public RenderState getCurrentState(final RenderState.StateType type) {
    return _currentStates.get(type);
  }

  public void enforceMaterial(final RenderMaterial material) {
    _enforcedMaterial = material;
  }

  public RenderMaterial getEnforcedMaterial() { return _enforcedMaterial; }

  public Object getContextKey() { return _contextKey; }

  public void setCurrentState(final StateType type, final RenderState state) {
    _currentStates.put(type, state);
  }

  public Camera getCurrentCamera() { return _currentCamera; }

  public void setCurrentCamera(final Camera cam) { _currentCamera = cam; }

  public CanvasRenderer getCurrentCanvasRenderer() { return _currentCanvasRenderer; }

  public void setCurrentCanvasRenderer(final CanvasRenderer renderer) { _currentCanvasRenderer = renderer; }

  public SceneIndexer getSceneIndexer() { return _sceneIndexer; }

  public void setSceneIndexer(final SceneIndexer indexer) { _sceneIndexer = indexer; }

  public RenderContextRef getSharableContextRef() { return _sharableContextRef; }

  public RenderContextRef getUniqueContextRef() { return _uniqueContextRef; }

  public RenderPhase getRenderPhase() { return _renderPhase; }

  public void setRenderPhase(final RenderPhase renderPhase) { _renderPhase = renderPhase; }

  /**
   * Saves the currently set states to a stack. Does not changes the currently enforced states.
   */
  public void pushEnforcedStates() {
    _enforcedBackStack.push(new EnumMap<>(_enforcedStates));
  }

  /**
   * Restores the enforced states from the stack. Any states enforced or cleared since the last push
   * are reverted.
   *
   * @throws EmptyStackException
   *           if this method is called without first calling {@link #pushEnforcedStates()}
   */
  public void popEnforcedStates() {
    _enforcedStates.clear();
    _enforcedStates.putAll(_enforcedBackStack.pop());
  }

  public void pushEnforcedMaterial() {
    _materialBackStack.push(_enforcedMaterial);
  }

  public void popEnforcedMaterial() {
    _enforcedMaterial = _materialBackStack.pop();
  }

  public void pushFBOTextureRenderer(final AbstractFBOTextureRenderer top) {
    if (!_textureRenderers.isEmpty()) {
      _textureRenderers.peek().deactivate();
    }
    _textureRenderers.push(top);
    top.activate();
  }

  public void popFBOTextureRenderer() {
    AbstractFBOTextureRenderer top = _textureRenderers.pop();
    top.deactivate();
    if (!_textureRenderers.isEmpty()) {
      top = _textureRenderers.peek();
      top.activate();
    }
  }

  /**
   * Should only be called on a thread with an active context.
   */
  public void contextLost() {
    // Notify any interested parties of the deletion.
    ContextManager.fireCleanContextEvent(this);

    // invalidate our render states
    invalidateStates();

    // force camera update
    if (_currentCamera != null) {
      _currentCamera.update();
    }
  }

}
