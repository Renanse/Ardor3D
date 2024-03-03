/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui;

import java.util.EnumSet;

import com.ardor3d.extension.ui.event.DragListener;
import com.ardor3d.extension.ui.event.DrawerDragListener;
import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;

/**
 *
 */
public class UIDrawer extends UIContainer {
  /** The panel meant to hold the contents of the drawer. */
  protected UIPanel _contentPanel;

  /** The top title bar of the drawer, part of the drawer's "chrome" */
  protected final UIDrawerBar _titleBar;

  /**
   * The top, hidden panel of the drawer, holding the title bar, allowing it to be smaller than the
   * drawer contents.
   */
  protected final UIPanel _titleBarContainer;

  /**
   * The drag listener responsible for allowing repositioning of the drawer by dragging the title
   * label.
   */
  protected DragListener _dragListener = new DrawerDragListener(this);

  /** If true (the default) then allow dragging of this drawer using the drawer bar. */
  protected boolean _draggable = true;

  protected boolean _dragInterupt = false;

  /** If false (the default) the drawer is collapsed into the edge of the screen. */
  protected boolean _expanded = true;

  protected static double _slideSpeed = 175;

  protected DrawerEdge _edge;

  /**
   * Construct a new UIDrawer with the given title and default buttons (CLOSE).
   *
   * @param title
   *          the text to display on the title bar of this drawer
   */
  public UIDrawer(final String title) {
    this(title, DrawerEdge.BOTTOM, EnumSet.of(DrawerButtons.CLOSE));
  }

  /**
   * Construct a new UIDrawer with the given title and default buttons (CLOSE).
   *
   * @param title
   *          the text to display on the title bar of this drawer
   * @param edge
   *          the edge to place the drawer on.
   */
  public UIDrawer(final String title, final DrawerEdge edge) {
    this(title, edge, EnumSet.of(DrawerButtons.CLOSE));
  }

  /**
   * Construct a new UIDrawer with the given title and button.
   *
   * @param title
   *          the text to display on the title bar of this drawer
   * @param edge
   *          the edge to place the drawer on.
   * @param buttons
   *          which buttons we should show in the drawer bar.
   */
  public UIDrawer(final String title, final DrawerEdge edge, final EnumSet<DrawerButtons> buttons) {
    setLayout(new BorderLayout());

    _edge = edge;

    _titleBarContainer = new UIPanel("titleContainer");
    _titleBarContainer.setLayout(new RowLayout(_edge == DrawerEdge.TOP || _edge == DrawerEdge.BOTTOM, false, false));
    add(_titleBarContainer);

    _contentPanel = new UIPanel("contentPanel");
    _contentPanel.setLayout(new RowLayout(_edge == DrawerEdge.TOP || _edge == DrawerEdge.BOTTOM));
    _contentPanel.setLayoutData(BorderLayoutData.CENTER);
    add(_contentPanel);

    _titleBar = new UIDrawerBar(buttons, this);
    switch (_edge) {
      case LEFT:
        _titleBarContainer.setLayoutData(BorderLayoutData.EAST);
        break;
      case RIGHT:
        _titleBarContainer.setLayoutData(BorderLayoutData.WEST);
        break;
      case TOP:
        _titleBarContainer.setLayoutData(BorderLayoutData.SOUTH);
        break;
      default:
      case BOTTOM:
        _titleBarContainer.setLayoutData(BorderLayoutData.NORTH);
        break;
    }

    setTitle(title);
    _titleBarContainer.add(_titleBar);

    applySkin();
  }

  /**
   * @param draggable
   *          true if we should allow dragging of this drawer via a drawer bar.
   */
  public void setDraggable(final boolean draggable) { _draggable = draggable; }

  /**
   * @return true if this drawer allows dragging.
   */
  public boolean isDraggable() { return _draggable && !_dragInterupt; }

  public static double getSlideSpeed() { return UIDrawer._slideSpeed; }

  public static void setSlideSpeed(final double slideSpeed) { UIDrawer._slideSpeed = slideSpeed; }

  @Override
  public UIComponent getUIComponent(final int hudX, final int hudY) {
    final UIComponent picked = super.getUIComponent(hudX, hudY);
    if (picked == _titleBarContainer) {
      return null;
    }
    return picked;
  }

  /**
   * Remove this drawer from the hud it is attached to.
   *
   * @throws IllegalStateException
   *           if drawer is not currently attached to a hud.
   */
  public void close() {
    final UIHud hud = getHud();
    if (hud == null) {
      throw new IllegalStateException("UIDrawer is not attached to a hud.");
    }

    // Remove our drag listener
    hud.removeDragListener(_dragListener);

    // When a drawer closes, close any open tooltip
    hud.getTooltip().setVisible(false);

    // clear any resources for standin
    clearStandin();

    // clean up any state
    acceptVisitor((final Spatial spatial) -> {
      if (spatial instanceof StateBasedUIComponent comp) {
        comp.switchState(comp.getDefaultState());
      }
    }, true);

    hud.remove(this);
    _parent = null;
  }

  public boolean isExpanded() { return _expanded; }

  public DrawerEdge getEdge() { return _edge; }

  public UIPanel getTitleBarContainer() { return _titleBarContainer; }

  /**
   * @return this drawer's title bar
   */
  public UIDrawerBar getTitleBar() { return _titleBar; }

  /**
   * @return the center content panel of this drawer.
   */
  public UIPanel getContentPanel() { return _contentPanel; }

  /**
   * Replaces the content panel of this drawer with a new one.
   *
   * @param panel
   *          the new content panel.
   */
  public void setContentPanel(final UIPanel panel) {
    remove(_contentPanel);
    _contentPanel = panel;
    panel.setLayoutData(BorderLayoutData.CENTER);
    add(panel);
  }

  /**
   * @return the current title of this drawer
   */
  public String getTitle() {
    if (_titleBar != null) {
      return _titleBar.getTitleLabel().getText();
    }

    return null;
  }

  /**
   * Sets the title of this drawer
   *
   * @param title
   *          the new title
   */
  public void setTitle(final String title) {
    if (_titleBar != null) {
      _titleBar.getTitleLabel().setText(title);
      _titleBar.layout();
    }
  }

  @Override
  public void attachedToHud() {
    super.attachedToHud();
    // add our drag listener to the hud
    getHud().addDragListener(_dragListener);

    switch (getEdge()) {
      case LEFT:
        setHudX(0);
        break;
      case RIGHT:
        setHudX(getHud().getWidth() - getLocalComponentWidth());
        break;
      case TOP:
        setHudY(getHud().getHeight() - getLocalComponentHeight());
        break;
      default:
      case BOTTOM:
        setHudY(0);
        break;
    }
  }

  @Override
  public void detachedFromHud() {
    super.detachedFromHud();

    // Remove our drag listener from the hud
    if (getHud() != null) {
      getHud().removeDragListener(_dragListener);
    }
  }

  @Override
  public void pack() {
    updateMinimumSizeFromContents();
    // grab the desired width and height of the drawer.
    final int width = _contentPanel.getMinimumLocalComponentWidth();
    final int height = _contentPanel.getMinimumLocalComponentHeight() + _titleBar.getLocalComponentHeight();

    // Set our size, obeying min sizes.
    setLocalComponentSize(width, height);

    // Layout the panel
    layout();
  }

  /**
   * Set a new drag listener on this drawer.
   *
   * @param listener
   *          the drag listener. Must not be null.
   */
  public void setDragListener(final DragListener listener) {
    assert listener != null : "listener must not be null";
    if (isAttachedToHUD()) {
      getHud().removeDragListener(_dragListener);
    }
    _dragListener = listener;
    if (isAttachedToHUD()) {
      getHud().addDragListener(_dragListener);
    }
  }

  protected void applyExpansionAnimation() {
    if (!isAttachedToHUD()) {
      return;
    }

    // decide our new target
    final int start, target;
    switch (getEdge()) {
      case BOTTOM:
        start = getHudY();
        target = _expanded ? 0 : -_contentPanel.getLocalComponentHeight();
        break;
      case TOP:
        start = getHudY();
        target = getHud().getHeight() + (_expanded ? -getLocalComponentHeight() : -_titleBar.getLocalComponentHeight());
        break;
      case LEFT:
        start = getHudX();
        target = _expanded ? 0 : -_contentPanel.getLocalComponentWidth();
        break;
      case RIGHT:
        start = getHudX();
        target = getHud().getWidth() + (_expanded ? -getLocalComponentWidth() : -_titleBar.getLocalComponentWidth());
        break;
      default:
        return;
    }

    if (start == target) {
      return;
    }

    // temporarily disable drag
    _dragInterupt = true;

    clearControllers();
    addController(new SpatialController<>() {
      double pos = start;
      double dir = Math.signum(target - start);

      @Override
      public void update(final double time, final Spatial caller) {
        boolean done = false;
        pos += UIDrawer._slideSpeed * time * dir;
        if (dir > 0 && pos >= target || dir < 0 && pos <= target) {
          pos = target;
          done = true;
        }

        final ReadOnlyVector3 translation = getTranslation();
        switch (getEdge()) {
          case BOTTOM:
          case TOP:
            UIDrawer.this.setTranslation(translation.getX(), pos, translation.getZ());
            break;
          case LEFT:
          case RIGHT:
            UIDrawer.this.setTranslation(pos, translation.getY(), translation.getZ());
            break;
          default:
            break;
        }

        if (done) {
          _dragInterupt = false;
          UIDrawer.this.removeController(this);
          return;
        }
      }
    });
  }

  /**
   * Enumeration of possible drawer chrome buttons.
   */
  public enum DrawerButtons {
    CLOSE,
    // TEAROFF, RESTORE;
  }

  public enum DrawerEdge {
    TOP, LEFT, BOTTOM, RIGHT
  }

  public void toggleExpanded() {
    if (_dragInterupt) {
      return;
    }

    _expanded = !_expanded;
    applyExpansionAnimation();
  }

  public void setExpanded(final boolean expanded, final boolean immediate) {
    if (!isAttachedToHUD()) {
      System.err.println("Must be attached to hud first!");
      return;
    }
    if (!immediate) {
      applyExpansionAnimation();
      return;
    }

    _expanded = expanded;

    final int target;
    final ReadOnlyVector3 translation = getTranslation();
    switch (getEdge()) {
      case BOTTOM:
        target = _expanded ? 0 : -_contentPanel.getLocalComponentHeight();
        setTranslation(translation.getX(), target, translation.getZ());
        break;
      case TOP:
        target = getHud().getHeight() + (_expanded ? -getLocalComponentHeight() : -_titleBar.getLocalComponentHeight());
        setTranslation(translation.getX(), target, translation.getZ());
        break;
      case LEFT:
        target = _expanded ? 0 : -_contentPanel.getLocalComponentWidth();
        setTranslation(target, translation.getY(), translation.getZ());
        break;
      case RIGHT:
        target = getHud().getWidth() + (_expanded ? -getLocalComponentWidth() : -_titleBar.getLocalComponentWidth());
        setTranslation(target, translation.getY(), translation.getZ());
        break;
      default:
        return;
    }
  }
}
