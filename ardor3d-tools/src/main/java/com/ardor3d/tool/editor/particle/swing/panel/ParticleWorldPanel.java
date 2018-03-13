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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;

public class ParticleWorldPanel extends ParticleEditPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel _countLabel;
    private final ValuePanel _speedPanel = new ValuePanel("Time Modifier: ", "x", 0.0, Double.MAX_VALUE, 0.01);
    private final ValuePanel _precisionPanel = new ValuePanel("Precision: ", "s", 0.0, Double.MAX_VALUE, 0.001);
    private final JComboBox<RenderBucketType> _renderBucketCB = new JComboBox<RenderBucketType>(new RenderBucketType[] {
            RenderBucketType.Inherit, RenderBucketType.Transparent, RenderBucketType.Opaque, RenderBucketType.Ortho });

    public ParticleWorldPanel() {
        super();
        setLayout(new GridBagLayout());

        _countLabel = createBoldLabel("Particles: 300");
        final JButton countButton = new JButton(new AbstractAction("Change...") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                countButton_actionPerformed(e);
            }
        });
        countButton.setFont(new Font("Arial", Font.BOLD, 12));
        countButton.setMargin(new Insets(2, 2, 2, 2));

        final JPanel countPanel = new JPanel(new GridBagLayout());
        countPanel.setBorder(createTitledBorder("PARTICLE COUNT"));
        countPanel.add(_countLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 0, 0));
        countPanel.add(countButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(5, 10, 5, 10), 0, 0));

        _speedPanel.setBorder(createTitledBorder("TIME"));
        _speedPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().getParticleController().setSpeed(_speedPanel.getDoubleValue());
            }
        });

        _precisionPanel.setBorder(createTitledBorder("UPDATE PRECISION"));
        _precisionPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().getParticleController().setPrecision(_precisionPanel.getDoubleValue());
            }
        });

        final JLabel queueLabel = createBoldLabel("Render Bucket:");

        _renderBucketCB.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getEdittedParticles().getSceneHints().setRenderBucketType(
                        (RenderBucketType) _renderBucketCB.getSelectedItem());
            }
        });
        final JPanel queuePanel = new JPanel(new GridBagLayout());
        queuePanel.setBorder(createTitledBorder("RENDER BUCKET"));
        queuePanel.add(queueLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        queuePanel.add(_renderBucketCB, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

        add(countPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 10), 0, 0));
        add(_speedPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 5), 0, 0));
        add(_precisionPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 10, 5, 5), 0, 0));
        add(queuePanel, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 5), 0, 0));
    }

    /**
     * updateCountLabels
     */
    private void updateCountLabels() {
        final int val = getEdittedParticles().getNumParticles();
        _countLabel.setText("Particles: " + val);
    }

    @Override
    public void updateWidgets() {
        updateCountLabels();
        _speedPanel.setValue(getEdittedParticles().getParticleController().getSpeed());
        _precisionPanel.setValue(getEdittedParticles().getParticleController().getPrecision());
        final ParticleSystem system = getEdittedParticles();
        _renderBucketCB.setSelectedItem(system.getSceneHints().getRenderBucketType());
    }

    private void countButton_actionPerformed(final ActionEvent e) {
        final String response = JOptionPane.showInputDialog(this, "Please enter a new particle count for this system:",
                "How many particles?", JOptionPane.PLAIN_MESSAGE);
        if (response == null) {
            return;
        }
        int particles = 100;
        try {
            particles = Integer.parseInt(response);
        } catch (final NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number entered.  Using 100 instead.", "Invalid",
                    JOptionPane.WARNING_MESSAGE);
            particles = 100;
        }
        getEdittedParticles().recreate(particles);
        updateCountLabels();
        validate();
    }
}
