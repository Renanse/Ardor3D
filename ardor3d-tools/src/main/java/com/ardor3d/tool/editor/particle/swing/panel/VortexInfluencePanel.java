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
import javax.swing.JComboBox;

import com.ardor3d.extension.effect.particle.SimpleParticleInfluenceFactory;
import com.ardor3d.math.Line3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.tool.editor.swing.widget.SphericalUnitVectorPanel;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;

public class VortexInfluencePanel extends InfluenceEditPanel {

  private static final long serialVersionUID = 1L;

  private final JComboBox<String> _vortexTypeBox = new JComboBox<>(new String[] {"Cylinder", "Torus"});
  private final ValuePanel _vortexRadiusPanel = new ValuePanel("Radius: ", "", 0.0, Double.MAX_VALUE, 1.0);
  private final ValuePanel _vortexHeightPanel =
      new ValuePanel("Height: ", "", -Double.MAX_VALUE, Double.MAX_VALUE, 1.0);
  private final ValuePanel _vortexStrengthPanel = new ValuePanel("Strength: ", "", 0.0, Double.MAX_VALUE, 0.1);
  private final ValuePanel _vortexDivergencePanel = new ValuePanel("Divergence: ", "", -90.0, 90.0, 1.0);
  private final SphericalUnitVectorPanel _vortexDirectionPanel = new SphericalUnitVectorPanel();
  private final JCheckBox _vortexRandomBox;

  public VortexInfluencePanel() {
    super();
    setLayout(new GridBagLayout());

    _vortexTypeBox.addActionListener(e -> {
      final int type = _vortexTypeBox.getSelectedIndex();
      ((SimpleParticleInfluenceFactory.BasicVortex) getEdittedInfluence()).setType(type);
      _vortexRadiusPanel.setEnabled(type == SimpleParticleInfluenceFactory.BasicVortex.VT_TORUS);
      _vortexHeightPanel.setEnabled(type == SimpleParticleInfluenceFactory.BasicVortex.VT_TORUS);
    });

    _vortexRadiusPanel.addChangeListener(e -> ((SimpleParticleInfluenceFactory.BasicVortex) getEdittedInfluence())
        .setRadius(_vortexRadiusPanel.getDoubleValue()));

    _vortexHeightPanel.addChangeListener(e -> ((SimpleParticleInfluenceFactory.BasicVortex) getEdittedInfluence())
        .setHeight(_vortexHeightPanel.getDoubleValue()));

    _vortexDirectionPanel.setBorder(createTitledBorder(" DIRECTION "));
    _vortexDirectionPanel.addChangeListener(e -> {
      final SimpleParticleInfluenceFactory.BasicVortex vortex =
          (SimpleParticleInfluenceFactory.BasicVortex) getEdittedInfluence();
      final Line3 axis = new Line3(vortex.getAxis());
      axis.setDirection(_vortexDirectionPanel.getValue());
      vortex.setAxis(axis);
    });
    _vortexStrengthPanel.addChangeListener(e -> ((SimpleParticleInfluenceFactory.BasicVortex) getEdittedInfluence())
        .setStrength(_vortexStrengthPanel.getDoubleValue()));
    _vortexDivergencePanel.addChangeListener(e -> ((SimpleParticleInfluenceFactory.BasicVortex) getEdittedInfluence())
        .setDivergence(_vortexDivergencePanel.getDoubleValue() * MathUtils.DEG_TO_RAD));
    _vortexRandomBox = new JCheckBox(new AbstractAction("Vary Randomly") {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        ((SimpleParticleInfluenceFactory.BasicVortex) getEdittedInfluence()).setRandom(_vortexRandomBox.isSelected());
      }
    });

    setBorder(createTitledBorder(" VORTEX PARAMETERS "));
    add(createBoldLabel("Type:"), new GridBagConstraints(0, 0, 1, 1, 0, 0.0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
    add(_vortexRadiusPanel, new GridBagConstraints(0, 1, 2, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
    add(_vortexHeightPanel, new GridBagConstraints(0, 2, 2, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
    add(_vortexTypeBox, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
    add(_vortexStrengthPanel, new GridBagConstraints(0, 3, 2, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
    add(_vortexDivergencePanel, new GridBagConstraints(0, 4, 2, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
    add(_vortexDirectionPanel, new GridBagConstraints(0, 5, 2, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
    add(_vortexRandomBox, new GridBagConstraints(0, 6, 2, 1, 0.5, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(5, 5, 10, 5), 0, 0));
  }

  @Override
  public void updateWidgets() {
    final SimpleParticleInfluenceFactory.BasicVortex vortex =
        (SimpleParticleInfluenceFactory.BasicVortex) getEdittedInfluence();
    _vortexTypeBox.setSelectedIndex(vortex.getType());
    _vortexHeightPanel.setValue(vortex.getHeight());
    _vortexHeightPanel.setEnabled(vortex.getType() == SimpleParticleInfluenceFactory.BasicVortex.VT_TORUS);
    _vortexRadiusPanel.setValue(vortex.getRadius());
    _vortexRadiusPanel.setEnabled(vortex.getType() == SimpleParticleInfluenceFactory.BasicVortex.VT_TORUS);
    _vortexDirectionPanel.setValue(vortex.getAxis().getDirection());
    _vortexStrengthPanel.setValue(vortex.getStrength());
    _vortexDivergencePanel.setValue(vortex.getDivergence() * MathUtils.RAD_TO_DEG);
    _vortexRandomBox.setSelected(vortex.isRandom());
  }
}
