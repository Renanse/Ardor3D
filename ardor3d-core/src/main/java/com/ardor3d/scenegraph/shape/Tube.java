/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;

import com.ardor3d.math.MathUtils;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

public class Tube extends Mesh {

    private int _axisSamples;
    private int _radialSamples;

    private double _outerRadius;
    private double _innerRadius;
    private double _height;

    protected boolean _viewInside;

    /**
     * Constructor meant for Savable use only.
     */
    public Tube() {}

    public Tube(final String name, final double outerRadius, final double innerRadius, final double height,
            final int axisSamples, final int radialSamples) {
        super(name);
        _outerRadius = outerRadius;
        _innerRadius = innerRadius;
        _height = height;
        _axisSamples = axisSamples;
        _radialSamples = radialSamples;
        allocateVertices();
    }

    public Tube(final String name, final double outerRadius, final double innerRadius, final double height) {
        this(name, outerRadius, innerRadius, height, 2, 20);
    }

    private void allocateVertices() {
        final int verts = (2 * (_axisSamples + 1) * (_radialSamples + 1) + _radialSamples * 4);
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), verts));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(_meshData.getNormalBuffer(), verts));
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(_meshData.getTextureBuffer(0), verts), 0);

        final int tris = (4 * _radialSamples * (1 + _axisSamples));
        if (_meshData.getIndices() == null || _meshData.getIndices().getBufferLimit() != 3 * tris) {
            _meshData.setIndices(BufferUtils.createIndexBufferData(3 * tris, verts - 1));
        }

        setGeometryData();
        setIndexData();
    }

    public int getAxisSamples() {
        return _axisSamples;
    }

    public void setAxisSamples(final int axisSamples) {
        _axisSamples = axisSamples;
        allocateVertices();
    }

    public int getRadialSamples() {
        return _radialSamples;
    }

    public void setRadialSamples(final int radialSamples) {
        _radialSamples = radialSamples;
        allocateVertices();
    }

    public double getOuterRadius() {
        return _outerRadius;
    }

    public void setOuterRadius(final double outerRadius) {
        _outerRadius = outerRadius;
        allocateVertices();
    }

    public double getInnerRadius() {
        return _innerRadius;
    }

    public void setInnerRadius(final double innerRadius) {
        _innerRadius = innerRadius;
        allocateVertices();
    }

    public double getHeight() {
        return _height;
    }

    public void setHeight(final double height) {
        _height = height;
        allocateVertices();
    }

    private void setGeometryData() {
        _meshData.getVertexBuffer().rewind();
        _meshData.getNormalBuffer().rewind();
        _meshData.getTextureCoords(0).getBuffer().rewind();

        final double inverseRadial = 1.0 / _radialSamples;
        final double axisStep = _height / _axisSamples;
        final double axisTextureStep = 1.0 / _axisSamples;
        final double halfHeight = 0.5 * _height;
        final double innerOuterRatio = _innerRadius / _outerRadius;
        final double[] sin = new double[_radialSamples];
        final double[] cos = new double[_radialSamples];

        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            final double angle = MathUtils.TWO_PI * inverseRadial * radialCount;
            cos[radialCount] = MathUtils.cos(angle);
            sin[radialCount] = MathUtils.sin(angle);
        }

        // outer cylinder
        for (int radialCount = 0; radialCount < _radialSamples + 1; radialCount++) {
            for (int axisCount = 0; axisCount < _axisSamples + 1; axisCount++) {
                _meshData.getVertexBuffer().put((float) (cos[radialCount % _radialSamples] * _outerRadius))
                        .put((float) (axisStep * axisCount - halfHeight))
                        .put((float) (sin[radialCount % _radialSamples] * _outerRadius));
                if (_viewInside) {
                    _meshData.getNormalBuffer().put((float) cos[radialCount % _radialSamples]).put(0)
                            .put((float) sin[radialCount % _radialSamples]);
                } else {
                    _meshData.getNormalBuffer().put((float) -cos[radialCount % _radialSamples]).put(0)
                            .put((float) -sin[radialCount % _radialSamples]);
                }
                _meshData.getTextureCoords(0).getBuffer().put((float) (radialCount * inverseRadial))
                        .put((float) (axisTextureStep * axisCount));
            }
        }
        // inner cylinder
        for (int radialCount = 0; radialCount < _radialSamples + 1; radialCount++) {
            for (int axisCount = 0; axisCount < _axisSamples + 1; axisCount++) {
                _meshData.getVertexBuffer().put((float) (cos[radialCount % _radialSamples] * _innerRadius))
                        .put((float) (axisStep * axisCount - halfHeight))
                        .put((float) (sin[radialCount % _radialSamples] * _innerRadius));
                if (_viewInside) {
                    _meshData.getNormalBuffer().put((float) -cos[radialCount % _radialSamples]).put(0)
                            .put((float) -sin[radialCount % _radialSamples]);
                } else {
                    _meshData.getNormalBuffer().put((float) cos[radialCount % _radialSamples]).put(0)
                            .put((float) sin[radialCount % _radialSamples]);
                }
                _meshData.getTextureCoords(0).getBuffer().put((float) (radialCount * inverseRadial))
                        .put((float) (axisTextureStep * axisCount));
            }
        }
        // bottom edge
        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            _meshData.getVertexBuffer().put((float) (cos[radialCount] * _outerRadius)).put((float) -halfHeight)
                    .put((float) (sin[radialCount] * _outerRadius));
            _meshData.getVertexBuffer().put((float) (cos[radialCount] * _innerRadius)).put((float) -halfHeight)
                    .put((float) (sin[radialCount] * _innerRadius));
            if (_viewInside) {
                _meshData.getNormalBuffer().put(0).put(1).put(0);
                _meshData.getNormalBuffer().put(0).put(1).put(0);
            } else {
                _meshData.getNormalBuffer().put(0).put(-1).put(0);
                _meshData.getNormalBuffer().put(0).put(-1).put(0);
            }
            _meshData.getTextureCoords(0).getBuffer().put((float) (0.5 + 0.5 * cos[radialCount]))
                    .put((float) (0.5 + 0.5 * sin[radialCount]));
            _meshData.getTextureCoords(0).getBuffer().put((float) (0.5 + innerOuterRatio * 0.5 * cos[radialCount]))
                    .put((float) (0.5 + innerOuterRatio * 0.5 * sin[radialCount]));
        }
        // top edge
        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            _meshData.getVertexBuffer().put((float) (cos[radialCount] * _outerRadius)).put((float) halfHeight)
                    .put((float) (sin[radialCount] * _outerRadius));
            _meshData.getVertexBuffer().put((float) (cos[radialCount] * _innerRadius)).put((float) halfHeight)
                    .put((float) (sin[radialCount] * _innerRadius));
            if (_viewInside) {
                _meshData.getNormalBuffer().put(0).put(-1).put(0);
                _meshData.getNormalBuffer().put(0).put(-1).put(0);
            } else {
                _meshData.getNormalBuffer().put(0).put(1).put(0);
                _meshData.getNormalBuffer().put(0).put(1).put(0);
            }
            _meshData.getTextureCoords(0).getBuffer().put((float) (0.5 + 0.5 * cos[radialCount]))
                    .put((float) (0.5 + 0.5 * sin[radialCount]));
            _meshData.getTextureCoords(0).getBuffer().put((float) (0.5 + innerOuterRatio * 0.5 * cos[radialCount]))
                    .put((float) (0.5 + innerOuterRatio * 0.5 * sin[radialCount]));
        }

    }

    private void setIndexData() {
        _meshData.getIndices().rewind();

        final int outerCylinder = (_axisSamples + 1) * (_radialSamples + 1);
        final int bottomEdge = 2 * outerCylinder;
        final int topEdge = bottomEdge + 2 * _radialSamples;
        // inner cylinder
        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            for (int axisCount = 0; axisCount < _axisSamples; axisCount++) {
                final int index0 = axisCount + (_axisSamples + 1) * radialCount;
                final int index1 = index0 + 1;
                final int index2 = index0 + (_axisSamples + 1);
                final int index3 = index2 + 1;
                if (_viewInside) {
                    _meshData.getIndices().put(index0).put(index1).put(index2);
                    _meshData.getIndices().put(index1).put(index3).put(index2);
                } else {
                    _meshData.getIndices().put(index0).put(index2).put(index1);
                    _meshData.getIndices().put(index1).put(index2).put(index3);
                }
            }
        }

        // outer cylinder
        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            for (int axisCount = 0; axisCount < _axisSamples; axisCount++) {
                final int index0 = outerCylinder + axisCount + (_axisSamples + 1) * radialCount;
                final int index1 = index0 + 1;
                final int index2 = index0 + (_axisSamples + 1);
                final int index3 = index2 + 1;
                if (_viewInside) {
                    _meshData.getIndices().put(index0).put(index2).put(index1);
                    _meshData.getIndices().put(index1).put(index2).put(index3);
                } else {
                    _meshData.getIndices().put(index0).put(index1).put(index2);
                    _meshData.getIndices().put(index1).put(index3).put(index2);
                }
            }
        }

        // bottom edge
        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            final int index0 = bottomEdge + 2 * radialCount;
            final int index1 = index0 + 1;
            final int index2 = bottomEdge + 2 * ((radialCount + 1) % _radialSamples);
            final int index3 = index2 + 1;
            if (_viewInside) {
                _meshData.getIndices().put(index0).put(index2).put(index1);
                _meshData.getIndices().put(index1).put(index2).put(index3);
            } else {
                _meshData.getIndices().put(index0).put(index1).put(index2);
                _meshData.getIndices().put(index1).put(index3).put(index2);
            }
        }

        // top edge
        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            final int index0 = topEdge + 2 * radialCount;
            final int index1 = index0 + 1;
            final int index2 = topEdge + 2 * ((radialCount + 1) % _radialSamples);
            final int index3 = index2 + 1;
            if (_viewInside) {
                _meshData.getIndices().put(index0).put(index1).put(index2);
                _meshData.getIndices().put(index1).put(index3).put(index2);
            } else {
                _meshData.getIndices().put(index0).put(index2).put(index1);
                _meshData.getIndices().put(index1).put(index2).put(index3);
            }
        }
    }

    /**
     * 
     * @return true if the normals are inverted to point into the torus so that the face is oriented for a viewer inside
     *         the torus. false (the default) for exterior viewing.
     */
    public boolean isViewFromInside() {
        return _viewInside;
    }

    /**
     * 
     * @param viewInside
     *            if true, the normals are inverted to point into the torus so that the face is oriented for a viewer
     *            inside the torus. Default is false (for outside viewing)
     */
    public void setViewFromInside(final boolean viewInside) {
        if (viewInside != _viewInside) {
            _viewInside = viewInside;
            setGeometryData();
            setIndexData();
        }
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(getAxisSamples(), "axisSamples", 0);
        capsule.write(getRadialSamples(), "radialSamples", 0);
        capsule.write(getOuterRadius(), "outerRadius", 0);
        capsule.write(getInnerRadius(), "innerRadius", 0);
        capsule.write(getHeight(), "height", 0);
        capsule.write(_viewInside, "viewInside", false);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        setAxisSamples(capsule.readInt("axisSamples", 0));
        setRadialSamples(capsule.readInt("radialSamples", 0));
        setOuterRadius(capsule.readDouble("outerRadius", 0));
        setInnerRadius(capsule.readDouble("innerRadius", 0));
        setHeight(capsule.readDouble("height", 0));
        _viewInside = capsule.readBoolean("viewInside", false);
    }
}