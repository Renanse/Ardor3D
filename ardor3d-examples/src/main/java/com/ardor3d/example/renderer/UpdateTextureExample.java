/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.util.awt.AWTImageLoader;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.RenderState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.ui.text.BasicText;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * A demonstration of procedurally updating a texture.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.renderer.UpdateTextureExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_UpdateTextureExample.jpg", //
    maxHeapMemory = 64)
public class UpdateTextureExample extends ExampleBase {
  private Mesh t;
  private final Matrix3 rotate = new Matrix3();
  private double angle = 0;
  private final Vector3 axis = new Vector3(1, 1, 0.5f).normalizeLocal();
  private BufferedImage img;
  private Graphics imgGraphics;
  private ByteBuffer imageBuffer;
  private double counter = 0;
  private int mode = 0;

  public static void main(final String[] args) {
    start(UpdateTextureExample.class);
  }

  @Override
  protected void renderExample(final Renderer renderer) {
    switch (mode) {
      case 0: {
        final byte[] data = AWTImageLoader.asByteArray(img);
        imageBuffer.put(data);
        imageBuffer.flip();
        final Texture prevTexture =
            ((TextureState) _root.getLocalRenderState(RenderState.StateType.Texture)).getTexture();
        renderer.getTextureUtils().updateTexture2DSubImage((Texture2D) prevTexture, 0, 0, img.getWidth(),
            img.getHeight(), imageBuffer, 0, 0, img.getWidth());
        break;
      }
      case 1: {
        final byte[] data = AWTImageLoader.asByteArray(img);
        imageBuffer.put(data);
        imageBuffer.flip();
        final Texture prevTexture =
            ((TextureState) _root.getLocalRenderState(RenderState.StateType.Texture)).getTexture();
        renderer.getTextureUtils().updateTexture2DSubImage((Texture2D) prevTexture, 100, 50, 100, 100, imageBuffer, 100,
            50, img.getWidth());
        break;
      }
      case 2: {
        final Image nextImage = AWTImageLoader.makeArdor3dImage(img, false);
        final Texture nextTexture = TextureManager.loadFromImage(nextImage, Texture.MinificationFilter.Trilinear);
        final TextureState ts = (TextureState) _root.getLocalRenderState(RenderState.StateType.Texture);
        ts.setTexture(nextTexture);
        break;
      }
    }

    super.renderExample(renderer);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    final double tpf = timer.getTimePerFrame();
    if (tpf < 1) {
      angle = angle + tpf * 20;
      if (angle > 360) {
        angle = 0;
      }
    }

    rotate.fromAngleNormalAxis(angle * MathUtils.DEG_TO_RAD, axis);
    t.setRotation(rotate);

    for (int i = 0; i < 1000; i++) {
      final int w = MathUtils.nextRandomInt(0, img.getWidth() - 1);
      final int h = MathUtils.nextRandomInt(0, img.getHeight() - 1);
      final int y = Math.max(0, h - 8);
      final int rgb = img.getRGB(w, h);
      for (int j = h; j > y; j -= 2) {
        img.setRGB(w, j, rgb);
      }
    }

    final int x = (int) (Math.sin(counter) * img.getWidth() * 0.3 + img.getWidth() * 0.5);
    final int y = (int) (Math.sin(counter * 0.7) * img.getHeight() * 0.3 + img.getHeight() * 0.5);
    imgGraphics.setColor(new Color(MathUtils.nextRandomInt()));
    imgGraphics.fillOval(x, y, 10, 10);
    counter += tpf * 5;
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Update texture - Example");

    final BasicText keyText = BasicText.createDefaultTextLabel("Text", "[SPACE] Updating existing texture...");
    keyText.setTranslation(new Vector3(10, 10, 0));
    _orthoRoot.attachChild(keyText);

    _logicalLayer.registerTrigger(new InputTrigger(new KeyPressedCondition(Key.SPACE), (source, inputStates, tpf) -> {
      mode++;
      mode %= 3;
      switch (mode) {
        case 0:
          keyText.setText("[SPACE] Updating existing texture...");
          break;
        case 1:
          keyText.setText("[SPACE] Updating just part of the texture...");
          break;
        case 2:
          keyText.setText("[SPACE] Recreating texture from scratch...");
          break;
      }
    }));

    final Vector3 max = new Vector3(5, 5, 5);
    final Vector3 min = new Vector3(-5, -5, -5);

    t = new Box("Box", min, max);
    t.setModelBound(new BoundingBox());
    t.setTranslation(new Vector3(0, 0, -15));
    _root.attachChild(t);

    final TextureState ts = new TextureState();
    ts.setEnabled(true);
    ts.setTexture(TextureManager.load("images/ardor3d_white_256.jpg", Texture.MinificationFilter.Trilinear, false));

    _root.setRenderState(ts);
    _root.setRenderMaterial("unlit/textured/basic.yaml");

    try {
      img = ImageIO.read(ResourceLocatorTool
          .locateResource(ResourceLocatorTool.TYPE_TEXTURE, "images/ardor3d_white_256.jpg").openStream());
      // FIXME: Check if this is a int[] or byte[]
      final byte[] data = AWTImageLoader.asByteArray(img);
      imageBuffer = BufferUtils.createByteBuffer(data.length);

      imgGraphics = img.getGraphics();
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

  }
}
