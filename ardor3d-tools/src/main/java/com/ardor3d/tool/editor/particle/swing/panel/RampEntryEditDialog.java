/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.tool.editor.particle.swing.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ardor3d.extension.effect.particle.RampEntry;
import com.ardor3d.image.util.awt.AwtColorUtil;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;
import com.ardor3d.tool.editor.swing.widget.ValueSpinner;

public class RampEntryEditDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JColorChooser _colorChooser = new JColorChooser();
    private JDialog _colorChooserDialog = new JDialog((JFrame) null, "Choose a color:");
    private final ValueSpinner _alphaSpinner = new ValueSpinner(0, 255, 1);
    private final JLabel _colorHex = new JLabel();
    private final JPanel _sColorPanel = new JPanel();

    public RampEntryEditDialog(final RampEntry entry) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setLayout(new GridBagLayout());

        if (entry.getOffset() != -1) {
            final ValuePanel offsetPanel = new ValuePanel("Offset: ", "%", 1, 100, 1);
            offsetPanel.setValue((int) (entry.getOffset() * 100));
            offsetPanel.addChangeListener(new ChangeListener() {
                public void stateChanged(final ChangeEvent e) {
                    entry.setOffset(offsetPanel.getIntValue() / 100f);
                }
            });

            final JPanel off = new JPanel(new GridBagLayout());
            off.setBorder(createTitledBorder("OFFSET"));
            off.add(offsetPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                    GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            add(off, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                    GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));
        }

        final ValuePanel sizePanel = new ValuePanel("Size: ", "", 0.0, Double.MAX_VALUE, 1.0);
        sizePanel.setValue(0.0);
        sizePanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                entry.setSize(sizePanel.getDoubleValue());
            }
        });

        if (entry.getOffset() != -1) {
            final JCheckBox sizeCheck = new JCheckBox("");
            sizeCheck.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    sizePanel.setEnabled(sizeCheck.isSelected());
                    entry.setSize(sizeCheck.isSelected() ? sizePanel.getDoubleValue() : RampEntry.DEFAULT_SIZE);
                }
            });
            if (entry.hasSizeSet()) {
                sizeCheck.setSelected(true);
            } else {
                sizeCheck.doClick();
                sizeCheck.doClick();
            }
            add(sizeCheck, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
                    GridBagConstraints.NONE, new Insets(10, 5, 5, 5), 0, 0));
        }
        if (entry.hasSizeSet()) {
            sizePanel.setValue(entry.getSize());
        }

        final JPanel size = new JPanel(new GridBagLayout());
        size.setBorder(createTitledBorder("PARTICLE SIZE"));
        size.add(sizePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        add(size, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));

        final ValuePanel spinPanel = new ValuePanel("Spin: ", "", -Double.MAX_VALUE, Double.MAX_VALUE, 0.01);
        spinPanel.setValue(0.0);
        spinPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                entry.setSpin(spinPanel.getDoubleValue());
            }
        });

        if (entry.getOffset() != -1) {
            final JCheckBox spinCheck = new JCheckBox("");
            spinCheck.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    spinPanel.setEnabled(spinCheck.isSelected());
                    entry.setSpin(spinCheck.isSelected() ? spinPanel.getDoubleValue() : RampEntry.DEFAULT_SPIN);
                }
            });
            if (entry.hasSpinSet()) {
                spinCheck.setSelected(true);
            } else {
                spinCheck.doClick();
                spinCheck.doClick();
            }
            add(spinCheck, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
                    GridBagConstraints.NONE, new Insets(10, 5, 5, 5), 0, 0));
        }
        if (entry.hasSpinSet()) {
            spinPanel.setValue(entry.getSpin());
        }

        final JPanel spin = new JPanel(new GridBagLayout());
        spin.setBorder(createTitledBorder("PARTICLE SPIN"));
        spin.add(spinPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        add(spin, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));

        final ValuePanel massPanel = new ValuePanel("Mass: ", "", -Double.MAX_VALUE, Double.MAX_VALUE, 0.01);
        massPanel.setValue(0.0);
        massPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                entry.setMass(massPanel.getDoubleValue());
            }
        });

        if (entry.getOffset() != -1) {
            final JCheckBox massCheck = new JCheckBox("");
            massCheck.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    massPanel.setEnabled(massCheck.isSelected());
                    entry.setMass(massCheck.isSelected() ? massPanel.getDoubleValue() : RampEntry.DEFAULT_MASS);
                }
            });
            if (entry.hasMassSet()) {
                massCheck.setSelected(true);
            } else {
                massCheck.doClick();
                massCheck.doClick();
            }
            add(massCheck, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
                    GridBagConstraints.NONE, new Insets(10, 5, 5, 5), 0, 0));
        }
        if (entry.hasMassSet()) {
            massPanel.setValue(entry.getMass());
        }

        final JPanel mass = new JPanel(new GridBagLayout());
        mass.setBorder(createTitledBorder("PARTICLE MASS"));
        mass.add(massPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        add(mass, new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));

        final JLabel colorLabel = createBoldLabel("Color:"), alphaLabel = new JLabel("A:");
        _colorHex.setFont(new Font("Arial", Font.PLAIN, 10));
        _colorHex.setText("#FFFFFF");

        _sColorPanel.setBackground(Color.white);
        _sColorPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        _sColorPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (_sColorPanel.isEnabled()) {
                    colorPanel_mouseClicked(e);
                }
            }
        });

        _alphaSpinner.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final ColorRGBA color = new ColorRGBA(entry.getColor());
                color.setAlpha(((Number) _alphaSpinner.getValue()).intValue() / 255f);
                entry.setColor(color);
            }
        });

        final JPanel colorPanel = new JPanel(new GridBagLayout());
        colorPanel.setBorder(createTitledBorder("PARTICLE COLOR"));
        colorPanel.add(colorLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(5, 10, 0, 10), 0, 0));
        colorPanel.add(_sColorPanel, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 25, 25));
        colorPanel.add(_colorHex, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(0, 0, 4, 0), 0, 0));
        colorPanel.add(_alphaSpinner, new GridBagConstraints(1, 3, 1, 1, 0.25, 0.0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 20, 0));
        colorPanel.add(alphaLabel, new GridBagConstraints(0, 3, 1, 1, 0.25, 0.0, GridBagConstraints.EAST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        if (entry.getOffset() != -1) {
            final JCheckBox colorCheck = new JCheckBox("");
            colorCheck.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    _sColorPanel.setEnabled(colorCheck.isSelected());
                    alphaLabel.setEnabled(colorCheck.isSelected());
                    colorLabel.setEnabled(colorCheck.isSelected());
                    _alphaSpinner.setEnabled(colorCheck.isSelected());

                    final ColorRGBA color = AwtColorUtil.makeColorRGBA(_sColorPanel.getBackground());
                    color.setAlpha(((Number) _alphaSpinner.getValue()).intValue() / 255f);
                    entry.setColor(colorCheck.isSelected() ? color : RampEntry.DEFAULT_COLOR);
                }
            });
            if (entry.hasColorSet()) {
                colorCheck.setSelected(true);
            } else {
                colorCheck.doClick();
                colorCheck.doClick();
            }
            add(colorCheck, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
                    GridBagConstraints.NONE, new Insets(10, 5, 5, 5), 0, 0));
        }
        if (entry.hasColorSet()) {
            _sColorPanel.setBackground(AwtColorUtil.makeColor(entry.getColor(), false));
            _colorHex.setText(convColorToHex(_sColorPanel.getBackground()));
            _alphaSpinner.setValue(new Integer(AwtColorUtil.makeColor(entry.getColor(), true).getAlpha()));
        }
        add(colorPanel, new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));

        setColorChooserDialogOwner(this, entry);
        pack();
    }

    public void setColorChooserDialogOwner(final JDialog owner, final RampEntry entry) {
        _colorChooserDialog = new JDialog(owner, "Choose a color:");
        initColorChooser(entry);
    }

    private void initColorChooser(final RampEntry entry) {
        _colorChooser.setColor(_sColorPanel.getBackground());
        _colorChooserDialog.setLayout(new BorderLayout());
        _colorChooserDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        _colorChooserDialog.add(_colorChooser, BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        final JButton okButton = new JButton("Ok");
        okButton.setOpaque(true);
        okButton.setMnemonic('O');
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Color color = _colorChooser.getColor();
                if (color == null) {
                    return;
                }
                final ColorRGBA rgba = AwtColorUtil.makeColorRGBA(color);
                rgba.setAlpha((Integer.parseInt(_alphaSpinner.getValue().toString()) / 255f));
                entry.setColor(rgba);
                _sColorPanel.setBackground(color);
                _colorHex.setText(convColorToHex(_sColorPanel.getBackground()));
                _colorChooserDialog.setVisible(false);
            }
        });

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.setOpaque(true);
        cancelButton.setMnemonic('C');
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                _colorChooserDialog.setVisible(false);
            }
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        _colorChooserDialog.add(buttonPanel, BorderLayout.SOUTH);
        _colorChooserDialog.setSize(_colorChooserDialog.getPreferredSize());
        _colorChooserDialog.setLocationRelativeTo(null);
    }

    protected TitledBorder createTitledBorder(final String title) {
        final TitledBorder border = new TitledBorder(" " + title + " ");
        border.setTitleFont(new Font("Arial", Font.PLAIN, 10));
        return border;
    }

    private String convColorToHex(final Color c) {
        if (c == null) {
            return null;
        }
        String sRed = Integer.toHexString(c.getRed());
        if (sRed.length() == 1) {
            sRed = "0" + sRed;
        }
        String sGreen = Integer.toHexString(c.getGreen());
        if (sGreen.length() == 1) {
            sGreen = "0" + sGreen;
        }
        String sBlue = Integer.toHexString(c.getBlue());
        if (sBlue.length() == 1) {
            sBlue = "0" + sBlue;
        }
        return "#" + sRed + sGreen + sBlue;
    }

    private void colorPanel_mouseClicked(final MouseEvent e) {
        _colorChooser.setColor(_sColorPanel.getBackground());
        if (!_colorChooserDialog.isVisible()) {
            _colorChooserDialog.setVisible(true);
        }
    }

    protected JLabel createBoldLabel(final String text) {
        final JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 13));
        return label;
    }

}
