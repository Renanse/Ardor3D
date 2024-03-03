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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.ui.layout.BorderLayout;
import com.ardor3d.extension.ui.layout.BorderLayoutData;
import com.ardor3d.extension.ui.layout.RowLayout;
import com.ardor3d.extension.ui.layout.UILayoutData;
import com.ardor3d.extension.ui.util.ButtonGroup;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.scenegraph.Spatial;

/**
 * A container similar to Swing's {@link javax.swing.JTabbedPane} in which multiple components may
 * be contained and viewed one at a time using a set of navigation buttons.
 */
public class UITabbedPane extends UIPanel {
  private static final Logger logger = Logger.getLogger(UITabbedPane.class.getName());

  /**
   * Our contents... Used instead of the normal children field because we need to track and show just
   * one at a time.
   */
  private final ArrayList<UIComponent> _contents = new ArrayList<>();

  /** The panel containing our navigation tab buttons. */
  private final UIPanel _tabsPanel;

  /** The placement enum describing which edge to place the tab buttons on. */
  private final TabPlacement _placement;

  /**
   * Our button group. The tabs are toggle buttons and this allows us to toggle only one at a time.
   */
  private final ButtonGroup _tabButtonGroup = new ButtonGroup();

  /** The currently viewed tab index. -1 indicates no current view (empty tab pane) */
  private int _currentTabIndex = -1;

  /** Our center panel - will hold the currently viewed content item. */
  final UIPanel _center = new UIPanel(new BorderLayout());

  /**
   * Construct a new Tabbed Pane with the given tab placement.
   *
   * @param placement
   *          which edge to place the tab buttons on
   */
  public UITabbedPane(final TabPlacement placement) {
    super(new BorderLayout());

    _placement = placement;

    // Setup our center content panel
    _center.setLayoutData(BorderLayoutData.CENTER);
    super.add(_center);

    // Create a panel that will hold our tab buttons
    final RowLayout layout = new RowLayout(_placement.isHorizontal(), false, false);
    _tabsPanel = new UIPanel(layout);
    _tabsPanel.setLayoutData(_placement.getLayoutData());
    super.add(_tabsPanel);

    applySkin();
  }

  /**
   * @return which edge we place the tab buttons on
   */
  public TabPlacement getTabPlacement() { return _placement; }

  /**
   * @return the index of the currently viewed tab or -1 if nothing is currently viewed.
   */
  public int getCurrentTab() { return _currentTabIndex; }

  /**
   * @param index
   * @return the tab button at the given index.
   */
  public UITab getTabButton(final int index) {
    return getTabs().get(index);
  }

  /**
   * @param index
   * @return the component contents for the given tab index.
   */
  public UIComponent getTabContents(final int index) {
    return _contents.get(index);
  }

  @Override
  public void layout() {
    super.layout();

    // Make sure all of our visible and non-visible contents are properly resized.
    if (_contents != null) {
      for (int x = 0, max = _contents.size(); x < max; x++) {
        final Spatial child = _contents.get(x);
        if (child instanceof UIComponent comp) {
          comp.setLocalComponentSize(getViewedComponent().getLocalComponentWidth(),
              getViewedComponent().getLocalComponentHeight());
          comp.layout();
        }
      }
    }
  }

  /**
   * @return the number of components/tabs in this tabbed pane.
   */
  public int getTabCount() { return _contents.size(); }

  /**
   * @return the currently viewed tab's component. If no component is viewed, null is returned.
   */
  public UIComponent getViewedComponent() {
    if (_currentTabIndex >= _contents.size()) {
      _currentTabIndex = -1;
    }
    if (_currentTabIndex < 0) {
      return null;
    }
    return _contents.get(_currentTabIndex);
  }

  /**
   * Adds the given component to this tabbed pane with no icon and "unnamed" as the tab label.
   */
  @Override
  public <T extends UIComponent> T add(final T component) {
    return add(component, "unnamed", null);
  }

  /**
   * Adds the given component to this tabbed pane using the given tab label text and no icon.
   *
   * @param component
   *          the component to add
   * @param label
   *          the text of the tab label
   */
  public <T extends UIComponent> T add(final T component, final String label) {
    return add(component, label, null);
  }

  /**
   * Adds the given component to this tabbed pane using the given tab label text and icon.
   *
   * @param component
   *          the component to add
   * @param label
   *          the text of the tab label
   * @param icon
   *          the icon of the tab label
   */
  public <T extends UIComponent> T add(final T component, final String label, final SubTex icon) {
    if (component == null) {
      UITabbedPane.logger.log(Level.WARNING, "Can not add a null component to a TabbedPane.");
      return null;
    }

    // make sure the component uses center layout position
    component.setLayoutData(BorderLayoutData.CENTER);

    // add our component to the local tracking
    _contents.add(component);

    // Make our new tab and add it to the tab panel
    final UITab tab = makeTab(label, icon);
    _tabsPanel.add(tab);

    // Set as viewed, if this is our only tab
    if (_contents.size() == 1) {
      setCurrentTab(0);
    }

    return component;
  }

  /**
   * Remove the tab at the given tab index.
   *
   * @param index
   *          the tab index
   */
  public void removeTab(final int index) {
    int newIndex = _currentTabIndex;

    // clear the panel if removing the current tab
    if (index == _currentTabIndex) {
      clearViewedComponent();
    }

    // Remove the tab and contents for this index.
    final ArrayList<UITab> tabs = getTabs();
    _tabsPanel.remove(tabs.get(index));
    _contents.remove(index);

    // If our previously viewed tab was above the removed index, or we were at the end of the stack,
    // decrement to
    // keep us on the same tab.
    if (_currentTabIndex > index || newIndex >= _contents.size()) {
      newIndex--;
    }

    // If our new potential viewed index is valid, set it as the index
    if (newIndex >= 0) {
      setCurrentTab(newIndex);
    } else {
      // otherwise just fire that we're dirty.
      fireComponentDirty();
    }
  }

  /**
   * Set the currently viewed component to the one associated with the given tab.
   *
   * @param tab
   *          the tab
   */
  public void setCurrentTab(final UITab tab) {
    setCurrentTab(getTabs().indexOf(tab));
  }

  /**
   * Set the currently viewed component to the one at the given tab index.
   *
   * @param index
   *          the tab index
   */
  public void setCurrentTab(final int index) {
    if (index > _contents.size() || index < 0) {
      throw new IndexOutOfBoundsException("" + index);
    }
    _currentTabIndex = index;

    // clear the current contents
    clearViewedComponent();

    // set our new contents
    final UIComponent centerComp = _contents.get(_currentTabIndex);
    centerComp.setLayoutData(BorderLayoutData.CENTER);
    _center.add(centerComp);

    // Update our buttons
    final ArrayList<UITab> buttons = getTabs();
    for (int x = buttons.size(); --x >= 0;) {
      buttons.get(x).setSelected(x == _currentTabIndex);
    }

    // resize and layout
    updateMinimumSizeFromContents();
    layout();
  }

  /**
   * @return an array of tabs from our tab panel.
   */
  private ArrayList<UITab> getTabs() {
    final ArrayList<UITab> buttons = new ArrayList<>();
    for (int x = 0, max = _tabsPanel.getNumberOfChildren(); x < max; x++) {
      final Spatial spat = _tabsPanel.getChild(x);
      if (spat instanceof UITab) {
        buttons.add((UITab) spat);
      }
    }
    return buttons;
  }

  /**
   * Remove the currently viewed component from view. Usually the is done in preparation for setting a
   * new view.
   */
  private void clearViewedComponent() {
    _center.removeAllComponents();
  }

  /**
   * Make our tab button using the given label text and icon.
   *
   * @param label
   *          optional label text
   * @param icon
   *          optional label icon
   * @return the new UITab
   */
  private UITab makeTab(final String label, final SubTex icon) {
    final UITab button = new UITab(label, icon, _placement);
    button.addActionListener(event -> setCurrentTab(button));
    _tabButtonGroup.add(button);
    return button;
  }

  /**
   * An enum describing the edge on which to place tab buttons.
   */
  public enum TabPlacement {
    /** Place tabs along top edge of pane. */
    NORTH(BorderLayoutData.NORTH, true),

    /** Place tabs along bottom edge of pane. */
    SOUTH(BorderLayoutData.SOUTH, true),

    /** Place tabs along right edge of pane. */
    EAST(BorderLayoutData.EAST, false),

    /** Place tabs along left edge of pane. */
    WEST(BorderLayoutData.WEST, false);

    private final UILayoutData _layoutData;
    private final boolean _horizontal;

    TabPlacement(final BorderLayoutData layData, final boolean horizontal) {
      _layoutData = layData;
      _horizontal = horizontal;
    }

    UILayoutData getLayoutData() { return _layoutData; }

    public boolean isHorizontal() { return _horizontal; }
  }
}
