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

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.extension.ui.util.Insets;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.scenegraph.Spatial;

/**
 * <p>
 * Layout applied to a parent container used to position child components as sub-rectangles pinned
 * to the content area of this component. Child components should have a {@link RectLayoutData} set.
 * </p>
 *
 * <p>
 * Components are laid out by pinning the bottom left and top right corners of the component to two
 * locations in the parent container's content area, forming a sub-rectangle. Insets are then
 * applied to this rectangle, shifting the 4 edges further in or out. With this behavior, it is
 * possible to create both statically sized and dynamically sized child components.
 * </p>
 *
 * <p>
 * Example: Set up a 100x30 button and pin it to the center of its parent, regardless of parent
 * size.
 *
 * <pre>
 * UIPanel panel = new UIPanel(new RectLayout());
 * UIButton okButton = new UIButton(&quot;OK!&quot;);
 * panel.add(okButton);
 *
 * // THIS...
 * okButton.setLayoutData(new RectLayoutData(.5, .5, .5, .5, -15, -50, -15, -50));
 * // OR...
 * okButton.setLayoutData(RectLayoutData.pinCenter(100, 30, 0, 0));
 * </pre>
 *
 * </p>
 *
 * <p>
 * Example: Pin the above button to center of its parent, but set it to a third of the panel wide
 * and 30 pixels tall.
 *
 * <pre>
 * okButton.setLayoutData(new RectLayoutData(.33, .5, .67, .5, -15, 0, -15, 0));
 * </pre>
 *
 * </p>
 *
 */
public class RectLayout extends UILayout {

  @Override
  public void layoutContents(final UIContainer container) {
    final int cWidth = container.getContentWidth();
    final int cHeight = container.getContentHeight();

    // go through all children of a container
    for (int i = container.getNumberOfChildren(); --i >= 0;) {

      // make sure child is UIComponent and has a RectLayoutData set
      final Spatial child = container.getChild(i);
      if (!(child instanceof UIComponent)) {
        continue;
      }
      final UIComponent childComp = (UIComponent) child;
      if (!(childComp.getLayoutData() instanceof RectLayoutData)) {
        continue;
      }
      final RectLayoutData layData = (RectLayoutData) childComp.getLayoutData();
      final Vector2 rMin = layData.getRelativeMin(), rMax = layData.getRelativeMax();
      final Insets layInsets = layData.getPixelInsets();

      // find min and max anchor points
      final int minAnchorX = (int) MathUtils.round(rMin.getX() * cWidth);
      final int minAnchorY = (int) MathUtils.round(rMin.getY() * cHeight);
      final int maxAnchorX = (int) MathUtils.round(rMax.getX() * cWidth);
      final int maxAnchorY = (int) MathUtils.round(rMax.getY() * cHeight);

      // add our offsets
      final int minX = minAnchorX + layInsets.getLeft();
      final int maxX = maxAnchorX - layInsets.getRight();
      final int minY = minAnchorY + layInsets.getBottom();
      final int maxY = maxAnchorY - layInsets.getTop();

      // resize
      final int compWidth = maxX - minX;
      final int compHeight = maxY - minY;
      childComp.fitComponentIn(compWidth, compHeight);

      // set position
      childComp.setLocalXY(minX, minY);
    }
  }

  /**
   * Do nothing - just use what is given by to the container. We can calculate a min width/height as
   * below in comments, but it is prone to exploding due to recursive dependency (imagine an anchor of
   * -0.2 for example.)
   */
  @Override
  public void updateMinimumSizeFromContents(final UIContainer container) {

  }

  // @Override
  // public void updateMinimumSizeFromContents(final UIContainer container) {
  //
  // int minWidth = 0;
  // int minHeight = 0;
  //
  // // go through all children of a container
  // for (int i = container.getNumberOfChildren(); --i >= 0;) {
  //
  // // make sure child is UIComponent and has a RectLayoutData set
  // final Spatial child = container.getChild(i);
  // if (!(child instanceof UIComponent)) {
  // continue;
  // }
  // final UIComponent childComp = (UIComponent) child;
  // if (!(childComp.getLayoutData() instanceof RectLayoutData)) {
  // continue;
  // }
  // final RectLayoutData layData = (RectLayoutData) childComp.getLayoutData();
  //
  // // calculate container min width/height for this child
  // minWidth = Math.max(minWidth, calculateMinContainerWidth(layData));
  // minHeight = Math.max(minHeight, calculateMinContainerHeight(layData));
  // }
  //
  // // set min dimensions on container
  // container.setLayoutMinimumContentSize(minWidth, minHeight);
  // }
  //
  // protected int calculateMinContainerWidth(final RectLayoutData data) {
  // final Vector2 rMin = data.getRelativeMin(), rMax = data.getRelativeMax();
  // final Insets layInsets = data.getPixelInsets();
  //
  // int leftWidth = layInsets.getLeft();
  // if (leftWidth < 0) {
  // leftWidth = (int) MathUtils.round(Math.abs(leftWidth / rMin.getX()));
  // } else if (leftWidth > 0) {
  // leftWidth = (int) MathUtils.round(leftWidth / (1.0 - rMin.getX()));
  // }
  //
  // int rightWidth = layInsets.getRight();
  // if (rightWidth > 0) {
  // rightWidth = (int) MathUtils.round(rightWidth / rMax.getX());
  // } else if (rightWidth < 0) {
  // rightWidth = (int) MathUtils.round(Math.abs(rightWidth / (1.0 - rMax.getX())));
  // }
  //
  // return Math.max(leftWidth, rightWidth);
  // }
  //
  // protected int calculateMinContainerHeight(final RectLayoutData data) {
  // final Vector2 rMin = data.getRelativeMin(), rMax = data.getRelativeMax();
  // final Insets layInsets = data.getPixelInsets();
  //
  // int bottomWidth = layInsets.getBottom();
  // if (bottomWidth < 0) {
  // bottomWidth = (int) MathUtils.round(Math.abs(bottomWidth / rMin.getY()));
  // } else if (bottomWidth > 0) {
  // bottomWidth = (int) MathUtils.round(bottomWidth / (1.0 - rMin.getY()));
  // }
  //
  // int topWidth = layInsets.getTop();
  // if (topWidth > 0) {
  // topWidth = (int) MathUtils.round(topWidth / rMax.getY());
  // } else if (topWidth < 0) {
  // topWidth = (int) MathUtils.round(Math.abs(topWidth / (1.0 - rMax.getY())));
  // }
  //
  // return Math.max(bottomWidth, topWidth);
  // }

}
