/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.tool.editor.particle.swing.panel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ardor3d.extension.effect.particle.SwarmInfluence;
import com.ardor3d.math.MathUtils;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;
import com.ardor3d.tool.editor.swing.widget.Vector3Panel;

public class SwarmInfluencePanel extends InfluenceEditPanel {

    private static final long serialVersionUID = 1L;

    private final ValuePanel _swarmRange = new ValuePanel("Range: ", "", -Double.MIN_VALUE, Double.MAX_VALUE, 0.1);
    private final ValuePanel _swarmTurnSpeed = new ValuePanel("Turn Speed: ", "", -Double.MIN_VALUE, Double.MAX_VALUE,
            0.1);
    private final ValuePanel _swarmMaxSpeed = new ValuePanel("Max Speed: ", "", -Double.MIN_VALUE, Double.MAX_VALUE,
            0.1);
    private final ValuePanel _swarmAcceleration = new ValuePanel("Acceleration: ", "", -Double.MIN_VALUE,
            Double.MAX_VALUE, 0.1);
    private final ValuePanel _swarmDeviance = new ValuePanel("Deviance: ", "", 0.0, 180.0, 1.0);
    private final Vector3Panel _swarmLocationPanel = new Vector3Panel(-Double.MIN_VALUE, Double.MAX_VALUE, 0.1);

    public SwarmInfluencePanel() {
        super();
        setLayout(new GridBagLayout());

        _swarmRange.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                ((SwarmInfluence) getEdittedInfluence()).setSwarmRange(_swarmRange.getDoubleValue());
            }
        });
        _swarmDeviance.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                ((SwarmInfluence) getEdittedInfluence()).setDeviance(MathUtils.DEG_TO_RAD
                        * _swarmDeviance.getDoubleValue());
            }
        });
        _swarmMaxSpeed.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                ((SwarmInfluence) getEdittedInfluence()).setMaxSpeed(_swarmMaxSpeed.getDoubleValue());
            }
        });
        _swarmAcceleration.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                ((SwarmInfluence) getEdittedInfluence()).setSpeedBump(_swarmAcceleration.getDoubleValue());
            }
        });
        _swarmTurnSpeed.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                ((SwarmInfluence) getEdittedInfluence()).setTurnSpeed(_swarmTurnSpeed.getDoubleValue());
            }
        });

        _swarmLocationPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                ((SwarmInfluence) getEdittedInfluence()).setSwarmOffset(_swarmLocationPanel.getValue());
            }
        });

        _swarmLocationPanel.setBorder(createTitledBorder(" SWARM OFFSET "));

        setBorder(createTitledBorder(" SWARM PARAMETERS "));
        add(_swarmLocationPanel, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
        add(_swarmRange, new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
        add(_swarmMaxSpeed, new GridBagConstraints(0, 2, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
        add(_swarmAcceleration, new GridBagConstraints(0, 3, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
        add(_swarmTurnSpeed, new GridBagConstraints(0, 4, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
        add(_swarmDeviance, new GridBagConstraints(0, 5, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
    }

    @Override
    public void updateWidgets() {
        final SwarmInfluence swarm = (SwarmInfluence) getEdittedInfluence();
        _swarmLocationPanel.setValue(swarm.getSwarmOffset());
        _swarmRange.setValue(swarm.getSwarmRange());
        _swarmMaxSpeed.setValue(swarm.getMaxSpeed());
        _swarmAcceleration.setValue(swarm.getSpeedBump());
        _swarmTurnSpeed.setValue(swarm.getTurnSpeed());
        _swarmDeviance.setValue(swarm.getDeviance() * MathUtils.RAD_TO_DEG);
    }
}
