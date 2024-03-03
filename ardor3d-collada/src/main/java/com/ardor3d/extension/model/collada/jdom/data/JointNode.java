/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom.data;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.scenegraph.Node;

public class JointNode {
  private JointNode parent;
  private final List<JointNode> children = new ArrayList<>();
  private final Joint joint;

  /** Scene node associated with the Joint. */
  private Node sceneNode;

  public JointNode(final Joint joint) {
    this.joint = joint;
  }

  public List<JointNode> getChildren() { return children; }

  public Joint getJoint() { return joint; }

  public JointNode getParent() { return parent; }

  public void setParent(final JointNode parent) { this.parent = parent; }

  public Node getSceneNode() { return sceneNode; }

  public void setSceneNode(final Node node) { sceneNode = node; }
}
