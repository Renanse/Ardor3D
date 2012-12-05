/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jdom2.Element;

import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.Skeleton;
import com.ardor3d.extension.model.collada.jdom.data.AssetData;
import com.ardor3d.extension.model.collada.jdom.data.ControllerStore;
import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.extension.model.collada.jdom.data.JointNode;
import com.ardor3d.extension.model.collada.jdom.data.NodeType;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.google.common.collect.Lists;

/**
 * Methods for parsing Collada data related to scenes and node hierarchy.
 */
public class ColladaNodeUtils {
    private static final Logger logger = Logger.getLogger(ColladaNodeUtils.class.getName());

    private final DataCache _dataCache;
    private final ColladaDOMUtil _colladaDOMUtil;
    private final ColladaMaterialUtils _colladaMaterialUtils;
    private final ColladaMeshUtils _colladaMeshUtils;
    private final ColladaAnimUtils _colladaAnimUtils;

    public ColladaNodeUtils(final DataCache dataCache, final ColladaDOMUtil colladaDOMUtil,
            final ColladaMaterialUtils colladaMaterialUtils, final ColladaMeshUtils colladaMeshUtils,
            final ColladaAnimUtils colladaAnimUtils) {
        _dataCache = dataCache;
        _colladaDOMUtil = colladaDOMUtil;
        _colladaMaterialUtils = colladaMaterialUtils;
        _colladaMeshUtils = colladaMeshUtils;
        _colladaAnimUtils = colladaAnimUtils;
    }

    /**
     * Retrieves the scene and returns it as an Ardor3D Node.
     * 
     * @param colladaRoot
     *            The collada root element
     * @return Scene as an Node or null if not found
     */
    @SuppressWarnings("unchecked")
    public Node getVisualScene(final Element colladaRoot) {
        if (colladaRoot.getChild("scene") == null) {
            logger.warning("No scene found in collada file!");
            return null;
        }

        final Element instance_visual_scene = colladaRoot.getChild("scene").getChild("instance_visual_scene");
        if (instance_visual_scene == null) {
            logger.warning("No instance_visual_scene found in collada file!");
            return null;
        }

        final Element visualScene = _colladaDOMUtil.findTargetWithId(instance_visual_scene.getAttributeValue("url"));

        if (visualScene != null) {
            final Node sceneRoot = new Node(
                    visualScene.getAttributeValue("name") != null ? visualScene.getAttributeValue("name")
                            : "Collada Root");

            // Load each sub node and attach
            final JointNode baseJointNode = new JointNode(null);
            _dataCache.setRootJointNode(baseJointNode);
            for (final Element n : visualScene.getChildren("node")) {
                final Node subNode = buildNode(n, baseJointNode);
                if (subNode != null) {
                    sceneRoot.attachChild(subNode);
                }
            }

            // build a list of joints - one list per skeleton
            final List<List<Joint>> jointCollection = Lists.newArrayList();
            for (final JointNode jointChildNode : _dataCache.getRootJointNode().getChildren()) {
                final List<Joint> jointList = Lists.newArrayList();
                buildJointLists(jointChildNode, jointList);
                jointCollection.add(jointList);
            }

            // build a skeleton for each joint list.
            for (final List<Joint> jointList : jointCollection) {
                final Joint[] joints = jointList.toArray(new Joint[jointList.size()]);
                final Skeleton skeleton = new Skeleton(joints[0].getName() + "_skeleton", joints);
                logger.fine(skeleton.getName());
                for (final Joint joint : jointList) {
                    _dataCache.getJointSkeletonMapping().put(joint, skeleton);
                    logger.fine("- Joint " + joint.getName() + " - index: " + joint.getIndex() + " parent index: "
                            + joint.getParentIndex());
                }
                _dataCache.addSkeleton(skeleton);
            }

            // update our world transforms so we can use them to init the default bind matrix of any joints.
            sceneRoot.updateWorldTransform(true);
            initDefaultJointTransforms(baseJointNode);

            // Setup our skinned mesh objects.
            for (final ControllerStore controllerStore : _dataCache.getControllers()) {
                _colladaMaterialUtils.bindMaterials(controllerStore.instanceController.getChild("bind_material"));
                _colladaAnimUtils.buildController(controllerStore.ardorParentNode, controllerStore.instanceController);
                _colladaMaterialUtils.unbindMaterials(controllerStore.instanceController.getChild("bind_material"));
            }

            return sceneRoot;
        }
        return null;
    }

    private void buildJointLists(final JointNode jointNode, final List<Joint> jointList) {
        final Joint joint = jointNode.getJoint();
        joint.setIndex((short) jointList.size());
        if (jointNode.getParent().getJoint() != null) {
            joint.setParentIndex(jointNode.getParent().getJoint().getIndex());
        } else {
            joint.setParentIndex(Joint.NO_PARENT);
        }
        jointList.add(joint);
        for (final JointNode jointChildNode : jointNode.getChildren()) {
            buildJointLists(jointChildNode, jointList);
        }
    }

    private void initDefaultJointTransforms(final JointNode jointNode) {
        final Joint j = jointNode.getJoint();
        if (j != null && jointNode.getSceneNode() != null) {
            j.setInverseBindPose(jointNode.getSceneNode().getWorldTransform().invert(null));
        }
        for (final JointNode jointChildNode : jointNode.getChildren()) {
            initDefaultJointTransforms(jointChildNode);
        }
    }

    /**
     * Parse an asset element into an AssetData object.
     * 
     * @param asset
     * @return
     */
    @SuppressWarnings("unchecked")
    public AssetData parseAsset(final Element asset) {
        final AssetData assetData = new AssetData();

        for (final Element child : asset.getChildren()) {
            if ("contributor".equals(child.getName())) {
                parseContributor(assetData, child);
            } else if ("created".equals(child.getName())) {
                assetData.setCreated(child.getText());
            } else if ("keywords".equals(child.getName())) {
                assetData.setKeywords(child.getText());
            } else if ("modified".equals(child.getName())) {
                assetData.setModified(child.getText());
            } else if ("revision".equals(child.getName())) {
                assetData.setRevision(child.getText());
            } else if ("subject".equals(child.getName())) {
                assetData.setSubject(child.getText());
            } else if ("title".equals(child.getName())) {
                assetData.setTitle(child.getText());
            } else if ("unit".equals(child.getName())) {
                final String name = child.getAttributeValue("name");
                if (name != null) {
                    assetData.setUnitName(name);
                }
                final String meter = child.getAttributeValue("meter");
                if (meter != null) {
                    assetData.setUnitMeter(Float.parseFloat(meter.replace(",", ".")));
                }
            } else if ("up_axis".equals(child.getName())) {
                final String axis = child.getText();
                if ("X_UP".equals(axis)) {
                    assetData.setUpAxis(new Vector3());
                } else if ("Y_UP".equals(axis)) {
                    assetData.setUpAxis(Vector3.UNIT_Y);
                } else if ("Z_UP".equals(axis)) {
                    assetData.setUpAxis(Vector3.UNIT_Z);
                }
            }
        }

        return assetData;
    }

    @SuppressWarnings("unchecked")
    private void parseContributor(final AssetData assetData, final Element contributor) {
        for (final Element child : contributor.getChildren()) {
            if ("author".equals(child.getName())) {
                assetData.setAuthor(child.getText());
            } else if ("authoringTool".equals(child.getName())) {
                assetData.setCreated(child.getText());
            } else if ("comments".equals(child.getName())) {
                assetData.setComments(child.getText());
            } else if ("copyright".equals(child.getName())) {
                assetData.setCopyright(child.getText());
            } else if ("source_data".equals(child.getName())) {
                assetData.setSourceData(child.getText());
            }
        }
    }

    /**
     * @param instanceNode
     * @return a new Ardor3D node, created from the <node> pointed to by the given <instance_node> element
     */
    public Node getNode(final Element instanceNode, final JointNode jointNode) {
        final Element node = _colladaDOMUtil.findTargetWithId(instanceNode.getAttributeValue("url"));

        if (node == null) {
            throw new ColladaException("No node with id: " + instanceNode.getAttributeValue("url") + " found",
                    instanceNode);
        }

        return buildNode(node, jointNode);
    }

    /**
     * Recursively parse the node hierarcy.
     * 
     * @param dNode
     * @return a new Ardor3D node, created from the given <node> element
     */
    @SuppressWarnings("unchecked")
    private Node buildNode(final Element dNode, JointNode jointNode) {
        NodeType nodeType = NodeType.NODE;
        if (dNode.getAttribute("type") != null) {
            nodeType = Enum.valueOf(NodeType.class, dNode.getAttributeValue("type"));
        }
        final JointNode jointChildNode;
        if (nodeType == NodeType.JOINT) {
            String name = dNode.getAttributeValue("name");
            if (name == null) {
                name = dNode.getAttributeValue("id");
            }
            if (name == null) {
                name = dNode.getAttributeValue("sid");
            }
            final Joint joint = new Joint(name);
            jointChildNode = new JointNode(joint);
            jointChildNode.setParent(jointNode);
            jointNode.getChildren().add(jointChildNode);
            jointNode = jointChildNode;

            _dataCache.getElementJointMapping().put(dNode, joint);
        } else {
            jointChildNode = null;
        }

        String nodeName = dNode.getAttributeValue("name", (String) null);
        if (nodeName == null) { // use id if name doesn't exist
            nodeName = dNode.getAttributeValue("id", dNode.getName());
        }
        final Node node = new Node(nodeName);

        final List<Element> transforms = new ArrayList<Element>();
        for (final Element child : dNode.getChildren()) {
            if (_dataCache.getTransformTypes().contains(child.getName())) {
                transforms.add(child);
            }
        }

        // process any transform information.
        if (!transforms.isEmpty()) {
            final Transform localTransform = getNodeTransforms(transforms);

            node.setTransform(localTransform);
            if (jointChildNode != null) {
                jointChildNode.setSceneNode(node);
            }
        }

        // process any instance geometries
        for (final Element instance_geometry : dNode.getChildren("instance_geometry")) {
            _colladaMaterialUtils.bindMaterials(instance_geometry.getChild("bind_material"));

            final Spatial mesh = _colladaMeshUtils.getGeometryMesh(instance_geometry);
            if (mesh != null) {
                node.attachChild(mesh);
            }

            _colladaMaterialUtils.unbindMaterials(instance_geometry.getChild("bind_material"));
        }

        // process any instance controllers
        for (final Element instanceController : dNode.getChildren("instance_controller")) {
            _dataCache.getControllers().add(new ControllerStore(node, instanceController));
        }

        // process any instance nodes
        for (final Element in : dNode.getChildren("instance_node")) {
            final Node subNode = getNode(in, jointNode);
            if (subNode != null) {
                node.attachChild(subNode);
            }
        }

        // process any concrete child nodes.
        for (final Element n : dNode.getChildren("node")) {
            final Node subNode = buildNode(n, jointNode);
            if (subNode != null) {
                node.attachChild(subNode);
            }
        }

        // Cache reference
        _dataCache.getElementSpatialMapping().put(dNode, node);

        return node;
    }

    /**
     * Combines a list of transform elements into an Ardor3D Transform object.
     * 
     * @param transforms
     *            List of transform elements
     * @return an Ardor3D Transform object
     */
    public Transform getNodeTransforms(final List<Element> transforms) {
        final Matrix4 workingMat = Matrix4.fetchTempInstance();
        final Matrix4 finalMat = Matrix4.fetchTempInstance();
        finalMat.setIdentity();
        for (final Element transform : transforms) {
            final double[] array = _colladaDOMUtil.parseDoubleArray(transform);
            if ("translate".equals(transform.getName())) {
                workingMat.setIdentity();
                workingMat.setColumn(3, new Vector4(array[0], array[1], array[2], 1.0));
                finalMat.multiplyLocal(workingMat);
            } else if ("rotate".equals(transform.getName())) {
                if (array[3] != 0) {
                    workingMat.setIdentity();
                    final Matrix3 rotate = new Matrix3().fromAngleAxis(array[3] * MathUtils.DEG_TO_RAD, new Vector3(
                            array[0], array[1], array[2]));
                    workingMat.set(rotate);
                    finalMat.multiplyLocal(workingMat);
                }
            } else if ("scale".equals(transform.getName())) {
                workingMat.setIdentity();
                workingMat.scale(new Vector4(array[0], array[1], array[2], 1), workingMat);
                finalMat.multiplyLocal(workingMat);
            } else if ("matrix".equals(transform.getName())) {
                workingMat.fromArray(array);
                finalMat.multiplyLocal(workingMat);
            } else if ("lookat".equals(transform.getName())) {
                final Vector3 pos = new Vector3(array[0], array[1], array[2]);
                final Vector3 target = new Vector3(array[3], array[4], array[5]);
                final Vector3 up = new Vector3(array[6], array[7], array[8]);
                final Matrix3 rot = new Matrix3();
                rot.lookAt(target.subtractLocal(pos), up);
                workingMat.set(rot);
                workingMat.setColumn(3, new Vector4(array[0], array[1], array[2], 1));
                finalMat.multiplyLocal(workingMat);
            } else {
                logger.warning("transform not currently supported: " + transform.getClass().getCanonicalName());
            }
        }
        return new Transform().fromHomogeneousMatrix(finalMat);
    }
}
