/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.renderer.lwjgl3;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Assume;
import org.junit.Test;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl3.GLFWCanvas;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.SceneIndexer;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * GL smoke test: renders a lit box, an unlit box and a line into a real GL context and asserts
 * that each path actually produces pixels. This guards against the class of bug where a strict
 * driver (all of Mesa - llvmpipe, WSLg's D3D12, Linux Intel/AMD) rejects draws that lenient
 * drivers accept - e.g. two sampler uniforms of different types sharing a texture unit, which
 * silently blanked the entire lit pipeline while NVIDIA rendered it fine.
 *
 * The test SKIPS when no usable GL context or framebuffer readback is available (headless CI
 * without Xvfb, WSLg's D3D12 readback), so it cannot break builds over infrastructure. Set
 * ARDOR3D_REQUIRE_GL_SMOKE=1 (as CI does, under Xvfb + llvmpipe) to turn those skips into
 * failures so the net cannot silently rot.
 */
public class GlLitRenderSmokeTest {

  private static final int SIZE = 256;
  private static final ColorRGBA CLEAR_COLOR = new ColorRGBA(0.1f, 0.15f, 0.3f, 1f);
  private static final int GRAB_FRAME = 12;

  /** Renders the probe scene and counts classified pixels at frame {@link #GRAB_FRAME}. */
  private static class ProbeScene implements Scene {
    private final Node _root = new Node("root");
    private boolean _initialized = false;
    private int _frames = 0;

    boolean done = false;
    int bgPixels, litPixels, unlitPixels, linePixels;
    String glRenderer;

    @Override
    public boolean render(final Renderer renderer) {
      final var canvasRenderer = com.ardor3d.renderer.ContextManager.getCurrentContext();
      final Camera cam = Camera.getCurrentCamera();

      if (!_initialized) {
        _initialized = true;
        renderer.setBackgroundColor(CLEAR_COLOR);
        cam.setLocation(new Vector3(0, 0, 22));
        cam.lookAt(Vector3.ZERO, Vector3.UNIT_Y);
        cam.setFrustumPerspective(45.0, 1.0, 0.1, 100.0);
        buildScene();
        SceneIndexer.getCurrent().addSceneRoot(_root);
        glRenderer = org.lwjgl.opengl.GL11C.glGetString(org.lwjgl.opengl.GL11C.GL_RENDERER);
      }

      _root.updateGeometricState(0.05, true);
      SceneIndexer.getCurrent().onRender(renderer);

      cam.apply(renderer);
      _root.onDraw(renderer);
      renderer.renderBuckets();

      if (++_frames == GRAB_FRAME) {
        classifyPixels(renderer);
        done = true;
      }
      return canvasRenderer != null;
    }

    private void buildScene() {
      // Right half: lit box (the path strict drivers rejected)
      final Box litBox = new Box("LitBox", Vector3.ZERO, 2.5, 2.5, 2.5);
      litBox.setModelBound(new BoundingBox());
      litBox.setTranslation(4.5, 1.0, 0.0);
      _root.attachChild(litBox);

      // Left half: unlit box
      final Box unlitBox = new Box("UnlitBox", Vector3.ZERO, 2.5, 2.5, 2.5);
      unlitBox.setModelBound(new BoundingBox());
      unlitBox.setTranslation(-4.5, 1.0, 0.0);
      unlitBox.setRenderMaterial("basic_white.yaml");
      _root.attachChild(unlitBox);

      // Bottom band: green line
      final Line line = new Line("ControlLine",
          new Vector3[] {new Vector3(-4, -6.5, 0), new Vector3(4, -6.5, 0)}, null, null, null);
      line.setDefaultColor(new ColorRGBA(0f, 1f, 0f, 1f));
      line.setLineWidth(3f);
      _root.attachChild(line);

      final PointLight light = new PointLight();
      light.setColor(ColorRGBA.WHITE);
      light.setIntensity(0.75f);
      light.setTranslation(10, 10, 10);
      light.setEnabled(true);
      _root.attachChild(light);

      MaterialUtil.autoMaterials(_root);
    }

    private void classifyPixels(final Renderer renderer) {
      final ByteBuffer buff = BufferUtils.createByteBuffer(SIZE * SIZE * 3);
      renderer.grabScreenContents(buff, ImageDataFormat.RGB, 0, 0, SIZE, SIZE);

      final int bottomBand = (int) (SIZE * 0.30); // buffer rows are bottom-up
      for (int y = 0; y < SIZE; y++) {
        for (int x = 0; x < SIZE; x++) {
          final int i = (y * SIZE + x) * 3;
          final int r = buff.get(i) & 0xFF;
          final int g = buff.get(i + 1) & 0xFF;
          final int b = buff.get(i + 2) & 0xFF;
          if (Math.abs(r - 26) < 20 && Math.abs(g - 38) < 20 && Math.abs(b - 77) < 20) {
            bgPixels++;
          } else if (y < bottomBand) {
            if (g > 96 && g > 2 * r && g > 2 * b) {
              linePixels++;
            }
          } else if (x < SIZE / 2) {
            unlitPixels++;
          } else {
            litPixels++;
          }
        }
      }
    }

    @Override
    public PickResults doPick(final Ray3 pickRay) {
      return null;
    }
  }

  private static void skipOrFail(final String reason) {
    if ("1".equals(System.getenv("ARDOR3D_REQUIRE_GL_SMOKE"))) {
      fail(reason + " - and ARDOR3D_REQUIRE_GL_SMOKE=1 requires this test to run");
    }
    Assume.assumeTrue(reason, false);
  }

  private static void setupResourceLocators() throws Exception {
    ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MATERIAL, new SimpleResourceLocator(
        ResourceLocatorTool.getClassPathResource(MaterialManager.class, "com/ardor3d/renderer/material")));
    ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_SHADER, new SimpleResourceLocator(
        ResourceLocatorTool.getClassPathResource(MaterialManager.class, "com/ardor3d/renderer/shader")));
  }

  @Test
  public void litUnlitAndLineGeometryAllRender() throws Exception {
    final ProbeScene scene = new ProbeScene();
    GLFWCanvas canvas = null;
    try {
      setupResourceLocators();
      canvas = new GLFWCanvas(new DisplaySettings(SIZE, SIZE, 24, 0), new Lwjgl3CanvasRenderer(scene));
      canvas.init();
    } catch (final Throwable t) {
      skipOrFail("Could not create a GL context here: " + t);
      return;
    }

    try {
      for (int i = 0; i < GRAB_FRAME + 2 && !scene.done; i++) {
        canvas.draw(null);
      }

      assertTrue("scene never reached the readback frame", scene.done);

      final int total = SIZE * SIZE;
      if (scene.bgPixels < total / 2) {
        // e.g. WSLg's D3D12 driver returns zeros from default-framebuffer readback
        skipOrFail("Framebuffer readback unusable on '" + scene.glRenderer + "' (clear color not visible: "
            + scene.bgPixels + "/" + total + " bg pixels)");
      }

      final String on = " on '" + scene.glRenderer + "'";
      assertTrue("unlit triangles did not render" + on, scene.unlitPixels > 500);
      assertTrue("lines did not render" + on, scene.linePixels > 30);
      assertTrue("LIT triangles did not render" + on
          + " - was the draw rejected (e.g. GL_INVALID_OPERATION from conflicting sampler units)?",
          scene.litPixels > 500);
    } finally {
      canvas.close();
    }
  }
}
