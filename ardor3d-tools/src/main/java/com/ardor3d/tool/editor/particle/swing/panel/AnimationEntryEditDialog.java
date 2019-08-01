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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.ardor3d.extension.effect.particle.AnimationEntry;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;

public class AnimationEntryEditDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public AnimationEntryEditDialog(final AnimationEntry entry) {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        setLayout(new GridBagLayout());

        final ValuePanel offsetPanel = new ValuePanel("Offset: ", "%", 0, 100, 1);
        offsetPanel.setValue((int) (entry.getOffset() * 100));
        offsetPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                entry.setOffset(offsetPanel.getIntValue() / 100.0);
            }
        });

        final JPanel off = new JPanel(new GridBagLayout());
        off.setBorder(createTitledBorder("OFFSET"));
        off.add(offsetPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        add(off, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(10, 5, 5, 5), 0, 0));

        final ValuePanel ratePanel = new ValuePanel("Transition Time: ", "secs", 0.0, Double.MAX_VALUE, .01);
        ratePanel.setValue(0.0);
        ratePanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                entry.setRate(ratePanel.getFloatValue());
            }
        });

        ratePanel.setValue(entry.getRate());

        final JPanel rate = new JPanel(new GridBagLayout());
        rate.setBorder(createTitledBorder("PARTICLE SIZE"));
        rate.add(ratePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        add(rate, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));

        final JLabel framesLabel = createBoldLabel("Frames: ");

        final JTextField framesField = new JTextField();
        framesField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(final DocumentEvent e) {
                entry.setFrames(makeFrames(framesField.getText()));
            }

            public void insertUpdate(final DocumentEvent e) {
                entry.setFrames(makeFrames(framesField.getText()));
            }

            public void removeUpdate(final DocumentEvent e) {
                entry.setFrames(makeFrames(framesField.getText()));
            }
        });

        framesField.setText(makeText(entry.getFrames()));

        final JPanel frames = new JPanel(new GridBagLayout());
        frames.setBorder(createTitledBorder("ANIMATION FRAMES"));
        frames.add(framesLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        frames.add(framesField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        add(frames, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));

        pack();
    }

    private String makeText(final int[] frames) {
        if (frames == null || frames.length == 0) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        for (final int frame : frames) {
            sb.append(frame);
            sb.append(",");
        }
        return sb.substring(0, sb.length() - 1);
    }

    private int[] makeFrames(final String text) {
        final StringTokenizer tok = new StringTokenizer(text, ",", false);
        final ArrayList<Integer> vals = new ArrayList<Integer>();
        while (tok.hasMoreTokens()) {
            final String token = tok.nextToken();
            if (token != null) {
                try {
                    vals.add(Integer.parseInt(token.trim()));
                } catch (final NumberFormatException nfe) {
                    ; // ignore.
                }
            }
        }
        final int[] rVal = new int[vals.size()];
        for (int x = 0; x < rVal.length; x++) {
            rVal[x] = vals.get(x);
        }
        return rVal;
    }

    protected TitledBorder createTitledBorder(final String title) {
        final TitledBorder border = new TitledBorder(" " + title + " ");
        border.setTitleFont(new Font("Arial", Font.PLAIN, 10));
        return border;
    }

    protected JLabel createBoldLabel(final String text) {
        final JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 13));
        return label;
    }

}
