/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material;

import java.io.*;
import java.lang.ref.ReferenceQueue;
import java.nio.Buffer;
import java.util.*;

import com.ardor3d.buffer.AbstractBufferData;
import com.ardor3d.light.LightManager;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.RendererCallable;
import com.ardor3d.renderer.material.uniform.Ardor3dStateProperty;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.SceneIndexer;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.Constants;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.collection.Multimap;
import com.ardor3d.util.collection.SimpleMultimap;
import com.ardor3d.util.gc.ContextValueReference;

public class TechniquePass {
  /** Name of this pass - optional, useful for debug, etc. */
  protected String _name;

  private static final Map<TechniquePass, Object> _identityCache = Collections.synchronizedMap(new WeakHashMap<>());
  private static final Object STATIC_REF = new Object();

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

  public TechniquePass() {
    synchronized (_identityCache) {
      _identityCache.put(this, STATIC_REF);
    }
  }

  /**
   * @param context the OpenGL context to get our id for.
   * @return the program id of a shader program in the given context. If the program is not found in the given
   * *         context rep, 0 is returned.
   */
  public int getProgramId(final RenderContext context) {
    return getProgramIdByRef(context.getSharableContextRef());
  }

  /**
   * @param contextRef the reference to a shared GL context to get our id for.
   * @return the program id of a shader program in the given context. If the program is not found in the given
   * context rep, 0 is returned.
   */
  public int getProgramIdByRef(final RenderContext.RenderContextRef contextRef) {
    if (_shaderIdCache != null) {
      final Integer id = _shaderIdCache.getValue(contextRef);
      if (id != null) {
        return id;
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

  /**
   * Clean all tracked Shader Programs from the hardware, using the given utility object to do the work immediately,
   * if given. If not, we will delete in the next execution of the appropriate context's game task
   * render queue.
   *
   * @param utils
   *          the util class to use. If null, execution will not occur immediately.
   */
  public static void cleanAllPrograms(final IShaderUtils utils) {
    final Multimap<RenderContext.RenderContextRef, Integer> idMap = new SimpleMultimap<>();

    // gather up expired shader program ids... these don't exist in our cache
    gatherGCdIds(idMap);

    Set<TechniquePass> keySetCopy;
    synchronized (_identityCache) {
      keySetCopy = new HashSet<>(_identityCache.keySet());
    }

    // Walk through the cached items and delete those too.
    for (final TechniquePass pass : keySetCopy) {
      if (pass._shaderIdCache != null) {
        if (Constants.useMultipleContexts) {
          final Set<RenderContext.RenderContextRef> contextObjects = pass._shaderIdCache.getContextRefs();
          for (final RenderContext.RenderContextRef o : contextObjects) {
            // Add id to map
            idMap.put(o, pass.getProgramIdByRef(o));
          }
        } else {
          idMap.put(ContextManager.getCurrentContext().getSharableContextRef(), pass.getProgramIdByRef(null));
        }
        pass._shaderIdCache.clear();
      }
    }

    // send to be deleted (perhaps on next render.)
    handleProgramDelete(utils, idMap);
  }

  /**
   * Clean all tracked Shader Programs associated with the given RenderContext from the hardware, using the given utility object to do the work immediately,
   * if given. If not, we will delete in the next execution of the appropriate context's game task
   * render queue.
   *
   * @param utils   the util class to use. If null, execution will not occur immediately.
   * @param context the context to clean programs for.
   */
  public static void cleanAllPrograms(final IShaderUtils utils, final RenderContext context) {
    final Multimap<RenderContext.RenderContextRef, Integer> idMap = new SimpleMultimap<>();

    // gather up expired vbos... these don't exist in our cache
    gatherGCdIds(idMap);

    final RenderContext.RenderContextRef glRef = context.getSharableContextRef();

    Set<TechniquePass> keySetCopy;
    synchronized (_identityCache) {
      keySetCopy = new HashSet<>(_identityCache.keySet());
    }

    // Walk through the cached items and delete those too.
    for (final TechniquePass pass : keySetCopy) {
      // only worry about buffers that have received ids.
      if (pass._shaderIdCache != null) {
        final Integer id = pass._shaderIdCache.removeValue(glRef);
        if (id != null && id != 0) {
          idMap.put(context.getSharableContextRef(), id);
        }
      }
    }

    // send to be deleted (perhaps on next render.)
    handleProgramDelete(utils, idMap);
  }

  /**
   * Clean tracked Shader Programs from the hardware for this TechniquePass, using the given utility object to do the work immediately,
   * if given. If not, we will delete in the next execution of the appropriate context's game task
   * render queue.
   *
   * @param utils the util class to use. If null, execution will not occur immediately.
   */
  public void cleanProgram(final IShaderUtils utils) {
    if (_shaderIdCache != null) {
      final Multimap<RenderContext.RenderContextRef, Integer> idMap = new SimpleMultimap<>();

      if (Constants.useMultipleContexts) {
        final Set<RenderContext.RenderContextRef> contextObjects = _shaderIdCache.getContextRefs();
        for (final RenderContext.RenderContextRef o : contextObjects) {
          // Add id to map
          idMap.put(o, getProgramIdByRef(o));
        }
      } else {
        idMap.put(ContextManager.getCurrentContext().getSharableContextRef(), getProgramIdByRef(null));
      }

      // clear out the cache
      _shaderIdCache.clear();

      // send to be deleted (perhaps on next render.)
      handleProgramDelete(utils, idMap);
    }
  }

  /**
   * Clean any tracked, expired Shader Programs from the hardware, using the given utility object to do the work immediately,
   * if given. If not, we will delete in the next execution of the appropriate context's game task
   * render queue.
   * <p>
   * A Shader Program is considered expired if it has been garbage collected by Java.
   *
   * @param utils the util class to use. If null, execution will not occur immediately.
   */
  public static void cleanExpiredPrograms(final IShaderUtils utils) {
    // gather up expired vbos...
    final Multimap<RenderContext.RenderContextRef, Integer> idMap = gatherGCdIds(null);

    if (idMap != null) {
      // send to be deleted (perhaps on next render.)
      handleProgramDelete(utils, idMap);
    }
  }

  @SuppressWarnings("unchecked")
  private static Multimap<RenderContext.RenderContextRef, Integer> gatherGCdIds(Multimap<RenderContext.RenderContextRef, Integer> store) {
    // Pull all expired shader programs from ref queue and add to an id multimap.
    ContextValueReference<TechniquePass, Integer> ref;
    while ((ref = (ContextValueReference<TechniquePass, Integer>) _shaderRefQueue.poll()) != null) {
      if (Constants.useMultipleContexts) {
        final Set<RenderContext.RenderContextRef> renderRefs = ref.getContextRefs();
        for (final RenderContext.RenderContextRef renderRef : renderRefs) {
          // Add id to map
          final Integer id = ref.getValue(renderRef);
          if (id != null) {
            if (store == null) { // lazy init
              store = new SimpleMultimap<>();
            }
            store.put(renderRef, id);
          }
        }
      } else {
        final Integer id = ref.getValue(null);
        if (id != null) {
          if (store == null) { // lazy init
            store = new SimpleMultimap<>();
          }
          store.put(ContextManager.getCurrentContext().getSharableContextRef(), id);
        }
      }
      ref.clear();
    }

    return store;
  }

  private static void handleProgramDelete(final IShaderUtils utils, final Multimap<RenderContext.RenderContextRef, Integer> idMap) {
    RenderContext.RenderContextRef currentSharableRef = null;
    // Grab the current context, if any.
    if (utils != null && ContextManager.getCurrentContext() != null) {
      currentSharableRef = ContextManager.getCurrentContext().getSharableContextRef();
    }
    // For each affected context...
    for (final RenderContext.RenderContextRef sharableRef : idMap.keySet()) {
      // If we have a deleter and the context is current, immediately delete
      if (utils != null && sharableRef.equals(currentSharableRef)) {
        utils.deleteShaderPrograms(idMap.values(sharableRef));
      }
      // Otherwise, add a delete request to that context's render task queue.
      else {
        GameTaskQueueManager.getManager(ContextManager.getContextForSharableRef(sharableRef))
            .render(new RendererCallable<Void>() {
              @Override
              public Void call() {
                getRenderer().getShaderUtils().deleteShaderPrograms(idMap.values(sharableRef));
                return null;
              }
            });
      }
    }
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
      var writer = new StringWriter();
      reader.transferTo(writer);
      text = writer.toString();
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
    if (location == Integer.MIN_VALUE) {
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
    Object supplier;
    String clazzName;
    switch (uniform.getSource()) {
      case SpatialProperty:
        supplier = mesh.getProperty(uniform.getValue().toString(), uniform.getDefaultValue());
        clazzName = uniform.getExtra() != null ? uniform.getExtra().toString() : null;
        break;
      case Ardor3dState:
        final Ardor3dStateProperty prop = Ardor3dStateProperty.valueOf(uniform.getValue().toString());
        final SceneIndexer si = SceneIndexer.getCurrent();
        final LightManager lm = si != null ? si.getLightManager() : null;
        if (prop == Ardor3dStateProperty.LightProperties) {
          supplier = lm;
          clazzName = null;
        } else if (prop == Ardor3dStateProperty.Light) {
          final int index = (!(uniform.getExtra() instanceof Integer)) ? 0 : (Integer) uniform.getExtra();
          // grab the appropriate light from the current SceneIndexer's LightManager
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
