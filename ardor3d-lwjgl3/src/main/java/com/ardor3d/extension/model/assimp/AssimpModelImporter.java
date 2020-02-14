/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.extension.model.assimp;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIFile;
import org.lwjgl.assimp.AIFileCloseProc;
import org.lwjgl.assimp.AIFileCloseProcI;
import org.lwjgl.assimp.AIFileIO;
import org.lwjgl.assimp.AIFileOpenProc;
import org.lwjgl.assimp.AIFileOpenProcI;
import org.lwjgl.assimp.AIFileReadProc;
import org.lwjgl.assimp.AIFileReadProcI;
import org.lwjgl.assimp.AIFileSeek;
import org.lwjgl.assimp.AIFileSeekI;
import org.lwjgl.assimp.AIFileTellProc;
import org.lwjgl.assimp.AIFileTellProcI;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryStack;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Transform;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.CullState.Face;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.surface.ColorSurface;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.RelativeResourceLocator;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

/**
 * Uses Lwjgl's Assimp binding to attempt to load models and convert them to an Ardor3D scenegraph.
 *
 * <p>
 * <b>Known issues:</b>
 * <ul>
 * <li>Loading MD2 models will only give you the first keyframe. Use Ardor3D's Md2Importer if you need frame
 * animation.</li>
 * </ul>
 * </p>
 */
public class AssimpModelImporter {

    private boolean _loadTextures = true;
    private ResourceLocator _textureLocator;
    private ResourceLocator _relativeLocator;
    private ResourceLocator _modelLocator;

    // texture defaults
    private MinificationFilter _minificationFilter = MinificationFilter.Trilinear;
    private boolean _useCompression = true;
    private boolean _flipTextureVertically = true;

    private ResourceSource _originalResource;

    public AssimpModelImporter setTextureLocator(final ResourceLocator locator) {
        _textureLocator = locator;
        return this;
    }

    public AssimpModelImporter setModelLocator(final ResourceLocator locator) {
        _modelLocator = locator;
        return this;
    }

    public boolean isLoadTextures() {
        return _loadTextures;
    }

    public void setLoadTextures(final boolean loadTextures) {
        _loadTextures = loadTextures;
    }

    public void setFlipTextureVertically(final boolean flipTextureVertically) {
        _flipTextureVertically = flipTextureVertically;
    }

    public boolean isFlipTextureVertically() {
        return _flipTextureVertically;
    }

    public void setUseCompression(final boolean useCompression) {
        _useCompression = useCompression;
    }

    public boolean isUseCompression() {
        return _useCompression;
    }

    public void setMinificationFilter(final MinificationFilter minificationFilter) {
        _minificationFilter = minificationFilter;
    }

    public MinificationFilter getMinificationFilter() {
        return _minificationFilter;
    }

    public ModelDataStore load(final String resource) {
        if (resource == null) {
            throw new Error("Unable to locate '" + resource + "'");
        }
        _originalResource = null;

        final AIFileIO fileIo = AIFileIO.create();
        final AIFileOpenProcI fileOpenProc = new AIFileOpenProc() {
            public long invoke(final long pFileIO, final long fileName, final long openMode) {
                final AIFile aiFile = AIFile.create();
                final ByteBuffer data;
                final String fileNameUtf8 = memUTF8(fileName);
                try {
                    final ResourceSource source;
                    if (_modelLocator == null) {
                        source = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, fileNameUtf8);
                    } else {
                        source = _modelLocator.locateResource(fileNameUtf8);
                    }
                    applyRootResource(source);
                    System.err.println("loading resource: " + fileNameUtf8);
                    data = loadResourceAsByteBuffer(source, 8192);
                } catch (final Exception e) {
                    throw new RuntimeException("Could not open file: " + fileNameUtf8);
                }
                final AIFileReadProcI fileReadProc = new AIFileReadProc() {
                    public long invoke(final long pFile, final long pBuffer, final long size, final long count) {
                        final long max = Math.min(data.remaining(), size * count);
                        memCopy(memAddress(data) + data.position(), pBuffer, max);
                        return max;
                    }
                };
                final AIFileSeekI fileSeekProc = new AIFileSeek() {
                    public int invoke(final long pFile, final long offset, final int origin) {
                        if (origin == Assimp.aiOrigin_CUR) {
                            data.position(data.position() + (int) offset);
                        } else if (origin == Assimp.aiOrigin_SET) {
                            data.position((int) offset);
                        } else if (origin == Assimp.aiOrigin_END) {
                            data.position(data.limit() + (int) offset);
                        }
                        return 0;
                    }
                };
                final AIFileTellProcI fileTellProc = new AIFileTellProc() {
                    public long invoke(final long pFile) {
                        return data.limit();
                    }
                };
                aiFile.ReadProc(fileReadProc);
                aiFile.SeekProc(fileSeekProc);
                aiFile.FileSizeProc(fileTellProc);
                return aiFile.address();
            }
        };
        final AIFileCloseProcI fileCloseProc = new AIFileCloseProc() {
            public void invoke(final long pFileIO, final long pFile) {
                /* Nothing to do */
            }
        };
        fileIo.set(fileOpenProc, fileCloseProc, NULL);
        try {
            final AIScene scene = Assimp.aiImportFileEx(resource, Assimp.aiProcess_JoinIdenticalVertices
                    | Assimp.aiProcess_GenSmoothNormals | Assimp.aiProcess_Triangulate | Assimp.aiProcess_SortByPType,
                    fileIo);
            if (scene == null) {
                throw new IllegalStateException(Assimp.aiGetErrorString());
            }

            // Patch inspired by https://github.com/ros-planning/geometric_shapes/pull/52
            // Assimp enforces Y_UP convention by rotating models with different conventions.
            // However, that behaviour is confusing and doesn't match the ROS convention
            // where the Z axis is pointing up.
            // Hopefully this doesn't undo legit use of the root node transformation...
            // Note that this is also what RViz does internally.
            scene.mRootNode().mTransformation(AIMatrix4x4.create().set(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1));

            final ModelDataStore store = new ModelDataStore();
            readMaterialInfo(scene, store);

            final Node root = processNode(scene.mRootNode(), scene, store);
            store.setScene(root);

            // release our imported scenes
            Assimp.aiReleaseImport(scene);

            return store;

        } catch (final Exception e) {
            throw new Error("Unable to load model resource from URL: " + resource, e);
        } finally {
            _originalResource = null;
            if (_relativeLocator != null) {
                ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, _relativeLocator);
                _relativeLocator = null;
            }
        }
    }

    static ByteBuffer loadResourceAsByteBuffer(final ResourceSource rsrc, final int initialSize) {
        ByteBuffer buffer;
        try (final ReadableByteChannel channel = Channels.newChannel(rsrc.openStream())) {
            buffer = memAlloc(initialSize);

            while (true) {
                final int bytes = channel.read(buffer);
                if (bytes == -1) {
                    break;
                }
                if (buffer.remaining() == 0) {
                    final int newSize = buffer.capacity() * 2;
                    final ByteBuffer newBuffer = memAlloc(newSize);
                    buffer.flip();
                    newBuffer.put(buffer);
                    memFree(buffer);

                    buffer = newBuffer;
                }
            }
            buffer.flip();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        return buffer;
    }

    private void applyRootResource(final ResourceSource source) {
        if (_originalResource != null) {
            return;
        }
        _originalResource = source;

        // if we don't specify a texture locator, add a temporary texture locator
        if (_textureLocator == null) {
            _relativeLocator = new RelativeResourceLocator(source);
            ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, _relativeLocator);
        }
    }

    private Node processNode(final AINode node, final AIScene scene, final ModelDataStore store) {
        final Node rVal = new Node(node.mName().dataString());
        System.err.println("processing node: " + rVal);

        rVal.setTransform(new Transform().fromHomogeneousMatrix(toMatrix4(node.mTransformation(), new Matrix4())));

        // process all the node's meshes (if any)
        final IntBuffer meshIndices = node.mMeshes();
        for (int i = 0, maxI = node.mNumMeshes(); i < maxI; i++) {
            final int meshIndex = meshIndices.get(i);
            final AIMesh mesh = AIMesh.create(scene.mMeshes().get(meshIndex));
            rVal.attachChild(processMesh(mesh, scene, store));
        }

        // then do the same for each of its children
        for (int i = 0, maxI = node.mNumChildren(); i < maxI; i++) {
            rVal.attachChild(processNode(AINode.create(node.mChildren().get(i)), scene, store));
        }

        return rVal;
    }

    private Spatial processMesh(final AIMesh mesh, final AIScene scene, final ModelDataStore store) {
        final Mesh rVal = new Mesh(mesh.mName().dataString());
        System.err.println("processing mesh: " + rVal);

        final MeshData meshData = rVal.getMeshData();

        // pull vertices
        meshData.setVertexCoords(toFloatBufferData(mesh.mVertices()));

        // pull normals
        meshData.setNormalCoords(toFloatBufferData(mesh.mNormals()));

        // pull tangents
        meshData.setTangentCoords(toFloatBufferData(mesh.mTangents()));

        // pull colors - we'll only use the first set, if present
        meshData.setColorCoords(toFloatBufferData(mesh.mColors(0)));

        // pull texture coords
        for (int i = 0; i < Assimp.AI_MAX_NUMBER_OF_TEXTURECOORDS; i++) {
            final AIVector3D.Buffer coords = mesh.mTextureCoords(i);
            if (coords == null) {
                break;
            }

            final int maxUv = mesh.mNumUVComponents(i);
            meshData.setTextureCoords(toFloatBufferData(coords, maxUv), i);
        }

        // pull index mode
        int tupleCount = 0;
        if (mesh.mPrimitiveTypes() == Assimp.aiPrimitiveType_TRIANGLE) {
            meshData.setIndexMode(IndexMode.Triangles);
            tupleCount = 3;
        } else if (mesh.mPrimitiveTypes() == Assimp.aiPrimitiveType_LINE) {
            meshData.setIndexMode(IndexMode.Lines);
            tupleCount = 2;
        } else if (mesh.mPrimitiveTypes() == Assimp.aiPrimitiveType_POINT) {
            meshData.setIndexMode(IndexMode.Points);
            tupleCount = 1;
        }

        // pull indices
        meshData.setIndices(toIndexBufferData(mesh.mFaces(), tupleCount, mesh.mNumVertices()));

        // set our material
        applyMaterial(rVal, mesh.mMaterialIndex(), store);

        return rVal;
    }

    private void applyMaterial(final Mesh mesh, final int materialIndex, final ModelDataStore store) {
        if (store.materialSurfaces.containsKey(materialIndex)) {
            mesh.setProperty(ColorSurface.DefaultPropertyKey, store.materialSurfaces.get(materialIndex));
        }

        if (store.materialDiffuseTexs.containsKey(materialIndex)) {
            final TextureState ts = new TextureState();

            ts.setTexture(store.materialDiffuseTexs.get(materialIndex));

            mesh.setRenderState(ts);
        }
    }

    private IndexBufferData<?> toIndexBufferData(final AIFace.Buffer buffer, final int tupleCount,
            final int vertexCount) {
        final int count = buffer != null ? buffer.remaining() : 0;
        if (count == 0) {
            return null;
        }

        final IndexBufferData<?> rVal = com.ardor3d.util.geom.BufferUtils.createIndexBufferData(count * tupleCount,
                vertexCount);
        for (int i = 0; i < count; ++i) {
            final AIFace face = buffer.get();
            rVal.put(face.mIndices());
        }

        return rVal;
    }

    private FloatBufferData toFloatBufferData(final AIColor4D.Buffer buffer) {
        final int count = buffer != null ? buffer.remaining() : 0;
        if (count == 0) {
            return null;
        }

        final FloatBufferData rVal = new FloatBufferData(count * 4, 4);
        for (int i = 0; i < count; i++) {
            final AIColor4D color = buffer.get();
            rVal.getBuffer().put(color.r());
            rVal.getBuffer().put(color.g());
            rVal.getBuffer().put(color.b());
            rVal.getBuffer().put(color.a());
        }
        return rVal;
    }

    private FloatBufferData toFloatBufferData(final AIVector3D.Buffer buffer) {
        return toFloatBufferData(buffer, 3);
    }

    private FloatBufferData toFloatBufferData(final AIVector3D.Buffer buffer, final int maxChannel) {
        final int count = buffer != null ? buffer.remaining() : 0;
        if (count == 0) {
            return null;
        }

        final FloatBufferData rVal = new FloatBufferData(count * maxChannel, maxChannel);
        for (int i = 0; i < count; i++) {
            final AIVector3D vec = buffer.get();
            rVal.getBuffer().put(vec.x());
            if (maxChannel > 1) {
                rVal.getBuffer().put(vec.y());
            }
            if (maxChannel > 2) {
                rVal.getBuffer().put(vec.z());
            }
        }
        return rVal;
    }

    private Matrix4 toMatrix4(final AIMatrix4x4 m, final Matrix4 store) {
        return store.set(m.a1(), m.a2(), m.a3(), m.a4(), m.b1(), m.b2(), m.b3(), m.b4(), m.c1(), m.c2(), m.c3(), m.c4(),
                m.d1(), m.d2(), m.d3(), m.d4());
    }

    private ReadOnlyColorRGBA toColorRGBA(final AIColor4D color) {
        return new ColorRGBA(color.r(), color.g(), color.b(), color.a());
    }

    private void readMaterialInfo(final AIScene scene, final ModelDataStore store) {
        final int matCount = scene.mNumMaterials();
        try (MemoryStack stack = stackPush()) {
            for (int i = 0; i < matCount; i++) {
                final AIMaterial mat = AIMaterial.create(scene.mMaterials().get(i));

                final AIString string = AIString.create();
                Assimp.aiGetMaterialString(mat, Assimp.AI_MATKEY_NAME, Assimp.aiTextureType_NONE, 0, string);
                System.err.println("reading material: " + string.dataString());

                final ColorSurface surface = new ColorSurface();
                store.materialSurfaces.put(i, surface);

                final AIColor4D color = AIColor4D.create();

                if (readMaterialColor(mat, Assimp.AI_MATKEY_COLOR_AMBIENT, color)) {
                    surface.setAmbient(toColorRGBA(color));
                }

                if (readMaterialColor(mat, Assimp.AI_MATKEY_COLOR_DIFFUSE, color)) {
                    surface.setDiffuse(toColorRGBA(color));
                }

                if (readMaterialColor(mat, Assimp.AI_MATKEY_COLOR_EMISSIVE, color)) {
                    surface.setEmissive(toColorRGBA(color));
                }

                if (readMaterialColor(mat, Assimp.AI_MATKEY_COLOR_SPECULAR, color)) {
                    surface.setSpecular(toColorRGBA(color));
                }

                final FloatBuffer fValue = stack.mallocFloat(1);
                final IntBuffer iValue = stack.mallocInt(1);
                final IntBuffer one = stack.ints(1);

                if (readMaterialFloat(mat, Assimp.AI_MATKEY_SHININESS, fValue, one)) {
                    surface.setShininess(Math.max(0.001f, fValue.get(0)));
                }

                // check if we need blending
                if (readMaterialFloat(mat, Assimp.AI_MATKEY_OPACITY, fValue, one)) {

                    final float opacity = fValue.get(0);

                    if (opacity > 0f && opacity <= .999f) {
                        surface.setOpacity(opacity);

                        final BlendState bs = new BlendState();
                        store.addRenderState(i, bs);
                        bs.setBlendEnabled(true);

                        if (readMaterialInt(mat, Assimp.AI_MATKEY_BLEND_FUNC, iValue, one)) {
                            if (iValue.get(0) == Assimp.aiBlendMode_Additive) {
                                bs.setSourceFunction(BlendState.SourceFunction.One);
                                bs.setDestinationFunction(BlendState.DestinationFunction.One);
                            } else {
                                bs.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
                                bs.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
                            }
                        }
                    }
                }

                // check if cull state is needed
                if (readMaterialInt(mat, Assimp.AI_MATKEY_TWOSIDED, iValue, one)) {
                    final CullState cs = new CullState();
                    store.addRenderState(i, cs);
                    cs.setCullFace(iValue.get(0) == 0 ? Face.Back : Face.None);
                }

                if (isLoadTextures()) {
                    final String diffuse = getTexturePath(mat, Assimp.aiTextureType_DIFFUSE);
                    if (diffuse != null) {
                        final Texture tex = loadTexture(diffuse);
                        store.materialDiffuseTexs.put(i, tex);
                    }
                }
            }
        }
    }

    private Texture loadTexture(final String textureName) {
        if (_textureLocator == null) {
            return TextureManager.load(textureName, getMinificationFilter(),
                    isUseCompression() ? TextureStoreFormat.GuessCompressedFormat
                            : TextureStoreFormat.GuessNoCompressedFormat,
                    isFlipTextureVertically());
        }

        final ResourceSource source = _textureLocator.locateResource(textureName);
        return TextureManager.load(source, getMinificationFilter(),
                isUseCompression() ? TextureStoreFormat.GuessCompressedFormat
                        : TextureStoreFormat.GuessNoCompressedFormat,
                isFlipTextureVertically());
    }

    private String getTexturePath(final AIMaterial aiMaterial, final int texType) {
        final AIString path = AIString.calloc();
        final int result = Assimp.aiGetMaterialTexture(aiMaterial, texType, 0, path, (IntBuffer) null, null, null, null,
                null, null);
        String p = (result == Assimp.aiReturn_SUCCESS) ? path.dataString() : null;
        if (p != null && p.startsWith("$texture_dummy")) {
            p = _originalResource.getName();
        }

        System.err.println("looking for texture: " + p);

        return p;
    }

    private boolean readMaterialColor(final AIMaterial mat, final String key, final AIColor4D store) {
        return Assimp.aiGetMaterialColor(mat, key, Assimp.aiTextureType_NONE, 0, store) == Assimp.aiReturn_SUCCESS;
    }

    private boolean readMaterialFloat(final AIMaterial mat, final String key, final FloatBuffer store,
            final IntBuffer inOut) {
        store.clear();
        return Assimp.aiGetMaterialFloatArray(mat, key, Assimp.aiTextureType_NONE, 0, store,
                inOut) == Assimp.aiReturn_SUCCESS && inOut.get(0) == 1;
    }

    private boolean readMaterialInt(final AIMaterial mat, final String key, final IntBuffer store,
            final IntBuffer inOut) {
        store.clear();
        return Assimp.aiGetMaterialIntegerArray(mat, key, Assimp.aiTextureType_NONE, 0, store,
                inOut) == Assimp.aiReturn_SUCCESS && inOut.get(0) == 1;
    }
}
