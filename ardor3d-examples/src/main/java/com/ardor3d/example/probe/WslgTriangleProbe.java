/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.probe;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL11C;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.MaterialUtil;

/**
 * Self-measuring probe for the WSLg missing-triangles issue: renders a box (the triangle-mesh
 * case under test) beside a bright green line X (the control - engine lines are known to render
 * under WSLg), then reads back the framebuffer at frame 30, classifies pixels, optionally saves
 * a PNG, prints one PROBE result line and exits.
 *
 * System properties: -Dprobe.material=unlit forces basic_white.yaml on the box (the editor grid
 * uses the unlit path); -Dprobe.cull=none disables face culling on the box; -Dprobe.out=path
 * writes the grabbed frame as a PNG.
 */
public class WslgTriangleProbe extends ExampleBase {

  private int _frames = 0;

  public static void main(final String[] args) {
    start(WslgTriangleProbe.class);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("WSLg triangle probe");

    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    cam.setLocation(new Vector3(0, 0, 22));
    cam.lookAt(Vector3.ZERO, Vector3.UNIT_Y);

    final Box box = new Box("Box", Vector3.ZERO, 3, 3, 3);
    box.setModelBound(new BoundingBox());
    if ("unlit".equals(System.getProperty("probe.material"))) {
      box.setRenderMaterial("basic_white.yaml");
    }
    if ("none".equals(System.getProperty("probe.cull"))) {
      final CullState cull = new CullState();
      cull.setCullFace(CullState.Face.None);
      box.setRenderState(cull);
    }
    _root.attachChild(box);

    // Control: a green X of engine Lines beside the box.
    final Vector3[] verts = {
        new Vector3(-9, -4, 0), new Vector3(-5, 4, 0),
        new Vector3(-5, -4, 0), new Vector3(-9, 4, 0)
    };
    final Line line = new Line("ControlLine", verts, null, null, null);
    line.setDefaultColor(new ColorRGBA(0f, 1f, 0f, 1f));
    line.setLineWidth(3f);
    _root.attachChild(line);

    MaterialUtil.autoMaterials(_root);
  }

  @Override
  protected void renderExample(final Renderer renderer) {
    if (_frames == 0) {
      // Non-black clear color: if the readback can't even see this, glReadPixels itself is
      // broken and pixel counts of zero say nothing about drawing.
      renderer.setBackgroundColor(new ColorRGBA(0.1f, 0.15f, 0.3f, 1f));
    }

    super.renderExample(renderer);

    if (++_frames != 30) {
      return;
    }

    System.out.println("PROBE GL_VENDOR:   " + GL11C.glGetString(GL11C.GL_VENDOR));
    System.out.println("PROBE GL_RENDERER: " + GL11C.glGetString(GL11C.GL_RENDERER));
    System.out.println("PROBE GL_VERSION:  " + GL11C.glGetString(GL11C.GL_VERSION));

    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    final int width = cam.getWidth();
    final int height = cam.getHeight();
    final ByteBuffer buff = BufferUtils.createByteBuffer(width * height * 3);
    renderer.grabScreenContents(buff, ImageDataFormat.RGB, 0, 0, width, height);

    int background = 0;
    int linePixels = 0;
    int boxPixels = 0;
    for (int i = 0, max = width * height; i < max; i++) {
      final int r = buff.get(i * 3) & 0xFF;
      final int g = buff.get(i * 3 + 1) & 0xFF;
      final int b = buff.get(i * 3 + 2) & 0xFF;
      // clear color is (0.1, 0.15, 0.3) -> roughly (26, 38, 77)
      if (Math.abs(r - 26) < 15 && Math.abs(g - 38) < 15 && Math.abs(b - 77) < 15) {
        background++;
      } else if (g > 96 && g > 2 * r && g > 2 * b) {
        linePixels++; // saturated green - the control line
      } else if (r > 40 && b > 40 && r + g + b > 150) {
        boxPixels++; // white/gray - the box under white light (or unlit white)
      }
    }

    final int cx = width / 2, cy = height / 2;
    final int ci = (cy * width + cx) * 3;
    System.out.println("PROBE size=" + width + "x" + height + " bgPixels=" + background + " linePixels=" + linePixels
        + " boxPixels=" + boxPixels + " centerRGB=" + (buff.get(ci) & 0xFF) + "," + (buff.get(ci + 1) & 0xFF) + ","
        + (buff.get(ci + 2) & 0xFF));

    final String out = System.getProperty("probe.out");
    if (out != null) {
      try {
        final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
          for (int x = 0; x < width; x++) {
            final int i = (y * width + x) * 3;
            final int rgb = ((buff.get(i) & 0xFF) << 16) | ((buff.get(i + 1) & 0xFF) << 8) | (buff.get(i + 2) & 0xFF);
            img.setRGB(x, height - 1 - y, rgb); // GL rows are bottom-up
          }
        }
        ImageIO.write(img, "png", new File(out));
        System.out.println("PROBE png=" + out);
      } catch (final Exception ex) {
        System.out.println("PROBE png write failed: " + ex);
      }
    }

    _exit = true;
  }
}
