/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.terrain.compound;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.ardor3d.example.terrain.compound.CompoundTerrainExample.ExampleEntry;
import com.ardor3d.extension.terrain.providers.compound.CompoundTerrainSource;
import com.ardor3d.extension.terrain.providers.procedural.ProceduralTerrainSource;

public class CompoundTerrainSourcePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    final private CompoundTerrainSource _source;
    final private JSplitPane _splitPane;

    private JTree _layerTree;
    private LayerTreeModel _layerTreeModel;

    private JPanel _bottomPanel;

    public CompoundTerrainSourcePanel(final CompoundTerrainSource terrainSource) {

        _source = terrainSource;
        _splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        setupLayerTree();
        setupPropsPanel();
        setLayout(new BorderLayout());
        add(_splitPane, BorderLayout.CENTER);
        add(new JLabel("TerrainSource Tree:"), BorderLayout.NORTH);
        _splitPane.setDividerLocation(100);

        updatePropsPanel();
    }

    @SuppressWarnings("serial")
    private void setupLayerTree() {
        _layerTreeModel = new LayerTreeModel(_source);
        _layerTree = new JTree(_layerTreeModel) {
            @Override
            public String convertValueToText(final Object value, final boolean selected, final boolean expanded,
                    final boolean leaf, final int row, final boolean hasFocus) {
                if (value instanceof CompoundTerrainSource) {
                    return "Compound Terrain Source";
                }
                if (value instanceof ExampleEntry) {
                    return ((ExampleEntry) value).getName();
                }
                return "?";
            }
        };

        _layerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        final DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
        cellRenderer.setLeafIcon(null);
        _layerTree.setCellRenderer(cellRenderer);

        _layerTree.getSelectionModel().addTreeSelectionListener(e -> updatePropsPanel());

        final JScrollPane sp = new JScrollPane(_layerTree);
        _splitPane.setTopComponent(sp);
    }

    private void setupPropsPanel() {
        _bottomPanel = new JPanel();
        _splitPane.setBottomComponent(_bottomPanel);
    }

    protected void updatePropsPanel() {
        ExampleEntry selectedEntry = null;
        // just take the first selected node
        final TreePath tp = _layerTree.getSelectionPath();
        if (tp != null) {
            final Object selected = tp.getLastPathComponent();
            if (selected instanceof ExampleEntry) {
                selectedEntry = (ExampleEntry) selected;
                if (selectedEntry.getSource() instanceof ProceduralTerrainSource) {
                    final ProceduralTerrainSource src = (ProceduralTerrainSource) selectedEntry.getSource();
                    if (src.getFunction() instanceof UIEditableFunction) {
                        ((UIEditableFunction) src.getFunction()).setupFunctionEditPanel(_bottomPanel, src);
                        return;
                    }
                }
            }
        }
        _bottomPanel.removeAll();
    }

    class LayerTreeModel implements TreeModel {

        private final CompoundTerrainSource _source;

        public LayerTreeModel(final CompoundTerrainSource source) {
            _source = source;
        }

        public Object getChild(final Object parent, final int index) {
            if (parent instanceof CompoundTerrainSource) {
                final CompoundTerrainSource parentSource = (CompoundTerrainSource) parent;
                return parentSource.getEntry(index);
            }

            return null;
        }

        public int getChildCount(final Object parent) {
            if (parent instanceof CompoundTerrainSource) {
                final CompoundTerrainSource parentSource = (CompoundTerrainSource) parent;
                return parentSource.getEntries().size();
            }
            return 0;
        }

        public int getIndexOfChild(final Object parent, final Object child) {
            if (parent instanceof CompoundTerrainSource) {
                final CompoundTerrainSource parentSource = (CompoundTerrainSource) parent;
                return parentSource.getEntries().indexOf(child);
            }
            return 0;
        }

        public Object getRoot() {
            return _source;
        }

        public boolean isLeaf(final Object node) {
            return !(node instanceof CompoundTerrainSource);
        }

        public void addTreeModelListener(final TreeModelListener l) {
            // Left empty
        }

        public void removeTreeModelListener(final TreeModelListener l) {
            // Left empty
        }

        public void valueForPathChanged(final TreePath path, final Object newValue) {
            // Left empty
        }
    }

}
