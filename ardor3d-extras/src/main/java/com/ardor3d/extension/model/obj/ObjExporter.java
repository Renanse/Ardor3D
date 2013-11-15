/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.obj;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.extension.model.util.KeyframeController;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.util.TextureKey;

/**
 * WaveFront OBJ exporter. It supports only the meshes. Several meshes can be exported into the same OBJ file. Only a
 * few kinds of primitives are supported. N.B: If the texture is flipped in Ardor3D, you will have to flip it manually
 * when loading the resulting OBJ file.
 * 
 * @author Julien Gouesse
 */
public class ObjExporter {

    private static final Logger logger = Logger.getLogger(ObjExporter.class.getName());

    public ObjExporter() {
        super();
    }

    /**
     * Save a mesh to a single WaveFront OBJ file and a MTL file
     * 
     * @param mesh
     *            mesh to export
     * @param objFile
     *            WaveFront OBJ file
     * @param mtlFile
     *            material file, optional
     * @throws IOException
     */
    public void save(final Mesh mesh, final File objFile, final File mtlFile) throws IOException {
        if (mesh.getControllerCount() == 0 || !(mesh.getController(0) instanceof KeyframeController)) {
            save(mesh, objFile, mtlFile, false, 0, true, null, null);
        } else {
            final KeyframeController<?> controller = (KeyframeController<?>) mesh.getController(0);
            final ArrayList<Mesh> meshList = new ArrayList<Mesh>();
            for (final KeyframeController.PointInTime pit : controller._keyframes) {
                if (pit != null && pit._newShape != null) {
                    meshList.add(pit._newShape);
                }
            }
            save(meshList, objFile, mtlFile, getLocalMeshTextureName(mesh));
        }
    }

    /**
     * Save several meshes to a single WaveFront OBJ file and a MTL file
     * 
     * @param meshList
     *            meshes to export
     * @param objFile
     *            WaveFront OBJ file
     * @param mtlFile
     *            material file, optional
     * @param customTextureName
     *            texture name that overrides the one of the mesh (except in key frames), optional
     * @throws IOException
     */
    public void save(final List<Mesh> meshList, final File objFile, final File mtlFile, final String customTextureName)
            throws IOException {
        if (!meshList.isEmpty()) {
            int firstVertexIndex = 0;
            boolean firstFiles = true;
            final List<ObjMaterial> materialList = new ArrayList<ObjMaterial>();
            for (final Mesh mesh : meshList) {
                if (mesh != null) {
                    if (mesh.getControllerCount() == 0 || !(mesh.getController(0) instanceof KeyframeController)) {
                        save(mesh, objFile, mtlFile, !firstFiles, firstVertexIndex, firstFiles, materialList,
                                customTextureName);
                        firstFiles = false;
                        firstVertexIndex += mesh.getMeshData().getVertexCount();
                    } else {
                        final KeyframeController<?> controller = (KeyframeController<?>) mesh.getController(0);
                        final ArrayList<Mesh> subMeshList = new ArrayList<Mesh>();
                        for (final KeyframeController.PointInTime pit : controller._keyframes) {
                            if (pit != null && pit._newShape != null) {
                                subMeshList.add(pit._newShape);
                            }
                        }
                        final String textureName = getLocalMeshTextureName(mesh);
                        for (final Mesh submesh : subMeshList) {
                            save(submesh, objFile, mtlFile, !firstFiles, firstVertexIndex, firstFiles, materialList,
                                    textureName);
                            firstFiles = false;
                            firstVertexIndex += submesh.getMeshData().getVertexCount();
                        }
                    }
                }
            }
        }
    }

    /**
     * Save a mesh to the given files.
     * 
     * @param mesh
     *            mesh to export
     * @param objFile
     *            WaveFront OBJ file
     * @param mtlFile
     *            material file, optional
     * @param append
     *            indicates whether the data are written to the end of the OBJ file
     * @param firstVertexIndex
     *            first vertex index used for this mesh during the export
     * @param firstFiles
     *            indicates whether the couple of files is used for the first time, i.e there is nothing to append
     *            despite the value of <code>append</code>
     * @param materialList
     *            list of materials already exported in this material file
     * @param customTextureName
     *            texture name that overrides the one of the mesh, optional
     * @throws IOException
     */
    protected void save(final Mesh mesh, final File objFile, final File mtlFile, final boolean append,
            final int firstVertexIndex, final boolean firstFiles, final List<ObjMaterial> materialList,
            final String customTextureName) throws IOException {
        File parentDirectory = objFile.getParentFile();
        if (parentDirectory != null && !parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }
        if (mtlFile != null) {
            parentDirectory = mtlFile.getParentFile();
            if (parentDirectory != null && !parentDirectory.exists()) {
                parentDirectory.mkdirs();
            }
        }
        PrintWriter objPw = null, mtlPw = null;
        try {
            // fills the MTL file
            final String mtlName;
            if (mtlFile != null) {
                final FileOutputStream mtlOs = new FileOutputStream(mtlFile, append);
                mtlPw = new PrintWriter(new BufferedOutputStream(mtlOs));
                // writes some comments
                if (firstFiles) {
                    mtlPw.println("# Ardor3D 1.0 MTL file");
                }
                if (mesh.getLocalRenderState(StateType.Material) != null) {
                    // TODO
                }
                if (mesh.getLocalRenderState(StateType.Blend) != null) {
                    // TODO
                }
                final String currentTextureName;
                if (customTextureName == null) {
                    currentTextureName = getLocalMeshTextureName(mesh);
                } else {
                    currentTextureName = customTextureName;
                }

                final int currentIllumination;
                if (mesh.getSceneHints().getLightCombineMode() == LightCombineMode.Off) {
                    // Color on and Ambient off
                    currentIllumination = 0;
                } else {
                    // Color on and Ambient on
                    currentIllumination = 1;
                }
                ObjMaterial sameObjMtl = null;
                if (materialList != null && !materialList.isEmpty()) {
                    for (final ObjMaterial mtl : materialList) {
                        // TODO support more parameters
                        if (mtl.illumType == currentIllumination
                                && (currentTextureName == null && mtl.textureName == null || currentTextureName != null
                                        && mtl.textureName != null && currentTextureName.equals(mtl.textureName))) {
                            sameObjMtl = mtl;
                            break;
                        }
                    }
                }
                if (sameObjMtl == null) {
                    // writes the new material library
                    mtlName = mtlFile.getName().trim().replaceAll(" ", "") + "_"
                            + (materialList == null ? 1 : materialList.size() + 1);
                    if (materialList != null) {
                        final ObjMaterial mtl = new ObjMaterial(mtlName);
                        mtl.illumType = currentIllumination;
                        mtl.textureName = currentTextureName;
                        materialList.add(mtl);
                    }
                    mtlPw.println("newmtl " + mtlName);
                    if (currentTextureName != null) {
                        mtlPw.println("map_Kd " + currentTextureName);
                    }
                    mtlPw.println("illum " + currentIllumination);
                } else {
                    mtlName = sameObjMtl.getName();
                }
            } else {
                mtlName = null;
            }

            final FileOutputStream objOs = new FileOutputStream(objFile, append);
            objPw = new PrintWriter(new BufferedOutputStream(objOs));
            // writes some comments
            if (firstFiles) {
                objPw.println("# Ardor3D 1.0 OBJ file");
                objPw.println("# www.ardor3d.com");
                // writes the material file name if any
                if (mtlFile != null) {
                    final String mtlLibFilename = mtlFile.getName();
                    objPw.println("mtllib " + mtlLibFilename);
                }
            }
            // writes the object name
            final String objName;
            String meshName = mesh.getName();
            // removes all spaces from the mesh name
            if (meshName != null && !meshName.isEmpty()) {
                meshName = meshName.trim().replaceAll(" ", "");
            }
            if (meshName != null && !meshName.isEmpty()) {
                objName = meshName;
            } else {
                objName = "obj_mesh" + mesh.hashCode();
            }
            objPw.println("o " + objName);
            final MeshData meshData = mesh.getMeshData();
            // writes the coordinates
            final FloatBufferData verticesData = meshData.getVertexCoords();
            if (verticesData == null) {
                throw new IllegalArgumentException("cannot export a mesh with no vertices");
            }
            final int expectedTupleCount = verticesData.getTupleCount();
            saveFloatBufferData(verticesData, objPw, "v", expectedTupleCount);
            final FloatBufferData texCoordsData = meshData.getTextureCoords(0);
            saveFloatBufferData(texCoordsData, objPw, "vt", expectedTupleCount);
            final FloatBufferData normalsData = meshData.getNormalCoords();
            saveFloatBufferData(normalsData, objPw, "vn", expectedTupleCount);
            // writes the used material library
            if (mtlFile != null) {
                objPw.println("usemtl " + mtlName);
            }
            // writes the faces
            for (int sectionIndex = 0; sectionIndex < meshData.getSectionCount(); sectionIndex++) {
                final IndexMode indexMode = meshData.getIndexMode(sectionIndex);
                final int[] indices = new int[indexMode.getVertexCount()];
                switch (indexMode) {
                    case TriangleFan:
                    case Triangles:
                    case TriangleStrip:
                    case Quads:
                        for (int primIndex = 0, primCount = meshData.getPrimitiveCount(sectionIndex); primIndex < primCount; primIndex++) {
                            meshData.getPrimitiveIndices(primIndex, sectionIndex, indices);
                            objPw.print("f");
                            for (int vertexIndex = 0; vertexIndex < indices.length; vertexIndex++) {
                                // indices start at 1 in the WaveFront OBJ format whereas indices start at 0 in
                                // Ardor3D
                                final int shiftedIndex = indices[vertexIndex] + 1 + firstVertexIndex;
                                // vertex index
                                objPw.print(" " + shiftedIndex);
                                // texture coordinate index
                                if (texCoordsData != null) {
                                    objPw.print("/" + shiftedIndex);
                                }
                                // normal coordinate index
                                if (normalsData != null) {
                                    objPw.print("/" + shiftedIndex);
                                }
                            }
                            objPw.println();
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("index mode " + indexMode + " not supported");
                }
            }
        } catch (final Throwable t) {
            throw new Error("Unable to save the mesh into an obj", t);
        } finally {
            if (objPw != null) {
                objPw.flush();
                objPw.close();
            }
            if (mtlPw != null) {
                mtlPw.flush();
                mtlPw.close();
            }
        }
    }

    private String getLocalMeshTextureName(final Mesh mesh) {
        final String textureName;
        if (mesh.getLocalRenderState(StateType.Texture) != null) {
            final TextureState textureState = (TextureState) mesh.getLocalRenderState(StateType.Texture);
            if (textureState.isEnabled() && textureState.getTexture() != null) {
                final TextureKey tKey = textureState.getTexture().getTextureKey();
                final String tmpTextureName = tKey.getSource().getName();
                final int lastIndexOfUnixPathSeparator = tmpTextureName.lastIndexOf('/');
                final int lastIndexOfWindowsPathSeparator = tmpTextureName.lastIndexOf('\\');
                if (lastIndexOfUnixPathSeparator != -1) {
                    textureName = tmpTextureName.substring(lastIndexOfUnixPathSeparator + 1);
                } else {
                    if (lastIndexOfWindowsPathSeparator != -1) {
                        textureName = tmpTextureName.substring(lastIndexOfWindowsPathSeparator + 1);
                    } else {
                        textureName = tmpTextureName;
                    }
                }
                if (tKey.isFlipped()) {
                    ObjExporter.logger.warning("The texture " + tmpTextureName
                            + " will have to be flipped manually when loading this OBJ file");
                } else {
                    ObjExporter.logger.warning("The texture " + tmpTextureName
                            + " might need to be flipped manually when loading this OBJ file");
                }
            } else {
                textureName = null;
            }
        } else {
            textureName = null;
        }
        return textureName;
    }

    private void saveFloatBufferData(final FloatBufferData data, final PrintWriter objPw, final String keyword,
            final int expectedTupleCount) {
        if (data != null) {
            if (keyword == null || keyword.isEmpty()) {
                throw new IllegalArgumentException("null or empty keyword not supported");
            } else {
                final int tupleSize = data.getValuesPerTuple();
                final int tupleCount = data.getTupleCount();
                if (tupleCount < expectedTupleCount) {
                    throw new IllegalArgumentException("[" + keyword
                            + "] not enough data to match with the vertex count: " + tupleCount + " < "
                            + expectedTupleCount);
                } else {
                    if (tupleCount > expectedTupleCount) {
                        ObjExporter.logger.warning("[" + keyword + "] too much data to match with the vertex count: "
                                + tupleCount + " > " + expectedTupleCount + ". Skips useless tuple(s)");
                    }
                }
                for (int tupleIndex = 0; tupleIndex < expectedTupleCount; tupleIndex++) {
                    objPw.print(keyword);
                    for (int valueIndex = 0; valueIndex < tupleSize; valueIndex++) {
                        objPw.print(" " + data.getBuffer().get(tupleIndex * tupleSize + valueIndex));
                    }
                    objPw.println();
                }
            }
        }
    }
}
