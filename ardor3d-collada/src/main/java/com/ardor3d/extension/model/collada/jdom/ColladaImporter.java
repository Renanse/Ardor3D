/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.DefaultJDOMFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMFactory;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.SAXHandler;
import org.jdom2.input.sax.SAXHandlerFactory;
import org.xml.sax.SAXException;

import com.ardor3d.extension.animation.skeletal.Joint;
import com.ardor3d.extension.model.collada.jdom.data.AssetData;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.extension.model.collada.jdom.plugin.ColladaExtraPlugin;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.util.geom.GeometryTool.MatchCondition;
import com.ardor3d.util.resource.RelativeResourceLocator;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Main class for importing Collada files.
 * <p>
 * Example usages:
 * <li>new ColladaImporter().load(resource);</li>
 * <li>new ColladaImporter().loadTextures(false).modelLocator(locator).load(resource);</li>
 * </p>
 */
public class ColladaImporter {
    private boolean _loadTextures = true;
    private boolean _flipTransparency = false;
    private boolean _loadAnimations = true;
    private ResourceLocator _textureLocator;
    private ResourceLocator _modelLocator;
    private boolean _compressTextures = false;
    private boolean _optimizeMeshes = false;
    private final EnumSet<MatchCondition> _optimizeSettings = EnumSet.of(MatchCondition.UVs, MatchCondition.Normal,
            MatchCondition.Color);
    private Map<String, Joint> _externalJointMapping;
    private final List<ColladaExtraPlugin> _extraPlugins = new ArrayList<>();

    public boolean isLoadTextures() {
        return _loadTextures;
    }

    public ColladaImporter setLoadTextures(final boolean loadTextures) {
        _loadTextures = loadTextures;
        return this;
    }

    public boolean isCompressTextures() {
        return _compressTextures;
    }

    public ColladaImporter setCompressTextures(final boolean compressTextures) {
        _compressTextures = compressTextures;
        return this;
    }

    public boolean isLoadAnimations() {
        return _loadAnimations;
    }

    public ColladaImporter setLoadAnimations(final boolean loadAnimations) {
        _loadAnimations = loadAnimations;
        return this;
    }

    public boolean isFlipTransparency() {
        return _flipTransparency;
    }

    public void addExtraPlugin(final ColladaExtraPlugin plugin) {
        _extraPlugins.add(plugin);
    }

    public void clearExtraPlugins() {
        _extraPlugins.clear();
    }

    /**
     * @param flipTransparency
     *            if true, invert the value of any "transparency" entries found - required for some exporters.
     * @return this importer, for chaining
     */
    public ColladaImporter setFlipTransparency(final boolean flipTransparency) {
        _flipTransparency = flipTransparency;
        return this;
    }

    public ResourceLocator getTextureLocator() {
        return _textureLocator;
    }

    public ColladaImporter setTextureLocator(final ResourceLocator textureLocator) {
        _textureLocator = textureLocator;
        return this;
    }

    public ColladaImporter setExternalJoints(final Map<String, Joint> map) {
        _externalJointMapping = map;
        return this;
    }

    public Map<String, Joint> getExternalJoints() {
        return _externalJointMapping;
    }

    public ResourceLocator getModelLocator() {
        return _modelLocator;
    }

    public ColladaImporter setModelLocator(final ResourceLocator modelLocator) {
        _modelLocator = modelLocator;
        return this;
    }

    public boolean isOptimizeMeshes() {
        return _optimizeMeshes;
    }

    public void setOptimizeMeshes(final boolean optimizeMeshes) {
        _optimizeMeshes = optimizeMeshes;
    }

    public Set<MatchCondition> getOptimizeSettings() {
        return ImmutableSet.copyOf(_optimizeSettings);
    }

    public void setOptimizeSettings(final MatchCondition... optimizeSettings) {
        _optimizeSettings.clear();
        for (final MatchCondition cond : optimizeSettings) {
            _optimizeSettings.add(cond);
        }
    }

    /**
     * Reads a Collada file from the given resource and returns it as a ColladaStorage object.
     *
     * @param resource
     *            the name of the resource to find. ResourceLocatorTool will be used with TYPE_MODEL to find the
     *            resource.
     * @return a ColladaStorage data object containing the Collada scene and other useful elements.
     * @throws IOException
     *             if the resource can not be located or loaded for some reason.
     */
    public ColladaStorage load(final String resource) throws IOException {
        final ResourceSource source;
        if (_modelLocator == null) {
            source = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL, resource);
        } else {
            source = _modelLocator.locateResource(resource);
        }

        if (source == null) {
            throw new IOException("Unable to locate '" + resource + "'");
        }

        return load(source);
    }

    /**
     * Reads a Collada file from the given resource and returns it as a ColladaStorage object.
     *
     * @param resource
     *            the name of the resource to find.
     * @return a ColladaStorage data object containing the Collada scene and other useful elements.
     * @throws IOException
     *             if the resource can not be loaded for some reason.
     */
    public ColladaStorage load(final ResourceSource resource) throws IOException {
        final ColladaStorage colladaStorage = new ColladaStorage();
        final DataCache dataCache = new DataCache();
        if (_externalJointMapping != null) {
            dataCache.getExternalJointMapping().putAll(_externalJointMapping);
        }
        final ColladaDOMUtil colladaDOMUtil = new ColladaDOMUtil(dataCache);
        final ColladaMaterialUtils colladaMaterialUtils = new ColladaMaterialUtils(this, dataCache, colladaDOMUtil);
        final ColladaMeshUtils colladaMeshUtils = new ColladaMeshUtils(dataCache, colladaDOMUtil, colladaMaterialUtils,
                _optimizeMeshes, _optimizeSettings);
        final ColladaAnimUtils colladaAnimUtils = new ColladaAnimUtils(colladaStorage, dataCache, colladaDOMUtil,
                colladaMeshUtils);
        final ColladaNodeUtils colladaNodeUtils = new ColladaNodeUtils(dataCache, colladaDOMUtil, colladaMaterialUtils,
                colladaMeshUtils, colladaAnimUtils);

        try {
            // Pull in the DOM tree of the Collada resource.
            final Element collada = readCollada(resource, dataCache);

            // if we don't specify a texture locator, add a temporary texture locator at the location of this model
            // resource..
            final boolean addLocator = _textureLocator == null;

            final RelativeResourceLocator loc;
            if (addLocator) {
                loc = new RelativeResourceLocator(resource);
                ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);
            } else {
                loc = null;
            }

            final AssetData assetData = colladaNodeUtils.parseAsset(collada.getChild("asset"));

            // Collada may or may not have a scene, so this can return null.
            final Node scene = colladaNodeUtils.getVisualScene(collada);

            if (_loadAnimations) {
                colladaAnimUtils.parseLibraryAnimations(collada);
            }

            // reattach attachments to scene
            if (scene != null) {
                colladaNodeUtils.reattachAttachments(scene);
            }

            // set our scene into storage
            colladaStorage.setScene(scene);

            // set our asset data into storage
            colladaStorage.setAssetData(assetData);

            // drop our added locator if needed.
            if (addLocator) {
                ResourceLocatorTool.removeResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, loc);
            }

            // copy across our mesh colors - only for objects with multiple channels
            final Multimap<MeshData, FloatBuffer> colors = ArrayListMultimap.create();
            final Multimap<MeshData, FloatBuffer> temp = dataCache.getParsedVertexColors();
            for (final MeshData key : temp.keySet()) {
                // only copy multiple channels since their data is lost
                final Collection<FloatBuffer> val = temp.get(key);
                if (val != null && val.size() > 1) {
                    colors.putAll(key, val);
                }
            }
            colladaStorage.setParsedVertexColors(colors);

            // copy across our mesh material info
            colladaStorage.setMeshMaterialInfo(dataCache.getMeshMaterialMap());
            colladaStorage.setMaterialMap(dataCache.getMaterialInfoMap());

            // return storage
            return colladaStorage;
        } catch (final Exception e) {
            throw new IOException("Unable to load collada resource from URL: " + resource, e);
        }
    }

    /**
     * Reads the whole Collada DOM tree from the given resource and returns its root element. Exceptions may be thrown
     * by underlying tools; these will be wrapped in a RuntimeException and rethrown.
     *
     * @param resource
     *            the ResourceSource to read the resource from
     * @return the Collada root element
     */
    private Element readCollada(final ResourceSource resource, final DataCache dataCache) {
        try {
            final JDOMFactory jdomFac = new ArdorFactory(dataCache);
            final SAXBuilder builder = new SAXBuilder(null, new SAXHandlerFactory() {
                @Override
                public SAXHandler createSAXHandler(final JDOMFactory factory) {
                    return new SAXHandler(jdomFac) {
                        @Override
                        public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
                            // Just kill what's usually done here...
                        }
                    };
                }
            }, jdomFac);

            final Document doc = builder.build(resource.openStream());
            final Element collada = doc.getRootElement();

            // ColladaDOMUtil.stripNamespace(collada);

            return collada;
        } catch (final Exception e) {
            throw new RuntimeException("Unable to load collada resource from source: " + resource, e);
        }
    }

    private enum BufferType {
        None, Float, Double, Int, String, P
    }

    /**
     * A JDOMFactory that normalizes all text (strips extra whitespace etc), preparses all arrays and hashes all
     * elements based on their id/sid.
     */
    private static final class ArdorFactory extends DefaultJDOMFactory {
        private final Logger logger = Logger.getLogger(ArdorFactory.class.getName());

        private final DataCache dataCache;
        private Element currentElement;
        private BufferType bufferType = BufferType.None;
        private int count = 0;
        private final List<String> list = new ArrayList<String>();

        ArdorFactory(final DataCache dataCache) {
            this.dataCache = dataCache;
        }

        @Override
        public Text text(final int line, final int col, final String text) {
            try {
                switch (bufferType) {
                    case Float: {
                        final String normalizedText = Text.normalizeString(text);
                        if (normalizedText.length() == 0) {
                            return new Text("");
                        }
                        final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
                        final float[] floatArray = new float[count];
                        for (int i = 0; i < count; i++) {
                            floatArray[i] = parseFloat(tokenizer.nextToken());
                        }

                        dataCache.getFloatArrays().put(currentElement, floatArray);

                        return new Text("");
                    }
                    case Double: {
                        final String normalizedText = Text.normalizeString(text);
                        if (normalizedText.length() == 0) {
                            return new Text("");
                        }
                        final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
                        final double[] doubleArray = new double[count];
                        for (int i = 0; i < count; i++) {
                            doubleArray[i] = Double.parseDouble(tokenizer.nextToken().replace(",", "."));
                        }

                        dataCache.getDoubleArrays().put(currentElement, doubleArray);

                        return new Text("");
                    }
                    case Int: {
                        final String normalizedText = Text.normalizeString(text);
                        if (normalizedText.length() == 0) {
                            return new Text("");
                        }
                        final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
                        final int[] intArray = new int[count];
                        int i = 0;
                        while (tokenizer.hasMoreTokens()) {
                            intArray[i++] = Integer.parseInt(tokenizer.nextToken());
                        }

                        dataCache.getIntArrays().put(currentElement, intArray);

                        return new Text("");
                    }
                    case P: {
                        list.clear();
                        final String normalizedText = Text.normalizeString(text);
                        if (normalizedText.length() == 0) {
                            return new Text("");
                        }
                        final StringTokenizer tokenizer = new StringTokenizer(normalizedText, " ");
                        while (tokenizer.hasMoreTokens()) {
                            list.add(tokenizer.nextToken());
                        }
                        final int listSize = list.size();
                        final int[] intArray = new int[listSize];
                        for (int i = 0; i < listSize; i++) {
                            intArray[i] = Integer.parseInt(list.get(i));
                        }

                        dataCache.getIntArrays().put(currentElement, intArray);

                        return new Text("");
                    }
                    default:
                        break;
                }
            } catch (final NoSuchElementException e) {
                throw new ColladaException("Number of values in collada array does not match its count attribute: "
                        + count, e);
            }
            return new Text(Text.normalizeString(text));
        }

        @Override
        public void setAttribute(final Element parent, final Attribute a) {
            if ("id".equals(a.getName())) {
                if (dataCache.getIdCache().containsKey(a.getValue())) {
                    logger.warning("id already exists in id cache: " + a.getValue());
                }
                dataCache.getIdCache().put(a.getValue(), parent);
            } else if ("sid".equals(a.getName())) {
                dataCache.getSidCache().put(a.getValue(), parent);
            } else if ("count".equals(a.getName())) {
                try {
                    count = a.getIntValue();
                } catch (final DataConversionException e) {
                    e.printStackTrace();
                }
            }

            super.setAttribute(parent, a);
        }

        @Override
        public Element element(final int line, final int col, final String name, final Namespace namespace) {
            currentElement = super.element(line, col, name);
            handleTypes(name);
            return currentElement;
        }

        @Override
        public Element element(final int line, final int col, final String name, final String prefix, final String uri) {
            currentElement = super.element(line, col, name);
            handleTypes(name);
            return currentElement;
        }

        @Override
        public Element element(final int line, final int col, final String name, final String uri) {
            currentElement = super.element(line, col, name);
            handleTypes(name);
            return currentElement;
        }

        @Override
        public Element element(final int line, final int col, final String name) {
            currentElement = super.element(line, col, name);
            handleTypes(name);
            return currentElement;
        }

        private void handleTypes(final String name) {
            if ("float_array".equals(name)) {
                bufferType = BufferType.Float;
            } else if ("double_array".equals(name)) {
                bufferType = BufferType.Double;
            } else if ("int_array".equals(name)) {
                bufferType = BufferType.Int;
            } else if ("p".equals(name)) {
                bufferType = BufferType.P;
            } else {
                bufferType = BufferType.None;
            }
        }
    }

    /**
     * Parse a numeric value. Commas are replaced by dot automaticly. Also handle special values : INF, -INF, NaN
     *
     * @param number
     *            string
     * @return float
     */
    public static float parseFloat(String candidate) {
        candidate = candidate.replace(',', '.');
        if (candidate.contains("-INF")) {
            return Float.NEGATIVE_INFINITY;
        } else if (candidate.contains("INF")) {
            return Float.POSITIVE_INFINITY;
        }
        return Float.parseFloat(candidate);
    }

    public boolean readExtra(final Element extra, final Object... params) {
        for (final ColladaExtraPlugin plugin : _extraPlugins) {
            if (plugin.processExtra(extra, params)) {
                return true;
            }
        }
        return false;
    }
}
