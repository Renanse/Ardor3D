/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.material.reader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import com.ardor3d.renderer.RenderMatrixType;
import com.ardor3d.renderer.material.MaterialTechnique;
import com.ardor3d.renderer.material.RenderMaterial;
import com.ardor3d.renderer.material.ShaderType;
import com.ardor3d.renderer.material.TechniquePass;
import com.ardor3d.renderer.material.VertexAttributeRef;
import com.ardor3d.renderer.material.uniform.RenderStateProperty;
import com.ardor3d.renderer.material.uniform.UniformRef;
import com.ardor3d.renderer.material.uniform.UniformSource;
import com.ardor3d.renderer.material.uniform.UniformType;
import com.ardor3d.util.Ardor3dException;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.google.common.io.CharStreams;

public class YamlMaterialReader {

    /** Our class logger */
    private static final Logger logger = Logger.getLogger(YamlMaterialReader.class.getName());

    public static Map<String, RenderMaterial> loadAll(final ResourceSource source) throws IOException {

        final Map<String, RenderMaterial> rVal = new HashMap<>();

        if (source == null) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "loadAll(ResourceSource)",
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
            logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "load(String)",
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

    public static RenderMaterial load(final ResourceSource source) {
        try {
            return loadChecked(source);
        } catch (final IOException ex) {
            logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "load(ResourceSource)",
                    "Unable to load '" + source + "'");
            return null;
        }

    }

    public static RenderMaterial loadChecked(final ResourceSource source) throws IOException {

        if (source == null) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.logp(Level.WARNING, YamlMaterialReader.class.getName(), "load(ResourceSource)",
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

        // ****** READ OUR DEFINES
        final String defSingle = getString(properties, "define", null);
        final List<String> defines = new ArrayList<>();
        if (defSingle != null) {
            defines.add(defSingle);
        }
        final List<Object> defObjs = getList(properties.get("defines"), false);
        if (defObjs != null) {
            defObjs.forEach((final Object o) -> defines.add(o.toString()));
        }

        // ****** READ OUR INLINE PROGRAM(S)
        final String prgSingle = getString(properties, "program", null);
        final List<String> programs = new ArrayList<>();
        if (prgSingle != null) {
            programs.add(prgSingle);
        }
        final List<Object> prgObjs = getList(properties.get("programs"), false);
        if (prgObjs != null) {
            prgObjs.forEach((final Object o) -> programs.add(o.toString()));
        }

        // walk through our programs and inject defines, if the program starts with #version
        programs.forEach((final String prg) -> {
            rVal.add(injectDefines(prg, defines));
        });

        // ****** READ OUR EXTERNALLY DEFINED PROGRAM(S)
        final String srcSingle = getString(properties, "source", null);
        final List<String> sources = new ArrayList<>();
        if (srcSingle != null) {
            sources.add(srcSingle);
        }
        final List<Object> srcObjs = getList(properties.get("sources"), false);
        if (srcObjs != null) {
            srcObjs.forEach((final Object o) -> sources.add(o.toString()));
        }

        // walk through our programs, read them and inject defines, if the program starts with #version
        sources.forEach((final String source) -> {
            final ResourceSource src = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_SHADER, source);
            if (src == null) {
                return;
            }

            String text = null;
            try (final Reader reader = new InputStreamReader(src.openStream())) {
                text = CharStreams.toString(reader);
            } catch (final IOException ex) {
                logger.logp(Level.SEVERE, YamlMaterialReader.class.getName(), "readShaderPrograms(Object)",
                        "Failed to read a shader source: " + source + " Error: " + ex.getMessage());
                ex.printStackTrace();
                return;
            }

            rVal.add(injectDefines(text, defines));
        });

        return rVal;
    }

    private static String injectDefines(final String program, final List<String> defines) {
        if (defines.isEmpty() || !program.startsWith("#version")) {
            return program;
        }

        final StringBuilder sb = new StringBuilder();
        int firstLF = program.indexOf('\n');
        if (firstLF == -1) {
            firstLF = program.indexOf('\r');
        }
        sb.append(program.substring(0, firstLF));
        sb.append('\n');
        defines.forEach((final String define) -> sb.append(define).append('\n'));
        sb.append(program.substring(firstLF + 1));
        return sb.toString();
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

        // Figure out which key type we are
        final int location = getInt(properties, "location", -1);
        if (location >= 0) {
            final String meshKey = getString(properties, "meshKey", key);
            return new VertexAttributeRef(location, meshKey);
        }

        final String meshKey = getString(properties, "meshKey", key);
        final String shaderKey = getString(properties, "shaderKey", key);
        return new VertexAttributeRef(shaderKey, meshKey);
    }

    private static void readUniforms(final Object doc, final TechniquePass pass) {
        if (doc == null) {
            return;
        }

        final List<Object> items = getList(doc, false);
        if (items == null) {
            final UniformRef uniform = readUniform(doc);
            if (uniform != null) {
                pass.addUniform(uniform);
            }
            return;
        }

        for (final Object item : items) {
            final UniformRef uniform = readUniform(item);
            if (uniform != null) {
                pass.addUniform(uniform);
            }
        }
    }

    private static UniformRef readUniform(final Object doc) {
        // doc needs to be a Map here
        final Map<String, Object> properties = getMap(doc, true);

        // determine our type
        final UniformType type = getEnum(properties, "type", UniformType.class, UniformType.Int1);

        // determine our source
        final UniformSource source = getEnum(properties, "source", UniformSource.class, UniformSource.Value);

        // determine our value
        final Object value = readUniformValue(properties.get("value"), type, source);

        // Figure out which key type we are
        final int location = getInt(properties, "location", -1);
        if (location >= 0) {
            return new UniformRef(location, type, source, value);
        }

        final String shaderKey = getString(properties, "shaderKey", null);
        return new UniformRef(shaderKey, type, source, value);
    }

    private static Object readUniformValue(final Object doc, final UniformType type, final UniformSource source) {
        switch (source) {
            case RenderState:
                return Enum.valueOf(RenderStateProperty.class, getString(doc));

            case RendererMatrix:
                return Enum.valueOf(RenderMatrixType.class, getString(doc));

            case SpatialProperty:
                return getString(doc);

            case Value:
                return getBufferForType(doc, type);

            case Function:
            default:
                throw new Ardor3dException("unhandled UniformSource: " + source);

        }
    }

    private static Buffer getBufferForType(final Object doc, final UniformType type) {
        // TODO: FINISH!
        switch (type) {
            case Double1:
                break;
            case Double2:
                break;
            case Double3:
                break;
            case Double4:
                break;
            case Float1:
                break;
            case Float2:
                break;
            case Float3:
                break;
            case Float4:
                break;
            case Int1:
                break;
            case Int2:
                break;
            case Int3:
                break;
            case Int4:
                break;
            case Matrix2x2:
                break;
            case Matrix2x2D:
                break;
            case Matrix2x3:
                break;
            case Matrix2x3D:
                break;
            case Matrix2x4:
                break;
            case Matrix2x4D:
                break;
            case Matrix3x2:
                break;
            case Matrix3x2D:
                break;
            case Matrix3x3:
                break;
            case Matrix3x3D:
                break;
            case Matrix3x4:
                break;
            case Matrix3x4D:
                break;
            case Matrix4x2:
                break;
            case Matrix4x2D:
                break;
            case Matrix4x3:
                break;
            case Matrix4x3D:
                break;
            case Matrix4x4:
                break;
            case Matrix4x4D:
                break;
            case UInt1:
                break;
            case UInt2:
                break;
            case UInt3:
                break;
            case UInt4:
                break;
            default:
                break;

        }
        return null;
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

    protected static String getString(final Map<String, Object> properties, final String key, final String defaultVal) {
        if (!properties.containsKey(key)) {
            return defaultVal;
        }

        return getString(properties.get(key));
    }

    /// YAML NODE CONVERTERS

    protected static int getInt(final Object doc) {
        return (Integer) doc;
    }

    protected static String getString(final Object doc) {
        return doc.toString();
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
