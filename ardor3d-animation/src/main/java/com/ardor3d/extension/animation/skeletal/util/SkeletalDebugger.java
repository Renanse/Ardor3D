/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.animation.skeletal.util;

import java.util.HashSet;
import java.util.Set;

import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.Skeleton;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.ProjectionMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.WireframeState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Pyramid;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BMText.Align;
import com.ardor3d.ui.text.BasicText;

/**
 * Utility useful for drawing Skeletons found in a scene.
 */
public class SkeletalDebugger {
  public static double BONE_RATIO = .05;
  public static double JOINT_RATIO = .075;
  public static double LABEL_RATIO = .5;

  protected static final BoundingSphere measureSphere = new BoundingSphere();
  protected static final BasicText jointText = BasicText.createDefaultTextLabel("", "");
  static {
    // No lighting, replace texturing
    SkeletalDebugger.jointText.getSceneHints().setLightCombineMode(LightCombineMode.Off);
    SkeletalDebugger.jointText.getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
    // Do not queue... draw right away.
    SkeletalDebugger.jointText.getSceneHints().setRenderBucketType(RenderBucketType.Skip);

    SkeletalDebugger.jointText.setDefaultColor(ColorRGBA.YELLOW);
    SkeletalDebugger.jointText.setAlign(Align.Center);

    SkeletalDebugger.jointText.updateGeometricState(0);
  }

  protected static Camera orthoCam = new Camera(1, 1);
  static {
    SkeletalDebugger.orthoCam.setFrustum(-1, 1, 0, 100, 100, 0);
    SkeletalDebugger.orthoCam.setProjectionMode(ProjectionMode.Orthographic);

    final Vector3 loc = new Vector3(0.0f, 0.0f, 0.0f);
    final Vector3 left = new Vector3(-1.0f, 0.0f, 0.0f);
    final Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);
    final Vector3 dir = new Vector3(0.0f, 0f, -1.0f);
    /** Move our camera to a correct place and orientation. */
    SkeletalDebugger.orthoCam.setFrame(loc, left, up, dir);
  }

  /**
   * Traverse the given scene and draw the currently posed Skeleton of any SkinnedMesh we encounter.
   *
   * @param scene
   *          the scene
   * @param renderer
   *          the Renderer to draw with.
   */
  public static void drawSkeletons(final Spatial scene, final Renderer renderer) {
    SkeletalDebugger.drawSkeletons(scene, renderer, false, false);
  }

  /**
   * Traverse the given scene and draw the currently posed Skeleton of any SkinnedMesh we encounter.
   * If showLabels is true, joint names will be drawn over the joints.
   *
   * @param scene
   *          the scene
   * @param renderer
   *          the Renderer to draw with.
   * @param allowSkeletonRedraw
   *          if true, we will draw the skeleton for every skinnedmesh we encounter, even if two
   *          skinnedmeshes are on the same skeleton.
   * @param showLabels
   *          show the names of the joints over them.
   */
  public static void drawSkeletons(final Spatial scene, final Renderer renderer, final boolean allowSkeletonRedraw,
      final boolean showLabels) {
    SkeletalDebugger.drawSkeletons(scene, renderer, allowSkeletonRedraw, showLabels, new HashSet<Skeleton>());
  }

  private static void drawSkeletons(final Spatial scene, final Renderer renderer, final boolean allowSkeletonRedraw,
      final boolean showLabels, final Set<Skeleton> alreadyDrawn) {
    assert scene != null : "scene must not be null.";

    // Check if we are a skinned mesh
    boolean doChildren = true;
    if (scene instanceof SkinnedMesh) {
      final SkeletonPose pose = ((SkinnedMesh) scene).getCurrentPose();
      if (pose != null && (allowSkeletonRedraw || !alreadyDrawn.contains(pose.getSkeleton()))) {
        // If we're in view, go ahead and draw our associated skeleton pose
        final Camera cam = Camera.getCurrentCamera();
        final int state = cam.getPlaneState();
        if (cam.contains(scene.getWorldBound()) != Camera.FrustumIntersect.Outside) {
          SkeletalDebugger.drawSkeleton(pose, scene, renderer, showLabels);
          alreadyDrawn.add(pose.getSkeleton());
        } else {
          doChildren = false;
        }
        cam.setPlaneState(state);
      }
    }

    // Recurse down the scene if we're a Node and we were not flagged to ignore children.
    if (doChildren && scene instanceof Node) {
      final Node n = (Node) scene;
      if (n.getNumberOfChildren() != 0) {
        for (int i = n.getNumberOfChildren(); --i >= 0;) {
          SkeletalDebugger.drawSkeletons(n.getChild(i), renderer, allowSkeletonRedraw, showLabels, alreadyDrawn);
        }
      }
    }
  }

  /**
   * Draw a skeleton in a specific pose.
   *
   * @param pose
   *          the posed skeleton to draw
   * @param scene
   * @param renderer
   *          the Renderer to draw with.
   * @param showLabels
   *          show the names of the joints over them.
   */
  private static void drawSkeleton(final SkeletonPose pose, final Spatial scene, final Renderer renderer,
      final boolean showLabels) {
    final Joint[] joints = pose.getSkeleton().getJoints();
    final Transform[] globals = pose.getGlobalJointTransforms();

    for (int i = 0, max = joints.length; i < max; i++) {
      SkeletalDebugger.drawJoint(globals[i], scene, renderer);
      final short parentIndex = joints[i].getParentIndex();

      if (parentIndex != Joint.NO_PARENT) {
        SkeletalDebugger.drawBone(globals[parentIndex], globals[i], scene, renderer);
      }
    }

    if (showLabels) {
      final Camera current = Camera.getCurrentCamera();
      if (SkeletalDebugger.orthoCam.getWidth() != current.getWidth()
          || SkeletalDebugger.orthoCam.getHeight() != current.getHeight()) {
        SkeletalDebugger.orthoCam.resize(current.getWidth(), current.getHeight());
        SkeletalDebugger.orthoCam.setFrustumRight(current.getWidth());
        SkeletalDebugger.orthoCam.setFrustumTop(current.getHeight());
      }
      SkeletalDebugger.orthoCam.apply(renderer);
      final Transform store = Transform.fetchTempInstance();
      final Vector3 point = Vector3.fetchTempInstance();
      for (int i = 0, max = joints.length; i < max; i++) {
        SkeletalDebugger.jointText.setText(i + ". " + joints[i].getName());

        final Transform t = scene.getWorldTransform().multiply(globals[i], store);
        point.zero();
        SkeletalDebugger.jointText.setTranslation(current.getScreenCoordinates(t.applyForward(point)));

        final double size = SkeletalDebugger.LABEL_RATIO;
        SkeletalDebugger.jointText.setScale(size, size, -size);

        SkeletalDebugger.jointText.draw(renderer);
      }
      Transform.releaseTempInstance(store);
      Vector3.releaseTempInstance(point);
      renderer.renderBuckets();
      current.apply(renderer);
    }
  }

  /** Our bone shape. */
  private static final Pyramid bone = new Pyramid("bone", 1, 1);
  static {
    // Alter the primitive to better represent our bone.
    // Set color to white
    SkeletalDebugger.setBoneColor(ColorRGBA.WHITE);
    // Rotate the vertices of our bone to point along the Z axis instead of the Y.
    SkeletalDebugger.bone.getMeshData()
        .rotatePoints(new Quaternion().fromAngleAxis(90 * MathUtils.DEG_TO_RAD, Vector3.UNIT_X));
    // Drop the normals
    SkeletalDebugger.bone.getMeshData().setNormalBuffer(null);

    // No lighting or texturing
    SkeletalDebugger.bone.getSceneHints().setLightCombineMode(LightCombineMode.Off);
    SkeletalDebugger.bone.getSceneHints().setTextureCombineMode(TextureCombineMode.Off);
    // Do not queue... draw right away.
    SkeletalDebugger.bone.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    // Draw in wire frame mode.
    SkeletalDebugger.bone.setRenderState(new WireframeState());
    // Respect existing zbuffer, and write into it
    SkeletalDebugger.bone.setRenderState(new ZBufferState());
    // Update our bone and make it ready for use.
    SkeletalDebugger.bone.updateGeometricState(0);
  }

  /**
   * Draw a single bone using the given world-space joint transformations.
   *
   * @param start
   *          our parent joint transform
   * @param end
   *          our child joint transform
   * @param scene
   * @param renderer
   *          the Renderer to draw with.
   */
  private static void drawBone(final Transform start, final Transform end, final Spatial scene,
      final Renderer renderer) {
    // Determine our start and end points
    final Vector3 stPnt = Vector3.fetchTempInstance();
    final Vector3 endPnt = Vector3.fetchTempInstance();
    start.applyForward(Vector3.ZERO, stPnt);
    end.applyForward(Vector3.ZERO, endPnt);

    // determine distance and use as a scale to elongate the bone
    double scale = stPnt.distance(endPnt);
    if (scale == 0) {
      scale = MathUtils.ZERO_TOLERANCE;
    }
    final BoundingVolume vol = scene.getWorldBound();
    double size = 1.0;
    if (vol != null) {
      SkeletalDebugger.measureSphere.setCenter(vol.getCenter());
      SkeletalDebugger.measureSphere.setRadius(0);
      SkeletalDebugger.measureSphere.mergeLocal(vol);
      size = SkeletalDebugger.BONE_RATIO * SkeletalDebugger.measureSphere.getRadius();
    }
    SkeletalDebugger.bone.setWorldTransform(Transform.IDENTITY);
    SkeletalDebugger.bone.setWorldScale(size, size, scale);

    // determine center point of bone (translation).
    final Vector3 store = Vector3.fetchTempInstance();
    SkeletalDebugger.bone.setWorldTranslation(stPnt.add(endPnt, store).divideLocal(2.0));
    Vector3.releaseTempInstance(store);

    // Orient bone to point along axis formed by start and end points.
    final Matrix3 orient = Matrix3.fetchTempInstance();
    orient.lookAt(endPnt.subtractLocal(stPnt).normalizeLocal(), Vector3.UNIT_Y);
    final Quaternion q = new Quaternion().fromRotationMatrix(orient);
    q.normalizeLocal();
    SkeletalDebugger.bone.setWorldRotation(q);

    // Offset with skin transform
    SkeletalDebugger.bone
        .setWorldTransform(scene.getWorldTransform().multiply(SkeletalDebugger.bone.getWorldTransform(), null));

    // Release some temp vars.
    Matrix3.releaseTempInstance(orient);
    Vector3.releaseTempInstance(stPnt);
    Vector3.releaseTempInstance(endPnt);

    // Draw our bone!
    SkeletalDebugger.bone.draw(renderer);
  }

  /**
   * Set the color of the joint label object used in showing joint names.
   *
   * @param color
   *          the new color to use for joint labels.
   */
  public static void setJointLabelColor(final ReadOnlyColorRGBA color) {
    SkeletalDebugger.jointText.setDefaultColor(color);
  }

  /**
   * Set the color of the bone object used in skeleton drawing.
   *
   * @param color
   *          the new color to use for skeleton bones.
   */
  public static void setBoneColor(final ReadOnlyColorRGBA color) {
    SkeletalDebugger.bone.setSolidColor(color);
  }

  /** Our joint shape. */
  private static final Sphere joint = new Sphere("joint", 3, 4, 0.5);
  static {
    // Alter the primitive to better represent our joint.
    // Set color to cyan
    SkeletalDebugger.setJointColor(ColorRGBA.RED);
    // Drop the normals
    SkeletalDebugger.joint.getMeshData().setNormalBuffer(null);

    // No lighting or texturing
    SkeletalDebugger.joint.getSceneHints().setLightCombineMode(LightCombineMode.Off);
    SkeletalDebugger.joint.getSceneHints().setTextureCombineMode(TextureCombineMode.Off);
    // Do not queue... draw right away.
    SkeletalDebugger.joint.getSceneHints().setRenderBucketType(RenderBucketType.Skip);
    // Draw in wire frame mode.
    SkeletalDebugger.joint.setRenderState(new WireframeState());
    // Respect existing zbuffer, and write into it
    SkeletalDebugger.joint.setRenderState(new ZBufferState());
    // Update our joint and make it ready for use.
    SkeletalDebugger.joint.updateGeometricState(0);
  }
  private static Transform spTransform = new Transform();
  private static Matrix3 spMatrix = new Matrix3();

  /**
   * Draw a single Joint using the given world-space joint transform.
   *
   * @param jntTransform
   *          our joint transform
   * @param scene
   * @param renderer
   *          the Renderer to draw with.
   */
  private static void drawJoint(final Transform jntTransform, final Spatial scene, final Renderer renderer) {
    final BoundingVolume vol = scene.getWorldBound();
    double size = 1.0;
    if (vol != null) {
      SkeletalDebugger.measureSphere.setCenter(vol.getCenter());
      SkeletalDebugger.measureSphere.setRadius(0);
      SkeletalDebugger.measureSphere.mergeLocal(vol);
      size = SkeletalDebugger.BONE_RATIO * SkeletalDebugger.measureSphere.getRadius();
    }
    scene.getWorldTransform().multiply(jntTransform, SkeletalDebugger.spTransform);
    SkeletalDebugger.spTransform.getMatrix().scale(new Vector3(size, size, size), SkeletalDebugger.spMatrix);
    SkeletalDebugger.spTransform.setRotation(SkeletalDebugger.spMatrix);
    SkeletalDebugger.joint.setWorldTransform(SkeletalDebugger.spTransform);
    SkeletalDebugger.joint.draw(renderer);
  }

  /**
   * Set the color of the joint object used in skeleton drawing.
   *
   * @param color
   *          the new color to use for skeleton joints.
   */
  public static void setJointColor(final ReadOnlyColorRGBA color) {
    SkeletalDebugger.joint.setSolidColor(color);
  }
}
