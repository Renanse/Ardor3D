/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.tool.editor.swing.widget;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.ardor3d.extension.ui.UIFrame;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;

public class SceneTreeModel implements TreeModel {

  private final Node _rootNode;

  public SceneTreeModel(final Node root) {
    _rootNode = root;
  }

  @Override
  public Object getChild(final Object parent, final int index) {
    if (parent instanceof UIFrame) {
      return index == 0 ? ((UIFrame) parent).getContentPanel() : null;
    }

    if (parent instanceof Node parentNode) {
      return parentNode.getChild(index);
    }

    return null;
  }

  @Override
  public int getChildCount(final Object parent) {
    if (parent instanceof UIFrame) {
      return 1;
    }

    if (parent instanceof Node parentNode) {
      return parentNode.getNumberOfChildren();
    }
    return 0;
  }

  @Override
  public int getIndexOfChild(final Object parent, final Object child) {
    if (parent instanceof Node parentNode && child instanceof Spatial) {
      return parentNode.getChildIndex((Spatial) child);
    }
    return 0;
  }

  @Override
  public Object getRoot() { return _rootNode; }

  @Override
  public boolean isLeaf(final Object node) {
    return !(node instanceof Node);
  }

  @Override
  public void addTreeModelListener(final TreeModelListener l) {}

  @Override
  public void removeTreeModelListener(final TreeModelListener l) {}

  @Override
  public void valueForPathChanged(final TreePath path, final Object newValue) {}
}
