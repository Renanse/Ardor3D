/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.pipeline;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.SceneHints;

public class ScenegraphTree {

    public void show(final Node node) {
        final JFrame frame = new JFrame("Scenegraph Tree");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final TreeModel model = new ArdorModel(node);
        final JTree tree = new JTree(model);

        final JScrollPane scrollPane = new JScrollPane(tree);

        final JTextArea textArea = new JTextArea(20, 40);

        final TreeSelectionListener listener = new ArdorListener(textArea);
        tree.addTreeSelectionListener(listener);

        final JSplitPane splitPane = new JSplitPane();
        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(textArea);

        frame.getContentPane().add(splitPane);

        frame.pack();
        frame.setVisible(true);
    }

    private static class ArdorListener implements TreeSelectionListener {
        JTextArea textArea;

        public ArdorListener(final JTextArea textArea) {
            this.textArea = textArea;
        }

        public void valueChanged(final TreeSelectionEvent e) {
            if (e.getNewLeadSelectionPath() == null || e.getNewLeadSelectionPath().getLastPathComponent() == null) {
                return;
            }

            final Spatial spatial = (Spatial) e.getNewLeadSelectionPath().getLastPathComponent();

            final StringBuilder str = new StringBuilder();

            str.append(spatial.getName());
            str.append(" - ");
            str.append(spatial.getClass().getName()).append("\n");

            if (spatial instanceof Mesh) {
                final Mesh mesh = (Mesh) spatial;
                str.append("Primitives: ");
                str.append(mesh.getMeshData().getTotalPrimitiveCount()).append("\n");
                str.append("Index mode: ");
                str.append(mesh.getMeshData().getIndexMode(0)).append("\n");
            }

            str.append(spatial.getTransform()).append("\n");
            if (spatial.getWorldBound() != null) {
                str.append(spatial.getWorldBound()).append("\n");
            }

            str.append("\n");
            final SceneHints sceneHints = spatial.getSceneHints();
            str.append("Cull hint: ");
            str.append(sceneHints.getCullHint()).append("\n");
            str.append("Bucket: ");
            str.append(sceneHints.getRenderBucketType()).append("\n");

            textArea.setText(str.toString());
        }
    }

    private static class ArdorModel implements TreeModel {
        private final Node rootNode;

        public ArdorModel(final Node node) {
            rootNode = node;
        }

        public Object getChild(final Object parent, final int index) {
            if (parent instanceof Node) {
                final Node parentNode = (Node) parent;
                return parentNode.getChild(index);
            }
            return null;
        }

        public int getChildCount(final Object parent) {
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
            return rootNode;
        }

        public boolean isLeaf(final Object node) {
            return !(node instanceof Node);
        }

        public void addTreeModelListener(final TreeModelListener l) {}

        public void removeTreeModelListener(final TreeModelListener l) {}

        public void valueForPathChanged(final TreePath path, final Object newValue) {}
    }
}
