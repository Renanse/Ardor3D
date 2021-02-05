/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.interact.filter;

import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.extension.interact.widget.DragState;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.math.util.MathUtils;

/**
 * UpdateFilter useful for delaying drag interactions. Delaying the drag interaction is useful in
 * situations such as where the mouse is also used for scene navigation and inadvertent interaction
 * is possible or likely.
 */
public abstract class AbstractDragDelayFilter extends UpdateFilterAdapter {
  protected float _delayTime;
  protected float _exitTime;
  protected long _dragStartTime;

  /**
   *
   * @param delayTime
   * @param exitTime
   */
  public AbstractDragDelayFilter(final float delayTime, final float exitTime) {
    _delayTime = delayTime;
    _exitTime = exitTime;
  }

  @Override
  public void beginDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState current) {
    if (widget.getDragState() == DragState.START_DRAG) {
      _dragStartTime = System.currentTimeMillis();
      widget.setDragState(DragState.PREPARE_DRAG);
      showTimerViz(manager, widget, current);
    } else if (widget.getDragState() == DragState.PREPARE_DRAG) {
      final float elapsed = (System.currentTimeMillis() - _dragStartTime) / 1000f;
      if (elapsed > getDelayTime()) {
        widget.setDragState(DragState.DRAG);
        updateTimerViz(manager, widget, current, 1f);
        clearTimerViz(manager, widget, current, false);
      } else {
        updateTimerViz(manager, widget, current, MathUtils.clamp01(elapsed / getDelayTime()));
      }
    }
  }

  @Override
  public void endDrag(final InteractManager manager, final AbstractInteractWidget widget, final MouseState current) {
    clearTimerViz(manager, widget, current, true);
  }

  public float getDelayTime() { return _delayTime; }

  public void setDelayTime(final float seconds) { _delayTime = seconds; }

  public float getExitTime() { return _exitTime; }

  public void setExitTime(final float seconds) { _exitTime = seconds; }

  /**
   * Set up our visualization. Called once at the start of the attempted drag/mouse press.
   *
   * @param manager
   *          the InteractManger being used.
   * @param widget
   *          the widget this filter is acting on
   * @param current
   *          the current MouseState involved in the interaction
   */
  protected abstract void showTimerViz(final InteractManager manager, final AbstractInteractWidget widget,
      final MouseState current);

  /**
   * Update the visualization for the current progress.
   *
   * @param manager
   *          the InteractManger being used.
   * @param widget
   *          the widget this filter is acting on
   * @param current
   *          the current MouseState involved in the interaction
   * @param percent
   *          the progress, as a percent, made so far in the delay prior to allowing the drag. In the
   *          range, [0f, 1f]
   */
  protected abstract void updateTimerViz(final InteractManager manager, final AbstractInteractWidget widget,
      final MouseState current, final float percent);

  /**
   * Clear our timer visualization, possibly adding an exit animation, if we don't need to immediately
   * clear.
   *
   * @param manager
   *          the InteractManger being used.
   * @param widget
   *          the widget this filter is acting on
   * @param current
   *          the current MouseState involved in the interaction
   * @param immediate
   *          if true, immediately clear the time visualization. If false, it is okay to play some
   *          exit animation/etc. before clearing the timer.
   */
  protected abstract void clearTimerViz(final InteractManager manager, final AbstractInteractWidget widget,
      final MouseState current, final boolean immediate);
}
