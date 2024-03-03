/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.model;

/**
 * An interface describing a data store or model for a UIComboBox. The model should maintain a list
 * of values for a combo box, as well as an optional tool tip and view String override.
 */
public interface ComboBoxModel {

  /**
   * Add a new item to the end of our model.
   * 
   * @param value
   *          the value to add. value.toString() is used for the view of this value by default.
   * @return the index we were added at
   * @see #setViewAt(int, String)
   */
  int addItem(Object value);

  /**
   * Add a new item to at the specified index in our model. Should pad with null entries if index >
   * current max model index.
   * 
   * @param index
   *          the index to add at
   * @param value
   *          the value to add
   */
  void addItem(int index, Object value);

  /**
   * Get the item value at the specified index.
   * 
   * @param index
   *          the index to retrieve
   * @return the value of the item at the given index.
   */
  Object getValueAt(int index);

  /**
   * Sets the value of the item at the given index. If that item already has a name or tooltip set, it
   * is left unchanged.
   * 
   * @param index
   *          the index to add at.
   * @param value
   *          the value to set
   */
  void setValueAt(int index, Object value);

  /**
   * @param index
   *          the index to retrieve
   * @return the view String for the given index. If no specific view is set, the value at that
   *         location is converted with toString().
   */
  String getViewAt(int index);

  /**
   * Sets the view String for the given index.
   * 
   * @param index
   *          the index to add at.
   * @param view
   *          the view String to set
   */
  void setViewAt(int index, String view);

  /**
   * @param index
   *          the index to retrieve
   * @return the tool tip for the given index.
   */
  String getToolTipAt(int index);

  /**
   * Sets the tool tip for the given index.
   * 
   * @param index
   *          the index to add at.
   * @param toolTip
   *          the tool tip to set
   */
  void setToolTipAt(int index, String toolTip);

  /**
   * @return the number of items, including null items, in our model.
   */
  int size();

  /**
   * Removes all items from this model.
   */
  void clear();

}
