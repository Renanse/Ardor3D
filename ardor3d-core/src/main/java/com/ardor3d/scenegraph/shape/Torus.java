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

public class Torus extends Mesh {

    protected int _circleSamples;

    protected int _radialSamples;

    protected double _tubeRadius;

    protected double _centerRadius;

    protected boolean _viewInside;

    /**
     * private constructor for Savable use only.
     */
    public Torus() {

    }

    /**
     * Constructs a new Torus. Center is the origin, but the Torus may be transformed.
     * 
     * @param name
     *            The name of the Torus.
     * @param circleSamples
     *            The number of samples along the circles.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param tubeRadius
     *            the radius of the torus tube.
     * @param centerRadius
     *            The distance from the center of the torus hole to the center of the torus tube.
     */
    public Torus(final String name, final int circleSamples, final int radialSamples, final double tubeRadius,
            final double centerRadius) {

        super(name);
        _circleSamples = circleSamples;
        _radialSamples = radialSamples;
        _tubeRadius = tubeRadius;
        _centerRadius = centerRadius;

        setGeometryData();
        setIndexData();

    }

    private void setGeometryData() {
        // allocate vertices
        final int verts = ((_circleSamples + 1) * (_radialSamples + 1));
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate normals if requested
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate texture coordinates
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(verts), 0);

        // generate geometry
        final double inverseCircleSamples = 1.0 / _circleSamples;
        final double inverseRadialSamples = 1.0 / _radialSamples;
        int i = 0;
        // generate the cylinder itself
        final Vector3 radialAxis = new Vector3(), torusMiddle = new Vector3(), tempNormal = new Vector3();
        for (int circleCount = 0; circleCount < _circleSamples; circleCount++) {
            // compute center point on torus circle at specified angle
            final double circleFraction = circleCount * inverseCircleSamples;
            final double theta = MathUtils.TWO_PI * circleFraction;
            final double cosTheta = MathUtils.cos(theta);
            final double sinTheta = MathUtils.sin(theta);
            radialAxis.set(cosTheta, sinTheta, 0);
            radialAxis.multiply(_centerRadius, torusMiddle);

            // compute slice vertices with duplication at end point
            final int iSave = i;
            for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
                final double radialFraction = radialCount * inverseRadialSamples;
                // in [0,1)
                final double phi = MathUtils.TWO_PI * radialFraction;
                final double cosPhi = MathUtils.cos(phi);
                final double sinPhi = MathUtils.sin(phi);
                tempNormal.set(radialAxis).multiplyLocal(cosPhi);
                tempNormal.setZ(tempNormal.getZ() + sinPhi);
                tempNormal.normalizeLocal();
                if (!_viewInside) {
                    _meshData.getNormalBuffer().put((float) tempNormal.getX()).put((float) tempNormal.getY())
                            .put((float) tempNormal.getZ());
                } else {
                    _meshData.getNormalBuffer().put((float) -tempNormal.getX()).put((float) -tempNormal.getY())
                            .put((float) -tempNormal.getZ());
                }

                tempNormal.multiplyLocal(_tubeRadius).addLocal(torusMiddle);
                _meshData.getVertexBuffer().put((float) tempNormal.getX()).put((float) tempNormal.getY())
                        .put((float) tempNormal.getZ());

                _meshData.getTextureCoords(0).getBuffer().put((float) radialFraction).put((float) circleFraction);
                i++;
            }

            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), iSave, i);
            BufferUtils.copyInternalVector3(_meshData.getNormalBuffer(), iSave, i);

            _meshData.getTextureCoords(0).getBuffer().put(1.0f).put((float) circleFraction);

            i++;
        }

        // duplicate the cylinder ends to form a torus
        for (int iR = 0; iR <= _radialSamples; iR++, i++) {
            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), iR, i);
            BufferUtils.copyInternalVector3(_meshData.getNormalBuffer(), iR, i);
            BufferUtils.copyInternalVector2(_meshData.getTextureCoords(0).getBuffer(), iR, i);
            _meshData.getTextureCoords(0).getBuffer().put(i * 2 + 1, 1.0f);
        }
    }

    private void setIndexData() {
        // allocate connectivity
        final int verts = ((_circleSamples + 1) * (_radialSamples + 1));
        final int tris = (2 * _circleSamples * _radialSamples);
        _meshData.setIndices(BufferUtils.createIndexBufferData(3 * tris, verts - 1));
        int i;
        // generate connectivity
        int connectionStart = 0;
        for (int circleCount = 0; circleCount < _circleSamples; circleCount++) {
            int i0 = connectionStart;
            int i1 = i0 + 1;
            connectionStart += _radialSamples + 1;
            int i2 = connectionStart;
            int i3 = i2 + 1;
            for (i = 0; i < _radialSamples; i++) {
                if (!_viewInside) {
                    _meshData.getIndices().put(i0++);
                    _meshData.getIndices().put(i2);
                    _meshData.getIndices().put(i1);
                    _meshData.getIndices().put(i1++);
                    _meshData.getIndices().put(i2++);
                    _meshData.getIndices().put(i3++);
                } else {
                    _meshData.getIndices().put(i0++);
                    _meshData.getIndices().put(i1);
                    _meshData.getIndices().put(i2);
                    _meshData.getIndices().put(i1++);
                    _meshData.getIndices().put(i3++);
                    _meshData.getIndices().put(i2++);
                }
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
        capsule.write(_circleSamples, "circleSamples", 0);
        capsule.write(_radialSamples, "radialSamples", 0);
        capsule.write(_tubeRadius, "tubeRadius", 0);
        capsule.write(_centerRadius, "centerRadius", 0);
        capsule.write(_viewInside, "viewInside", false);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _circleSamples = capsule.readInt("circleSamples", 0);
        _radialSamples = capsule.readInt("radialSamples", 0);
        _tubeRadius = capsule.readDouble("tubeRadius", 0);
        _centerRadius = capsule.readDouble("centerRadius", 0);
        _viewInside = capsule.readBoolean("viewInside", false);
    }

}