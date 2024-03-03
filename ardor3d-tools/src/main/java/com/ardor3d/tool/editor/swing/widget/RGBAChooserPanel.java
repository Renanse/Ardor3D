/**
 * Copyright (c) 2008-2021 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.tool.editor.swing.widget;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;

import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.ardor3d.image.util.awt.AwtColorUtil;
import com.ardor3d.math.ColorRGBA;

public abstract class RGBAChooserPanel extends JPanel {
  @Serial
  private static final long serialVersionUID = 1L;

  private final JSpinner _alphaSpinner;
  private final JPanel _rgbPanel;

  public RGBAChooserPanel() {
    super();
    setLayout(new GridBagLayout());

    final JLabel rgbLabel = new JLabel();
    rgbLabel.setText("RGB");
    final GridBagConstraints gridBagConstraints_2 = new GridBagConstraints();
    gridBagConstraints_2.anchor = GridBagConstraints.SOUTH;
    gridBagConstraints_2.gridx = 0;
    gridBagConstraints_2.gridy = 0;
    add(rgbLabel, gridBagConstraints_2);

    _rgbPanel = new JPanel();
    final GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.ipady = 20;
    gridBagConstraints.ipadx = 20;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridx = 0;
    _rgbPanel.setToolTipText("Click here to set RGB color.");
    final ColorRGBA rgb = new ColorRGBA(getColor());
    rgb.setAlpha(1);
    _rgbPanel.setBackground(AwtColorUtil.makeColor(rgb));
    _rgbPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        final Color picked = JColorChooser.showDialog(RGBAChooserPanel.this, "Pick a color", _rgbPanel.getBackground());
        if (picked == null) {
          return;
        }
        _rgbPanel.setBackground(picked);
        final ColorRGBA color = AwtColorUtil.makeColorRGBA(picked);
        color.setAlpha(((Integer) _alphaSpinner.getValue()) / 255f);
        setColor(color);
      }
    });
    add(_rgbPanel, gridBagConstraints);

    final JLabel alphaLabel = new JLabel();
    alphaLabel.setText("alpha");
    final GridBagConstraints gridBagConstraints_3 = new GridBagConstraints();
    gridBagConstraints_3.insets = new Insets(2, 0, 0, 0);
    gridBagConstraints_3.anchor = GridBagConstraints.SOUTH;
    gridBagConstraints_3.gridy = 2;
    gridBagConstraints_3.gridx = 0;
    add(alphaLabel, gridBagConstraints_3);

    final SpinnerNumberModel snm = new SpinnerNumberModel((int) (getColor().getAlpha() * 255), 0, 255, 1);
    _alphaSpinner = new JSpinner(snm);
    _alphaSpinner.setToolTipText("Alpha value for above color.");
    _alphaSpinner.addChangeListener(e -> {
      final ColorRGBA color = AwtColorUtil.makeColorRGBA(_rgbPanel.getBackground());
      color.setAlpha(snm.getNumber().floatValue() / 255f);
      setColor(color);
    });
    final GridBagConstraints gridBagConstraints_1 = new GridBagConstraints();
    gridBagConstraints_1.insets = new Insets(2, 0, 0, 0);
    gridBagConstraints_1.ipadx = 15;
    gridBagConstraints_1.gridy = 3;
    gridBagConstraints_1.gridx = 0;
    add(_alphaSpinner, gridBagConstraints_1);
  }

  public void updateColor() {
    final ColorRGBA rgb = new ColorRGBA(getColor());
    _rgbPanel.setBackground(AwtColorUtil.makeColor(rgb));
    _alphaSpinner.setValue((int) (getColor().getAlpha() * 255));
  }

  protected abstract ColorRGBA getColor();

  protected abstract void setColor(ColorRGBA color);
}
