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
import java.nio.FloatBuffer;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Capsule</code> provides an extension of <code>Mesh</code>. A <code>Capsule</code> is defined by a height and a
 * radius. The center of the Cylinder is the origin.
 */
public class Capsule extends Mesh {

    private int axisSamples, radialSamples, sphereSamples;
    private double radius, height;

    public Capsule() {}

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
     */
    public Capsule(final String name, final int axisSamples, final int radialSamples, final int sphereSamples,
            final double radius, final double height) {

        super(name);

        this.axisSamples = axisSamples;
        this.sphereSamples = sphereSamples;
        this.radialSamples = radialSamples;
        this.radius = radius;
        this.height = height;

        recreateBuffers();
    }

    /**
     * @return Returns the height.
     */
    public double getHeight() {
        return height;
    }

    /**
     * @param height
     *            The height to set.
     */
    public void setHeight(final double height) {
        this.height = height;
        recreateBuffers();
    }

    /**
     * @return Returns the radius.
     */
    public double getRadius() {
        return radius;
    }

    /**
     * Change the radius of this cylinder.
     * 
     * @param radius
     *            The radius to set.
     */
    public void setRadius(final double radius) {
        this.radius = radius;
        setGeometryData();
    }

    private void recreateBuffers() {
        // determine vert quantity - first the sphere caps
        final int sampleLines = (2 * sphereSamples - 1 + axisSamples);
        final int verts = (radialSamples + 1) * sampleLines + 2;

        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), verts));

        // allocate normals
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(_meshData.getNormalBuffer(), verts));

        // allocate texture coordinates
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(verts), 0);

        // determine tri quantity
        final int tris = 2 * radialSamples * sampleLines;

        if (_meshData.getIndices() == null || _meshData.getIndices().getBufferLimit() != 3 * tris) {
            _meshData.setIndices(BufferUtils.createIndexBufferData(3 * tris, verts - 1));
        }

        setGeometryData();
        setIndexData();
    }

    private void setGeometryData() {
        final FloatBuffer verts = _meshData.getVertexBuffer();
        final FloatBuffer norms = _meshData.getNormalBuffer();
        final FloatBuffer texs = _meshData.getTextureBuffer(0);
        verts.rewind();
        norms.rewind();
        texs.rewind();

        // generate geometry
        final double inverseRadial = 1.0 / radialSamples;
        final double inverseSphere = 1.0 / sphereSamples;
        final double halfHeight = 0.5 * height;

        // Generate points on the unit circle to be used in computing the mesh
        // points on a cylinder slice.
        final double[] sin = new double[radialSamples + 1];
        final double[] cos = new double[radialSamples + 1];

        for (int radialCount = 0; radialCount < radialSamples; radialCount++) {
            final double angle = MathUtils.TWO_PI * inverseRadial * radialCount;
            cos[radialCount] = MathUtils.cos(angle);
            sin[radialCount] = MathUtils.sin(angle);
        }
        sin[radialSamples] = sin[0];
        cos[radialSamples] = cos[0];

        final Vector3 tempA = new Vector3();

        // top point.
        verts.put(0).put((float) (radius + halfHeight)).put(0);
        norms.put(0).put(1).put(0);
        texs.put(1).put(1);

        // generating the top dome.
        for (int i = 0; i < sphereSamples; i++) {
            final double center = radius * (1 - (i + 1) * (inverseSphere));
            final double lengthFraction = (center + height + radius) / (height + 2 * radius);

            // compute radius of slice
            final double fSliceRadius = Math.sqrt(Math.abs(radius * radius - center * center));

            for (int j = 0; j <= radialSamples; j++) {
                final Vector3 kRadial = tempA.set(cos[j], 0, sin[j]);
                kRadial.multiplyLocal(fSliceRadius);
                verts.put(kRadial.getXf()).put((float) (center + halfHeight)).put(kRadial.getZf());
                kRadial.setY(center);
                kRadial.normalizeLocal();
                norms.put(kRadial.getXf()).put(kRadial.getYf()).put(kRadial.getZf());
                final double radialFraction = 1 - (j * inverseRadial); // in [0,1)
                texs.put((float) radialFraction).put((float) lengthFraction);
            }
        }

        // generate cylinder... but no need to add points for first and last
        // samples as they are already part of domes.
        for (int i = 1; i < axisSamples; i++) {
            final double center = halfHeight - (i * height / axisSamples);
            final double lengthFraction = (center + halfHeight + radius) / (height + 2 * radius);

            for (int j = 0; j <= radialSamples; j++) {
                final Vector3 kRadial = tempA.set(cos[j], 0, sin[j]);
                kRadial.multiplyLocal(radius);
                verts.put(kRadial.getXf()).put((float) center).put(kRadial.getZf());
                kRadial.normalizeLocal();
                norms.put(kRadial.getXf()).put(kRadial.getYf()).put(kRadial.getZf());
                final double radialFraction = 1 - (j * inverseRadial); // in [0,1)
                texs.put((float) radialFraction).put((float) lengthFraction);
            }

        }

        // generating the bottom dome.
        for (int i = 0; i < sphereSamples; i++) {
            final double center = i * (radius / sphereSamples);
            final double lengthFraction = (radius - center) / (height + 2 * radius);

            // compute radius of slice
            final double fSliceRadius = Math.sqrt(Math.abs(radius * radius - center * center));

            for (int j = 0; j <= radialSamples; j++) {
                final Vector3 kRadial = tempA.set(cos[j], 0, sin[j]);
                kRadial.multiplyLocal(fSliceRadius);
                verts.put(kRadial.getXf()).put((float) (-center - halfHeight)).put(kRadial.getZf());
                kRadial.setY(-center);
                kRadial.normalizeLocal();
                norms.put(kRadial.getXf()).put(kRadial.getYf()).put(kRadial.getZf());
                final double radialFraction = 1 - (j * inverseRadial); // in [0,1)
                texs.put((float) radialFraction).put((float) lengthFraction);
            }
        }

        // bottom point.
        verts.put(0).put((float) (-radius - halfHeight)).put(0);
        norms.put(0).put(-1).put(0);
        texs.put(0).put(0);

    }

    private void setIndexData() {
        _meshData.getIndices().rewind();

        // start with top of top dome.
        for (int samples = 1; samples <= radialSamples; samples++) {
            _meshData.getIndices().put(samples + 1);
            _meshData.getIndices().put(samples);
            _meshData.getIndices().put(0);
        }

        for (int plane = 1; plane < (sphereSamples); plane++) {
            final int topPlaneStart = plane * (radialSamples + 1);
            final int bottomPlaneStart = (plane - 1) * (radialSamples + 1);
            for (int sample = 1; sample <= radialSamples; sample++) {
                _meshData.getIndices().put(bottomPlaneStart + sample);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample);
            }
        }

        int start = sphereSamples * (radialSamples + 1);

        // add cylinder
        for (int plane = 0; plane < (axisSamples); plane++) {
            final int topPlaneStart = start + plane * (radialSamples + 1);
            final int bottomPlaneStart = start + (plane - 1) * (radialSamples + 1);
            for (int sample = 1; sample <= radialSamples; sample++) {
                _meshData.getIndices().put(bottomPlaneStart + sample);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample);
            }
        }

        start += ((axisSamples - 1) * (radialSamples + 1));

        // Add most of the bottom dome triangles.
        for (int plane = 1; plane < (sphereSamples); plane++) {
            final int topPlaneStart = start + plane * (radialSamples + 1);
            final int bottomPlaneStart = start + (plane - 1) * (radialSamples + 1);
            for (int sample = 1; sample <= radialSamples; sample++) {
                _meshData.getIndices().put(bottomPlaneStart + sample);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample);
                _meshData.getIndices().put(bottomPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample + 1);
                _meshData.getIndices().put(topPlaneStart + sample);
            }
        }

        start += ((sphereSamples - 1) * (radialSamples + 1));
        // Finally the bottom of bottom dome.
        for (int samples = 1; samples <= radialSamples; samples++) {
            _meshData.getIndices().put(start + samples);
            _meshData.getIndices().put(start + samples + 1);
            _meshData.getIndices().put(start + radialSamples + 2);
        }
    }

    public void reconstruct(final Vector3 top, final Vector3 bottom, final double radius) {
        // our temp vars
        final Vector3 localTranslation = Vector3.fetchTempInstance();
        final Vector3 capsuleUp = Vector3.fetchTempInstance();

        // first make the capsule the right shape
        height = top.distance(bottom);
        this.radius = radius;
        setGeometryData();

        // now orient it in space.
        localTranslation.set(_localTransform.getTranslation());
        top.add(bottom, localTranslation).multiplyLocal(.5);

        // rotation that takes us from 0,1,0 to the unit vector described by top/center.
        top.subtract(localTranslation, capsuleUp).normalizeLocal();
        final Matrix3 rotation = Matrix3.fetchTempInstance();
        rotation.fromStartEndLocal(Vector3.UNIT_Y, capsuleUp);
        _localTransform.setRotation(rotation);

        Vector3.releaseTempInstance(localTranslation);
        Vector3.releaseTempInstance(capsuleUp);
        Matrix3.releaseTempInstance(rotation);

        updateWorldTransform(false);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(axisSamples, "axisSamples", 0);
        capsule.write(radialSamples, "radialSamples", 0);
        capsule.write(sphereSamples, "sphereSamples", 0);
        capsule.write(radius, "radius", 0);
        capsule.write(height, "height", 0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        axisSamples = capsule.readInt("circleSamples", 0);
        radialSamples = capsule.readInt("radialSamples", 0);
        sphereSamples = capsule.readInt("sphereSamples", 0);
        radius = capsule.readDouble("radius", 0);
        height = capsule.readDouble("height", 0);
    }
}
