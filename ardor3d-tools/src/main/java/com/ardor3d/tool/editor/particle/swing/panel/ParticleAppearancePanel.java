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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.ardor3d.extension.effect.particle.AnimationEntry;
import com.ardor3d.extension.effect.particle.ParticleFactory;
import com.ardor3d.extension.effect.particle.ParticleInfluence;
import com.ardor3d.extension.effect.particle.ParticlePoints;
import com.ardor3d.extension.effect.particle.ParticleSystem;
import com.ardor3d.extension.effect.particle.ParticleSystem.ParticleType;
import com.ardor3d.extension.effect.particle.RampEntry;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.tool.editor.particle.swing.ParticleEditorFrame;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;
import com.ardor3d.tool.editor.swing.widget.Vector3Panel;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.URLResourceSource;

public abstract class ParticleAppearancePanel extends ParticleEditPanel {
    private static final Logger logger = Logger.getLogger(ParticleAppearancePanel.class.getName());

    private static final long serialVersionUID = 1L;

    private File _newTexture = null;

    private final JCheckBox _additiveBlendingBox;
    private final JComboBox<ParticleType> _geomTypeBox;
    private final JCheckBox _velocityAlignedBox;
    private final JCheckBox _cameraAlignedBox;
    private final Vector3Panel _parentDirectionLeftPanel;
    private final Vector3Panel _parentDirectionUpPanel;
    private final JLabel _imageLabel = new JLabel();

    private final JList<RampEntry> _rampList;
    private final DefaultListModel<RampEntry> _rampModel = new DefaultListModel<RampEntry>();
    private final JButton _rampAddButton = makeListButton("Add");
    private final JButton _rampRemoveButton = makeListButton("Remove");
    private final JButton _rampEditButton = makeListButton("Edit");
    private final JButton _rampMoveUpButton = makeListButton("/\\");
    private final JButton _rampMoveDownButton = makeListButton("\\/");

    private final JList<AnimationEntry> _animList;
    private final DefaultListModel<AnimationEntry> _animModel = new DefaultListModel<AnimationEntry>();
    private final JButton _animAddButton = makeListButton("Add");
    private final JButton _animRemoveButton = makeListButton("Remove");
    private final JButton _animEditButton = makeListButton("Edit");
    private final JButton _animMoveUpButton = makeListButton("/\\");
    private final JButton _animMoveDownButton = makeListButton("\\/");

    private final Preferences _prefs;
    private final JFileChooser _textureChooser = new JFileChooser();
    private final JPanel _texturePanel;

    private final ValuePanel _texPanel, _startTexPanel;

    public ParticleAppearancePanel(final Preferences prefs) {
        super();
        _prefs = prefs;
        setLayout(new GridBagLayout());

        _geomTypeBox = new JComboBox<ParticleType>(new ParticleType[] { ParticleType.Triangle, ParticleType.Line,
                ParticleType.Point });
        _geomTypeBox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                changeParticleType((ParticleType) _geomTypeBox.getSelectedItem());
            }
        });

        _parentDirectionLeftPanel = new Vector3Panel(-1.0, 1.0, 0.1);
        _parentDirectionLeftPanel.setBorder(createTitledBorder(" particle left "));
        _parentDirectionLeftPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setFacingLeftVector(_parentDirectionLeftPanel.getValue());
            }
        });
        _parentDirectionLeftPanel.setVisible(false);

        _parentDirectionUpPanel = new Vector3Panel(-1.0, 1.0, 0.1);
        _parentDirectionUpPanel.setBorder(createTitledBorder(" particle up "));
        _parentDirectionUpPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setFacingUpVector(_parentDirectionUpPanel.getValue());
            }
        });
        _parentDirectionUpPanel.setVisible(false);

        _velocityAlignedBox = new JCheckBox(new AbstractAction("Align with Velocity") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                getEdittedParticles().setVelocityAligned(_velocityAlignedBox.isSelected());
            }
        });
        _velocityAlignedBox.setFont(new Font("Arial", Font.BOLD, 13));

        _cameraAlignedBox = new JCheckBox(new AbstractAction("Align with Camera") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                getEdittedParticles().setCameraFacing(_cameraAlignedBox.isSelected());
                updateVisibleControls();
            }
        });
        _cameraAlignedBox.setFont(new Font("Arial", Font.BOLD, 13));

        _rampList = new JList<RampEntry>(_rampModel);
        _rampList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _rampList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                final int selected = _rampList.getSelectedIndex();
                _rampRemoveButton.setEnabled(selected > 0 && selected < _rampModel.getSize() - 1);
                _rampEditButton.setEnabled(selected != -1);
                _rampMoveUpButton.setEnabled(selected > 1 && selected < _rampModel.getSize() - 1);
                _rampMoveDownButton.setEnabled(selected < _rampModel.getSize() - 2 && selected > 0);
            }
        });
        _rampList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() > 1) {
                    _rampEditButton.doClick();
                    e.consume();
                }
            }
        });

        _rampAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        final RampEntry entry = new RampEntry();
                        getEdittedParticles().getRamp().addEntry(entry);
                        showEditWindow(entry);
                        updateRampModel();
                        _rampList.setSelectedValue(entry, true);
                    }
                }.start();
            }
        });

        _rampEditButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        final int index = _rampList.getSelectedIndex();
                        final RampEntry entry = _rampList.getSelectedValue();
                        showEditWindow(entry);
                        updateRampModel();
                        _rampList.setSelectedIndex(index);
                    };
                }.start();
            }
        });

        _rampRemoveButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final RampEntry entry = _rampList.getSelectedValue();
                getEdittedParticles().getRamp().removeEntry(entry);
                updateRampModel();
            }
        });

        _rampMoveUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final int index = _rampList.getSelectedIndex();
                final RampEntry entry = _rampList.getSelectedValue();
                getEdittedParticles().getRamp().removeEntry(entry);
                getEdittedParticles().getRamp().addEntry(index - 2, entry);
                updateRampModel();
                _rampList.setSelectedValue(entry, true);
            }
        });

        _rampMoveDownButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final int index = _rampList.getSelectedIndex();
                final RampEntry entry = _rampList.getSelectedValue();
                getEdittedParticles().getRamp().removeEntry(entry);
                getEdittedParticles().getRamp().addEntry(index, entry);
                updateRampModel();
                _rampList.setSelectedValue(entry, true);
            }
        });

        _rampRemoveButton.setEnabled(false);
        _rampEditButton.setEnabled(false);
        _rampMoveUpButton.setEnabled(false);
        _rampMoveDownButton.setEnabled(false);

        final JPanel geomPanel = new JPanel(new GridBagLayout());
        geomPanel.setBorder(createTitledBorder("PARTICLE GEOMETRY"));
        geomPanel.add(createBoldLabel("Type:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
        geomPanel.add(_geomTypeBox, new GridBagConstraints(1, 0, 2, 1, 0, 0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 0), 0, 0));
        geomPanel.add(_cameraAlignedBox, new GridBagConstraints(1, 1, 2, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        geomPanel.add(_velocityAlignedBox, new GridBagConstraints(1, 2, 2, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        geomPanel.add(_parentDirectionUpPanel, new GridBagConstraints(1, 3, 1, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        geomPanel.add(_parentDirectionLeftPanel, new GridBagConstraints(2, 3, 1, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 0), 0, 0));
        geomPanel.add(new JLabel(""), new GridBagConstraints(3, 0, 1, 1, 1.0, 0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 5), 0, 0));

        final JPanel rampPanel = new JPanel(new GridBagLayout());
        rampPanel.setBorder(createTitledBorder("APPEARANCE TIMELINE"));
        rampPanel.add(new JScrollPane(_rampList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(1, 0, 1, 6, 1.0, 1.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        rampPanel.add(_rampAddButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        rampPanel.add(_rampRemoveButton, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        rampPanel.add(_rampEditButton, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        rampPanel.add(_rampMoveUpButton, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        rampPanel.add(_rampMoveDownButton, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        _animList = new JList<AnimationEntry>(_animModel);
        _animList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        _animList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                final int selected = _animList.getSelectedIndex();
                _animRemoveButton.setEnabled(selected != -1);
                _animEditButton.setEnabled(selected != -1);
                _animMoveUpButton.setEnabled(selected > 0);
                _animMoveDownButton.setEnabled(selected != -1 && selected < _animModel.getSize() - 1);
            }
        });

        _animList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() > 1) {
                    _animEditButton.doClick();
                    e.consume();
                }
            }
        });

        _animAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        final AnimationEntry entry = new AnimationEntry();
                        getEdittedParticles().getTexAnimation().addEntry(entry);
                        showEditWindow(entry);
                        updateAnimModel();
                        _animList.setSelectedValue(entry, true);
                    }
                }.start();
            }
        });

        _animEditButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        final int index = _animList.getSelectedIndex();
                        final AnimationEntry entry = _animList.getSelectedValue();
                        showEditWindow(entry);
                        updateAnimModel();
                        _animList.setSelectedIndex(index);
                    };
                }.start();
            }
        });

        _animRemoveButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final AnimationEntry entry = _animList.getSelectedValue();
                getEdittedParticles().getTexAnimation().removeEntry(entry);
                updateAnimModel();
            }
        });

        _animMoveUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final int index = _animList.getSelectedIndex();
                final AnimationEntry entry = _animList.getSelectedValue();
                getEdittedParticles().getTexAnimation().removeEntry(entry);
                getEdittedParticles().getTexAnimation().addEntry(index - 1, entry);
                updateAnimModel();
                _animList.setSelectedValue(entry, true);
            }
        });

        _animMoveDownButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final int index = _animList.getSelectedIndex();
                final AnimationEntry entry = _animList.getSelectedValue();
                getEdittedParticles().getTexAnimation().removeEntry(entry);
                getEdittedParticles().getTexAnimation().addEntry(index + 1, entry);
                updateAnimModel();
                _animList.setSelectedValue(entry, true);
            }
        });

        _animRemoveButton.setEnabled(false);
        _animEditButton.setEnabled(false);
        _animMoveUpButton.setEnabled(false);
        _animMoveDownButton.setEnabled(false);

        final JPanel animPanel = new JPanel(new GridBagLayout());
        animPanel.setBorder(createTitledBorder("ANIMATION TIMELINE"));
        animPanel.add(new JScrollPane(_animList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED), new GridBagConstraints(1, 0, 1, 6, 1.0, 1.0,
                        GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
        animPanel.add(_animAddButton, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        animPanel.add(_animRemoveButton, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        animPanel.add(_animEditButton, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        animPanel.add(_animMoveUpButton, new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        animPanel.add(_animMoveDownButton, new GridBagConstraints(0, 4, 1, 1, 0, 0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));

        _additiveBlendingBox = new JCheckBox(new AbstractAction("Additive Blending") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                updateBlendState(_additiveBlendingBox.isSelected());
            }
        });
        _additiveBlendingBox.setFont(new Font("Arial", Font.BOLD, 13));

        final JPanel blendPanel = new JPanel(new GridBagLayout());
        blendPanel.setBorder(createTitledBorder("PARTICLE BLENDING"));
        blendPanel.add(_additiveBlendingBox, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        final JLabel textureLabel = createBoldLabel("Texture Image:");
        final JButton changeTextureButton = new JButton(new AbstractAction("Browse...") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                changeTexture();
            }
        });
        changeTextureButton.setFont(new Font("Arial", Font.BOLD, 12));
        changeTextureButton.setMargin(new Insets(2, 2, 2, 2));

        final JButton clearTextureButton = new JButton(new AbstractAction("Clear") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                ((TextureState) getEdittedParticles().getLocalRenderState(StateType.Texture)).setTexture(null);
                _imageLabel.setIcon(null);
            }
        });
        clearTextureButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearTextureButton.setMargin(new Insets(2, 2, 2, 2));

        _imageLabel.setBackground(Color.lightGray);
        _imageLabel.setMaximumSize(new Dimension(128, 128));
        _imageLabel.setMinimumSize(new Dimension(0, 0));
        _imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        _imageLabel.setOpaque(false);

        _texturePanel = new JPanel(new GridBagLayout());
        _texturePanel.setBorder(createTitledBorder("PARTICLE TEXTURE"));
        _texturePanel.add(textureLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        _texturePanel.add(changeTextureButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0));
        _texturePanel.add(clearTextureButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST,
                GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0));
        _texturePanel.add(_imageLabel, new GridBagConstraints(1, 0, 1, 3, 1.0, 1.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        _texPanel = new ValuePanel("Sub Images: ", "", 1, Integer.MAX_VALUE, 1);
        _texPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setTexQuantity(_texPanel.getIntValue());
            }
        });

        _texturePanel.add(_texPanel, new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        _startTexPanel = new ValuePanel("Start Index: ", "", 0, Integer.MAX_VALUE, 1);
        _startTexPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setStartTexIndex(_startTexPanel.getIntValue());
            }
        });

        _texturePanel.add(_startTexPanel, new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST,
                GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        add(geomPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 10), 0, 0));
        add(_texturePanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
        add(blendPanel, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
        add(rampPanel, new GridBagConstraints(0, 4, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
        add(animPanel, new GridBagConstraints(0, 5, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));

        final String tdir = _prefs.get("texture_dir", null);
        if (tdir != null) {
            _textureChooser.setCurrentDirectory(new File(tdir));
        }
    }

    protected JButton makeListButton(final String text) {
        final JButton button = new JButton(text);
        button.setMargin(new Insets(2, 2, 2, 2));
        return button;
    }

    protected void showEditWindow(final RampEntry entry) {
        final RampEntryEditDialog dialog = new RampEntryEditDialog(entry);
        dialog.setLocationRelativeTo(ParticleAppearancePanel.this);
        dialog.setModal(true);
        dialog.setVisible(true);
        dialog.toFront();
    }

    protected void showEditWindow(final AnimationEntry entry) {
        final AnimationEntryEditDialog dialog = new AnimationEntryEditDialog(entry);
        dialog.setLocationRelativeTo(ParticleAppearancePanel.this);
        dialog.setModal(true);
        dialog.setVisible(true);
        dialog.toFront();
    }

    protected void updateRampModel() {
        _rampModel.clear();
        _rampModel.addElement(new StartRamp(getEdittedParticles()));
        final Iterator<RampEntry> it = getEdittedParticles().getRamp().getEntries();
        while (it.hasNext()) {
            final RampEntry e = it.next();
            _rampModel.addElement(e);
        }
        _rampModel.addElement(new EndRamp(getEdittedParticles()));
    }

    protected void updateAnimModel() {
        _animModel.clear();
        final Iterator<AnimationEntry> it = getEdittedParticles().getTexAnimation().getEntries();
        while (it.hasNext()) {
            final AnimationEntry e = it.next();
            _animModel.addElement(e);
        }
    }

    private void changeParticleType(final ParticleType newType) {
        if (getEdittedParticles() == null) {
            return;
        }
        final ParticleType oldType = getEdittedParticles().getParticleType();
        if (newType == oldType) {
            return;
        }
        final ParticleSystem oldGeom = getEdittedParticles();
        ParticleSystem newGeom;
        if (newType == ParticleSystem.ParticleType.Point) {
            final ParticlePoints pPoints = (ParticlePoints) ParticleFactory.buildParticles(oldGeom.getName(),
                    oldGeom.getNumParticles(), ParticleType.Point);
            newGeom = pPoints;
            pPoints.setPointSize(5);
            pPoints.setAntialiased(true);
        } else if (newType == ParticleSystem.ParticleType.Line) {
            newGeom = ParticleFactory.buildParticles(oldGeom.getName(), oldGeom.getNumParticles(), ParticleType.Line);
        } else {
            newGeom = ParticleFactory.buildParticles(oldGeom.getName(), oldGeom.getNumParticles(), newType);
        }
        // copy appearance parameters
        newGeom.setVelocityAligned(oldGeom.isVelocityAligned());
        newGeom.setStartColor(oldGeom.getStartColor());
        newGeom.setEndColor(oldGeom.getEndColor());
        newGeom.setStartTexIndex(oldGeom.getStartTexIndex());
        newGeom.setStartSize(oldGeom.getStartSize());
        newGeom.setEndSize(oldGeom.getEndSize());
        newGeom.setStartMass(oldGeom.getStartMass());
        newGeom.setEndMass(oldGeom.getEndMass());
        newGeom.setStartSpin(oldGeom.getStartSpin());
        newGeom.setEndSpin(oldGeom.getEndSpin());
        newGeom.setRamp(oldGeom.getRamp());
        newGeom.setTexQuantity(oldGeom.getTexQuantity());

        // copy origin parameters
        newGeom.setTransform(oldGeom.getTransform());
        newGeom.setOriginOffset(oldGeom.getOriginOffset());
        newGeom.setParticleEmitter(oldGeom.getParticleEmitter());

        // copy emission parameters
        newGeom.setRotateWithScene(oldGeom.isRotateWithScene());
        newGeom.setEmissionDirection(oldGeom.getEmissionDirection());
        newGeom.setMinimumAngle(oldGeom.getMinimumAngle());
        newGeom.setMaximumAngle(oldGeom.getMaximumAngle());
        newGeom.setInitialVelocity(oldGeom.getInitialVelocity());

        // copy flow parameters
        newGeom.setControlFlow(oldGeom.getParticleController().isControlFlow());
        newGeom.setReleaseRate(oldGeom.getReleaseRate());
        newGeom.setReleaseVariance(oldGeom.getReleaseVariance());
        newGeom.setRepeatType(oldGeom.getParticleController().getRepeatType());

        // copy world parameters
        newGeom.setSpeed(oldGeom.getParticleController().getSpeed());
        newGeom.setMinimumLifeTime(oldGeom.getMinimumLifeTime());
        newGeom.setMaximumLifeTime(oldGeom.getMaximumLifeTime());
        newGeom.getParticleController().setPrecision(oldGeom.getParticleController().getPrecision());

        // copy influence parameters
        final List<ParticleInfluence> infs = oldGeom.getInfluences();
        if (infs != null) {
            for (final ParticleInfluence inf : infs) {
                newGeom.addInfluence(inf);
            }
        }

        // copy render states
        for (final StateType type : StateType.values) {
            final RenderState rs = oldGeom.getLocalRenderState(type);
            if (rs != null) {
                newGeom.setRenderState(rs);
            }
        }

        // warm up
        newGeom.warmUp(60);

        requestParticleSystemOverwrite(newGeom);
    }

    protected abstract void requestParticleSystemOverwrite(ParticleSystem newParticles);

    private void changeTexture() {
        try {
            final int result = _textureChooser.showOpenDialog(this);
            if (result == JFileChooser.CANCEL_OPTION) {
                return;
            }
            final File textFile = _textureChooser.getSelectedFile();
            _prefs.put("texture_dir", textFile.getParent());

            _newTexture = textFile;

            GameTaskQueueManager.getManager(ParticleEditorFrame.GLOBAL_CONTEXT).render(new Callable<Object>() {
                public Object call() throws Exception {
                    loadApplyTexture();
                    return null;
                }
            });

            final ImageIcon icon = new ImageIcon(getToolkit().createImage(textFile.getAbsolutePath()));
            _imageLabel.setIcon(icon);
            validate();
        } catch (final Exception ex) {
            logger.logp(Level.SEVERE, this.getClass().toString(), "changeTexture()", "Exception", ex);
        }
    }

    private void loadApplyTexture() throws MalformedURLException {
        final TextureState ts = (TextureState) getEdittedParticles().getLocalRenderState(StateType.Texture);
        // XXX: Needed?
        // TextureManager.clearCache();
        ts.setTexture(TextureManager.load(new URLResourceSource(_newTexture.toURI().toURL()),
                Texture.MinificationFilter.BilinearNearestMipMap, TextureStoreFormat.GuessCompressedFormat, true));
        ts.getTexture().setWrap(WrapMode.Clamp);
        ts.setEnabled(true);
        getEdittedParticles().setRenderState(ts);
        _newTexture = null;
    }

    private void updateBlendState(final boolean additive) {
        BlendState blend = (BlendState) getEdittedParticles().getLocalRenderState(StateType.Blend);
        if (blend == null) {
            blend = new BlendState();
            blend.setBlendEnabled(true);
            blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
            blend.setTestEnabled(true);
            blend.setTestFunction(BlendState.TestFunction.GreaterThan);
            getEdittedParticles().setRenderState(blend);
        }
        blend.setDestinationFunction(additive ? BlendState.DestinationFunction.One
                : BlendState.DestinationFunction.OneMinusSourceAlpha);
    }

    @Override
    public void updateWidgets() {
        updateRampModel();

        final ParticleSystem system = getEdittedParticles();
        _geomTypeBox.setSelectedItem(system.getParticleType());
        _velocityAlignedBox.setSelected(system.isVelocityAligned());
        _parentDirectionLeftPanel.setValue(system.getFacingLeftVector());
        _parentDirectionUpPanel.setValue(system.getFacingUpVector());
        _cameraAlignedBox.setSelected(system.isCameraFacing());
        _texPanel.setValue(system.getTexQuantity());
        _startTexPanel.setValue(system.getStartTexIndex());
        updateVisibleControls();

        final BlendState as = (BlendState) system.getLocalRenderState(StateType.Blend);
        _additiveBlendingBox.setSelected(as == null
                || as.getDestinationFunctionRGB() == BlendState.DestinationFunction.One);
        if (getTexturePanel().isVisible()) {
            Texture tex = null;
            try {
                tex = ((TextureState) system.getLocalRenderState(StateType.Texture)).getTexture();
                if (tex != null) {
                    if (tex.getTextureKey() != null && tex.getTextureKey().getSource() != null) {
                        _imageLabel.setIcon(new ImageIcon(((URLResourceSource) tex.getTextureKey().getSource())
                                .getURL()));
                    }
                } else {
                    _imageLabel.setIcon(null);
                }
            } catch (final Exception e) {
                logger.warning("image: " + tex + " : " + tex != null ? tex.getTextureKey().getSource().toString() : "");
            }
        }
    }

    public JCheckBox getAdditiveBlendingBox() {
        return _additiveBlendingBox;
    }

    public JPanel getTexturePanel() {
        return _texturePanel;
    }

    private void updateVisibleControls() {
        final boolean selected = _cameraAlignedBox.isSelected();
        _parentDirectionUpPanel.setVisible(!selected);
        _parentDirectionLeftPanel.setVisible(!selected);
    }

    public class StartRamp extends RampEntry {

        private final ParticleSystem particles;

        public StartRamp(final ParticleSystem particles) {
            super(-1);
            this.particles = particles;
            setColor(particles.getStartColor());
            setSize(particles.getStartSize());
            setMass(particles.getStartMass());
            setSpin(particles.getStartSpin());
        }

        @Override
        public String toString() {
            return "START: " + super.toString();
        }

        @Override
        public void setSize(final double size) {
            super.setSize(size);
            particles.setStartSize(size);
        }

        @Override
        public void setMass(final double mass) {
            super.setMass(mass);
            particles.setStartMass(mass);
        }

        @Override
        public void setSpin(final double spin) {
            super.setSpin(spin);
            particles.setStartSpin(spin);
        }

        @Override
        public void setColor(final ReadOnlyColorRGBA color) {
            super.setColor(color);
            particles.setStartColor(color);
        }
    }

    public class EndRamp extends RampEntry {

        private final ParticleSystem particles;

        public EndRamp(final ParticleSystem particles) {
            super(-1);
            this.particles = particles;
            setColor(particles.getEndColor());
            setSize(particles.getEndSize());
            setMass(particles.getEndMass());
            setSpin(particles.getEndSpin());
        }

        @Override
        public String toString() {
            return "END: " + super.toString();
        }

        @Override
        public void setSize(final double size) {
            super.setSize(size);
            particles.setEndSize(size);
        }

        @Override
        public void setMass(final double mass) {
            super.setMass(mass);
            particles.setEndMass(mass);
        }

        @Override
        public void setSpin(final double spin) {
            super.setSpin(spin);
            particles.setEndSpin(spin);
        }

        @Override
        public void setColor(final ReadOnlyColorRGBA color) {
            super.setColor(color);
            particles.setEndColor(color);
        }
    }
}
