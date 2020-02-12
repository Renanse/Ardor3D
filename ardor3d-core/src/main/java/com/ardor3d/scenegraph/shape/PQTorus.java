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
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * PQTorus generates the geometry of a parameterized torus, also known as a pq torus.
 */
public class PQTorus extends Mesh {

    private double _p, _q;

    private double _radius, _width;

    private int _steps, _radialSamples;

    public PQTorus() {}

    /**
     * Creates a parameterized torus. Steps and radialSamples are both degree of accuracy values.
     * 
     * @param name
     *            The name of the torus.
     * @param p
     *            The x/z oscillation.
     * @param q
     *            The y oscillation.
     * @param radius
     *            The radius of the PQTorus.
     * @param width
     *            The width of the torus.
     * @param steps
     *            The steps along the torus.
     * @param radialSamples
     *            Radial samples for the torus.
     */
    public PQTorus(final String name, final double p, final double q, final double radius, final double width,
            final int steps, final int radialSamples) {
        super(name);

        _p = p;
        _q = q;
        _radius = radius;
        _width = width;
        _steps = steps;
        _radialSamples = radialSamples;

        setGeometryData();
        setIndexData();
    }

    private void setGeometryData() {
        final double THETA_STEP = (MathUtils.TWO_PI / _steps);
        final double BETA_STEP = (MathUtils.TWO_PI / _radialSamples);

        final Vector3[] toruspoints = new Vector3[_steps];
        // allocate vertices
        final int verts = _radialSamples * _steps;
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate normals if requested
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(verts));

        // allocate texture coordinates
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(verts), 0);

        final Vector3 pointB = Vector3.fetchTempInstance();
        final Vector3 T = Vector3.fetchTempInstance(), N = Vector3.fetchTempInstance(), B = Vector3.fetchTempInstance();
        final Vector3 tempNormA = Vector3.fetchTempInstance();
        final Vector3 tempNormB = Vector3.fetchTempInstance();
        double r, x, y, z, theta = 0.0, beta = 0.0;

        // Move along the length of the pq torus
        for (int i = 0; i < _steps; i++) {
            theta += THETA_STEP;
            final double circleFraction = ((double) i) / (double) _steps;

            // Find the point on the torus
            r = (0.5 * (2.0 + MathUtils.sin(_q * theta)) * _radius);
            x = (r * MathUtils.cos(_p * theta) * _radius);
            y = (r * MathUtils.sin(_p * theta) * _radius);
            z = (r * MathUtils.cos(_q * theta) * _radius);
            toruspoints[i] = new Vector3(x, y, z);

            // Now find a point slightly farther along the torus
            r = (0.5 * (2.0 + MathUtils.sin(_q * (theta + 0.01))) * _radius);
            x = (r * MathUtils.cos(_p * (theta + 0.01)) * _radius);
            y = (r * MathUtils.sin(_p * (theta + 0.01)) * _radius);
            z = (r * MathUtils.cos(_q * (theta + 0.01)) * _radius);
            pointB.set(x, y, z);

            // Approximate the Frenet Frame
            pointB.subtract(toruspoints[i], T);
            toruspoints[i].add(pointB, N);
            T.cross(N, B);
            B.cross(T, N);

            // Normalize the two vectors before use
            N.normalizeLocal();
            B.normalizeLocal();

            // Create a circle oriented by these new vectors
            beta = 0.0;
            for (int j = 0; j < _radialSamples; j++) {
                beta += BETA_STEP;
                final double cx = MathUtils.cos(beta) * _width;
                final double cy = MathUtils.sin(beta) * _width;
                final double radialFraction = ((double) j) / _radialSamples;
                tempNormA.setX((cx * N.getX() + cy * B.getX()));
                tempNormA.setY((cx * N.getY() + cy * B.getY()));
                tempNormA.setZ((cx * N.getZ() + cy * B.getZ()));
                tempNormA.normalize(tempNormB);
                tempNormA.addLocal(toruspoints[i]);

                _meshData.getVertexBuffer().put(tempNormA.getXf()).put(tempNormA.getYf()).put(tempNormA.getZf());
                _meshData.getNormalBuffer().put(tempNormB.getXf()).put(tempNormB.getYf()).put(tempNormB.getZf());
                _meshData.getTextureCoords(0).getBuffer().put((float) radialFraction).put((float) circleFraction);
            }
        }
        Vector3.releaseTempInstance(tempNormA);
        Vector3.releaseTempInstance(tempNormB);
        Vector3.releaseTempInstance(T);
        Vector3.releaseTempInstance(N);
        Vector3.releaseTempInstance(B);
        Vector3.releaseTempInstance(pointB);
    }

    private void setIndexData() {
        final IndexBufferData<?> indices = BufferUtils.createIndexBufferData(6 * _meshData.getVertexCount(),
                _meshData.getVertexCount() - 1);

        for (int i = _radialSamples; i < _meshData.getVertexCount() + (_radialSamples); i++) {
            indices.put(i);
            indices.put(i - _radialSamples);
            indices.put(i + 1);

            indices.put(i + 1);
            indices.put(i - _radialSamples);
            indices.put(i - _radialSamples + 1);
        }

        for (int i = 0, len = indices.getBufferCapacity(); i < len; i++) {
            int ind = indices.get(i);
            if (ind < 0) {
                ind += _meshData.getVertexCount();
                indices.put(i, ind);
            }
            if (ind >= _meshData.getVertexCount()) {
                ind -= _meshData.getVertexCount();
                indices.put(i, ind);
            }
        }
        indices.getBuffer().rewind();

        _meshData.setIndices(indices);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_p, "p", 0);
        capsule.write(_q, "q", 0);
        capsule.write(_radius, "radius", 0);
        capsule.write(_width, "width", 0);
        capsule.write(_steps, "steps", 0);
        capsule.write(_radialSamples, "radialSamples", 0);

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _p = capsule.readDouble("p", 0);
        _q = capsule.readDouble("q", 0);
        _radius = capsule.readDouble("radius", 0);
        _width = capsule.readDouble("width", 0);
        _steps = capsule.readInt("steps", 0);
        _radialSamples = capsule.readInt("radialSamples", 0);

    }
}