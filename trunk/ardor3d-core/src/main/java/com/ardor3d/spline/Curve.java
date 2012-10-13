/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.spline;

import java.util.List;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Point;

/**
 * Curve class contains a list of control points and a spline. It also contains method for visualising itself as a
 * renderable series of points or a line.
 */
public class Curve {

    /** @see #setControlPoints(List) */
    private List<ReadOnlyVector3> _controlPoints;

    /** @see #setSpline(Spline) */
    private Spline _spline;

    /**
     * Creates a new instance of <code>Curve</code>.
     * 
     * @param controlPoints
     *            see {@link #setControlPoints(List)}
     * @param spline
     *            see {@link #setSpline(Spline)}
     */
    public Curve(final List<ReadOnlyVector3> controlPoints, final Spline spline) {
        super();

        setControlPoints(controlPoints);
        setSpline(spline);
    }

    /**
     * Creates a new <code>Point</code> from the control points making up this curve. It will have the name
     * <code>point</code>, no normals, colour or texture, these can be added to the returned point if needed.
     * 
     * @param steps
     *            The number of iterations to perform between control points, the higher this number the more points
     *            will be shown, but it will also contain more vertices, must be greater than one. Use two to just show
     *            the actual control points set for the curve.
     * @return A <code>Point</code> containing all the curve points, will not be <code>null</code>.
     */
    public Point toRenderablePoint(final int steps) {
        return toRenderablePoint(1, getControlPointCount() - 2, steps);
    }

    /**
     * Creates a new <code>Point</code> from the given control point indices. It will have the name <code>point</code>,
     * no normals, colour or texture, these can be added to the returned point if needed.
     * 
     * @param start
     *            The index of the control point to start from, must be greater than or equal to one and less than
     *            <code>end</code>.
     * @param end
     *            The index of the control point to end with, must be less than {@link #getControlPointCount()
     *            controlPointCount} minus one and greater than <code>start</code>.
     * @param steps
     *            The number of iterations to perform between control points, the higher this number the more points
     *            will be shown, but it will also contain more vertices, must be greater than one.
     * @return A <code>Point</code> containing all the curve points, will not be <code>null</code>.
     */
    public Point toRenderablePoint(final int start, final int end, final int steps) {
        final Vector3[] points = toVector3(start, end, steps);

        return new Point("point", points, null, null, null);
    }

    /**
     * Creates a new <code>Line</code> from the control points making up this curve. It will have the name
     * <code>curve</code>, no normals, colour or texture, these can be added to the returned line if needed.
     * 
     * @param steps
     *            The number of iterations to perform between control points, the higher this number the smoother the
     *            returned line will be, but it will also contain more vertices, must be greater than one.
     * @return A <code>Line</code> representing this curve, will not be <code>null</code>.
     */
    public Line toRenderableLine(final int steps) {
        return toRenderableLine(1, getControlPointCount() - 2, steps);
    }

    /**
     * Creates a new <code>Line</code> from the given control point indices. It will have the name <code>curve</code>,
     * no normals, colour or texture, these can be added to the returned line if needed.
     * 
     * @param start
     *            The index of the control point to start from, must be greater than or equal to one and less than
     *            <code>end</code>.
     * @param end
     *            The index of the control point to end with, must be less than {@link #getControlPointCount()
     *            controlPointCount} minus one and greater than <code>start</code>.
     * @param steps
     *            The number of iterations to perform between control points, the higher this number the smoother the
     *            returned line will be, but it will also contain more vertices, must be greater than one.
     * @return A <code>Line</code> representing this curve, will not be <code>null</code>.
     */
    public Line toRenderableLine(final int start, final int end, final int steps) {
        final Vector3[] vertex = toVector3(start, end, steps);
        final Vector3[] normal = null;
        final ColorRGBA[] color = null;
        final Vector2[] texture = null;

        final Line line = new Line("curve", vertex, normal, color, texture);

        line.getMeshData().setIndexMode(IndexMode.LineStrip);

        return line;
    }

    /**
     * Calculates the length of this curve.
     * <p>
     * <strong>Important note:</strong><br />
     * To calculate the length of a curve it must be interpolated (hence the steps parameter), this method will do this
     * EVERY time it's called (creating a lot of garbage vectors in the process). This has been done for the sake of
     * keeping this class simple and the code as readable as possible. Therefore the length should be manually cached
     * somewhere in your code if it is going to be used repeatedly.
     * </p>
     * 
     * @param steps
     *            The number of iterations to perform between control points, the higher this number the more accurate
     *            the returned result will be.
     * @return The length of this curve.
     * @see #getApproximateLength(int, int, int)
     */
    public double getApproximateLength(final int steps) {
        return getApproximateLength(1, getControlPointCount() - 2, steps);
    }

    /**
     * Calculates the length between the given control point indices.
     * <p>
     * <strong>Important note:</strong><br />
     * See the Javadoc for the {@link #getApproximateLength(int)} method for important information.
     * </p>
     * 
     * @param start
     *            The index of the control point to start from, must be greater than or equal to one and less than
     *            <code>end</code>.
     * @param end
     *            The index of the control point to end with, must be less than {@link #getControlPointCount()
     *            controlPointCount} minus one and greater than <code>start</code>.
     * @param steps
     *            The number of iterations to perform between control points, the higher this number the more accurate
     *            the returned result will be.
     * @return The length between the given control points.
     * @see #getApproximateLength(int)
     */
    public double getApproximateLength(final int start, final int end, final int steps) {
        double length = 0.0;

        final Vector3[] vectors = toVector3(start, end, steps);

        for (int i = 0; i < (vectors.length - 1); i++) {
            length += vectors[i].distance(vectors[i + 1]);
        }

        return length;
    }

    /**
     * Interpolates between the control points at the given indices.
     * 
     * @param start
     *            The index of the control point to start from.
     * @param end
     *            The index of the control point to end at.
     * @param t
     *            Should be between zero and one. Zero will return point <code>start</code> while one will return
     *            <code>end</code>, a value in between will return an interpolated vector between the two.
     * @return The interpolated vector.
     */
    public ReadOnlyVector3 interpolate(final int start, final int end, final double t) {
        return interpolate(start, end, t, new Vector3());
    }

    /**
     * Interpolates between the control points at the given indices.
     * 
     * @param start
     *            The index of the control point to start from.
     * @param end
     *            The index of the control point to end at.
     * @param t
     *            Should be between zero and one. Zero will return point <code>start</code> while one will return
     *            <code>end</code>, a value in between will return an interpolated vector between the two.
     * @param result
     *            The result of the interpolation will be stored in this vector.
     * @return The result vector as a convenience.
     */
    public ReadOnlyVector3 interpolate(final int start, final int end, final double t, final Vector3 result) {
        if (start <= 0) {
            throw new IllegalArgumentException("start must be > 0! start=" + start);
        }
        if (end >= (getControlPointCount() - 1)) {
            throw new IllegalArgumentException("end must be < " + (getControlPointCount() - 1) + "! end=" + end);
        }

        final List<ReadOnlyVector3> points = getControlPoints();

        return getSpline().interpolate(points.get(start - 1), points.get(start), points.get(end), points.get(end + 1),
                t, result);
    }

    /**
     * @return The number of control points in this curve.
     */
    public int getControlPointCount() {
        return getControlPoints().size();
    }

    /**
     * @param controlPoints
     *            The new control points, can not be <code>null</code>.
     * @see #getControlPoints()
     */
    public void setControlPoints(final List<ReadOnlyVector3> controlPoints) {
        if (null == controlPoints) {
            throw new IllegalArgumentException("controlPoints can not be null!");
        }
        if (controlPoints.size() < 4) {
            throw new IllegalArgumentException("controlPoints must contain at least 4 elements for this class to work!");
        }

        _controlPoints = controlPoints;
    }

    /**
     * @return The control points making up this curve, will not be <code>null</code>.
     * @see #setControlPoints(List)
     */
    public List<ReadOnlyVector3> getControlPoints() {
        assert (null != _controlPoints) : "_controlPoints was null, it must be set before use!";
        assert (_controlPoints.size() >= 4) : "_controlPoints contained less than 4 elements, it must be contain at least 4 for this class to work!";

        return _controlPoints;
    }

    /**
     * @param spline
     *            The new spline, can not be <code>null</code>.
     * @see #getSpline()
     */
    public void setSpline(final Spline spline) {
        if (null == spline) {
            throw new IllegalArgumentException("spline can not be null!");
        }

        _spline = spline;
    }

    /**
     * The default is a {@link CatmullRomSpline}.
     * 
     * @return The spline, will not be <code>null</code>.
     * @see #setSpline(Spline)
     */
    public Spline getSpline() {
        assert (null != _spline) : "_spline was null, it must be set before use!";

        return _spline;
    }

    /**
     * Interpolates the curve and returns an array of vectors.
     */
    private Vector3[] toVector3(final int start, final int end, final int steps) {
        if (start <= 0) {
            throw new IllegalArgumentException("start must be > 0! start=" + start);
        }
        if (end >= (getControlPointCount() - 1)) {
            throw new IllegalArgumentException("end must be < " + (getControlPointCount() - 1) + "! end=" + end);
        }
        if (start >= end) {
            throw new IllegalArgumentException("start must be < end! start=" + start + ", end=" + end);
        }
        if (steps <= 1) {
            throw new IllegalArgumentException("steps must be >= 1! steps=" + steps);
        }

        final List<ReadOnlyVector3> controlPoints = getControlPoints();

        final int count = (end - start) * steps;

        final Vector3[] vectors = new Vector3[count];

        int index = start;

        for (int i = 0; i < count; i++) {
            final int is = i % steps;

            if (0 == is && i >= steps) {
                index++;
            }

            final double t = is / (steps - 1.0);

            final int p0 = index - 1;
            final int p1 = index;
            final int p2 = index + 1;
            final int p3 = index + 2;

            vectors[i] = getSpline().interpolate(controlPoints.get(p0), controlPoints.get(p1), controlPoints.get(p2),
                    controlPoints.get(p3), t);
        }

        return vectors;
    }
}