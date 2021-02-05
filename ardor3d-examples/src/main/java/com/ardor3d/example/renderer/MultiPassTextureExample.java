/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Texture;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.extension.PassNode;
import com.ardor3d.scenegraph.extension.PassNodeState;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Borrows from the BoxExample to illustrate using PassNode to do simple multi-texturing.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.renderer.MultiPassTextureExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_MultiPassTextureExample.jpg", //
    maxHeapMemory = 64)
public class MultiPassTextureExample extends ExampleBase {

  /** Keep a reference to the box to be able to rotate it each frame. */
  private Mesh box;

  /** Rotation matrix for the spinning box. */
  private final Matrix3 rotate = new Matrix3();

  /** Angle of rotation for the box. */
  private double angle = 0;

  /** Axis to rotate the box around. */
  private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();

  public static void main(final String[] args) {
    start(MultiPassTextureExample.class);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    // Update the angle using the current tpf to rotate at a constant speed.
    angle += timer.getTimePerFrame() * 50;
    // Wrap the angle to keep it inside 0-360 range
    angle %= 360;

    // Update the rotation matrix using the angle and rotation axis.
    rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
    // Update the box rotation using the rotation matrix.
    box.setRotation(rotate);
  }

  @Override
  protected void initExample() {
    // turn off lighting calcs, it looks a little nicer.
    _lightState.setEnabled(false);

    _canvas.setTitle("Multi-pass Texture Example");

    // Create a new box centered at (0,0,0) with width/height/depth of size 10.
    box = new Box("Box", new Vector3(0, 0, 0), 5, 5, 5);
    // Set a bounding box for frustum culling.
    box.setModelBound(new BoundingBox());
    // Move the box out from the camera 15 units.
    box.setTranslation(new Vector3(0, 0, -15));

    // Create our states to use in the passes
    final TextureState ts1 = new TextureState();
    ts1.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, true));

    final TextureState ts2 = new TextureState();
    ts2.setTexture(TextureManager.load("images/flaresmall.jpg", Texture.MinificationFilter.Trilinear, true));

    final BlendState as = new BlendState();
    as.setBlendEnabled(true);
    as.setSourceFunction(BlendState.SourceFunction.DestinationColor);
    as.setDestinationFunction(BlendState.DestinationFunction.SourceColor);

    // Set up our passes
    final PassNodeState pass1 = new PassNodeState();
    pass1.setPassState(ts1);

    final PassNodeState pass2 = new PassNodeState();
    pass2.setPassState(ts2);
    pass2.setPassState(as);

    // Add the passes to the pass node
    final PassNode pNode = new PassNode();
    pNode.addPass(pass1);
    pNode.addPass(pass2);

    // Attach the box to the pass node.
    pNode.attachChild(box);

    // Attach the pass node to the scenegraph root.
    _root.attachChild(pNode);
  }
}
