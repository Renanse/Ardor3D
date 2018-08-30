/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.material;

import java.nio.Buffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;

public interface IShaderUtils {

    int createShaderProgram(Map<ShaderType, List<String>> shaders, RenderContext context);

    void useShaderProgram(int id, RenderContext context);

    int createVertexArrayObject(RenderContext context);

    void setBoundVAO(int id, RenderContext context);

    int findAttributeLocation(int programId, String attributeName);

    int setupBufferObject(final AbstractBufferData<? extends Buffer> buffer, boolean isEBO, RenderContext context);

    void bindVertexAttribute(int location, AbstractBufferData<? extends Buffer> buffer);

    int findUniformLocation(int programId, String uniformName);

    void sendUniformValue(int location, UniformRef uniform, Mesh mesh);

    /**
     * Attempts to delete a OpenGL buffer associated with this buffer that is relevant to the current RenderContext.
     *
     * @param ids
     */
    void deleteBuffer(AbstractBufferData<?> buffer);

    /**
     * Attempts to delete OpenGL buffers with the given ids. Ignores null ids or ids < 1.
     *
     * @param ids
     */
    void deleteBuffers(Collection<Integer> ids);

    /**
     * Attempts to delete a vertex array associated with this mesh data that is relevant to the current RenderContext.
     *
     * @param ids
     */
    void deleteVertexArray(MeshData data);

    /**
     * Attempts to delete vertex arrays with the given ids. Ignores null ids or ids < 1.
     *
     * @param ids
     */
    void deleteVertexArrays(Collection<Integer> ids);

}
