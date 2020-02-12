/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.tool.editor.particle.swing.panel;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.ardor3d.extension.effect.particle.FloorInfluence;
import com.ardor3d.extension.effect.particle.ParticleInfluence;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.extension.effect.particle.SimpleParticleInfluenceFactory;
import com.ardor3d.extension.effect.particle.SwarmInfluence;
import com.ardor3d.extension.effect.particle.WanderInfluence;
import com.ardor3d.math.Line3;
import com.ardor3d.math.Plane;
import com.ardor3d.math.Vector3;

public class ParticleInfluencePanel extends ParticleEditPanel {

    private static final long serialVersionUID = 1L;

    private final InfluenceListModel _influenceModel = new InfluenceListModel();
    private final JList<String> _influenceList = new JList<String>(_influenceModel);
    private final JButton _deleteInfluenceButton;
    private final JPanel _influenceParamsPanel;

    public ParticleInfluencePanel() {
        super();
        setLayout(new GridBagLayout());

        _influenceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _influenceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                final int idx = _influenceList.getSelectedIndex();
                _deleteInfluenceButton.setEnabled(idx != -1);
                updateInfluenceParams();
            }
        });

        final JButton newInfluenceButton = new JButton(new AbstractAction("Add Influence") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                final ParticleInfluence infl = getNewInfluence();
                if (infl != null) {
                    getEdittedParticles().addInfluence(infl);
                    final int idx = getEdittedParticles().getInfluences().size() - 1;
                    _influenceModel.fireIntervalAdded(idx, idx);
                    _influenceList.setSelectedIndex(idx);
                }
            }

            private ParticleInfluence getNewInfluence() {
                final Object chosen = JOptionPane.showInputDialog(ParticleInfluencePanel.this,
                        "Choose the influence type to add:", "Add Influence", JOptionPane.OK_CANCEL_OPTION, null,
                        new String[] { "wind", "gravity", "drag", "vortex", "swarm", "wander", "floor" }, null);

                ParticleInfluence infl = null;
                if ("wind".equals(chosen)) {
                    infl = SimpleParticleInfluenceFactory.createBasicWind(1f, Vector3.UNIT_X, true, true);
                } else if ("gravity".equals(chosen)) {
                    infl = SimpleParticleInfluenceFactory.createBasicGravity(Vector3.ZERO, true);
                } else if ("drag".equals(chosen)) {
                    infl = SimpleParticleInfluenceFactory.createBasicDrag(1f);
                } else if ("vortex".equals(chosen)) {
                    infl = SimpleParticleInfluenceFactory.createBasicVortex(1f, 0f, new Line3(new Vector3(),
                            Vector3.UNIT_Y), true, true);
                } else if ("swarm".equals(chosen)) {
                    infl = new SwarmInfluence(new Vector3(), 3);
                } else if ("wander".equals(chosen)) {
                    infl = new WanderInfluence();
                } else if ("floor".equals(chosen)) {
                    infl = new FloorInfluence(new Plane(Vector3.UNIT_Y, 0), 0.3f);
                }
                return infl;
            }
        });
        newInfluenceButton.setMargin(new Insets(2, 2, 2, 2));

        _deleteInfluenceButton = new JButton(new AbstractAction("Delete") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                final int idx = _influenceList.getSelectedIndex();
                getEdittedParticles().getInfluences().remove(idx);
                _influenceModel.fireIntervalRemoved(idx, idx);
                _influenceList.setSelectedIndex(idx >= getEdittedParticles().getInfluences().size() ? idx - 1 : idx);
            }
        });
        _deleteInfluenceButton.setMargin(new Insets(2, 2, 2, 2));
        _deleteInfluenceButton.setEnabled(false);

        final JPanel influenceListPanel = new JPanel(new GridBagLayout());
        influenceListPanel.setBorder(createTitledBorder("PARTICLE INFLUENCES"));
        influenceListPanel.add(_influenceList, new GridBagConstraints(0, 0, 1, 3, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.BOTH, new Insets(5, 10, 10, 5), 0, 0));
        influenceListPanel.add(newInfluenceButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        influenceListPanel.add(_deleteInfluenceButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 10, 5), 0, 0));

        _influenceParamsPanel = new JPanel(new BorderLayout());

        add(influenceListPanel, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 10, 5), 0, 0));
        add(_influenceParamsPanel, new GridBagConstraints(0, 1, 1, 1, 0.5, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 10, 5), 0, 0));
    }

    @Override
    public void updateWidgets() {
        _influenceList.clearSelection();
        final int fcount = (getEdittedParticles().getInfluences() == null) ? 0 : getEdittedParticles().getInfluences()
                .size();
        _influenceModel.fireContentsChanged(0, fcount - 1);
    }

    /**
     * updateInfluenceParams
     */
    private void updateInfluenceParams() {
        _influenceParamsPanel.removeAll();
        final int idx = _influenceList.getSelectedIndex();
        if (idx == -1) {
            _influenceParamsPanel.validate();
            return;
        }
        final ParticleInfluence influence = getEdittedParticles().getInfluences().get(idx);
        if (influence instanceof SimpleParticleInfluenceFactory.BasicWind) {
            final WindInfluencePanel windParamsPanel = new WindInfluencePanel();
            windParamsPanel.setEdittedInfluence(influence);
            windParamsPanel.updateWidgets();
            _influenceParamsPanel.add(windParamsPanel);

        } else if (influence instanceof SimpleParticleInfluenceFactory.BasicGravity) {
            final GravityInfluencePanel gravityParamsPanel = new GravityInfluencePanel();
            gravityParamsPanel.setEdittedInfluence(influence);
            gravityParamsPanel.updateWidgets();
            _influenceParamsPanel.add(gravityParamsPanel);

        } else if (influence instanceof SimpleParticleInfluenceFactory.BasicDrag) {
            final DragInfluencePanel dragParamsPanel = new DragInfluencePanel();
            dragParamsPanel.setEdittedInfluence(influence);
            dragParamsPanel.updateWidgets();
            _influenceParamsPanel.add(dragParamsPanel);

        } else if (influence instanceof SimpleParticleInfluenceFactory.BasicVortex) {
            final VortexInfluencePanel vortexParamsPanel = new VortexInfluencePanel();
            vortexParamsPanel.setEdittedInfluence(influence);
            vortexParamsPanel.updateWidgets();
            _influenceParamsPanel.add(vortexParamsPanel);

        } else if (influence instanceof SwarmInfluence) {
            final SwarmInfluencePanel swarmInfluencePanel = new SwarmInfluencePanel();
            swarmInfluencePanel.setEdittedInfluence(influence);
            swarmInfluencePanel.updateWidgets();
            _influenceParamsPanel.add(swarmInfluencePanel);

        } else if (influence instanceof WanderInfluence) {
            final WanderInfluencePanel influencePanel = new WanderInfluencePanel();
            influencePanel.setEdittedInfluence(influence);
            influencePanel.updateWidgets();
            _influenceParamsPanel.add(influencePanel);

        } else if (influence instanceof FloorInfluence) {
            final FloorInfluencePanel floorInfluencePanel = new FloorInfluencePanel();
            floorInfluencePanel.setEdittedInfluence(influence);
            floorInfluencePanel.updateWidgets();
            _influenceParamsPanel.add(floorInfluencePanel);
        }
        _influenceParamsPanel.getParent().validate();
        _influenceParamsPanel.getParent().repaint();
    }

    class InfluenceListModel extends AbstractListModel<String> {

        private static final long serialVersionUID = 1L;

        public int getSize() {
            final ParticleSystem particles = getEdittedParticles();
            return (particles == null || particles.getInfluences() == null) ? 0 : particles.getInfluences().size();
        }

        public String getElementAt(final int index) {
            final ParticleInfluence pf = getEdittedParticles().getInfluences().get(index);
            if (pf instanceof SimpleParticleInfluenceFactory.BasicWind) {
                return "Wind";
            } else if (pf instanceof SimpleParticleInfluenceFactory.BasicGravity) {
                return "Gravity";
            } else if (pf instanceof SimpleParticleInfluenceFactory.BasicDrag) {
                return "Drag";
            } else if (pf instanceof SimpleParticleInfluenceFactory.BasicVortex) {
                return "Vortex";
            } else if (pf instanceof SwarmInfluence) {
                return "Swarm";
            } else if (pf instanceof WanderInfluence) {
                return "Wander";
            } else if (pf instanceof FloorInfluence) {
                return "Floor";
            } else {
                return "???";
            }
        }

        public void fireContentsChanged(final int idx0, final int idx1) {
            super.fireContentsChanged(this, idx0, idx1);
        }

        public void fireIntervalAdded(final int idx0, final int idx1) {
            super.fireIntervalAdded(this, idx0, idx1);
        }

        public void fireIntervalRemoved(final int idx0, final int idx1) {
            super.fireIntervalRemoved(this, idx0, idx1);
        }
    }

}
