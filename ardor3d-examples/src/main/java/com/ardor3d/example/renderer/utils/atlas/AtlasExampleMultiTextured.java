/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer.utils.atlas;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.atlas.TexturePacker;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.WrapMode;
import com.ardor3d.image.util.awt.AWTImageUtil;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.MeshCombiner;

/**
 * Example showing how to use the TexturePacker to create a texture atlas. Also shows the benefits of using it together
 * with the MeshCombiner.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.basic.AtlasExampleMultiTextured", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_BoxExample.jpg", //
        maxHeapMemory = 64)
public class AtlasExampleMultiTextured extends ExampleBase {

    /** Text fields used to present info about the example. */
    private final BasicText _exampleInfo[] = new BasicText[3];

    private double counter = 0;
    private int frames = 0;

    private Node boxNode;

    public static void main(final String[] args) {
        start(AtlasExampleMultiTextured.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        counter += timer.getTimePerFrame();
        frames++;
        if (counter > 1) {
            final double fps = (frames / counter);
            counter = 0;
            frames = 0;
            System.out.printf("%7.1f FPS\n", fps);
        }
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Atlas Example Multitextured");

        // Use a separate node for packing/combining, otherwise we will get the text packed as well
        boxNode = new Node("boxes");
        _root.attachChild(boxNode);
        _root.setRenderMaterial("unlit/textured/basic.yaml");

        resetBoxes();

        // Setup text labels for presenting example info.
        final Node textNodes = new Node("Text");
        _orthoRoot.attachChild(textNodes);
        textNodes.getSceneHints().setLightCombineMode(LightCombineMode.Off);

        final double infoStartY = _canvas.getCanvasRenderer().getCamera().getHeight() - 20;
        for (int i = 0; i < _exampleInfo.length; i++) {
            _exampleInfo[i] = BasicText.createDefaultTextLabel("Text", "", 12);
            _exampleInfo[i].setTranslation(new Vector3(10, infoStartY - i * 16, 0));
            textNodes.attachChild(_exampleInfo[i]);
        }

        textNodes.updateGeometricState(0.0);
        updateText();

        // Pack textures into atlas
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.F), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                packIntoAtlas(boxNode);
            }
        }));

        // Combine into one mesh
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.G), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                final Mesh merged = MeshCombiner.combine(boxNode);
                boxNode.detachAllChildren();
                boxNode.attachChild(merged);
            }
        }));

        // Combine into one mesh
        _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                resetBoxes();
            }
        }));

    }

    private void resetBoxes() {
        boxNode.detachAllChildren();

        // Setup lots of boxes with different textures and wrap modes
        for (int i = 0; i < 40; i++) {
            createBox(boxNode, "images/ball.png", "icons/console.png", WrapMode.BorderClamp);
            createSphere(boxNode, "images/ball.png", "icons/console.png", WrapMode.BorderClamp);
            createBox(boxNode, "images/trail.png", "icons/console.png", WrapMode.EdgeClamp);
            createBox(boxNode, "images/flare.png", "icons/console.png", WrapMode.EdgeClamp);
            createBox(boxNode, "images/flaresmall.jpg", "icons/console.png", WrapMode.MirrorEdgeClamp);
            createBox(boxNode, "icons/ardor3d_white_24.png", "icons/console.png", WrapMode.MirrorEdgeClamp);
            createBox(boxNode, "icons/console.png", "icons/console.png", WrapMode.MirrorEdgeClamp);
            createBox(boxNode, "icons/declaration.png", "icons/console.png", WrapMode.MirroredRepeat);
            createBox(boxNode, "icons/list-add.png", "images/flare.png", WrapMode.Repeat);
            createBox(boxNode, "icons/world.png", "icons/console.png", WrapMode.Repeat);
            createSphere(boxNode, "icons/world.png", "images/ball.png", WrapMode.Repeat);
        }
    }

    private void packIntoAtlas(final Spatial spatial) {
        // Gather up all meshes to do the atlas operation on
        final List<Mesh> meshes = new ArrayList<>();
        spatial.acceptVisitor((final Spatial spat) -> {
            if (spat instanceof Mesh) {
                meshes.add((Mesh) spat);
            }
        }, false);

        // Pack textures at index 0 into one set of atlases
        packIntoAtlas(meshes, 0);
        // Pack textures at index 1 into one set of atlases
        packIntoAtlas(meshes, 1);
    }

    private void packIntoAtlas(final List<Mesh> meshes, final int textureIndex) {
        // Create an atlas packer with maximum atlas size of 256x256
        final TexturePacker packer = new TexturePacker(256, 256);

        // Add meshes into atlas (lots of different ways of doing this if you have other source/target
        // texture index)
        for (final Mesh mesh : meshes) {
            packer.insert(mesh, textureIndex, textureIndex); // make the index for the atlases the same as the source
            // textures for this case
        }

        // Create all the atlases (also possible to set filters here)
        packer.createAtlases();

        // XXX: This is only to write down the atlases to disk for debugging and viewing pleasure
        debugDumpAtlases(packer);
    }

    private Mesh createSphere(final Node parentNode, final String textureName1, final String textureName2,
            final WrapMode mode) {
        // Create sphere
        final Sphere sphere = new Sphere("Sphere", 10, 10, 1);
        sphere.setModelBound(new BoundingBox());
        sphere.setTranslation(new Vector3(MathUtils.rand.nextInt(40) - 20, MathUtils.rand.nextInt(40) - 20,
                MathUtils.rand.nextInt(40) - 100));
        parentNode.attachChild(sphere);

        setupStates(sphere, textureName1, textureName2, mode);

        return sphere;
    }

    private Mesh createBox(final Node parentNode, final String textureName1, final String textureName2,
            final WrapMode mode) {
        // Create box
        final Box box = new Box("Box", new Vector3(0, 0, 0), 1, 1, 1);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(MathUtils.rand.nextInt(40) - 20, MathUtils.rand.nextInt(40) - 20,
                MathUtils.rand.nextInt(40) - 100));
        parentNode.attachChild(box);

        setupStates(box, textureName1, textureName2, mode);

        return box;
    }

    private void setupStates(final Mesh mesh, final String textureName1, final String textureName2,
            final WrapMode mode) {
        // Add a texture to the mesh.
        final TextureState ts = new TextureState();

        final Texture texture1 = TextureManager.load(textureName1, Texture.MinificationFilter.Trilinear, true);
        texture1.setWrap(mode);
        texture1.setBorderColor(ColorRGBA.RED);
        ts.setTexture(texture1, 0);

        if (textureName2 != null) {
            final Texture texture2 = TextureManager.load(textureName2, Texture.MinificationFilter.Trilinear, true);
            texture2.setWrap(mode);
            texture2.setBorderColor(ColorRGBA.RED);
            ts.setTexture(texture2, 1);

            mesh.getMeshData().copyTextureCoordinates(0, 1, 1);
        }

        mesh.setRenderState(ts);
    }

    public void debugDumpAtlases(final TexturePacker packer) {
        int index = 0;
        for (final Texture texture : packer.getTextures()) {
            debugDumpAtlases(index++, packer.getAtlasWidth(), packer.getAtlasHeight(), texture);
        }
    }

    private void debugDumpAtlases(final int index, final int totalWidth, final int totalHeight, final Texture texture) {
        final List<BufferedImage> img = AWTImageUtil.convertToAWT(texture.getImage());
        try {
            final File file = new File("textureAtlas_" + totalWidth + "_" + totalHeight + "_" + index + ".png");
            ImageIO.write(img.get(0), "PNG", file);
            System.err.println(file.getAbsolutePath());
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Update text information.
     */
    private void updateText() {
        _exampleInfo[0].setText("1. Press [F] to create texture atlas and apply");
        _exampleInfo[1].setText("2. Press [G] to combine all into one mesh");
        _exampleInfo[2].setText("Press [R] to start over");
    }
}
