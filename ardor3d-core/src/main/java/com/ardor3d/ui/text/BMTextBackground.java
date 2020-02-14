/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.ui.text;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector4;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyVector2;
import com.ardor3d.math.type.ReadOnlyVector4;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.IndexBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * A 9-slice capable background, suitable for sitting behind BMText to increase readability.
 *
 * @author Mark B. Allan
 * @author Joshua Slack
 */
public class BMTextBackground extends Mesh implements BMTextChangeListener {

    final BMText _text;
    ColorRGBA _tintColor = new ColorRGBA(ColorRGBA.WHITE);

    Vector4 _textureBorderOffsets = new Vector4();
    Vector4 _contentPadding = new Vector4();
    float _borderScale = 1f;

    transient Texture _texture;

    public BMTextBackground(final String name, final BMText text, final Texture texture) {
        this(name, text, texture, new Vector4(0.25, 0.25, 0.25, 0.25));
    }

    public BMTextBackground(final String name, final BMText text, final Texture texture,
            final ReadOnlyVector4 textureBorderOffsets) {
        super(name);
        _text = text;
        _texture = texture;

        _textureBorderOffsets.set(textureBorderOffsets);
        setRenderStates(texture);

        // allocate vertex data
        _meshData.setVertexBuffer(BufferUtils.createVector3Buffer(16));
        _meshData.setTextureBuffer(BufferUtils.createVector2Buffer(16), 0);
        _meshData.setIndices(BufferUtils.createIndexBufferData(28, 15));

        // Set initial static data.
        setTextureData();
        setIndexData();

        setRenderMaterial("ui/textured/default_color.yaml");

        // -- add self as change listener
        _text.addChangeListener(this);
    }

    private void setRenderStates(final Texture texture) {
        final TextureState ts = new TextureState();
        ts.setTexture(texture);
        setRenderState(ts);

        final ZBufferState zs = new ZBufferState();
        zs.setWritable(false);
        setRenderState(zs);

        setDefaultColor(_tintColor);
        setModelBound(null);

        getSceneHints().setLightCombineMode(LightCombineMode.Off);
        getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
        getSceneHints().setCullHint(CullHint.Never);
    }

    private void setIndexData() {
        final IndexBufferData<?> indices = _meshData.getIndices();
        indices.getBuffer().rewind();
        indices.put(9).put(5).put(10).put(6);
        indices.put(4).put(0).put(5).put(1).put(6).put(2);
        indices.put(2).put(3).put(6).put(7).put(10).put(11);
        indices.put(11).put(15).put(10).put(14).put(9).put(13);
        indices.put(13).put(12).put(9).put(8).put(5).put(4);
        _meshData.setIndexLengths(new int[] { 4, 6, 6, 6, 6 });
        _meshData.setIndexMode(IndexMode.TriangleStrip);
        _meshData.markIndicesDirty();
    }

    private void setTextureData() {
        // x = left, y = top, z = right, w = bottom
        final FloatBuffer coords = _meshData.getTextureBuffer(0);
        coords.rewind();
        coords.put(0).put(0);
        coords.put(_textureBorderOffsets.getXf()).put(0);
        coords.put(1f - _textureBorderOffsets.getZf()).put(0);
        coords.put(1).put(0);

        coords.put(0).put(_textureBorderOffsets.getWf());
        coords.put(_textureBorderOffsets.getXf()).put(_textureBorderOffsets.getWf());
        coords.put(1f - _textureBorderOffsets.getZf()).put(_textureBorderOffsets.getWf());
        coords.put(1).put(_textureBorderOffsets.getWf());

        coords.put(0).put(1f - _textureBorderOffsets.getYf());
        coords.put(_textureBorderOffsets.getXf()).put(1f - _textureBorderOffsets.getYf());
        coords.put(1f - _textureBorderOffsets.getZf()).put(1f - _textureBorderOffsets.getYf());
        coords.put(1).put(1f - _textureBorderOffsets.getYf());

        coords.put(0).put(1);
        coords.put(_textureBorderOffsets.getXf()).put(1);
        coords.put(1f - _textureBorderOffsets.getZf()).put(1);
        coords.put(1).put(1);
        _meshData.markBufferDirty(MeshData.KEY_TextureCoords0);
    }

    public void setBackgroundColor(final ReadOnlyColorRGBA color) {
        _tintColor.set(color);
        textAlphaChanged(_text, _text.getDefaultColor().getAlpha());
    }

    /**
     * Sets the scale of the 9 slice border texture. By default this is set to 1 meaning we attempt to make it pixel
     * perfect. If the source texture is too large or small, leading to an undesired border size, changing this value
     * will adjust the border without scaling other transform values.
     */
    public void setBorderScale(final float scale) {
        _borderScale = scale;
        textSizeChanged(_text, _text.getSize());
    }

    public float getBorderScale() {
        return _borderScale;
    }

    /**
     * Splits up the texture drawn on this background into a grid. x,y,z, and w of the given vector are interpreted as
     * the u/v coordinate offset from the left, top, right and bottom edges of the texture respectively. This forms a 9
     * slice/patch grid. The edges of the grid are stretched along their given axis to fit. The corners are presented
     * without stretching their natural ratio. The center patch is stretched to fit on both axis. The default value is
     * (.25, .25, .25, .25) which reserves a 25% border around the center content.
     */
    public void setTexBorderOffsets(final ReadOnlyVector4 offsets) {
        setTexBorderOffsets(offsets.getXf(), offsets.getYf(), offsets.getZf(), offsets.getWf());
    }

    /**
     * Set the amount of texture uv space to reserve for the border for all sides to the same value.
     */
    public void setTexBorderOffsets(final float offset) {
        setTexBorderOffsets(offset, offset, offset, offset);
    }

    public void setTexBorderOffsets(final float x, final float y, final float z, final float w) {
        _textureBorderOffsets.set(x, y, z, w);
        setTextureData();
    }

    public ReadOnlyVector4 getTexBorderOffsets() {
        return _textureBorderOffsets;
    }

    /**
     * Set internal padding between text and border.
     */
    public void setContentPadding(final ReadOnlyVector4 padding) {
        setContentPadding(padding.getXf(), padding.getYf(), padding.getZf(), padding.getWf());
    }

    /**
     * Set internal padding between text and border.
     */
    public void setContentPadding(final float padding) {
        setContentPadding(padding, padding, padding, padding);
    }

    /**
     * Set internal padding between text and border.
     */
    public void setContentPadding(final float x, final float y, final float z, final float w) {
        _contentPadding.set(x, y, z, w);
        textSizeChanged(_text, _text.getSize());
    }

    public ReadOnlyVector4 getContentPadding() {
        return _contentPadding;
    }

    @Override
    public synchronized void draw(final Renderer r) {
        this.setWorldRotation(_text.getWorldRotation());
        this.setWorldTranslation(_text.getWorldTranslation());
        this.setWorldScale(_text.getWorldScale());
        super.draw(r);
    }

    @Override
    public void textSizeChanged(final BMText text, final ReadOnlyVector2 size) {
        final ReadOnlyVector2 fixedOffset = text.getFixedOffset();
        final BMText.Align align = text.getAlign();
        float x = size.getXf() * align.horizontal;
        float y = size.getYf() * align.vertical;
        x += fixedOffset.getX();
        y += fixedOffset.getY();
        float xs = x + size.getXf();
        float ys = y + size.getYf();

        x -= _contentPadding.getXf();
        ys += _contentPadding.getYf();
        xs += _contentPadding.getZf();
        y -= _contentPadding.getWf();

        float leftB = 0f, topB = 0f, rightB = 0f, bottomB = 0f;

        if (_texture != null && _texture.getImage() != null) {
            final Image img = _texture.getImage();
            leftB = img.getWidth() * _textureBorderOffsets.getXf() * _borderScale;
            rightB = img.getWidth() * _textureBorderOffsets.getZf() * _borderScale;
            topB = img.getHeight() * _textureBorderOffsets.getYf() * _borderScale;
            bottomB = img.getHeight() * _textureBorderOffsets.getWf() * _borderScale;
        }

        final FloatBuffer vertices = _meshData.getVertexBuffer();
        vertices.rewind();
        vertices.put(x - leftB).put(0).put(y - bottomB);
        vertices.put(x).put(0).put(y - bottomB);
        vertices.put(xs).put(0).put(y - bottomB);
        vertices.put(xs + rightB).put(0).put(y - bottomB);

        vertices.put(x - leftB).put(0).put(y);
        vertices.put(x).put(0).put(y);
        vertices.put(xs).put(0).put(y);
        vertices.put(xs + rightB).put(0).put(y);

        vertices.put(x - leftB).put(0).put(ys);
        vertices.put(x).put(0).put(ys);
        vertices.put(xs).put(0).put(ys);
        vertices.put(xs + rightB).put(0).put(ys);

        vertices.put(x - leftB).put(0).put(ys + topB);
        vertices.put(x).put(0).put(ys + topB);
        vertices.put(xs).put(0).put(ys + topB);
        vertices.put(xs + rightB).put(0).put(ys + topB);
        _meshData.markBufferDirty(MeshData.KEY_VertexCoords);
    }

    @Override
    public void textAlphaChanged(final BMText text, final float alpha) {
        setDefaultColor(_tintColor.getRed(), _tintColor.getGreen(), _tintColor.getBlue(),
                _tintColor.getAlpha() * alpha);
    }

    // /////////////////
    // Methods for Savable
    // /////////////////

    @Override
    public Class<? extends BMTextBackground> getClassTag() {
        return this.getClass();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_tintColor, "tintColor", (ColorRGBA) ColorRGBA.WHITE);
        capsule.write(_textureBorderOffsets, "borderCoords", new Vector4(0.25, 0.25, 0.25, 0.25));
        capsule.write(_contentPadding, "contentPadding", (Vector4) Vector4.ZERO);
        capsule.write(_borderScale, "borderScale", 1f);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _tintColor = capsule.readSavable("tintColor", (ColorRGBA) ColorRGBA.WHITE);
        _textureBorderOffsets = capsule.readSavable("borderCoords", new Vector4(0.25, 0.25, 0.25, 0.25));
        _contentPadding.set(capsule.readSavable("contentPadding", (Vector4) Vector4.ZERO));
        _borderScale = capsule.readFloat("borderScale", 1f);
        final TextureState ts = ((TextureState) getLocalRenderState(StateType.Texture));
        if (ts != null) {
            _texture = ts.getTexture();
        }
    }
}
