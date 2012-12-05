/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * An approximations of a flat circle. It is simply defined with a radius. It starts out flat along the Z, with center
 * at the origin.
 */
public class Disk extends Mesh {

    private int _shellSamples;

    private int _radialSamples;

    private double _radius;

    public Disk() {}

    /**
     * Creates a flat disk (circle) at the origin flat along the Z. Usually, a higher sample number creates a better
     * looking cylinder, but at the cost of more vertex information.
     * 
     * @param name
     *            The name of the disk.
     * @param shellSamples
     *            The number of shell samples.
     * @param radialSamples
     *            The number of radial samples.
     * @param radius
     *            The radius of the disk.
     */
    public Disk(final String name, final int shellSamples, final int radialSamples, final double radius) {
        super(name);

        _shellSamples = shellSamples;
        _radialSamples = radialSamples;
        _radius = radius;

        final int radialless = radialSamples - 1;
        final int shellLess = shellSamples - 1;
        // allocate vertices
        final int verts = 1 + radialSamples * shellLess;
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(verts));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(verts));
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(verts), 0);

        final int tris = radialSamples * (2 * shellLess - 1);
        _meshData.setIndices(BufferUtils.createIndexBufferData(3 * tris, verts - 1));

        setGeometryData(shellLess);
        setIndexData(radialless, shellLess);

    }

    private void setGeometryData(final int shellLess) {
        // generate geometry
        // center of disk
        _meshData.getVertexBuffer().put(0).put(0).put(0);

        for (int x = 0; x < _meshData.getVertexCount(); x++) {
            _meshData.getNormalBuffer().put(0).put(0).put(1);
        }

        _meshData.getTextureCoords(0).getBuffer().put(.5f).put(.5f);

        final double inverseShellLess = 1.0 / shellLess;
        final double inverseRadial = 1.0 / _radialSamples;
        final Vector3 radialFraction = new Vector3();
        final Vector2 texCoord = new Vector2();
        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            final double angle = MathUtils.TWO_PI * inverseRadial * radialCount;
            final double cos = MathUtils.cos(angle);
            final double sin = MathUtils.sin(angle);
            final Vector3 radial = new Vector3(cos, sin, 0);

            for (int shellCount = 1; shellCount < _shellSamples; shellCount++) {
                final double fraction = inverseShellLess * shellCount; // in (0,R]
                radialFraction.set(radial).multiplyLocal(fraction);
                final int i = shellCount + shellLess * radialCount;
                texCoord.setX(0.5 * (1.0 + radialFraction.getX()));
                texCoord.setY(0.5 * (1.0 + radialFraction.getY()));
                BufferUtils.setInBuffer(texCoord, _meshData.getTextureCoords(0).getBuffer(), i);

                radialFraction.multiplyLocal(_radius);
                BufferUtils.setInBuffer(radialFraction, _meshData.getVertexBuffer(), i);
            }
        }
    }

    private void setIndexData(final int radialless, final int shellLess) {
        // generate connectivity
        for (int radialCount0 = radialless, radialCount1 = 0; radialCount1 < _radialSamples; radialCount0 = radialCount1++) {
            _meshData.getIndices().put(0);
            _meshData.getIndices().put(1 + shellLess * radialCount0);
            _meshData.getIndices().put(1 + shellLess * radialCount1);
            for (int iS = 1; iS < shellLess; iS++) {
                final int i00 = iS + shellLess * radialCount0;
                final int i01 = iS + shellLess * radialCount1;
                final int i10 = i00 + 1;
                final int i11 = i01 + 1;
                _meshData.getIndices().put(i00);
                _meshData.getIndices().put(i10);
                _meshData.getIndices().put(i11);
                _meshData.getIndices().put(i00);
                _meshData.getIndices().put(i11);
                _meshData.getIndices().put(i01);
            }
        }
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_shellSamples, "shellSamples", 0);
        capsule.write(_radialSamples, "radialSamples", 0);
        capsule.write(_radius, "radius", 0);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _shellSamples = capsule.readInt("shellSamples", 0);
        _radialSamples = capsule.readInt("radialSamples", 0);
        _radius = capsule.readDouble("radius", 0);
    }
}