/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>Point</code> defines a collection of vertices that are rendered as single points or textured sprites depending
 * on PointType.
 */
public class Point extends Mesh {

    public enum PointType {
        Point, PointSprite;
    };

    private PointType _pointType;

    private float _pointSize = 1.0f;
    private boolean _antialiased = false;

    /**
     * Distance Attenuation fields.
     */
    // XXX: LWJGL requires 4 floats, but only 3 coefficients are mentioned in the specification?
    // JOGL works fine with 3.
    private final FloatBuffer _attenuationCoefficients = BufferUtils.createFloatBuffer(new float[] { 0.0f, 0f,
            0.000004f, 0f });
    private float _minPointSize = 1.0f;
    private float _maxPointSize = 64.0f;
    private boolean _useDistanceAttenuation = false;

    public Point() {
        this("point", null, null, null, (FloatBufferData) null);
    }

    public Point(final PointType type) {
        this();
        _pointType = type;
    }

    /**
     * Constructor instantiates a new <code>Point</code> object with a given set of data. Any data may be null, except
     * the vertex array. If this is null an exception is thrown.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparison purposes.
     * @param vertex
     *            the vertices or points.
     * @param normal
     *            the normals of the points.
     * @param color
     *            the color of the points.
     * @param texture
     *            the texture coordinates of the points.
     */
    public Point(final String name, final ReadOnlyVector3[] vertex, final ReadOnlyVector3[] normal,
            final ReadOnlyColorRGBA[] color, final ReadOnlyVector2[] texture) {
        super(name);
        setupData(BufferUtils.createFloatBuffer(vertex), BufferUtils.createFloatBuffer(normal),
                BufferUtils.createFloatBuffer(color), FloatBufferDataUtil.makeNew(texture));
        _meshData.setIndexMode(IndexMode.Points);
    }

    /**
     * Constructor instantiates a new <code>Point</code> object with a given set of data. Any data may be null, except
     * the vertex array. If this is null an exception is thrown.
     * 
     * @param name
     *            the name of the scene element. This is required for identification and comparison purposes.
     * @param vertex
     *            the vertices or points.
     * @param normal
     *            the normals of the points.
     * @param color
     *            the color of the points.
     * @param coords
     *            the texture coordinates of the points.
     */
    public Point(final String name, final FloatBuffer vertex, final FloatBuffer normal, final FloatBuffer color,
            final FloatBufferData coords) {
        super(name);
        setupData(vertex, normal, color, coords);
        _meshData.setIndexMode(IndexMode.Points);
    }

    /**
     * Initialize the meshdata object with data.
     * 
     * @param vertices
     * @param normals
     * @param colors
     * @param coords
     */
    private void setupData(final FloatBuffer vertices, final FloatBuffer normals, final FloatBuffer colors,
            final FloatBufferData coords) {
        _meshData.setVertexBuffer(vertices);
        _meshData.setNormalBuffer(normals);
        _meshData.setColorBuffer(colors);
        _meshData.setTextureCoords(coords, 0);
    }

    public boolean isPointSprite() {
        return _pointType == PointType.PointSprite;
    }

    /**
     * Default attenuation coefficient is calculated to work best with pointSize = 1.
     * 
     * @param bool
     */
    public void enableDistanceAttenuation(final boolean bool) {
        _useDistanceAttenuation = bool;
    }

    /**
     * Distance Attenuation Equation:<br>
     * x = distance from the eye<br>
     * Derived Size = clamp( pointSize * sqrt( attenuation(x) ) )<br>
     * attenuation(x) = 1 / (a + b*x + c*x^2)
     * <p>
     * Default coefficients used are(they should work best with pointSize=1):<br>
     * a = 0, b = 0, c = 0.000004f<br>
     * This should give(without taking regards to the max,min pointSize clamping):<br>
     * 1. A size of 1 pixel at distance of 500 units.<br>
     * Derived Size = 1/(0.000004*500^2) = 1<br>
     * 2. A size of 25 pixel at distance of 100 units.<br>
     * 3. A size of 2500 at a distance of 10 units.<br>
     * 
     * @see <a href="http://www.opengl.org/registry/specs/ARB/point_parameters.txt">OpenGL specification</a>
     * @param a
     *            constant term in the attenuation equation
     * @param b
     *            linear term in the attenuation equation
     * @param c
     *            quadratic term in the attenuation equation
     */
    public void setDistanceAttenuationCoefficients(final float a, final float b, final float c) {
        _attenuationCoefficients.put(0, a);
        _attenuationCoefficients.put(1, b);
        _attenuationCoefficients.put(2, c);
    }

    /**
     * @return true if points are to be drawn antialiased
     */
    public boolean isAntialiased() {
        return _antialiased;
    }

    /**
     * Sets whether the point should be antialiased. May decrease performance. If you want to enabled antialiasing, you
     * should also use an alphastate with a source of SourceFunction.SourceAlpha and a destination of
     * DB_ONE_MINUS_SRC_ALPHA or DB_ONE.
     * 
     * @param antialiased
     *            true if the line should be antialiased.
     */
    public void setAntialiased(final boolean antialiased) {
        _antialiased = antialiased;
    }

    public PointType getPointType() {
        return _pointType;
    }

    public void setPointType(final PointType pointType) {
        _pointType = pointType;
    }

    /**
     * @return the pixel size of each point.
     */
    public float getPointSize() {
        return _pointSize;
    }

    /**
     * Sets the pixel width of the point when drawn. Non anti-aliased point sizes are rounded to the nearest whole
     * number by opengl.
     * 
     * @param size
     *            The size to set.
     */
    public void setPointSize(final float size) {
        _pointSize = size;
    }

    /**
     * When DistanceAttenuation is enabled, the points maximum size will get clamped to this value.
     * 
     * @param maxSize
     */
    public void setMaxPointSize(final float maxSize) {
        _maxPointSize = maxSize;
    }

    /**
     * When DistanceAttenuation is enabled, the points maximum size will get clamped to this value.
     * 
     * @param maxSize
     */
    public float getMaxPointSize() {
        return _maxPointSize;
    }

    /**
     * When DistanceAttenuation is enabled, the points minimum size will get clamped to this value.
     * 
     * @param maxSize
     */
    public void setMinPointSize(final float minSize) {
        _minPointSize = minSize;
    }

    /**
     * When DistanceAttenuation is enabled, the points minimum size will get clamped to this value.
     * 
     * @param maxSize
     */
    public float getMinPointSize() {
        return _minPointSize;
    }

    /**
     * Used with Serialization. Do not call this directly.
     * 
     * @param s
     * @throws IOException
     * @see java.io.Serializable
     */
    private void writeObject(final java.io.ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    /**
     * Used with Serialization. Do not call this directly.
     * 
     * @param s
     * @throws IOException
     * @throws ClassNotFoundException
     * @see java.io.Serializable
     */
    private void readObject(final java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    @Override
    public Point makeCopy(final boolean shareGeometricData) {
        final Point pointCopy = (Point) super.makeCopy(shareGeometricData);
        pointCopy.setAntialiased(_antialiased);
        pointCopy.setDistanceAttenuationCoefficients(_attenuationCoefficients.get(0), _attenuationCoefficients.get(1),
                _attenuationCoefficients.get(2));
        pointCopy.setMaxPointSize(_maxPointSize);
        pointCopy.setMinPointSize(_minPointSize);
        pointCopy.setPointSize(_pointSize);
        pointCopy.setPointType(_pointType);
        return pointCopy;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_pointSize, "pointSize", 1);
        capsule.write(_antialiased, "antialiased", false);
        capsule.write(_pointType, "pointType", PointType.Point);
        capsule.write(_useDistanceAttenuation, "useDistanceAttenuation", false);
        capsule.write(_attenuationCoefficients, "attenuationCoefficients", null);
        capsule.write(_minPointSize, "minPointSize", 1);
        capsule.write(_maxPointSize, "maxPointSize", 64);

    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _pointSize = capsule.readFloat("pointSize", 1);
        _antialiased = capsule.readBoolean("antialiased", false);
        _pointType = capsule.readEnum("pointType", PointType.class, PointType.Point);
        _useDistanceAttenuation = capsule.readBoolean("useDistanceAttenuation", false);
        final FloatBuffer coef = capsule.readFloatBuffer("attenuationCoefficients", null);
        if (coef == null) {
            _attenuationCoefficients.clear();
            _attenuationCoefficients.put(new float[] { 0.0f, 0f, 0.000004f, 0f });
        } else {
            _attenuationCoefficients.clear();
            _attenuationCoefficients.put(coef);
        }
        _minPointSize = capsule.readFloat("minPointSize", 1);
        _maxPointSize = capsule.readFloat("maxPointSize", 64);

    }

    @Override
    public void render(final Renderer renderer) {
        renderer.setupPointParameters(_pointSize, isAntialiased(), isPointSprite(), _useDistanceAttenuation,
                _attenuationCoefficients, _minPointSize, _maxPointSize);

        super.render(renderer);
    }

}
