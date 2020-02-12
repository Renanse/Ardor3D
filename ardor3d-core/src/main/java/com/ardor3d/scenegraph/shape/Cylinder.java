/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.shape;

import java.io.IOException;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Cylinder</code> provides an extension of <code>Mesh</code>. A <code>Cylinder</code> is defined by a height and
 * radius. The center of the Cylinder is the origin.
 */
public class Cylinder extends Mesh {

    private int _axisSamples;

    private int _radialSamples;

    private double _radius;
    private double _radius2;

    private double _height;
    private boolean _closed;
    private boolean _inverted;

    public Cylinder() {}

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a higher sample number creates a better
     * looking cylinder, but at the cost of more vertex information.
     * 
     * @param name
     *            The name of this Cylinder.
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     */
    public Cylinder(final String name, final int axisSamples, final int radialSamples, final double radius,
            final double height) {
        this(name, axisSamples, radialSamples, radius, height, false);
    }

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a higher sample number creates a better
     * looking cylinder, but at the cost of more vertex information. <br>
     * If the cylinder is closed the texture is split into axisSamples parts: top most and bottom most part is used for
     * top and bottom of the cylinder, rest of the texture for the cylinder wall. The middle of the top is mapped to
     * texture coordinates (0.5, 1), bottom to (0.5, 0). Thus you need a suited distorted texture.
     * 
     * @param name
     *            The name of this Cylinder.
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     * @param closed
     *            true to create a cylinder with top and bottom surface
     */
    public Cylinder(final String name, final int axisSamples, final int radialSamples, final double radius,
            final double height, final boolean closed) {
        this(name, axisSamples, radialSamples, radius, height, closed, false);
    }

    /**
     * Creates a new Cylinder. By default its center is the origin. Usually, a higher sample number creates a better
     * looking cylinder, but at the cost of more vertex information. <br>
     * If the cylinder is closed the texture is split into axisSamples parts: top most and bottom most part is used for
     * top and bottom of the cylinder, rest of the texture for the cylinder wall. The middle of the top is mapped to
     * texture coordinates (0.5, 1), bottom to (0.5, 0). Thus you need a suited distorted texture.
     * 
     * @param name
     *            The name of this Cylinder.
     * @param axisSamples
     *            Number of triangle samples along the axis.
     * @param radialSamples
     *            Number of triangle samples along the radial.
     * @param radius
     *            The radius of the cylinder.
     * @param height
     *            The cylinder's height.
     * @param closed
     *            true to create a cylinder with top and bottom surface
     * @param inverted
     *            true to create a cylinder that is meant to be viewed from the interior.
     */
    public Cylinder(final String name, final int axisSamples, final int radialSamples, final double radius,
            final double height, final boolean closed, final boolean inverted) {

        super(name);

        _axisSamples = axisSamples + (closed ? 2 : 0);
        _radialSamples = radialSamples;
        setRadius(radius);
        _height = height;
        _closed = closed;
        _inverted = inverted;

        allocateVertices();
    }

    /**
     * @return Returns the height.
     */
    public double getHeight() {
        return _height;
    }

    /**
     * @param height
     *            The height to set.
     */
    public void setHeight(final double height) {
        _height = height;
        allocateVertices();
    }

    /**
     * @return Returns the radius.
     */
    public double getRadius() {
        return _radius;
    }

    /**
     * Change the radius of this cylinder. This resets any second radius.
     * 
     * @param radius
     *            The radius to set.
     */
    public void setRadius(final double radius) {
        _radius = radius;
        _radius2 = radius;
        allocateVertices();
    }

    /**
     * Set the top radius of the 'cylinder' to differ from the bottom radius.
     * 
     * @param radius
     *            The first radius to set.
     * @see com.ardor3d.extension.shape.Cone
     */
    public void setRadius1(final double radius) {
        _radius = radius;
        allocateVertices();
    }

    /**
     * Set the bottom radius of the 'cylinder' to differ from the top radius. This makes the Mesh be a frustum of
     * pyramid, or if set to 0, a cone.
     * 
     * @param radius
     *            The second radius to set.
     * @see com.ardor3d.extension.shape.Cone
     */
    public void setRadius2(final double radius) {
        _radius2 = radius;
        allocateVertices();
    }

    /**
     * @return the number of samples along the cylinder axis
     */
    public int getAxisSamples() {
        return _axisSamples;
    }

    /**
     * @return true if end caps are used.
     */
    public boolean isClosed() {
        return _closed;
    }

    /**
     * @return true if normals and uvs are created for interior use
     */
    public boolean isInverted() {
        return _inverted;
    }

    /**
     * @return number of samples around cylinder
     */
    public int getRadialSamples() {
        return _radialSamples;
    }

    private void allocateVertices() {
        // allocate vertices
        final int verts = _axisSamples * (_radialSamples + 1) + (_closed ? 2 : 0);
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), verts));

        // allocate normals if requested
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(_meshData.getNormalBuffer(), verts));

        // allocate texture coordinates
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(verts), 0);

        final int count = ((_closed ? 2 : 0) + 2 * (_axisSamples - 1)) * _radialSamples;

        if (_meshData.getIndices() == null || _meshData.getIndices().getBufferLimit() != 3 * count) {
            _meshData.setIndices(BufferUtils.createIndexBufferData(3 * count, verts - 1));
        }

        setGeometryData();
        setIndexData();
    }

    private void setGeometryData() {
        // generate geometry
        final double inverseRadial = 1.0 / _radialSamples;
        final double inverseAxisLess = 1.0 / (_closed ? _axisSamples - 3 : _axisSamples - 1);
        final double inverseAxisLessTexture = 1.0 / (_axisSamples - 1);
        final double halfHeight = 0.5 * _height;

        // Generate points on the unit circle to be used in computing the mesh
        // points on a cylinder slice.
        final double[] sin = new double[_radialSamples + 1];
        final double[] cos = new double[_radialSamples + 1];

        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            final double angle = MathUtils.TWO_PI * inverseRadial * radialCount;
            cos[radialCount] = MathUtils.cos(angle);
            sin[radialCount] = MathUtils.sin(angle);
        }
        sin[_radialSamples] = sin[0];
        cos[_radialSamples] = cos[0];

        // generate the cylinder itself
        final Vector3 tempNormal = new Vector3();
        for (int axisCount = 0, i = 0; axisCount < _axisSamples; axisCount++) {
            double axisFraction;
            double axisFractionTexture;
            int topBottom = 0;
            if (!_closed) {
                axisFraction = axisCount * inverseAxisLess; // in [0,1]
                axisFractionTexture = axisFraction;
            } else {
                if (axisCount == 0) {
                    topBottom = -1; // bottom
                    axisFraction = 0;
                    axisFractionTexture = inverseAxisLessTexture;
                } else if (axisCount == _axisSamples - 1) {
                    topBottom = 1; // top
                    axisFraction = 1;
                    axisFractionTexture = 1 - inverseAxisLessTexture;
                } else {
                    axisFraction = (axisCount - 1) * inverseAxisLess;
                    axisFractionTexture = axisCount * inverseAxisLessTexture;
                }
            }
            final double z = -halfHeight + _height * axisFraction;

            // compute center of slice
            final Vector3 sliceCenter = new Vector3(0, 0, z);

            // compute slice vertices with duplication at end point
            final int save = i;
            for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
                final double radialFraction = radialCount * inverseRadial; // in [0,1)
                tempNormal.set(cos[radialCount], sin[radialCount], 0);
                if (topBottom == 0) {
                    if (!_inverted) {
                        _meshData.getNormalBuffer().put(tempNormal.getXf()).put(tempNormal.getYf())
                                .put(tempNormal.getZf());
                    } else {
                        _meshData.getNormalBuffer().put(-tempNormal.getXf()).put(-tempNormal.getYf())
                                .put(-tempNormal.getZf());
                    }
                } else {
                    _meshData.getNormalBuffer().put(0).put(0).put(topBottom * (_inverted ? -1 : 1));
                }

                tempNormal.multiplyLocal((_radius - _radius2) * axisFraction + _radius2).addLocal(sliceCenter);
                _meshData.getVertexBuffer().put(tempNormal.getXf()).put(tempNormal.getYf()).put(tempNormal.getZf());

                _meshData.getTextureCoords(0).getBuffer()
                        .put((float) (_inverted ? 1 - radialFraction : radialFraction))
                        .put((float) axisFractionTexture);
                i++;
            }

            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), save, i);
            BufferUtils.copyInternalVector3(_meshData.getNormalBuffer(), save, i);

            _meshData.getTextureCoords(0).getBuffer().put((_inverted ? 0.0f : 1.0f)).put((float) axisFractionTexture);

            i++;
        }

        if (_closed) {
            _meshData.getVertexBuffer().put(0).put(0).put((float) -halfHeight); // bottom center
            _meshData.getNormalBuffer().put(0).put(0).put(-1 * (_inverted ? -1 : 1));
            _meshData.getTextureCoords(0).getBuffer().put(0.5f).put(0);
            _meshData.getVertexBuffer().put(0).put(0).put((float) halfHeight); // top center
            _meshData.getNormalBuffer().put(0).put(0).put(1 * (_inverted ? -1 : 1));
            _meshData.getTextureCoords(0).getBuffer().put(0.5f).put(1);
        }
    }

    private void setIndexData() {
        _meshData.getIndices().rewind();

        // generate connectivity
        for (int axisCount = 0, axisStart = 0; axisCount < _axisSamples - 1; axisCount++) {
            int i0 = axisStart;
            int i1 = i0 + 1;
            axisStart += _radialSamples + 1;
            int i2 = axisStart;
            int i3 = i2 + 1;
            for (int i = 0; i < _radialSamples; i++) {
                if (_closed && axisCount == 0) {
                    if (!_inverted) {
                        _meshData.getIndices().put(i0++);
                        _meshData.getIndices().put(_meshData.getVertexCount() - 2);
                        _meshData.getIndices().put(i1++);
                    } else {
                        _meshData.getIndices().put(i0++);
                        _meshData.getIndices().put(i1++);
                        _meshData.getIndices().put(_meshData.getVertexCount() - 2);
                    }
                } else if (_closed && axisCount == _axisSamples - 2) {
                    if (!_inverted) {
                        _meshData.getIndices().put(i2++);
                        _meshData.getIndices().put(i3++);
                        _meshData.getIndices().put(_meshData.getVertexCount() - 1);
                    } else {
                        _meshData.getIndices().put(i2++);
                        _meshData.getIndices().put(_meshData.getVertexCount() - 1);
                        _meshData.getIndices().put(i3++);
                    }
                } else {
                    if (!_inverted) {
                        _meshData.getIndices().put(i0++);
                        _meshData.getIndices().put(i1);
                        _meshData.getIndices().put(i2);
                        _meshData.getIndices().put(i1++);
                        _meshData.getIndices().put(i3++);
                        _meshData.getIndices().put(i2++);
                    } else {
                        _meshData.getIndices().put(i0++);
                        _meshData.getIndices().put(i2);
                        _meshData.getIndices().put(i1);
                        _meshData.getIndices().put(i1++);
                        _meshData.getIndices().put(i2++);
                        _meshData.getIndices().put(i3++);
                    }
                }
            }
        }
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_axisSamples, "axisSamples", 0);
        capsule.write(_radialSamples, "radialSamples", 0);
        capsule.write(_radius, "radius", 0);
        capsule.write(_radius2, "radius2", 0);
        capsule.write(_height, "height", 0);
        capsule.write(_closed, "closed", false);
        capsule.write(_inverted, "inverted", false);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _axisSamples = capsule.readInt("axisSamples", 0);
        _radialSamples = capsule.readInt("radialSamples", 0);
        _radius = capsule.readDouble("radius", 0);
        _radius2 = capsule.readDouble("radius2", 0);
        _height = capsule.readDouble("height", 0);
        _closed = capsule.readBoolean("closed", false);
        _inverted = capsule.readBoolean("inverted", false);
    }
}
