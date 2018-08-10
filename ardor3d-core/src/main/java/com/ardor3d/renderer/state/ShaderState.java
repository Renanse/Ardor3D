/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.ReferenceQueue;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import com.ardor3d.renderer.state.record.ShaderStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.scenegraph.ByteBufferData;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.IntBufferData;
import com.ardor3d.scenegraph.ShortBufferData;
import com.ardor3d.util.ContextIdReference;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
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
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

/**
 * Implementation of the GL_ARB_shader_objects extension.
 */
public class ShaderState extends RenderState {
    public enum ShaderType {
        Fragment, Vertex, Geometry, TessellationControl, TessellationEvaluation
    }

    public class ShaderInfo {
        public int id;
        public String program;
        public String name;
    }

    private static final Logger logger = Logger.getLogger(ShaderState.class.getName());

    protected Map<ShaderType, ShaderInfo> _shaders = Maps.newEnumMap(ShaderType.class);

    /** OpenGL id for this program. */
    protected int _programID = -1;

    private static ReferenceQueue<ShaderState> _shaderRefQueue = new ReferenceQueue<ShaderState>();
    protected transient ContextIdReference<ShaderState> _shaderIdCache;

    public int getProgramId(final Object glContext) {
        if (_shaderIdCache != null) {
            final Integer id = _shaderIdCache.getValue(glContext);
            if (id != null) {
                return id.intValue();
            }
        }
        return 0;
    }

    public void setProgramId(final Object glContextRep, final int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be > 0");
        }

        if (_shaderIdCache == null) {
            _shaderIdCache = new ContextIdReference<ShaderState>(this, _shaderRefQueue);
        }
        _shaderIdCache.put(glContextRep, id);
    }

    /** Storage for shader uniform values */
    protected List<ShaderVariable> _shaderUniforms = new ArrayList<ShaderVariable>();
    /** Storage for shader attribute values */
    protected List<ShaderVariable> _shaderAttributes = new ArrayList<ShaderVariable>();

    public boolean hasShader(final ShaderType type) {
        return _shaders.containsKey(type);
    }

    public String getShader(final ShaderType type) {
        final ShaderInfo info = _shaders.get(type);
        if (info == null) {
            return null;
        }
        return info.program;
    }

    public String getShaderName(final ShaderType type) {
        final ShaderInfo info = _shaders.get(type);
        if (info == null) {
            return null;
        }
        return info.name;
    }

    public int getShaderId(final ShaderType type) {
        final ShaderInfo info = _shaders.get(type);
        if (info == null) {
            return -1;
        }
        return info.id;
    }

    public boolean setShaderId(final ShaderType type, final int id) {
        final ShaderInfo info = _shaders.get(type);
        if (info == null) {
            return false;
        }

        info.id = id;

        return true;
    }

    public void setShader(final ShaderType type, final String name, final String shaderContents) {
        final ShaderInfo info = new ShaderInfo();
        info.name = name;
        info.program = shaderContents;

        _shaders.put(type, info);
    }

    public void setShader(final ShaderType type, final String shaderContents) {
        setShader(type, type.name(), shaderContents);
    }

    public void setShader(final ShaderType type, final String name, final InputStream shaderContents)
            throws IOException {
        String text;
        try (final Reader reader = new InputStreamReader(shaderContents)) {
            text = CharStreams.toString(reader);
        }

        setShader(type, name, text);
    }

    public void setShader(final ShaderType type, final InputStream shaderContents) throws IOException {
        setShader(type, type.name(), shaderContents);
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        // capsule.write(_vertShader, "vertShader", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        // _vertShader = capsule.readByteBuffer("vertShader", null);
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new ShaderStateRecord();
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
        return StateType.Shader;
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

}
