/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.state;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Stack;

import com.ardor3d.math.ObjectPool;
import com.ardor3d.math.Poolable;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.Constants;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * <code>RenderState</code> is the base class for all states that affect the rendering of a piece of
 * geometry. They aren't created directly, but are created for users from the renderer. The
 * renderstate of a parent can affect its children and it is OK to assign to more than one Spatial
 * the same render state.
 */
public abstract class RenderState implements Savable {

  public enum StateType {
    Blend, Texture, Wireframe, ZBuffer, Cull, Stencil, ColorMask, Offset;

    // cached
    public static StateType[] values = values();
  }

  /**
   * <p>
   * If false, each renderstate of that type is always applied in the renderer and only field by field
   * checks are done to minimize jni overhead. This is slower than setting to true, but relieves the
   * programmer from situations where he has to remember to update the needsRefresh field of a state.
   * </p>
   * <p>
   * If true, each renderstate of that type is checked for == with the last applied renderstate of the
   * same type. If same and the state's needsRefresh method returns false, then application of the
   * renderstate is skipped. This can be much faster than setting false, but in certain circumstances,
   * the programmer must manually set needsRefresh (for example, in a FogState, if you call
   * getFogColor().set(....) to change the color, the fogstate will not set the needsRefresh field. In
   * non-quick compare mode, this is not a problem because it will go into the apply method and do an
   * actual check of the current fog color in opengl vs. the color in the state being applied.)
   * </p>
   * <p>
   * DEFAULTS:
   * <ul>
   * <li>Blend: true</li>
   * <li>Light: false - because you can change a light object directly without telling the state</li>
   * <li>Texture: false - because you can change a texture object directly without telling the
   * state</li>
   * <li>Wireframe: false - because line attributes can change when drawing regular lines, affecting
   * wireframe lines</li>
   * <li>ZBuffer: true</li>
   * <li>Cull: true</li>
   * <li>Stencil: false</li>
   * <li>ColorMask: true</li>
   * <li>Offset: true</li>
   * </ul>
   */
  public static final EnumSet<StateType> _quickCompare = EnumSet.noneOf(StateType.class);
  static {
    RenderState._quickCompare.add(StateType.Blend);
    RenderState._quickCompare.add(StateType.ZBuffer);
    RenderState._quickCompare.add(StateType.Cull);
    RenderState._quickCompare.add(StateType.ColorMask);
    RenderState._quickCompare.add(StateType.Offset);
  }

  private static final ObjectPool<StateStack> STATESTACKS_POOL =
      ObjectPool.create(StateStack.class, Constants.maxStatePoolSize);

  static public class StateStack implements Poolable {

    private final EnumMap<RenderState.StateType, Stack<RenderState>> stacks =
        new EnumMap<>(RenderState.StateType.class);

    public StateStack() {}

    public static StateStack fetchTempInstance() {
      if (Constants.useStatePools) {
        final StateStack s = RenderState.STATESTACKS_POOL.fetch();
        // re-use already allocated stacks
        for (final Stack<RenderState> stack : s.stacks.values()) {
          stack.clear();
        }
        return s;
      } else {
        return new StateStack();
      }
    }

    public static void releaseTempInstance(final StateStack s) {
      if (Constants.useStatePools) {
        RenderState.STATESTACKS_POOL.release(s);
      }
    }

    public void push(final RenderState state) {
      Stack<RenderState> stack = stacks.get(state.getType());
      if (stack == null) {
        stack = new Stack<>();
        stacks.put(state.getType(), stack);
      }
      stack.push(state);
    }

    public void pop(final RenderState state) {
      final Stack<RenderState> stack = stacks.get(state.getType());
      stack.pop();
    }

    public void extract(final EnumMap<StateType, RenderState> states, final Spatial caller) {
      RenderState state;
      for (final Stack<RenderState> stack : stacks.values()) {
        if (!stack.isEmpty()) {
          state = stack.peek().extract(stack, caller);
          states.put(state.getType(), state);
        }
      }
    }
  }

  private boolean _enabled = true;
  private boolean _needsRefresh = false;

  /**
   * Constructs a new RenderState. The state is enabled by default.
   */
  public RenderState() {}

  /**
   * @return An statetype enum value for the subclass.
   * @see StateType
   */
  public abstract StateType getType();

  /**
   * Returns if this render state is enabled during rendering. Disabled states are ignored.
   *
   * @return True if this state is enabled.
   */
  public boolean isEnabled() { return _enabled; }

  /**
   * Sets if this render state is enabled during rendering. Disabled states are ignored.
   *
   * @param value
   *          False if the state is to be disabled, true otherwise.
   */
  public void setEnabled(final boolean value) {
    _enabled = value;
    setNeedsRefresh(true);
  }

  /**
   * Extracts from the stack the correct renderstate that should apply to the given spatial. This is
   * mainly used for RenderStates that can be cumulitive such as TextureState or LightState. By
   * default, the top of the static is returned. This function should not be called by users directly.
   *
   * @param stack
   *          The stack to extract render states from.
   * @param spat
   *          The spatial to apply the render states too.
   * @return The render state to use.
   */
  public RenderState extract(final Stack<? extends RenderState> stack, final Spatial spat) {
    // The default behavior is to return the top of the stack, the last item
    // pushed during the recursive traversal.
    return stack.peek();
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    capsule.write(_enabled, "enabled", true);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    _enabled = capsule.readBoolean("enabled", true);
  }

  @Override
  public Class<? extends RenderState> getClassTag() { return this.getClass(); }

  public abstract StateRecord createStateRecord(final ContextCapabilities caps);

  /**
   * @return true if we should apply this state even if we think it is the current state of its type
   *         in the current context. Is reset to false after apply is finished.
   */
  public boolean needsRefresh() {
    return _needsRefresh;
  }

  /**
   * This should be called by states when it knows internal data has been altered.
   *
   * @param refresh
   *          true if we should apply this state even if we think it is the current state of its type
   *          in the current context.
   */
  public void setNeedsRefresh(final boolean refresh) { _needsRefresh = refresh; }

  /**
   * @see #_quickCompare
   * @param enabled
   */
  public static void setQuickCompares(final boolean enabled) {
    RenderState._quickCompare.clear();
    if (enabled) {
      RenderState._quickCompare.addAll(EnumSet.allOf(StateType.class));
    }
  }

  public static RenderState createState(final StateType type) {
    switch (type) {
      case Blend:
        return new BlendState();
      case ColorMask:
        return new ColorMaskState();
      case Cull:
        return new CullState();
      case Offset:
        return new OffsetState();
      case Stencil:
        return new StencilState();
      case Texture:
        return new TextureState();
      case Wireframe:
        return new WireframeState();
      case ZBuffer:
        return new ZBufferState();
    }
    throw new IllegalArgumentException("Unknown state type: " + type);
  }
}
