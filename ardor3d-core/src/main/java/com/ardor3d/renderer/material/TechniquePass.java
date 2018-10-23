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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.ReferenceQueue;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.uniform.Ardor3dStateProperty;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.AbstractBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;
import com.ardor3d.util.gc.ContextValueReference;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

public class TechniquePass implements Savable {
    /** Name of this pass - optional, useful for debug, etc. */
    protected String _name;

    /** Our shaders, mapped by their type. */
    protected Map<ShaderType, List<String>> _shaders = Maps.newEnumMap(ShaderType.class);

    /** Context specific reference to an id for the shader program used by this pass. */
    protected static ReferenceQueue<TechniquePass> _shaderRefQueue = new ReferenceQueue<TechniquePass>();
    protected transient ContextValueReference<TechniquePass, Integer> _shaderIdCache;

    /** Information about the Attributes we care about in this pass. */
    protected List<VertexAttributeRef> _attributes = new ArrayList<>();

    /** Information about the Uniforms we care about in this pass. */
    protected List<UniformRef> _uniforms = new ArrayList<>();

    public int getProgramId(final RenderContext context) {
        if (_shaderIdCache != null) {
            final Integer id = _shaderIdCache.getValue(context.getGlContextRef());
            if (id != null) {
                return id.intValue();
            }
        }
        return 0;
    }

    public void setProgramId(final RenderContext context, final int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be > 0");
        }

        if (_shaderIdCache == null) {
            _shaderIdCache = ContextValueReference.newReference(this, _shaderRefQueue);
        }
        _shaderIdCache.put(context.getGlContextRef(), id);
    }

    public void setName(final String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public void setShader(final ShaderType type, final String shaderContents) {
        final List<String> programs = new ArrayList<>();
        programs.add(shaderContents);
        setShader(type, programs);
    }

    public void setShader(final ShaderType type, final List<String> shaderContents) {
        _shaders.put(type, shaderContents);
    }

    public void setShader(final ShaderType type, final InputStream shaderContents) throws IOException {
        String text;
        try (final Reader reader = new InputStreamReader(shaderContents)) {
            text = CharStreams.toString(reader);
        }

        setShader(type, text);
    }

    public void addAttribute(final VertexAttributeRef attribute) {
        _attributes.add(attribute);
    }

    public void addUniform(final UniformRef uniform) {
        _uniforms.add(uniform);
    }

    public void addLightInfoUniforms(final int maxLights) {
        for (int i = 0; i < maxLights; i++) {
            addUniform(new UniformRef("light[" + i + "]", UniformType.UniformSupplier, UniformSource.Ardor3dState,
                    Ardor3dStateProperty.Light, i, null));
        }
    }

    public void setupForDraw(final Renderer renderer, final Mesh mesh, final MeshData data) {
        // Apply our states - modulated by any enforced by the pass
        applyRenderStates(renderer, mesh);

        // Start our pass - generates the shader program and links the shader objects.
        startPass(renderer);

        // Setup our vertex attributes using mesh data
        setupAttributes(renderer, mesh, data);

        // Setup our uniforms using spatial and current scene information
        setupUniforms(renderer, mesh);
    }

    protected void startPass(final Renderer renderer) {
        final RenderContext context = ContextManager.getCurrentContext();

        // Make sure we have a program id
        int id = getProgramId(context);
        if (id <= 0) {
            // No valid id. Check our shader objects have ids and package them into a new program
            id = renderer.getShaderUtils().createShaderProgram(_shaders, context);
            setProgramId(context, id);
        }

        renderer.getShaderUtils().useShaderProgram(id, context);
    }

    protected void setupAttributes(final Renderer renderer, final Mesh mesh, final MeshData data) {
        final RenderContext context = ContextManager.getCurrentContext();

        // Make sure our meshdata has a VAO bound
        final IShaderUtils shaderUtils = renderer.getShaderUtils();
        int vaoID = data.getVAOID(context);
        if (vaoID <= 0) {
            vaoID = shaderUtils.createVertexArrayObject(context);
            data.setVAOID(context, vaoID);
            shaderUtils.setBoundVAO(vaoID, context);
        } else {
            shaderUtils.setBoundVAO(vaoID, context);
            if (data.isBuffersClean(context)) {
                return;
            }
        }

        // send our mesh data to the card, binding them to the VAO
        final int programId = getProgramId(context);
        for (int i = 0; i < _attributes.size(); i++) {
            final VertexAttributeRef attribute = _attributes.get(i);

            // Check for the item in our mesh data
            if (!data.containsKey(attribute.getMeshDataKey())) {
                if (Constants.strictVertexAttributes) {
                    throw new Ardor3dException("Required mesh data key '" + attribute.getMeshDataKey()
                            + "' not found in mesh: " + mesh.getName());
                }
                continue;
            }

            // pull our related buffer
            final AbstractBufferData<? extends Buffer> buffer = data.getCoords(attribute.getMeshDataKey());
            if (buffer.isBufferClean(context)) {
                continue;
            }

            int location = attribute.getLocation();
            if (location < 0) {
                // Use the name to find our location
                location = shaderUtils.findAttributeLocation(programId, attribute.getShaderVariableName());

                // still less than 0? might have been removed during compilation
                if (location < 0) {
                    continue;
                }
                attribute._location = location;
            }

            // Make sure our buffer has a vbo and its data is on the card.
            shaderUtils.setupBufferObject(buffer, false, context);

            // Bind our attribute to the current shader program
            shaderUtils.bindVertexAttribute(location, buffer);
        }

        // send any indices as well
        if (data.getIndexBuffer() != null) {
            shaderUtils.setupBufferObject(data.getIndices(), true, context);
        }

        data.markBuffersClean(context);
    }

    protected void setupUniforms(final Renderer renderer, final Mesh mesh) {
        final RenderContext context = ContextManager.getCurrentContext();

        final IShaderUtils shaderUtils = renderer.getShaderUtils();
        final int programId = getProgramId(context);
        for (int i = 0; i < _uniforms.size(); i++) {
            setupUniform(mesh, shaderUtils, programId, _uniforms.get(i), "");
        }
    }

    private void setupUniform(final Mesh mesh, final IShaderUtils shaderUtils, final int programId,
            final UniformRef uniform, final String namePrepend) {
        // If we are a UniformSupplier, pull uniforms out and set them up instead.
        if (uniform.getType() == UniformType.UniformSupplier) {
            final List<UniformRef> srcUniforms = getUniformsFromSupplier(uniform, mesh);
            if (srcUniforms != null) {
                final String prepend = namePrepend + uniform.getShaderVariableName() + ".";
                for (int i = 0; i < srcUniforms.size(); i++) {
                    setupUniform(mesh, shaderUtils, programId, srcUniforms.get(i), prepend);
                }
            }
            return;
        }

        // Set up non-bundle uniform
        int location = uniform.getCachedLocation() >= 0 ? uniform.getCachedLocation() : uniform.getLocation();
        if (location < 0) {
            // Use the name to find our location
            location = shaderUtils.findUniformLocation(programId, namePrepend + uniform.getShaderVariableName());
            if (location < 0) {
                return;
            } else {
                uniform.setCachedLocation(location);
            }
        }

        shaderUtils.sendUniformValue(location, uniform, mesh);
    }

    private List<UniformRef> getUniformsFromSupplier(final UniformRef uniform, final Mesh mesh) {
        Object supplier = uniform._cachedSupplier;
        String clazzName = null;
        if (supplier == null) {
            switch (uniform.getSource()) {
                case SpatialProperty:
                    supplier = mesh.getProperty(uniform.getValue().toString(), uniform.getDefaultValue());
                    clazzName = uniform.getExtra() != null ? uniform.getExtra().toString() : null;
                    break;
                case Ardor3dState:
                    final Ardor3dStateProperty prop = Ardor3dStateProperty.valueOf(uniform.getValue().toString());
                    if (prop == Ardor3dStateProperty.Light) {
                        // grab the appropriate light
                        final int index = (!(uniform.getExtra() instanceof Integer)) ? 0
                                : ((Integer) uniform.getExtra()).intValue();
                        final RenderContext context = ContextManager.getCurrentContext();
                        final LightState ls = (LightState) context.getCurrentState(StateType.Light);
                        if (ls.count() > index) {
                            supplier = ls.get(index);
                        }
                        clazzName = "com.ardor3d.light.PointLight";
                    } else {
                        throw new Ardor3dException(
                                "Uniform type 'UniformSupplier' can not be used with Ardor3dState." + prop);
                    }
                    break;
                default:
                    throw new Ardor3dException("Unsupported Uniform Source type '" + uniform.getSource()
                            + "' for use with UniformSupplier.");
            }
        }

        if (supplier instanceof IUniformSupplier) {
            return ((IUniformSupplier) supplier).getUniforms();
        } else if (supplier == null && clazzName != null) {
            final IUniformSupplier prov = IUniformSupplier.getDefaultProvider(clazzName);
            if (prov != null) {
                uniform._cachedSupplier = prov;
                return prov.getUniforms();
            }
        }
        return null;
    }

    protected void applyRenderStates(final Renderer renderer, final Mesh mesh) {
        for (final StateType type : StateType.values) {
            renderer.applyState(type, mesh.getWorldRenderState(type));
        }
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends TechniquePass> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_name, "name", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        _name = capsule.readString("name", null);
    }

}
