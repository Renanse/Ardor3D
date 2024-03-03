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

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.util.GeneratedImageFactory;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.MouseButtonReleasedCondition;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.light.LightProperties;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.functions.Functions;
import com.ardor3d.math.functions.MandelbrotFunction3D;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.TextureKey;

/**
 * Illustrates the MandelbrotFunction3D class, which allow for procedural creation of the famous
 * Mandelbrot set.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.renderer.MandelbrotExplorerExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_MandelbrotExplorerExample.jpg", //
    maxHeapMemory = 64)
public class MandelbrotExplorerExample extends ExampleBase {

  private final Quad display = new Quad("display");
  private final Texture tex = new Texture2D();
  private final Vector2 trans = new Vector2(-.7, 0);
  private final Vector2 scale = new Vector2(1.5, 1.5);
  private float iterations = 80;
  private final ReadOnlyColorRGBA[] colors = new ReadOnlyColorRGBA[256];

  public static void main(final String[] args) {
    start(MandelbrotExplorerExample.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Ardor3D - Mandelbrot Explorer");

    // Set up our view
    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    display.resize(cam.getWidth(), cam.getHeight());
    display.setTranslation(cam.getWidth() / 2, cam.getHeight() / 2, 0);
    LightProperties.setLightReceiver(display, false);
    _orthoRoot.attachChild(display);

    // set up our color map
    colors[0] = ColorRGBA.BLUE;
    colors[30] = ColorRGBA.YELLOW;
    colors[70] = ColorRGBA.BLUE;
    colors[140] = ColorRGBA.WHITE;
    colors[220] = ColorRGBA.BLUE;
    colors[255] = ColorRGBA.BLACK;
    GeneratedImageFactory.fillInColorTable(colors);

    // set up the texture
    final TextureState ts = new TextureState();
    ts.setTexture(tex);
    _orthoRoot.setRenderState(ts);
    updateTexture();

    _orthoRoot.setRenderMaterial("unlit/textured/basic.yaml");
  }

  @Override
  public PickResults doPick(final Ray3 pickRay) {
    return null;
  }

  @Override
  protected void registerInputTriggers() {
    super.registerInputTriggers();
    _logicalLayer.registerTrigger(
        new InputTrigger(new MouseButtonReleasedCondition(MouseButton.LEFT), (source, inputState, tpf) -> {
          // zoom in
          final MouseState mouse = inputState.getCurrent().getMouseState();
          final Vector2 add =
              new Vector2(mouse.getX() - .5 * display.getWidth(), mouse.getY() - .5 * display.getHeight());
          add.multiplyLocal(scale).multiplyLocal(new Vector2(2.0 / display.getWidth(), 2.0 / display.getHeight()));
          trans.addLocal(add.getX(), add.getY());
          scale.multiplyLocal(0.5);
          updateTexture();
          iterations *= 1.1f;
        }));
    _logicalLayer.registerTrigger(
        new InputTrigger(new MouseButtonReleasedCondition(MouseButton.RIGHT), (source, inputState, tpf) -> {
          // zoom out
          final MouseState mouse = inputState.getCurrent().getMouseState();
          final Vector2 add =
              new Vector2(mouse.getX() - .5 * display.getWidth(), mouse.getY() - .5 * display.getHeight());
          add.multiplyLocal(scale).multiplyLocal(new Vector2(2.0 / display.getWidth(), 2.0 / display.getHeight()));
          trans.addLocal(add.getX(), add.getY());
          scale.multiplyLocal(1 / .5);
          updateTexture();
          iterations /= 1.1f;
        }));
  }

  private void updateTexture() {
    // Build up our function
    final MandelbrotFunction3D mandelBase = new MandelbrotFunction3D(Math.round(iterations));
    final Function3D translatedMandel = Functions.translateInput(mandelBase, trans.getX(), trans.getY(), 0);
    final Function3D finalMandel = Functions.scaleInput(translatedMandel, scale.getX(), scale.getY(), 1);

    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    Image img = GeneratedImageFactory.createRed8Image(finalMandel, cam.getWidth(), cam.getHeight(), 1);

    img = GeneratedImageFactory.createColorImageFromLuminance8(img, false, colors);
    tex.setImage(img);
    tex.setTextureKey(TextureKey.getRTTKey(MinificationFilter.Trilinear));
    tex.setMagnificationFilter(MagnificationFilter.Bilinear);
    tex.setMinificationFilter(MinificationFilter.Trilinear);
  }
}
