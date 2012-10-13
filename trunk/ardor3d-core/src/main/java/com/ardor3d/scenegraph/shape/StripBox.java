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
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

public class StripBox extends Mesh {

    public double _xExtent, _yExtent, _zExtent;

    public final Vector3 _center = new Vector3(0f, 0f, 0f);

    /**
     * instantiates a new <code>StripBox</code> object. All information must be applies later. For internal usage only
     */
    public StripBox() {
        super("temp");
    }

    /**
     * Constructor instantiates a new <code>StripBox</code> object. Center and vertice information must be supplied
     * later.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparision purposes.
     */
    public StripBox(final String name) {
        super(name);
    }

    /**
     * Constructor instantiates a new <code>StripBox</code> object. The minimum and maximum point are provided. These
     * two points define the shape and size of the box, but not it's orientation or position. You should use the
     * <code>setTranslation</code> and <code>setLocalRotation</code> for those attributes.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparison purposes.
     * @param min
     *            the minimum point that defines the box.
     * @param max
     *            the maximum point that defines the box.
     */
    public StripBox(final String name, final Vector3 min, final Vector3 max) {
        super(name);
        setData(min, max);
    }

    /**
     * Constructs a new box. The box has the given center and extends in the x, y, and z out from the center (+ and -)
     * by the given amounts. So, for example, a box with extent of .5 would be the unit cube.
     * 
     * @param name
     *            Name of the box.
     * @param center
     *            Center of the box.
     * @param xExtent
     *            x extent of the box, in both directions.
     * @param yExtent
     *            y extent of the box, in both directions.
     * @param zExtent
     *            z extent of the box, in both directions.
     */
    public StripBox(final String name, final Vector3 center, final double xExtent, final double yExtent,
            final double zExtent) {
        super(name);
        setData(center, xExtent, yExtent, zExtent);
    }

    /**
     * Changes the data of the box so that the two opposite corners are minPoint and maxPoint. The other corners are
     * created from those two poitns. If update buffers is flagged as true, the vertex/normal/texture/color/index
     * buffers are updated when the data is changed.
     * 
     * @param minPoint
     *            The new minPoint of the box.
     * @param maxPoint
     *            The new maxPoint of the box.
     */
    public void setData(final Vector3 minPoint, final Vector3 maxPoint) {
        _center.set(maxPoint).addLocal(minPoint).multiplyLocal(0.5f);

        final double x = maxPoint.getX() - _center.getX();
        final double y = maxPoint.getY() - _center.getY();
        final double z = maxPoint.getZ() - _center.getZ();
        setData(_center, x, y, z);
    }

    /**
     * Changes the data of the box so that its center is <code>center</code> and it extends in the x, y, and z
     * directions by the given extent. Note that the actual sides will be 2x the given extent values because the box
     * extends in + & - from the center for each extent.
     * 
     * @param center
     *            The center of the box.
     * @param xExtent
     *            x extent of the box, in both directions.
     * @param yExtent
     *            y extent of the box, in both directions.
     * @param zExtent
     *            z extent of the box, in both directions.
     */
    public void setData(final Vector3 center, final double xExtent, final double yExtent, final double zExtent) {
        if (center != null) {
            _center.set(center);
        }

        _xExtent = xExtent;
        _yExtent = yExtent;
        _zExtent = zExtent;

        setVertexData();
        setNormalData();
        setTextureData();
        setIndexData();

    }

    /**
     * 
     * <code>setVertexData</code> sets the vertex positions that define the box. These eight points are determined from
     * the minimum and maximum point.
     * 
     */
    private void setVertexData() {
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(_meshData.getVertexBuffer(), 8));
        final Vector3[] vert = computeVertices(); // returns 8
        _meshData.getVertexBuffer().clear();
        _meshData.getVertexBuffer().put(vert[0].getXf()).put(vert[0].getYf()).put(vert[0].getZf());
        _meshData.getVertexBuffer().put(vert[1].getXf()).put(vert[1].getYf()).put(vert[1].getZf());
        _meshData.getVertexBuffer().put(vert[2].getXf()).put(vert[2].getYf()).put(vert[2].getZf());
        _meshData.getVertexBuffer().put(vert[3].getXf()).put(vert[3].getYf()).put(vert[3].getZf());
        _meshData.getVertexBuffer().put(vert[4].getXf()).put(vert[4].getYf()).put(vert[4].getZf());
        _meshData.getVertexBuffer().put(vert[5].getXf()).put(vert[5].getYf()).put(vert[5].getZf());
        _meshData.getVertexBuffer().put(vert[6].getXf()).put(vert[6].getYf()).put(vert[6].getZf());
        _meshData.getVertexBuffer().put(vert[7].getXf()).put(vert[7].getYf()).put(vert[7].getZf());
    }

    /**
     * 
     * <code>setNormalData</code> sets the normals of each of the box's planes.
     * 
     * 
     */
    private void setNormalData() {
        final Vector3[] vert = computeVertices(); // returns 8
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(_meshData.getNormalBuffer(), 8));
        final Vector3 norm = new Vector3();

        _meshData.getNormalBuffer().clear();
        for (int i = 0; i < 8; i++) {
            norm.set(vert[i]).normalizeLocal();
            _meshData.getNormalBuffer().put(norm.getXf()).put(norm.getYf()).put(norm.getZf());
        }
    }

    /**
     * 
     * <code>setTextureData</code> sets the points that define the texture of the box. It's a one-to-one ratio, where
     * each plane of the box has it's own copy of the texture. That is, the texture is repeated one time for each six
     * faces.
     * 
     */
    private void setTextureData() {
        if (_meshData.getTextureCoords(0) == null) {
            _meshData.setTextureCoords(new FloatBufferData(BufferUtils.createVector2Buffer(8), 2), 0);
            final FloatBuffer tex = _meshData.getTextureCoords(0).getBuffer();
            tex.put(1).put(0); // 0
            tex.put(0).put(0); // 1
            tex.put(0).put(1); // 2
            tex.put(1).put(1); // 3
            tex.put(1).put(0); // 4
            tex.put(0).put(0); // 5
            tex.put(1).put(1); // 6
            tex.put(0).put(1); // 7
        }
    }

    /**
     * 
     * <code>setIndexData</code> sets the indices into the list of vertices, defining all triangles that constitute the
     * box.
     * 
     */
    private void setIndexData() {
        _meshData.setIndexMode(IndexMode.TriangleStrip);
        if (_meshData.getIndexBuffer() == null) {
            final byte[] indices = new byte[] { 2, 3, 6, 7, 5, 3, 0, 2, 1, 6, 4, 5, 1, 0 };
            final ByteBuffer buf = BufferUtils.createByteBuffer(indices.length);
            buf.put(indices);
            buf.rewind();
            _meshData.setIndexBuffer(buf);
        }
    }

    /**
     * <code>clone</code> creates a new StripBox object containing the same data as this one.
     * 
     * @return the new StripBox
     */
    @Override
    public StripBox clone() {
        return new StripBox(getName() + "_clone", _center.clone(), _xExtent, _yExtent, _zExtent);
    }

    /**
     * 
     * @return a size 8 array of Vectors representing the 8 points of the box.
     */
    public Vector3[] computeVertices() {

        final Vector3 akEAxis[] = { Vector3.UNIT_X.multiply(_xExtent, Vector3.fetchTempInstance()),
                Vector3.UNIT_Y.multiply(_yExtent, Vector3.fetchTempInstance()),
                Vector3.UNIT_Z.multiply(_zExtent, Vector3.fetchTempInstance()) };

        final Vector3 rVal[] = new Vector3[8];
        rVal[0] = _center.subtract(akEAxis[0], new Vector3()).subtractLocal(akEAxis[1]).subtractLocal(akEAxis[2]);
        rVal[1] = _center.add(akEAxis[0], new Vector3()).subtractLocal(akEAxis[1]).subtractLocal(akEAxis[2]);
        rVal[2] = _center.add(akEAxis[0], new Vector3()).addLocal(akEAxis[1]).subtractLocal(akEAxis[2]);
        rVal[3] = _center.subtract(akEAxis[0], new Vector3()).addLocal(akEAxis[1]).subtractLocal(akEAxis[2]);
        rVal[4] = _center.add(akEAxis[0], new Vector3()).subtractLocal(akEAxis[1]).addLocal(akEAxis[2]);
        rVal[5] = _center.subtract(akEAxis[0], new Vector3()).subtractLocal(akEAxis[1]).addLocal(akEAxis[2]);
        rVal[6] = _center.add(akEAxis[0], new Vector3()).addLocal(akEAxis[1]).addLocal(akEAxis[2]);
        rVal[7] = _center.subtract(akEAxis[0], new Vector3()).addLocal(akEAxis[1]).addLocal(akEAxis[2]);
        for (final Vector3 axis : akEAxis) {
            Vector3.releaseTempInstance(axis);
        }
        return rVal;
    }

    /**
     * Returns the current center of the box.
     * 
     * @return The box's center.
     */
    public Vector3 getCenter() {
        return _center;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_xExtent, "xExtent", 0);
        capsule.write(_yExtent, "yExtent", 0);
        capsule.write(_zExtent, "zExtent", 0);
        capsule.write(_center, "center", new Vector3(Vector3.ZERO));

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _xExtent = capsule.readDouble("xExtent", 0);
        _yExtent = capsule.readDouble("yExtent", 0);
        _zExtent = capsule.readDouble("zExtent", 0);
        _center.set((Vector3) capsule.readSavable("center", new Vector3(Vector3.ZERO)));
    }
}