/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer.utils.atlas;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
import com.ardor3d.input.Key;
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
import com.ardor3d.scenegraph.visitor.Visitor;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.MeshCombiner;
import com.google.common.collect.Lists;

/**
 * Example showing how to use the TexturePacker to create a texture atlas. Also shows the benefits of using it together
 * with the MeshCombiner.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.basic.AtlasExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_BoxExample.jpg", //
        maxHeapMemory = 64)
public class AtlasExample extends ExampleBase {

    /** Text fields used to present info about the example. */
    private final BasicText _exampleInfo[] = new BasicText[3];

    private double counter = 0;
    private int frames = 0;

    private Node boxNode;

    public static void main(final String[] args) {
        start(AtlasExample.class);
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
        _canvas.setTitle("Atlas Example");

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
            createBox(boxNode, "images/ball.png", WrapMode.BorderClamp);
            createSphere(boxNode, "images/ball.png", WrapMode.BorderClamp);
            createBox(boxNode, "images/trail.png", WrapMode.EdgeClamp);
            createBox(boxNode, "images/flare.png", WrapMode.EdgeClamp);
            createBox(boxNode, "images/flaresmall.jpg", WrapMode.MirrorEdgeClamp);
            createBox(boxNode, "icons/ardor3d_white_24.png", WrapMode.MirrorEdgeClamp);
            createBox(boxNode, "icons/console.png", WrapMode.MirrorEdgeClamp);
            createBox(boxNode, "icons/declaration.png", WrapMode.MirroredRepeat);
            createBox(boxNode, "icons/list-add.png", WrapMode.Repeat);
            createBox(boxNode, "icons/world.png", WrapMode.Repeat);
            createSphere(boxNode, "icons/world.png", WrapMode.Repeat);
        }
    }

    private void packIntoAtlas(final Spatial spatial) {
        // Gather up all meshes to do the atlas operation on
        final List<Mesh> meshes = Lists.newArrayList();
        final Visitor visitor = new Visitor() {
            @Override
            public void visit(final Spatial spatial) {
                if (spatial instanceof Mesh) {
                    meshes.add((Mesh) spatial);
                }
            }
        };
        spatial.acceptVisitor(visitor, false);

        // Create an atlas packer with maximum atlas size of 256x256
        final TexturePacker packer = new TexturePacker(256, 256);

        // Add meshes into atlas (lots of different ways of doing this if you have other source/target
        // texture index)
        for (final Mesh mesh : meshes) {
            packer.insert(mesh);
        }

        // Create all the atlases (also possible to set filters here)
        packer.createAtlases();

        // XXX: This is only to write down the atlases to disk for debugging and viewing pleasure
        debugDumpAtlases(packer);
    }

    private Mesh createSphere(final Node parentNode, final String textureName, final WrapMode mode) {
        // Create sphere
        final Sphere sphere = new Sphere("Sphere", 10, 10, 1);
        sphere.setModelBound(new BoundingBox());
        sphere.setTranslation(new Vector3(MathUtils.rand.nextInt(40) - 20, MathUtils.rand.nextInt(40) - 20,
                MathUtils.rand.nextInt(40) - 100));
        parentNode.attachChild(sphere);

        setupStates(sphere, textureName, mode);

        return sphere;
    }

    private Mesh createBox(final Node parentNode, final String textureName, final WrapMode mode) {
        // Create box
        final Box box = new Box("Box", new Vector3(0, 0, 0), 1, 1, 1);
        box.setModelBound(new BoundingBox());
        box.setTranslation(new Vector3(MathUtils.rand.nextInt(40) - 20, MathUtils.rand.nextInt(40) - 20,
                MathUtils.rand.nextInt(40) - 100));
        parentNode.attachChild(box);

        setupStates(box, textureName, mode);

        return box;
    }

    private void setupStates(final Mesh mesh, final String textureName, final WrapMode mode) {
        // Add a texture to the mesh.
        final TextureState ts = new TextureState();
        final Texture texture = TextureManager.load(textureName, Texture.MinificationFilter.Trilinear, true);
        texture.setWrap(mode);
        texture.setBorderColor(ColorRGBA.RED);
        ts.setTexture(texture);
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
