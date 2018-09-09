/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.FloatBuffer;
import java.util.TreeSet;

import com.ardor3d.bounding.CollisionTreeManager;
import com.ardor3d.extension.animation.skeletal.util.SkinUtils;
import com.ardor3d.math.Matrix4;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;
import com.google.common.collect.Sets;

/**
 * Mesh supporting deformation via skeletal animation.
 */
public class SkinnedMesh extends Mesh implements PoseListener {

    /**
     * Number of weights per vertex.
     */
    protected int _weightsPerVert = 1;

    /**
     * If true and we are using gpu skinning, we'll reorder our weights for matrix attribute use.
     */
    protected boolean _gpuUseMatrixAttribute = false;

    /**
     * Size to pad our attributes to. If we are using matrices (see {@link #setGpuUseMatrixAttribute(boolean)}) then
     * this is the size of an edge of the matrix. eg. 4 would mean either a vec4 or a mat4 object is expected in the
     * shader.
     */
    protected int _gpuAttributeSize = 4;

    /**
     * Storage for per vertex joint indices. There should be "weightsPerVert" entries per vertex.
     */
    protected short[] _jointIndices;

    /**
     * Storage for per vertex joint indices. These should already be normalized (all joints affecting the vertex add to
     * 1.) There should be "weightsPerVert" entries per vertex.
     */
    protected float[] _weights;

    /**
     * The original bind pose form of this SkinnedMesh. When doing CPU skinning, this will be used as a source and the
     * destination will go into the normal _meshData field for rendering. For GPU skinning, _meshData will be ignored
     * and only _bindPose will be sent to the card.
     */
    protected MeshData _bindPoseData = new MeshData();

    /**
     * The current skeleton pose we are targeting.
     */
    protected SkeletonPose _currentPose;

    /**
     * Flag for switching between GPU and CPU skinning.
     */
    protected boolean _useGPU;

    /**
     * <p>
     * Flag for enabling automatically updating the skin's model bound when the pose changes. Only effective in CPU
     * skinning mode. Default is false as this is currently expensive.
     * </p>
     *
     * XXX: If we can find a better way to update the bounds, maybe we should make this default to true or remove this
     * altogether.
     */
    protected boolean _autoUpdateSkinBound = false;

    /**
     * Custom update apply logic.
     */
    protected SkinPoseApplyLogic _customApplier = null;

    /**
     * Constructs a new SkinnedMesh.
     */
    public SkinnedMesh() {
        super();
    }

    /**
     * Constructs a new SkinnedMesh with a given name.
     *
     * @param name
     *                 the name of the skinned mesh.
     */
    public SkinnedMesh(final String name) {
        super(name);
    }

    /**
     * @return the bind pose MeshData object used by this skinned mesh.
     */
    public MeshData getBindPoseData() {
        return _bindPoseData;
    }

    /**
     * Sets the bind pose mesh data object used by this skinned mesh.
     *
     * @param poseData
     *                     the new bind pose
     */
    public void setBindPoseData(final MeshData poseData) {
        _bindPoseData = poseData;
    }

    /**
     * @return the number of weights and jointIndices this skin uses per vertex.
     */
    public int getWeightsPerVert() {
        return _weightsPerVert;
    }

    /**
     * @param weightsPerVert
     *                           the number of weights and jointIndices this skin should use per vertex. Make sure this
     *                           value matches up with the contents of jointIndices and weights.
     */
    public void setWeightsPerVert(final int weightsPerVert) {
        _weightsPerVert = weightsPerVert;
    }

    /**
     * @return true if we should use a matrix to send joints and weights to a gpu shader.
     */
    public boolean isGpuUseMatrixAttribute() {
        return _gpuUseMatrixAttribute;
    }

    /**
     * @param useMatrix
     *                      true if we should use a matrix to send joints and weights to a gpu shader.
     */
    public void setGpuUseMatrixAttribute(final boolean useMatrix) {
        _gpuUseMatrixAttribute = useMatrix;
    }

    /**
     * @return size to pad our attributes to. If we are using matrices (see {@link #setGpuUseMatrixAttribute(boolean)})
     *         then this is the size of an edge of the matrix. eg. 4 would mean either a vec4 or a mat4 object is
     *         expected in the shader.
     */
    public int getGpuAttributeSize() {
        return _gpuAttributeSize;
    }

    /**
     * @param size
     *                 Size to pad our attributes to. If we are using matrices (see
     *                 {@link #setGpuUseMatrixAttribute(boolean)}) then this is the size of an edge of the matrix. eg. 4
     *                 would mean either a vec4 or a mat4 object is expected in the shader.
     */
    public void setGpuAttributeSize(final int size) {
        _gpuAttributeSize = size;
    }

    /**
     * @return this skinned mesh's joint influences as indices into a Skeleton's Joint array.
     * @see #setJointIndices(short[])
     */
    public short[] getJointIndices() {
        return _jointIndices;
    }

    /**
     * Sets the joint indices used by this skinned mesh to compute mesh deformation. Each entry is interpreted as an
     * 16bit signed integer index into a Skeleton's Joint.
     *
     * @param jointIndices
     */
    public void setJointIndices(final short[] jointIndices) {
        _jointIndices = jointIndices;
        if (_jointIndices != null && _bindPoseData.containsKey("jointIds")) {
            recreateJointAttributeBuffer();
        }
    }

    /**
     * @return this skinned mesh's joint weights.
     * @see #setWeights(FloatBuffer)
     */
    public float[] getWeights() {
        return _weights;
    }

    /**
     * Sets the joint weights used by this skinned mesh.
     *
     * @param weights
     *                    the new weights.
     */
    public void setWeights(final float[] weights) {
        _weights = weights;
        if (_weights != null && _bindPoseData.containsKey("weights")) {
            recreateWeightAttributeBuffer();
        }
    }

    /**
     * @return a representation of the pose and skeleton to use for morphing this mesh.
     */
    public SkeletonPose getCurrentPose() {
        return _currentPose;
    }

    /**
     * @param currentPose
     *                        the representation responsible for the pose and skeleton to use for morphing this mesh.
     */
    public void setCurrentPose(final SkeletonPose currentPose) {
        if (_currentPose != null) {
            _currentPose.removePoseListener(this);
        }
        _currentPose = currentPose;
        if (_currentPose != null) {
            _currentPose.addPoseListener(this);
        }
    }

    /**
     * @return true if we should automatically update our model bounds when our pose updates. If useGPU is true, bounds
     *         are ignored.
     */
    public boolean isAutoUpdateSkinBounds() {
        return _autoUpdateSkinBound;
    }

    /**
     * @param autoUpdateSkinBound
     *                                true if we should automatically update our model bounds when our pose updates. If
     *                                useGPU is true, bounds are ignored.
     */
    public void setAutoUpdateSkinBounds(final boolean autoUpdateSkinBound) {
        _autoUpdateSkinBound = autoUpdateSkinBound;
    }

    /**
     * @return true if we are doing skinning on the card (GPU) or false if on the CPU.
     */
    public boolean isUseGPU() {
        return _useGPU;
    }

    /**
     * This should be set after setting up gpu attribute params.
     *
     * @param useGPU
     *                   true if we should do skinning on the card (GPU) or false if on the CPU.
     */
    public void setUseGPU(final boolean useGPU) {
        _useGPU = useGPU;
    }

    /**
     * @return any custom apply logic set on this skin or null if default logic is used.
     * @see #setCustomApplier(SkinPoseApplyLogic)
     */
    public SkinPoseApplyLogic getCustomApplier() {
        return _customApplier;
    }

    /**
     * Set custom logic for how this skin should react when it is told its pose has updated. This might include
     * throttling skin application, ignoring skin application when the skin is outside of the camera view, etc. If null,
     * (the default) the skin will always apply the new pose and optionally update the model bound.
     *
     * @param customApplier
     *                          the new custom logic, or null to use the default behavior.
     */
    public void setCustomApplier(final SkinPoseApplyLogic customApplier) {
        _customApplier = customApplier;
    }

    /**
     * Apply skinning values
     */
    public void applyPose() {
        if (_currentPose == null) {
            return;
        }

        if (isUseGPU()) {
            // Running skinning on the GPU
            if (!_bindPoseData.containsKey("weights")) {
                recreateWeightAttributeBuffer();
            }
            if (!_bindPoseData.containsKey("jointIds")) {
                recreateJointAttributeBuffer();
            }

            setProperty("jointPalette", _currentPose.getMatrixPalette());
            return;
        }

        // Running skinning on the CPU
        // Get a handle to the source and dest vertices buffers
        final FloatBuffer bindVerts = _bindPoseData.getVertexBuffer();
        FloatBuffer storeVerts = _meshData.getVertexBuffer();
        bindVerts.rewind();
        if (storeVerts == null || storeVerts.capacity() != bindVerts.capacity()) {
            storeVerts = BufferUtils.createFloatBuffer(bindVerts.capacity());
            _meshData.setVertexBuffer(storeVerts);
        } else {
            storeVerts.rewind();
        }

        // Get a handle to the source and dest normals buffers
        final FloatBuffer bindNorms = _bindPoseData.getNormalBuffer();
        FloatBuffer storeNorms = _meshData.getNormalBuffer();
        if (bindNorms != null) {
            bindNorms.rewind();

            if (storeNorms == null || storeNorms.capacity() < bindNorms.capacity()) {
                storeNorms = BufferUtils.createFloatBuffer(bindNorms.capacity());
                _meshData.setNormalBuffer(storeNorms);
            } else {
                storeNorms.rewind();
            }
        }

        Matrix4 jntMat;
        double bindVX, bindVY, bindVZ;
        double bindNX = 0, bindNY = 0, bindNZ = 0;
        double vSumX, vSumY, vSumZ;
        double nSumX = 0, nSumY = 0, nSumZ = 0;
        double tempX, tempY, tempZ;
        float weight;
        int jointIndex;

        // Cycle through each vertex
        for (int i = 0; i < _bindPoseData.getVertexCount(); i++) {
            // zero out our sum var
            vSumX = 0;
            vSumY = 0;
            vSumZ = 0;

            // Grab the bind pose vertex Vbp from _bindPoseData
            bindVX = bindVerts.get();
            bindVY = bindVerts.get();
            bindVZ = bindVerts.get();

            // See if we should do the corresponding normal as well
            if (bindNorms != null) {
                // zero out our sum var
                nSumX = 0;
                nSumY = 0;
                nSumZ = 0;

                // Grab the bind pose norm Nbp from _bindPoseData
                bindNX = bindNorms.get();
                bindNY = bindNorms.get();
                bindNZ = bindNorms.get();
            }

            // for each joint where the weight != 0
            for (int j = 0; j < getWeightsPerVert(); j++) {
                final int index = i * getWeightsPerVert() + j;
                if (_weights[index] == 0) {
                    continue;
                }

                jointIndex = _jointIndices[index];
                jntMat = _currentPose.getMatrixPalette()[jointIndex];
                weight = _weights[index];

                // Multiply our vertex by the matrix palette entry
                tempX = jntMat.getM00() * bindVX + jntMat.getM01() * bindVY + jntMat.getM02() * bindVZ
                        + jntMat.getM03();
                tempY = jntMat.getM10() * bindVX + jntMat.getM11() * bindVY + jntMat.getM12() * bindVZ
                        + jntMat.getM13();
                tempZ = jntMat.getM20() * bindVX + jntMat.getM21() * bindVY + jntMat.getM22() * bindVZ
                        + jntMat.getM23();

                // Sum, weighted.
                vSumX += tempX * weight;
                vSumY += tempY * weight;
                vSumZ += tempZ * weight;

                if (bindNorms != null) {
                    // Multiply our normal by the matrix palette entry
                    tempX = jntMat.getM00() * bindNX + jntMat.getM01() * bindNY + jntMat.getM02() * bindNZ;
                    tempY = jntMat.getM10() * bindNX + jntMat.getM11() * bindNY + jntMat.getM12() * bindNZ;
                    tempZ = jntMat.getM20() * bindNX + jntMat.getM21() * bindNY + jntMat.getM22() * bindNZ;

                    // Sum, weighted.
                    nSumX += tempX * weight;
                    nSumY += tempY * weight;
                    nSumZ += tempZ * weight;
                }
            }

            // Store sum into _meshData
            storeVerts.put((float) vSumX).put((float) vSumY).put((float) vSumZ);

            if (bindNorms != null) {
                storeNorms.put((float) nSumX).put((float) nSumY).put((float) nSumZ);
            }
        }

        _meshData.markBufferDirty(MeshData.KEY_VertexCoords);
        if (bindNorms != null) {
            _meshData.markBufferDirty(MeshData.KEY_NormalCoords);
        }
    }

    /**
     * Override render to allow for GPU/CPU switch
     */
    @Override
    public boolean render(final Renderer renderer) {
        if (!_useGPU) {
            // render as normal
            return super.render(renderer);
        } else {
            // render using the bind pose.
            return super.render(renderer, getBindPoseData());
        }
    }

    /**
     * Calls to apply our pose on pose update.
     */
    public void poseUpdated(final SkeletonPose pose) {
        // custom behavior?
        if (_customApplier != null) {
            _customApplier.doApply(this, pose);
        }

        // Just run our default behavior
        else {
            // update our pose
            applyPose();

            // update our model bounds
            if (!isUseGPU() && isAutoUpdateSkinBounds()) {
                updateModelBound();
            }
        }
    }

    @Override
    public void updateModelBound() {
        super.updateModelBound();
        // if we make our model bound accurate, also make the collision tree accurate
        CollisionTreeManager.INSTANCE.removeCollisionTree(this);
    }

    public void recreateJointAttributeBuffer() {
        final float[] data;
        if (isGpuUseMatrixAttribute()) {
            data = SkinUtils.reorderAndPad(SkinUtils.convertToFloat(_jointIndices), getWeightsPerVert(),
                    getGpuAttributeSize());
        } else {
            data = SkinUtils.pad(SkinUtils.convertToFloat(_jointIndices), getWeightsPerVert(), getGpuAttributeSize());
        }

        _bindPoseData.setCoords("jointIds",
                new FloatBufferData(BufferUtils.createFloatBuffer(data), getGpuAttributeSize()));
    }

    public void recreateWeightAttributeBuffer() {
        final float[] data;
        if (isGpuUseMatrixAttribute()) {
            data = SkinUtils.reorderAndPad(_weights, getWeightsPerVert(), getGpuAttributeSize());
        } else {
            data = SkinUtils.pad(_weights, getWeightsPerVert(), getGpuAttributeSize());
        }

        _bindPoseData.setCoords("weights",
                new FloatBufferData(BufferUtils.createFloatBuffer(data), getGpuAttributeSize()));
    }

    @Override
    public SkinnedMesh makeCopy(final boolean shareGeometricData) {
        final SkinnedMesh skin = (SkinnedMesh) super.makeCopy(shareGeometricData);

        // we don't want to share mesh data, just bind pose
        if (shareGeometricData) {
            // overriding parent's reuse
            skin._meshData = _meshData.makeCopy();
            // reuse
            skin._bindPoseData = _bindPoseData;
        } else {
            skin._bindPoseData = _bindPoseData.makeCopy();
        }

        skin._weightsPerVert = _weightsPerVert;
        skin._useGPU = _useGPU;
        skin._gpuUseMatrixAttribute = _gpuUseMatrixAttribute;
        skin._gpuAttributeSize = _gpuAttributeSize;
        skin._autoUpdateSkinBound = _autoUpdateSkinBound;
        skin._customApplier = _customApplier;

        // bring across arrays
        if (shareGeometricData) {
            skin._weights = _weights;
            skin._jointIndices = _jointIndices;
        } else {
            skin._weights = new float[_weights.length];
            System.arraycopy(_weights, 0, skin._weights, 0, _weights.length);
            skin._jointIndices = new short[_jointIndices.length];
            System.arraycopy(_jointIndices, 0, skin._jointIndices, 0, _jointIndices.length);
        }

        skin._currentPose = _currentPose;

        // make sure pose listener added
        if (skin._currentPose != null) {
            skin._currentPose.addPoseListener(skin);
        }

        return skin;
    }

    @Override
    public void reorderIndices(final IndexBufferData<?> newIndices, final IndexMode[] modes, final int[] lengths) {
        super.reorderIndices(newIndices, modes, lengths);
        _bindPoseData.setIndices(newIndices);
        _bindPoseData.setIndexModes(modes);
        _bindPoseData.setIndexLengths(lengths);
    }

    @Override
    public void reorderVertexData(final int[] newVertexOrder) {
        if (_meshData != null) {
            reorderVertexData(newVertexOrder, _meshData);
        }
        reorderVertexData(newVertexOrder, _bindPoseData);

        // reorder weight/joint information
        final float[] weights = new float[_weights.length];
        final short[] jointIndices = new short[_jointIndices.length];

        for (int i = 0; i < _bindPoseData.getVertexCount(); i++) {
            for (int j = 0; j < _weightsPerVert; j++) {
                final int oldIndex = i * _weightsPerVert + j;
                final int newIndex = newVertexOrder[i] * _weightsPerVert + j;
                weights[newIndex] = _weights[oldIndex];
                jointIndices[newIndex] = _jointIndices[oldIndex];
            }
        }

        setWeights(weights);
        setJointIndices(jointIndices);
    }

    /**
     * Rewrites the weights on this SkinnedMesh, if necessary, to reduce the number of weights per vert to the given
     * max. This is done by dropping the least significant weight and balancing the remainder to total 1.0 again.
     *
     * @param maxCount
     *                     the desired maximum weightsPerVert. If this is >= the current weightsPerVert, this method is
     *                     a NOOP.
     */
    public void constrainWeightCount(final int maxCount) {
        if (maxCount >= _weightsPerVert) {
            return;
        }

        // Generate new joint and weight buffers
        final int vcount = _weights.length / _weightsPerVert;
        final short[] joints = new short[vcount * maxCount];
        final float[] weights = new float[vcount * maxCount];

        final TreeSet<JointWeight> weightSort = Sets.newTreeSet();
        // Walk through old data vertex by vertex
        int index;
        for (int i = 0; i < vcount; i++) {
            weightSort.clear();
            for (int j = 0; j < _weightsPerVert; j++) {
                index = i * _weightsPerVert + j;
                weightSort.add(new JointWeight(_jointIndices[index], _weights[index]));
            }
            // go through and grab the top values
            float totalWeight = 0;
            index = 0;
            for (final JointWeight jw : weightSort) {
                if (index < maxCount) {
                    if (jw.weight > 0) {
                        totalWeight += jw.weight;
                        joints[i * maxCount + index] = jw.joint;
                        weights[i * maxCount + index] = jw.weight;
                        index++;
                    }
                } else {
                    break;
                }
            }
            if (totalWeight > 0) {
                // normalize
                for (int j = 0; j < maxCount; j++) {
                    weights[i * maxCount + j] /= totalWeight;
                }
            }
        }
        _weightsPerVert = maxCount;
        setJointIndices(joints);
        setWeights(weights);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends SkinnedMesh> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_weightsPerVert, "weightsPerVert", 1);
        capsule.write(_jointIndices, "jointIndices", null);
        capsule.write(_weights, "weights", null);
        capsule.write(_bindPoseData, "bindPoseData", null);
        capsule.write(_currentPose, "currentPose", null);
        capsule.write(_useGPU, "useGPU", false);
        capsule.write(_gpuAttributeSize, "gpuAttributeSize", 4);
        capsule.write(_gpuUseMatrixAttribute, "gpuUseMatrixAttribute", false);
        capsule.write(_autoUpdateSkinBound, "autoUpdateSkinBound", false);
        if (_customApplier instanceof Savable) {
            capsule.write((Savable) _customApplier, "customApplier", null);
        }
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _weightsPerVert = capsule.readInt("weightsPerVert", 1);
        _jointIndices = capsule.readShortArray("jointIndices", null);
        _weights = capsule.readFloatArray("weights", null);
        _bindPoseData = (MeshData) capsule.readSavable("bindPoseData", null);
        _currentPose = (SkeletonPose) capsule.readSavable("currentPose", null);
        _useGPU = capsule.readBoolean("useGPU", false);
        _gpuAttributeSize = capsule.readInt("gpuAttributeSize", 4);
        _gpuUseMatrixAttribute = capsule.readBoolean("gpuUseMatrixAttribute", false);
        _autoUpdateSkinBound = capsule.readBoolean("autoUpdateSkinBound", false);
        final SkinPoseApplyLogic customApplier = (SkinPoseApplyLogic) capsule.readSavable("customApplier", null);
        if (customApplier != null) {
            _customApplier = customApplier;
        }

        // make sure pose listener added
        if (_currentPose != null) {
            _currentPose.addPoseListener(this);
        }
    }

    class JointWeight implements Comparable<JointWeight> {
        short joint;
        float weight;

        public JointWeight(final short joint, final float weight) {
            this.joint = joint;
            this.weight = weight;
        }

        @Override
        public int hashCode() {
            int result = 17;

            // only care about joint
            result += 31 * result + joint;

            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof JointWeight)) {
                return false;
            }
            final JointWeight comp = (JointWeight) o;
            // only care about joint
            return joint == comp.joint;
        }

        @Override
        public int compareTo(final JointWeight o) {
            if (o.weight < weight) {
                return -1;
            } else if (o.weight > weight) {
                return 1;
            } else {
                return o.joint - joint;
            }
        }
    }

    public static void addDefaultResourceLocators() {
        try {
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MATERIAL,
                    new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(SkinnedMesh.class,
                            "com/ardor3d/extension/animation/skeletal/material")));
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_SHADER,
                    new SimpleResourceLocator(ResourceLocatorTool.getClassPathResource(SkinnedMesh.class,
                            "com/ardor3d/extension/animation/skeletal/shader")));
        } catch (final URISyntaxException ex) {
            ex.printStackTrace();
        }
    }
}
