/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyMatrix4;
import com.ardor3d.math.type.ReadOnlyQuaternion;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.type.ReadOnlyVector4;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.state.record.ShaderObjectsStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.scenegraph.ByteBufferData;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IntBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.ShortBufferData;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.shader.ShaderVariable;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat2;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloat4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableFloatArray;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt2;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableInt4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableIntArray;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix4Array;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerByte;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerFloat;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerFloatMatrix;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerInt;
import com.ardor3d.util.shader.uniformtypes.ShaderVariablePointerShort;

/**
 * Implementation of the GL_ARB_shader_objects extension.
 */
public class GLSLShaderObjectsState extends RenderState {
    private static final Logger logger = Logger.getLogger(GLSLShaderObjectsState.class.getName());

    /** Storage for shader uniform values */
    protected List<ShaderVariable> _shaderUniforms = new ArrayList<ShaderVariable>();
    /** Storage for shader attribute values */
    protected List<ShaderVariable> _shaderAttributes = new ArrayList<ShaderVariable>();

    protected ByteBuffer _vertShader, _fragShader, _geomShader, _tessControlShader, _tessEvalShader;

    // XXX: The below fields are public for brevity mostly as a way to remember that this class needs revisiting.

    /**
     * Optional logic for setting shadervariables based on the current geom. Note: If this object does not implement
     * Savable, it will be ignored during write.
     */
    public GLSLShaderDataLogic _shaderDataLogic;

    /** The Mesh this shader currently operates on during rendering */
    public Mesh _mesh;

    public boolean _needSendShader = true;

    /** OpenGL id for this program. * */
    public int _programID = -1;

    /** OpenGL id for the attached vertex shader. */
    public int _vertexShaderID = -1;

    /** OpenGL id for the attached fragment shader. */
    public int _fragmentShaderID = -1;

    /** OpenGL id for the attached geometry shader. */
    public int _geometryShaderID = -1;

    /** OpenGL id for the attached tessellation control shader. */
    public int _tessellationControlShaderID = -1;

    /** OpenGL id for the attached tessellation evaluation shader. */
    public int _tessellationEvaluationShaderID = -1;

    /** if true, we'll send our vertex attributes to the shader via vbo */
    private boolean _useAttributeVBO;

    /** optional name for our vertex shader, used for debugging details. */
    public String _vertexShaderName;

    /** optional name for our fragment shader, used for debugging details. */
    public String _fragmentShaderName;

    /** optional name for our geometry shader, used for debugging details. */
    public String _geometryShaderName;

    /** optional name for our tessellation control shader, used for debugging details. */
    public String _tessellationControlShaderName;

    /** optional name for our tessellation evaluation shader, used for debugging details. */
    public String _tessellationEvaluationShaderName;

    /**
     * Gets the currently loaded vertex shader.
     * 
     * @return
     */
    public ByteBuffer getVertexShader() {
        return _vertShader;
    }

    /**
     * Gets the currently loaded fragment shader.
     * 
     * @return
     */
    public ByteBuffer getFragmentShader() {
        return _fragShader;
    }

    /**
     * Gets the currently loaded geometry shader.
     * 
     * @return
     */
    public ByteBuffer getGeometryShader() {
        return _geomShader;
    }

    /**
     * Gets the currently loaded tessellation control shader.
     * 
     * @return
     */
    public ByteBuffer getTessellationControlShader() {
        return _tessControlShader;
    }

    /**
     * Gets the currently loaded tessellation evaluation shader.
     * 
     * @return
     */
    public ByteBuffer getTessellationEvaluationShader() {
        return _tessEvalShader;
    }

    public void setVertexShader(final InputStream stream) throws IOException {
        setVertexShader(stream, "");
    }

    public void setVertexShader(final InputStream stream, final String name) throws IOException {
        setVertexShader(load(stream));
        _vertexShaderName = name;
    }

    public void setFragmentShader(final InputStream stream) throws IOException {
        setFragmentShader(stream, "");
    }

    public void setFragmentShader(final InputStream stream, final String name) throws IOException {
        setFragmentShader(load(stream));
        _fragmentShaderName = name;
    }

    public void setGeometryShader(final InputStream stream) throws IOException {
        setGeometryShader(stream, "");
    }

    public void setGeometryShader(final InputStream stream, final String name) throws IOException {
        setGeometryShader(load(stream));
        _geometryShaderName = name;
    }

    public void setTessellationControlShader(final InputStream stream) throws IOException {
        setTessellationControlShader(stream, "");
    }

    public void setTessellationControlShader(final InputStream stream, final String name) throws IOException {
        setTessellationControlShader(load(stream));
        _tessellationControlShaderName = name;
    }

    public void setTessellationEvaluationShader(final InputStream stream) throws IOException {
        setTessellationEvaluationShader(stream, "");
    }

    public void setTessellationEvaluationShader(final InputStream stream, final String name) throws IOException {
        setTessellationEvaluationShader(load(stream));
        _tessellationEvaluationShaderName = name;
    }

    protected ByteBuffer load(final InputStream in) throws IOException {
        DataInputStream dataStream = null;
        try {
            final BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
            dataStream = new DataInputStream(bufferedInputStream);
            final byte shaderCode[] = new byte[bufferedInputStream.available()];
            dataStream.readFully(shaderCode);
            bufferedInputStream.close();
            dataStream.close();
            final ByteBuffer shaderByteBuffer = BufferUtils.createByteBuffer(shaderCode.length);
            shaderByteBuffer.put(shaderCode);
            shaderByteBuffer.rewind();

            return shaderByteBuffer;
        } finally {
            // Ensure that the stream is closed, even if there is an exception.
            if (dataStream != null) {
                try {
                    dataStream.close();
                } catch (final IOException closeFailure) {
                    logger.log(Level.WARNING, "Failed to close the shader object", closeFailure);
                }
            }
        }
    }

    /**
     * Set the contents for our vertex shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setVertexShader(final ByteBuffer shader) {
        setVertexShader(shader, "");
    }

    /**
     * Set the contents for our vertex shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setVertexShader(final ByteBuffer shader, final String name) {
        _vertShader = shader;
        _vertexShaderName = name;
    }

    /**
     * Set the contents for our fragment shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setFragmentShader(final ByteBuffer shader) {
        setFragmentShader(shader, "");
    }

    /**
     * Set the contents for our fragment shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setFragmentShader(final ByteBuffer shader, final String name) {
        _fragShader = shader;
        _fragmentShaderName = name;
    }

    /**
     * Set the contents for our geometry shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setGeometryShader(final ByteBuffer shader) {
        setGeometryShader(shader, "");
    }

    /**
     * Set the contents for our geometry shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setGeometryShader(final ByteBuffer shader, final String name) {
        _geomShader = shader;
        _geometryShaderName = name;
    }

    /**
     * Set the contents for our tessellation control shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setTessellationControlShader(final ByteBuffer shader) {
        setTessellationControlShader(shader, "");
    }

    /**
     * Set the contents for our tessellation control shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setTessellationControlShader(final ByteBuffer shader, final String name) {
        _tessControlShader = shader;
        _tessellationControlShaderName = name;
    }

    /**
     * Set the contents for our tessellation evaluation shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setTessellationEvaluationShader(final ByteBuffer shader) {
        setTessellationEvaluationShader(shader, "");
    }

    /**
     * Set the contents for our tessellation evaluation shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setTessellationEvaluationShader(final ByteBuffer shader, final String name) {
        _tessEvalShader = shader;
        _tessellationEvaluationShaderName = name;
    }

    /**
     * Set the contents for our vertex shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setVertexShader(final String shader) {
        setVertexShader(shader, "");
    }

    /**
     * Set the contents for our vertex shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setVertexShader(final String shader, final String name) {
        _vertShader = stringToByteBuffer(shader);
        _vertexShaderName = name;
    }

    /**
     * Set the contents for our fragment shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setFragmentShader(final String shader) {
        setFragmentShader(shader, "");
    }

    /**
     * Set the contents for our fragment shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setFragmentShader(final String shader, final String name) {
        _fragShader = stringToByteBuffer(shader);
        _fragmentShaderName = name;
    }

    /**
     * Set the contents for our geometry shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setGeometryShader(final String shader) {
        setGeometryShader(shader, "");
    }

    /**
     * Set the contents for our geometry shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setGeometryShader(final String shader, final String name) {
        _geomShader = stringToByteBuffer(shader);
        _geometryShaderName = name;
    }

    /**
     * Set the contents for our tessellation control shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setTessellationControlShader(final String shader) {
        setTessellationControlShader(shader, "");
    }

    /**
     * Set the contents for our tessellation control shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setTessellationControlShader(final String shader, final String name) {
        _tessControlShader = stringToByteBuffer(shader);
        _tessellationControlShaderName = name;
    }

    /**
     * Set the contents for our tessellation evaluation shader
     * 
     * @param shader
     *            the shader contents.
     */
    public void setTessellationEvaluationShader(final String shader) {
        setTessellationEvaluationShader(shader, "");
    }

    /**
     * Set the contents for our tessellation evaluation shader
     * 
     * @param shader
     *            the shader contents.
     * @param name
     *            a label for this shader, displayer upon shader errors.
     */
    public void setTessellationEvaluationShader(final String shader, final String name) {
        _tessEvalShader = stringToByteBuffer(shader);
        _tessellationEvaluationShaderName = name;
    }

    private ByteBuffer stringToByteBuffer(final String str) {
        final byte[] bytes = str.getBytes();
        final ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length);
        buf.put(bytes);
        buf.rewind();
        return buf;
    }

    /**
     * Gets all shader uniforms variables.
     * 
     * @return
     */
    public List<ShaderVariable> getShaderUniforms() {
        return _shaderUniforms;
    }

    /**
     * Retrieves a shader uniform by name.
     * 
     * @param uniformName
     * @return
     */
    public ShaderVariable getUniformByName(final String uniformName) {
        for (final ShaderVariable shaderVar : _shaderUniforms) {
            if (shaderVar.name.equals(uniformName)) {
                return shaderVar;
            }
        }

        return null;
    }

    /**
     * Gets all shader attribute variables.
     * 
     * @return
     */
    public List<ShaderVariable> getShaderAttributes() {
        return _shaderAttributes;
    }

    /**
     * Retrieves a shader attribute by name.
     * 
     * @param uniformName
     * @return
     */
    public ShaderVariable getAttributeByName(final String attributeName) {
        for (final ShaderVariable shaderVar : _shaderAttributes) {
            if (shaderVar.name.equals(attributeName)) {
                return shaderVar;
            }
        }

        return null;
    }

    /**
     * 
     * @param meshData
     */
    public void setMesh(final Mesh mesh) {
        _mesh = mesh;
    }

    /**
     * Logic to handle setting mesh-specific data to a shader before rendering
     * 
     * @param shaderDataLogic
     */
    public void setShaderDataLogic(final GLSLShaderDataLogic shaderDataLogic) {
        _shaderDataLogic = shaderDataLogic;
    }

    public GLSLShaderDataLogic getShaderDataLogic() {
        return _shaderDataLogic;
    }

    public boolean isUseAttributeVBO() {
        return _useAttributeVBO;
    }

    /**
     * @param useAttributeVBO
     *            if true, and we support VBO, we'll use VBO for shader attributes.
     */
    public void setUseAttributeVBO(final boolean useAttributeVBO) {
        _useAttributeVBO = useAttributeVBO;
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final boolean value) {
        final ShaderVariableInt shaderUniform = getShaderUniform(name, ShaderVariableInt.class);
        shaderUniform.value1 = value ? 1 : 0;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final int value) {
        final ShaderVariableInt shaderUniform = getShaderUniform(name, ShaderVariableInt.class);
        shaderUniform.value1 = value;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final float value) {
        final ShaderVariableFloat shaderUniform = getShaderUniform(name, ShaderVariableFloat.class);
        shaderUniform.value1 = value;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     */
    public void setUniform(final String name, final boolean value1, final boolean value2) {
        final ShaderVariableInt2 shaderUniform = getShaderUniform(name, ShaderVariableInt2.class);
        shaderUniform.value1 = value1 ? 1 : 0;
        shaderUniform.value2 = value2 ? 1 : 0;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     */
    public void setUniform(final String name, final int value1, final int value2) {
        final ShaderVariableInt2 shaderUniform = getShaderUniform(name, ShaderVariableInt2.class);
        shaderUniform.value1 = value1;
        shaderUniform.value2 = value2;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     */
    public void setUniform(final String name, final float value1, final float value2) {
        final ShaderVariableFloat2 shaderUniform = getShaderUniform(name, ShaderVariableFloat2.class);
        shaderUniform.value1 = value1;
        shaderUniform.value2 = value2;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     */
    public void setUniform(final String name, final boolean value1, final boolean value2, final boolean value3) {
        final ShaderVariableInt3 shaderUniform = getShaderUniform(name, ShaderVariableInt3.class);
        shaderUniform.value1 = value1 ? 1 : 0;
        shaderUniform.value2 = value2 ? 1 : 0;
        shaderUniform.value3 = value3 ? 1 : 0;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     */
    public void setUniform(final String name, final int value1, final int value2, final int value3) {
        final ShaderVariableInt3 shaderUniform = getShaderUniform(name, ShaderVariableInt3.class);
        shaderUniform.value1 = value1;
        shaderUniform.value2 = value2;
        shaderUniform.value3 = value3;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     */
    public void setUniform(final String name, final float value1, final float value2, final float value3) {
        final ShaderVariableFloat3 shaderUniform = getShaderUniform(name, ShaderVariableFloat3.class);
        shaderUniform.value1 = value1;
        shaderUniform.value2 = value2;
        shaderUniform.value3 = value3;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     * @param value4
     *            the new value
     */
    public void setUniform(final String name, final boolean value1, final boolean value2, final boolean value3,
            final boolean value4) {
        final ShaderVariableInt4 shaderUniform = getShaderUniform(name, ShaderVariableInt4.class);
        shaderUniform.value1 = value1 ? 1 : 0;
        shaderUniform.value2 = value2 ? 1 : 0;
        shaderUniform.value3 = value3 ? 1 : 0;
        shaderUniform.value4 = value4 ? 1 : 0;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     * @param value4
     *            the new value
     */
    public void setUniform(final String name, final int value1, final int value2, final int value3, final int value4) {
        final ShaderVariableInt4 shaderUniform = getShaderUniform(name, ShaderVariableInt4.class);
        shaderUniform.value1 = value1;
        shaderUniform.value2 = value2;
        shaderUniform.value3 = value3;
        shaderUniform.value4 = value4;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value1
     *            the new value
     * @param value2
     *            the new value
     * @param value3
     *            the new value
     * @param value4
     *            the new value
     */
    public void setUniform(final String name, final float value1, final float value2, final float value3,
            final float value4) {
        final ShaderVariableFloat4 shaderUniform = getShaderUniform(name, ShaderVariableFloat4.class);
        shaderUniform.value1 = value1;
        shaderUniform.value2 = value2;
        shaderUniform.value3 = value3;
        shaderUniform.value4 = value4;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new float data
     * @param size
     *            the number of components per entry (must be 1, 2, 3, or 4)
     */
    public void setUniform(final String name, final FloatBuffer value, final int size) {
        assert (size >= 1 && size <= 4) : "Size must be 1, 2, 3 or 4";
        final ShaderVariableFloatArray shaderUniform = getShaderUniform(name, ShaderVariableFloatArray.class);
        shaderUniform.value = value;
        shaderUniform.size = size;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final float[] value) {
        final ShaderVariableFloatArray shaderUniform = getShaderUniform(name, ShaderVariableFloatArray.class);
        shaderUniform.value = BufferUtils.createFloatBuffer(value);

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final IntBuffer value) {
        final ShaderVariableIntArray shaderUniform = getShaderUniform(name, ShaderVariableIntArray.class);
        shaderUniform.value = value;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final int[] value) {
        final ShaderVariableIntArray shaderUniform = getShaderUniform(name, ShaderVariableIntArray.class);
        shaderUniform.value = BufferUtils.createIntBuffer(value);

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final ReadOnlyVector2 value) {
        final ShaderVariableFloat2 shaderUniform = getShaderUniform(name, ShaderVariableFloat2.class);
        shaderUniform.value1 = (float) value.getX();
        shaderUniform.value2 = (float) value.getY();

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final ReadOnlyVector3 value) {
        final ShaderVariableFloat3 shaderUniform = getShaderUniform(name, ShaderVariableFloat3.class);
        shaderUniform.value1 = (float) value.getX();
        shaderUniform.value2 = (float) value.getY();
        shaderUniform.value3 = (float) value.getZ();

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final ReadOnlyVector4 value) {
        final ShaderVariableFloat4 shaderUniform = getShaderUniform(name, ShaderVariableFloat4.class);
        shaderUniform.value1 = (float) value.getX();
        shaderUniform.value2 = (float) value.getY();
        shaderUniform.value3 = (float) value.getZ();
        shaderUniform.value4 = (float) value.getW();

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final ReadOnlyColorRGBA value) {
        final ShaderVariableFloat4 shaderUniform = getShaderUniform(name, ShaderVariableFloat4.class);
        shaderUniform.value1 = value.getRed();
        shaderUniform.value2 = value.getGreen();
        shaderUniform.value3 = value.getBlue();
        shaderUniform.value4 = value.getAlpha();

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     */
    public void setUniform(final String name, final ReadOnlyQuaternion value) {
        final ShaderVariableFloat4 shaderUniform = getShaderUniform(name, ShaderVariableFloat4.class);
        shaderUniform.value1 = (float) value.getX();
        shaderUniform.value2 = (float) value.getY();
        shaderUniform.value3 = (float) value.getZ();
        shaderUniform.value4 = (float) value.getW();

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     * @param rowMajor
     *            true if is this in row major order
     */
    public void setUniform(final String name, final ReadOnlyMatrix3 value, final boolean rowMajor) {
        final ShaderVariableMatrix3 shaderUniform = getShaderUniform(name, ShaderVariableMatrix3.class);
        // prepare buffer for writing
        shaderUniform.matrixBuffer.rewind();
        value.toFloatBuffer(shaderUniform.matrixBuffer);
        // prepare buffer for reading
        shaderUniform.matrixBuffer.rewind();
        shaderUniform.rowMajor = rowMajor;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     * @param rowMajor
     *            true if is this in row major order
     */
    public void setUniform(final String name, final ReadOnlyMatrix4 value, final boolean rowMajor) {
        final ShaderVariableMatrix4 shaderUniform = getShaderUniform(name, ShaderVariableMatrix4.class);
        // prepare buffer for writing
        shaderUniform.matrixBuffer.rewind();
        value.toFloatBuffer(shaderUniform.matrixBuffer);
        // prepare buffer for reading
        shaderUniform.matrixBuffer.rewind();
        shaderUniform.rowMajor = rowMajor;

        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform Matrix4 variable to change
     * @param value
     *            the new value, assumed row major
     */
    public void setUniformMatrix4(final String name, final FloatBuffer value) {
        final ShaderVariableMatrix4 shaderUniform = getShaderUniform(name, ShaderVariableMatrix4.class);
        // prepare buffer for writing
        shaderUniform.matrixBuffer.rewind();
        shaderUniform.matrixBuffer.put(value);
        // prepare buffer for reading
        shaderUniform.matrixBuffer.rewind();
        value.rewind();
        shaderUniform.rowMajor = true;
        setNeedsRefresh(true);
    }

    /**
     * Set an uniform value for this shader object.
     * 
     * @param name
     *            uniform variable to change
     * @param value
     *            the new value
     * @param rowMajor
     *            true if is this in row major order
     */
    public void setUniform(final String name, final ReadOnlyMatrix4[] values, final boolean rowMajor) {
        final ShaderVariableMatrix4Array shaderUniform = getShaderUniform(name, ShaderVariableMatrix4Array.class);
        // prepare buffer for writing
        FloatBuffer matrixBuffer = shaderUniform.matrixBuffer;
        if (matrixBuffer == null || matrixBuffer.capacity() < values.length * 16) {
            matrixBuffer = BufferUtils.createFloatBuffer(values.length * 16);
            shaderUniform.matrixBuffer = matrixBuffer;
        }

        matrixBuffer.clear();
        for (final ReadOnlyMatrix4 value : values) {
            value.toFloatBuffer(matrixBuffer);
        }
        matrixBuffer.flip();

        // prepare buffer for reading
        shaderUniform.rowMajor = rowMajor;

        setNeedsRefresh(true);
    }

    /** <code>clearUniforms</code> clears all uniform values from this state. */
    public void clearUniforms() {
        _shaderUniforms.clear();
    }

    /**
     * Set an attribute pointer value for this shader object.
     * 
     * @param name
     *            attribute variable to change
     * @param size
     *            Specifies the number of values for each element of the generic vertex attribute array. Must be 1, 2,
     *            3, or 4.
     * @param normalized
     *            Specifies whether fixed-point data values should be normalized or converted directly as fixed-point
     *            values when they are accessed.
     * @param stride
     *            Specifies the byte offset between consecutive attribute values. If stride is 0 (the initial value),
     *            the attribute values are understood to be tightly packed in the array.
     * @param data
     *            The actual data to use as attribute pointer
     */
    public void setAttributePointer(final String name, final int size, final boolean normalized, final int stride,
            final FloatBufferData data) {
        final ShaderVariablePointerFloat shaderUniform = getShaderAttribute(name, ShaderVariablePointerFloat.class);
        shaderUniform.size = size;
        shaderUniform.normalized = normalized;
        shaderUniform.stride = stride;
        shaderUniform.data = data;

        setNeedsRefresh(true);
    }

    /**
     * Set an attribute pointer value for this shader object.
     * 
     * @param name
     *            attribute variable to change
     * @param size
     *            the number of rows and cols in the matrix. Must be 2, 3, or 4.
     * @param normalized
     *            Specifies whether fixed-point data values should be normalized or converted directly as fixed-point
     *            values when they are accessed.
     * @param data
     *            The actual data to use as attribute pointer
     */
    public void setAttributePointerMatrix(final String name, final int size, final boolean normalized,
            final FloatBufferData data) {
        final ShaderVariablePointerFloatMatrix shaderUniform = getShaderAttribute(name,
                ShaderVariablePointerFloatMatrix.class);
        shaderUniform.size = size;
        shaderUniform.normalized = normalized;
        shaderUniform.data = data;

        setNeedsRefresh(true);
    }

    /**
     * Set an attribute pointer value for this shader object.
     * 
     * @param name
     *            attribute variable to change
     * @param size
     *            Specifies the number of values for each element of the generic vertex attribute array. Must be 1, 2,
     *            3, or 4.
     * @param normalized
     *            Specifies whether fixed-point data values should be normalized or converted directly as fixed-point
     *            values when they are accessed.
     * @param unsigned
     *            Specifies wheter the data is signed or unsigned
     * @param stride
     *            Specifies the byte offset between consecutive attribute values. If stride is 0 (the initial value),
     *            the attribute values are understood to be tightly packed in the array.
     * @param data
     *            The actual data to use as attribute pointer
     */
    public void setAttributePointer(final String name, final int size, final boolean normalized,
            final boolean unsigned, final int stride, final ByteBufferData data) {
        final ShaderVariablePointerByte shaderUniform = getShaderAttribute(name, ShaderVariablePointerByte.class);
        shaderUniform.size = size;
        shaderUniform.normalized = normalized;
        shaderUniform.unsigned = unsigned;
        shaderUniform.stride = stride;
        shaderUniform.data = data;

        setNeedsRefresh(true);
    }

    /**
     * Set an attribute pointer value for this shader object.
     * 
     * @param name
     *            attribute variable to change
     * @param size
     *            Specifies the number of values for each element of the generic vertex attribute array. Must be 1, 2,
     *            3, or 4.
     * @param normalized
     *            Specifies whether fixed-point data values should be normalized or converted directly as fixed-point
     *            values when they are accessed.
     * @param unsigned
     *            Specifies wheter the data is signed or unsigned
     * @param stride
     *            Specifies the byte offset between consecutive attribute values. If stride is 0 (the initial value),
     *            the attribute values are understood to be tightly packed in the array.
     * @param data
     *            The actual data to use as attribute pointer
     */
    public void setAttributePointer(final String name, final int size, final boolean normalized,
            final boolean unsigned, final int stride, final IntBufferData data) {
        final ShaderVariablePointerInt shaderUniform = getShaderAttribute(name, ShaderVariablePointerInt.class);
        shaderUniform.size = size;
        shaderUniform.normalized = normalized;
        shaderUniform.unsigned = unsigned;
        shaderUniform.stride = stride;
        shaderUniform.data = data;

        setNeedsRefresh(true);
    }

    /**
     * Set an attribute pointer value for this shader object.
     * 
     * @param name
     *            attribute variable to change
     * @param size
     *            Specifies the number of values for each element of the generic vertex attribute array. Must be 1, 2,
     *            3, or 4.
     * @param normalized
     *            Specifies whether fixed-point data values should be normalized or converted directly as fixed-point
     *            values when they are accessed.
     * @param unsigned
     *            Specifies wheter the data is signed or unsigned
     * @param stride
     *            Specifies the byte offset between consecutive attribute values. If stride is 0 (the initial value),
     *            the attribute values are understood to be tightly packed in the array.
     * @param data
     *            The actual data to use as attribute pointer
     */
    public void setAttributePointer(final String name, final int size, final boolean normalized,
            final boolean unsigned, final int stride, final ShortBufferData data) {
        final ShaderVariablePointerShort shaderUniform = getShaderAttribute(name, ShaderVariablePointerShort.class);
        shaderUniform.size = size;
        shaderUniform.normalized = normalized;
        shaderUniform.unsigned = unsigned;
        shaderUniform.stride = stride;
        shaderUniform.data = data;

        setNeedsRefresh(true);
    }

    /**
     * <code>clearAttributes</code> clears all attribute values from this state.
     */
    public void clearAttributes() {
        _shaderAttributes.clear();
    }

    @Override
    public StateType getType() {
        return StateType.GLSLShader;
    }

    /**
     * Creates or retrieves a uniform shadervariable.
     * 
     * @param name
     *            Name of the uniform shadervariable to retrieve or create
     * @param classz
     *            Class type of the shadervariable
     * @return
     */
    private <T extends ShaderVariable> T getShaderUniform(final String name, final Class<T> classz) {
        final T shaderVariable = getShaderVariable(name, classz, _shaderUniforms);
        return shaderVariable;
    }

    /**
     * Creates or retrieves a attribute shadervariable.
     * 
     * @param name
     *            Name of the attribute shadervariable to retrieve or create
     * @param classz
     *            Class type of the shadervariable
     * @return
     */
    private <T extends ShaderVariable> T getShaderAttribute(final String name, final Class<T> classz) {
        final T shaderVariable = getShaderVariable(name, classz, _shaderAttributes);
        checkAttributeSizeLimits();
        return shaderVariable;
    }

    /**
     * @param name
     *            Name of the shadervariable to retrieve or create
     * @param classz
     *            Class type of the shadervariable
     * @param shaderVariableList
     *            List retrieve shadervariable from
     * @return
     */
    @SuppressWarnings("unchecked")
    private <T extends ShaderVariable> T getShaderVariable(final String name, final Class<T> classz,
            final List<ShaderVariable> shaderVariableList) {
        for (int i = shaderVariableList.size(); --i >= 0;) {
            final ShaderVariable temp = shaderVariableList.get(i);
            if (name.equals(temp.name)) {
                temp.needsRefresh = true;
                return (T) temp;
            }
        }

        try {
            final T shaderUniform = classz.newInstance();
            shaderUniform.name = name;
            shaderVariableList.add(shaderUniform);

            return shaderUniform;
        } catch (final InstantiationException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(),
                    "getShaderVariable(name, classz, shaderVariableList)", "Exception", e);
        } catch (final IllegalAccessException e) {
            logger.logp(Level.SEVERE, this.getClass().toString(),
                    "getShaderVariable(name, classz, shaderVariableList)", "Exception", e);
        }

        return null;
    }

    /**
     * Check if we are keeping the size limits in terms of attribute locations on the card.
     */
    public void checkAttributeSizeLimits() {
        final RenderContext context = ContextManager.getCurrentContext();
        final ContextCapabilities caps = context.getCapabilities();
        if (_shaderAttributes.size() > caps.getMaxGLSLVertexAttributes()) {
            logger.severe("Too many shader attributes(standard+defined): " + _shaderAttributes.size() + " maximum: "
                    + caps.getMaxGLSLVertexAttributes());
        }
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.writeSavableList(_shaderUniforms, "shaderUniforms", new ArrayList<ShaderVariable>());
        capsule.writeSavableList(_shaderAttributes, "shaderAttributes", new ArrayList<ShaderVariable>());
        capsule.write(_vertShader, "vertShader", null);
        capsule.write(_fragShader, "fragShader", null);
        capsule.write(_geomShader, "geomShader", null);
        capsule.write(_geomShader, "geomShader", null);
        capsule.write(_tessControlShader, "tessControlShader", null);
        capsule.write(_tessEvalShader, "tessEvalShader", null);
        capsule.write(_useAttributeVBO, "useAttributeVBO", false);

        if (_shaderDataLogic instanceof Savable) {
            capsule.write((Savable) _shaderDataLogic, "shaderDataLogic", null);
        }
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _shaderUniforms = capsule.readSavableList("shaderUniforms", new ArrayList<ShaderVariable>());
        _shaderAttributes = capsule.readSavableList("shaderAttributes", new ArrayList<ShaderVariable>());
        _vertShader = capsule.readByteBuffer("vertShader", null);
        _fragShader = capsule.readByteBuffer("fragShader", null);
        _geomShader = capsule.readByteBuffer("geomShader", null);
        _tessControlShader = capsule.readByteBuffer("tessControlShader", null);
        _tessEvalShader = capsule.readByteBuffer("tessEvalShader", null);
        _useAttributeVBO = capsule.readBoolean("useAttributeVBO", false);

        final Savable shaderDataLogic = capsule.readSavable("shaderDataLogic", null);
        // only override set _shaderDataLogic if we have something in the capsule.
        if (shaderDataLogic != null) {
            if (shaderDataLogic instanceof GLSLShaderDataLogic) {
                _shaderDataLogic = (GLSLShaderDataLogic) shaderDataLogic;
            } else {
                logger.warning("Deserialized shaderDataLogic is not of type GLSLShaderDataLogic. "
                        + shaderDataLogic.getClass().getName());
            }
        }
    }

    @Override
    public StateRecord createStateRecord() {
        return new ShaderObjectsStateRecord();
    }
}
