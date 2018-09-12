/**
 * Copyright (c) 2008-2010 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.ui.util;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector2;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;

public class UIDisk extends Mesh {

    protected int _radialSamples;

    protected double _radius;

    protected double _innerRadius;

    public UIDisk() {}

    /**
     * Creates a flat disk (circle) at the origin flat along the Z. Usually, a higher sample number creates a better
     * looking disk, but at the cost of more vertex information.
     *
     * @param name
     *            The name of the disk.
     * @param radialSamples
     *            The number of radial samples.
     * @param radius
     *            The outer radius of the disk.
     */
    public UIDisk(final String name, final int radialSamples, final double radius) {
        this(name, radialSamples, radius, 0);
    }

    /**
     * Creates a flat disk (circle) at the origin flat along the Z. Usually, a higher sample number creates a better
     * looking disk, but at the cost of more vertex information.
     *
     * @param name
     *            The name of the disk.
     * @param radialSamples
     *            The number of radial samples.
     * @param radius
     *            The outer radius of the disk.
     * @param innerRadius
     *            The inner radius of the disk. If greater than 0, the center of the disk has a hole of this size.
     */
    public UIDisk(final String name, final int radialSamples, final double radius, final double innerRadius) {
        super(name);

        _radialSamples = radialSamples;

        // allocate vertices
        final int verts = radialSamples * 2;
        _meshData.setVertexCoords(new FloatBufferData(verts * 2, 2));
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(verts), 0);

        final int tris = radialSamples * 2;
        _meshData.setIndices(BufferUtils.createIndexBufferData(3 * tris, verts - 1));

        resetGeometry(radius, innerRadius, null);
        setIndexData();

    }

    public void resetGeometry(final double radius, final double innerRadius, final SubTex subTex) {
        _radius = radius;
        _innerRadius = innerRadius;

        // generate geometry
        final double inverseRadial = 1.0 / _radialSamples;
        final Vector2 radialFraction = new Vector2();
        final Vector2 texCoord = new Vector2();

        float txOff = 0f, tyOff = 0f, txScale = 1f, tyScale = 1f;
        if (subTex != null && subTex.getTexture() != null && subTex.getTexture() != null) {
            txOff = subTex.getStartX();
            tyOff = subTex.getStartY();
            txScale = subTex.getEndX() - subTex.getStartX();
            tyScale = subTex.getEndY() - subTex.getStartY();
        }

        final Vector2 radialInner = new Vector2();
        for (int radialCount = 0; radialCount < _radialSamples; radialCount++) {
            final double angle = MathUtils.TWO_PI * inverseRadial * radialCount;
            final double cos = MathUtils.cos(angle);
            final double sin = MathUtils.sin(angle);
            final Vector2 radial = new Vector2(cos, sin);
            radialInner.set(radial).multiplyLocal(_innerRadius);

            for (int shellCount = 0; shellCount < 2; shellCount++) {
                radialFraction.set(radial).multiplyLocal(shellCount);

                final int i = shellCount + 2 * radialCount;

                radialFraction.multiplyLocal(_radius - _innerRadius).addLocal(radialInner);
                BufferUtils.setInBuffer(radialFraction, _meshData.getVertexBuffer(), i);

                texCoord.setX(txOff + txScale * 0.5 * (1.0 + radialFraction.getX() / _radius));
                texCoord.setY(tyOff + tyScale * 0.5 * (1.0 + radialFraction.getY() / _radius));
                BufferUtils.setInBuffer(texCoord, _meshData.getTextureCoords(0).getBuffer(), i);
            }
        }

        _meshData.markBufferDirty(MeshData.KEY_VertexCoords);
        _meshData.markBufferDirty(MeshData.KEY_TextureCoords0);
    }

    private void setIndexData() {
        // generate connectivity
        for (int radialCount0 = _radialSamples
                - 1, radialCount1 = 0; radialCount1 < _radialSamples; radialCount0 = radialCount1++) {
            final int i00 = 2 * radialCount0;
            final int i01 = 2 * radialCount1;
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

    public int getRadialSamples() {
        return _radialSamples;
    }

    public double getRadius() {
        return _radius;
    }

    public double getInnerRadius() {
        return _innerRadius;
    }

}
