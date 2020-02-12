/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.extension;

import java.io.IOException;
import java.io.Serializable;
import java.util.EnumMap;

import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.util.export.CapsuleUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

public class PassNodeState implements Savable, Serializable {

    private static final long serialVersionUID = 1L;

    /** if false, pass will not be updated or rendered. */
    protected boolean _enabled = true;

    /**
     * RenderStates registered with this pass - if a given state is not null it overrides the corresponding state set
     * during rendering.
     */
    protected final EnumMap<RenderState.StateType, RenderState> _passStates = new EnumMap<RenderState.StateType, RenderState>(
            RenderState.StateType.class);

    /**
     * Applies all currently set renderstates to the supplied context
     * 
     * @param context
     */
    public void applyPassNodeStates(final RenderContext context) {
        context.pushEnforcedStates();
        context.enforceStates(_passStates);
    }

    /**
     * Enforce a particular state. In other words, the given state will override any state of the same type set on a
     * scene object. Remember to clear the state when done enforcing. Very useful for multipass techniques where
     * multiple sets of states need to be applied to a scenegraph drawn multiple times.
     * 
     * @param state
     *            state to enforce
     */
    public void setPassState(final RenderState state) {
        _passStates.put(state.getType(), state);
    }

    /**
     * @param type
     *            the type to query
     * @return the state enforced for a give state type, or null if none.
     */
    public RenderState getPassState(final StateType type) {
        return _passStates.get(type);
    }

    /**
     * Clears an enforced render state index by setting it to null. This allows object specific states to be used.
     * 
     * @param type
     *            The type of RenderState to clear enforcement on.
     */
    public void clearPassState(final StateType type) {
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

    /** @return Returns the enabled. */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
     * @param enabled
     *            The enabled to set.
     */
    public void setEnabled(final boolean enabled) {
        _enabled = enabled;
    }

    public Class<? extends PassNodeState> getClassTag() {
        return this.getClass();
    }

    public void write(final OutputCapsule capsule) throws IOException {
        final OutputCapsule oc = capsule;
        oc.write(_enabled, "enabled", true);
        oc.write(_passStates.values().toArray(new RenderState[0]), "passStates", null);
    }

    public void read(final InputCapsule capsule) throws IOException {
        final InputCapsule ic = capsule;
        _enabled = ic.readBoolean("enabled", true);
        final RenderState[] states = CapsuleUtils.asArray(ic.readSavableArray("passStates", null), RenderState.class);
        _passStates.clear();
        if (states != null) {
            for (final RenderState state : states) {
                _passStates.put(state.getType(), state);
            }
        }
    }
}
