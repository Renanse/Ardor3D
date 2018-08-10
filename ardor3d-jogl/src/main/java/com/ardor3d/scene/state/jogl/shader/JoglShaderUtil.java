/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scene.state.jogl.shader;

import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import javax.media.opengl.GL4;
import javax.media.opengl.GLContext;

import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.ShaderState.ShaderType;
import com.ardor3d.renderer.state.record.ShaderStateRecord;
import com.ardor3d.util.Ardor3dException;
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

/** Utility class for updating shadervariables(uniforms and attributes) */
public abstract class JoglShaderUtil {
    private static final Logger logger = Logger.getLogger(JoglShaderUtil.class.getName());

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
        final GL gl = GLContext.getCurrentGL();

        if (variable.variableID == -1) {
            variable.variableID = gl.getGL2ES2().glGetUniformLocation(programID, variable.name); // TODO Check
            // variable.name

            if (variable.variableID == -1 && !variable.errorLogged) {
                logger.severe("Shader uniform [" + variable.name + "] could not be located in shader");
                variable.errorLogged = true;
            }
        }
    }

    private static void updateShaderUniform(final ShaderVariableInt shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2ES2().glUniform1i(shaderUniform.variableID, shaderUniform.value1);
    }

    private static void updateShaderUniform(final ShaderVariableInt2 shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2ES2().glUniform2i(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2);
    }

    private static void updateShaderUniform(final ShaderVariableInt3 shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2ES2().glUniform3i(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2,
                shaderUniform.value3);
    }

    private static void updateShaderUniform(final ShaderVariableInt4 shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2ES2().glUniform4i(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2,
                shaderUniform.value3, shaderUniform.value4);
    }

    private static void updateShaderUniform(final ShaderVariableIntArray shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        switch (shaderUniform.size) {
            case 1:
                gl.getGL2ES2().glUniform1iv(shaderUniform.variableID, shaderUniform.value.remaining(),
                        shaderUniform.value);
                break;
            case 2:
                gl.getGL2ES2().glUniform2iv(shaderUniform.variableID, shaderUniform.value.remaining(),
                        shaderUniform.value);
                break;
            case 3:
                gl.getGL2ES2().glUniform3iv(shaderUniform.variableID, shaderUniform.value.remaining(),
                        shaderUniform.value);
                break;
            case 4:
                gl.getGL2ES2().glUniform4iv(shaderUniform.variableID, shaderUniform.value.remaining(),
                        shaderUniform.value);
                break;
            default:
                throw new IllegalArgumentException("Wrong size: " + shaderUniform.size);
        }
    }

    private static void updateShaderUniform(final ShaderVariableFloat shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2ES2().glUniform1f(shaderUniform.variableID, shaderUniform.value1);
    }

    private static void updateShaderUniform(final ShaderVariableFloat2 shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2ES2().glUniform2f(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2);
    }

    private static void updateShaderUniform(final ShaderVariableFloat3 shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2ES2().glUniform3f(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2,
                shaderUniform.value3);
    }

    private static void updateShaderUniform(final ShaderVariableFloat4 shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        gl.getGL2ES2().glUniform4f(shaderUniform.variableID, shaderUniform.value1, shaderUniform.value2,
                shaderUniform.value3, shaderUniform.value4);
    }

    private static void updateShaderUniform(final ShaderVariableFloatArray shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        switch (shaderUniform.size) {
            case 1:
                gl.getGL2ES2().glUniform1fv(shaderUniform.variableID, shaderUniform.value.remaining(),
                        shaderUniform.value);
                break;
            case 2:
                gl.getGL2ES2().glUniform2fv(shaderUniform.variableID, shaderUniform.value.remaining(),
                        shaderUniform.value);
                break;
            case 3:
                gl.getGL2ES2().glUniform3fv(shaderUniform.variableID, shaderUniform.value.remaining(),
                        shaderUniform.value);
                break;
            case 4:
                gl.getGL2ES2().glUniform4fv(shaderUniform.variableID, shaderUniform.value.remaining(),
                        shaderUniform.value);
                break;
            default:
                throw new IllegalArgumentException("Wrong size: " + shaderUniform.size);
        }
    }

    private static void updateShaderUniform(final ShaderVariableMatrix2 shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        shaderUniform.matrixBuffer.rewind();
        gl.getGL2ES2().glUniformMatrix2fv(shaderUniform.variableID, 1, shaderUniform.rowMajor,
                shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix3 shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        shaderUniform.matrixBuffer.rewind();
        gl.getGL2ES2().glUniformMatrix3fv(shaderUniform.variableID, 1, shaderUniform.rowMajor,
                shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix4 shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        shaderUniform.matrixBuffer.rewind();
        gl.getGL2ES2().glUniformMatrix4fv(shaderUniform.variableID, 1, shaderUniform.rowMajor,
                shaderUniform.matrixBuffer);
    }

    private static void updateShaderUniform(final ShaderVariableMatrix4Array shaderUniform) {
        final GL gl = GLContext.getCurrentGL();

        shaderUniform.matrixBuffer.rewind();
        // count == number of matrices we are sending, or iotw, limit / 16
        gl.getGL2ES2().glUniformMatrix4fv(shaderUniform.variableID, shaderUniform.matrixBuffer.limit() >> 4,
                shaderUniform.rowMajor, shaderUniform.matrixBuffer);
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
        final GL gl = GLContext.getCurrentGL();

        if (variable.variableID == -1) {
            variable.variableID = gl.getGL2ES2().glGetAttribLocation(programID, variable.name); // TODO Check
            // variable.name

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
     */
    public static void updateShaderAttribute(final Renderer renderer, final ShaderVariable shaderVariable) {
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
            final GL gl = GLContext.getCurrentGL();
            gl.getGL3().glUseProgram(id);
            record.programId = id;
        }
    }

    public static int getGLShaderType(final ShaderType type) {
        switch (type) {
            case Fragment:
                return GL2ES2.GL_FRAGMENT_SHADER;
            case Geometry:
                return GL3.GL_GEOMETRY_SHADER;
            case TessellationControl:
                return GL4.GL_TESS_CONTROL_SHADER;
            case TessellationEvaluation:
                return GL4.GL_TESS_EVALUATION_SHADER;
            case Vertex:
                return GL2ES2.GL_VERTEX_SHADER;
            default:
                throw new Ardor3dException("Unhandled shader type: " + type);
        }
    }
}
