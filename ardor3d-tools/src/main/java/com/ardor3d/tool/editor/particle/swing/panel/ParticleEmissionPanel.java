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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ardor3d.extension.effect.particle.emitter.LineSegmentEmitter;
import com.ardor3d.extension.effect.particle.emitter.PointEmitter;
import com.ardor3d.extension.effect.particle.emitter.RectangleEmitter;
import com.ardor3d.extension.effect.particle.emitter.RingEmitter;
import com.ardor3d.extension.effect.particle.emitter.SavableParticleEmitter;
import com.ardor3d.math.LineSegment3;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Rectangle3;
import com.ardor3d.math.Ring;
import com.ardor3d.math.Vector3;
import com.ardor3d.tool.editor.swing.widget.SphericalUnitVectorPanel;
import com.ardor3d.tool.editor.swing.widget.ValuePanel;
import com.ardor3d.tool.editor.swing.widget.Vector3Panel;

public class ParticleEmissionPanel extends ParticleEditPanel {
    private static final long serialVersionUID = 1L;

    enum EmitTypes {
        Point, LineSegment, Rectangle, Ring;
    }

    private final JCheckBox _rotateWithEmitterBox;
    private final SphericalUnitVectorPanel _directionPanel = new SphericalUnitVectorPanel();
    private final ValuePanel _minAnglePanel = new ValuePanel("Min Degrees Off Dir.: ", "", 0.0, 360.0, 1.0);
    private final ValuePanel _maxAnglePanel = new ValuePanel("Max Degrees Off Dir.: ", "", 0.0, 360.0, 1.0);
    private final ValuePanel _velocityPanel = new ValuePanel("Initial Velocity: ", "", 0.0, Double.MAX_VALUE, 0.001);

    private final Vector3Panel _translationPanel = new Vector3Panel(-Double.MAX_VALUE, Double.MAX_VALUE, 1f);
    private final Vector3Panel _rotationPanel = new Vector3Panel(-180f, 180f, 1f);
    private final ValuePanel _scalePanel = new ValuePanel("System Scale: ", " ", 0f, Double.MAX_VALUE, 0.01f);
    private final JComboBox<EmitTypes> _originTypeBox;
    private final JPanel _originParamsPanel;
    private final JPanel _pointParamsPanel;
    private final JPanel _lineParamsPanel;
    private final ValuePanel _lineLengthPanel = new ValuePanel("Length: ", "", 0f, Double.MAX_VALUE, 1f);
    private final JPanel _rectParamsPanel;
    private final ValuePanel _rectWidthPanel = new ValuePanel("Width: ", "", 0f, Double.MAX_VALUE, 1f);
    private final ValuePanel _rectHeightPanel = new ValuePanel("Height: ", "", 0f, Double.MAX_VALUE, 1f);
    private final JPanel _ringParamsPanel;
    private final ValuePanel _ringInnerPanel = new ValuePanel("Inner Radius: ", "", 0f, Double.MAX_VALUE, 1f);
    private final ValuePanel _ringOuterPanel = new ValuePanel("Outer Radius: ", "", 0f, Double.MAX_VALUE, 1f);

    public ParticleEmissionPanel() {
        super();
        setLayout(new GridBagLayout());

        _rotateWithEmitterBox = new JCheckBox(new AbstractAction("Rotate With Emitter") {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(final ActionEvent e) {
                getEdittedParticles().setRotateWithScene(_rotateWithEmitterBox.isSelected());
            }
        });
        _rotateWithEmitterBox.setFont(new Font("Arial", Font.BOLD, 12));

        _directionPanel.setBorder(createTitledBorder("DIRECTION"));
        _directionPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                if (getEdittedParticles() != null) {
                    getEdittedParticles().getEmissionDirection().set(_directionPanel.getValue());
                    getEdittedParticles().updateRotationMatrix();
                }
            }
        });
        _directionPanel.add(_rotateWithEmitterBox, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        _minAnglePanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setMinimumAngle(_minAnglePanel.getDoubleValue() * MathUtils.DEG_TO_RAD);
            }
        });
        _maxAnglePanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setMaximumAngle(_maxAnglePanel.getDoubleValue() * MathUtils.DEG_TO_RAD);
            }
        });
        final JPanel anglePanel = new JPanel(new GridBagLayout());
        anglePanel.setBorder(createTitledBorder("ANGLE"));
        anglePanel.add(_minAnglePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
        anglePanel.add(_maxAnglePanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));

        _velocityPanel.setBorder(createTitledBorder("VELOCITY"));
        _velocityPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setInitialVelocity(_velocityPanel.getDoubleValue());
            }
        });

        add(_directionPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 5, 5, 5), 0, 0));
        add(anglePanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));
        add(_velocityPanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 5, 10, 5), 0, 0));

        final JPanel transformPanel = new JPanel(new GridBagLayout());
        transformPanel.setBorder(createTitledBorder(" EMITTER TRANSFORM "));

        _translationPanel.setBorder(createTitledBorder(" TRANSLATION "));
        _translationPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setTranslation(_translationPanel.getValue());
            }
        });

        _rotationPanel.setBorder(createTitledBorder(" ROTATION "));
        _rotationPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final Vector3 val = _rotationPanel.getValue().multiplyLocal(MathUtils.DEG_TO_RAD);
                final Matrix3 mat = Matrix3.fetchTempInstance().fromAngles(val.getX(), val.getY(), val.getZ());
                getEdittedParticles().setRotation(mat);
                Matrix3.releaseTempInstance(mat);
            }
        });

        _scalePanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                getEdittedParticles().setScale(_scalePanel.getDoubleValue());
            }
        });

        transformPanel.add(_translationPanel, new GridBagConstraints(0, 0, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        transformPanel.add(_rotationPanel, new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        transformPanel.add(_scalePanel, new GridBagConstraints(0, 2, 2, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        _originTypeBox = new JComboBox<EmitTypes>(EmitTypes.values());
        _originTypeBox.setBorder(createTitledBorder(" EMITTER TYPE "));
        _originTypeBox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                updateOriginParams();
            }
        });

        _originParamsPanel = new JPanel(new BorderLayout());

        _pointParamsPanel = createPointParamsPanel();
        _lineParamsPanel = createLineParamsPanel();
        _rectParamsPanel = createRectParamsPanel();
        _ringParamsPanel = createRingParamsPanel();

        add(transformPanel, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0));
        add(_originTypeBox, new GridBagConstraints(0, 4, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0));
        add(_originParamsPanel, new GridBagConstraints(0, 5, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 5), 0, 0));
    }

    private JPanel createPointParamsPanel() {
        return new JPanel();
    }

    private JPanel createLineParamsPanel() {
        _lineLengthPanel.setBorder(createTitledBorder(" LINE PARAMETERS "));
        _lineLengthPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final LineSegment3 line = ((LineSegmentEmitter) getEdittedParticles().getParticleEmitter()).getSource();
                final double val = _lineLengthPanel.getDoubleValue();
                line.setExtent(val / 2.0);
            }
        });
        return _lineLengthPanel;
    }

    private JPanel createRectParamsPanel() {
        _rectWidthPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final Rectangle3 rect = ((RectangleEmitter) getEdittedParticles().getParticleEmitter()).getSource();
                final double width = _rectWidthPanel.getDoubleValue();
                final Vector3 helper = Vector3.fetchTempInstance();
                helper.set(rect.getA()).setX(-width / 2.0);
                rect.setA(helper);
                helper.set(rect.getB()).setX(width / 2.0);
                rect.setB(helper);
                helper.set(rect.getC()).setX(-width / 2.0);
                rect.setC(helper);
                Vector3.releaseTempInstance(helper);
            }
        });
        _rectHeightPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final Rectangle3 rect = ((RectangleEmitter) getEdittedParticles().getParticleEmitter()).getSource();
                final double height = _rectHeightPanel.getDoubleValue();
                final Vector3 helper = Vector3.fetchTempInstance();
                helper.set(rect.getA()).setZ(-height / 2.0);
                rect.setA(helper);
                helper.set(rect.getB()).setZ(-height / 2.0);
                rect.setB(helper);
                helper.set(rect.getC()).setZ(height / 2.0);
                rect.setC(helper);
                Vector3.releaseTempInstance(helper);
            }
        });

        final JPanel rectParamsPanel = new JPanel(new GridBagLayout());
        rectParamsPanel.setBorder(createTitledBorder(" RECTANGLE PARAMETERS "));
        rectParamsPanel.add(_rectWidthPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
        rectParamsPanel.add(_rectHeightPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
        return rectParamsPanel;
    }

    private JPanel createRingParamsPanel() {
        _ringInnerPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final Ring ring = ((RingEmitter) getEdittedParticles().getParticleEmitter()).getSource();
                ring.setInnerRadius(_ringInnerPanel.getDoubleValue());
            }
        });
        _ringOuterPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                final Ring ring = ((RingEmitter) getEdittedParticles().getParticleEmitter()).getSource();
                ring.setOuterRadius(_ringOuterPanel.getDoubleValue());
            }
        });

        final JPanel ringParamsPanel = new JPanel(new GridBagLayout());
        ringParamsPanel.setBorder(createTitledBorder(" RING PARAMETERS "));
        ringParamsPanel.add(_ringInnerPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
        ringParamsPanel.add(_ringOuterPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 10), 0, 0));
        return ringParamsPanel;
    }

    /**
     * updateOriginParams
     */
    private void updateOriginParams() {
        _originParamsPanel.removeAll();
        final EmitTypes type = (EmitTypes) _originTypeBox.getSelectedItem();
        SavableParticleEmitter emitter = getEdittedParticles().getParticleEmitter();
        switch (type) {
            case Point:
                _originParamsPanel.add(_pointParamsPanel);
                if (!(emitter instanceof PointEmitter)) {
                    getEdittedParticles().setParticleEmitter(new PointEmitter());
                }
                break;
            case LineSegment:
                if (!(emitter instanceof LineSegmentEmitter)) {
                    emitter = new LineSegmentEmitter();
                    getEdittedParticles().setParticleEmitter(emitter);
                }
                final LineSegment3 line = ((LineSegmentEmitter) emitter).getSource();
                _lineLengthPanel.setValue(line.getExtent() * 2);
                _originParamsPanel.add(_lineParamsPanel);
                break;
            case Rectangle:
                if (!(emitter instanceof RectangleEmitter)) {
                    emitter = new RectangleEmitter();
                    getEdittedParticles().setParticleEmitter(emitter);
                }
                final Rectangle3 rect = ((RectangleEmitter) emitter).getSource();
                _rectWidthPanel.setValue(rect.getA().distance(rect.getB()));
                _rectHeightPanel.setValue(rect.getA().distance(rect.getC()));
                _originParamsPanel.add(_rectParamsPanel);
                break;
            case Ring:
                if (!(emitter instanceof RingEmitter)) {
                    emitter = new RingEmitter();
                    getEdittedParticles().setParticleEmitter(emitter);
                }
                final Ring ring = ((RingEmitter) emitter).getSource();
                _ringInnerPanel.setValue(ring.getInnerRadius());
                _ringOuterPanel.setValue(ring.getOuterRadius());
                _originParamsPanel.add(_ringParamsPanel);
                break;
        }

        _originParamsPanel.getParent().validate();
        _originParamsPanel.getParent().repaint();
    }

    @Override
    public void updateWidgets() {
        _rotateWithEmitterBox.setSelected(getEdittedParticles().isRotateWithScene());
        _directionPanel.setValue(getEdittedParticles().getEmissionDirection());
        _minAnglePanel.setValue(getEdittedParticles().getMinimumAngle() * MathUtils.RAD_TO_DEG);
        _maxAnglePanel.setValue(getEdittedParticles().getMaximumAngle() * MathUtils.RAD_TO_DEG);
        _velocityPanel.setValue(getEdittedParticles().getInitialVelocity());

        _translationPanel.setValue(getEdittedParticles().getTranslation());
        final double[] angles = getEdittedParticles().getRotation().toAngles(null);
        _rotationPanel.setValue(new Vector3(angles[0], angles[1], angles[2]).multiplyLocal(MathUtils.RAD_TO_DEG));
        _scalePanel.setValue(getEdittedParticles().getScale().getX());

        final SavableParticleEmitter emitter = getEdittedParticles().getParticleEmitter();
        if (emitter instanceof RingEmitter) {
            _originTypeBox.setSelectedItem(EmitTypes.Ring);
        } else if (emitter instanceof RectangleEmitter) {
            _originTypeBox.setSelectedItem(EmitTypes.Rectangle);
        } else if (emitter instanceof LineSegmentEmitter) {
            _originTypeBox.setSelectedItem(EmitTypes.LineSegment);
        } else {
            _originTypeBox.setSelectedItem(EmitTypes.Point);
        }

        updateOriginParams();
    }
}
