/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.layout;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.math.Rectangle2;
import com.ardor3d.scenegraph.Spatial;

/**
 * This layout places components in either a horizontal or vertical row, ordered as they are placed
 * in their container. Depending on settings, the layout may also take any extra space in the
 * container and divide it up equally among child components that are marked as "layout resizeable".
 */
public class RowLayout extends UILayout {

  private static final int MAX_RELAX = 50;
  private final boolean _horizontal;
  private final boolean _expandsHorizontally;
  private final boolean _expandsVertically;
  private int _spacing = 0;

  /**
   * Construct a new RowLayout
   *
   * @param horizontal
   *          true if we should lay out horizontally, false if vertically
   */
  public RowLayout(final boolean horizontal) {
    this(horizontal, true, true);
  }

  /**
   * Construct a new RowLayout
   *
   * @param horizontal
   *          true if we should lay out horizontally, false if vertically
   * @param expandsHorizontally
   *          true (the default) if horizontal free space in the container should be divided up among
   *          the child components.
   * @param expandsVertically
   *          true (the default) if vertical free space in the container should be divided up among
   *          the child components.
   */
  public RowLayout(final boolean horizontal, final boolean expandsHorizontally, final boolean expandsVertically) {
    _horizontal = horizontal;
    _expandsHorizontally = expandsHorizontally;
    _expandsVertically = expandsVertically;
  }

  /**
   * @return true if we lay out horizontally, false if vertically
   */
  public boolean isHorizontal() { return _horizontal; }

  /**
   * @return true (the default) if horizontal free space in the container should be divided up among
   *         the child components.
   */
  public boolean isExpandsHorizontally() { return _expandsHorizontally; }

  /**
   *
   * @return true (the default) if vertical free space in the container should be divided up among the
   *         child components.
   */
  public boolean isExpandsVertically() { return _expandsVertically; }

  @Override
  public void layoutContents(final UIContainer container) {

    final List<Spatial> content = container.getChildren();
    if (content == null) {
      return;
    }

    final Rectangle2 storeA = Rectangle2.fetchTempInstance();
    final Rectangle2 storeB = Rectangle2.fetchTempInstance();

    // Grab a list of components, squeezing them down to their min size on the flow axis
    List<UIComponent> comps = new ArrayList<>();
    List<UIComponent> compsBack = new ArrayList<>();
    for (int i = 0; i < content.size(); i++) {
      final Spatial spat = content.get(i);
      if (spat instanceof UIComponent comp) {
        final Rectangle2 rect = comp.getRelativeComponentBounds(storeA);
        final Rectangle2 minRect = comp.getRelativeMinComponentBounds(storeB);
        if (_horizontal) {
          comp.fitComponentIn(minRect.getWidth(), rect.getHeight());
        } else {
          comp.fitComponentIn(rect.getWidth(), minRect.getHeight());
        }
        comps.add(comp);
      }
    }

    // if we have components to layout...
    if (!comps.isEmpty()) {

      // Determine how much space we feel we need.
      final int reqSpace =
          _spacing * (comps.size() - 1) + (_horizontal ? getSumOfAllWidths(content) : getSumOfAllHeights(content));

      // How much extra space do we have?
      int freeSpace = (_horizontal ? container.getContentWidth() : container.getContentHeight()) - reqSpace;

      int relaxIndex = 0;
      // cycle through until we've given away all of the space
      while ((freeSpace > 0 || relaxIndex == 0) && !comps.isEmpty() && relaxIndex < RowLayout.MAX_RELAX) {
        final int extraPerComp = freeSpace / comps.size();
        while (!comps.isEmpty()) {
          final UIComponent comp = comps.remove(0);
          Rectangle2 rect = comp.getRelativeComponentBounds(storeA);
          final Rectangle2 origRect = storeB.set(rect);
          if (freeSpace < 0) {
            freeSpace = 0;
          }
          if (_horizontal) {
            final int height = _expandsVertically ? container.getContentHeight() : rect.getHeight();
            final int width = (_expandsHorizontally ? extraPerComp : 0) + rect.getWidth();
            if (height == rect.getHeight() && width == rect.getWidth()) {
              continue;
            }

            comp.fitComponentIn(width, height);
            rect = comp.getRelativeComponentBounds(storeA);
            if (Math.abs(rect.getWidth() - width) <= 1) {
              compsBack.add(comp);
            }
            freeSpace -= rect.getWidth() - origRect.getWidth();
          } else {
            final int width = _expandsHorizontally ? container.getContentWidth() : rect.getWidth();
            final int height = (_expandsVertically ? extraPerComp : 0) + rect.getHeight();
            if (height == rect.getHeight() && width == rect.getWidth()) {
              continue;
            }

            comp.fitComponentIn(width, height);
            rect = comp.getRelativeComponentBounds(storeA);
            if (Math.abs(rect.getHeight() - height) <= 1) {
              compsBack.add(comp);
            }
            freeSpace -= rect.getHeight() - origRect.getHeight();
          }
        }
        final List<UIComponent> compsTemp = comps;
        comps = compsBack;
        compsBack = compsTemp;
        relaxIndex++;
      }

      int x = 0;
      int y = !_expandsVertically && !_horizontal ? container.getContentHeight() - reqSpace : 0;

      // Now, go through children and set proper location.
      for (int i = 0; i < content.size(); i++) {
        final Spatial spat = _horizontal ? content.get(i) : content.get(content.size() - i - 1);

        if (!(spat instanceof UIComponent comp)) {
          continue;
        }
        final Rectangle2 rect = comp.getRelativeComponentBounds(storeA);

        if (_horizontal) {
          comp.setLocalXY(x - rect.getX(),
              Math.max(container.getContentHeight() / 2 - rect.getHeight() / 2 - rect.getY(), 0));
          x += rect.getWidth() + _spacing;
        } else {
          comp.setLocalXY(Math.max(container.getContentWidth() / 2 - rect.getWidth() / 2 - rect.getX(), 0),
              y - rect.getY());
          y += rect.getHeight() + _spacing;
        }
      }
    }

    Rectangle2.releaseTempInstance(storeA);
    Rectangle2.releaseTempInstance(storeB);
  }

  @Override
  public void updateMinimumSizeFromContents(final UIContainer container) {

    int minW = 0, minH = 0;
    if (container.getNumberOfChildren() > 0) {
      final List<Spatial> content = container.getChildren();

      // compute the min width and height of the container
      final Rectangle2 store = new Rectangle2();
      int spaces = -1;
      for (final Spatial s : content) {
        if (!(s instanceof UIComponent comp)) {
          continue;
        }
        final Rectangle2 rect = comp.getRelativeMinComponentBounds(store);
        if (_horizontal) {
          minW += rect.getWidth();
          if (minH < rect.getHeight()) {
            minH = rect.getHeight();
          }
        } else {
          if (minW < rect.getWidth()) {
            minW = rect.getWidth();
          }
          minH += rect.getHeight();
        }
        spaces++;
      }

      if (spaces > 0) {
        if (_horizontal) {
          minW += _spacing * spaces;
        } else {
          minH += _spacing * spaces;
        }
      }
    }
    container.setLayoutMinimumContentSize(minW, minH);
  }

  private int getSumOfAllHeights(final List<Spatial> content) {
    int sum = 0;
    if (content != null) {
      final Rectangle2 store = new Rectangle2();
      for (final Spatial spat : content) {
        if (spat instanceof UIComponent) {
          final Rectangle2 rect = ((UIComponent) spat).getRelativeMinComponentBounds(store);
          sum += rect.getHeight();
        }
      }
    }
    return sum;
  }

  private int getSumOfAllWidths(final List<Spatial> content) {
    int sum = 0;
    if (content != null) {
      final Rectangle2 store = new Rectangle2();
      for (final Spatial spat : content) {
        if (spat instanceof UIComponent) {
          final Rectangle2 rect = ((UIComponent) spat).getRelativeMinComponentBounds(store);
          sum += rect.getWidth();
        }
      }
    }
    return sum;
  }

  public int getSpacing() { return _spacing; }

  public void setSpacing(final int spacing) { _spacing = spacing; }
}
