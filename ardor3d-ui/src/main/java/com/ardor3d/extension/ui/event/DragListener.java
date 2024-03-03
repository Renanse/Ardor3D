/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.ui.event;

import com.ardor3d.extension.ui.UIComponent;

/**
 * Classes interested in processing drag events should implement this interface.
 */
public interface DragListener {

  /**
   * Ask if the given component and coordinates indicate a drag handle for the implementor. For
   * example, a scroll bar setting up a listener might only allow dragging if the component acted on
   * is its button.
   * 
   * @param component
   *          the UIComponent being acted upon in a suspected drag operation
   * @param mouseX
   *          the x mouse coordinate
   * @param mouseY
   *          the y mouse coordinate
   * @return true if the given parameters describe a drag handle
   */
  boolean isDragHandle(UIComponent component, int mouseX, int mouseY);

  /**
   * Let the implementor know that we've accepted this as our current drag target. This is called by
   * the hud if a drag action is detected and isDragHandle has returned true.
   * 
   * @param mouseX
   *          the x mouse coordinate
   * @param mouseY
   *          the y mouse coordinate
   */
  void startDrag(int mouseX, int mouseY);

  /**
   * Method called when the button is still held after startDrag and the mouse has moved again.
   * 
   * @param mouseX
   *          the new x mouse coordinate
   * @param mouseY
   *          the new y mouse coordinate
   */
  void drag(int mouseX, int mouseY);

  /**
   * End our drag. This is called when the button is released after initDrag.
   * 
   * @param component
   *          the UIComponent our drag ended over.
   * @param mouseX
   *          the x mouse coordinate
   * @param mouseY
   *          the y mouse coordinate
   */
  void endDrag(UIComponent component, int mouseX, int mouseY);

}
