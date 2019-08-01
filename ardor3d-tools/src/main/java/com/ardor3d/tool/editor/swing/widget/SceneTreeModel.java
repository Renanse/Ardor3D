/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
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

    public Object getChild(final Object parent, final int index) {
        if (parent instanceof UIFrame) {
            return index == 0 ? ((UIFrame) parent).getContentPanel() : null;
        }

        if (parent instanceof Node) {
            final Node parentNode = (Node) parent;
            return parentNode.getChild(index);
        }

        return null;
    }

    public int getChildCount(final Object parent) {
        if (parent instanceof UIFrame) {
            return 1;
        }

        if (parent instanceof Node) {
            final Node parentNode = (Node) parent;
            return parentNode.getNumberOfChildren();
        }
        return 0;
    }

    public int getIndexOfChild(final Object parent, final Object child) {
        if (parent instanceof Node && child instanceof Spatial) {
            final Node parentNode = (Node) parent;
            return parentNode.getChildIndex((Spatial) child);
        }
        return 0;
    }

    public Object getRoot() {
        return _rootNode;
    }

    public boolean isLeaf(final Object node) {
        return !(node instanceof Node);
    }

    public void addTreeModelListener(final TreeModelListener l) {}

    public void removeTreeModelListener(final TreeModelListener l) {}

    public void valueForPathChanged(final TreePath path, final Object newValue) {}
}