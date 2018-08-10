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

import java.nio.FloatBuffer;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.geom.BufferUtils;

/**
 * GeoSphere - generate a polygon mesh approximating a sphere by recursive subdivision. First approximation is an
 * octahedron; each level of refinement increases the number of polygons by a factor of 4.
 * <p/>
 * Shared vertices are not retained, so numerical errors may produce cracks between polygons at high subdivision levels.
 * <p/>
 * Initial idea and text from C-Sourcecode by Jon Leech 3/24/89
 */

public class GeoSphere extends Mesh {

    public enum TextureMode {
        Original, Projected;
    }

    private int _maxlevels;
    private boolean _usingIcosahedron = true;
    private TextureMode _textureMode = TextureMode.Original;
    private double _radius;

    /**
     * @param name
     *            name of the spatial
     * @param useIcosahedron
     *            true to start with a 20 triangle mesh, false to start with a 8 triangle mesh
     * @param radius
     *            the radius of this sphere
     * @param maxlevels
     *            an integer >= 1 setting the recursion level
     * @param textureMode
     *            the texture mode to use when generating texture coordinates
     */
    public GeoSphere(final String name, final boolean useIcosahedron, final double radius, final int maxlevels,
            final TextureMode textureMode) {
        super(name);
        _maxlevels = maxlevels;
        _radius = radius;
        _maxlevels = maxlevels;
        _usingIcosahedron = useIcosahedron;
        _textureMode = textureMode;
        updateGeometry();
    }

    /**
     * Default Constructor for Savable use.
     */
    public GeoSphere() {}

    public double getRadius() {
        return _radius;
    }

    public boolean isUsingIcosahedron() {
        return _usingIcosahedron;
    }

    public void setTextureMode(final TextureMode textureMode) {
        if (textureMode != _textureMode) {
            _textureMode = textureMode;
            updateGeometry();
        }
    }

    public TextureMode getTextureMode() {
        return _textureMode;
    }

    private void updateGeometry() {
        final int initialTriangleCount = _usingIcosahedron ? 20 : 8;
        final int initialVertexCount = _usingIcosahedron ? 12 : 6;
        // number of triangles = initialTriangleCount * 4^(maxlevels-1)
        final int tris = initialTriangleCount << ((_maxlevels - 1) * 2);

        // number of vertBuf = (initialVertexCount + initialTriangleCount*4 +
        // initialTriangleCount*4*4 + ...)
        // = initialTriangleCount*(((4^maxlevels)-1)/(4-1)-1) +
        // initialVertexCount
        final int verts = initialTriangleCount * (((1 << (_maxlevels * 2)) - 1) / (4 - 1) - 1) + initialVertexCount
                + calculateBorderTriangles(_maxlevels);

        FloatBuffer vertBuf = _meshData.getVertexBuffer();
        _meshData.setVertexBuffer(vertBuf = BufferUtils.createVector3Buffer(vertBuf, verts));
        _meshData.setNormalBuffer(BufferUtils.createVector3Buffer(_meshData.getNormalBuffer(), verts));
        final FloatBufferData textureCoords = _meshData.getTextureCoords(0);
        _meshData.setTextureCoords(
                new FloatBufferData(BufferUtils.createVector2Buffer(textureCoords != null ? textureCoords.getBuffer()
                        : null, verts), 2), 0);

        int pos = 0;

        Triangle[] old;
        if (_usingIcosahedron) {
            final int[] indices = new int[] { pos + 0, pos + 1, pos + 2, pos + 0, pos + 2, pos + 3, pos + 0, pos + 3,
                    pos + 4, pos + 0, pos + 4, pos + 5, pos + 0, pos + 5, pos + 1, pos + 1, pos + 10, pos + 6, pos + 2,
                    pos + 6, pos + 7, pos + 3, pos + 7, pos + 8, pos + 4, pos + 8, pos + 9, pos + 5, pos + 9, pos + 10,
                    pos + 6, pos + 2, pos + 1, pos + 7, pos + 3, pos + 2, pos + 8, pos + 4, pos + 3, pos + 9, pos + 5,
                    pos + 4, pos + 10, pos + 1, pos + 5, pos + 11, pos + 7, pos + 6, pos + 11, pos + 8, pos + 7,
                    pos + 11, pos + 9, pos + 8, pos + 11, pos + 10, pos + 9, pos + 11, pos + 6, pos + 10 };
            final double y = 0.4472 * _radius;
            final double a = 0.8944 * _radius;
            final double b = 0.2764 * _radius;
            final double c = 0.7236 * _radius;
            final double d = 0.8507 * _radius;
            final double e = 0.5257 * _radius;
            pos++;
            put(new Vector3(0, _radius, 0));
            pos++;
            put(new Vector3(a, y, 0));
            pos++;
            put(new Vector3(b, y, -d));
            pos++;
            put(new Vector3(-c, y, -e));
            pos++;
            put(new Vector3(-c, y, e));
            pos++;
            put(new Vector3(b, y, d));
            pos++;
            put(new Vector3(c, -y, -e));
            pos++;
            put(new Vector3(-b, -y, -d));
            pos++;
            put(new Vector3(-a, -y, 0));
            pos++;
            put(new Vector3(-b, -y, d));
            pos++;
            put(new Vector3(c, -y, e));
            pos++;
            put(new Vector3(0, -_radius, 0));
            final Triangle[] ikosaedron = new Triangle[indices.length / 3];
            for (int i = 0; i < ikosaedron.length; i++) {
                final Triangle triangle = ikosaedron[i] = new Triangle();
                triangle.pt[0] = indices[i * 3];
                triangle.pt[1] = indices[i * 3 + 1];
                triangle.pt[2] = indices[i * 3 + 2];
            }

            old = ikosaedron;
        } else {
            /* Six equidistant points lying on the unit sphere */
            final Vector3 XPLUS = new Vector3(_radius, 0, 0); /* X */
            final Vector3 XMIN = new Vector3(-_radius, 0, 0); /* -X */
            final Vector3 YPLUS = new Vector3(0, _radius, 0); /* Y */
            final Vector3 YMIN = new Vector3(0, -_radius, 0); /* -Y */
            final Vector3 ZPLUS = new Vector3(0, 0, _radius); /* Z */
            final Vector3 ZMIN = new Vector3(0, 0, -_radius); /* -Z */

            final int xplus = pos++;
            put(XPLUS);
            final int xmin = pos++;
            put(XMIN);
            final int yplus = pos++;
            put(YPLUS);
            final int ymin = pos++;
            put(YMIN);
            final int zplus = pos++;
            put(ZPLUS);
            final int zmin = pos++;
            put(ZMIN);

            final Triangle[] octahedron = new Triangle[] { new Triangle(yplus, zplus, xplus),
                    new Triangle(xmin, zplus, yplus), new Triangle(ymin, zplus, xmin),
                    new Triangle(xplus, zplus, ymin), new Triangle(zmin, yplus, xplus),
                    new Triangle(zmin, xmin, yplus), new Triangle(zmin, ymin, xmin), new Triangle(zmin, xplus, ymin) };

            old = octahedron;
        }

        final Vector3 pt0 = new Vector3();
        final Vector3 pt1 = new Vector3();
        final Vector3 pt2 = new Vector3();

        /* Subdivide each starting triangle (maxlevels - 1) times */
        for (int level = 1; level < _maxlevels; level++) {
            /* Allocate a next triangle[] */
            final Triangle[] next = new Triangle[old.length * 4];
            for (int i = 0; i < next.length; i++) {
                next[i] = new Triangle();
            }

            /**
             * Subdivide each polygon in the old approximation and normalize the next points thus generated to lie on
             * the surface of the unit sphere. Each input triangle with vertBuf labeled [0,1,2] as shown below will be
             * turned into four next triangles:
             * 
             * <pre>
             * Make next points
             *   a = (0+2)/2
             *   b = (0+1)/2
             *   c = (1+2)/2
             *   
             * 1   /\   Normalize a, b, c
             *    /  \
             * b /____\ c
             * 
             * Construct next triangles
             * 
             *    /\    /\   [0,b,a] 
             *   /  \  /  \  [b,1,c]
             *  /____\/____\ [a,b,c]
             *  0 a 2 [a,c,2]
             * </pre>
             */
            for (int i = 0; i < old.length; i++) {
                int newi = i * 4;
                final Triangle oldt = old[i];
                Triangle newt = next[newi];

                BufferUtils.populateFromBuffer(pt0, vertBuf, oldt.pt[0]);
                BufferUtils.populateFromBuffer(pt1, vertBuf, oldt.pt[1]);
                BufferUtils.populateFromBuffer(pt2, vertBuf, oldt.pt[2]);
                final Vector3 av = createMidpoint(pt0, pt2).normalizeLocal().multiplyLocal(_radius);
                final Vector3 bv = createMidpoint(pt0, pt1).normalizeLocal().multiplyLocal(_radius);
                final Vector3 cv = createMidpoint(pt1, pt2).normalizeLocal().multiplyLocal(_radius);
                final int a = pos++;
                put(av);
                final int b = pos++;
                put(bv);
                final int c = pos++;
                put(cv);

                newt.pt[0] = oldt.pt[0];
                newt.pt[1] = b;
                newt.pt[2] = a;
                newt = next[++newi];

                newt.pt[0] = b;
                newt.pt[1] = oldt.pt[1];
                newt.pt[2] = c;
                newt = next[++newi];

                newt.pt[0] = a;
                newt.pt[1] = b;
                newt.pt[2] = c;
                newt = next[++newi];

                newt.pt[0] = a;
                newt.pt[1] = c;
                newt.pt[2] = oldt.pt[2];
            }

            /* Continue subdividing next triangles */
            old = next;
        }

        final IndexBufferData<?> indexBuffer = BufferUtils.createIndexBufferData(tris * 3, verts - 1);
        _meshData.setIndices(indexBuffer);

        int carryIntIndex = _meshData.getVertexBuffer().position() / 3;
        for (final Triangle triangle : old) {
            for (final int aPt : triangle.pt) {
                final Vector3 point = new Vector3();
                BufferUtils.populateFromBuffer(point, _meshData.getVertexBuffer(), aPt);
                if (point.getX() > 0 && point.getY() == 0) {
                    // Find out which 'y' side the triangle is on
                    final double yCenter = (_meshData.getVertexBuffer().get(triangle.pt[0] * 3 + 1)
                            + _meshData.getVertexBuffer().get(triangle.pt[1] * 3 + 1) + _meshData.getVertexBuffer()
                            .get(triangle.pt[2] * 3 + 1)) / 3.0;
                    if (yCenter > 0.0) {
                        put(point, true);
                        indexBuffer.put(carryIntIndex++);
                        continue;
                    }
                }
                indexBuffer.put(aPt);
            }
        }
    }

    private void put(final Vector3 vec) {
        put(vec, false);
    }

    private void put(final Vector3 vec, final boolean begining) {
        final FloatBuffer vertBuf = _meshData.getVertexBuffer();
        vertBuf.put((float) vec.getX());
        vertBuf.put((float) vec.getY());
        vertBuf.put((float) vec.getZ());

        final double length = vec.length();
        final FloatBuffer normBuf = _meshData.getNormalBuffer();
        final double xNorm = vec.getX() / length;
        normBuf.put((float) xNorm);
        final double yNorm = vec.getY() / length;
        normBuf.put((float) yNorm);
        final double zNorm = vec.getZ() / length;
        normBuf.put((float) zNorm);

        final FloatBuffer texBuf = _meshData.getTextureCoords(0).getBuffer();
        if (vec.getX() > 0.0 && vec.getY() == 0.0) {
            if (begining) {
                texBuf.put(0);
            } else {
                texBuf.put(1);
            }
        } else {
            texBuf.put((float) ((Math.atan2(yNorm, xNorm) / (2 * Math.PI) + 1) % 1));
        }

        double vPos = 0;
        switch (_textureMode) {
            case Original:
                vPos = .5 * (zNorm + 1);
                break;
            case Projected:
                vPos = MathUtils.INV_PI * (MathUtils.HALF_PI + Math.asin(zNorm));
                break;
        }
        texBuf.put((float) vPos);
    }

    private int calculateBorderTriangles(int levels) {
        int current = 108;
        // Pattern starts at 4
        levels -= 4;
        while (levels-- > 0) {
            current = 2 * current + 12;
        }
        return current;
    }

    /**
     * Compute the average of two vectors.
     * 
     * @param a
     *            first vector
     * @param b
     *            second vector
     * @return the average of two points
     */
    protected Vector3 createMidpoint(final Vector3 a, final Vector3 b) {
        return new Vector3((a.getX() + b.getX()) * 0.5, (a.getY() + b.getY()) * 0.5, (a.getZ() + b.getZ()) * 0.5);
    }

    static class Triangle {
        int[] pt = new int[3]; /* Vertices of triangle */

        public Triangle() {}

        public Triangle(final int pt0, final int pt1, final int pt2) {
            pt[0] = pt0;
            pt[1] = pt1;
            pt[2] = pt2;
        }
    }
}
