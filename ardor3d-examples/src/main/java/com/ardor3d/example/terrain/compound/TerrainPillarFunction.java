
package com.ardor3d.example.terrain.compound;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.ardor3d.extension.terrain.providers.procedural.ProceduralTerrainSource;
import com.ardor3d.math.functions.Function3D;

public class TerrainPillarFunction implements Function3D, UIEditableFunction {
  private double _offX;
  private double _offY;
  private double _radius;
  private double _height;

  public TerrainPillarFunction(final double offX, final double offY, final double radius, final double height) {
    _offX = offX;
    _offY = offY;
    _radius = radius;
    _height = height;
  }

  @Override
  public double eval(final double x, final double y, final double z) {
    final double tX = x - getOffX();
    final double tY = y - getOffY();
    return (tX * tX + tY * tY) < getRadius() * getRadius() ? getHeight() : Double.NaN;
  }

  public double getOffX() { return _offX; }

  public void setOffX(final double offX) { _offX = offX; }

  public double getOffY() { return _offY; }

  public void setOffY(final double offY) { _offY = offY; }

  public double getRadius() { return _radius; }

  public void setRadius(final double radius) { _radius = radius; }

  public double getHeight() { return _height; }

  public void setHeight(final double height) { _height = height; }

  @Override
  public void setupFunctionEditPanel(final Container parent, final ProceduralTerrainSource terrainSource) {
    parent.removeAll();
    parent.setLayout(new BoxLayout(parent, BoxLayout.PAGE_AXIS));
    parent.add(getDoubleFieldEditor("offset X:", _offX, d -> {
      setOffX(d);
      terrainSource.markInvalid();
    }));
    parent.add(getDoubleFieldEditor("offset Y:", _offY, d -> {
      setOffY(d);
      terrainSource.markInvalid();
    }));
    parent.add(getDoubleFieldEditor("radius:", _radius, d -> {
      setRadius(d);
      terrainSource.markInvalid();
    }));
    parent.add(getDoubleFieldEditor("height:", _height, d -> {
      setHeight(d);
      terrainSource.markInvalid();
    }));
    parent.revalidate();
    parent.repaint();
  }

  private static Component getDoubleFieldEditor(final String label, final double value,
      final Consumer<Double> onAction) {
    final JPanel editPanel = new JPanel();
    editPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
    editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.LINE_AXIS));
    editPanel.add(Box.createRigidArea(new Dimension(15, 0)));
    editPanel.add(new JLabel(label));
    editPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    final JTextField valueField = new JTextField(4);
    valueField.setMaximumSize(new Dimension(100, 30));
    valueField.setText(Double.toString(value));
    valueField.addActionListener(e -> {
      try {
        final double d = Double.parseDouble(valueField.getText());
        onAction.accept(d);
      } catch (final NumberFormatException nfe) {
        nfe.printStackTrace();
      }
    });
    editPanel.add(valueField);
    editPanel.add(Box.createHorizontalGlue());
    editPanel.add(Box.createRigidArea(new Dimension(15, 0)));
    return editPanel;
  }
}
