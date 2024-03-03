/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.scenegraph.shape;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;

/**
 * An extrusion of a 2D object ({@link Line}) along a path (List of Vector3). Either a convenience
 * constructor can be used or the {@link #updateGeometry} method. It is also capable of doing a
 * cubic spline interpolation for a list of supporting points
 */
public class Extrusion extends Mesh {

  /**
   * Default Constructor. Creates an empty Extrusion.
   * 
   * @see #updateGeometry(Line, List, Vector3)
   * @see #updateGeometry(Line, List, boolean, Vector3)
   * @see #updateGeometry(Line, List, int, Vector3)
   * @see #updateGeometry(Line, List, int, boolean, Vector3)
   */
  public Extrusion() {}

  /**
   * Creates an empty named Extrusion.
   * 
   * @param name
   *          name
   * @see #updateGeometry(Line, List, Vector3)
   * @see #updateGeometry(Line, List, boolean, Vector3)
   * @see #updateGeometry(Line, List, int, Vector3)
   * @see #updateGeometry(Line, List, int, boolean, Vector3)
   */
  public Extrusion(final String name) {
    super(name);
  }

  /**
   * Convenience constructor. Calls {@link #updateGeometry(Line, List, Vector3)}.
   * 
   * @param shape
   *          see {@link #updateGeometry(Line, List, Vector3)}
   * @param path
   *          see {@link #updateGeometry(Line, List, Vector3)}
   * @param up
   *          up vector
   */
  public Extrusion(final Line shape, final List<ReadOnlyVector3> path, final ReadOnlyVector3 up) {
    updateGeometry(shape, path, up);
  }

  /**
   * Convenience constructor. Sets the name and calls {@link #updateGeometry(Line, List, Vector3)}.
   * 
   * @param name
   *          name
   * @param shape
   *          see {@link #updateGeometry(Line, List, Vector3)}
   * @param path
   *          see {@link #updateGeometry(Line, List, Vector3)}
   * @param up
   *          up vector
   */
  public Extrusion(final String name, final Line shape, final List<ReadOnlyVector3> path, final ReadOnlyVector3 up) {
    super(name);
    updateGeometry(shape, path, up);
  }

  /**
   * Update vertex, color, index and texture buffers (0) to contain an extrusion of shape along path.
   * 
   * @param shape
   *          an instance of Line that describes the 2D shape
   * @param path
   *          a list of vectors that describe the path the shape should be extruded
   * @param up
   *          up vector
   */
  public void updateGeometry(final Line shape, final List<ReadOnlyVector3> path, final ReadOnlyVector3 up) {
    updateGeometry(shape, path, false, up);
  }

  /**
   * Update vertex, color, index and texture buffers (0) to contain an extrusion of shape along path.
   * 
   * @param shape
   *          an instance of Line that describes the 2D shape
   * @param path
   *          a list of vectors that describe the path the shape should be extruded
   * @param closed
   *          true to connect first and last point
   * @param up
   *          up vector
   */
  public void updateGeometry(final Line shape, final List<ReadOnlyVector3> path, final boolean closed,
      final ReadOnlyVector3 up) {
    final FloatBuffer shapeBuffer = shape.getMeshData().getVertexBuffer();
    final FloatBuffer shapeNormalBuffer = shape.getMeshData().getNormalBuffer();

    final int numVertices = path.size() * shapeBuffer.limit();

    FloatBuffer vertices;
    if (_meshData.getVertexBuffer() != null && _meshData.getVertexBuffer().limit() == numVertices) {
      vertices = _meshData.getVertexBuffer();
      vertices.rewind();
    } else {
      vertices = BufferUtils.createFloatBuffer(numVertices);
    }

    FloatBuffer normals = null;
    if (shapeNormalBuffer != null) {
      if (_meshData.getNormalBuffer() != null && _meshData.getNormalBuffer().limit() == numVertices) {
        normals = _meshData.getNormalBuffer();
        normals.rewind();
      } else {
        normals = BufferUtils.createFloatBuffer(numVertices);
      }
    }

    final int numIndices = (path.size() - 1) * 2 * shapeBuffer.limit();
    IndexBufferData<?> indices;
    if (_meshData.getIndices() != null && _meshData.getIndices().limit() == numIndices) {
      indices = _meshData.getIndices();
      indices.rewind();
    } else {
      indices = BufferUtils.createIndexBufferData(numIndices, numVertices - 1);
    }

    final IndexMode indexMode = shape.getMeshData().getIndexMode(0);

    final int shapeVertices = shapeBuffer.limit() / 3;
    final Vector3 vector = new Vector3();
    final Vector3 direction = new Vector3();
    final Quaternion rotation = new Quaternion();

    for (int i = 0; i < path.size(); i++) {
      final ReadOnlyVector3 point = path.get(i);
      shapeBuffer.rewind();
      if (shapeNormalBuffer != null) {
        shapeNormalBuffer.rewind();
      }
      int shapeVertice = 0;
      do {
        final ReadOnlyVector3 nextPoint = i < path.size() - 1 ? path.get(i + 1) : closed ? path.get(0) : null;
        final ReadOnlyVector3 lastPoint = i > 0 ? path.get(i - 1) : null;
        if (nextPoint != null) {
          direction.set(nextPoint).subtractLocal(point);
        } else {
          direction.set(point).subtractLocal(lastPoint);
        }
        rotation.lookAt(direction, up);

        if (shapeNormalBuffer != null && normals != null) {
          vector.set(shapeNormalBuffer.get(), shapeNormalBuffer.get(), shapeNormalBuffer.get());
          rotation.apply(vector, vector);
          normals.put(vector.getXf());
          normals.put(vector.getYf());
          normals.put(vector.getZf());
        }

        vector.set(shapeBuffer.get(), shapeBuffer.get(), shapeBuffer.get());
        rotation.apply(vector, vector);
        vector.addLocal(point);
        vertices.put(vector.getXf());
        vertices.put(vector.getYf());
        vertices.put(vector.getZf());

        if (indexMode != IndexMode.Lines || (shapeVertice & 1) == 0) {
          if (i < path.size() - 1) {
            if (shapeBuffer.hasRemaining()) {
              indices.put(i * shapeVertices + shapeVertice);
              indices.put(i * shapeVertices + shapeVertice + 1);
              indices.put((i + 1) * shapeVertices + shapeVertice);

              indices.put((i + 1) * shapeVertices + shapeVertice + 1);
              indices.put((i + 1) * shapeVertices + shapeVertice);
              indices.put(i * shapeVertices + shapeVertice + 1);
            } else if (indexMode == IndexMode.LineLoop) {
              indices.put(i * shapeVertices + shapeVertice);
              indices.put(i * shapeVertices + shapeVertice + 1);
              indices.put((i + 1) * shapeVertices + shapeVertice);

              indices.put(i * shapeVertices + shapeVertice);
              indices.put((i - 1) * shapeVertices + shapeVertice + 1);
              indices.put(i * shapeVertices + shapeVertice + 1);
            }
          } else if (closed) {
            indices.put(i * shapeVertices + shapeVertice);
            indices.put(i * shapeVertices + shapeVertice + 1);
            indices.put(0 + shapeVertice);

            indices.put(0 + shapeVertice + 1);
            indices.put(0 + shapeVertice);
            indices.put(i * shapeVertices + shapeVertice + 1);
          }
        }
        shapeVertice++;
      } while (shapeBuffer.hasRemaining());
    }

    _meshData.setVertexBuffer(vertices);
    if (normals != null) {
      _meshData.setNormalBuffer(normals);
    }
    _meshData.setIndices(indices);
  }

  /**
   * Performs cubic spline interpolation to find a path through the supporting points where the second
   * derivative is zero. Then calls {@link #updateGeometry(Line, List, Vector3)} with this path.
   * 
   * @param shape
   *          an instance of Line that describes the 2D shape
   * @param points
   *          a list of supporting points for the spline interpolation
   * @param segments
   *          number of resulting path segments per supporting point
   * @param up
   *          up vector
   */
  public void updateGeometry(final Line shape, final List<ReadOnlyVector3> points, final int segments,
      final ReadOnlyVector3 up) {
    updateGeometry(shape, points, segments, false, up);
  }

  /**
   * Performs cubic spline interpolation to find a path through the supporting points where the second
   * derivative is zero. Then calls {@link #updateGeometry(Line, List, boolean, Vector3)} with this
   * path.
   * 
   * @param shape
   *          an instance of Line that describes the 2D shape
   * @param points
   *          a list of supporting points for the spline interpolation
   * @param segments
   *          number of resulting path segments per supporting point
   * @param closed
   *          true to close the shape (connect last and first point)
   * @param up
   *          up vector
   */
  public void updateGeometry(final Line shape, final List<ReadOnlyVector3> points, final int segments,
      final boolean closed, final ReadOnlyVector3 up) {
    int np = points.size(); // number of points
    if (closed) {
      np = np + 3;
    }
    final double[][] d = new double[3][np]; // Newton form coefficients
    final double[] x = new double[np]; // x-coordinates of nodes

    final List<ReadOnlyVector3> path = new ArrayList<>();

    for (int i = 0; i < np; i++) {
      ReadOnlyVector3 p;
      if (!closed) {
        p = points.get(i);
      } else {
        if (i == 0) {
          p = points.get(points.size() - 1);
        } else if (i >= np - 2) {
          p = points.get(i - np + 2);
        } else {
          p = points.get(i - 1);
        }
      }
      x[i] = i;
      d[0][i] = p.getX();
      d[1][i] = p.getY();
      d[2][i] = p.getZ();
    }

    if (np > 1) {
      final double[][] a = new double[3][np];
      final double[] h = new double[np];
      for (int i = 1; i <= np - 1; i++) {
        h[i] = x[i] - x[i - 1];
      }
      if (np > 2) {
        final double[] sub = new double[np - 1];
        final double[] diag = new double[np - 1];
        final double[] sup = new double[np - 1];

        for (int i = 1; i <= np - 2; i++) {
          diag[i] = (h[i] + h[i + 1]) / 3;
          sup[i] = h[i + 1] / 6;
          sub[i] = h[i] / 6;
          for (int dim = 0; dim < 3; dim++) {
            a[dim][i] = (d[dim][i + 1] - d[dim][i]) / h[i + 1] - (d[dim][i] - d[dim][i - 1]) / h[i];
          }
        }
        for (int dim = 0; dim < 3; dim++) {
          solveTridiag(sub.clone(), diag.clone(), sup.clone(), a[dim], np - 2);
        }
      }
      // note that a[0]=a[np-1]=0
      // draw
      if (!closed) {
        path.add(new Vector3(d[0][0], d[1][0], d[2][0]));
      }
      final double[] point = new double[3];
      for (int i = closed ? 2 : 1; i <= np - 2; i++) { // loop over
        // intervals
        // between nodes
        for (int j = 1; j <= segments; j++) {
          for (int dim = 0; dim < 3; dim++) {
            final double t1 = (h[i] * j) / segments;
            final double t2 = h[i] - t1;
            final double v = ((-a[dim][i - 1] / 6 * (t2 + h[i]) * t1 + d[dim][i - 1]) * t2
                + (-a[dim][i] / 6 * (t1 + h[i]) * t2 + d[dim][i]) * t1) / h[i];
            // float t = x[i - 1] + t1;
            point[dim] = v;
          }
          path.add(new Vector3(point[0], point[1], point[2]));
        }
      }
    }

    this.updateGeometry(shape, path, closed, up);
  }

  /*
   * solve linear system with tridiagonal n by n matrix a using Gaussian elimination without pivoting
   * where a(i,i-1) = sub[i] for 2<=i<=n a(i,i) = diag[i] for 1<=i<=n a(i,i+1) = sup[i] for 1<=i<=n-1
   * (the values sub[1], sup[n] are ignored) right hand side vector b[1:n] is overwritten with
   * solution NOTE: 1...n is used in all arrays, 0 is unused
   */
  private static void solveTridiag(final double[] sub, final double[] diag, final double[] sup, final double[] b,
                                   final int n) {
    // factorization and forward substitution
    for (int i = 2; i <= n; i++) {
      sub[i] = sub[i] / diag[i - 1];
      diag[i] = diag[i] - sub[i] * sup[i - 1];
      b[i] = b[i] - sub[i] * b[i - 1];
    }
    b[n] = b[n] / diag[n];
    for (int i = n - 1; i >= 1; i--) {
      b[i] = (b[i] - sup[i] * b[i + 1]) / diag[i];
    }
  }
}
