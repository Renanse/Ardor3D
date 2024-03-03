/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.util.geom;

import java.nio.FloatBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.bounding.OrientedBoundingBox;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.buffer.IndexBufferData;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.uniform.AlphaTestConsts;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.NormalsMode;
import com.ardor3d.scenegraph.shape.AxisRods;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.OrientedBox;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.ExtendedCamera;
import com.ardor3d.util.MaterialUtil;

/**
 * Debugger provides tools for viewing scene data such as boundings and normals.
 *
 * Make sure you set the RenderStateFactory before using this class.
 *
 * @see Debugger#setRenderStateFactory(RenderStateFactory)
 */
public final class Debugger {

  // -- **** METHODS FOR DRAWING BOUNDING VOLUMES **** -- //

  private static final Sphere boundingSphere = new Sphere("bsphere", 10, 10, 1);
  static {
    Debugger.boundingSphere.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    Debugger.boundingSphere.getSceneHints().setNormalsMode(NormalsMode.Off);
    Debugger.boundingSphere.setRenderState(new WireframeState());
    Debugger.boundingSphere.setRenderState(new ZBufferState());
    Debugger.boundingSphere.updateWorldRenderStates(false);
  }
  private static final Box boundingBox = new Box("bbox", new Vector3(), 1, 1, 1);
  static {
    Debugger.boundingBox.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    Debugger.boundingBox.getSceneHints().setNormalsMode(NormalsMode.Off);
    Debugger.boundingBox.setRenderState(new WireframeState());
    Debugger.boundingBox.setRenderState(new ZBufferState());
    Debugger.boundingBox.updateWorldRenderStates(false);
  }
  private static final OrientedBox boundingOB = new OrientedBox("bobox");
  static {
    Debugger.boundingOB.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    Debugger.boundingOB.getSceneHints().setNormalsMode(NormalsMode.Off);
    Debugger.boundingOB.setRenderState(new WireframeState());
    Debugger.boundingOB.setRenderState(new ZBufferState());
    Debugger.boundingOB.updateWorldRenderStates(false);
  }

  /**
   * <code>drawBounds</code> draws the bounding volume for a given Spatial and its children.
   *
   * @param se
   *          the Spatial to draw boundings for.
   * @param r
   *          the Renderer to use to draw the bounding.
   */
  public static void drawBounds(final Spatial se, final Renderer r) {
    drawBounds(se, r, true);
  }

  /**
   * <code>drawBounds</code> draws the bounding volume for a given Spatial and optionally its
   * children.
   *
   * @param se
   *          the Spatial to draw boundings for.
   * @param r
   *          the Renderer to use to draw the bounding.
   * @param doChildren
   *          if true, boundings for any children will also be drawn
   */
  public static void drawBounds(final Spatial se, final Renderer r, boolean doChildren) {
    if (se == null) {
      return;
    }

    if (se.getWorldBound() != null && se.getSceneHints().getCullHint() != CullHint.Always) {
      final Camera cam = Camera.getCurrentCamera();
      final int state = cam.getPlaneState();
      if (cam.contains(se.getWorldBound()) != Camera.FrustumIntersect.Outside) {
        drawBounds(se.getWorldBound(), r);
      } else {
        doChildren = false;
      }
      cam.setPlaneState(state);
    }
    if (doChildren && se instanceof Node) {
      final Node n = (Node) se;
      if (n.getNumberOfChildren() != 0) {
        for (int i = n.getNumberOfChildren(); --i >= 0;) {
          drawBounds(n.getChild(i), r, true);
        }
      }
    }
  }

  public static void drawBounds(final BoundingVolume bv, final Renderer r) {

    switch (bv.getType()) {
      case AABB:
        drawBoundingBox((BoundingBox) bv, r);
        break;
      case Sphere:
        drawBoundingSphere((BoundingSphere) bv, r);
        break;
      case OBB:
        drawOBB((OrientedBoundingBox) bv, r);
        break;
      default:
        break;
    }
  }

  public static void setBoundsColor(final ReadOnlyColorRGBA color) {
    Debugger.boundingBox.setDefaultColor(color);
    Debugger.boundingOB.setDefaultColor(color);
    Debugger.boundingSphere.setDefaultColor(color);
  }

  public static void drawBoundingSphere(final BoundingSphere sphere, final Renderer r) {
    Debugger.boundingSphere.setData(sphere.getCenter(), 10, 10, sphere.getRadius());
    Debugger.boundingSphere.draw(r);
  }

  public static void drawBoundingBox(final BoundingBox box, final Renderer r) {
    Debugger.boundingBox.setData(box.getCenter(), box.getXExtent(), box.getYExtent(), box.getZExtent());
    Debugger.boundingBox.draw(r);
  }

  public static void drawOBB(final OrientedBoundingBox box, final Renderer r) {
    Debugger.boundingOB.getCenter().set(box.getCenter());
    Debugger.boundingOB.getxAxis().set(box.getXAxis());
    Debugger.boundingOB.getYAxis().set(box.getYAxis());
    Debugger.boundingOB.getZAxis().set(box.getZAxis());
    Debugger.boundingOB.getExtent().set(box.getExtent());
    Debugger.boundingOB.computeInformation();
    Debugger.boundingOB.draw(r);
  }

  // -- **** METHODS FOR DRAWING NORMALS **** -- //

  private static final Line normalLines = new Line("normLine");
  static {
    Debugger.normalLines.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    Debugger.normalLines.setRenderState(new ZBufferState());
    Debugger.normalLines.getMeshData().setIndexMode(IndexMode.Lines);
    Debugger.normalLines.getMeshData().setVertexBuffer(BufferUtils.createVector3Buffer(500));
    Debugger.normalLines.getMeshData().setColorBuffer(BufferUtils.createColorBuffer(500));
    Debugger.normalLines.updateWorldRenderStates(false);
    MaterialUtil.autoMaterials(Debugger.normalLines);
  }
  private static final Vector3 _normalVect = new Vector3(), _normalVect2 = new Vector3();
  public static final ColorRGBA NORMAL_COLOR_BASE = new ColorRGBA(ColorRGBA.RED);
  public static final ColorRGBA NORMAL_COLOR_TIP = new ColorRGBA(ColorRGBA.PINK);
  public static final ColorRGBA TANGENT_COLOR_BASE = new ColorRGBA(ColorRGBA.ORANGE);
  public static final ColorRGBA TANGENT_COLOR_TIP = new ColorRGBA(ColorRGBA.YELLOW);
  protected static final BoundingBox measureBox = new BoundingBox();
  public static double AUTO_NORMAL_RATIO = .05;

  /**
   * <code>drawNormals</code> draws lines representing normals for a given Spatial and its children.
   *
   * @param element
   *          the Spatial to draw normals for.
   * @param r
   *          the Renderer to use to draw the normals.
   */
  public static void drawNormals(final Spatial element, final Renderer r) {
    drawNormals(element, r, -1f, true);
  }

  public static void drawTangents(final Spatial element, final Renderer r) {
    drawTangents(element, r, -1f, true);
  }

  /**
   * <code>drawNormals</code> draws the normals for a given Spatial and optionally its children.
   *
   * @param element
   *          the Spatial to draw normals for.
   * @param r
   *          the Renderer to use to draw the normals.
   * @param size
   *          the length of the drawn normal (default is -1.0 which means autocalc based on boundings
   *          - if any).
   * @param doChildren
   *          if true, normals for any children will also be drawn
   */
  public static void drawNormals(final Spatial element, final Renderer r, final double size, final boolean doChildren) {
    if (element == null) {
      return;
    }

    final Camera cam = Camera.getCurrentCamera();
    final int state = cam.getPlaneState();
    if (element.getWorldBound() != null && cam.contains(element.getWorldBound()) == Camera.FrustumIntersect.Outside) {
      cam.setPlaneState(state);
      return;
    }
    cam.setPlaneState(state);
    if (element instanceof Mesh && element.getSceneHints().getCullHint() != CullHint.Always) {
      final Mesh mesh = (Mesh) element;

      double rSize = size;
      if (rSize == -1) {
        final BoundingVolume vol = element.getWorldBound();
        if (vol != null) {
          Debugger.measureBox.setCenter(vol.getCenter());
          Debugger.measureBox.setXExtent(0);
          Debugger.measureBox.setYExtent(0);
          Debugger.measureBox.setZExtent(0);
          Debugger.measureBox.mergeLocal(vol);
          rSize = Debugger.AUTO_NORMAL_RATIO * ((Debugger.measureBox.getXExtent() + Debugger.measureBox.getYExtent()
              + Debugger.measureBox.getZExtent()) / 3);
        } else {
          rSize = 1.0;
        }

        if (Double.isInfinite(rSize) || Double.isNaN(rSize)) {
          rSize = 1.0;
        }
      }

      final MeshData meshData = mesh.getMeshData();
      final FloatBuffer norms = meshData.getNormalBuffer();
      final FloatBuffer verts = meshData.getVertexBuffer();
      if (norms == null || verts == null || norms.limit() != verts.limit()) {
        return;
      }

      final MeshData normMD = Debugger.normalLines.getMeshData();
      FloatBuffer lineVerts = normMD.getVertexBuffer();
      if (lineVerts.capacity() < (3 * (2 * meshData.getVertexCount()))) {
        normMD.setVertexBuffer(null);
        lineVerts = BufferUtils.createVector3Buffer(meshData.getVertexCount() * 2);
        normMD.setVertexBuffer(lineVerts);
      } else {
        lineVerts.clear();
        lineVerts.limit(3 * 2 * meshData.getVertexCount());
        normMD.updateVertexCount();
      }

      FloatBuffer lineColors = normMD.getColorBuffer();
      if (lineColors.capacity() < (4 * (2 * meshData.getVertexCount()))) {
        normMD.setColorBuffer(null);
        lineColors = BufferUtils.createColorBuffer(meshData.getVertexCount() * 2);
        normMD.setColorBuffer(lineColors);
      } else {
        lineColors.clear();
      }

      IndexBufferData<?> lineInds = normMD.getIndices();
      if (lineInds == null || lineInds.getBufferCapacity() < (normMD.getVertexCount())) {
        normMD.setIndices(null);
        lineInds = BufferUtils.createIndexBufferData(meshData.getVertexCount() * 2, normMD.getVertexCount() - 1);
        normMD.setIndices(lineInds);
      } else {
        lineInds.getBuffer().clear();
        lineInds.getBuffer().limit(normMD.getVertexCount());
      }

      verts.rewind();
      norms.rewind();
      lineVerts.rewind();
      lineInds.getBuffer().rewind();

      for (int x = 0; x < meshData.getVertexCount(); x++) {
        Debugger._normalVect.set(verts.get(), verts.get(), verts.get());
        mesh.getWorldTransform().applyForward(Debugger._normalVect);
        lineVerts.put(Debugger._normalVect.getXf());
        lineVerts.put(Debugger._normalVect.getYf());
        lineVerts.put(Debugger._normalVect.getZf());

        lineColors.put(Debugger.NORMAL_COLOR_BASE.getRed());
        lineColors.put(Debugger.NORMAL_COLOR_BASE.getGreen());
        lineColors.put(Debugger.NORMAL_COLOR_BASE.getBlue());
        lineColors.put(Debugger.NORMAL_COLOR_BASE.getAlpha());

        lineInds.put(x * 2);

        Debugger._normalVect2.set(norms.get(), norms.get(), norms.get());
        mesh.getWorldTransform().applyForwardVector(Debugger._normalVect2).normalizeLocal().multiplyLocal(rSize);
        Debugger._normalVect.addLocal(Debugger._normalVect2);
        lineVerts.put(Debugger._normalVect.getXf());
        lineVerts.put(Debugger._normalVect.getYf());
        lineVerts.put(Debugger._normalVect.getZf());

        lineColors.put(Debugger.NORMAL_COLOR_TIP.getRed());
        lineColors.put(Debugger.NORMAL_COLOR_TIP.getGreen());
        lineColors.put(Debugger.NORMAL_COLOR_TIP.getBlue());
        lineColors.put(Debugger.NORMAL_COLOR_TIP.getAlpha());

        lineInds.put((x * 2) + 1);
      }

      normMD.markBufferDirty(MeshData.KEY_VertexCoords);
      normMD.markBufferDirty(MeshData.KEY_ColorCoords);
      normMD.markIndicesDirty();
      Debugger.normalLines.onDraw(r);
    }

    if (doChildren && element instanceof Node) {
      final Node n = (Node) element;
      if (n.getNumberOfChildren() != 0) {
        for (int i = n.getNumberOfChildren(); --i >= 0;) {
          drawNormals(n.getChild(i), r, size, true);
        }
      }
    }
  }

  public static void drawTangents(final Spatial element, final Renderer r, final double size,
      final boolean doChildren) {
    if (element == null) {
      return;
    }

    final Camera cam = Camera.getCurrentCamera();
    final int state = cam.getPlaneState();
    if (element.getWorldBound() != null && cam.contains(element.getWorldBound()) == Camera.FrustumIntersect.Outside) {
      cam.setPlaneState(state);
      return;
    }
    cam.setPlaneState(state);
    if (element instanceof Mesh && element.getSceneHints().getCullHint() != CullHint.Always) {
      final Mesh mesh = (Mesh) element;

      double rSize = size;
      if (rSize == -1) {
        final BoundingVolume vol = element.getWorldBound();
        if (vol != null) {
          Debugger.measureBox.setCenter(vol.getCenter());
          Debugger.measureBox.setXExtent(0);
          Debugger.measureBox.setYExtent(0);
          Debugger.measureBox.setZExtent(0);
          Debugger.measureBox.mergeLocal(vol);
          rSize = Debugger.AUTO_NORMAL_RATIO * ((Debugger.measureBox.getXExtent() + Debugger.measureBox.getYExtent()
              + Debugger.measureBox.getZExtent()) / 3f);
        } else {
          rSize = 1.0;
        }
      }

      final FloatBuffer norms = mesh.getMeshData().getTangentBuffer();
      final FloatBuffer verts = mesh.getMeshData().getVertexBuffer();
      if (norms != null && verts != null && norms.limit() == verts.limit()) {
        FloatBuffer lineVerts = Debugger.normalLines.getMeshData().getVertexBuffer();
        if (lineVerts.capacity() < (3 * (2 * mesh.getMeshData().getVertexCount()))) {
          Debugger.normalLines.getMeshData().setVertexBuffer(null);
          lineVerts = BufferUtils.createVector3Buffer(mesh.getMeshData().getVertexCount() * 2);
          Debugger.normalLines.getMeshData().setVertexBuffer(lineVerts);
        } else {
          lineVerts.clear();
          lineVerts.limit(3 * 2 * mesh.getMeshData().getVertexCount());
          Debugger.normalLines.getMeshData().setVertexBuffer(lineVerts);
        }

        FloatBuffer lineColors = Debugger.normalLines.getMeshData().getColorBuffer();
        if (lineColors.capacity() < (4 * (2 * mesh.getMeshData().getVertexCount()))) {
          Debugger.normalLines.getMeshData().setColorBuffer(null);
          lineColors = BufferUtils.createColorBuffer(mesh.getMeshData().getVertexCount() * 2);
          Debugger.normalLines.getMeshData().setColorBuffer(lineColors);
        } else {
          lineColors.clear();
        }

        IndexBufferData<?> lineInds = Debugger.normalLines.getMeshData().getIndices();
        if (lineInds == null || lineInds.getBufferCapacity() < (Debugger.normalLines.getMeshData().getVertexCount())) {
          Debugger.normalLines.getMeshData().setIndices(null);
          lineInds = BufferUtils.createIndexBufferData(mesh.getMeshData().getVertexCount() * 2,
              Debugger.normalLines.getMeshData().getVertexCount() - 1);
          Debugger.normalLines.getMeshData().setIndices(lineInds);
        } else {
          lineInds.getBuffer().clear();
          lineInds.getBuffer().limit(Debugger.normalLines.getMeshData().getVertexCount());
        }

        verts.rewind();
        norms.rewind();
        lineVerts.rewind();
        lineInds.getBuffer().rewind();

        for (int x = 0; x < mesh.getMeshData().getVertexCount(); x++) {
          Debugger._normalVect.set(verts.get(), verts.get(), verts.get());
          Debugger._normalVect.multiplyLocal(mesh.getWorldScale());
          lineVerts.put(Debugger._normalVect.getXf());
          lineVerts.put(Debugger._normalVect.getYf());
          lineVerts.put(Debugger._normalVect.getZf());

          lineColors.put(Debugger.TANGENT_COLOR_BASE.getRed());
          lineColors.put(Debugger.TANGENT_COLOR_BASE.getGreen());
          lineColors.put(Debugger.TANGENT_COLOR_BASE.getBlue());
          lineColors.put(Debugger.TANGENT_COLOR_BASE.getAlpha());

          lineInds.put(x * 2);

          Debugger._normalVect.addLocal(norms.get() * rSize, norms.get() * rSize, norms.get() * rSize);
          lineVerts.put(Debugger._normalVect.getXf());
          lineVerts.put(Debugger._normalVect.getYf());
          lineVerts.put(Debugger._normalVect.getZf());

          lineColors.put(Debugger.TANGENT_COLOR_TIP.getRed());
          lineColors.put(Debugger.TANGENT_COLOR_TIP.getGreen());
          lineColors.put(Debugger.TANGENT_COLOR_TIP.getBlue());
          lineColors.put(Debugger.TANGENT_COLOR_TIP.getAlpha());

          lineInds.put((x * 2) + 1);
        }

        Debugger.normalLines.setWorldTranslation(mesh.getWorldTranslation());
        Debugger.normalLines.setWorldRotation(mesh.getWorldRotation());
        Debugger.normalLines.onDraw(r);
      }

    }

    if (doChildren && element instanceof Node) {
      final Node n = (Node) element;
      if (n.getNumberOfChildren() != 0) {
        for (int i = n.getNumberOfChildren(); --i >= 0;) {
          drawTangents(n.getChild(i), r, size, true);
        }
      }
    }
  }

  // -- **** METHODS FOR DRAWING AXIS **** -- //

  private static final AxisRods rods = new AxisRods("debug_rods", true, 1);
  static {
    Debugger.rods.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
  }
  private static boolean axisInited = false;

  public static void drawAxis(final Spatial spat, final Renderer r) {
    drawAxis(spat, r, true, false);
  }

  public static void drawAxis(final Spatial spat, final Renderer r, final boolean drawChildren, final boolean drawAll) {
    if (!Debugger.axisInited) {
      final BlendState blendState = new BlendState();
      blendState.setBlendEnabled(true);
      blendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
      blendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
      Debugger.rods.setRenderState(blendState);
      Debugger.rods.updateGeometricState(0, false);
      Debugger.axisInited = true;
    }

    if (drawAll || (spat instanceof Mesh)) {
      if (spat.getWorldBound() != null) {
        double rSize;
        final BoundingVolume vol = spat.getWorldBound();
        if (vol != null) {
          Debugger.measureBox.setCenter(vol.getCenter());
          Debugger.measureBox.setXExtent(0);
          Debugger.measureBox.setYExtent(0);
          Debugger.measureBox.setZExtent(0);
          Debugger.measureBox.mergeLocal(vol);
          rSize = 1 * ((Debugger.measureBox.getXExtent() + Debugger.measureBox.getYExtent()
              + Debugger.measureBox.getZExtent()) / 3);
        } else {
          rSize = 1.0;
        }

        Debugger.rods.setTranslation(spat.getWorldBound().getCenter());
        Debugger.rods.setScale(rSize);
      } else {
        Debugger.rods.setTranslation(spat.getWorldTranslation());
        Debugger.rods.setScale(spat.getWorldScale());
      }
      Debugger.rods.setRotation(spat.getWorldRotation());
      Debugger.rods.updateGeometricState(0, false);

      Debugger.rods.draw(r);
    }

    if ((spat instanceof Node) && drawChildren) {
      final Node n = (Node) spat;
      if (n.getNumberOfChildren() == 0) {
        return;
      }
      for (int x = 0, count = n.getNumberOfChildren(); x < count; x++) {
        drawAxis(n.getChild(x), r, drawChildren, drawAll);
      }
    }
  }

  // -- **** METHODS FOR DISPLAYING BUFFERS **** -- //
  public static final int NORTHWEST = 0;
  public static final int NORTHEAST = 1;
  public static final int SOUTHEAST = 2;
  public static final int SOUTHWEST = 3;

  private static final Quad bQuad = new Quad("", 128, 128);
  private static Texture2D bufTexture;
  private static TextureRenderer bufTexRend;

  static {
    Debugger.bQuad.getSceneHints().setRenderBucketType(RenderBucketType.OrthoOrder);
    Debugger.bQuad.getSceneHints().setCullHint(CullHint.Never);
  }

  public static void drawBuffer(final TextureStoreFormat rttFormat, final int location, final Renderer r) {
    final Camera cam = Camera.getCurrentCamera();
    drawBuffer(rttFormat, location, r, cam.getWidth() / 6.25);
  }

  public static void drawBuffer(final TextureStoreFormat rttFormat, final int location, final Renderer r,
      final double size) {
    final Camera cam = Camera.getCurrentCamera();
    r.flushGraphics();
    double locationX = cam.getWidth(), locationY = cam.getHeight();
    Debugger.bQuad.resize(size, (cam.getHeight() / (double) cam.getWidth()) * size);
    if (Debugger.bQuad.getLocalRenderState(RenderState.StateType.Texture) == null) {
      final TextureState ts = new TextureState();
      Debugger.bufTexture = new Texture2D();
      ts.setTexture(Debugger.bufTexture);
      Debugger.bQuad.setRenderState(ts);
    }

    int width = cam.getWidth();
    if (!MathUtils.isPowerOfTwo(width)) {
      int newWidth = 2;
      do {
        newWidth <<= 1;

      } while (newWidth < width);
      Debugger.bQuad.getMeshData().getTextureBuffer(0).put(4, width / (float) newWidth);
      Debugger.bQuad.getMeshData().getTextureBuffer(0).put(6, width / (float) newWidth);
      width = newWidth;
    }

    int height = cam.getHeight();
    if (!MathUtils.isPowerOfTwo(height)) {
      int newHeight = 2;
      do {
        newHeight <<= 1;

      } while (newHeight < height);
      Debugger.bQuad.getMeshData().getTextureBuffer(0).put(1, height / (float) newHeight);
      Debugger.bQuad.getMeshData().getTextureBuffer(0).put(7, height / (float) newHeight);
      height = newHeight;
    }
    if (Debugger.bufTexRend == null) {
      Debugger.bufTexRend = r.createTextureRenderer(width, height, 0, 0);
      Debugger.bufTexRend.setupTexture(Debugger.bufTexture);
    }
    Debugger.bufTexRend.copyToTexture(Debugger.bufTexture, 0, 0, width, height, 0, 0);

    final double loc = size * .75;
    switch (location) {
      case NORTHWEST:
        locationX = loc;
        locationY -= loc;
        break;
      case NORTHEAST:
        locationX -= loc;
        locationY -= loc;
        break;
      case SOUTHEAST:
        locationX -= loc;
        locationY = loc;
        break;
      case SOUTHWEST:
      default:
        locationX = loc;
        locationY = loc;
        break;
    }

    Debugger.bQuad.setWorldTranslation(locationX, locationY, 0);

    Debugger.bQuad.updateGeometricState(0);
    Debugger.bQuad.onDraw(r);
    r.flushGraphics();
  }

  // -- **** METHODS FOR DISPLAYING CAMERAS **** -- //
  private static Line lineFrustum;
  private static final ExtendedCamera extendedCamera = new ExtendedCamera();

  public static void drawCameraFrustum(final Renderer r, final Camera camera, final ReadOnlyColorRGBA color,
      final short pattern, final boolean drawOriginConnector) {
    drawCameraFrustum(r, camera, camera.getFrustumNear(), camera.getFrustumFar(), color, pattern, drawOriginConnector);
  }

  public static void drawCameraFrustum(final Renderer r, final Camera camera, final double fNear, final double fFar,
      final ReadOnlyColorRGBA color, final short pattern, final boolean drawOriginConnector) {
    if (Debugger.lineFrustum == null) {
      final FloatBuffer verts = BufferUtils.createVector3Buffer(24);
      final FloatBuffer colors = BufferUtils.createColorBuffer(24);

      final Line line = new Line("Lines", verts, null, colors, null);
      line.getMeshData()
          .setIndexModes(new IndexMode[] {IndexMode.LineLoop, IndexMode.LineLoop, IndexMode.Lines, IndexMode.Lines});
      line.getMeshData().setIndexLengths(new int[] {4, 4, 8, 8});
      LightProperties.setLightReceiver(line, false);

      final BlendState lineBlendState = new BlendState();
      lineBlendState.setEnabled(true);
      lineBlendState.setBlendEnabled(true);
      lineBlendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
      lineBlendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
      line.setRenderState(lineBlendState);

      // set alpha testing
      line.setProperty(AlphaTestConsts.KEY_AlphaTestType, AlphaTestConsts.TestFunction.GreaterThan);
      line.setProperty(AlphaTestConsts.KEY_AlphaReference, 0f);

      final ZBufferState zstate = new ZBufferState();
      line.setRenderState(zstate);
      line.updateGeometricState(0.0);

      line.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
      Debugger.lineFrustum = line;
    }

    Debugger.lineFrustum.setDefaultColor(color);
    MaterialUtil.autoMaterials(Debugger.lineFrustum);

    Debugger.extendedCamera.set(camera);
    Debugger.extendedCamera.calculateFrustum(fNear, fFar);

    final FloatBuffer colors = Debugger.lineFrustum.getMeshData().getColorBuffer();
    for (int i = 0; i < 16; i++) {
      BufferUtils.setInBuffer(color, colors, i);
    }
    final float alpha = drawOriginConnector ? 0.4f : 0.0f;
    for (int i = 16; i < 24; i++) {
      colors.position(i * 4);
      colors.put(color.getRed());
      colors.put(color.getGreen());
      colors.put(color.getBlue());
      colors.put(alpha);
    }

    final Vector3[] corners = Debugger.extendedCamera.getCorners();

    final FloatBuffer verts = Debugger.lineFrustum.getMeshData().getVertexBuffer();
    BufferUtils.setInBuffer(corners[0], verts, 0);
    BufferUtils.setInBuffer(corners[1], verts, 1);
    BufferUtils.setInBuffer(corners[2], verts, 2);
    BufferUtils.setInBuffer(corners[3], verts, 3);

    BufferUtils.setInBuffer(corners[4], verts, 4);
    BufferUtils.setInBuffer(corners[5], verts, 5);
    BufferUtils.setInBuffer(corners[6], verts, 6);
    BufferUtils.setInBuffer(corners[7], verts, 7);

    BufferUtils.setInBuffer(corners[0], verts, 8);
    BufferUtils.setInBuffer(corners[4], verts, 9);
    BufferUtils.setInBuffer(corners[1], verts, 10);
    BufferUtils.setInBuffer(corners[5], verts, 11);
    BufferUtils.setInBuffer(corners[2], verts, 12);
    BufferUtils.setInBuffer(corners[6], verts, 13);
    BufferUtils.setInBuffer(corners[3], verts, 14);
    BufferUtils.setInBuffer(corners[7], verts, 15);

    BufferUtils.setInBuffer(Debugger.extendedCamera.getLocation(), verts, 16);
    BufferUtils.setInBuffer(corners[0], verts, 17);
    BufferUtils.setInBuffer(Debugger.extendedCamera.getLocation(), verts, 18);
    BufferUtils.setInBuffer(corners[1], verts, 19);
    BufferUtils.setInBuffer(Debugger.extendedCamera.getLocation(), verts, 20);
    BufferUtils.setInBuffer(corners[2], verts, 21);
    BufferUtils.setInBuffer(Debugger.extendedCamera.getLocation(), verts, 22);
    BufferUtils.setInBuffer(corners[3], verts, 23);

    Debugger.lineFrustum.draw(r);
  }

}
