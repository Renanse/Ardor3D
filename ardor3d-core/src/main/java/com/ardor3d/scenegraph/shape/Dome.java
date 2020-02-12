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
 * A half sphere.
 */
public class Dome extends Mesh {

    private int _planes;

    private int _radialSamples;

    /** The radius of the dome */
    private double _radius;

    public Dome() {}

    /**
     * Constructs a dome. By default the dome has not geometry data or center.
     * 
     * @param name
     *            The name of the dome.
     */
    public Dome(final String name) {
        super(name);
    }

    /**
     * Constructs a dome with center at the origin. For details, see the other constructor.
     * 
     * @param name
     *            Name of dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The samples along the radial.
     * @param radius
     *            Radius of the dome.
     * @see #Dome(java.lang.String, com.ardor3d.math.Vector3, int, int, double)
     */
    public Dome(final String name, final int planes, final int radialSamples, final double radius) {
        this(name, new Vector3(0, 0, 0), planes, radialSamples, radius);
    }

    /**
     * Constructs a dome. All geometry data buffers are updated automatically. Both planes and radialSamples increase
     * the quality of the generated dome.
     * 
     * @param name
     *            Name of the dome.
     * @param center
     *            Center of the dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param radius
     *            The radius of the dome.
     */
    public Dome(final String name, final Vector3 center, final int planes, final int radialSamples, final double radius) {

        super(name);
        setData(center, planes, radialSamples, radius, true, true);
    }

    /**
     * Constructs a dome. All geometry data buffers are updated automatically. Both planes and radialSamples increase
     * the quality of the generated dome.
     * 
     * @param name
     *            Name of the dome.
     * @param center
     *            Center of the dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The number of samples along the radial.
     * @param radius
     *            The radius of the dome.
     * @param outsideView
     *            If true, the triangles will be connected for a view outside of the dome.
     */
    public Dome(final String name, final Vector3 center, final int planes, final int radialSamples,
            final double radius, final boolean outsideView) {
        super(name);
        setData(center, planes, radialSamples, radius, true, outsideView);
    }

    /**
     * Changes the information of the dome into the given values. The boolean at the end signals if buffer data should
     * be updated as well. If the dome is to be rendered, then that value should be true.
     * 
     * @param center
     *            The new center of the dome.
     * @param planes
     *            The number of planes along the Z-axis.
     * @param radialSamples
     *            The new number of radial samples of the dome.
     * @param radius
     *            The new radius of the dome.
     * @param updateBuffers
     *            If true, buffer information is updated as well.
     * @param outsideView
     *            If true, the triangles will be connected for a view outside of the dome.
     */
    public void setData(final Vector3 center, final int planes, final int radialSamples, final double radius,
            final boolean updateBuffers, final boolean outsideView) {
        _planes = planes;
        _radialSamples = radialSamples;
        _radius = radius;

        if (updateBuffers) {
            setGeometryData(outsideView, center);
            setIndexData();
        }
    }

    /**
     * Generates the vertices of the dome
     * 
     * @param outsideView
     *            If the dome should be viewed from the outside (if not zbuffer is used)
     * @param center
     */
    private void setGeometryData(final boolean outsideView, final Vector3 center) {
        final Vector3 tempVa = Vector3.fetchTempInstance();
        final Vector3 tempVb = Vector3.fetchTempInstance();
        final Vector3 tempVc = Vector3.fetchTempInstance();

        // allocate vertices, we need one extra in each radial to get the
        // correct texture coordinates
        final int verts = ((_planes - 1) * (_radialSamples + 1)) + 1;
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate normals
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate texture coordinates
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(verts), 0);

        // generate geometry
        final double fInvRS = 1.0 / _radialSamples;
        final double fYFactor = 1.0 / (_planes - 1);

        // Generate points on the unit circle to be used in computing the mesh
        // points on a dome slice.
        final double[] afSin = new double[(_radialSamples)];
        final double[] afCos = new double[(_radialSamples)];
        for (int iR = 0; iR < _radialSamples; iR++) {
            final double fAngle = MathUtils.TWO_PI * fInvRS * iR;
            afCos[iR] = MathUtils.cos(fAngle);
            afSin[iR] = MathUtils.sin(fAngle);
        }

        // generate the dome itself
        int i = 0;
        for (int iY = 0; iY < (_planes - 1); iY++) {
            final double fYFraction = fYFactor * iY; // in (0,1)
            final double fY = _radius * fYFraction;
            // compute center of slice
            final Vector3 kSliceCenter = tempVb.set(center);
            kSliceCenter.addLocal(0, fY, 0);

            // compute radius of slice
            final double fSliceRadius = Math.sqrt(Math.abs(_radius * _radius - fY * fY));

            // compute slice vertices
            Vector3 kNormal;
            final int iSave = i;
            for (int iR = 0; iR < _radialSamples; iR++) {
                final double fRadialFraction = iR * fInvRS; // in [0,1)
                final Vector3 kRadial = tempVc.set(afCos[iR], 0, afSin[iR]);
                kRadial.multiply(fSliceRadius, tempVa);
                _meshData.getVertexBuffer().put((float) (kSliceCenter.getX() + tempVa.getX()))
                        .put((float) (kSliceCenter.getY() + tempVa.getY()))
                        .put((float) (kSliceCenter.getZ() + tempVa.getZ()));

                BufferUtils.populateFromBuffer(tempVa, _meshData.getVertexBuffer(), i);
                kNormal = tempVa.subtractLocal(center);
                kNormal.normalizeLocal();
                if (outsideView) {
                    _meshData.getNormalBuffer().put((float) kNormal.getX()).put((float) kNormal.getY())
                            .put((float) kNormal.getZ());
                } else {
                    _meshData.getNormalBuffer().put((float) -kNormal.getX()).put((float) -kNormal.getY())
                            .put((float) -kNormal.getZ());
                }

                _meshData.getTextureCoords(0).getBuffer().put((float) fRadialFraction).put((float) fYFraction);

                i++;
            }

            BufferUtils.copyInternalVector3(_meshData.getVertexBuffer(), iSave, i);
            BufferUtils.copyInternalVector3(_meshData.getNormalBuffer(), iSave, i);

            _meshData.getTextureCoords(0).getBuffer().put(1.0f).put((float) fYFraction);

            i++;
        }

        // pole
        _meshData.getVertexBuffer().put((float) center.getX()).put((float) (center.getY() + _radius))
                .put((float) center.getZ());

        if (outsideView) {
            _meshData.getNormalBuffer().put(0).put(1).put(0);
        } else {
            _meshData.getNormalBuffer().put(0).put(-1).put(0);
        }

        _meshData.getTextureCoords(0).getBuffer().put(0.5f).put(1.0f);

        Vector3.releaseTempInstance(tempVa);
        Vector3.releaseTempInstance(tempVb);
        Vector3.releaseTempInstance(tempVc);
    }

    /**
     * Generates the connections
     */
    private void setIndexData() {
        // allocate connectivity
        final int verts = ((_planes - 1) * (_radialSamples + 1)) + 1;
        final int tris = (_planes - 2) * _radialSamples * 2 + _radialSamples;
        _meshData.setIndices(BufferUtils.createIndexBufferData(3 * tris, verts - 1));

        // generate connectivity
        // Generate only for middle planes
        for (int plane = 1; plane < (_planes - 1); plane++) {
            final int bottomPlaneStart = (plane - 1) * (_radialSamples + 1);
            final int topPlaneStart = plane * (_radialSamples + 1);
            for (int sample = 0; sample < _radialSamples; sample++) {
                _meshData.getIndices().put(bottomPlaneStart + sample);
                _meshData.getIndices().put(topPlaneStart + sample);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample);
                _meshData.getIndices().put(topPlaneStart + sample + 1);
            }
        }

        // pole triangles
        final int bottomPlaneStart = (_planes - 2) * (_radialSamples + 1);
        for (int samples = 0; samples < _radialSamples; samples++) {
            _meshData.getIndices().put(bottomPlaneStart + samples);
            _meshData.getIndices().put(_meshData.getVertexCount() - 1);
            _meshData.getIndices().put(bottomPlaneStart + samples + 1);
        }
    }

    public int getPlanes() {
        return _planes;
    }

    public int getRadialSamples() {
        return _radialSamples;
    }

    public double getRadius() {
        return _radius;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_planes, "planes", 0);
        capsule.write(_radialSamples, "radialSamples", 0);
        capsule.write(_radius, "radius", 0);

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _planes = capsule.readInt("planes", 0);
        _radialSamples = capsule.readInt("radialSamples", 0);
        _radius = capsule.readDouble("radius", 0);

    }
}