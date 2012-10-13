/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.model.collada.jdom;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.jdom.Element;

import com.ardor3d.extension.model.collada.jdom.data.DataCache;
import com.ardor3d.extension.model.collada.jdom.data.MaterialInfo;
import com.ardor3d.extension.model.collada.jdom.data.SamplerTypes;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.ApplyMode;
import com.ardor3d.image.Texture.CombinerFunctionRGB;
import com.ardor3d.image.Texture.CombinerOperandRGB;
import com.ardor3d.image.Texture.CombinerSource;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.MaterialState;
import com.ardor3d.renderer.state.MaterialState.MaterialFace;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocator;
import com.ardor3d.util.resource.ResourceSource;

/**
 * Methods for parsing Collada data related to materials.
 */
public class ColladaMaterialUtils {
    private static final Logger logger = Logger.getLogger(ColladaMaterialUtils.class.getName());

    private final boolean _loadTextures;
    private final DataCache _dataCache;
    private final ColladaDOMUtil _colladaDOMUtil;
    private final ResourceLocator _textureLocator;
    private final boolean _compressTextures;
    private final boolean _flipTransparency;

    public ColladaMaterialUtils(final boolean loadTextures, final DataCache dataCache,
            final ColladaDOMUtil colladaDOMUtil, final ResourceLocator textureLocator, final boolean compressTextures,
            final boolean flipTransparency) {
        _loadTextures = loadTextures;
        _dataCache = dataCache;
        _colladaDOMUtil = colladaDOMUtil;
        _textureLocator = textureLocator;
        _compressTextures = compressTextures;
        _flipTransparency = flipTransparency;
    }

    /**
     * Find and apply the given material to the given Mesh.
     * 
     * @param materialName
     *            our material name
     * @param mesh
     *            the mesh to apply material to.
     */
    public void applyMaterial(final String materialName, final Mesh mesh) {
        if (materialName == null) {
            logger.warning("materialName is null");
            return;
        }

        Element mat = _dataCache.getBoundMaterial(materialName);
        if (mat == null) {
            logger.warning("material not bound: " + materialName + ", trying search with id.");
            mat = _colladaDOMUtil.findTargetWithId(materialName);
        }
        if (mat == null || !"material".equals(mat.getName())) {
            logger.warning("material not found: " + materialName);
            return;
        }

        final String originalMaterial = mat.getAttributeValue("id");
        MaterialInfo mInfo = null;
        if (!_dataCache.getMaterialInfoMap().containsKey(originalMaterial)) {
            mInfo = new MaterialInfo();
            mInfo.setMaterialName(originalMaterial);
            _dataCache.getMaterialInfoMap().put(originalMaterial, mInfo);
        }
        _dataCache.getMeshMaterialMap().put(mesh, originalMaterial);

        final Element child = mat.getChild("instance_effect");
        final Element effectNode = _colladaDOMUtil.findTargetWithId(child.getAttributeValue("url"));
        if (effectNode == null) {
            logger.warning("material effect not found: " + mat.getChild("instance_material").getAttributeValue("url"));
            return;
        }

        if ("effect".equals(effectNode.getName())) {
            /*
             * temp cache for textures, we do not want to add textures twice (for example, transparant map might point
             * to diffuse texture)
             */
            final HashMap<String, Texture> loadedTextures = new HashMap<String, Texture>();
            final Element effect = effectNode;
            // XXX: For now, just grab the common technique:
            if (effect.getChild("profile_COMMON") != null) {
                if (mInfo != null) {
                    mInfo.setProfile("COMMON");
                }
                final Element technique = effect.getChild("profile_COMMON").getChild("technique");
                String type = "blinn";
                if (technique.getChild(type) == null) {
                    type = "phong";
                    if (technique.getChild(type) == null) {
                        type = "lambert";
                        if (technique.getChild(type) == null) {
                            type = "constant";
                            if (technique.getChild(type) == null) {
                                ColladaMaterialUtils.logger.warning("COMMON material has unusuable techinque. "
                                        + child.getAttributeValue("url"));
                                return;
                            }
                        }
                    }
                }
                final Element blinnPhongLambert = technique.getChild(type);
                if (mInfo != null) {
                    mInfo.setTechnique(type);
                }
                final MaterialState mState = new MaterialState();

                // TODO: implement proper transparency handling
                Texture diffuseTexture = null;
                ColorRGBA transparent = new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f);
                float transparency = 1.0f;
                boolean useTransparency = false;
                String opaqueMode = "A_ONE";

                /*
                 * place holder for current property, we import material properties in fixed order (for texture order)
                 */
                Element property = null;
                /* Diffuse property */
                property = blinnPhongLambert.getChild("diffuse");
                if (property != null) {
                    final Element propertyValue = (Element) property.getChildren().get(0);
                    if ("color".equals(propertyValue.getName())) {
                        final ColorRGBA color = _colladaDOMUtil.getColor(propertyValue.getText());
                        mState.setDiffuse(MaterialFace.FrontAndBack, color);
                    } else if ("texture".equals(propertyValue.getName()) && _loadTextures) {
                        diffuseTexture = populateTextureState(mesh, propertyValue, effect, loadedTextures, mInfo,
                                "diffuse");
                    }
                }
                /* Ambient property */
                property = blinnPhongLambert.getChild("ambient");
                if (property != null) {
                    final Element propertyValue = (Element) property.getChildren().get(0);
                    if ("color".equals(propertyValue.getName())) {
                        final ColorRGBA color = _colladaDOMUtil.getColor(propertyValue.getText());
                        mState.setAmbient(MaterialFace.FrontAndBack, color);
                    } else if ("texture".equals(propertyValue.getName()) && _loadTextures) {
                        populateTextureState(mesh, propertyValue, effect, loadedTextures, mInfo, "ambient");
                    }
                }
                /* Transparent property */
                property = blinnPhongLambert.getChild("transparent");
                if (property != null) {
                    final Element propertyValue = (Element) property.getChildren().get(0);
                    if ("color".equals(propertyValue.getName())) {
                        transparent = _colladaDOMUtil.getColor(propertyValue.getText());
                        // TODO: use this
                        useTransparency = true;
                    } else if ("texture".equals(propertyValue.getName()) && _loadTextures) {
                        populateTextureState(mesh, propertyValue, effect, loadedTextures, mInfo, "transparent");
                    }
                    opaqueMode = property.getAttributeValue("opaque", "A_ONE");
                }
                /* Transparency property */
                property = blinnPhongLambert.getChild("transparency");
                if (property != null) {
                    final Element propertyValue = (Element) property.getChildren().get(0);
                    if ("float".equals(propertyValue.getName())) {
                        transparency = Float.parseFloat(propertyValue.getText().replace(",", "."));
                        // TODO: use this
                        if (_flipTransparency) {
                            transparency = 1f - transparency;
                        }
                        useTransparency = true;
                    } else if ("texture".equals(propertyValue.getName()) && _loadTextures) {
                        populateTextureState(mesh, propertyValue, effect, loadedTextures, mInfo, "transparency");
                    }
                }
                /* Emission property */
                property = blinnPhongLambert.getChild("emission");
                if (property != null) {
                    final Element propertyValue = (Element) property.getChildren().get(0);
                    if ("color".equals(propertyValue.getName())) {
                        mState.setEmissive(MaterialFace.FrontAndBack, _colladaDOMUtil.getColor(propertyValue.getText()));
                    } else if ("texture".equals(propertyValue.getName()) && _loadTextures) {
                        populateTextureState(mesh, propertyValue, effect, loadedTextures, mInfo, "emissive");
                    }
                }
                /* Specular property */
                property = blinnPhongLambert.getChild("specular");
                if (property != null) {
                    final Element propertyValue = (Element) property.getChildren().get(0);
                    if ("color".equals(propertyValue.getName())) {
                        mState.setSpecular(MaterialFace.FrontAndBack, _colladaDOMUtil.getColor(propertyValue.getText()));
                    } else if ("texture".equals(propertyValue.getName()) && _loadTextures) {
                        populateTextureState(mesh, propertyValue, effect, loadedTextures, mInfo, "specular");
                    }
                }
                /* Shininess property */
                property = blinnPhongLambert.getChild("shininess");
                if (property != null) {
                    final Element propertyValue = (Element) property.getChildren().get(0);
                    if ("float".equals(propertyValue.getName())) {
                        float shininess = Float.parseFloat(propertyValue.getText().replace(",", "."));
                        if (shininess >= 0.0f && shininess <= 1.0f) {
                            final float oldShininess = shininess;
                            shininess *= 128;
                            logger.finest("Shininess - " + oldShininess
                                    + " - was in the [0,1] range. Scaling to [0, 128] - " + shininess);
                        } else if (shininess < 0 || shininess > 128) {
                            final float oldShininess = shininess;
                            shininess = MathUtils.clamp(shininess, 0, 128);
                            logger.warning("Shininess must be between 0 and 128. Shininess " + oldShininess
                                    + " was clamped to " + shininess);
                        }
                        mState.setShininess(MaterialFace.FrontAndBack, shininess);
                    } else if ("texture".equals(propertyValue.getName()) && _loadTextures) {
                        populateTextureState(mesh, propertyValue, effect, loadedTextures, mInfo, "shininess");
                    }
                }
                /* Reflectivity property */
                float reflectivity = 1.0f;
                property = blinnPhongLambert.getChild("reflectivity");
                if (property != null) {
                    final Element propertyValue = (Element) property.getChildren().get(0);
                    if ("float".equals(propertyValue.getName())) {
                        reflectivity = Float.parseFloat(propertyValue.getText().replace(",", "."));
                    }
                }
                /* Reflective property. Texture only */
                property = blinnPhongLambert.getChild("reflective");
                if (property != null) {
                    final Element propertyValue = (Element) property.getChildren().get(0);
                    if ("texture".equals(propertyValue.getName()) && _loadTextures) {
                        final Texture reflectiveTexture = populateTextureState(mesh, propertyValue, effect,
                                loadedTextures, mInfo, "reflective");

                        reflectiveTexture.setEnvironmentalMapMode(Texture.EnvironmentalMapMode.SphereMap);
                        reflectiveTexture.setApply(ApplyMode.Combine);

                        reflectiveTexture.setCombineFuncRGB(CombinerFunctionRGB.Interpolate);
                        // color 1
                        reflectiveTexture.setCombineSrc0RGB(CombinerSource.CurrentTexture);
                        reflectiveTexture.setCombineOp0RGB(CombinerOperandRGB.SourceColor);
                        // color 2
                        reflectiveTexture.setCombineSrc1RGB(CombinerSource.Previous);
                        reflectiveTexture.setCombineOp1RGB(CombinerOperandRGB.SourceColor);
                        // interpolate param will come from alpha of constant color
                        reflectiveTexture.setCombineSrc2RGB(CombinerSource.Constant);
                        reflectiveTexture.setCombineOp2RGB(CombinerOperandRGB.SourceAlpha);

                        reflectiveTexture.setConstantColor(new ColorRGBA(1, 1, 1, reflectivity));
                    }
                }

                /*
                 * An extra tag defines some materials not part of the collada standard. Since we're not able to parse
                 * we simply extract the textures from the element (such that shaders etc can at least pick up on them)
                 */
                property = technique.getChild("extra");
                if (property != null) {
                    getTexturesFromElement(mesh, property, effect, loadedTextures, mInfo);
                }

                // XXX: There are some issues with clarity on how to use alpha blending in OpenGL FFP.
                // The best interpretation I have seen is that if transparent has a texture == diffuse,
                // Turn on alpha blending and use diffuse alpha.

                // check to make sure we actually need this.
                // testing separately for a transparency of 0.0 is to hack around erroneous exports, since usually
                // there is no use in exporting something with 100% transparency.
                if ("A_ONE".equals(opaqueMode) && ColorRGBA.WHITE.equals(transparent) && transparency == 1.0
                        || transparency == 0.0) {
                    useTransparency = false;
                }

                if (useTransparency) {
                    if (diffuseTexture != null) {
                        final BlendState blend = new BlendState();
                        blend.setBlendEnabled(true);
                        blend.setTestEnabled(true);
                        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
                        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
                        mesh.setRenderState(blend);
                    } else {
                        final BlendState blend = new BlendState();
                        blend.setBlendEnabled(true);
                        blend.setTestEnabled(true);
                        transparent.setAlpha(transparent.getAlpha() * transparency);
                        blend.setConstantColor(transparent);
                        blend.setSourceFunction(BlendState.SourceFunction.ConstantAlpha);
                        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusConstantAlpha);
                        mesh.setRenderState(blend);
                    }

                    mesh.getSceneHints().setRenderBucketType(RenderBucketType.Transparent);
                }

                if (mInfo != null) {
                    if (useTransparency) {
                        mInfo.setUseTransparency(useTransparency);
                        if (diffuseTexture == null) {
                            mInfo.setTransparency(transparent.getAlpha() * transparency);
                        }
                    }
                    mInfo.setMaterialState(mState);
                }
                mesh.setRenderState(mState);
            }
        } else {
            ColladaMaterialUtils.logger.warning("material effect not found: "
                    + mat.getChild("instance_material").getAttributeValue("url"));
        }
    }

    /**
     * Function to searches an xml node for <texture> elements and adds them to the texture state of the mesh.
     * 
     * @param mesh
     *            the Ardor3D Mesh to add the Texture to.
     * @param element
     *            the xml element to start the search on
     * @param effect
     *            our <instance_effect> element
     * @param info
     */
    private void getTexturesFromElement(final Mesh mesh, final Element element, final Element effect,
            final HashMap<String, Texture> loadedTextures, final MaterialInfo info) {
        if ("texture".equals(element.getName()) && _loadTextures) {
            populateTextureState(mesh, element, effect, loadedTextures, info, null);
        }
        @SuppressWarnings("unchecked")
        final List<Element> children = element.getChildren();
        if (children != null) {
            for (final Element child : children) {
                /* recurse on children */
                getTexturesFromElement(mesh, child, effect, loadedTextures, info);
            }
        }
    }

    /**
     * Convert a <texture> element to an Ardor3D representation and store in the given state.
     * 
     * @param mesh
     *            the Ardor3D Mesh to add the Texture to.
     * @param daeTexture
     *            our <texture> element
     * @param effect
     *            our <instance_effect> element
     * @return the created Texture.
     */
    private Texture populateTextureState(final Mesh mesh, final Element daeTexture, final Element effect,
            final HashMap<String, Texture> loadedTextures, final MaterialInfo info, String textureSlot) {
        // TODO: Use vert data to determine which texcoords and set to use.
        // final String uvName = daeTexture.getAttributeValue("texcoord");
        TextureState tState = (TextureState) mesh.getLocalRenderState(RenderState.StateType.Texture);
        if (tState == null) {
            tState = new TextureState();
            mesh.setRenderState(tState);
        }

        // Use texture attrib to find correct sampler
        final String textureReference = daeTexture.getAttributeValue("texture");
        if (textureSlot == null) {
            // if we have no texture slot defined (like in the case of an "extra" texture), we'll use the
            // textureReference.
            textureSlot = textureReference;
        }

        /* only add the texture to the state once */
        if (loadedTextures.containsKey(textureReference)) {
            final Texture tex = loadedTextures.get(textureReference);
            if (info != null) {
                info.setTextureSlot(textureSlot, textureReference, tex, null);
            }
            return tex;
        }

        Element node = _colladaDOMUtil.findTargetWithSid(textureReference);
        if (node == null) {
            // Not sure if this is quite right, but spec seems to indicate looking for global id
            node = _colladaDOMUtil.findTargetWithId("#" + textureReference);
        }

        if ("newparam".equals(node.getName())) {
            node = (Element) node.getChildren().get(0);
        }

        Element sampler = null;
        Element surface = null;
        Element image = null;

        Texture.MinificationFilter min = Texture.MinificationFilter.BilinearNoMipMaps;
        if ("sampler2D".equals(node.getName())) {
            sampler = node;
            if (sampler.getChild("minfilter") != null) {
                final String minfilter = sampler.getChild("minfilter").getText();
                min = Enum.valueOf(SamplerTypes.MinFilterType.class, minfilter).getArdor3dFilter();
            }
            // Use sampler to get correct surface
            node = _colladaDOMUtil.findTargetWithSid(sampler.getChild("source").getText());
            // node = resolveSid(effect, sampler.getSource());
        }

        if ("newparam".equals(node.getName())) {
            node = (Element) node.getChildren().get(0);
        }

        if ("surface".equals(node.getName())) {
            surface = node;
            // image(s) will come from surface.
        } else if ("image".equals(node.getName())) {
            image = node;
        }

        // Ok, a few possibilities here...
        Texture texture = null;
        String fileName = null;
        if (surface == null && image != null) {
            // Only an image found (no sampler). Assume 2d texture. Load.
            fileName = image.getChild("init_from").getText();
            texture = loadTexture2D(fileName, min);
        } else if (surface != null) {
            // We have a surface, pull images from that.
            if ("2D".equals(surface.getAttributeValue("type"))) {
                // look for an init_from with lowest mip and use that. (usually 0)

                // TODO: mip?
                final Element lowest = (Element) surface.getChildren("init_from").get(0);
                // Element lowest = null;
                // for (final Element i : (List<Element>) surface.getChildren("init_from")) {
                // if (lowest == null || lowest.getMip() > i.getMip()) {
                // lowest = i;
                // }
                // }

                if (lowest == null) {
                    logger.warning("surface given with no usable init_from: " + surface);
                    return null;
                }

                image = _colladaDOMUtil.findTargetWithId("#" + lowest.getText());
                // image = (DaeImage) root.resolveUrl("#" + lowest.getValue());
                if (image != null) {
                    fileName = image.getChild("init_from").getText();
                    texture = loadTexture2D(fileName, min);
                }

                // TODO: add support for mip map levels other than 0.
            }
            // TODO: add support for the other texture types.
        } else {
            // No surface OR image... warn.
            logger.warning("texture given with no matching <sampler*> or <image> found.");
            if (info != null) {
                info.setTextureSlot(textureSlot, textureReference, null, null);
            }
            return null;
        }

        if (texture != null) {
            if (sampler != null) {
                // Apply params from our sampler.
                applySampler(sampler, texture);
            }

            // Add to texture state.
            tState.setTexture(texture, tState.getNumberOfSetTextures());
            loadedTextures.put(textureReference, texture);
            if (info != null) {
                info.setTextureSlot(textureSlot, textureReference, texture, fileName);
            }
        } else {
            logger.warning("unable to load texture: " + daeTexture);
            if (info != null) {
                info.setTextureSlot(textureSlot, textureReference, null, fileName);
            }
        }

        return texture;
    }

    private void applySampler(final Element sampler, final Texture texture) {
        if (sampler.getChild("minfilter") != null) {
            final String minfilter = sampler.getChild("minfilter").getText();
            texture.setMinificationFilter(Enum.valueOf(SamplerTypes.MinFilterType.class, minfilter).getArdor3dFilter());
        }
        if (sampler.getChild("magfilter") != null) {
            final String magfilter = sampler.getChild("magfilter").getText();
            texture.setMagnificationFilter(Enum.valueOf(SamplerTypes.MagFilterType.class, magfilter).getArdor3dFilter());
        }
        if (sampler.getChild("wrap_s") != null) {
            final String wrapS = sampler.getChild("wrap_s").getText();
            texture.setWrap(Texture.WrapAxis.S, Enum.valueOf(SamplerTypes.WrapModeType.class, wrapS)
                    .getArdor3dWrapMode());
        }
        if (sampler.getChild("wrap_t") != null) {
            final String wrapT = sampler.getChild("wrap_t").getText();
            texture.setWrap(Texture.WrapAxis.T, Enum.valueOf(SamplerTypes.WrapModeType.class, wrapT)
                    .getArdor3dWrapMode());
        }
        if (sampler.getChild("border_color") != null) {
            texture.setBorderColor(_colladaDOMUtil.getColor(sampler.getChild("border_color").getText()));
        }
    }

    @SuppressWarnings("unchecked")
    public void bindMaterials(final Element bindMaterial) {
        if (bindMaterial == null || bindMaterial.getChildren().isEmpty()) {
            return;
        }

        for (final Element instance : (List<Element>) bindMaterial.getChild("technique_common").getChildren(
                "instance_material")) {
            final Element matNode = _colladaDOMUtil.findTargetWithId(instance.getAttributeValue("target"));
            if (matNode != null && "material".equals(matNode.getName())) {
                _dataCache.bindMaterial(instance.getAttributeValue("symbol"), matNode);
            } else {
                logger.warning("instance material target not found: " + instance.getAttributeValue("target"));
            }

            // TODO: need to store bound vert data as local data. (also unstore on unbind.)
        }
    }

    @SuppressWarnings("unchecked")
    public void unbindMaterials(final Element bindMaterial) {
        if (bindMaterial == null || bindMaterial.getChildren().isEmpty()) {
            return;
        }
        for (final Element instance : (List<Element>) bindMaterial.getChild("technique_common").getChildren(
                "instance_material")) {
            _dataCache.unbindMaterial(instance.getAttributeValue("symbol"));
        }
    }

    private Texture loadTexture2D(final String path, final Texture.MinificationFilter minFilter) {
        if (_dataCache.containsTexture(path)) {
            return _dataCache.getTexture(path);
        }

        final Texture texture;
        if (_textureLocator == null) {
            texture = TextureManager.load(path, minFilter, _compressTextures ? TextureStoreFormat.GuessCompressedFormat
                    : TextureStoreFormat.GuessNoCompressedFormat, true);
        } else {
            final ResourceSource source = _textureLocator.locateResource(path);
            texture = TextureManager.load(source, minFilter,
                    _compressTextures ? TextureStoreFormat.GuessCompressedFormat
                            : TextureStoreFormat.GuessNoCompressedFormat, true);
        }
        _dataCache.addTexture(path, texture);

        return texture;
    }

}
