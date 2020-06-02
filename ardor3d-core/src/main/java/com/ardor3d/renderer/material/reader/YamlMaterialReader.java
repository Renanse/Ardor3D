/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.material.reader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix4;
import com.ardor3d.renderer.RenderMatrixType;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.renderer.material.MaterialTechnique;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.material.ShaderType;
import com.ardor3d.renderer.material.TechniquePass;
import com.ardor3d.renderer.material.VertexAttributeRef;
import com.ardor3d.renderer.material.fog.FogParams;
import com.ardor3d.renderer.material.uniform.AlphaTestConsts;
import com.ardor3d.renderer.material.uniform.Ardor3dStateProperty;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.surface.ColorSurface;
import com.ardor3d.surface.PbrSurface;
import com.ardor3d.surface.PbrTexturedSurface;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

public class YamlMaterialReader {

  /** Our class logger */
  private static final Logger logger = Logger.getLogger(YamlMaterialReader.class.getName());

  public static Map<String, RenderMaterial> loadAll(final ResourceSource source) throws IOException {

    final Map<String, RenderMaterial> rVal = new HashMap<>();

    if (source == null) {
      if (YamlMaterialReader.logger.isLoggable(Level.WARNING)) {
        YamlMaterialReader.logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "loadAll(ResourceSource)",
            "source was null.  Returning empty map.");
      }

      return rVal;
    }

    final Yaml yamlDoc = new Yaml();
    for (final Object doc : yamlDoc.loadAll(new UnicodeReader(source.openStream()))) {
      final RenderMaterial material = readMaterial(doc);
      if (material != null) {
        rVal.put(material.getName(), material);
      }
    }
    return rVal;
  }

  public static RenderMaterial load(final String sourceUrl) {
    try {
      return loadChecked(sourceUrl);
    } catch (final IOException ex) {
      YamlMaterialReader.logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "load(String)",
          "Unable to locate '" + sourceUrl + "'");
      ex.printStackTrace();
      return null;
    }
  }

  public static RenderMaterial loadChecked(final String sourceUrl) throws IOException {
    final ResourceSource source = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MATERIAL, sourceUrl);

    if (source == null) {
      throw new IOException("Unable to locate '" + sourceUrl + "'");
    }

    return load(source);
  }

  public static RenderMaterial load(final InputStream stream) {
    try {
      return loadChecked(stream);
    } catch (final IOException ex) {
      YamlMaterialReader.logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "load(String)",
          "Unable to read from stream. " + ex.getMessage());
      ex.printStackTrace();
      return null;
    }
  }

  public static RenderMaterial loadChecked(final InputStream stream) throws IOException {

    if (stream == null) {
      if (YamlMaterialReader.logger.isLoggable(Level.WARNING)) {
        YamlMaterialReader.logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "load(ResourceSource)",
            "stream was null.  Returning null.");
      }

      return null;
    }

    final Yaml yamlDoc = new Yaml();
    final Object doc = yamlDoc.load(new UnicodeReader(stream));
    return readMaterial(doc);
  }

  public static RenderMaterial load(final ResourceSource source) {
    try {
      return loadChecked(source);
    } catch (final IOException ex) {
      YamlMaterialReader.logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "load(ResourceSource)",
          "Unable to load '" + source + "'");
      return null;
    }

  }

  public static RenderMaterial loadChecked(final ResourceSource source) throws IOException {

    if (source == null) {
      if (YamlMaterialReader.logger.isLoggable(Level.WARNING)) {
        YamlMaterialReader.logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "load(ResourceSource)",
            "source was null.  Returning null.");
      }

      return null;
    }

    final Yaml yamlDoc = new Yaml();
    final Object doc = yamlDoc.load(new UnicodeReader(source.openStream()));
    return readMaterial(doc);
  }

  protected static RenderMaterial readMaterial(final Object doc) {
    // doc needs to be a Map here
    final Map<String, Object> properties = getMap(doc, true);

    final RenderMaterial material = new RenderMaterial();

    // parse our name
    material.setName(getString(properties, "name", null));

    // parse our techniques
    readTechniques(properties.get("techniques"), material.getTechniques());

    return material;
  }

  protected static void readTechniques(final Object doc, final List<MaterialTechnique> store) {
    if (doc == null) {
      return;
    }

    final List<Object> items = getList(doc, false);
    if (items == null) {
      final MaterialTechnique tech = readTechnique(doc);
      if (tech != null) {
        store.add(tech);
      }
      return;
    }

    for (final Object item : items) {
      final MaterialTechnique tech = readTechnique(item);
      if (tech != null) {
        store.add(tech);
      }
    }
  }

  protected static MaterialTechnique readTechnique(final Object doc) {
    // doc needs to be a Map here
    final Map<String, Object> properties = getMap(doc, true);

    final MaterialTechnique tech = new MaterialTechnique();

    // parse our name
    tech.setName(getString(properties, "name", null));

    // parse our passes
    readPasses(properties.get("passes"), tech.getPasses());

    return tech;
  }

  protected static void readPasses(final Object doc, final List<TechniquePass> store) {
    if (doc == null) {
      return;
    }

    final List<Object> items = getList(doc, false);
    if (items == null) {
      final TechniquePass pass = readPass(doc);
      if (pass != null) {
        store.add(pass);
      }
      return;
    }

    for (final Object item : items) {
      final TechniquePass pass = readPass(item);
      if (pass != null) {
        store.add(pass);
      }
    }
  }

  protected static TechniquePass readPass(final Object doc) {
    // doc needs to be a Map here
    final Map<String, Object> properties = getMap(doc, true);

    final TechniquePass pass = new TechniquePass();

    // parse our name
    pass.setName(getString(properties, "name", null));

    // parse our shaders
    readShaders(properties.get("shaders"), pass);

    // parse our attributes
    readAttributes(properties.get("attributes"), pass);

    // parse our uniforms
    readUniforms(properties.get("uniforms"), pass);

    return pass;
  }

  private static void readShaders(final Object doc, final TechniquePass pass) {
    // doc needs to be a Map here
    final Map<String, Object> properties = getMap(doc, true);

    for (final String key : properties.keySet()) {
      // convert key to a shader type
      final ShaderType type = ShaderType.valueOf(key);

      // read the program(s) that make up our shader
      final List<String> programs = readShaderPrograms(properties.get(key));
      if (programs != null && programs.size() > 0) {
        pass.setShader(type, programs);
      }
    }
  }

  private static List<String> readShaderPrograms(final Object doc) {
    if (doc == null) {
      return null;
    }

    // doc needs to be a Map here
    final Map<String, Object> properties = getMap(doc, true);

    // prepare our return value
    final List<String> rVal = new ArrayList<>();

    // ****** READ OUR INJECTIONS
    final String injSingle = getString(properties, "inject", null);
    final List<String> injects = new ArrayList<>();
    if (injSingle != null) {
      injects.add(injSingle);
    }
    final List<Object> injObjs = getList(properties.get("injects"), false);
    if (injObjs != null) {
      injObjs.forEach((final Object o) -> injects.add(o.toString()));
    }

    // ****** READ OUR DEFINES - SPECIAL KIND OF INJECT
    final String defSingle = getString(properties, "define", null);
    if (defSingle != null) {
      injects.add("#define " + defSingle);
    }
    final List<Object> defObjs = getList(properties.get("defines"), false);
    if (defObjs != null) {
      defObjs.forEach((final Object o) -> injects.add("#define " + o.toString()));
    }

    final String program = getString(properties, "program", null);
    final String srcSingle = getString(properties, "source", null);
    final List<Object> srcObjs = getList(properties.get("sources"), false);
    if (program != null && (srcSingle != null || srcObjs != null)) {
      YamlMaterialReader.logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "readShaderPrograms(Object)",
          "Found both program and source nodes in pass.  There can be only one.  Ignoring source.");
    }

    // ****** HANDLE INLINE PROGRAM IF ANY
    if (program != null) {
      // inflate any import statements we find
      final String text = MaterialManager.inflateShaderImports(program);
      rVal.add(MaterialManager.inject(text, injects));
      return rVal;
    }

    // ****** OTHERWISE, HANDLE EXTERNALLY DEFINED PROGRAM(S)
    final List<String> sources = new ArrayList<>();
    if (srcSingle != null) {
      sources.add(srcSingle);
    }
    if (srcObjs != null) {
      srcObjs.forEach((final Object o) -> sources.add(o.toString()));
    }

    // walk through our programs, read them and inject defines, if the program starts with #version
    sources.forEach((final String source) -> {
      final String text = MaterialManager.getShaderText(source, true);
      rVal.add(MaterialManager.inject(text, injects));
    });

    return rVal;
  }

  private static void readAttributes(final Object doc, final TechniquePass pass) {
    if (doc == null) {
      return;
    }

    final List<Object> items = getList(doc, false);
    if (items == null) {
      final VertexAttributeRef attrib = readAttribute(doc);
      if (attrib != null) {
        pass.addAttribute(attrib);
      }
      return;
    }

    for (final Object item : items) {
      final VertexAttributeRef attrib = readAttribute(item);
      if (attrib != null) {
        pass.addAttribute(attrib);
      }
    }
  }

  private static VertexAttributeRef readAttribute(final Object doc) {
    // doc needs to be a Map here
    final Map<String, Object> properties = getMap(doc, true);

    // override key
    final String key = getString(properties, "key", null);

    VertexAttributeRef rVal;
    final String meshKey = getString(properties, "meshKey", key);

    // Figure out which key type we are
    final int location = getInt(properties, "location", -1);
    if (location >= 0) {
      rVal = new VertexAttributeRef(location, meshKey);
    }

    else {
      final String shaderKey = getString(properties, "shaderKey", key);
      rVal = new VertexAttributeRef(shaderKey, meshKey);
    }

    // optional buffer properties
    rVal.setStride(getInt(properties, "stride", 0));
    rVal.setOffset(getInt(properties, "offset", 0));
    rVal.setDivisor(getInt(properties, "divisor", 0));
    rVal.setNormalized(getBoolean(properties, "normalized", false));

    return rVal;
  }

  private static void readUniforms(final Object doc, final TechniquePass pass) {
    if (doc == null) {
      return;
    }

    final List<Object> items = getList(doc, false);
    if (items == null) {
      final UniformRef uniform = readUniform(doc, pass);
      if (uniform != null) {
        pass.addUniform(uniform);
      }
      return;
    }

    for (final Object item : items) {
      final UniformRef uniform = readUniform(item, pass);
      if (uniform != null) {
        pass.addUniform(uniform);
      }
    }
  }

  private static UniformRef readUniform(final Object doc, final TechniquePass pass) {
    // doc needs to be a Map here
    final Map<String, Object> properties = getMap(doc, true);

    // check for builtIn requests first
    if (properties.containsKey("builtIn")) {
      final Object builtIn = properties.get("builtIn");
      final List<Object> values = getList(builtIn, false);
      if (values == null) {
        addDefaultUniform(getString(builtIn), pass);
      } else {
        for (final Object obj : values) {
          addDefaultUniform(getString(obj), pass);
        }
      }
      return null;
    }

    // get our shaderKey, if provided
    final String shaderKey = getString(properties, "shaderKey", null);

    // determine our type
    final UniformType type = getEnum(properties, "type", UniformType.class, UniformType.Int1);

    // determine our source
    final UniformSource source = getEnum(properties, "source", UniformSource.class, UniformSource.Value);

    // determine any extra
    final Object extra = properties.get("extra");

    // determine any default value
    final Object defaultValue = getBufferForType(properties.get("defaultValue"), type);

    // determine our value
    final Object value = readUniformValue(properties.get("value"), type, source, shaderKey);

    // Figure out which key type we are
    final int location = getInt(properties, "location", -1);
    if (location >= 0) {
      return new UniformRef(location, type, source, value, extra, defaultValue);
    }

    return new UniformRef(shaderKey, type, source, value, extra, defaultValue);
  }

  private static void addDefaultUniform(final String type, final TechniquePass pass) {
    switch (type) {
      case "view":
        pass.addUniform(
            new UniformRef("view", UniformType.Matrix4x4, UniformSource.RendererMatrix, RenderMatrixType.View));
        return;
      case "model":
        pass.addUniform(
            new UniformRef("model", UniformType.Matrix4x4, UniformSource.RendererMatrix, RenderMatrixType.Model));
        return;
      case "projection":
        pass.addUniform(new UniformRef("projection", UniformType.Matrix4x4, UniformSource.RendererMatrix,
            RenderMatrixType.Projection));
        return;
      case "normalMat":
        pass.addUniform(
            new UniformRef("normalMat", UniformType.Matrix3x3, UniformSource.RendererMatrix, RenderMatrixType.Normal));
        return;
      case "modelViewProj":
        pass.addUniform(new UniformRef("modelViewProj", UniformType.Matrix4x4, UniformSource.RendererMatrix,
            RenderMatrixType.ModelViewProjection));
        return;
      case "modelViewProjection":
        pass.addUniform(new UniformRef("modelViewProjection", UniformType.Matrix4x4, UniformSource.RendererMatrix,
            RenderMatrixType.ModelViewProjection));
        return;
      case "viewSize":
        pass.addUniform(new UniformRef("viewSize", UniformType.Float2, UniformSource.Ardor3dState,
            Ardor3dStateProperty.CurrentViewportSizePixels));
        return;
      case "viewOffset":
        pass.addUniform(new UniformRef("viewOffset", UniformType.Float2, UniformSource.Ardor3dState,
            Ardor3dStateProperty.CurrentViewportOffsetPixels));
        return;
      case "cameraLoc":
        pass.addUniform(new UniformRef("cameraLoc", UniformType.Float3, UniformSource.Ardor3dState,
            Ardor3dStateProperty.CurrentCameraLocation));
        return;
      case "defaultColor":
        pass.addUniform(new UniformRef("defaultColor", UniformType.Float4, UniformSource.Ardor3dState,
            Ardor3dStateProperty.MeshDefaultColorRGBA, null, ColorRGBA.WHITE));
        return;
      case "defaultColorRGB":
        pass.addUniform(new UniformRef("defaultColorRGB", UniformType.Float3, UniformSource.Ardor3dState,
            Ardor3dStateProperty.MeshDefaultColorRGB));
        return;
      case "colorSurface":
        pass.addUniform(new UniformRef(ColorSurface.DefaultPropertyKey, UniformType.UniformSupplier,
            UniformSource.SpatialProperty, ColorSurface.DefaultPropertyKey, "com.ardor3d.surface.ColorSurface", null));
        return;
      case "pbrSurface":
        pass.addUniform(new UniformRef(PbrSurface.DefaultPropertyKey, UniformType.UniformSupplier,
            UniformSource.SpatialProperty, PbrSurface.DefaultPropertyKey, "com.ardor3d.surface.PbrSurface", null));
        return;
      case "pbrTexturedSurface":
        pass.addUniform(new UniformRef(PbrTexturedSurface.DefaultPropertyKey, UniformType.UniformSupplier,
            UniformSource.SpatialProperty, PbrTexturedSurface.DefaultPropertyKey,
            "com.ardor3d.surface.PbrTexturedSurface", null));
        return;
      case "fogParams":
        pass.addUniform(
            new UniformRef(FogParams.DefaultPropertyKey, UniformType.UniformSupplier, UniformSource.SpatialProperty,
                FogParams.DefaultPropertyKey, "com.ardor3d.renderer.material.fog.FogParams", null));
        return;
      case "lights1":
        pass.addLightInfoUniforms(1);
        return;
      case "lights2":
        pass.addLightInfoUniforms(2);
        return;
      case "lights3":
        pass.addLightInfoUniforms(3);
        return;
      case "lights4":
        pass.addLightInfoUniforms(4);
        return;
      case "textureMatrix0":
        pass.addUniform(new UniformRef(Texture.KEY_TextureMatrix0, UniformType.Matrix4x4, UniformSource.SpatialProperty,
            Texture.KEY_TextureMatrix0, null, Matrix4.IDENTITY));
        return;
      case "textureMatrix1":
        pass.addUniform(new UniformRef(Texture.KEY_TextureMatrix1, UniformType.Matrix4x4, UniformSource.SpatialProperty,
            Texture.KEY_TextureMatrix1, null, Matrix4.IDENTITY));
        return;
      case "textureMatrix2":
        pass.addUniform(new UniformRef(Texture.KEY_TextureMatrix2, UniformType.Matrix4x4, UniformSource.SpatialProperty,
            Texture.KEY_TextureMatrix2, null, Matrix4.IDENTITY));
        return;
      case "textureMatrix3":
        pass.addUniform(new UniformRef(Texture.KEY_TextureMatrix3, UniformType.Matrix4x4, UniformSource.SpatialProperty,
            Texture.KEY_TextureMatrix3, null, Matrix4.IDENTITY));
        return;
      case "alphaTest":
        pass.addUniform(
            new UniformRef(AlphaTestConsts.KEY_AlphaTestType, UniformType.Int1, UniformSource.SpatialProperty,
                AlphaTestConsts.KEY_AlphaTestType, null, AlphaTestConsts.TestFunction.Always));
        pass.addUniform(new UniformRef(AlphaTestConsts.KEY_AlphaReference, UniformType.Float1,
            UniformSource.SpatialProperty, AlphaTestConsts.KEY_AlphaReference, null, 0f));
        return;
      default:
        throw new Ardor3dException("Unknown default uniform type: " + type);
    }
  }

  private static Object readUniformValue(final Object doc, final UniformType type, final UniformSource source,
      final String shaderKey) {
    switch (source) {
      case Ardor3dState:
        return Enum.valueOf(Ardor3dStateProperty.class, getString(doc));

      case RendererMatrix:
        return Enum.valueOf(RenderMatrixType.class, getString(doc));

      case SpatialProperty: {
        final String key = getString(doc);
        return (key == null) ? shaderKey : key;
      }

      case Value:
        return getBufferForType(doc, type);

      case Function:
      case Supplier:
      default:
        throw new Ardor3dException("unhandled UniformSource: " + source);

    }
  }

  private static Buffer getBufferForType(final Object doc, final UniformType type) {
    if (doc == null) {
      return null;
    }

    switch (type) {
      case Double1:
        return getDoubleBuffer(doc, 1);
      case Double2:
        return getDoubleBuffer(doc, 2);
      case Double3:
        return getDoubleBuffer(doc, 3);
      case Double4:
        return getDoubleBuffer(doc, 4);
      case Float1:
        return getFloatBuffer(doc, 1);
      case Float2:
        return getFloatBuffer(doc, 2);
      case Float3:
        return getFloatBuffer(doc, 3);
      case Float4:
        return getFloatBuffer(doc, 4);
      case Int1:
      case UInt1:
        return getIntBuffer(doc, 1);
      case Int2:
      case UInt2:
        return getIntBuffer(doc, 2);
      case Int3:
      case UInt3:
        return getIntBuffer(doc, 3);
      case Int4:
      case UInt4:
        return getIntBuffer(doc, 4);
      case Matrix2x2:
        return getFloatBuffer(doc, 2, 2);
      case Matrix2x2D:
        return getDoubleBuffer(doc, 2, 2);
      case Matrix2x3:
        return getFloatBuffer(doc, 2, 3);
      case Matrix2x3D:
        return getDoubleBuffer(doc, 2, 3);
      case Matrix2x4:
        return getFloatBuffer(doc, 2, 4);
      case Matrix2x4D:
        return getDoubleBuffer(doc, 2, 4);
      case Matrix3x2:
        return getFloatBuffer(doc, 3, 2);
      case Matrix3x2D:
        return getDoubleBuffer(doc, 3, 2);
      case Matrix3x3:
        return getFloatBuffer(doc, 3, 3);
      case Matrix3x3D:
        return getDoubleBuffer(doc, 3, 3);
      case Matrix3x4:
        return getFloatBuffer(doc, 3, 4);
      case Matrix3x4D:
        return getDoubleBuffer(doc, 3, 4);
      case Matrix4x2:
        return getFloatBuffer(doc, 4, 2);
      case Matrix4x2D:
        return getDoubleBuffer(doc, 4, 2);
      case Matrix4x3:
        return getFloatBuffer(doc, 4, 3);
      case Matrix4x3D:
        return getDoubleBuffer(doc, 4, 3);
      case Matrix4x4:
        return getFloatBuffer(doc, 4, 4);
      case Matrix4x4D:
        return getDoubleBuffer(doc, 4, 4);
      default:
        throw new Ardor3dException("Unhandled uniform type: " + type);
    }
  }

  /// PROPERTY READERS

  public static <T extends Enum<T>> T getEnum(final Map<String, Object> properties, final String key,
      final Class<T> enumType, final T defVal) {
    final String eVal = getString(properties, key, defVal != null ? defVal.name() : null);
    if (eVal != null) {
      return Enum.valueOf(enumType, eVal);
    } else {
      return null;
    }
  }

  protected static int getInt(final Map<String, Object> properties, final String key, final int defaultVal) {
    if (!properties.containsKey(key)) {
      return defaultVal;
    }

    return getInt(properties.get(key));
  }

  protected static boolean getBoolean(final Map<String, Object> properties, final String key,
      final boolean defaultVal) {
    if (!properties.containsKey(key)) {
      return defaultVal;
    }

    return getBoolean(properties.get(key));
  }

  protected static String getString(final Map<String, Object> properties, final String key, final String defaultVal) {
    if (!properties.containsKey(key)) {
      return defaultVal;
    }

    return getString(properties.get(key));
  }

  /// YAML NODE CONVERTERS

  private static final List<String> YAML_YES_VALUES = Arrays.asList("y", "yes", "true", "on");
  private static final List<String> YAML_NO_VALUES = Arrays.asList("n", "no", "false", "off");

  protected static boolean getBoolean(final Object doc) {
    if (doc instanceof Boolean) {
      return ((Boolean) doc).booleanValue();
    } else {
      final String value = doc.toString().trim().toLowerCase();
      if (YamlMaterialReader.YAML_YES_VALUES.contains(value)) {
        return true;
      }
      if (YamlMaterialReader.YAML_NO_VALUES.contains(value)) {
        return false;
      }
      throw new Ardor3dException(MessageFormat.format("Invalid yaml boolean value: ''{0}''", value));
    }
  }

  protected static int getInt(final Object doc) {
    if (doc instanceof Number) {
      return ((Number) doc).intValue();
    } else {
      return Integer.parseInt(doc.toString());
    }
  }

  protected static float getFloat(final Object doc) {
    if (doc instanceof Number) {
      return ((Number) doc).floatValue();
    } else {
      return Float.parseFloat(doc.toString());
    }
  }

  protected static double getDouble(final Object doc) {
    if (doc instanceof Number) {
      return ((Number) doc).doubleValue();
    } else {
      return Double.parseDouble(doc.toString());
    }
  }

  protected static String getString(final Object doc) {
    return doc != null ? doc.toString() : null;
  }

  @SuppressWarnings("unchecked")
  private static DoubleBuffer getDoubleBuffer(final Object doc, final int sizeM, final int sizeN) {
    if (doc == null) {
      return null;
    }

    if (!(doc instanceof ArrayList<?>)) {
      throw new Ardor3dException("Incorrect type.  Expected ArrayList of size " + sizeM + ".  Got: "
          + doc.getClass().getName() + " value: " + doc);
    }
    final List<Object> vals = (ArrayList<Object>) doc;
    if (vals.size() != sizeM) {
      throw new Ardor3dException("Incorrect size.  Expected: " + sizeM + " value: " + doc);
    }

    final DoubleBuffer buff = BufferUtils.createDoubleBuffer(sizeM * sizeN);
    for (final Object val : vals) {
      buff.put(getDoubleBuffer(val, sizeN));
    }

    buff.flip();
    return buff;
  }

  @SuppressWarnings("unchecked")
  private static DoubleBuffer getDoubleBuffer(final Object doc, final int size) {
    if (doc == null) {
      return null;
    }
    DoubleBuffer buff;
    if (size == 1) {
      buff = BufferUtils.createDoubleBuffer(1);
      buff.put(getDouble(doc));
    } else {
      if (!(doc instanceof ArrayList<?>)) {
        throw new Ardor3dException("Incorrect type.  Expected ArrayList of size " + size + ".  Got: "
            + doc.getClass().getName() + " value: " + doc);
      }
      final List<Object> vals = (ArrayList<Object>) doc;
      if (vals.size() != size) {
        throw new Ardor3dException("Incorrect size.  Expected: " + size + " value: " + doc);
      }

      buff = BufferUtils.createDoubleBuffer(size);
      for (final Object val : vals) {
        buff.put(getDouble(val));
      }
    }

    buff.flip();
    return buff;
  }

  @SuppressWarnings("unchecked")
  private static FloatBuffer getFloatBuffer(final Object doc, final int sizeM, final int sizeN) {
    if (doc == null) {
      return null;
    }

    if (!(doc instanceof ArrayList<?>)) {
      throw new Ardor3dException("Incorrect type.  Expected ArrayList of size " + sizeM + ".  Got: "
          + doc.getClass().getName() + " value: " + doc);
    }
    final List<Object> vals = (ArrayList<Object>) doc;
    if (vals.size() != sizeM) {
      throw new Ardor3dException("Incorrect size.  Expected: " + sizeM + " value: " + doc);
    }

    final FloatBuffer buff = BufferUtils.createFloatBuffer(sizeM * sizeN);
    for (final Object val : vals) {
      buff.put(getFloatBuffer(val, sizeN));
    }

    buff.flip();
    return buff;
  }

  @SuppressWarnings("unchecked")
  private static FloatBuffer getFloatBuffer(final Object doc, final int size) {
    if (doc == null) {
      return null;
    }
    FloatBuffer buff;
    if (size == 1) {
      buff = BufferUtils.createFloatBuffer(1);
      buff.put(getFloat(doc));
    } else {
      if (!(doc instanceof ArrayList<?>)) {
        throw new Ardor3dException("Incorrect type.  Expected ArrayList of size " + size + ".  Got: "
            + doc.getClass().getName() + " value: " + doc);
      }
      final List<Object> vals = (ArrayList<Object>) doc;
      if (vals.size() != size) {
        throw new Ardor3dException("Incorrect size.  Expected: " + size + " value: " + doc);
      }

      buff = BufferUtils.createFloatBuffer(size);
      for (final Object val : vals) {
        buff.put(getFloat(val));
      }
    }

    buff.flip();
    return buff;
  }

  @SuppressWarnings("unchecked")
  private static IntBuffer getIntBuffer(final Object doc, final int size) {
    if (doc == null) {
      return null;
    }
    IntBuffer buff;
    if (size == 1) {
      buff = BufferUtils.createIntBuffer(1);
      buff.put(getInt(doc));
    } else {
      if (!(doc instanceof ArrayList<?>)) {
        throw new Ardor3dException("Incorrect type.  Expected ArrayList of size " + size + ".  Got: "
            + doc.getClass().getName() + " value: " + doc);
      }
      final List<Object> vals = (ArrayList<Object>) doc;
      if (vals.size() != size) {
        throw new Ardor3dException("Incorrect size.  Expected: " + size + " value: " + doc);
      }

      buff = BufferUtils.createIntBuffer(size);
      for (final Object val : vals) {
        buff.put(getInt(val));
      }
    }

    buff.flip();
    return buff;
  }

  @SuppressWarnings("unchecked")
  protected static Map<String, Object> getMap(final Object doc, final boolean verify) {
    if (!(doc instanceof Map<?, ?>)) {
      if (verify) {
        throw new Ardor3dException("Could not parse material.  Expected Map at: " + doc);
      } else {
        return null;
      }
    }

    return (Map<String, Object>) doc;
  }

  @SuppressWarnings("unchecked")
  protected static List<Object> getList(final Object doc, final boolean verify) {
    if (!(doc instanceof List<?>)) {
      if (verify) {
        throw new Ardor3dException("Could not parse material.  Expected List at: " + doc);
      } else {
        return null;
      }
    }

    return (List<Object>) doc;
  }
}
