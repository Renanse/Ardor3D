/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.useful;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * <code>TrailMesh</code>
 */
public class TrailMesh extends Mesh {

  private int nrTrailSections;
  private int trailVertices;

  public enum UpdateMode {
    Step, Interpolate
  }

  /**
   * Update mode defines how the trailmesh vertices are to be updated. Shifted position by position or
   * interpolated for smoother movement.
   */
  private UpdateMode updateMode = UpdateMode.Step;

  public enum FacingMode {
    Tangent, Billboard
  }

  /*
   * Facing mode defines the orientation of the trailmesh. Tangent uses the tangent value specified
   * when calling setTrailFront and Billboard automatically orients the trailmesh to always face the
   * camera.
   */
  private FacingMode facingMode = FacingMode.Billboard;

  /**
   * Storage for each section in the trailmesh.
   */
  public class TrailData {
    public Vector3 position = new Vector3();
    public Vector3 tangent;
    public double width;
    public Vector3 interpolatedPosition;
  }

  private final LinkedList<TrailData> trailVectors;

  private float throttle = Float.MAX_VALUE;

  // How often to update the trail front (controlling section spacing)
  private float updateSpeed = 20.0f;

  // Whether the TrailData is updated or not
  private boolean invalid;

  // Temporary vectors
  private final Vector3 trailCamVec = new Vector3();
  private final Vector3 trailDirection = new Vector3();

  /**
   * Creates a new TrailMesh.
   *
   * @param name
   *          Name of Spatial
   * @param nrTrailSections
   *          Number of sections the TrailMesh should consist of. Number of vertices in the mesh will
   *          be nrTrailSections * 2.
   */
  public TrailMesh(final String name, final int nrTrailSections) {
    super(name);
    this.nrTrailSections = nrTrailSections;
    trailVertices = nrTrailSections * 2;

    trailVectors = new LinkedList<>();
    for (int i = 0; i < nrTrailSections; i++) {
      trailVectors.add(new TrailData());
    }

    setData();
  }

  /**
   * Update the front position of the trail.
   *
   * @param position
   *          New position of the trail front
   * @param width
   *          Width of the trail
   * @param tpf
   *          Current time per frame
   */
  public void setTrailFront(final Vector3 position, final float width, final float tpf) {
    setTrailFront(position, null, width, tpf);
  }

  /**
   * Update the front position of the trail.
   *
   * @param position
   *          New position of the trail front
   * @param tangent
   *          Specifies the gradient of the trail (if facingmode is set to tangent)
   * @param width
   *          Width of the trail
   * @param tpf
   *          Current time per frame
   */
  public void setTrailFront(final ReadOnlyVector3 position, final ReadOnlyVector3 tangent, final double width,
      final double tpf) {

    TrailData trail = null;

    // Check if time to add or wrap the trail sections
    throttle += tpf * updateSpeed;
    if (throttle > 1.0f) {
      throttle %= 1.0f;

      trail = trailVectors.removeLast();
      trailVectors.addFirst(trail);
    } else {
      trail = trailVectors.getFirst();
    }

    if (trail == null) {
      return;
    }

    // Always update the front section
    trail.position.set(position);
    if (tangent != null) {
      if (trail.tangent == null) {
        trail.tangent = new Vector3();
      }
      trail.tangent.set(tangent);
    }
    trail.width = width;
    invalid = true;
  }

  /**
   * Update the vertices of the trail.
   *
   * @param camPos
   *          Camera position used for billboarding.
   */
  public void update(final ReadOnlyVector3 camPos) {
    if (trailVectors.size() < 2) {
      return;
    }

    if (invalid || facingMode == FacingMode.Billboard) {
      if (updateMode == UpdateMode.Step) {
        updateStep(camPos);
      } else {
        updateInterpolate(camPos);
      }
      _meshData.markBufferDirty(MeshData.KEY_VertexCoords);
      invalid = false;
    }
  }

  public void invalidate() {
    invalid = true;
  }

  private void updateStep(final ReadOnlyVector3 camPos) {
    final FloatBuffer vertBuf = getMeshData().getVertexBuffer();
    vertBuf.rewind();

    for (int i = 0; i < nrTrailSections; i++) {
      final TrailData trailData = trailVectors.get(i);
      final Vector3 trailVector = trailData.position;

      if (facingMode == FacingMode.Billboard) {
        if (i == 0) {
          trailDirection.set(trailVectors.get(i + 1).position).subtractLocal(trailVector);
        } else if (i == nrTrailSections - 1) {
          trailDirection.set(trailVector).subtractLocal(trailVectors.get(i - 1).position);
        } else {
          trailDirection.set(trailVectors.get(i + 1).position).subtractLocal(trailVectors.get(i - 1).position);
        }

        trailCamVec.set(trailVector).subtractLocal(camPos);
        trailDirection.crossLocal(trailCamVec);
        trailDirection.normalizeLocal().multiplyLocal(trailData.width * 0.5);
      } else if (trailData.tangent != null) {
        trailDirection.set(trailData.tangent).multiplyLocal(trailData.width * 0.5);
      } else {
        trailDirection.set(trailData.width * 0.5f, 0, 0);
      }

      vertBuf.put(trailVector.getXf() - trailDirection.getXf());
      vertBuf.put(trailVector.getYf() - trailDirection.getYf());
      vertBuf.put(trailVector.getZf() - trailDirection.getZf());

      vertBuf.put(trailVector.getXf() + trailDirection.getXf());
      vertBuf.put(trailVector.getYf() + trailDirection.getYf());
      vertBuf.put(trailVector.getZf() + trailDirection.getZf());
    }
  }

  private void updateInterpolate(final ReadOnlyVector3 camPos) {
    final FloatBuffer vertBuf = getMeshData().getVertexBuffer();
    vertBuf.rewind();

    for (int i = 0; i < nrTrailSections; i++) {
      final TrailData trailData = trailVectors.get(i);

      Vector3 interpolationVector = trailData.interpolatedPosition;
      if (trailData.interpolatedPosition == null) {
        trailData.interpolatedPosition = new Vector3();
        interpolationVector = trailData.interpolatedPosition;
      }

      interpolationVector.set(trailData.position);

      if (i > 0) {
        interpolationVector.lerpLocal(trailVectors.get(i - 1).position, throttle);
      }
    }

    for (int i = 0; i < nrTrailSections; i++) {
      final TrailData trailData = trailVectors.get(i);
      final Vector3 trailVector = trailData.interpolatedPosition;

      if (facingMode == FacingMode.Billboard) {
        if (i == 0) {
          trailDirection.set(trailVectors.get(i + 1).interpolatedPosition).subtractLocal(trailVector);
        } else if (i == nrTrailSections - 1) {
          trailDirection.set(trailVector).subtractLocal(trailVectors.get(i - 1).interpolatedPosition);
        } else {
          trailDirection.set(trailVectors.get(i + 1).interpolatedPosition)
              .subtractLocal(trailVectors.get(i - 1).interpolatedPosition);
        }

        trailCamVec.set(trailVector).subtractLocal(camPos);
        trailDirection.crossLocal(trailCamVec);
        trailDirection.normalizeLocal().multiplyLocal(trailData.width * 0.5);
      } else if (trailData.tangent != null) {
        trailDirection.set(trailData.tangent).multiplyLocal(trailData.width * 0.5);
      } else {
        trailDirection.set(trailData.width * 0.5f, 0, 0);
      }

      vertBuf.put(trailVector.getXf() - trailDirection.getXf());
      vertBuf.put(trailVector.getYf() - trailDirection.getYf());
      vertBuf.put(trailVector.getZf() - trailDirection.getZf());

      vertBuf.put(trailVector.getXf() + trailDirection.getXf());
      vertBuf.put(trailVector.getYf() + trailDirection.getYf());
      vertBuf.put(trailVector.getZf() + trailDirection.getZf());
    }
  }

  public void resetPosition(final ReadOnlyVector3 position) {
    for (int i = 0; i < nrTrailSections; i++) {
      trailVectors.get(i).position.set(position);
    }
  }

  private void setData() {
    getMeshData().setVertexBuffer(BufferUtils.createVector3Buffer(getMeshData().getVertexBuffer(), trailVertices));
    getMeshData().setNormalBuffer(BufferUtils.createVector3Buffer(getMeshData().getNormalBuffer(), trailVertices));
    setDefaultColor(new ColorRGBA(ColorRGBA.WHITE));
    setTextureData();
    setIndexData();
  }

  private void setTextureData() {
    if (getMeshData().getTextureCoords(0) == null) {
      final FloatBuffer tex = BufferUtils.createVector2Buffer(trailVertices);
      getMeshData().setTextureCoords(new FloatBufferData(tex, 2), 0);
      for (int i = 0; i < nrTrailSections; i++) {
        tex.put((float) i / nrTrailSections).put(0);
        tex.put((float) i / nrTrailSections).put(1);
      }
    }
  }

  private void setIndexData() {
    getMeshData().setIndexMode(IndexMode.TriangleStrip);
  }

  @Override
  public void write(final OutputCapsule capsule) throws IOException {
    super.write(capsule);
    capsule.write(nrTrailSections, "nrTrailSections", 0);
    capsule.write(trailVertices, "trailVertices", 0);
  }

  @Override
  public void read(final InputCapsule capsule) throws IOException {
    super.read(capsule);
    nrTrailSections = capsule.readInt("nrTrailSections", 0);
    trailVertices = capsule.readInt("trailVertices", 0);
  }

  public void setUpdateSpeed(final float updateSpeed) { this.updateSpeed = updateSpeed; }

  public float getUpdateSpeed() { return updateSpeed; }

  public void setUpdateMode(final UpdateMode updateMode) { this.updateMode = updateMode; }

  public UpdateMode getUpdateMode() { return updateMode; }

  public void setFacingMode(final FacingMode facingMode) { this.facingMode = facingMode; }

  public FacingMode getFacingMode() { return facingMode; }

  /**
   * Get the mesh data to modify it manually. If data is modified, invalidate() method call is
   * required.
   *
   * @return
   */
  public LinkedList<TrailData> getTrailData() { return trailVectors; }

}
