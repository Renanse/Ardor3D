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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.MouseInputAdapter;

import com.ardor3d.math.util.MathUtils;

public class ValueSpinner extends JSpinner {
  private static final long serialVersionUID = 1L;
  private final NumberEditor _ne;

  public ValueSpinner(final double minimum, final double maximum, final double stepSize) {
    this(Double.valueOf(minimum), Double.valueOf(maximum), Double.valueOf(stepSize));
    ((NumberEditor) getEditor()).getFormat().setMinimumFractionDigits((int) MathUtils.log(1.0 / stepSize, 10.0));
  }

  public ValueSpinner(final float minimum, final float maximum, final float stepSize) {
    this(Float.valueOf(minimum), Float.valueOf(maximum), Float.valueOf(stepSize));
    ((NumberEditor) getEditor()).getFormat().setMinimumFractionDigits((int) MathUtils.log(1f / stepSize, 10f));
  }

  public ValueSpinner(final int minimum, final int maximum, final int stepSize) {
    this(Integer.valueOf(minimum), Integer.valueOf(maximum), Integer.valueOf(stepSize));
  }

  public ValueSpinner(final Number minimum, final Number maximum, final Number stepSize) {
    super(new SpinnerNumberModel(minimum, (Comparable<?>) minimum, (Comparable<?>) maximum, stepSize));
    final MouseInputAdapter mia = new MouseInputAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        _last.setLocation(e.getPoint());
      }

      @Override
      public void mouseDragged(final MouseEvent e) {
        final int delta = (e.getX() - _last.x) + (_last.y - e.getY());
        _last.setLocation(e.getPoint());
        for (int ii = 0, nn = Math.abs(delta); ii < nn; ii++) {
          final Object next = (delta > 0) ? getModel().getNextValue() : getModel().getPreviousValue();
          if (next != null) {
            getModel().setValue(next);
          }
        }
      }

      protected Point _last = new Point();
    };
    _ne = new NumberEditor(this) {
      private static final long serialVersionUID = 1L;

      @Override
      public Dimension preferredLayoutSize(final Container parent) {
        final Dimension d = super.preferredLayoutSize(parent);
        d.width = Math.min(Math.max(d.width, 50), 65);
        return d;
      }
    };
    setEditor(_ne);
    addMouseInputListener(this, mia);
  }

  @Override
  public Dimension getMinimumSize() { return _ne.getPreferredSize(); }

  protected void addMouseInputListener(final Container c, final MouseInputAdapter mia) {
    for (int ii = 0, nn = c.getComponentCount(); ii < nn; ii++) {
      final Component comp = c.getComponent(ii);
      if (comp instanceof JButton) {
        comp.addMouseListener(mia);
        comp.addMouseMotionListener(mia);

      } else if (comp instanceof Container) {
        addMouseInputListener((Container) comp, mia);
      }
    }
  }
}
