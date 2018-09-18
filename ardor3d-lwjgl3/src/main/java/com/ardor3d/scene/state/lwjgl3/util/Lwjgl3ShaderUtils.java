/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl3.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL21C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL40C;
import org.lwjgl.system.MemoryStack;

import com.ardor3d.light.Light;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Matrix4;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.RenderMatrixType;
import com.ardor3d.renderer.lwjgl3.Lwjgl3Renderer;
import com.ardor3d.renderer.material.IShaderUtils;
import com.ardor3d.renderer.material.ShaderType;
import com.ardor3d.renderer.material.uniform.RenderStateProperty;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.record.RendererRecord;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.AbstractBufferData.VBOAccessMode;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.Ardor3dException;

public class Lwjgl3ShaderUtils implements IShaderUtils {
    private static final Logger logger = Logger.getLogger(Lwjgl3ShaderUtils.class.getName());
    private final Lwjgl3Renderer _renderer;

    public Lwjgl3ShaderUtils(final Lwjgl3Renderer renderer) {
        _renderer = renderer;
    }

    @Override
    public int createShaderProgram(final Map<ShaderType, List<String>> shaders, final RenderContext context) {
        // Validate we have appropriate shaders
        if (!shaders.containsKey(ShaderType.Vertex) || !shaders.containsKey(ShaderType.Fragment)) {
            throw new Ardor3dException("Invalid Shader Program - must have at least Vertex and Fragment shaders.");
        }

        // Ok, if we're getting called, it means we don't already have a program id, so make one.
        final int programId = GL20C.glCreateProgram();

        // Now, walk through our shaders
        final int vertShaderId = prepareShader(shaders.get(ShaderType.Vertex), ShaderType.Vertex);
        final int fragShaderId = prepareShader(shaders.get(ShaderType.Fragment), ShaderType.Fragment);
        final int geoShaderId = prepareShader(shaders.get(ShaderType.Geometry), ShaderType.Geometry);

        final ContextCapabilities caps = context.getCapabilities();
        final int tessCtrlShaderId, tessEvalShaderId;
        if (caps.isTessellationShadersSupported()) {
            tessCtrlShaderId = prepareShader(shaders.get(ShaderType.TessellationControl),
                    ShaderType.TessellationControl);
            tessEvalShaderId = prepareShader(shaders.get(ShaderType.TessellationEvaluation),
                    ShaderType.TessellationEvaluation);
        } else {
            tessCtrlShaderId = tessEvalShaderId = -1;
        }

        // Attach any prepared shaders - (vert and frag are required)
        GL20C.glAttachShader(programId, vertShaderId);
        GL20C.glAttachShader(programId, fragShaderId);

        if (geoShaderId != -1) {
            GL20C.glAttachShader(programId, geoShaderId);
        }
        if (tessCtrlShaderId != -1) {
            GL20C.glAttachShader(programId, tessCtrlShaderId);
        }
        if (tessEvalShaderId != -1) {
            GL20C.glAttachShader(programId, tessEvalShaderId);
        }

        // Link our shaders to the program
        GL20C.glLinkProgram(programId);

        // Check for link errors
        final int success = GL20C.glGetProgrami(programId, GL20C.GL_LINK_STATUS);
        if (success == GL11C.GL_FALSE) {
            final String info = GL20C.glGetProgramInfoLog(programId);
            throw new Ardor3dException("Error linking shaders: " + info);
        }

        // Delete our shaders - we're done with them now that they are linked
        GL20C.glDeleteShader(vertShaderId);
        GL20C.glDeleteShader(fragShaderId);

        if (geoShaderId != -1) {
            GL20C.glDeleteShader(geoShaderId);
        }
        if (tessCtrlShaderId != -1) {
            GL20C.glDeleteShader(tessCtrlShaderId);
        }
        if (tessEvalShaderId != -1) {
            GL20C.glDeleteShader(tessEvalShaderId);
        }

        return programId;
    }

    @Override
    public void useShaderProgram(final int id, final RenderContext context) {
        final RendererRecord record = context.getRendererRecord();
        if (record.getProgramId() != id) {
            GL20C.glUseProgram(id);
            record.setProgramId(id);
        }
    }

    public static int getGLShaderType(final ShaderType type) {
        switch (type) {
            case Fragment:
                return GL20C.GL_FRAGMENT_SHADER;
            case Geometry:
                return GL32C.GL_GEOMETRY_SHADER;
            case TessellationControl:
                return GL40C.GL_TESS_CONTROL_SHADER;
            case TessellationEvaluation:
                return GL40C.GL_TESS_EVALUATION_SHADER;
            case Vertex:
                return GL20C.GL_VERTEX_SHADER;
            default:
                throw new Ardor3dException("Unhandled shader type: " + type);
        }
    }

    private static int prepareShader(final List<String> info, final ShaderType type) {
        if (info == null) {
            return -1;
        }

        // generate a new shader object
        final int shaderId = GL20C.glCreateShader(Lwjgl3ShaderUtils.getGLShaderType(type));

        // provide our source code
        GL20C.glShaderSource(shaderId, info.toArray(new String[info.size()]));

        // compile
        GL20C.glCompileShader(shaderId);

        // check for errors
        final int success = GL20C.glGetShaderi(shaderId, GL20C.GL_COMPILE_STATUS);
        if (success == GL11C.GL_FALSE) {
            final String log = GL20C.glGetShaderInfoLog(shaderId);

            System.err.println(info);
            throw new Ardor3dException("Error compiling " + type.name() + " shader: " + log);
        }

        return shaderId;
    }

    @Override
    public int createVertexArrayObject(final RenderContext context) {
        return GL30C.glGenVertexArrays();
    }

    @Override
    public void setBoundVAO(final int id, final RenderContext context) {
        final RendererRecord rendRecord = context.getRendererRecord();
        if (!rendRecord.isVaoValid() || rendRecord.getCurrentVaoId() != id) {
            GL30C.glBindVertexArray(id);
            rendRecord.setCurrentVaoId(id);
            rendRecord.setVaoValid(true);
        }
    }

    @Override
    public int findAttributeLocation(final int programId, final String attributeName) {
        return GL20C.glGetAttribLocation(programId, attributeName);
    }

    @Override
    public int findUniformLocation(final int programId, final String uniformName) {
        return GL20C.glGetUniformLocation(programId, uniformName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void sendUniformValue(final int location, final UniformRef uniform, final Mesh mesh) {
        // TODO: check if we already have a given uniform value set.
        // Determine our value.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Buffer value;
            switch (uniform.getSource()) {
                case Value:
                    value = (Buffer) uniform.getValue();
                    break;
                case RendererMatrix:
                    final RenderMatrixType type = (RenderMatrixType) uniform.getValue();
                    value = _renderer.getMatrix(type);
                    break;
                case SpatialProperty:
                    value = getBuffer(uniform.getType(), mesh.getProperty(uniform.getValue().toString()), stack);
                    break;
                case RenderState:
                    value = getValue(mesh, (RenderStateProperty) uniform.getValue(), uniform.getExtra(), stack);
                    break;
                case Function:
                    value = ((Function<Mesh, Buffer>) uniform.getValue()).apply(mesh);
                    break;
                default:
                    logger.log(Level.SEVERE, "Unhandled uniform source type: " + uniform.getSource());
                    return;
            }

            if (value == null) {
                logger.log(Level.SEVERE, "Uniform value was null: " + uniform.getShaderVariableName());
                return;
            }

            value.rewind();

            // Determine how we want to send and send
            switch (uniform.getType()) {
                case Double1:
                    GL40C.glUniform1dv(location, (DoubleBuffer) value);
                    break;
                case Double2:
                    GL40C.glUniform2dv(location, (DoubleBuffer) value);
                    break;
                case Double3:
                    GL40C.glUniform3dv(location, (DoubleBuffer) value);
                    break;
                case Double4:
                    GL40C.glUniform4dv(location, (DoubleBuffer) value);
                    break;
                case Float1:
                    GL20C.glUniform1fv(location, (FloatBuffer) value);
                    break;
                case Float2:
                    GL20C.glUniform2fv(location, (FloatBuffer) value);
                    break;
                case Float3:
                    GL20C.glUniform3fv(location, (FloatBuffer) value);
                    break;
                case Float4:
                    GL20C.glUniform4fv(location, (FloatBuffer) value);
                    break;
                case Int1:
                    GL20C.glUniform1iv(location, (IntBuffer) value);
                    break;
                case Int2:
                    GL20C.glUniform2iv(location, (IntBuffer) value);
                    break;
                case Int3:
                    GL20C.glUniform3iv(location, (IntBuffer) value);
                    break;
                case Int4:
                    GL20C.glUniform4iv(location, (IntBuffer) value);
                    break;
                case UInt1:
                    GL30C.glUniform1uiv(location, (IntBuffer) value);
                    break;
                case UInt2:
                    GL30C.glUniform2uiv(location, (IntBuffer) value);
                    break;
                case UInt3:
                    GL30C.glUniform3uiv(location, (IntBuffer) value);
                    break;
                case UInt4:
                    GL30C.glUniform4uiv(location, (IntBuffer) value);
                    break;
                case Matrix2x2D:
                    GL40C.glUniformMatrix2dv(location, false, (DoubleBuffer) value);
                    break;
                case Matrix2x2:
                    GL20C.glUniformMatrix2fv(location, false, (FloatBuffer) value);
                    break;
                case Matrix2x3D:
                    GL40C.glUniformMatrix2x3dv(location, false, (DoubleBuffer) value);
                    break;
                case Matrix2x3:
                    GL21C.glUniformMatrix2x3fv(location, false, (FloatBuffer) value);
                    break;
                case Matrix2x4D:
                    GL40C.glUniformMatrix2x4dv(location, false, (DoubleBuffer) value);
                    break;
                case Matrix2x4:
                    GL21C.glUniformMatrix2x4fv(location, false, (FloatBuffer) value);
                    break;
                case Matrix3x2D:
                    GL40C.glUniformMatrix3x2dv(location, false, (DoubleBuffer) value);
                    break;
                case Matrix3x2:
                    GL21C.glUniformMatrix3x2fv(location, false, (FloatBuffer) value);
                    break;
                case Matrix3x3D:
                    GL40C.glUniformMatrix3dv(location, false, (DoubleBuffer) value);
                    break;
                case Matrix3x3:
                    GL20C.glUniformMatrix3fv(location, false, (FloatBuffer) value);
                    break;
                case Matrix3x4D:
                    GL40C.glUniformMatrix3x4dv(location, false, (DoubleBuffer) value);
                    break;
                case Matrix3x4:
                    GL21C.glUniformMatrix3x4fv(location, false, (FloatBuffer) value);
                    break;
                case Matrix4x2D:
                    GL40C.glUniformMatrix4x2dv(location, false, (DoubleBuffer) value);
                    break;
                case Matrix4x2:
                    GL21C.glUniformMatrix4x2fv(location, false, (FloatBuffer) value);
                    break;
                case Matrix4x3D:
                    GL40C.glUniformMatrix4x3dv(location, false, (DoubleBuffer) value);
                    break;
                case Matrix4x3:
                    GL21C.glUniformMatrix4x3fv(location, false, (FloatBuffer) value);
                    break;
                case Matrix4x4D:
                    GL40C.glUniformMatrix4dv(location, false, (DoubleBuffer) value);
                    break;
                case Matrix4x4:
                    GL20C.glUniformMatrix4fv(location, false, (FloatBuffer) value);
                    break;
                default:
                    logger.log(Level.SEVERE, "Unhandled uniform type: " + uniform.getType());

            }
        }
    }

    private Buffer getBuffer(final UniformType type, final Object value, final MemoryStack stack) {
        switch (type) {
            case Double1:
                if (value instanceof Number) {
                    return stack.mallocDouble(1).put(((Number) value).doubleValue()).flip();
                }
                return (DoubleBuffer) value;
            case Double2:
                if (value instanceof Vector2) {
                    final Vector2 vec = (Vector2) value;
                    return stack.mallocDouble(2).put(vec.getX()).put(vec.getY()).flip();
                }
                return (DoubleBuffer) value;
            case Double3:
                if (value instanceof Vector3) {
                    final Vector3 vec = (Vector3) value;
                    return stack.mallocDouble(3).put(vec.getX()).put(vec.getY()).put(vec.getZ()).flip();
                }
                return (DoubleBuffer) value;
            case Double4:
                if (value instanceof Vector4) {
                    final Vector4 vec = (Vector4) value;
                    return stack.mallocDouble(4).put(vec.getX()).put(vec.getY()).put(vec.getZ()).put(vec.getW()).flip();
                }
                return (DoubleBuffer) value;
            case Float1:
                if (value instanceof Number) {
                    return stack.mallocFloat(1).put(((Number) value).floatValue()).flip();
                }
                return (FloatBuffer) value;
            case Float2:
                if (value instanceof Vector2) {
                    final Vector2 vec = (Vector2) value;
                    return stack.mallocFloat(2).put(vec.getXf()).put(vec.getYf()).flip();
                }
                return (FloatBuffer) value;
            case Float3:
                if (value instanceof Vector3) {
                    final Vector3 vec = (Vector3) value;
                    return stack.mallocFloat(3).put(vec.getXf()).put(vec.getYf()).put(vec.getZf()).flip();
                }
                return (FloatBuffer) value;
            case Float4:
                if (value instanceof Vector4) {
                    final Vector4 vec = (Vector4) value;
                    return stack.mallocFloat(4).put(vec.getXf()).put(vec.getYf()).put(vec.getZf()).put(vec.getWf())
                            .flip();
                }
                if (value instanceof Quaternion) {
                    final Quaternion vec = (Quaternion) value;
                    return stack.mallocFloat(4).put(vec.getXf()).put(vec.getYf()).put(vec.getZf()).put(vec.getWf())
                            .flip();
                }
                if (value instanceof ReadOnlyColorRGBA) {
                    final ReadOnlyColorRGBA vec = (ReadOnlyColorRGBA) value;
                    return stack.mallocFloat(4).put(vec.getRed()).put(vec.getGreen()).put(vec.getBlue())
                            .put(vec.getAlpha()).flip();
                }
                return (FloatBuffer) value;
            case Int1:
            case UInt1:
                if (value instanceof Number) {
                    return stack.mallocInt(1).put(((Number) value).intValue()).flip();
                }
                return (IntBuffer) value;
            case Int2:
            case UInt2:
                return (IntBuffer) value;
            case Int3:
            case UInt3:
                return (IntBuffer) value;
            case Int4:
            case UInt4:
                return (IntBuffer) value;
            case Matrix2x2:
                return (FloatBuffer) value;
            case Matrix2x2D:
                return (DoubleBuffer) value;
            case Matrix2x3:
                return (FloatBuffer) value;
            case Matrix2x3D:
                return (DoubleBuffer) value;
            case Matrix2x4:
                return (FloatBuffer) value;
            case Matrix2x4D:
                return (DoubleBuffer) value;
            case Matrix3x2:
                return (FloatBuffer) value;
            case Matrix3x2D:
                return (DoubleBuffer) value;
            case Matrix3x3:
                if (value instanceof Matrix3) {
                    final Matrix3 mat = (Matrix3) value;
                    final FloatBuffer buff = stack.mallocFloat(9);
                    mat.toFloatBuffer(buff, false);
                    return buff.flip();
                }
                if (value instanceof Matrix3[]) {
                    final Matrix3[] mats = (Matrix3[]) value;
                    final FloatBuffer buff = stack.mallocFloat(9 * mats.length);
                    for (final Matrix3 mat : mats) {
                        mat.toFloatBuffer(buff, false);
                    }
                    return buff.flip();
                }
                return (FloatBuffer) value;
            case Matrix3x3D:
                if (value instanceof Matrix3) {
                    final Matrix3 mat = (Matrix3) value;
                    final DoubleBuffer buff = stack.mallocDouble(9);
                    mat.toDoubleBuffer(buff, false);
                    return buff.flip();
                }
                if (value instanceof Matrix3[]) {
                    final Matrix3[] mats = (Matrix3[]) value;
                    final DoubleBuffer buff = stack.mallocDouble(9 * mats.length);
                    for (final Matrix3 mat : mats) {
                        mat.toDoubleBuffer(buff, false);
                    }
                    return buff.flip();
                }
                return (DoubleBuffer) value;
            case Matrix3x4:
                return (FloatBuffer) value;
            case Matrix3x4D:
                return (DoubleBuffer) value;
            case Matrix4x2:
                return (FloatBuffer) value;
            case Matrix4x2D:
                return (DoubleBuffer) value;
            case Matrix4x3:
                return (FloatBuffer) value;
            case Matrix4x3D:
                return (DoubleBuffer) value;
            case Matrix4x4:
                if (value instanceof Matrix4) {
                    final Matrix4 mat = (Matrix4) value;
                    final FloatBuffer buff = stack.mallocFloat(16);
                    mat.toFloatBuffer(buff, false);
                    return buff.flip();
                }
                if (value instanceof Matrix4[]) {
                    final Matrix4[] mats = (Matrix4[]) value;
                    final FloatBuffer buff = stack.mallocFloat(16 * mats.length);
                    for (final Matrix4 mat : mats) {
                        mat.toFloatBuffer(buff, false);
                    }
                    return buff.flip();
                }
                return (FloatBuffer) value;
            case Matrix4x4D:
                if (value instanceof Matrix4) {
                    final Matrix4 mat = (Matrix4) value;
                    final DoubleBuffer buff = stack.mallocDouble(16);
                    mat.toDoubleBuffer(buff, false);
                    return buff.flip();
                }
                if (value instanceof Matrix4[]) {
                    final Matrix4[] mats = (Matrix4[]) value;
                    final DoubleBuffer buff = stack.mallocDouble(16 * mats.length);
                    for (final Matrix4 mat : mats) {
                        mat.toDoubleBuffer(buff, false);
                    }
                    return buff.flip();
                }
                return (DoubleBuffer) value;
            default:
                logger.log(Level.SEVERE, "Unhandled uniform type: " + type);
                return null;
        }
    }

    public static Buffer getValue(final Mesh mesh, final RenderStateProperty propertyType, final Object extra,
            final MemoryStack stack) {
        switch (propertyType) {
            case MeshDefaultColorRGB:
            case MeshDefaultColorRGBA: {
                final FloatBuffer buffer = stack
                        .mallocFloat(propertyType == RenderStateProperty.MeshDefaultColorRGB ? 3 : 4);
                final ReadOnlyColorRGBA color = mesh.getDefaultColor();
                buffer.put(color.getRed()).put(color.getGreen()).put(color.getBlue());
                if (propertyType == RenderStateProperty.MeshDefaultColorRGBA) {
                    buffer.put(color.getAlpha());
                }
                buffer.rewind();
                return buffer;
            }

            case CurrentCameraLocation: {
                final FloatBuffer buffer = stack.mallocFloat(3);
                final Camera cam = Camera.getCurrentCamera();
                final ReadOnlyVector3 loc = cam.getLocation();
                buffer.put(loc.getXf()).put(loc.getYf()).put(loc.getZf());
                buffer.rewind();
                return buffer;
            }

            case LightPosition: {
                final int index = (!(extra instanceof Integer)) ? 0 : ((Integer) extra).intValue();
                final RenderContext context = ContextManager.getCurrentContext();
                final LightState ls = (LightState) context.getCurrentState(StateType.Light);
                final Light light = ls.get(index);
                ReadOnlyVector3 position = Vector3.ZERO;
                if (light instanceof PointLight) {
                    position = ((PointLight) light).getLocation();
                }

                final FloatBuffer buffer = stack.mallocFloat(3);
                buffer.put(position.getXf()).put(position.getYf()).put(position.getZf());
                buffer.rewind();
                return buffer;
            }

            case LightColorRGB:
            case LightColorRGBA: {
                final int index = (!(extra instanceof Integer)) ? 0 : ((Integer) extra).intValue();
                final RenderContext context = ContextManager.getCurrentContext();
                final LightState ls = (LightState) context.getCurrentState(StateType.Light);
                final Light light = ls.get(index);
                final ReadOnlyColorRGBA color = light == null ? ColorRGBA.BLACK_NO_ALPHA : light.getDiffuse();

                final FloatBuffer buffer = stack.mallocFloat(propertyType == RenderStateProperty.LightColorRGB ? 3 : 4);
                buffer.put(color.getRed()).put(color.getGreen()).put(color.getBlue());
                if (propertyType == RenderStateProperty.LightColorRGBA) {
                    buffer.put(color.getAlpha());
                }
                buffer.rewind();
                return buffer;
            }
        }
        throw new Ardor3dException("Unhandled uniform source - RenderStateProperty type: " + propertyType);
    }

    @Override
    public int setupBufferObject(final AbstractBufferData<? extends Buffer> buffer, final boolean isEBO,
            final RenderContext context) {

        int id = buffer.getBufferId(context);
        if (id != 0 && buffer.isBufferClean(context)) {
            return id;
        }

        final boolean newBuffer = id == 0;

        final Buffer dataBuffer = buffer.getBuffer();
        if (dataBuffer != null) {
            dataBuffer.rewind();

            if (newBuffer) {
                id = GL15C.glGenBuffers();
                buffer.setBufferId(context, id);
            }

            final int target = isEBO ? GL15C.GL_ELEMENT_ARRAY_BUFFER : GL15C.GL_ARRAY_BUFFER;
            GL15C.glBindBuffer(target, id);
            if (newBuffer) {
                GL15C.glBufferData(target, dataBuffer.capacity() * buffer.getByteCount(),
                        getGLVBOAccessMode(buffer.getVboAccessMode()));
            }

            if (dataBuffer instanceof FloatBuffer) {
                GL15C.glBufferSubData(target, 0, (FloatBuffer) dataBuffer);
            } else if (dataBuffer instanceof ByteBuffer) {
                GL15C.glBufferSubData(target, 0, (ByteBuffer) dataBuffer);
            } else if (dataBuffer instanceof IntBuffer) {
                GL15C.glBufferSubData(target, 0, (IntBuffer) dataBuffer);
            } else if (dataBuffer instanceof ShortBuffer) {
                GL15C.glBufferSubData(target, 0, (ShortBuffer) dataBuffer);
            }

            buffer.markClean(context);

        } else {
            throw new Ardor3dException("Attempting to create a buffer object with no Buffer value.");
        }
        return id;
    }

    @Override
    public void bindVertexAttribute(final int location, final AbstractBufferData<? extends Buffer> buffer) {
        GL20C.glVertexAttribPointer(location, buffer.getValuesPerTuple(), getGLDataType(buffer.getBuffer()), false, 0,
                0);
        GL20C.glEnableVertexAttribArray(location);
    }

    protected static int getGLDataType(final Buffer buffer) {
        if (buffer instanceof FloatBuffer) {
            return GL11C.GL_FLOAT;
        } else if (buffer instanceof ByteBuffer) {
            return GL11C.GL_BYTE;
        } else if (buffer instanceof IntBuffer) {
            return GL11C.GL_INT;
        } else if (buffer instanceof ShortBuffer) {
            return GL11C.GL_SHORT;
        } else {
            throw new Ardor3dException("Unhandled buffer type: " + buffer.getClass().getName());
        }
    }

    protected static int getGLVBOAccessMode(final VBOAccessMode vboAccessMode) {
        int glMode = GL15C.GL_STATIC_DRAW;
        switch (vboAccessMode) {
            case StaticDraw:
                glMode = GL15C.GL_STATIC_DRAW;
                break;
            case StaticRead:
                glMode = GL15C.GL_STATIC_READ;
                break;
            case StaticCopy:
                glMode = GL15C.GL_STATIC_COPY;
                break;
            case DynamicDraw:
                glMode = GL15C.GL_DYNAMIC_DRAW;
                break;
            case DynamicRead:
                glMode = GL15C.GL_DYNAMIC_READ;
                break;
            case DynamicCopy:
                glMode = GL15C.GL_DYNAMIC_COPY;
                break;
            case StreamDraw:
                glMode = GL15C.GL_STREAM_DRAW;
                break;
            case StreamRead:
                glMode = GL15C.GL_STREAM_READ;
                break;
            case StreamCopy:
                glMode = GL15C.GL_STREAM_COPY;
                break;
        }
        return glMode;
    }

    @Override
    public void deleteBuffer(final AbstractBufferData<?> buffer) {
        if (buffer == null) {
            return;
        }

        final int id = buffer.removeBufferId(ContextManager.getCurrentContext());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        GL15C.glDeleteBuffers(id);
    }

    @Override
    public void deleteBuffers(final Collection<Integer> ids) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer idBuffer = stack.callocInt(ids.size());
            idBuffer.clear();
            for (final Integer i : ids) {
                if (i != null && i != 0) {
                    idBuffer.put(i);
                }
            }
            idBuffer.flip();
            if (idBuffer.remaining() > 0) {
                GL15C.glDeleteBuffers(idBuffer);
            }
        }
    }

    @Override
    public void deleteVertexArray(final MeshData data) {
        if (data == null) {
            return;
        }

        final int id = data.removeVAOID(ContextManager.getCurrentContext());
        if (id == 0) {
            // Not on card... return.
            return;
        }

        GL30C.glDeleteVertexArrays(id);
    }

    @Override
    public void deleteVertexArrays(final Collection<Integer> ids) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer idBuffer = stack.callocInt(ids.size());
            idBuffer.clear();
            for (final Integer i : ids) {
                if (i != null && i != 0) {
                    idBuffer.put(i);
                }
            }
            idBuffer.flip();
            if (idBuffer.remaining() > 0) {
                GL30C.glDeleteVertexArrays(idBuffer);
            }
        }
    }

}
