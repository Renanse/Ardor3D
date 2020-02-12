/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Element;

import com.ardor3d.extension.animation.skeletal.AttachmentPoint;
import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.animation.skeletal.Skeleton;
import com.ardor3d.extension.animation.skeletal.SkeletonPose;
import com.ardor3d.extension.animation.skeletal.SkinnedMesh;
import com.ardor3d.extension.animation.skeletal.clip.AnimationClip;
import com.ardor3d.extension.animation.skeletal.clip.JointChannel;
import com.ardor3d.extension.animation.skeletal.clip.TransformChannel;
import com.ardor3d.extension.model.collada.jdom.ColladaInputPipe.ParamType;
import com.ardor3d.extension.model.collada.jdom.ColladaInputPipe.Type;
import com.ardor3d.extension.model.collada.jdom.data.AnimationItem;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.extension.model.collada.jdom.data.MeshVertPairs;
import com.ardor3d.extension.model.collada.jdom.data.SkinData;
import com.ardor3d.extension.model.collada.jdom.data.TransformElement;
import com.ardor3d.extension.model.collada.jdom.data.TransformElement.TransformElementType;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.AbstractBufferData.VBOAccessMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.export.binary.BinaryExporter;
import com.ardor3d.util.export.binary.BinaryImporter;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.geom.VertMap;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * Methods for parsing Collada data related to animation, skinning and morphing.
 */
public class ColladaAnimUtils {
    private static final Logger logger = Logger.getLogger(ColladaAnimUtils.class.getName());

    private final ColladaStorage _colladaStorage;
    private final DataCache _dataCache;
    private final ColladaDOMUtil _colladaDOMUtil;
    private final ColladaMeshUtils _colladaMeshUtils;

    public ColladaAnimUtils(final ColladaStorage colladaStorage, final DataCache dataCache,
            final ColladaDOMUtil colladaDOMUtil, final ColladaMeshUtils colladaMeshUtils) {
        _colladaStorage = colladaStorage;
        _dataCache = dataCache;
        _colladaDOMUtil = colladaDOMUtil;
        _colladaMeshUtils = colladaMeshUtils;
    }

    /**
     * Retrieve a name to use for the skin node based on the element names.
     *
     * @param ic
     *            instance_controller element.
     * @param controller
     *            controller element
     * @return name.
     * @see SkinData#SkinData(String)
     */
    private String getSkinStoreName(final Element ic, final Element controller) {
        final String controllerName = controller.getAttributeValue("name", (String) null) != null ? controller
                .getAttributeValue("name", (String) null) : controller.getAttributeValue("id", (String) null);
                final String instanceControllerName = ic.getAttributeValue("name", (String) null) != null ? ic
                        .getAttributeValue("name", (String) null) : ic.getAttributeValue("sid", (String) null);
                        final String storeName = (controllerName != null ? controllerName : "")
                                + (controllerName != null && instanceControllerName != null ? " : " : "")
                                + (instanceControllerName != null ? instanceControllerName : "");
                        return storeName;
    }

    /**
     * Copy the render states from our source Spatial to the destination Spatial. Does not recurse.
     *
     * @param source
     * @param target
     */
    private void copyRenderStates(final Spatial source, final Spatial target) {
        final EnumMap<StateType, RenderState> states = source.getLocalRenderStates();
        for (final RenderState state : states.values()) {
            target.setRenderState(state);
        }
    }

    /**
     * Clone the given MeshData object via deep copy using the Ardor3D BinaryExporter and BinaryImporter.
     *
     * @param meshData
     *            the source to clone.
     * @return the clone.
     * @throws IOException
     *             if we have troubles during the clone.
     */
    private MeshData copyMeshData(final MeshData meshData) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final BinaryExporter exporter = new BinaryExporter();
        exporter.save(meshData, bos);
        bos.flush();
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final BinaryImporter importer = new BinaryImporter();
        final Savable sav = importer.load(bis);
        return (MeshData) sav;
    }

    /**
     * Builds data based on an instance controller element.
     *
     * @param node
     *            Ardor3D parent Node
     * @param instanceController
     */
    void buildController(final Node node, final Element instanceController) {
        final Element controller = _colladaDOMUtil.findTargetWithId(instanceController.getAttributeValue("url"));

        if (controller == null) {
            throw new ColladaException("Unable to find controller with id: "
                    + instanceController.getAttributeValue("url"), instanceController);
        }

        final Element skin = controller.getChild("skin");
        if (skin != null) {
            buildSkinMeshes(node, instanceController, controller, skin);
        } else {
            // look for morph... can only be one or the other according to Collada
            final Element morph = controller.getChild("morph");
            if (morph != null) {
                buildMorphMeshes(node, controller, morph);
            }
        }
    }

    /**
     * Construct skin mesh(es) from the skin element and attach them (under a single new Node) to the given parent Node.
     *
     * @param ardorParentNode
     *            Ardor3D Node to attach our skin node to.
     * @param instanceController
     *            the <instance_controller> element. We'll parse the skeleton reference from here.
     * @param controller
     *            the referenced <controller> element. Used for naming purposes.
     * @param skin
     *            our <skin> element.
     */
    private void buildSkinMeshes(final Node ardorParentNode, final Element instanceController,
            final Element controller, final Element skin) {
        final String skinSource = skin.getAttributeValue("source");

        final Element skinNodeEL = _colladaDOMUtil.findTargetWithId(skinSource);
        if (skinNodeEL == null || !"geometry".equals(skinNodeEL.getName())) {
            throw new ColladaException("Expected a mesh for skin source with url: " + skinSource + " got instead: "
                    + skinNodeEL, skin);
        }

        final Element geometry = skinNodeEL;

        final Node meshNode = _colladaMeshUtils.buildMesh(geometry);
        if (meshNode != null) {
            // Look for skeleton entries in the original <instance_controller> element
            final List<Element> skeletonRoots = new ArrayList<>();
            for (final Element sk : instanceController.getChildren("skeleton")) {
                final Element skroot = _colladaDOMUtil.findTargetWithId(sk.getText());
                if (skroot != null) {
                    // add as a possible root for when we need to locate a joint by name later.
                    skeletonRoots.add(skroot);
                } else {
                    throw new ColladaException("Unable to find node with id: " + sk.getText()
                            + ", referenced from skeleton " + sk, sk);
                }
            }

            // Read in our joints node
            final Element jointsEL = skin.getChild("joints");
            if (jointsEL == null) {
                throw new ColladaException("skin found without joints.", skin);
            }

            // Pull out our joint names and bind matrices
            final List<String> jointNames = new ArrayList<>();
            final List<Transform> bindMatrices = new ArrayList<>();
            final List<ColladaInputPipe.ParamType> paramTypes = new ArrayList<>();

            for (final Element inputEL : jointsEL.getChildren("input")) {
                final ColladaInputPipe pipe = new ColladaInputPipe(_colladaDOMUtil, inputEL);
                final ColladaInputPipe.SourceData sd = pipe.getSourceData();
                if (pipe.getType() == ColladaInputPipe.Type.JOINT) {
                    final String[] namesData = sd.stringArray;
                    for (int i = sd.offset; i < namesData.length; i += sd.stride) {
                        jointNames.add(namesData[i]);
                        paramTypes.add(sd.paramType);
                    }
                } else if (pipe.getType() == ColladaInputPipe.Type.INV_BIND_MATRIX) {
                    final float[] floatData = sd.floatArray;
                    final FloatBuffer source = BufferUtils.createFloatBufferOnHeap(16);
                    for (int i = sd.offset; i < floatData.length; i += sd.stride) {
                        source.rewind();
                        source.put(floatData, i, 16);
                        source.flip();
                        final Matrix4 mat = new Matrix4().fromFloatBuffer(source);
                        bindMatrices.add(new Transform().fromHomogeneousMatrix(mat));
                    }
                }
            }

            // Use the skeleton information from the instance_controller to set the parent array locations on the
            // joints.
            Skeleton ourSkeleton = null; // TODO: maybe not the best way. iterate
            final int[] order = new int[jointNames.size()];
            for (int i = 0; i < jointNames.size(); i++) {
                final String name = jointNames.get(i);
                final ParamType paramType = paramTypes.get(i);
                final String searcher = paramType == ParamType.idref_param ? "id" : "sid";
                Element found = null;
                for (final Element root : skeletonRoots) {
                    if (name.equals(root.getAttributeValue(searcher))) {
                        found = root;
                    } else if (paramType == ParamType.idref_param) {
                        found = _colladaDOMUtil.findTargetWithId(name);
                    } else {
                        found = (Element) _colladaDOMUtil.selectSingleNode(root, ".//*[@sid='" + name + "']");
                    }

                    // Last resorts (bad exporters)
                    if (found == null) {
                        found = _colladaDOMUtil.findTargetWithId(name);
                    }
                    if (found == null) {
                        found = (Element) _colladaDOMUtil.selectSingleNode(root, ".//*[@name='" + name + "']");
                    }

                    if (found != null) {
                        break;
                    }
                }
                if (found == null) {
                    if (paramType == ParamType.idref_param) {
                        found = _colladaDOMUtil.findTargetWithId(name);
                    } else {
                        found = (Element) _colladaDOMUtil.selectSingleNode(geometry, "/*//visual_scene//*[@sid='"
                                + name + "']");
                    }

                    // Last resorts (bad exporters)
                    if (found == null) {
                        found = _colladaDOMUtil.findTargetWithId(name);
                    }
                    if (found == null) {
                        found = (Element) _colladaDOMUtil.selectSingleNode(geometry, "/*//visual_scene//*[@name='"
                                + name + "']");
                    }

                    if (found == null) {
                        throw new ColladaException("Unable to find joint with " + searcher + ": " + name, skin);
                    }
                }

                final Joint joint = _dataCache.getElementJointMapping().get(found);
                if (joint == null) {
                    logger.warning("unable to parse joint for: " + found.getName() + " " + name);
                    return;
                }
                joint.setInverseBindPose(bindMatrices.get(i));

                ourSkeleton = _dataCache.getJointSkeletonMapping().get(joint);
                order[i] = joint.getIndex();
            }

            // Make our skeleton pose
            SkeletonPose skPose = _dataCache.getSkeletonPoseMapping().get(ourSkeleton);
            if (skPose == null) {
                skPose = new SkeletonPose(ourSkeleton);
                _dataCache.getSkeletonPoseMapping().put(ourSkeleton, skPose);

                // attach any attachment points found for the skeleton's joints
                addAttachments(skPose);

                // Skeleton's default to bind position, so update the global transforms.
                skPose.updateTransforms();
            }

            // Read in our vertex_weights node
            final Element weightsEL = skin.getChild("vertex_weights");
            if (weightsEL == null) {
                throw new ColladaException("skin found without vertex_weights.", skin);
            }

            // Pull out our per vertex joint indices and weights
            final List<Short> jointIndices = new ArrayList<>();
            final List<Float> jointWeights = new ArrayList<>();
            int indOff = 0, weightOff = 0;

            int maxOffset = 0;
            for (final Element inputEL : weightsEL.getChildren("input")) {
                final ColladaInputPipe pipe = new ColladaInputPipe(_colladaDOMUtil, inputEL);
                final ColladaInputPipe.SourceData sd = pipe.getSourceData();
                if (pipe.getOffset() > maxOffset) {
                    maxOffset = pipe.getOffset();
                }
                if (pipe.getType() == ColladaInputPipe.Type.JOINT) {
                    indOff = pipe.getOffset();
                    final String[] namesData = sd.stringArray;
                    for (int i = sd.offset; i < namesData.length; i += sd.stride) {
                        // XXX: the Collada spec says this could be -1?
                        final String name = namesData[i];
                        final int index = jointNames.indexOf(name);
                        if (index >= 0) {
                            jointIndices.add((short) index);
                        } else {
                            throw new ColladaException("Unknown joint accessed: " + name, inputEL);
                        }
                    }
                } else if (pipe.getType() == ColladaInputPipe.Type.WEIGHT) {
                    weightOff = pipe.getOffset();
                    final float[] floatData = sd.floatArray;
                    for (int i = sd.offset; i < floatData.length; i += sd.stride) {
                        jointWeights.add(floatData[i]);
                    }
                }
            }

            // Pull our values array
            int firstIndex = 0, count = 0;
            final int[] vals = _colladaDOMUtil.parseIntArray(weightsEL.getChild("v"));
            try {
                count = weightsEL.getAttribute("count").getIntValue();
            } catch (final DataConversionException e) {
                throw new ColladaException("Unable to parse count attribute.", weightsEL);
            }
            // use the vals to fill our vert weight map
            final int[][] vertWeightMap = new int[count][];
            int index = 0;
            for (final int length : _colladaDOMUtil.parseIntArray(weightsEL.getChild("vcount"))) {
                final int[] entry = new int[(maxOffset + 1) * length];
                vertWeightMap[index++] = entry;

                System.arraycopy(vals, (maxOffset + 1) * firstIndex, entry, 0, entry.length);

                firstIndex += length;
            }

            // Create a record for the global ColladaStorage.
            final String storeName = getSkinStoreName(instanceController, controller);
            final SkinData skinDataStore = new SkinData(storeName);
            // add pose to store
            skinDataStore.setPose(skPose);

            // Create a base Node for our skin meshes
            final Node skinNode = new Node(meshNode.getName());
            // copy Node render states across.
            copyRenderStates(meshNode, skinNode);
            // add node to store
            skinDataStore.setSkinBaseNode(skinNode);

            // Grab the bind_shape_matrix from skin
            final Element bindShapeMatrixEL = skin.getChild("bind_shape_matrix");
            final Transform bindShapeMatrix = new Transform();
            if (bindShapeMatrixEL != null) {
                final double[] array = _colladaDOMUtil.parseDoubleArray(bindShapeMatrixEL);
                bindShapeMatrix.fromHomogeneousMatrix(new Matrix4().fromArray(array));
            }

            // Visit our Node and pull out any Mesh children. Turn them into SkinnedMeshes
            for (final Spatial spat : meshNode.getChildren()) {
                if (spat instanceof Mesh && ((Mesh) spat).getMeshData().getVertexCount() > 0) {
                    final Mesh sourceMesh = (Mesh) spat;
                    final SkinnedMesh skMesh = new SkinnedMesh(sourceMesh.getName());
                    skMesh.setCurrentPose(skPose);

                    // copy material info mapping for later use
                    final String material = _dataCache.getMeshMaterialMap().get(sourceMesh);
                    _dataCache.getMeshMaterialMap().put(skMesh, material);

                    // copy mesh render states across.
                    copyRenderStates(sourceMesh, skMesh);

                    // copy hints across
                    skMesh.getSceneHints().set(sourceMesh.getSceneHints());

                    try {
                        // Use source mesh as bind pose data in the new SkinnedMesh
                        final MeshData bindPose = copyMeshData(sourceMesh.getMeshData());
                        skMesh.setBindPoseData(bindPose);

                        // Apply our BSM
                        if (!bindShapeMatrix.isIdentity()) {
                            bindPose.transformVertices(bindShapeMatrix);
                            if (bindPose.getNormalBuffer() != null) {
                                bindPose.transformNormals(bindShapeMatrix, true);
                            }
                        }

                        // TODO: This is only needed for CPU skinning... consider a way of making it optional.
                        // Copy bind pose to mesh data to setup for CPU skinning
                        final MeshData meshData = copyMeshData(skMesh.getBindPoseData());
                        meshData.getVertexCoords().setVboAccessMode(VBOAccessMode.StreamDraw);
                        if (meshData.getNormalCoords() != null) {
                            meshData.getNormalCoords().setVboAccessMode(VBOAccessMode.StreamDraw);
                        }
                        skMesh.setMeshData(meshData);
                    } catch (final IOException e) {
                        e.printStackTrace();
                        throw new ColladaException("Unable to copy skeleton bind pose data.", geometry);
                    }

                    // Grab the MeshVertPairs from Global for this mesh.
                    final Collection<MeshVertPairs> vertPairsList = _dataCache.getVertMappings().get(geometry);
                    MeshVertPairs pairsMap = null;
                    if (vertPairsList != null) {
                        for (final MeshVertPairs pairs : vertPairsList) {
                            if (pairs.getMesh() == sourceMesh) {
                                pairsMap = pairs;
                                break;
                            }
                        }
                    }

                    if (pairsMap == null) {
                        throw new ColladaException("Unable to locate pair map for geometry.", geometry);
                    }

                    // Check for a remapping, if we optimized geometry
                    final VertMap vertMap = _dataCache.getMeshVertMap().get(sourceMesh);

                    // Use pairs map and vertWeightMap to build our weights and joint indices.
                    {
                        // count number of weights used
                        int maxWeightsPerVert = 0;
                        int weightCount;
                        for (final int originalIndex : pairsMap.getIndices()) {
                            weightCount = 0;

                            // get weights and joints at original index and add weights up to get divisor sum
                            // we'll assume 0's for vertices with no matching weight.
                            if (vertWeightMap.length > originalIndex) {
                                final int[] data = vertWeightMap[originalIndex];
                                for (int i = 0; i < data.length; i += maxOffset + 1) {
                                    final float weight = jointWeights.get(data[i + weightOff]);
                                    if (weight != 0) {
                                        weightCount++;
                                    }
                                }
                                if (weightCount > maxWeightsPerVert) {
                                    maxWeightsPerVert = weightCount;
                                }
                            }
                        }

                        final int verts = skMesh.getMeshData().getVertexCount();
                        final FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(verts * maxWeightsPerVert);
                        final ShortBuffer jointIndexBuffer = BufferUtils.createShortBuffer(verts * maxWeightsPerVert);
                        int j;
                        float sum = 0;
                        final float[] weights = new float[maxWeightsPerVert];
                        final short[] indices = new short[maxWeightsPerVert];
                        int originalIndex;
                        for (int x = 0; x < verts; x++) {
                            if (vertMap != null) {
                                originalIndex = pairsMap.getIndices()[vertMap.getFirstOldIndex(x)];
                            } else {
                                originalIndex = pairsMap.getIndices()[x];
                            }

                            j = 0;
                            sum = 0;

                            // get weights and joints at original index and add weights up to get divisor sum
                            // we'll assume 0's for vertices with no matching weight.
                            if (vertWeightMap.length > originalIndex) {
                                final int[] data = vertWeightMap[originalIndex];
                                for (int i = 0; i < data.length; i += maxOffset + 1) {
                                    final float weight = jointWeights.get(data[i + weightOff]);
                                    if (weight != 0) {
                                        weights[j] = jointWeights.get(data[i + weightOff]);
                                        indices[j] = (short) order[jointIndices.get(data[i + indOff])];
                                        sum += weights[j++];
                                    }
                                }
                            }
                            // add extra padding as needed
                            while (j < maxWeightsPerVert) {
                                weights[j] = 0;
                                indices[j++] = 0;
                            }
                            // add weights to weightBuffer / sum
                            for (final float w : weights) {
                                weightBuffer.put(sum != 0 ? w / sum : 0);
                            }
                            // add joint indices to jointIndexBuffer
                            jointIndexBuffer.put(indices);
                        }

                        final float[] totalWeights = new float[weightBuffer.capacity()];
                        weightBuffer.flip();
                        weightBuffer.get(totalWeights);
                        skMesh.setWeights(totalWeights);

                        final short[] totalIndices = new short[jointIndexBuffer.capacity()];
                        jointIndexBuffer.flip();
                        jointIndexBuffer.get(totalIndices);
                        skMesh.setJointIndices(totalIndices);

                        skMesh.setWeightsPerVert(maxWeightsPerVert);
                    }

                    // add to the skinNode.
                    skinNode.attachChild(skMesh);

                    // Manually apply our bind pose to the skin mesh.
                    skMesh.applyPose();

                    // Update the model bounding.
                    skMesh.updateModelBound();

                    // add mesh to store
                    skinDataStore.getSkins().add(skMesh);
                }
            }

            // add to Node
            ardorParentNode.attachChild(skinNode);

            // Add skin record to storage.
            _colladaStorage.getSkins().add(skinDataStore);
        }
    }

    private void addAttachments(final SkeletonPose skPose) {
        final Skeleton skeleton = skPose.getSkeleton();
        for (final Joint joint : skeleton.getJoints()) {
            if (_dataCache.getAttachmentPoints().containsKey(joint)) {
                for (final AttachmentPoint point : _dataCache.getAttachmentPoints().get(joint)) {
                    point.setJointIndex(joint.getIndex());
                    skPose.addPoseListener(point);
                }
            }
        }
    }

    /**
     * Construct morph mesh(es) from the <morph> element and attach them (under a single new Node) to the given parent
     * Node.
     *
     * Note: This method current does not do anything but attach the referenced mesh since Ardor3D does not yet support
     * morph target animation.
     *
     * @param ardorParentNode
     *            Ardor3D Node to attach our morph mesh to.
     * @param controller
     *            the referenced <controller> element. Used for naming purposes.
     * @param morph
     *            our <morph> element
     */
    private void buildMorphMeshes(final Node ardorParentNode, final Element controller, final Element morph) {
        final String skinSource = morph.getAttributeValue("source");

        final Element skinNode = _colladaDOMUtil.findTargetWithId(skinSource);
        if (skinNode == null || !"geometry".equals(skinNode.getName())) {
            throw new ColladaException("Expected a mesh for morph source with url: " + skinSource
                    + " (line number is referring morph)", morph);
        }

        final Element geometry = skinNode;

        final Spatial baseMesh = _colladaMeshUtils.buildMesh(geometry);

        // TODO: support morph animations someday.
        if (logger.isLoggable(Level.WARNING)) {
            logger.warning("Morph target animation not yet supported.");
        }

        // Just add mesh.
        if (baseMesh != null) {
            ardorParentNode.attachChild(baseMesh);
        }
    }

    /**
     * Parse all animations in library_animations
     *
     * @param colladaRoot
     */
    public void parseLibraryAnimations(final Element colladaRoot) {
        final Element libraryAnimations = colladaRoot.getChild("library_animations");

        if (libraryAnimations == null || libraryAnimations.getChildren().isEmpty()) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.warning("No animations found in collada file!");
            }
            return;
        }

        final AnimationItem animationItemRoot = new AnimationItem("Animation Root");
        _colladaStorage.setAnimationItemRoot(animationItemRoot);

        final Multimap<Element, TargetChannel> channelMap = ArrayListMultimap.create();

        parseAnimations(channelMap, libraryAnimations, animationItemRoot);

        for (final Element key : channelMap.keySet()) {
            buildAnimations(key, channelMap.get(key));
        }
    }

    /**
     * Merge all animation channels into Ardor jointchannels
     *
     * @param entry
     */
    private void buildAnimations(final Element parentElement, final Collection<TargetChannel> targetList) {

        final List<Element> elementTransforms = new ArrayList<Element>();
        for (final Element child : parentElement.getChildren()) {
            if (_dataCache.getTransformTypes().contains(child.getName())) {
                elementTransforms.add(child);
            }
        }
        final List<TransformElement> transformList = getNodeTransformList(elementTransforms);

        AnimationItem animationItemRoot = null;
        for (final TargetChannel targetChannel : targetList) {
            if (animationItemRoot == null) {
                animationItemRoot = targetChannel.animationItemRoot;
            }
            final String source = targetChannel.source;
            // final Target target = targetChannel.target;
            final Element targetNode = targetChannel.targetNode;

            final int targetIndex = elementTransforms.indexOf(targetNode);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(parentElement.getName() + "(" + parentElement.getAttributeValue("name") + ") -> "
                        + targetNode.getName() + "(" + targetIndex + ")");
            }

            final EnumMap<Type, ColladaInputPipe> pipes = Maps.newEnumMap(Type.class);

            final Element samplerElement = _colladaDOMUtil.findTargetWithId(source);
            for (final Element inputElement : samplerElement.getChildren("input")) {
                final ColladaInputPipe pipe = new ColladaInputPipe(_colladaDOMUtil, inputElement);
                pipes.put(pipe.getType(), pipe);
            }

            // get input (which is TIME for now)
            final ColladaInputPipe inputPipe = pipes.get(Type.INPUT);
            final ColladaInputPipe.SourceData sdIn = inputPipe.getSourceData();
            final float[] time = sdIn.floatArray;
            targetChannel.time = time;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("inputPipe: " + Arrays.toString(time));
            }

            // get output data
            final ColladaInputPipe outputPipe = pipes.get(Type.OUTPUT);
            final ColladaInputPipe.SourceData sdOut = outputPipe.getSourceData();
            final float[] animationData = sdOut.floatArray;
            targetChannel.animationData = animationData;
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("outputPipe: " + Arrays.toString(animationData));
            }

            // TODO: Need to add support for other interpolation types.

            // get target array from transform list
            final TransformElement transformElement = transformList.get(targetIndex);
            final double[] array = transformElement.getArray();
            targetChannel.array = array;

            final int stride = sdOut.stride;
            targetChannel.stride = stride;

            targetChannel.currentPos = 0;
        }

        final List<Float> finalTimeList = new ArrayList<>();
        final List<Transform> finalTransformList = new ArrayList<>();
        final List<TargetChannel> workingChannels = new ArrayList<>();
        for (;;) {
            float lowestTime = Float.MAX_VALUE;
            boolean found = false;
            for (final TargetChannel targetChannel : targetList) {
                if (targetChannel.currentPos < targetChannel.time.length) {
                    final float time = targetChannel.time[targetChannel.currentPos];
                    if (time < lowestTime) {
                        lowestTime = time;
                    }
                    found = true;
                }
            }
            if (!found) {
                break;
            }

            workingChannels.clear();
            for (final TargetChannel targetChannel : targetList) {
                if (targetChannel.currentPos < targetChannel.time.length) {
                    final float time = targetChannel.time[targetChannel.currentPos];
                    if (time == lowestTime) {
                        workingChannels.add(targetChannel);
                    }
                }
            }

            for (final TargetChannel targetChannel : workingChannels) {
                final Target target = targetChannel.target;
                final float[] animationData = targetChannel.animationData;
                final double[] array = targetChannel.array;

                // set the correct values depending on accessor
                final int position = targetChannel.currentPos * targetChannel.stride;
                if (target.accessorType == AccessorType.None) {
                    for (int j = 0; j < array.length; j++) {
                        array[j] = animationData[position + j];
                    }
                } else {
                    if (target.accessorType == AccessorType.Vector) {
                        array[target.accessorIndexX] = animationData[position];
                    } else if (target.accessorType == AccessorType.Matrix) {
                        array[target.accessorIndexY * 4 + target.accessorIndexX] = animationData[position];
                    }
                }
                targetChannel.currentPos++;
            }

            // bake the transform
            final Transform transform = bakeTransforms(transformList);
            finalTimeList.add(lowestTime);
            finalTransformList.add(transform);
        }

        final float[] time = new float[finalTimeList.size()];
        for (int i = 0; i < finalTimeList.size(); i++) {
            time[i] = finalTimeList.get(i);
        }
        final Transform[] transforms = finalTransformList.toArray(new Transform[finalTransformList.size()]);

        AnimationClip animationClip = animationItemRoot.getAnimationClip();
        if (animationClip == null) {
            animationClip = new AnimationClip(animationItemRoot.getName());
            animationItemRoot.setAnimationClip(animationClip);
        }

        // Make an animation channel - first find if we have a matching joint
        Joint joint = _dataCache.getElementJointMapping().get(parentElement);
        if (joint == null) {
            String nodeName = parentElement.getAttributeValue("name", (String) null);
            if (nodeName == null) { // use id if name doesn't exist
                nodeName = parentElement.getAttributeValue("id", parentElement.getName());
            }
            if (nodeName != null) {
                joint = _dataCache.getExternalJointMapping().get(nodeName);
            }

            if (joint == null) {
                // no joint still, so make a transform channel.
                final TransformChannel transformChannel = new TransformChannel(nodeName, time, transforms);
                animationClip.addChannel(transformChannel);
                _colladaStorage.getAnimationChannels().add(transformChannel);
                return;
            }
        }

        // create joint channel
        final JointChannel jointChannel = new JointChannel(joint, time, transforms);
        animationClip.addChannel(jointChannel);
        _colladaStorage.getAnimationChannels().add(jointChannel);
    }

    /**
     * Stores animation data to use for merging into jointchannels.
     */
    private static class TargetChannel {
        Target target;
        Element targetNode;
        String source;
        AnimationItem animationItemRoot;

        float[] time;
        float[] animationData;
        double[] array;
        int stride;
        int currentPos;

        public TargetChannel(final Target target, final Element targetNode, final String source,
                final AnimationItem animationItemRoot) {
            this.target = target;
            this.targetNode = targetNode;
            this.source = source;
            this.animationItemRoot = animationItemRoot;
        }
    }

    /**
     * Gather up all animation channels based on what nodes they affect.
     *
     * @param channelMap
     * @param animationRoot
     * @param animationItemRoot
     */
    private void parseAnimations(final Multimap<Element, TargetChannel> channelMap, final Element animationRoot,
            final AnimationItem animationItemRoot) {
        if (animationRoot.getChild("animation") != null) {
            Attribute nameAttribute = animationRoot.getAttribute("name");
            if (nameAttribute == null) {
                nameAttribute = animationRoot.getAttribute("id");
            }
            final String name = nameAttribute != null ? nameAttribute.getValue() : "Default";

            final AnimationItem animationItem = new AnimationItem(name);
            animationItemRoot.getChildren().add(animationItem);

            for (final Element animationElement : animationRoot.getChildren("animation")) {
                parseAnimations(channelMap, animationElement, animationItem);
            }
        }
        if (animationRoot.getChild("channel") != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("\n-- Parsing animation channels --");
            }
            final List<Element> channels = animationRoot.getChildren("channel");
            for (final Element channel : channels) {
                final String source = channel.getAttributeValue("source");

                final String targetString = channel.getAttributeValue("target");
                if (targetString == null || targetString.isEmpty()) {
                    return;
                }

                final Target target = processTargetString(targetString);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("channel source: " + target.toString());
                }
                final Element targetNode = findTargetNode(target);
                if (targetNode == null || !_dataCache.getTransformTypes().contains(targetNode.getName())) {
                    // TODO: pass with warning or exception or nothing?
                    // throw new ColladaException("No target transform node found for target: " + target, target);
                    continue;
                }
                if ("rotate".equals(targetNode.getName())) {
                    target.accessorType = AccessorType.Vector;
                    target.accessorIndexX = 3;
                }

                channelMap.put(targetNode.getParentElement(), new TargetChannel(target, targetNode, source,
                        animationItemRoot));
            }
        }
    }

    /**
     * Find a target node based on collada target format.
     *
     * @param target
     * @return
     */
    private Element findTargetNode(final Target target) {
        Element currentElement = _colladaDOMUtil.findTargetWithId(target.id);
        if (currentElement == null) {
            throw new ColladaException("No target found with id: " + target.id, target);
        }

        for (final String sid : target.sids) {
            final String query = ".//*[@sid='" + sid + "']";
            final Element sidElement = (Element) _colladaDOMUtil.selectSingleNode(currentElement, query);
            if (sidElement == null) {
                // throw new ColladaException("No element found with sid: " + sid, target);

                // TODO: this is a hack to support older 3ds max exports. will be removed and instead use
                // the above exception
                // logger.warning("No element found with sid: " + sid + ", trying with first child.");
                // final List<Element> children = currentElement.getChildren();
                // if (!children.isEmpty()) {
                // currentElement = children.get(0);
                // }
                // break;

                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("No element found with sid: " + sid + ", skipping channel.");
                }
                return null;
            } else {
                currentElement = sidElement;
            }
        }

        return currentElement;
    }

    private static final Map<String, Integer> symbolMap = new HashMap<>();
    static {
        symbolMap.put("ANGLE", 3);
        symbolMap.put("TIME", 0);

        symbolMap.put("X", 0);
        symbolMap.put("Y", 1);
        symbolMap.put("Z", 2);
        symbolMap.put("W", 3);

        symbolMap.put("R", 0);
        symbolMap.put("G", 1);
        symbolMap.put("B", 2);
        symbolMap.put("A", 3);

        symbolMap.put("S", 0);
        symbolMap.put("T", 1);
        symbolMap.put("P", 2);
        symbolMap.put("Q", 3);

        symbolMap.put("U", 0);
        symbolMap.put("V", 1);
        symbolMap.put("P", 2);
        symbolMap.put("Q", 3);
    }

    /**
     * Break up a target uri string into id, sids and accessors
     *
     * @param targetString
     * @return
     */
    private Target processTargetString(final String targetString) {
        final Target target = new Target();

        int accessorIndex = targetString.indexOf(".");
        if (accessorIndex == -1) {
            accessorIndex = targetString.indexOf("(");
        }
        final boolean hasAccessor = accessorIndex != -1;
        if (accessorIndex == -1) {
            accessorIndex = targetString.length();
        }

        final String baseString = targetString.substring(0, accessorIndex);

        int sidIndex = baseString.indexOf("/");
        final boolean hasSid = sidIndex != -1;
        if (!hasSid) {
            sidIndex = baseString.length();
        }

        final String id = baseString.substring(0, sidIndex);
        target.id = id;

        if (hasSid) {
            final String sidGroup = baseString.substring(sidIndex + 1, baseString.length());

            final StringTokenizer tokenizer = new StringTokenizer(sidGroup, "/");
            while (tokenizer.hasMoreTokens()) {
                final String sid = tokenizer.nextToken();
                target.sids.add(sid);
            }
        }

        if (hasAccessor) {
            String accessorString = targetString.substring(accessorIndex, targetString.length());
            accessorString = accessorString.replace(".", "");

            if (accessorString.length() > 0 && accessorString.charAt(0) == '(') {
                int endPara = accessorString.indexOf(")");
                final String indexXString = accessorString.substring(1, endPara);
                target.accessorIndexX = Integer.parseInt(indexXString);
                if (endPara < accessorString.length() - 1) {
                    final String lastAccessorString = accessorString.substring(endPara + 1, accessorString.length());
                    endPara = lastAccessorString.indexOf(")");
                    final String indexYString = lastAccessorString.substring(1, endPara);
                    target.accessorIndexY = Integer.parseInt(indexYString);
                    target.accessorType = AccessorType.Matrix;
                } else {
                    target.accessorType = AccessorType.Vector;
                }
            } else {
                target.accessorIndexX = symbolMap.get(accessorString);
                target.accessorType = AccessorType.Vector;
            }
        }

        return target;
    }

    /**
     * Convert a list of collada elements into a list of TransformElements
     *
     * @param transforms
     * @return
     */
    private List<TransformElement> getNodeTransformList(final List<Element> transforms) {
        final List<TransformElement> transformList = new ArrayList<>();

        for (final Element transform : transforms) {
            final double[] array = _colladaDOMUtil.parseDoubleArray(transform);

            if ("translate".equals(transform.getName())) {
                transformList.add(new TransformElement(array, TransformElementType.Translation));
            } else if ("rotate".equals(transform.getName())) {
                transformList.add(new TransformElement(array, TransformElementType.Rotation));
            } else if ("scale".equals(transform.getName())) {
                transformList.add(new TransformElement(array, TransformElementType.Scale));
            } else if ("matrix".equals(transform.getName())) {
                transformList.add(new TransformElement(array, TransformElementType.Matrix));
            } else if ("lookat".equals(transform.getName())) {
                transformList.add(new TransformElement(array, TransformElementType.Lookat));
            } else {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("transform not currently supported: " + transform.getClass().getCanonicalName());
                }
            }
        }

        return transformList;
    }

    /**
     * Bake a list of TransformElements into an Ardor3D Transform object.
     *
     * @param transforms
     * @return
     */
    private Transform bakeTransforms(final List<TransformElement> transforms) {
        final Matrix4 workingMat = Matrix4.fetchTempInstance();
        final Matrix4 finalMat = Matrix4.fetchTempInstance();
        finalMat.setIdentity();
        for (final TransformElement transform : transforms) {
            final double[] array = transform.getArray();
            final TransformElementType type = transform.getType();
            if (type == TransformElementType.Translation) {
                workingMat.setIdentity();
                workingMat.setColumn(3, new Vector4(array[0], array[1], array[2], 1.0));
                finalMat.multiplyLocal(workingMat);
            } else if (type == TransformElementType.Rotation) {
                if (array[3] != 0) {
                    workingMat.setIdentity();
                    final Matrix3 rotate = new Matrix3().fromAngleAxis(array[3] * MathUtils.DEG_TO_RAD, new Vector3(
                            array[0], array[1], array[2]));
                    workingMat.set(rotate);
                    finalMat.multiplyLocal(workingMat);
                }
            } else if (type == TransformElementType.Scale) {
                workingMat.setIdentity();
                workingMat.scale(new Vector4(array[0], array[1], array[2], 1), workingMat);
                finalMat.multiplyLocal(workingMat);
            } else if (type == TransformElementType.Matrix) {
                workingMat.fromArray(array);
                finalMat.multiplyLocal(workingMat);
            } else if (type == TransformElementType.Lookat) {
                final Vector3 pos = new Vector3(array[0], array[1], array[2]);
                final Vector3 target = new Vector3(array[3], array[4], array[5]);
                final Vector3 up = new Vector3(array[6], array[7], array[8]);
                final Matrix3 rot = new Matrix3();
                rot.lookAt(target.subtractLocal(pos), up);
                workingMat.set(rot);
                workingMat.setColumn(3, new Vector4(array[0], array[1], array[2], 1.0));
                finalMat.multiplyLocal(workingMat);
            } else {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("transform not currently supported: " + transform.getClass().getCanonicalName());
                }
            }
        }
        return new Transform().fromHomogeneousMatrix(finalMat);
    }

    /**
     * Util for making a readable string out of a xml element hierarchy
     *
     * @param e
     * @param maxDepth
     * @return
     */
    public static String getElementString(final Element e, final int maxDepth) {
        return getElementString(e, maxDepth, true);
    }

    public static String getElementString(final Element e, final int maxDepth, final boolean showDots) {
        final StringBuilder str = new StringBuilder();
        getElementString(e, str, 0, maxDepth, showDots);
        return str.toString();
    }

    private static void getElementString(final Element e, final StringBuilder str, final int depth, final int maxDepth,
            final boolean showDots) {
        addSpacing(str, depth);
        str.append('<');
        str.append(e.getName());
        str.append(' ');
        final List<Attribute> attrs = e.getAttributes();
        for (int i = 0; i < attrs.size(); i++) {
            final Attribute attr = attrs.get(i);
            str.append(attr.getName());
            str.append("=\"");
            str.append(attr.getValue());
            str.append('"');
            if (i < attrs.size() - 1) {
                str.append(' ');
            }
        }
        if (!e.getChildren().isEmpty() || !"".equals(e.getText())) {
            str.append('>');
            if (depth < maxDepth) {
                str.append('\n');
                for (final Element child : e.getChildren()) {
                    getElementString(child, str, depth + 1, maxDepth, showDots);
                }
                if (!"".equals(e.getText())) {
                    addSpacing(str, depth + 1);
                    str.append(e.getText());
                    str.append('\n');
                }
            } else if (showDots) {
                str.append('\n');
                addSpacing(str, depth + 1);
                str.append("...");
                str.append('\n');
            }
            addSpacing(str, depth);
            str.append("</");
            str.append(e.getName());
            str.append('>');
        } else {
            str.append("/>");
        }
        str.append('\n');
    }

    private static void addSpacing(final StringBuilder str, final int depth) {
        for (int i = 0; i < depth; i++) {
            str.append("  ");
        }
    }

    private enum AccessorType {
        None, Vector, Matrix
    }

    private static class Target {
        public String id;
        public List<String> sids = new ArrayList<>();
        public AccessorType accessorType = AccessorType.None;
        public int accessorIndexX = -1, accessorIndexY = -1;

        @Override
        public String toString() {
            if (accessorType == AccessorType.None) {
                return "Target [accessorType=" + accessorType + ", id=" + id + ", sids=" + sids + "]";
            }
            return "Target [accessorType=" + accessorType + ", accessorIndexX=" + accessorIndexX + ", accessorIndexY="
            + accessorIndexY + ", id=" + id + ", sids=" + sids + "]";
        }
    }
}
