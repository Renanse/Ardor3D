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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ardor3d.extension.effect.particle.SimpleParticleInfluenceFactory;
import com.ardor3d.tool.editor.swing.widget.SphericalUnitVectorPanel;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;

public class WindInfluencePanel extends InfluenceEditPanel {

    private static final long serialVersionUID = 1L;

    private final ValuePanel _windStrengthPanel = new ValuePanel("Strength: ", "", 0.0, 100.0, 0.1);
    private final SphericalUnitVectorPanel _windDirectionPanel = new SphericalUnitVectorPanel();
    private final JCheckBox _windRandomBox;

    public WindInfluencePanel() {
        super();
        setLayout(new GridBagLayout());

        _windDirectionPanel.setBorder(createTitledBorder(" DIRECTION "));
        _windDirectionPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                ((SimpleParticleInfluenceFactory.BasicWind) getEdittedInfluence()).setWindDirection(_windDirectionPanel
                        .getValue());
            }
        });
        _windStrengthPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                ((SimpleParticleInfluenceFactory.BasicWind) getEdittedInfluence()).setStrength(_windStrengthPanel
                        .getDoubleValue());
            }
        });
        _windRandomBox = new JCheckBox(new AbstractAction("Vary Randomly") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                ((SimpleParticleInfluenceFactory.BasicWind) getEdittedInfluence()).setRandom(_windRandomBox
                        .isSelected());
            }
        });

        setBorder(createTitledBorder(" WIND PARAMETERS "));
        add(_windDirectionPanel, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
        add(_windStrengthPanel, new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
        add(_windRandomBox, new GridBagConstraints(0, 2, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(5, 5, 10, 5), 0, 0));
    }

    @Override
    public void updateWidgets() {
        final SimpleParticleInfluenceFactory.BasicWind wind = (SimpleParticleInfluenceFactory.BasicWind) getEdittedInfluence();
        _windDirectionPanel.setValue(wind.getWindDirection());
        _windStrengthPanel.setValue(wind.getStrength());
        _windRandomBox.setSelected(wind.isRandom());
    }
}
