/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.ReferenceQueue;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.ardor3d.buffer.AbstractBufferData;
import com.ardor3d.light.LightManager;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.uniform.Ardor3dStateProperty;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.SceneIndexer;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.gc.ContextValueReference;
import com.google.common.io.CharStreams;

public class TechniquePass {
  /** Name of this pass - optional, useful for debug, etc. */
  protected String _name;

  /** Our shaders, mapped by their type. */
  protected Map<ShaderType, List<String>> _shaders = new EnumMap<>(ShaderType.class);

  /** Context specific reference to an id for the shader program used by this pass. */
  protected static ReferenceQueue<TechniquePass> _shaderRefQueue = new ReferenceQueue<>();
  protected transient ContextValueReference<TechniquePass, Integer> _shaderIdCache;

  /** Information about the Attributes we care about in this pass. */
  protected List<VertexAttributeRef> _attributes = new ArrayList<>();

  /** Information about the Uniforms we care about in this pass. */
  protected List<UniformRef> _uniforms = new ArrayList<>();

  protected Map<UniformRef, Integer> _cachedLocations = new IdentityHashMap<>();

  public int getProgramId(final RenderContext context) {
    if (_shaderIdCache != null) {
      final Integer id = _shaderIdCache.getValue(context.getSharableContextRef());
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
      _shaderIdCache = ContextValueReference.newReference(this, TechniquePass._shaderRefQueue);
    }
    _shaderIdCache.put(context.getSharableContextRef(), id);
  }

  public void setName(final String name) { _name = name; }

  public String getName() { return _name; }

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

  /**
   * Set the value of a particular shader uniform, by shaderVariableName (which should, in theory, be
   * unique)
   *
   * @param shaderVarName
   *          Variable name in the shader that this uniform will connect to.
   * @param value
   *          the Value of the uniform.
   */
  public void setUniformValue(final String shaderVarName, final Object value) {
    _uniforms.forEach(u -> {
      if (shaderVarName.compareTo(u.getShaderVariableName()) == 0) {
        u.setValue(value);
      }
    });
  }

  public void addLightInfoUniforms(final int maxLights) {
    for (int i = 0; i < maxLights; i++) {
      addUniform(new UniformRef("lightProps.lights[" + i + "]", UniformType.UniformSupplier, UniformSource.Ardor3dState,
          Ardor3dStateProperty.Light, i, null));
    }
    addUniform(new UniformRef("lightProps.globalAmbient", UniformType.Float3, UniformSource.Ardor3dState,
        Ardor3dStateProperty.GlobalAmbientLight, null, LightManager.DEFAULT_GLOBAL_AMBIENT));
  }

  public void addShadowUniforms(final int maxShadows) {
    for (int i = 0; i < maxShadows; i++) {
      addUniform(new UniformRef("lightProps.shadowMaps[" + i + "]", UniformType.Int1, UniformSource.Ardor3dState,
          Ardor3dStateProperty.ShadowTexture, i, null));
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
    }
    shaderUtils.setBoundVAO(vaoID, context);

    // send our mesh data to the card, binding them to the VAO
    final int programId = getProgramId(context);
    for (int i = 0; i < _attributes.size(); i++) {
      final VertexAttributeRef attribute = _attributes.get(i);

      // Check for the item in our mesh data
      if (!data.containsKey(attribute.getMeshDataKey())) {
        if (Constants.strictVertexAttributes) {
          throw new Ardor3dException(
              "Required mesh data key '" + attribute.getMeshDataKey() + "' not found in mesh: " + mesh.getName());
        }
        continue;
      }

      // pull our related buffer
      final AbstractBufferData<? extends Buffer> buffer = data.getCoords(attribute.getMeshDataKey());

      // check we have a valid attribute location in the current program
      int location = attribute.getLocation();
      if (location < 0) {
        // Use the name to find our location
        location = shaderUtils.findAttributeLocation(programId, attribute.getShaderVariableName());

        // still less than 0? might have been removed during compilation - ignore
        if (location < 0) {
          continue;
        }
        attribute.setLocation(location);
      }

      // Make sure our buffer has a vbo and its data is on the card.
      shaderUtils.setupBufferObject(buffer, false, context);

      // Bind our buffer to the attribute location
      shaderUtils.bindVertexAttribute(attribute, buffer);
    }

    // make sure any index buffer we have is also up to date on the card
    if (data.getIndexBuffer() != null) {
      shaderUtils.setupBufferObject(data.getIndices(), true, context);
    }
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
    int location = _cachedLocations.containsKey(uniform) ? _cachedLocations.get(uniform) : uniform.getLocation();
    if (location < 0) {
      // Use the name to find our location
      location = shaderUtils.findUniformLocation(programId, namePrepend + uniform.getShaderVariableName());
      if (location < 0) {
        return;
      } else {
        _cachedLocations.put(uniform, location);
      }
    }

    shaderUtils.sendUniformValue(location, uniform, mesh);
  }

  private List<UniformRef> getUniformsFromSupplier(final UniformRef uniform, final Mesh mesh) {
    Object supplier = null;
    String clazzName = null;
    switch (uniform.getSource()) {
      case SpatialProperty:
        supplier = mesh.getProperty(uniform.getValue().toString(), uniform.getDefaultValue());
        clazzName = uniform.getExtra() != null ? uniform.getExtra().toString() : null;
        break;
      case Ardor3dState:
        final Ardor3dStateProperty prop = Ardor3dStateProperty.valueOf(uniform.getValue().toString());
        if (prop == Ardor3dStateProperty.Light) {
          final int index = (!(uniform.getExtra() instanceof Integer)) ? 0 : ((Integer) uniform.getExtra()).intValue();
          // grab the appropriate light from the current SceneIndexer's LightManager
          final SceneIndexer si = SceneIndexer.getCurrent();
          final LightManager lm = si != null ? si.getLightManager() : null;
          supplier = lm != null ? lm.getCurrentLight(index) : null;
          // defaults to point light if we don't get a supplier
          clazzName = "com.ardor3d.light.PointLight";
        } else {
          throw new Ardor3dException("Uniform type 'UniformSupplier' can not be used with Ardor3dState." + prop);
        }
        break;
      default:
        throw new Ardor3dException(
            "Unsupported Uniform Source type '" + uniform.getSource() + "' for use with UniformSupplier.");
    }

    if (supplier instanceof IUniformSupplier) {
      return ((IUniformSupplier) supplier).getUniforms();
    } else if (supplier == null && clazzName != null) {
      if (uniform._cachedDefaultSupplier != null) {
        return uniform._cachedDefaultSupplier.getUniforms();
      } else {
        final IUniformSupplier prov = IUniformSupplier.getDefaultProvider(clazzName);
        if (prov != null) {
          uniform._cachedDefaultSupplier = prov;
          return prov.getUniforms();
        }
      }
    }
    return null;
  }

  protected void applyRenderStates(final Renderer renderer, final Mesh mesh) {
    for (final StateType type : StateType.values) {
      renderer.applyState(type, mesh.getWorldRenderState(type));
    }
  }
}
