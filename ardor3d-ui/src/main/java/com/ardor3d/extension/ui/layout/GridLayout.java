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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.extension.ui.UIComponent;
import com.ardor3d.extension.ui.UIContainer;
import com.ardor3d.extension.ui.util.Alignment;
import com.ardor3d.scenegraph.Spatial;

/**
 * A UI Layout that puts content in rows and columns where the row and column cells are set to the
 * minimal size of its content plus some inter-cell spacing. The components should be added from top
 * to down and left to right. Set the layout data of the last component in a row to wrap, e.g. by
 * setLayoutData(GridLayoutData.Wrap); You can specify a horizontal span bigger than one to specify
 * that a component should use multiple cells in the current row.
 *
 * XXX: Note that this class does not currently support layout of rotated components.
 */
public class GridLayout extends UILayout {

  private LayoutGrid grid;
  private final int interCellSpacingHorizontal;
  private final int interCellSpacingVertical;
  private final int leftMargin;
  private final int rightMargin;
  private final int topMargin;
  private final int bottomMargin;
  private final Alignment verticalAlignment;
  private final boolean fillVerticalSpace;
  private final Logger logger = Logger.getLogger(GridLayout.class.getCanonicalName());

  /**
   * Create a GridLayout with the following defaults: 15 pixels horizontal cell spacing, 5 vertical
   * cell spacing, 10 pixels left, top and right margin, 0 bottom margin, vertical alignment is top
   * and the vertical space won't be distributed between rows
   */
  public GridLayout() {
    this(15, 5, 10, 10, 10, 0, Alignment.TOP, false);
  }

  /**
   * Create a GridLayout with the specified parameters and a vertical alignment to top and no
   * distribution of vertical space.
   *
   * @param interCellSpacingHorizontal
   * @param interCellSpacingVertical
   * @param leftMargin
   * @param topMargin
   * @param rightMargin
   * @param bottomMargin
   */
  public GridLayout(final int interCellSpacingHorizontal, final int interCellSpacingVertical, final int leftMargin,
    final int topMargin, final int rightMargin, final int bottomMargin) {
    this(interCellSpacingHorizontal, interCellSpacingVertical, leftMargin, topMargin, rightMargin, bottomMargin,
        Alignment.TOP, false);
  }

  /**
   * Create a Gridlayout with the specified parameters. If vertical space is distributed the vertical
   * alignment does not matter.
   *
   * @param interCellSpacingHorizontal
   * @param interCellSpacingVertical
   * @param leftMargin
   * @param topMargin
   * @param rightMargin
   * @param bottomMargin
   * @param fillVerticalSpace
   */
  public GridLayout(final int interCellSpacingHorizontal, final int interCellSpacingVertical, final int leftMargin,
    final int topMargin, final int rightMargin, final int bottomMargin, final boolean fillVerticalSpace) {
    this(interCellSpacingHorizontal, interCellSpacingVertical, leftMargin, topMargin, rightMargin, bottomMargin,
        Alignment.TOP, fillVerticalSpace);
  }

  /**
   * Create a GridLayout with the specified parameters. Any additional vertical space won't be
   * distributed between rows.
   *
   * @param interCellSpacingHorizontal
   * @param interCellSpacingVertical
   * @param leftMargin
   * @param topMargin
   * @param rightMargin
   * @param bottomMargin
   * @param verticalAlignment
   *          only TOP, MIDDLE and BOTTOM are meaningful
   */
  public GridLayout(final int interCellSpacingHorizontal, final int interCellSpacingVertical, final int leftMargin,
    final int topMargin, final int rightMargin, final int bottomMargin, final Alignment verticalAlignment) {
    this(interCellSpacingHorizontal, interCellSpacingVertical, leftMargin, topMargin, rightMargin, bottomMargin,
        verticalAlignment, false);
  }

  /**
   * Create a GridLayout with the specified parameters. Note that the vertical alignment does not
   * matter if you choose to distribute any additional space between rows.
   *
   * @param interCellSpacingHorizontal
   * @param interCellSpacingVertical
   * @param leftMargin
   * @param topMargin
   * @param rightMargin
   * @param bottomMargin
   * @param verticalAlignment
   *          only TOP, MIDDLE and BOTTOM are meaningful
   * @param fillVerticalSpace
   */
  public GridLayout(final int interCellSpacingHorizontal, final int interCellSpacingVertical, final int leftMargin,
    final int topMargin, final int rightMargin, final int bottomMargin, final Alignment verticalAlignment,
    final boolean fillVerticalSpace) {
    this.interCellSpacingHorizontal = interCellSpacingHorizontal;
    this.interCellSpacingVertical = interCellSpacingVertical;
    this.leftMargin = leftMargin;
    this.topMargin = topMargin;
    this.rightMargin = rightMargin;
    this.bottomMargin = bottomMargin;
    this.verticalAlignment = verticalAlignment;
    this.fillVerticalSpace = fillVerticalSpace;
  }

  @Override
  public void layoutContents(final UIContainer container) {
    rebuildGrid(container);
    grid.updateMinimalSize();
    final int height = grid.minHeight;
    final int heightDiff = container.getContentHeight() > height ? container.getContentHeight() - height : 0;
    int rowHeightDiff = heightDiff;
    switch (verticalAlignment) {
      case TOP:
        rowHeightDiff = heightDiff;
        break;
      case MIDDLE:
        rowHeightDiff = heightDiff / 2;
        break;
      case BOTTOM:
        rowHeightDiff = 0;
        break;
      default:
        rowHeightDiff = heightDiff;
    }
    for (final LayoutComponent lc : grid.components) {
      if (fillVerticalSpace) {
        rowHeightDiff = Math.round(heightDiff * (1f - (float) lc.firstRow / grid.maxRow));
      }
      lc.component.setLocalXY(grid.columnOffsets[lc.firstColumn],
          rowHeightDiff + height - grid.rowOffsets[lc.firstRow] - lc.getComponentHeight());
      if (lc.grow) {
        lc.component.setLocalComponentWidth(grid.getCellsWidth(lc.firstColumn, lc.lastColumn));
      }
    }
  }

  @Override
  public void updateMinimumSizeFromContents(final UIContainer container) {
    rebuildGrid(container);
    grid.updateMinimalSize();
    container.setLayoutMinimumContentSize(grid.minWidth, grid.minHeight);
  }

  private void rebuildGrid(final UIContainer container) {
    final List<Spatial> content = container.getChildren();
    grid = new LayoutGrid();
    for (final Spatial spatial : content) {
      if (spatial instanceof UIComponent c) {
        grid.add(c);
      }
    }
  }

  class LayoutGrid {
    int currentRow = 0;
    int currentColumn = 0;
    int nextColumn = 0;
    int nextRow = 0;
    int maxColumn;
    int maxRow;
    int minWidth;
    int minHeight;
    int[] columnOffsets;
    int[] rowOffsets;
    LinkedList<LayoutComponent> components;
    ArrayList<Integer> columnWidths;

    LayoutGrid() {
      components = new LinkedList<>();
      columnWidths = new ArrayList<>();
    }

    void add(final UIComponent c) {
      final UILayoutData data = c.getLayoutData();
      final LayoutComponent lc = new LayoutComponent(c);
      lc.firstColumn = currentColumn;
      lc.firstRow = currentRow;
      lc.lastColumn = currentColumn;
      lc.lastRow = currentRow;
      if (data instanceof GridLayoutData gld) {
        if (gld.getSpan() > 1) {
          if (!gld.isWrap()) {
            nextColumn += gld.getSpan();
          } else {
            nextColumn = 0;
            nextRow = currentRow + 1;
          }
          lc.lastColumn = lc.firstColumn + gld.getSpan() - 1;
          maxColumn = Math.max(maxColumn, lc.lastColumn);
        } else {
          if (gld.isWrap()) {
            nextColumn = 0;
            nextRow = currentRow + 1;
          } else {
            nextColumn = currentColumn + 1;
          }
        }
        lc.grow = gld.isGrow();
      } else {
        nextColumn = currentColumn + 1;
      }
      components.add(lc);
      if (logger.isLoggable(Level.FINE)) {
        logger.fine(lc.toString() + " max.col=" + maxColumn);
      }
      maxColumn = Math.max(maxColumn, currentColumn);
      maxRow = Math.max(maxRow, currentRow);
      currentColumn = nextColumn;
      currentRow = nextRow;
    }

    void updateMinimalSize() {
      columnOffsets = new int[maxColumn + 2];
      rowOffsets = new int[maxRow + 2];
      columnOffsets[0] = leftMargin;
      rowOffsets[0] = topMargin;
      for (final LayoutComponent lc : components) {
        columnOffsets[lc.lastColumn + 1] = Math.max(columnOffsets[lc.lastColumn + 1],
            lc.getComponentWidth() + interCellSpacingHorizontal + columnOffsets[lc.firstColumn]);
        rowOffsets[lc.firstRow + 1] = Math.max(rowOffsets[lc.firstRow + 1],
            lc.getComponentHeight() + interCellSpacingVertical + rowOffsets[lc.firstRow]);
      }

      if (logger.isLoggable(Level.FINE)) {
        logger.fine("column offsets: " + Arrays.toString(columnOffsets));
        logger.fine("row offsets: " + Arrays.toString(rowOffsets));
      }
      minWidth = columnOffsets[maxColumn + 1] - interCellSpacingHorizontal + rightMargin;
      minHeight = rowOffsets[maxRow + 1] - interCellSpacingVertical + bottomMargin;
    }

    int getCellsWidth(final int firstColumn, final int lastColumn) {
      int width = columnOffsets[lastColumn + 1] - columnOffsets[firstColumn] - interCellSpacingHorizontal;
      if (lastColumn >= maxColumn) {
        width -= rightMargin;
      }
      return width;
    }
  }

  class LayoutComponent {
    UIComponent component;
    int firstRow;
    int firstColumn;
    int lastRow;
    int lastColumn;
    boolean grow;

    LayoutComponent(final UIComponent c) {
      component = c;
    }

    public int getComponentWidth() {
      return Math.max(component.getLocalComponentWidth(), component.getMinimumLocalComponentWidth());
    }

    public int getComponentHeight() {
      return Math.max(component.getLocalComponentHeight(), component.getMinimumLocalComponentHeight());
    }

    @Override
    public String toString() {
      return component + " " + firstColumn + "-" + lastColumn + "/" + firstRow + "-" + lastRow;
    }
  }
}
