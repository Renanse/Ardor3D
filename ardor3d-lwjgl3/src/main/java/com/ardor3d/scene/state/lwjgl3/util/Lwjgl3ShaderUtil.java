/**
 * Copyright (c) 2008-2018 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.lwjgl3.util;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL40C;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.renderer.state.record.ShaderStateRecord;
import com.ardor3d.util.Ardor3dException;
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
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix2;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix3;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix4;
import com.ardor3d.util.shader.uniformtypes.ShaderVariableMatrix4Array;

/** Utility class for updating shadervariables (uniforms and attributes) */
public abstract class Lwjgl3ShaderUtil {
    private static final Logger logger = Logger.getLogger(Lwjgl3ShaderUtil.class.getName());

    /**
     * Updates a uniform shadervariable.
     *
     * @param shaderVariable
     *            variable to update
     */
    public static void updateShaderUniform(final ShaderVariable shaderVariable) {
        if (!shaderVariable.hasData()) {
            throw new IllegalArgumentException("shaderVariable has no data: " + shaderVariable.name + " type: "
                    + shaderVariable.getClass().getName());
        }

        if (shaderVariable instanceof ShaderVariableInt) {
            updateShaderUniform((ShaderVariableInt) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableInt2) {
            updateShaderUniform((ShaderVariableInt2) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableInt3) {
            updateShaderUniform((ShaderVariableInt3) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableInt4) {
            updateShaderUniform((ShaderVariableInt4) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableIntArray) {
            updateShaderUniform((ShaderVariableIntArray) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat) {
            updateShaderUniform((ShaderVariableFloat) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat2) {
            updateShaderUniform((ShaderVariableFloat2) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat3) {
            updateShaderUniform((ShaderVariableFloat3) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloat4) {
            updateShaderUniform((ShaderVariableFloat4) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableFloatArray) {
            updateShaderUniform((ShaderVariableFloatArray) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableMatrix2) {
            updateShaderUniform((ShaderVariableMatrix2) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableMatrix3) {
            updateShaderUniform((ShaderVariableMatrix3) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableMatrix4) {
            updateShaderUniform((ShaderVariableMatrix4) shaderVariable);
        } else if (shaderVariable instanceof ShaderVariableMatrix4Array) {
            updateShaderUniform((ShaderVariableMatrix4Array) shaderVariable);
        } else {
            logger.warning("updateShaderUniform: Unknown shaderVariable type!");
        }
    }

    /**
     * Update variableID for uniform shadervariable if needed.
     *
     * @param variable
     *            shadervaribale to update ID on
     * @param programID
     *            shader program context ID
     */
    public static void updateUniformLocation(final ShaderVariable variable, final int programID) {
        if (variable.variableID == -1) {
            final ByteBuffer nameBuf = BufferUtils.createByteBuffer(variable.name.getBytes().length + 1);
            nameBuf.clear();
            nameBuf.put(variable.name.getBytes());
            nameBuf.rewind();

            variable.variableID = GL20C.glGetUniformLocation(programID, nameBuf);

            if (variable.variableID == -1 && !variable.errorLogged) {
                logger.severe("Shader uniform [" + variable.name + "] could not be located in shader");
                variable.errorLogged = true;
            }
        }
    }

    private static void updateShaderUniform(final ShaderVariableInt shaderUniform) {
        GL20C.glUniform1i(shaderUniform.variableID, shaderUniform.value1);
    }

    private static void updateShaderUniform(final ShaderVariableInt2 shaderUniform) {
        GL20C.glUniform2i(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2);
    }

    private static void updateShaderUniform(final ShaderVariableInt3 shaderUniform) {
        GL20C.glUniform3i(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2, shaderUniform.value3);
    }

    private static void updateShaderUniform(final ShaderVariableInt4 shaderUniform) {
        GL20C.glUniform4i(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2, shaderUniform.value3,
                shaderUniform.value4);
    }

    private static void updateShaderUniform(final ShaderVariableIntArray shaderUniform) {
        switch (shaderUniform.size) {
            case 1:
                GL20C.glUniform1iv(shaderUniform.variableID, shaderUniform.value);
                break;
            case 2:
                GL20C.glUniform2iv(shaderUniform.variableID, shaderUniform.value);
                break;
            case 3:
                GL20C.glUniform3iv(shaderUniform.variableID, shaderUniform.value);
                break;
            case 4:
                GL20C.glUniform4iv(shaderUniform.variableID, shaderUniform.value);
                break;
            default:
                throw new IllegalArgumentException("Wrong size: " + shaderUniform.size);
        }
    }

    private static void updateShaderUniform(final ShaderVariableFloat shaderUniform) {
        GL20C.glUniform1f(shaderUniform.variableID, shaderUniform.value1);
    }

    private static void updateShaderUniform(final ShaderVariableFloat2 shaderUniform) {
        GL20C.glUniform2f(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2);
    }

    private static void updateShaderUniform(final ShaderVariableFloat3 shaderUniform) {
        GL20C.glUniform3f(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2, shaderUniform.value3);
    }

    private static void updateShaderUniform(final ShaderVariableFloat4 shaderUniform) {
        GL20C.glUniform4f(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2, shaderUniform.value3,
                shaderUniform.value4);
    }

    private static void updateShaderUniform(final ShaderVariableFloatArray shaderUniform) {
        switch (shaderUniform.size) {
            case 1:
                GL20C.glUniform1fv(shaderUniform.variableID, shaderUniform.value);
                break;
            case 2:
                GL20C.glUniform2fv(shaderUniform.variableID, shaderUniform.value);
                break;
            case 3:
                GL20C.glUniform3fv(shaderUniform.variableID, shaderUniform.value);
                break;
            case 4:
                GL20C.glUniform4fv(shaderUniform.variableID, shaderUniform.value);
                break;
            default:
                throw new IllegalArgumentException("Wrong size: " + shaderUniform.size);
        }
    }

    private static void updateShaderUniform(final ShaderVariableMatrix2 shaderUniform) {
        shaderUniform.matrixBuffer.rewind();
        GL20C.glUniformMatrix2fv(shaderUniform.variableID, shaderUniform.rowMajor, shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix3 shaderUniform) {
        shaderUniform.matrixBuffer.rewind();
        GL20C.glUniformMatrix3fv(shaderUniform.variableID, shaderUniform.rowMajor, shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix4 shaderUniform) {
        shaderUniform.matrixBuffer.rewind();
        GL20C.glUniformMatrix4fv(shaderUniform.variableID, shaderUniform.rowMajor, shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix4Array shaderUniform) {
        shaderUniform.matrixBuffer.rewind();
        GL20C.glUniformMatrix4fv(shaderUniform.variableID, shaderUniform.rowMajor, shaderUniform.matrixBuffer);
    }

    /**
     * Update variableID for attribute shadervariable if needed.
     *
     * @param variable
     *            shadervaribale to update ID on
     * @param programID
     *            shader program context ID
     */
    public static void updateAttributeLocation(final ShaderVariable variable, final int programID) {
        if (variable.variableID == -1) {
            final ByteBuffer nameBuf = BufferUtils.createByteBuffer(variable.name.getBytes().length + 1);
            nameBuf.clear();
            nameBuf.put(variable.name.getBytes());
            nameBuf.rewind();

            variable.variableID = GL20C.glGetAttribLocation(programID, nameBuf);

            if (variable.variableID == -1 && !variable.errorLogged) {
                logger.severe("Shader attribute [" + variable.name + "] could not be located in shader");
                variable.errorLogged = true;
            }
        }
    }

    /**
     * Updates an vertex attribute pointer.
     *
     * @param renderer
     *            the current renderer
     * @param shaderVariable
     *            variable to update
     * @param useVBO
     *            if true, we'll use VBO for the attributes, if false we'll use arrays.
     */
    public static void updateShaderAttribute(final Renderer renderer, final ShaderVariable shaderVariable,
            final boolean useVBO) {
        if (shaderVariable.variableID == -1) {
            // attribute is not bound, or was not found in shader.
            return;
        }

        if (!shaderVariable.hasData()) {
            throw new IllegalArgumentException("shaderVariable has no data: " + shaderVariable.name + " type: "
                    + shaderVariable.getClass().getName());
        }

    }

    public static void useShaderProgram(final int id, final ShaderStateRecord record) {
        if (record.programId != id) {
            GL20C.glUseProgram(id);
            record.programId = id;
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
}
