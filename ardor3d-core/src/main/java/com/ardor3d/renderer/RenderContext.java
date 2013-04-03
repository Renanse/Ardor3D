/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer;

import java.util.EmptyStackException;
import java.util.EnumMap;
import java.util.Stack;

import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.LineRecord;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.renderer.state.record.StateRecord;

/**
 * Represents the state of an individual context in OpenGL.
 */
public class RenderContext {

    /** List of states that override any set states on a spatial if not null. */
    protected final EnumMap<RenderState.StateType, RenderState> _enforcedStates = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    protected final Stack<EnumMap<StateType, RenderState>> _enforcedBackStack = new Stack<EnumMap<StateType, RenderState>>();

    protected final Stack<AbstractFBOTextureRenderer> _textureRenderers = new Stack<AbstractFBOTextureRenderer>();

    /** RenderStates a Spatial contains during rendering. */
    protected final EnumMap<RenderState.StateType, RenderState> _currentStates = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    protected final EnumMap<RenderState.StateType, StateRecord> _stateRecords = new EnumMap<RenderState.StateType, StateRecord>(
            RenderState.StateType.class);

    protected final LineRecord _lineRecord = new LineRecord();
    protected final RendererRecord _rendererRecord = createRendererRecord();

    /** Basically this object represents the sharable portion of a GL context... Textures, displayLists, etc. */
    protected final Object _glContextRep;

    protected final ContextCapabilities _capabilities;

    /** The object tied to this RenderContext, such as the Canvas, etc. */
    protected final Object _contextKey;

    protected Camera _currentCamera = null;

    public RenderContext(final Object key, final ContextCapabilities caps) {
        this(key, caps, null);
    }

    public RenderContext(final Object key, final ContextCapabilities caps, final RenderContext shared) {
        _contextKey = key;
        _capabilities = caps;
        setupRecords();
        _glContextRep = (shared == null) ? new Object() : shared._glContextRep;
    }

    protected RendererRecord createRendererRecord() {
        final RendererRecord rendererRecord = new RendererRecord();
        return rendererRecord;
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
        _lineRecord.invalidate();
        _rendererRecord.invalidate();

        clearCurrentStates();
    }

    public ContextCapabilities getCapabilities() {
        return _capabilities;
    }

    public StateRecord getStateRecord(final RenderState.StateType type) {
        return _stateRecords.get(type);
    }

    public LineRecord getLineRecord() {
        return _lineRecord;
    }

    public RendererRecord getRendererRecord() {
        return _rendererRecord;
    }

    /**
     * Enforce a particular state. In other words, the given state will override any state of the same type set on a
     * scene object. Remember to clear the state when done enforcing. Very useful for multipass techniques where
     * multiple sets of states need to be applied to a scenegraph drawn multiple times.
     * 
     * @param state
     *            state to enforce
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
     * Clears an enforced render state index by setting it to null. This allows object specific states to be used.
     * 
     * @param type
     *            The type of RenderState to clear enforcement on.
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
     *            the state type to clear.
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

    public Object getContextKey() {
        return _contextKey;
    }

    public void setCurrentState(final StateType type, final RenderState state) {
        _currentStates.put(type, state);
    }

    public Camera getCurrentCamera() {
        return _currentCamera;
    }

    public void setCurrentCamera(final Camera cam) {
        _currentCamera = cam;
    }

    public Object getGlContextRep() {
        return _glContextRep;
    }

    /**
     * Saves the currently set states to a stack. Does not changes the currently enforced states.
     */
    public void pushEnforcedStates() {
        _enforcedBackStack.push(new EnumMap<StateType, RenderState>(_enforcedStates));
    }

    /**
     * Restores the enforced states from the stack. Any states enforced or cleared since the last push are reverted.
     * 
     * @throws EmptyStackException
     *             if this method is called without first calling {@link #pushEnforcedStates()}
     */
    public void popEnforcedStates() {
        _enforcedStates.clear();
        _enforcedStates.putAll(_enforcedBackStack.pop());
    }

    public void pushFBOTextureRenderer(final AbstractFBOTextureRenderer top) {
        if (_textureRenderers.size() > 0) {
            _textureRenderers.peek().deactivate();
        }
        _textureRenderers.push(top);
        top.activate();
    }

    public void popFBOTextureRenderer() {
        AbstractFBOTextureRenderer top = _textureRenderers.pop();
        top.deactivate();
        if (_textureRenderers.size() > 0) {
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
