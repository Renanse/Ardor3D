/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.extension;

import java.io.IOException;

import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.FogState;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.export.CapsuleUtils;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * A Box made of textured quads that simulate having a sky, horizon and so forth around your scene. Either attach to a
 * camera node or update on each frame to set this skybox at the camera's position.
 */
public class Skybox extends Node {

    public enum Face {
        /** The +Z side of the skybox. */
        North,
        /** The -Z side of the skybox. */
        South,
        /** The -X side of the skybox. */
        East,
        /** The +X side of the skybox. */
        West,
        /** The +Y side of the skybox. */
        Up,
        /** The -Y side of the skybox. */
        Down;
    }

    private float _xExtent;
    private float _yExtent;
    private float _zExtent;

    private Quad[] _skyboxQuads;

    public Skybox() {}

    /**
     * Creates a new skybox. The size of the skybox and name is specified here. By default, no textures are set.
     * 
     * @param name
     *            The name of the skybox.
     * @param xExtent
     *            The x size of the skybox in both directions from the center.
     * @param yExtent
     *            The y size of the skybox in both directions from the center.
     * @param zExtent
     *            The z size of the skybox in both directions from the center.
     */
    public Skybox(final String name, final float xExtent, final float yExtent, final float zExtent) {
        super(name);

        _xExtent = xExtent;
        _yExtent = yExtent;
        _zExtent = zExtent;

        initialize();
    }

    /**
     * Set the texture to be displayed on the given face of the skybox. Replaces any existing texture on that face.
     * 
     * @param face
     *            the face to set
     * @param texture
     *            The texture for that side to assume.
     * @throws IllegalArgumentException
     *             if face is null.
     */
    public void setTexture(final Face face, final Texture texture) {
        if (face == null) {
            throw new IllegalArgumentException("Face can not be null.");
        }

        _skyboxQuads[face.ordinal()].clearRenderState(RenderState.StateType.Texture);
        setTexture(face, texture, 0);
    }

    /**
     * Set the texture to be displayed on the given side of the skybox. Only replaces the texture at the index specified
     * by textureUnit.
     * 
     * @param face
     *            the face to set
     * @param texture
     *            The texture for that side to assume.
     * @param textureUnit
     *            The texture unite of the given side's TextureState the texture will assume.
     */
    public void setTexture(final Face face, final Texture texture, final int textureUnit) {
        // Validate
        if (face == null) {
            throw new IllegalArgumentException("Face can not be null.");
        }

        TextureState ts = (TextureState) _skyboxQuads[face.ordinal()]
                .getLocalRenderState(RenderState.StateType.Texture);
        if (ts == null) {
            ts = new TextureState();
        }

        // Initialize the texture state
        ts.setTexture(texture, textureUnit);
        ts.setEnabled(true);

        texture.setWrap(WrapMode.EdgeClamp);

        // Set the texture to the quad
        _skyboxQuads[face.ordinal()].setRenderState(ts);

        return;
    }

    public Texture getTexture(final Face face) {
        if (face == null) {
            throw new IllegalArgumentException("Face can not be null.");
        }
        return ((TextureState) _skyboxQuads[face.ordinal()].getLocalRenderState(RenderState.StateType.Texture))
                .getTexture();
    }

    public void initialize() {

        // Skybox consists of 6 sides
        _skyboxQuads = new Quad[6];

        // Create each of the quads
        _skyboxQuads[Face.North.ordinal()] = new Quad("north", _xExtent * 2, _yExtent * 2);
        _skyboxQuads[Face.North.ordinal()].setRotation(new Matrix3().fromAngles(0, Math.toRadians(180), 0));
        _skyboxQuads[Face.North.ordinal()].setTranslation(new Vector3(0, 0, _zExtent));
        _skyboxQuads[Face.South.ordinal()] = new Quad("south", _xExtent * 2, _yExtent * 2);
        _skyboxQuads[Face.South.ordinal()].setTranslation(new Vector3(0, 0, -_zExtent));
        _skyboxQuads[Face.East.ordinal()] = new Quad("east", _zExtent * 2, _yExtent * 2);
        _skyboxQuads[Face.East.ordinal()].setRotation(new Matrix3().fromAngles(0, Math.toRadians(90), 0));
        _skyboxQuads[Face.East.ordinal()].setTranslation(new Vector3(-_xExtent, 0, 0));
        _skyboxQuads[Face.West.ordinal()] = new Quad("west", _zExtent * 2, _yExtent * 2);
        _skyboxQuads[Face.West.ordinal()].setRotation(new Matrix3().fromAngles(0, Math.toRadians(270), 0));
        _skyboxQuads[Face.West.ordinal()].setTranslation(new Vector3(_xExtent, 0, 0));
        _skyboxQuads[Face.Up.ordinal()] = new Quad("up", _xExtent * 2, _zExtent * 2);
        _skyboxQuads[Face.Up.ordinal()]
                .setRotation(new Matrix3().fromAngles(Math.toRadians(90), Math.toRadians(270), 0));
        _skyboxQuads[Face.Up.ordinal()].setTranslation(new Vector3(0, _yExtent, 0));
        _skyboxQuads[Face.Down.ordinal()] = new Quad("down", _xExtent * 2, _zExtent * 2);
        _skyboxQuads[Face.Down.ordinal()].setRotation(new Matrix3().fromAngles(Math.toRadians(270),
                Math.toRadians(270), 0));
        _skyboxQuads[Face.Down.ordinal()].setTranslation(new Vector3(0, -_yExtent, 0));

        // We don't want the light to effect our skybox
        getSceneHints().setLightCombineMode(LightCombineMode.Off);

        getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);

        final ZBufferState zbuff = new ZBufferState();
        zbuff.setEnabled(false);
        setRenderState(zbuff);

        final FogState fs = new FogState();
        fs.setEnabled(false);
        setRenderState(fs);

        // We don't want it making our skybox disapear, so force view
        getSceneHints().setCullHint(CullHint.Never);

        for (int i = 0; i < 6; i++) {
            // Make sure texture is only what is set.
            _skyboxQuads[i].getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);

            // Make sure no lighting on the skybox
            _skyboxQuads[i].getSceneHints().setLightCombineMode(LightCombineMode.Off);

            // Make sure the quad is viewable
            _skyboxQuads[i].getSceneHints().setCullHint(CullHint.Never);

            // Add to the prebucket.
            _skyboxQuads[i].getSceneHints().setRenderBucketType(RenderBucketType.PreBucket);

            // And attach the skybox as a child
            attachChild(_skyboxQuads[i]);
        }
    }

    /**
     * Retrieve the quad indicated by the given side.
     * 
     * @param face
     *            One of Skybox.Face.North, Skybox.Face.South, and so on...
     * @return The Quad that makes up that side of the Skybox.
     */
    public Quad getFace(final Face face) {
        return _skyboxQuads[face.ordinal()];
    }

    public void preloadTexture(final Face face, final Renderer r) {
        final TextureState ts = (TextureState) _skyboxQuads[face.ordinal()]
                .getLocalRenderState(RenderState.StateType.Texture);
        if (ts != null) {
            r.applyState(StateType.Texture, ts);
        }
    }

    /**
     * Force all of the textures to load. This prevents pauses later during the application as you pan around the world.
     */
    public void preloadTextures(final Renderer r) {
        for (int x = 0; x < 6; x++) {
            final TextureState ts = (TextureState) _skyboxQuads[x].getLocalRenderState(RenderState.StateType.Texture);
            if (ts != null) {
                r.applyState(StateType.Texture, ts);
            }
        }

    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_xExtent, "xExtent", 0);
        capsule.write(_yExtent, "yExtent", 0);
        capsule.write(_zExtent, "zExtent", 0);
        capsule.write(_skyboxQuads, "skyboxQuads", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _xExtent = capsule.readFloat("xExtent", 0);
        _yExtent = capsule.readFloat("yExtent", 0);
        _zExtent = capsule.readFloat("zExtent", 0);
        _skyboxQuads = CapsuleUtils.asArray(capsule.readSavableArray("skyboxQuads", null), Quad.class);
    }
}