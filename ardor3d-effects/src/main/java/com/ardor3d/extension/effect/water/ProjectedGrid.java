/**
 * Copyright (c) 2008 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.effect.water;

import java.nio.FloatBuffer;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.ExtendedCamera;
import com.ardor3d.util.Timer;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.Debugger;

/**
 * <code>ProjectedGrid</code> Projected grid mesh
 */
public class ProjectedGrid extends Mesh {
  /** The Constant logger. */
  private static final Logger logger = Logger.getLogger(ProjectedGrid.class.getName());

  private final int sizeX;
  private final int sizeY;

  private FloatBuffer vertBuf;
  private final FloatBuffer normBuf;
  private final FloatBuffer texs;

  private final ExtendedCamera mainCamera = new ExtendedCamera();
  private final Camera projectorCamera = new Camera();

  private final Vector4 origin = new Vector4();
  private final Vector4 direction = new Vector4();
  private final Vector2 source = new Vector2();
  private final Matrix4 rangeMatrix = new Matrix4();

  private final Vector4 intersectBottomLeft = new Vector4();
  private final Vector4 intersectTopLeft = new Vector4();
  private final Vector4 intersectTopRight = new Vector4();
  private final Vector4 intersectBottomRight = new Vector4();

  private final Vector3 planeIntersection = new Vector3();

  public boolean freezeProjector = false;
  private final Timer timer;
  private final Camera camera;

  private final HeightGenerator heightGenerator;
  private final float textureScale;

  private double projectorMinHeight = 100.0;
  private final Vector3[] intersections = new Vector3[24];

  private final float[] vertBufArray;
  private final float[] normBufArray;
  private final float[] texBufArray;

  private int nrUpdateThreads = 1;
  private final ExecutorService executorService = Executors.newCachedThreadPool(new DeamonThreadFactory());
  private final Stack<Future<?>> futureStack = new Stack<>();

  private final int connections[] = {0, 1, 2, 3, 0, 4, 1, 5, 2, 6, 3, 7, 4, 5, 6, 7,};

  // Debug drawing
  private boolean drawDebug = false;

  public ProjectedGrid(final String name, final Camera camera, final int sizeX, final int sizeY,
    final float textureScale, final HeightGenerator heightGenerator, final Timer timer) {
    super(name);
    this.sizeX = sizeX;
    this.sizeY = sizeY;
    this.textureScale = textureScale;
    this.heightGenerator = heightGenerator;
    this.camera = camera;
    this.timer = timer;

    buildVertices(sizeX * sizeY);
    texs = BufferUtils.createVector2Buffer(_meshData.getVertexCount());
    _meshData.setTextureBuffer(texs, 0);
    normBuf = BufferUtils.createVector3Buffer(_meshData.getVertexCount());
    _meshData.setNormalBuffer(normBuf);

    vertBufArray = new float[_meshData.getVertexCount() * 3];
    normBufArray = new float[_meshData.getVertexCount() * 3];
    texBufArray = new float[_meshData.getVertexCount() * 2];

    for (int i = 0; i < 24; i++) {
      intersections[i] = new Vector3();
    }
  }

  public void setNrUpdateThreads(final int nrUpdateThreads) {
    this.nrUpdateThreads = nrUpdateThreads;
    if (this.nrUpdateThreads < 1) {
      this.nrUpdateThreads = 1;
    }
  }

  public int getNrUpdateThreads() { return nrUpdateThreads; }

  public void setFreezeUpdate(final boolean freeze) { freezeProjector = freeze; }

  public boolean isFreezeUpdate() { return freezeProjector; }

  @Override
  public boolean render(final Renderer renderer) {
    final boolean doDraw = update();
    boolean drew = false;
    if (doDraw) {
      drew = super.render(renderer);
    }

    if (drawDebug) {
      Debugger.drawCameraFrustum(renderer, mainCamera, new ColorRGBA(1, 0, 0, 1), (short) 0xFFFF, true);
      Debugger.drawCameraFrustum(renderer, projectorCamera, new ColorRGBA(0, 1, 1, 1), (short) 0xFFFF, true);
    }

    return drew;
  }

  public boolean update() {
    final double upperBound = heightGenerator.getMaximumHeight();

    if (!freezeProjector) {
      mainCamera.set(camera);

      final Vector3 tmp = new Vector3();
      getWorldTransform().applyInverse(mainCamera.getLocation(), tmp);
      mainCamera.setLocation(tmp);
      getWorldTransform().applyInverseVector(mainCamera.getLeft(), tmp);
      mainCamera.setLeft(tmp);
      getWorldTransform().applyInverseVector(mainCamera.getUp(), tmp);
      mainCamera.setUp(tmp);
      getWorldTransform().applyInverseVector(mainCamera.getDirection(), tmp);
      mainCamera.setDirection(tmp);
    }

    final ReadOnlyVector3 mainCameraLocation = mainCamera.getLocation();
    if (mainCameraLocation.getY() > 0.0 && mainCameraLocation.getY() < upperBound + mainCamera.getFrustumNear()) {
      mainCamera.setLocation(mainCameraLocation.getX(), upperBound + mainCamera.getFrustumNear(),
          mainCameraLocation.getZ());
    } else if (mainCameraLocation.getY() < 0.0
        && mainCameraLocation.getY() > -upperBound - mainCamera.getFrustumNear()) {
      mainCamera.setLocation(mainCameraLocation.getX(), -upperBound - mainCamera.getFrustumNear(),
          mainCameraLocation.getZ());
    }
    mainCamera.calculateFrustum();
    final Vector3[] corners = mainCamera.getCorners();

    int nrPoints = 0;

    // check intersections of frustum connections with upper and lower bound
    final Vector3 tmpStorage = Vector3.fetchTempInstance();
    for (int i = 0; i < 8; i++) {
      final int source = connections[i * 2];
      final int destination = connections[i * 2 + 1];

      if (corners[source].getY() > upperBound && corners[destination].getY() < upperBound
          || corners[source].getY() < upperBound && corners[destination].getY() > upperBound) {
        getWorldIntersection(upperBound, corners[source], corners[destination], intersections[nrPoints++], tmpStorage);
      }
      if (corners[source].getY() > -upperBound && corners[destination].getY() < -upperBound
          || corners[source].getY() < -upperBound && corners[destination].getY() > -upperBound) {
        getWorldIntersection(-upperBound, corners[source], corners[destination], intersections[nrPoints++], tmpStorage);
      }
    }
    // check if any of the frustums corner vertices lie between the upper and lower bound planes
    for (int i = 0; i < 8; i++) {
      if (corners[i].getY() < upperBound && corners[i].getY() > -upperBound) {
        intersections[nrPoints++].set(corners[i]);
      }
    }

    if (nrPoints == 0) {
      // No intersection, grid not visible
      return false;
    }

    // set projector
    projectorCamera.set(mainCamera);

    // force the projector to point at the plane
    if (projectorCamera.getLocation().getY() > 0.0 && projectorCamera.getDirection().getY() > 0.0
        || projectorCamera.getLocation().getY() < 0.0 && projectorCamera.getDirection().getY() < 0.0) {
      projectorCamera.setDirection(new Vector3(projectorCamera.getDirection().getX(),
          -projectorCamera.getDirection().getY(), projectorCamera.getDirection().getZ()));
      projectorCamera.setUp(projectorCamera.getDirection().cross(projectorCamera.getLeft(), null).normalizeLocal());
    }

    // find the plane intersection point
    source.set(0.5, 0.5);
    getWorldIntersection(0.0, source, projectorCamera.getModelViewProjectionInverseMatrix(), planeIntersection);

    // force the projector to be a certain distance above the plane
    final ReadOnlyVector3 cameraLocation = projectorCamera.getLocation();
    if (cameraLocation.getY() > 0.0 && cameraLocation.getY() < projectorMinHeight * 2) {
      final double delta = (projectorMinHeight * 2 - cameraLocation.getY()) / (projectorMinHeight * 2);

      projectorCamera.setLocation(cameraLocation.getX(), projectorMinHeight * 2 - projectorMinHeight * delta,
          cameraLocation.getZ());
    } else if (cameraLocation.getY() < 0.0 && cameraLocation.getY() > -projectorMinHeight * 2) {
      final double delta = (-projectorMinHeight * 2 - cameraLocation.getY()) / (-projectorMinHeight * 2);

      projectorCamera.setLocation(cameraLocation.getX(), -projectorMinHeight * 2 + projectorMinHeight * delta,
          cameraLocation.getZ());
    }

    // restrict the intersection point to be a certain distance from the camera in plane coords
    planeIntersection.subtractLocal(projectorCamera.getLocation());
    planeIntersection.setY(0.0);
    final double length = planeIntersection.length();
    if (length > Math.abs(projectorCamera.getLocation().getY())) {
      planeIntersection.normalizeLocal();
      planeIntersection.multiplyLocal(Math.abs(projectorCamera.getLocation().getY()));
    } else if (length < MathUtils.EPSILON) {
      planeIntersection.addLocal(projectorCamera.getUp());
      planeIntersection.setY(0.0);
      planeIntersection.normalizeLocal();
      planeIntersection.multiplyLocal(0.1); // TODO: magic number
    }
    planeIntersection.addLocal(projectorCamera.getLocation());
    planeIntersection.setY(0.0);

    // point projector at the new intersection point
    projectorCamera.lookAt(planeIntersection, Vector3.UNIT_Y);

    // transform points to projector space
    final ReadOnlyMatrix4 modelViewProjectionMatrix = projectorCamera.getModelViewProjectionMatrix();
    final Vector4 spaceTransformation = new Vector4();
    for (int i = 0; i < nrPoints; i++) {
      spaceTransformation.set(intersections[i].getX(), 0.0, intersections[i].getZ(), 1.0);
      modelViewProjectionMatrix.applyPre(spaceTransformation, spaceTransformation);
      intersections[i].set(spaceTransformation.getX(), spaceTransformation.getY(), 0);
      intersections[i].divideLocal(spaceTransformation.getW());
    }

    // find min/max in projector space
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    double maxY = -Double.MAX_VALUE;
    for (int i = 0; i < nrPoints; i++) {
      if (intersections[i].getX() < minX) {
        minX = intersections[i].getX();
      }
      if (intersections[i].getX() > maxX) {
        maxX = intersections[i].getX();
      }
      if (intersections[i].getY() < minY) {
        minY = intersections[i].getY();
      }
      if (intersections[i].getY() > maxY) {
        maxY = intersections[i].getY();
      }
    }

    // create range matrix
    rangeMatrix.setIdentity();
    rangeMatrix.setM00(maxX - minX);
    rangeMatrix.setM11(maxY - minY);
    rangeMatrix.setM30(minX);
    rangeMatrix.setM31(minY);

    final ReadOnlyMatrix4 modelViewProjectionInverseMatrix = projectorCamera.getModelViewProjectionInverseMatrix();
    rangeMatrix.multiplyLocal(modelViewProjectionInverseMatrix);

    // convert screen coords to homogenous world coords with new range matrix
    source.set(0.5, 0.5);
    getWorldIntersectionHomogenous(0.0, source, rangeMatrix, intersectBottomLeft);
    source.set(0.5, 1);
    getWorldIntersectionHomogenous(0.0, source, rangeMatrix, intersectTopLeft);
    source.set(1, 1);
    getWorldIntersectionHomogenous(0.0, source, rangeMatrix, intersectTopRight);
    source.set(1, 0.5);
    getWorldIntersectionHomogenous(0.0, source, rangeMatrix, intersectBottomRight);

    // update data
    if (nrUpdateThreads <= 1) {
      updateGrid(0, sizeY);
    } else {
      for (int i = 0; i < nrUpdateThreads; i++) {
        final int from = sizeY * i / (nrUpdateThreads);
        final int to = sizeY * (i + 1) / (nrUpdateThreads);
        final Future<?> future = executorService.submit(() -> updateGrid(from, to));
        futureStack.push(future);
      }
      try {
        while (!futureStack.isEmpty()) {
          futureStack.pop().get();
        }
      } catch (final InterruptedException ex) {
        logger.log(Level.SEVERE, "InterruptedException in thread execution", ex);
      } catch (final ExecutionException ex) {
        logger.log(Level.SEVERE, "ExecutionException in thread execution", ex);
      }
    }

    vertBuf.rewind();
    vertBuf.put(vertBufArray);

    texs.rewind();
    texs.put(texBufArray);

    normBuf.rewind();
    normBuf.put(normBufArray);

    getMeshData().markBufferDirty(MeshData.KEY_VertexCoords);
    getMeshData().markBufferDirty(MeshData.KEY_NormalCoords);
    getMeshData().markBufferDirty(MeshData.KEY_TextureCoords0);

    return true;
  }

  private boolean getWorldIntersection(final double planeHeight, final Vector3 source, final Vector3 destination,
      final Vector3 store, final Vector3 tmpStorage) {
    final Vector3 origin = store.set(source);
    final Vector3 direction = tmpStorage.set(destination).subtractLocal(origin);

    final double t = (planeHeight - origin.getY()) / (direction.getY());

    direction.multiplyLocal(t);
    origin.addLocal(direction);

    return t >= 0.0 && t <= 1.0;
  }

  private void updateGrid(final int from, final int to) {
    final double time = timer.getTimeInSeconds();
    final double du = 1.0f / (double) (sizeX - 1);
    final double dv = 1.0f / (double) (sizeY - 1);

    final Vector4 pointTop = Vector4.fetchTempInstance();
    final Vector4 pointFinal = Vector4.fetchTempInstance();
    final Vector4 pointBottom = Vector4.fetchTempInstance();

    int smallerFrom = from;
    if (smallerFrom > 0) {
      smallerFrom--;
    }
    int biggerTo = to;
    if (biggerTo < sizeY) {
      biggerTo++;
    }
    double u = 0, v = smallerFrom * dv;
    int index = smallerFrom * sizeX * 3;
    for (int y = smallerFrom; y < biggerTo; y++) {
      for (int x = 0; x < sizeX; x++) {
        pointTop.lerpLocal(intersectTopLeft, intersectTopRight, u);
        pointBottom.lerpLocal(intersectBottomLeft, intersectBottomRight, u);
        pointFinal.lerpLocal(pointTop, pointBottom, v);

        pointFinal.setX(pointFinal.getX() / pointFinal.getW());
        pointFinal.setZ(pointFinal.getZ() / pointFinal.getW());
        pointFinal.setY(heightGenerator.getHeight(pointFinal.getX(), pointFinal.getZ(), time));

        vertBufArray[index++] = pointFinal.getXf();
        vertBufArray[index++] = pointFinal.getYf();
        vertBufArray[index++] = pointFinal.getZf();

        u += du;
      }
      v += dv;
      u = 0;
    }

    Vector4.releaseTempInstance(pointTop);
    Vector4.releaseTempInstance(pointFinal);
    Vector4.releaseTempInstance(pointBottom);

    final Vector3 oppositePoint = Vector3.fetchTempInstance();
    final Vector3 adjacentPoint = Vector3.fetchTempInstance();
    final Vector3 rootPoint = Vector3.fetchTempInstance();

    int adj = 0, opp = 0;
    int normalIndex = from * sizeX;
    for (int row = from; row < to; row++) {
      for (int col = 0; col < sizeX; col++) {
        if (row == sizeY - 1) {
          if (col == sizeX - 1) { // last row, last col
            // up cross left
            adj = normalIndex - sizeX;
            opp = normalIndex - 1;
          } else { // last row, except for last col
            // right cross up
            adj = normalIndex + 1;
            opp = normalIndex - sizeX;
          }
        } else {
          if (col == sizeX - 1) { // last column except for last row
            // left cross down
            adj = normalIndex - 1;
            opp = normalIndex + sizeX;
          } else { // most cases
            // down cross right
            adj = normalIndex + sizeX;
            opp = normalIndex + 1;
          }
        }

        final float x = vertBufArray[normalIndex * 3];
        final float y = vertBufArray[normalIndex * 3 + 1];
        final float z = vertBufArray[normalIndex * 3 + 2];

        texBufArray[normalIndex * 2] = x * textureScale;
        texBufArray[normalIndex * 2 + 1] = z * textureScale;

        rootPoint.set(x, y, z);
        adjacentPoint.set(vertBufArray[adj * 3], vertBufArray[adj * 3 + 1], vertBufArray[adj * 3 + 2]);
        adjacentPoint.subtractLocal(rootPoint);
        oppositePoint.set(vertBufArray[opp * 3], vertBufArray[opp * 3 + 1], vertBufArray[opp * 3 + 2]);
        oppositePoint.subtractLocal(rootPoint);

        adjacentPoint.crossLocal(oppositePoint).normalizeLocal();

        normBufArray[normalIndex * 3] = adjacentPoint.getXf();
        normBufArray[normalIndex * 3 + 1] = adjacentPoint.getYf();
        normBufArray[normalIndex * 3 + 2] = adjacentPoint.getZf();

        normalIndex++;
      }
    }

    Vector3.releaseTempInstance(oppositePoint);
    Vector3.releaseTempInstance(adjacentPoint);
    Vector3.releaseTempInstance(rootPoint);
  }

  private void getWorldIntersectionHomogenous(final double planeHeight, final ReadOnlyVector2 screenPosition,
      final ReadOnlyMatrix4 modelViewProjectionInverseMatrix, final Vector4 store) {
    calculateIntersection(planeHeight, screenPosition, modelViewProjectionInverseMatrix);
    store.set(origin);
  }

  private void getWorldIntersection(final double planeHeight, final ReadOnlyVector2 screenPosition,
      final ReadOnlyMatrix4 modelViewProjectionInverseMatrix, final Vector3 store) {
    calculateIntersection(planeHeight, screenPosition, modelViewProjectionInverseMatrix);
    store.set(origin.getX(), origin.getY(), origin.getZ()).divideLocal(origin.getW());
  }

  private void calculateIntersection(final double planeHeight, final ReadOnlyVector2 screenPosition,
      final ReadOnlyMatrix4 modelViewProjectionInverseMatrix) {
    origin.set(screenPosition.getX() * 2 - 1, screenPosition.getY() * 2 - 1, -1, 1);
    direction.set(screenPosition.getX() * 2 - 1, screenPosition.getY() * 2 - 1, 1, 1);

    modelViewProjectionInverseMatrix.applyPre(origin, origin);
    modelViewProjectionInverseMatrix.applyPre(direction, direction);

    direction.subtractLocal(origin);

    // final double t = (planeHeight * origin.getW() - origin.getY())
    // / (direction.getY() - planeHeight * direction.getW());

    if (Math.abs(direction.getY()) > MathUtils.EPSILON) {
      final double t = (planeHeight - origin.getY()) / direction.getY();
      direction.multiplyLocal(t);
    } else {
      direction.normalizeLocal();
      direction.multiplyLocal(mainCamera.getFrustumFar());
    }

    origin.addLocal(direction);
  }

  /**
   * <code>getSurfaceNormal</code> returns the normal of an arbitrary point on the terrain. The normal
   * is linearly interpreted from the normals of the 4 nearest defined points. If the point provided
   * is not within the bounds of the height map, null is returned.
   *
   * @param position
   *          the vector representing the location to find a normal at.
   * @param store
   *          the Vector3 object to store the result in. If null, a new one is created.
   * @return the normal vector at the provided location.
   */
  public Vector3 getSurfaceNormal(final Vector2 position, final Vector3 store) {
    return getSurfaceNormal(position.getX(), position.getY(), store);
  }

  /**
   * <code>getSurfaceNormal</code> returns the normal of an arbitrary point on the terrain. The normal
   * is linearly interpreted from the normals of the 4 nearest defined points. If the point provided
   * is not within the bounds of the height map, null is returned.
   *
   * @param position
   *          the vector representing the location to find a normal at. Only the x and z values are
   *          used.
   * @param store
   *          the Vector3 object to store the result in. If null, a new one is created.
   * @return the normal vector at the provided location.
   */
  public Vector3 getSurfaceNormal(final Vector3 position, final Vector3 store) {
    return getSurfaceNormal(position.getX(), position.getZ(), store);
  }

  /**
   * <code>getSurfaceNormal</code> returns the normal of an arbitrary point on the terrain. The normal
   * is linearly interpreted from the normals of the 4 nearest defined points. If the point provided
   * is not within the bounds of the height map, null is returned.
   *
   * @param x
   *          the x coordinate to check.
   * @param z
   *          the z coordinate to check.
   * @param store
   *          the Vector3 object to store the result in. If null, a new one is created.
   * @return the normal unit vector at the provided location.
   */
  public Vector3 getSurfaceNormal(final double x, final double z, Vector3 store) {
    final double col = MathUtils.floor(x);
    final double row = MathUtils.floor(z);

    if (col < 0 || row < 0 || col >= sizeX - 1 || row >= sizeY - 1) {
      return null;
    }
    final double intOnX = x - col, intOnZ = z - row;

    if (store == null) {
      store = new Vector3();
    }

    final Vector3 topLeft = store, topRight = new Vector3(), bottomLeft = new Vector3(), bottomRight = new Vector3();

    final int focalSpot = (int) (col + row * sizeX);

    // find the heightmap point closest to this position (but will always
    // be to the left ( < x) and above (< z) of the spot.
    BufferUtils.populateFromBuffer(topLeft, normBuf, focalSpot);

    // now find the next point to the right of topLeft's position...
    BufferUtils.populateFromBuffer(topRight, normBuf, focalSpot + 1);

    // now find the next point below topLeft's position...
    BufferUtils.populateFromBuffer(bottomLeft, normBuf, focalSpot + sizeX);

    // now find the next point below and to the right of topLeft's
    // position...
    BufferUtils.populateFromBuffer(bottomRight, normBuf, focalSpot + sizeX + 1);

    // Use linear interpolation to find the height.
    topLeft.lerpLocal(topRight, intOnX);
    bottomLeft.lerpLocal(bottomRight, intOnX);
    topLeft.lerpLocal(bottomLeft, intOnZ);
    return topLeft.normalizeLocal();
  }

  /**
   * <code>buildVertices</code> sets up the vertex and index arrays of the TriMesh.
   */
  private void buildVertices(final int vertexCount) {
    vertBuf = BufferUtils.createVector3Buffer(vertBuf, vertexCount);
    _meshData.setVertexBuffer(vertBuf);

    final Vector3 point = new Vector3();
    for (int x = 0; x < sizeX; x++) {
      for (int y = 0; y < sizeY; y++) {
        point.set(x, 0, y);
        BufferUtils.setInBuffer(point, vertBuf, (x + (y * sizeX)));
      }
    }

    // set up the indices
    final int triangleQuantity = ((sizeX - 1) * (sizeY - 1)) * 2;
    final IndexBufferData<?> indices = BufferUtils.createIndexBufferData(triangleQuantity * 3, vertexCount - 1);
    _meshData.setIndices(indices);

    // go through entire array up to the second to last column.
    for (int i = 0; i < (sizeX * (sizeY - 1)); i++) {
      // we want to skip the top row.
      if (i % ((sizeX * (i / sizeX + 1)) - 1) == 0 && i != 0) {
        // logger.info("skip row: "+i+" cause: "+((sizeY * (i / sizeX + 1)) - 1));
        continue;
      } else {
        // logger.info("i: "+i);
      }
      // set the top left corner.
      indices.put(i);
      // set the bottom right corner.
      indices.put((1 + sizeX) + i);
      // set the top right corner.
      indices.put(1 + i);
      // set the top left corner
      indices.put(i);
      // set the bottom left corner
      indices.put(sizeX + i);
      // set the bottom right corner
      indices.put((1 + sizeX) + i);
    }
  }

  static class DeamonThreadFactory implements ThreadFactory {
    static final AtomicInteger poolNumber = new AtomicInteger(1);
    final ThreadGroup group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;

    DeamonThreadFactory() {
      final SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
      namePrefix = "ProjectedGrid Pool-" + poolNumber.getAndIncrement() + "-thread-";
    }

    @Override
    public Thread newThread(final Runnable r) {
      final Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
      if (!t.isDaemon()) {
        t.setDaemon(true);
      }
      if (t.getPriority() != Thread.NORM_PRIORITY) {
        t.setPriority(Thread.NORM_PRIORITY);
      }
      return t;
    }
  }

  public double getProjectorMinHeight() { return projectorMinHeight; }

  public void setProjectorMinHeight(final double projectorMinHeight) { this.projectorMinHeight = projectorMinHeight; }

  public boolean isDrawDebug() { return drawDebug; }

  public void setDrawDebug(final boolean drawDebug) { this.drawDebug = drawDebug; }
}
