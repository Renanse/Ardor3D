/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.ui.event.DragListener;
import com.ardor3d.extension.ui.util.HudListener;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.InputState;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.character.CharacterInputEvent;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.logical.BasicTriggersApplier;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.GrabbedState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseManager;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;

/**
 * UIHud represents a "Heads Up Display" or the base of a game UI scenegraph. Various UI Input,
 * dragging, events, etc. are handled through this class.
 */
public class UIHud extends Node {

  private static final Logger _logger = Logger.getLogger(UIHud.class.getName());

  public static int MOUSE_CLICK_SENSITIVITY = 5;

  /**
   * The logical layer used by this UI to receive input events.
   */
  protected final LogicalLayer _logicalLayer = new LogicalLayer();

  /**
   * The single tooltip used by this hud - lazy inited
   */
  protected UITooltip _ttip;

  /**
   * Internal flag indicating whether the last input event was consumed by the UI. This is used to
   * decide if we will forward the event to the next LogicalLayer.
   */
  protected boolean _mouseInputConsumed;
  protected boolean _keyInputConsumed;

  /**
   * If true, we'll assume mouse input is always consumed whenever lastMouseOverComponent is not null.
   */
  protected boolean _autoConsumeMouseOnOver = true;

  /**
   * Flag used to determine if we should use mouse input when mouse is grabbed. Defaults to true.
   */
  protected boolean _ignoreMouseInputOnGrabbed = true;

  /** Which button is used for drag operations. Defaults to LEFT. */
  protected MouseButton _dragButton = MouseButton.LEFT;

  /**
   * Tracks the previous component our mouse was over so we can properly handle mouse entered and
   * departed events.
   */
  protected UIComponent _lastMouseOverComponent;

  /** Tracks last mouse location so we can detect movement. */
  protected int _lastMouseX, _lastMouseY;

  /** Tracks last mouse pressed so we can detect clicks. */
  protected int _mousePressedX, _mousePressedY;

  /**
   * List of potential drag listeners. When a drag operation is detected, we will offer it to each
   * item in the list until one accepts it.
   */
  protected final List<WeakReference<DragListener>> _dragListeners = new ArrayList<>();

  /** Our current drag listener. When an drag finished, this is set back to null. */
  protected DragListener _dragListener = null;

  /** The component that currently has key focus - key events will be sent to this component. */
  protected UIComponent _focusedComponent = null;

  /**
   * List of hud listeners.
   */
  protected final List<HudListener> _hudListeners = new ArrayList<>();

  /**
   * An optional mouseManager, required in order to test mouse is grabbed.
   */
  protected MouseManager _mouseManager;

  /**
   * The list of currently displayed pop-overs, with each entry being a "child" of the one previous.
   */
  protected final List<IPopOver> _popovers = new ArrayList<>();

  protected final Canvas _canvas;

  protected UIInputPostHook _postKeyboardHook;

  protected UIInputPostHook _postMouseHook;

  protected final Camera _hudCamera;

  /**
   * Construct a new UIHud for a given canvas
   */
  public UIHud(final Canvas canvas) {
    setName("UIHud");
    _canvas = canvas;
    _hudCamera = Camera.newOrthoCamera(_canvas);

    getSceneHints().setCullHint(CullHint.Never);
    getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    getSceneHints().setLightCombineMode(LightCombineMode.Off);
    getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
    setLayer(Spatial.LAYER_UI);

    final ZBufferState zstate = new ZBufferState();
    zstate.setEnabled(false);
    zstate.setWritable(false);
    setRenderState(zstate);

    setupLogicalLayer();
  }

  /**
   * @return the last detected x position of the mouse.
   */
  public int getLastMouseX() { return _lastMouseX; }

  /**
   * @return the last detected y position of the mouse.
   */
  public int getLastMouseY() { return _lastMouseY; }

  /**
   * @return this hud's associated tooltip object.
   */
  public UITooltip getTooltip() {
    if (_ttip == null) {
      _ttip = new UITooltip();
    }
    return _ttip;
  }

  /**
   * Add the given component to this hud.
   *
   * @param component
   *          the component to add
   */
  public void add(final UIComponent component) {
    attachChild(component);
    component.attachedToHud();
    component.fireComponentDirty();
    for (final HudListener hl : _hudListeners) {
      hl.componentAdded(component);
    }
  }

  /**
   * Remove the given component from the hud
   *
   * @param component
   *          the component to remove
   */
  public void remove(final UIComponent component) {
    // first lose focus, if appropriate
    if (_focusedComponent != null && (_focusedComponent == component || _focusedComponent.hasAncestor(component))) {
      setFocusedComponent(null);
    }
    // drop our last mouse over component if applicable
    if (_lastMouseOverComponent != null
        && (_lastMouseOverComponent == component || _lastMouseOverComponent.hasAncestor(component))) {
      _lastMouseOverComponent = null;
    }

    component.detachedFromHud();
    detachChild(component);
    for (final HudListener hl : _hudListeners) {
      hl.componentRemoved(component);
    }
  }

  /**
   * Overridden to force component detachments to go through {@link #remove(UIComponent)}
   */
  @Override
  public void detachAllChildren() {
    if (getNumberOfChildren() > 0) {
      for (int i = getNumberOfChildren() - 1; i >= 0; i--) {
        final Spatial spat = getChild(i);
        if (spat instanceof UIComponent) {
          remove((UIComponent) spat);
        } else {
          detachChildAt(i);
        }
      }
    }
  }

  /**
   * Reorder the components so that the given component is drawn last and is therefore "on top" of any
   * others.
   *
   * @param component
   *          the component to bring to front
   */
  public void bringToFront(final UIComponent component) {
    getChildren().remove(component);
    getChildren().add(component);
  }

  /**
   * Look for a UIComponent at the given screen coordinates. If no pickable component is at that
   * location, null is returned.
   *
   * @param x
   *          the x screen coordinate
   * @param y
   *          the y screen coordinate
   * @return the picked component or null if nothing pickable was found at the given coordinates.
   */
  public UIComponent getUIComponent(final int x, final int y) {
    UIComponent found = null;

    // check for a popover first
    for (int i = _popovers.size(); --i >= 0;) {
      final IPopOver popover = _popovers.get(i);
      found = popover.getUIComponent(x, y);
      if (found != null) {
        return found;
      }
    }

    for (int i = getNumberOfChildren(); --i >= 0;) {
      final Spatial s = getChild(i);
      if (s instanceof UIComponent) {
        final UIComponent comp = (UIComponent) s;
        if (!comp.isVisible()) {
          continue;
        }

        found = comp.getUIComponent(x, y);

        if (found != null) {
          break;
        }
      }
    }

    return found;
  }

  /**
   * @return the component that currently has key focus or null if none.
   */
  public UIComponent getFocusedComponent() { return _focusedComponent; }

  /**
   * @param compomponent
   *          the component that should now have key focus. If this component has a focus target, that
   *          will be used instead.
   */
  public void setFocusedComponent(final UIComponent compomponent) {
    // If we already have a different focused component, tell it that it has lost focus.
    if (_focusedComponent != null && _focusedComponent != compomponent) {
      final UIComponent oldFocus = _focusedComponent;
      _focusedComponent = null;
      oldFocus.lostFocus();
    }

    // Set our focused component to the given component (or its focus target)
    if (compomponent != null) {
      if (compomponent.getKeyFocusTarget() != null) {
        // set null so we don't re-tell it that it lost focus.
        _focusedComponent = null;
        // recurse down to target.
        setFocusedComponent(compomponent.getKeyFocusTarget());
      } else {
        _focusedComponent = compomponent;
        // let our new focused component know it has focus
        _focusedComponent.gainedFocus();
      }
    } else {
      _focusedComponent = null;
    }
  }

  /**
   * @return the MouseButton that triggers drag operations
   */
  public MouseButton getDragButton() { return _dragButton; }

  /**
   * @param dragButton
   *          set the MouseButton that triggers drag operations
   */
  public void setDragButton(final MouseButton dragButton) { _dragButton = dragButton; }

  /**
   * Override to force setting ortho before drawing and to specifically handle draw order of
   * components and tool tip.
   */
  @Override
  public void draw(final Renderer r) {
    final Camera cam = Camera.getCurrentCamera();
    r.getQueue().pushBuckets();
    _hudCamera.apply(r);
    try {
      Spatial child;
      int i, max;
      for (i = 0, max = getNumberOfChildren(); i < max; i++) {
        child = getChild(i);
        if (child != null) {
          child.onDraw(r);
        }
      }
      if (!_popovers.isEmpty()) {
        for (i = 0, max = _popovers.size(); i < max; i++) {
          _popovers.get(i).onDraw(r);
        }
      }
      if (_ttip != null && _ttip.isVisible()) {
        _ttip.onDraw(r);
      }
    } catch (final Exception e) {
      UIHud._logger.logp(Level.SEVERE, getClass().getName(), "draw(Renderer)", "Exception", e);
    } finally {
      r.getScissorUtils().clearClips();
    }
    r.renderBuckets();
    r.getQueue().popBuckets();
    cam.apply(r);
  }

  @Override
  public void updateGeometricState(final double time, final boolean initiator) {
    super.updateGeometricState(time, initiator);
    if (!_popovers.isEmpty()) {
      for (int i = 0, max = _popovers.size(); i < max; i++) {
        _popovers.get(i).updateGeometricState(time, true);
      }
    }
    if (_ttip != null && _ttip.isVisible()) {
      _ttip.updateGeometricState(time, true);
    }
  }

  /**
   * Add the given drag listener to this hud. Expired WeakReferences are also cleaned.
   *
   * @param listener
   *          the listener to add
   */
  public void addDragListener(final DragListener listener) {
    _dragListeners.add(new WeakReference<>(listener));

    // Clean list.
    for (int i = _dragListeners.size(); --i >= 0;) {
      if (_dragListeners.get(i).get() == null) {
        _dragListeners.remove(i);
      }
    }
  }

  /**
   * Remove any matching drag listener from this hud. Expired WeakReferences are also cleaned.
   *
   * @param listener
   *          the listener to remove
   * @return true if at least one "equal" DragListener was found in the pool of listeners and removed.
   */
  public boolean removeDragListener(final DragListener listener) {
    boolean rVal = false;
    for (int i = _dragListeners.size(); --i >= 0;) {
      final DragListener dl = _dragListeners.get(i).get();
      if (dl == null) {
        _dragListeners.remove(i);
      } else if (dl.equals(listener)) {
        _dragListeners.remove(i);
        rVal = true;
      }
    }
    return rVal;
  }

  /**
   * Add the given hud listener to this hud.
   *
   * @param listener
   *          the listener to add
   */
  public void addHudListener(final HudListener listener) {
    _hudListeners.add(listener);
  }

  /**
   * Remove any matching hud listener from this hud.
   *
   * @param listener
   *          the listener to remove
   * @return true if at least one "equal" HudListener was found in the pool of listeners and removed.
   */
  public boolean removeHudListener(final HudListener listener) {
    return _hudListeners.remove(listener);
  }

  /**
   * @return the logical layer associated with this hud. When chaining UI logic to game logic, this
   *         LogicalLayer is the one to call checkTriggers on.
   */
  public LogicalLayer getLogicalLayer() { return _logicalLayer; }

  /**
   * @return true if we should ignore (only forward) mouse input when the mouse is set to grabbed. If
   *         true, requires mouse manager to be set or this param is ignored.
   */
  public boolean isIgnoreMouseInputOnGrabbed() { return _ignoreMouseInputOnGrabbed; }

  /**
   * @param mouseInputOnGrabbed
   *          true if we should ignore (only forward) mouse input when the mouse is set to grabbed. If
   *          true, requires mouse manager to be set or this param is ignored.
   * @see #setMouseManager(MouseManager)
   */
  public void setIgnoreMouseInputOnGrabbed(final boolean mouseInputOnGrabbed) {
    _ignoreMouseInputOnGrabbed = mouseInputOnGrabbed;
  }

  /**
   * @return a MouseManager used to test if mouse is grabbed, or null if none was set.
   */
  public MouseManager getMouseManager() { return _mouseManager; }

  /**
   * @param manager
   *          a MouseManager used to test if mouse is grabbed.
   */
  public void setMouseManager(final MouseManager manager) { _mouseManager = manager; }

  /**
   * Convenience method for setting up the UI's connection to the Ardor3D input system, along with a
   * forwarding address for input events that the UI does not care about.
   *
   * @param canvas
   *          the canvas to register with
   * @param physicalLayer
   *          the physical layer to register with
   * @param forwardTo
   *          a LogicalLayer to send unconsumed (by the UI) input events to.
   */
  public void setupInput(final PhysicalLayer physicalLayer, final LogicalLayer forwardTo) {
    // Set up this logical layer to listen for events from the given canvas and PhysicalLayer
    _logicalLayer.registerInput(_canvas, physicalLayer);

    // Set up forwarding for events not consumed.
    if (forwardTo != null) {
      _logicalLayer.setApplier(new BasicTriggersApplier() {

        @Override
        public void checkAndPerformTriggers(final Set<InputTrigger> triggers, final Canvas source,
            final TwoInputStates states, final double tpf) {
          super.checkAndPerformTriggers(triggers, source, states, tpf);

          final InputState prev = states.getPrevious();
          final InputState curr = states.getCurrent();
          if (!_mouseInputConsumed && (!_autoConsumeMouseOnOver || _lastMouseOverComponent == null)) {
            if (!_keyInputConsumed) {
              // nothing consumed
              forwardTo.getApplier().checkAndPerformTriggers(forwardTo.getTriggers(), source, states, tpf);
            } else {
              // only key state consumed
              final TwoInputStates forwardingState = new TwoInputStates(new InputState(KeyboardState.NOTHING, //
                  prev.getMouseState(), //
                  prev.getControllerState(), //
                  prev.getGestureState(), //
                  prev.getCharacterState()),
                  new InputState(KeyboardState.NOTHING, //
                      curr.getMouseState(), //
                      curr.getControllerState(), //
                      curr.getGestureState(), //
                      curr.getCharacterState()));
              forwardTo.getApplier().checkAndPerformTriggers(forwardTo.getTriggers(), source, forwardingState, tpf);
            }
          } else {
            if (!_keyInputConsumed) {
              // only mouse consumed
              final TwoInputStates forwardingState = new TwoInputStates(new InputState(prev.getKeyboardState(), //
                  MouseState.NOTHING, //
                  prev.getControllerState(), //
                  prev.getGestureState(), //
                  prev.getCharacterState()),
                  new InputState(curr.getKeyboardState(), //
                      MouseState.NOTHING, //
                      curr.getControllerState(), //
                      curr.getGestureState(), //
                      curr.getCharacterState()));
              forwardTo.getApplier().checkAndPerformTriggers(forwardTo.getTriggers(), source, forwardingState, tpf);
            } else {
              // both consumed, do nothing.
            }
          }

          _mouseInputConsumed = false;
          _keyInputConsumed = false;
        }
      });
    }
  }

  /**
   * Set up our logical layer with a trigger that hands input to the UI and saves whether it was
   * "consumed".
   */
  protected void setupLogicalLayer() {
    _logicalLayer.registerTrigger(new InputTrigger((final TwoInputStates arg0) -> true, (source, inputStates, tpf) -> {
      _mouseInputConsumed = offerMouseInputToUI(inputStates);
      _keyInputConsumed = offerKeyInputToUI(inputStates);
    }));
  }

  protected boolean offerKeyInputToUI(final TwoInputStates inputStates) {
    boolean consumed = false;
    final InputState current = inputStates.getCurrent();

    // Keyboard checks
    {
      final KeyboardState previousKState = inputStates.getPrevious().getKeyboardState();
      final KeyboardState currentKState = current.getKeyboardState();
      if (!currentKState.getKeysDown().isEmpty()) {
        // new presses
        final EnumSet<Key> pressed = currentKState.getKeysPressedSince(previousKState);
        if (!pressed.isEmpty()) {
          for (final Key key : pressed) {
            consumed |= fireKeyPressedEvent(key, current);
          }
        }

        // repeats
        final EnumSet<Key> repeats = currentKState.getKeysHeldSince(previousKState);
        if (!repeats.isEmpty() && _focusedComponent != null) {
          for (final Key key : repeats) {
            consumed |= fireKeyHeldEvent(key, current);
          }
        }
      }

      // key releases
      if (!previousKState.getKeysDown().isEmpty()) {
        final EnumSet<Key> released = currentKState.getKeysReleasedSince(previousKState);
        if (!released.isEmpty()) {
          for (final Key key : released) {
            consumed |= fireKeyReleasedEvent(key, current);
          }
        }
      }
    }

    {
      // Character input
      final List<CharacterInputEvent> events = current.getCharacterState().getEvents();
      if (!events.isEmpty()) {
        for (final CharacterInputEvent e : events) {
          fireCharacterReceived(e.getValue(), current);
        }
      }
    }

    // Post hook, if we have one
    {
      if (_postKeyboardHook != null) {
        consumed |= _postKeyboardHook.process(consumed, this, inputStates);
      }
    }

    return consumed;

  }

  /**
   * Parse a given set of input states for UI events and pass these events to the UI components
   * contained in this hud.
   *
   * @param inputStates
   *          our two InputState objects, detailing a before and after snapshot of the input system.
   * @return true if a UI element consumed the event described by inputStates.
   */
  protected boolean offerMouseInputToUI(final TwoInputStates inputStates) {
    boolean consumed = false;
    final InputState current = inputStates.getCurrent();

    // Mouse checks.
    if (!isIgnoreMouseInputOnGrabbed() || _mouseManager == null || _mouseManager.getGrabbed() != GrabbedState.GRABBED) {
      final MouseState previousMState = inputStates.getPrevious().getMouseState();
      final MouseState currentMState = current.getMouseState();
      if (previousMState != currentMState) {
        // Check for presses.
        if (currentMState.hasButtonState(ButtonState.DOWN)) {
          final EnumSet<MouseButton> pressed = currentMState.getButtonsPressedSince(previousMState);
          if (!pressed.isEmpty()) {
            for (final MouseButton button : pressed) {
              consumed |= fireMouseButtonPressed(button, current);
            }
          }
        }

        // Check for releases.
        if (previousMState.hasButtonState(ButtonState.DOWN)) {
          final EnumSet<MouseButton> released = currentMState.getButtonsReleasedSince(previousMState);
          if (!released.isEmpty()) {
            for (final MouseButton button : released) {
              consumed |= fireMouseButtonReleased(button, current);
            }
          }
        }

        // Check for mouse movement
        if (currentMState.getDx() != 0 || currentMState.getDy() != 0) {
          consumed |= fireMouseMoved(currentMState.getX(), currentMState.getY(), current);
        }

        // Check for wheel change
        if (currentMState.getDwheel() != 0) {
          consumed |= fireMouseWheelMoved(currentMState.getDwheel(), current);
        }
      }

      // Post hook, if we have one
      {
        if (_postMouseHook != null) {
          consumed |= _postMouseHook.process(consumed, this, inputStates);
        }
      }
    }
    return consumed || _dragListener != null;
  }

  /**
   * Handle mouse presses.
   *
   * @param button
   *          the button that was pressed.
   * @param currentIS
   *          the current input state.
   * @return true if this event is consumed.
   */
  public boolean fireMouseButtonPressed(final MouseButton button, final InputState currentIS) {
    boolean consumed = false;
    final int mouseX = currentIS.getMouseState().getX(), mouseY = currentIS.getMouseState().getY();
    final UIComponent over = getUIComponent(mouseX, mouseY);

    setFocusedComponent(over);
    _mousePressedX = mouseX;
    _mousePressedY = mouseY;

    if (over == null) {
      closePopupMenus();
      return false;
    } else {
      consumed |= over.mousePressed(button, currentIS);
    }

    // Check if the component we are pressing on is "draggable"
    for (final WeakReference<DragListener> ref : _dragListeners) {
      final DragListener listener = ref.get();
      if (listener == null) {
        continue;
      }

      if (listener.isDragHandle(over, mouseX, mouseY)) {
        listener.startDrag(mouseX, mouseY);
        _dragListener = listener;
        consumed = true;
        break;
      }
    }

    // bring any clicked components to front
    final UIComponent component = over.getTopLevelComponent();
    if (component != null && !(component instanceof IPopOver)) {
      bringToFront(component);
      closePopupMenus();
    }
    return consumed;
  }

  /**
   * Handle mouse releases.
   *
   * @param button
   *          the button that was release.
   * @param currentIS
   *          the current input state.
   * @return true if this event is consumed.
   */
  public boolean fireMouseButtonReleased(final MouseButton button, final InputState currentIS) {
    boolean consumed = false;
    final int mouseX = currentIS.getMouseState().getX(), mouseY = currentIS.getMouseState().getY();
    final UIComponent over = getUIComponent(mouseX, mouseY);

    if (over != null) {
      consumed |= over.mouseReleased(button, currentIS);
      final int distance = Math.abs(mouseX - _mousePressedX) + Math.abs(mouseY - _mousePressedY);

      if (distance < UIHud.MOUSE_CLICK_SENSITIVITY) {
        over.mouseClicked(button, currentIS);
      }
    }

    if (button == _dragButton && _dragListener != null) {
      _dragListener.endDrag(over, mouseX, mouseY);
      _dragListener = null;
      consumed = true;
    }

    return consumed;
  }

  /**
   * Handle movement events.
   *
   * @param mouseX
   *          the new x position of the mouse
   * @param mouseY
   *          the new y position of the mouse
   * @param currentIS
   *          the current input state.
   * @return if this event is consumed.
   */
  public boolean fireMouseMoved(final int mouseX, final int mouseY, final InputState currentIS) {
    _lastMouseX = mouseX;
    _lastMouseY = mouseY;

    // Check for drag movements.
    if (currentIS.getMouseState().getButtonState(_dragButton) == ButtonState.DOWN) {
      if (_dragListener != null) {
        _dragListener.drag(mouseX, mouseY);
        return true;
      }
    }

    // grab the component the mouse is over, if any
    final UIComponent over = getUIComponent(mouseX, mouseY);

    boolean consumed = false;

    // Are we over a component? let it know we moved inside it.
    if (over != null) {
      consumed |= over.mouseMoved(mouseX, mouseY, currentIS);
    }

    // component points to a different UIComponent than before, so mark departed
    if (over == null || over != _lastMouseOverComponent) {
      if (_lastMouseOverComponent != null) {
        _lastMouseOverComponent.mouseDeparted(mouseX, mouseY, currentIS);
      }
      if (over != null) {
        over.mouseEntered(mouseX, mouseY, currentIS);
      }
    }
    _lastMouseOverComponent = over;

    return consumed;
  }

  /**
   * Handle wheel events.
   *
   * @param wheelDx
   *          the change in wheel position.
   * @param currentIS
   *          the current input state.
   * @return if this event is consumed.
   */
  public boolean fireMouseWheelMoved(final int wheelDx, final InputState currentIS) {
    final UIComponent over = getUIComponent(currentIS.getMouseState().getX(), currentIS.getMouseState().getY());

    if (over == null) {
      return false;
    }

    return over.mouseWheel(wheelDx, currentIS);
  }

  /**
   * Handle key presses.
   *
   * @param key
   *          the pressed key
   * @param currentIS
   *          the current input state.
   * @return if this event is consumed.
   */
  public boolean fireKeyPressedEvent(final Key key, final InputState currentIS) {
    if (_focusedComponent != null) {
      return _focusedComponent.keyPressed(key, currentIS);
    } else {
      return false;
    }
  }

  /**
   * Handle key held (pressed down over more than one input update cycle.)
   *
   * @param key
   *          the held key
   * @param currentIS
   *          the current input state.
   * @return if this event is consumed.
   */
  public boolean fireKeyHeldEvent(final Key key, final InputState currentIS) {
    if (_focusedComponent != null) {
      return _focusedComponent.keyHeld(key, currentIS);
    } else {
      return false;
    }
  }

  /**
   * Handle key releases.
   *
   * @param key
   *          the released key
   * @param currentIS
   *          the current input state.
   * @return if this event is consumed.
   */
  public boolean fireKeyReleasedEvent(final Key key, final InputState currentIS) {
    if (_focusedComponent != null) {
      return _focusedComponent.keyReleased(key, currentIS);
    } else {
      return false;
    }
  }

  /**
   * Handle receipt of character values, generally after typing on the keyboard.
   *
   * @param value
   *          character value received.
   * @param currentIS
   *          the current input state.
   * @return if this event is consumed.
   */
  public boolean fireCharacterReceived(final char value, final InputState currentIS) {
    if (_focusedComponent != null) {
      return _focusedComponent.characterReceived(value, currentIS);
    } else {
      return false;
    }
  }

  public int getWidth() { return _canvas.getCanvasRenderer().getCamera().getWidth(); }

  public int getHeight() { return _canvas.getCanvasRenderer().getCamera().getHeight(); }

  public Camera getHudCamera() { return _hudCamera; }

  public void closePopupMenus() {
    for (final IPopOver menu : _popovers) {
      if (menu.isAttachedToHUD()) {
        menu.close();
      }
    }
    _popovers.clear();
  }

  public void closePopupMenusAfter(final Object parent) {
    if (parent == this) {
      closePopupMenus();
      return;
    }

    boolean found = false;
    for (final Iterator<IPopOver> it = _popovers.iterator(); it.hasNext();) {
      final IPopOver pMenu = it.next();
      if (found) {
        pMenu.close();
        it.remove();
      } else if (pMenu == parent) {
        found = true;
      }
    }
  }

  public void showPopOver(final IPopOver pop) {
    closePopupMenus();
    _popovers.add(pop);
    pop.setHud(this);
  }

  public void showSubPopupMenu(final IPopOver pop) {
    _popovers.remove(pop);
    _popovers.add(pop);
    pop.setHud(this);
  }

  public UIInputPostHook getPostKeyboardHook() { return _postKeyboardHook; }

  public void setPostKeyboardHook(final UIInputPostHook postKeyboardHook) { _postKeyboardHook = postKeyboardHook; }

  public UIInputPostHook getPostMouseHook() { return _postMouseHook; }

  public void setPostMouseHook(final UIInputPostHook postMouseHook) { _postMouseHook = postMouseHook; }

  public Canvas getCanvas() { return _canvas; }

  public interface UIInputPostHook {
    boolean process(boolean consumed, UIHud source, TwoInputStates inputStates);
  }

  public void setAutoConsumeMouseOnOver(final boolean autoConsumeMouseOnOver) {
    _autoConsumeMouseOnOver = autoConsumeMouseOnOver;
  }

  public boolean isAutoConsumeMouseOnOver() { return _autoConsumeMouseOnOver; }

  public boolean isDragging(final InputState state) {
    return state != null && state.getMouseState().getButtonState(getDragButton()) == ButtonState.DOWN;
  }
}
