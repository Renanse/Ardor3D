/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.tool.editor.particle.swing.panel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ardor3d.scenegraph.controller.ComplexSpatialController.RepeatType;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;

public class ParticleFlowPanel extends ParticleEditPanel {
    private static final long serialVersionUID = 1L;

    // flow panel components
    private final JCheckBox _rateBox;
    private final ValuePanel _releaseRatePanel = new ValuePanel("Particles per second: ", "", 0, Integer.MAX_VALUE, 1);
    private final ValuePanel _rateVarPanel = new ValuePanel("Variance: ", "%", 0.0, 1.0, 0.001);
    private final ValuePanel _minAgePanel = new ValuePanel("Minimum Age: ", "ms", 0.0, Double.MAX_VALUE, 10.0);
    private final ValuePanel _maxAgePanel = new ValuePanel("Maximum Age: ", "ms", 0.0, Double.MAX_VALUE, 10.0);
    private final JCheckBox _spawnBox;

    public ParticleFlowPanel() {
        setLayout(new GridBagLayout());

        _rateBox = new JCheckBox(new AbstractAction("Regulate Flow") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                getEdittedParticles().getParticleController().setControlFlow(_rateBox.isSelected());
                updateRateLabels();
            }
        });

        _releaseRatePanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setReleaseRate(_releaseRatePanel.getIntValue());
            }
        });

        _rateVarPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setReleaseVariance(_rateVarPanel.getDoubleValue());
            }
        });

        _minAgePanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setMinimumLifeTime(_minAgePanel.getDoubleValue());
            }
        });
        _maxAgePanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setMaximumLifeTime(_maxAgePanel.getDoubleValue());
            }
        });
        final JPanel agePanel = new JPanel(new GridBagLayout());
        agePanel.setBorder(createTitledBorder("PARTICLE AGE"));
        agePanel.add(_minAgePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0));
        agePanel.add(_maxAgePanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0));

        final JPanel ratePanel = new JPanel(new GridBagLayout());
        ratePanel.setBorder(createTitledBorder("RATE"));
        ratePanel.add(_rateBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(10, 5, 5, 5), 0, 0));
        ratePanel.add(_releaseRatePanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
        ratePanel.add(_rateVarPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));

        _spawnBox = new JCheckBox(new AbstractAction("Respawn 'dead' particles.") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                if (_spawnBox.isSelected()) {
                    getEdittedParticles().getParticleController().setRepeatType(RepeatType.WRAP);
                } else {
                    getEdittedParticles().getParticleController().setRepeatType(RepeatType.CLAMP);
                }
            }
        });
        _spawnBox.setSelected(true);

        final JButton spawnButton = new JButton(new AbstractAction("Force Respawn") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                getEdittedParticles().forceRespawn();
            }
        });

        final JPanel spawnPanel = new JPanel(new GridBagLayout());
        spawnPanel.setBorder(createTitledBorder("SPAWN"));
        spawnPanel.add(_spawnBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(10, 10, 5, 10), 0, 0));
        spawnPanel.add(spawnButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(5, 10, 10, 10), 0, 0));

        add(ratePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 10), 0, 0));
        add(spawnPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0));
        add(agePanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 5), 0, 0));
    }

    @Override
    public void updateWidgets() {
        _rateBox.setSelected(getEdittedParticles().getParticleController().isControlFlow());
        _releaseRatePanel.setValue(getEdittedParticles().getReleaseRate());
        _rateVarPanel.setValue(getEdittedParticles().getReleaseVariance());
        updateRateLabels();
        _spawnBox.setSelected(getEdittedParticles().getParticleController().getRepeatType() == RepeatType.WRAP);
        _minAgePanel.setValue(getEdittedParticles().getMinimumLifeTime());
        _maxAgePanel.setValue(getEdittedParticles().getMaximumLifeTime());
    }

    /**
     * updateRateLabels
     */
    private void updateRateLabels() {
        _releaseRatePanel.setEnabled(_rateBox.isSelected());
        _rateVarPanel.setEnabled(_rateBox.isSelected());
    }
}
