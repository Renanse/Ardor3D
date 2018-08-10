/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.hint;

import java.io.IOException;
import java.util.EnumSet;

import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.export.Savable;

/**
 * SceneHints encapsulates various rendering and interaction preferences for a scene object.
 */
public class SceneHints implements Savable {

    /**
     * A flag indicating how normals should be treated by the renderer.
     */
    protected NormalsMode _normalsMode = NormalsMode.Inherit;

    /**
     * A flag indicating if scene culling should be done on this object by inheritance, dynamically, never, or always.
     */
    protected CullHint _cullHint = CullHint.Inherit;

    /**
     * Flag signaling how lights are combined for this node. By default set to INHERIT.
     */
    protected LightCombineMode _lightCombineMode = LightCombineMode.Inherit;

    /**
     * Flag signaling how textures are combined for this node. By default set to INHERIT.
     */
    protected TextureCombineMode _textureCombineMode = TextureCombineMode.Inherit;

    /**
     * RenderBucketType for this spatial
     */
    protected RenderBucketType _renderBucketType = RenderBucketType.Inherit;

    /**
     * Draw order to use when drawing in ortho mode.
     */
    protected int _orthoOrder = 0;

    /**
     * Field for setting whether the Spatial is enabled for Picking or Collision. Both are enabled by default.
     */
    protected final EnumSet<PickingHint> _pickingHints = EnumSet.allOf(PickingHint.class);

    /**
     * The source for this SceneHints.
     */
    private final Hintable _source;

    /**
     * Type of transparency to do.
     */
    private TransparencyType _transpType = TransparencyType.Inherit;

    /**
     * Hint for shadow implementations
     */
    protected boolean _castsShadows = true;

    public SceneHints(final Hintable source) {
        _source = source;
    }

    public void set(final SceneHints sceneHints) {
        _normalsMode = sceneHints._normalsMode;
        _cullHint = sceneHints._cullHint;
        _lightCombineMode = sceneHints._lightCombineMode;
        _textureCombineMode = sceneHints._textureCombineMode;
        _renderBucketType = sceneHints._renderBucketType;
        _orthoOrder = sceneHints._orthoOrder;
        _pickingHints.clear();
        _pickingHints.addAll(sceneHints._pickingHints);
        _castsShadows = sceneHints._castsShadows;
        _transpType = sceneHints._transpType;
    }

    /**
     * Returns the normals mode. If the mode is set to inherit, then we get its normals mode from the given source's
     * hintable parent. If no parent, we'll default to NormalizeIfScaled.
     *
     * @return The normals mode to use.
     */
    public NormalsMode getNormalsMode() {
        if (_normalsMode != NormalsMode.Inherit) {
            return _normalsMode;
        }

        final Hintable parent = _source.getParentHintable();
        if (parent != null) {
            return parent.getSceneHints().getNormalsMode();
        }

        return NormalsMode.NormalizeIfScaled;
    }

    /**
     * @return the exact normals mode set.
     */
    public NormalsMode getLocalNormalsMode() {
        return _normalsMode;
    }

    /**
     * @param mode
     *            the new normals mode to set on this SceneHints
     */
    public void setNormalsMode(final NormalsMode mode) {
        _normalsMode = mode;
    }

    /**
     * @see #setCullHint(CullHint)
     * @return the cull mode of this spatial, or if set to INHERIT, the cullmode of its parent.
     */
    public CullHint getCullHint() {
        if (_cullHint != CullHint.Inherit) {
            return _cullHint;
        }

        final Hintable parent = _source.getParentHintable();
        if (parent != null) {
            return parent.getSceneHints().getCullHint();
        }

        return CullHint.Dynamic;
    }

    /**
     * @return the cullmode set on this Spatial
     */
    public CullHint getLocalCullHint() {
        return _cullHint;
    }

    /**
     * <code>setCullHint</code> sets how scene culling should work on this spatial during drawing. CullHint.Dynamic:
     * Determine via the defined Camera planes whether or not this Spatial should be culled. CullHint.Always: Always
     * throw away this object and any children during draw commands. CullHint.Never: Never throw away this object
     * (always draw it) CullHint.Inherit: Look for a non-inherit parent and use its cull mode. NOTE: You must set this
     * AFTER attaching to a parent or it will be reset with the parent's cullMode value.
     *
     * @param hint
     *            one of CullHint.Dynamic, CullHint.Always, CullHint.Inherit or CullHint.Never
     */
    public void setCullHint(final CullHint hint) {
        _cullHint = hint;
    }

    /**
     * Returns this spatial's texture combine mode. If the mode is set to inherit, then the spatial gets its combine
     * mode from its parent.
     *
     * @return The spatial's texture current combine mode.
     */
    public TextureCombineMode getTextureCombineMode() {
        if (_textureCombineMode != TextureCombineMode.Inherit) {
            return _textureCombineMode;
        }

        final Hintable parent = _source.getParentHintable();
        if (parent != null) {
            return parent.getSceneHints().getTextureCombineMode();
        }

        return TextureCombineMode.CombineClosest;
    }

    /**
     * @return the textureCombineMode set on this Spatial
     */
    public TextureCombineMode getLocalTextureCombineMode() {
        return _textureCombineMode;
    }

    /**
     * Sets how textures from parents should be combined for this Spatial.
     *
     * @param mode
     *            The new texture combine mode for this spatial.
     * @throws IllegalArgumentException
     *             if mode is null
     */
    public void setTextureCombineMode(final TextureCombineMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        }
        _textureCombineMode = mode;
    }

    /**
     * Returns this spatial's light combine mode. If the mode is set to inherit, then the spatial gets its combine mode
     * from its parent.
     *
     * @return The spatial's light current combine mode.
     */
    public LightCombineMode getLightCombineMode() {
        if (_lightCombineMode != LightCombineMode.Inherit) {
            return _lightCombineMode;
        }

        final Hintable parent = _source.getParentHintable();
        if (parent != null) {
            return parent.getSceneHints().getLightCombineMode();
        }

        return LightCombineMode.CombineFirst;
    }

    /**
     * @return the lightCombineMode set on this Spatial
     */
    public LightCombineMode getLocalLightCombineMode() {
        return _lightCombineMode;
    }

    /**
     * Sets how lights from parents should be combined for this spatial.
     *
     * @param mode
     *            The light combine mode for this spatial
     * @throws IllegalArgumentException
     *             if mode is null
     */
    public void setLightCombineMode(final LightCombineMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode can not be null.");
        }
        _lightCombineMode = mode;
    }

    /**
     * Get the render bucket type used to determine which "phase" of the rendering process this Spatial will rendered
     * in.
     * <p>
     * This method returns the effective bucket type that is used for rendering. If the type is set to
     * {@link com.ardor3d.renderer.queue.RenderBucketType#Inherit Inherit} then the bucket type from the spatial's
     * parent will be used during rendering. If no parent, then
     * {@link com.ardor3d.renderer.queue.RenderBucketType#Opaque Opaque} is used.
     *
     * @return the render queue mode used for this spatial.
     * @see com.ardor3d.renderer.queue.RenderBucketType
     */
    public RenderBucketType getRenderBucketType() {
        if (_renderBucketType != RenderBucketType.Inherit) {
            return _renderBucketType;
        }

        final Hintable parent = _source.getParentHintable();
        if (parent != null) {
            return parent.getSceneHints().getRenderBucketType();
        }

        return RenderBucketType.Opaque;
    }

    /**
     * Get the render bucket type used to determine which "phase" of the rendering process this Spatial will rendered
     * in.
     * <p>
     * This method returns the actual bucket type that is set on this spatial, if the type is set to
     * {@link com.ardor3d.renderer.queue.RenderBucketType#Inherit Inherit} then the bucket type from the spatial's
     * parent will be used during rendering. If no parent, then
     * {@link com.ardor3d.renderer.queue.RenderBucketType#Opaque Opaque} is used.
     *
     * @return the render queue mode set on this spatial.
     * @see com.ardor3d.renderer.queue.RenderBucketType
     */
    public RenderBucketType getLocalRenderBucketType() {
        return _renderBucketType;
    }

    /**
     * Set the render bucket type used to determine which "phase" of the rendering process this Spatial will rendered
     * in.
     *
     * @param renderBucketType
     *            the render bucket type to use for this spatial.
     * @see com.ardor3d.renderer.queue.RenderBucketType
     */
    public void setRenderBucketType(final RenderBucketType renderBucketType) {
        _renderBucketType = renderBucketType;
    }

    /**
     * Returns whether a certain pick hint is set on this spatial.
     *
     * @param pickingHint
     *            Pick hint to test for
     * @return Enabled or disabled
     */
    public boolean isPickingHintEnabled(final PickingHint pickingHint) {
        return _pickingHints.contains(pickingHint);
    }

    /**
     * Enable or disable a picking hint for this Spatial
     *
     * @param pickingHint
     *            PickingHint to set. Pickable or Collidable
     * @param enabled
     *            Enable or disable
     */
    public void setPickingHint(final PickingHint pickingHint, final boolean enabled) {
        if (enabled) {
            _pickingHints.add(pickingHint);
        } else {
            _pickingHints.remove(pickingHint);
        }
    }

    /**
     * Enable or disable all picking hints for this Spatial
     *
     * @param enabled
     *            Enable or disable
     */
    public void setAllPickingHints(final boolean enabled) {
        if (enabled) {
            _pickingHints.addAll(EnumSet.allOf(PickingHint.class));
        } else {
            _pickingHints.clear();
        }
    }

    /**
     * @return a number representing z ordering when used in the Ortho bucket. Higher values are
     *         "further into the screen" and lower values are "closer". Or in other words, if you draw two quads, one
     *         with a zorder of 1 and the other with a zorder of 2, the quad with zorder of 2 will be "under" the other
     *         quad.
     */
    public int getOrthoOrder() {
        return _orthoOrder;
    }

    /**
     * @param orthoOrder
     */
    public void setOrthoOrder(final int orthoOrder) {
        _orthoOrder = orthoOrder;
    }

    /**
     * Returns the transparency rendering type. If the mode is set to inherit, then we get its type from the given
     * source's hintable parent. If no parent, we'll default to OnePass.
     *
     * @return The transparency rendering type to use.
     */
    public TransparencyType getTransparencyType() {
        if (_transpType != TransparencyType.Inherit) {
            return _transpType;
        }

        final Hintable parent = _source.getParentHintable();
        if (parent != null) {
            return parent.getSceneHints().getTransparencyType();
        }

        return TransparencyType.OnePass;
    }

    /**
     * @return the exact transparency rendering type set.
     */
    public TransparencyType getLocalTransparencyType() {
        return _transpType;
    }

    /**
     * @param type
     *            the new transparency rendering type to set on this SceneHints
     */
    public void setTransparencyType(final TransparencyType type) {
        _transpType = type;
    }

    /**
     * @return true if this object should cast shadows
     */
    public boolean isCastsShadows() {
        return _castsShadows;
    }

    /**
     * @param castsShadows
     *            set if this object should cast shadows
     */
    public void setCastsShadows(final boolean castsShadows) {
        _castsShadows = castsShadows;
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    public Class<? extends SceneHints> getClassTag() {
        return this.getClass();
    }

    public void read(final InputCapsule capsule) throws IOException {
        _orthoOrder = capsule.readInt("orthoOrder", 0);
        _cullHint = capsule.readEnum("cullMode", CullHint.class, CullHint.Inherit);
        final String bucketTypeName = capsule.readString("renderBucketType", RenderBucketType.Inherit.name());
        _renderBucketType = RenderBucketType.getRenderBucketType(bucketTypeName);
        _lightCombineMode = capsule.readEnum("lightCombineMode", LightCombineMode.class, LightCombineMode.Inherit);
        _textureCombineMode = capsule.readEnum("textureCombineMode", TextureCombineMode.class,
                TextureCombineMode.Inherit);
        _normalsMode = capsule.readEnum("normalsMode", NormalsMode.class, NormalsMode.Inherit);
        _transpType = capsule.readEnum("transpType", TransparencyType.class, TransparencyType.Inherit);
        _castsShadows = capsule.readBoolean("castsShadows", true);
        final PickingHint[] pickHints = capsule.readEnumArray("pickingHints", PickingHint.class, null);
        _pickingHints.clear();
        if (pickHints != null) {
            for (final PickingHint hint : pickHints) {
                _pickingHints.add(hint);
            }
        } else {
            // default is all values set.
            for (final PickingHint hint : PickingHint.values()) {
                _pickingHints.add(hint);
            }
        }
    }

    public void write(final OutputCapsule capsule) throws IOException {
        capsule.write(_orthoOrder, "orthoOrder", 0);
        capsule.write(_cullHint, "cullMode", CullHint.Inherit);
        capsule.write(_renderBucketType.name(), "renderBucketType", RenderBucketType.Inherit.name());
        capsule.write(_lightCombineMode, "lightCombineMode", LightCombineMode.Inherit);
        capsule.write(_textureCombineMode, "textureCombineMode", TextureCombineMode.Inherit);
        capsule.write(_normalsMode, "normalsMode", NormalsMode.Inherit);
        capsule.write(_pickingHints.toArray(new PickingHint[] {}), "pickingHints");
        capsule.write(_transpType, "transpType", TransparencyType.Inherit);
        capsule.write(_castsShadows, "castsShadows", true);
    }
}
