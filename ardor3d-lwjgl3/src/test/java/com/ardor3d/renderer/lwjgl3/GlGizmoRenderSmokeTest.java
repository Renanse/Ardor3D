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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.text.DecimalFormatSymbols;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assume;
import org.junit.Test;

import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.widget.gizmo.AbstractGizmo;
import com.ardor3d.extension.interact.widget.gizmo.RotateGizmo;
import com.ardor3d.extension.interact.widget.gizmo.ScaleGizmo;
import com.ardor3d.extension.interact.widget.gizmo.TranslateGizmo;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.framework.Scene;
import com.ardor3d.framework.lwjgl3.GLFWCanvas;
import com.ardor3d.framework.lwjgl3.Lwjgl3CanvasRenderer;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.input.InputState;
import com.ardor3d.input.character.CharacterInputState;
import com.ardor3d.input.controller.ControllerState;
import com.ardor3d.input.gesture.GestureState;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.intersection.PickResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Ray3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.material.MaterialManager;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.SceneIndexer;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.SimpleResourceLocator;

/**
 * GL smoke test for the v2 interact gizmos. It renders the translate, rotate and scale gizmos into
 * a real GL context and reads the framebuffer back, then asserts:
 * <ul>
 * <li>each gizmo's saturated axis colors (red/green/blue) actually rasterize - the gizmos' thin
 * geometry is drawn as antialiased screen-space strokes via a line geometry shader, exactly the
 * kind of pipeline a strict driver (all of Mesa - llvmpipe, WSLg's D3D12, Linux Intel/AMD) can
 * reject where a lenient driver would not (see {@link GlLitRenderSmokeTest});</li>
 * <li>hovering a handle brightens it - the hover highlight is a distinct render path;</li>
 * <li>a synthetic drag around the X rotate ring drives the pie wedge and leaves the drag angle,
 * the on-screen readout and the target's applied rotation all in agreement; and</li>
 * <li>a synthetic drag on the +X scale cube actually grows the target's X scale.</li>
 * </ul>
 * Like {@link GlLitRenderSmokeTest} this SKIPS when no usable GL context or framebuffer readback is
 * available, so it cannot break builds over infrastructure; set ARDOR3D_REQUIRE_GL_SMOKE=1 (as CI
 * does, under Xvfb + llvmpipe) to turn those skips into failures so the net cannot silently rot.
 * The drag math itself is covered headlessly by the gizmo drag tests in ardor3d-extras; this test
 * adds the GL rendering those tests cannot exercise.
 */
public class GlGizmoRenderSmokeTest {

  private static final int SIZE = 256;
  private static final ColorRGBA CLEAR_COLOR = new ColorRGBA(0.1f, 0.15f, 0.3f, 1f);

  // A fixed 50ms-per-frame timer so each scripted frame is a faithful update()+render() pair. The
  // gizmos advance their eased hover highlight in update(); four frames at this step (200ms) take
  // the highlight well past its ~100ms ease, matching what the interactive example shows.
  private static final ReadOnlyTimer FIXED_TIMER = new ReadOnlyTimer() {
    @Override
    public double getTimeInSeconds() { return 0; }

    @Override
    public long getTime() { return 0; }

    @Override
    public long getResolution() { return 1_000_000_000L; }

    @Override
    public double getFrameRate() { return 20.0; }

    @Override
    public double getTimePerFrame() { return 0.05; }

    @Override
    public long getPreviousFrameTime() { return 0; }
  };

  // How far a synthetic drag is walked, in per-frame steps, once it has started.
  private static final int DRAG_STEPS = 16;
  private static final double RING_STEP_RADIANS = 0.05; // ~0.75 rad (~43 deg) swept over the drag
  private static final double SCALE_STEP = 0.03; // pulls the +X cube ~48% further out over the drag

  /**
   * Drives the gizmos across a scripted sequence of frames and records what rendered. The phases
   * run back to back in one context: translate render, hover, rotate-ring drag, scale drag.
   */
  private static class ProbeScene implements Scene {
    private enum Phase {
      TRANSLATE, HOVER, ROTATE, SCALE, DONE
    }

    private final Node _root = new Node("root");
    private final Node _target = new Node("target");
    private final InteractManager _manager = new InteractManager();
    private TranslateGizmo _translate;
    private RotateGizmo _rotate;
    private ScaleGizmo _scale;

    private Canvas _canvas;
    private boolean _initialized = false;
    private Phase _phase = Phase.TRANSLATE;
    private int _phaseFrame = 0;
    private MouseState _lastDragMouse = null;

    boolean done = false;
    boolean readbackUnusable = false;
    int bgPixels;
    String glRenderer;

    // Translate gizmo, resting (no hover).
    int txRed, txGreen, txBlue, txHighlight;
    // Translate gizmo with the +X arrow hovered.
    int hoverRed, hoverHighlight;
    // Rotate gizmo: rings at rest, then mid-drag (drag-focus dims the inactive Y/Z rings).
    int rotRestGreen, rotRestBlue, rotRed, rotGreen, rotBlue;
    double dragAngleDeg, readoutDeg, targetAngleDeg;
    // Scale gizmo: shafts at rest, then mid-drag (drag-focus dims the inactive Y/Z shafts).
    int scRestGreen, scRestBlue, scRed, scGreen, scBlue;
    double targetScaleX, scReadout;

    void setCanvas(final Canvas canvas) { _canvas = canvas; }

    @Override
    public boolean render(final Renderer renderer) {
      final Camera cam = Camera.getCurrentCamera();

      if (!_initialized) {
        _initialized = true;
        renderer.setBackgroundColor(CLEAR_COLOR);
        // A three-quarter view so no world axis is edge-on (which would fade a handle out) and the
        // X ring is tilted enough to show its pie wedge.
        cam.setLocation(new Vector3(8, 6, 8));
        cam.lookAt(Vector3.ZERO, Vector3.UNIT_Y);
        cam.setFrustumPerspective(45.0, 1.0, 0.5, 100.0);
        buildScene();
        SceneIndexer.getCurrent().addSceneRoot(_root);
        glRenderer = org.lwjgl.opengl.GL11C.glGetString(org.lwjgl.opengl.GL11C.GL_RENDERER);
      }

      _root.updateGeometricState(0.05, true);
      SceneIndexer.getCurrent().onRender(renderer);

      cam.apply(renderer);
      _root.onDraw(renderer);
      renderer.renderBuckets();

      // Advance the gizmo's animation for the frame (eased hover highlight, etc.), reacting to the
      // hover/drag state the previous frame's input left, exactly like the example's update pass.
      _manager.update(FIXED_TIMER);

      // Inject the phase's synthetic input before the gizmo draws, so the render reflects it.
      switch (_phase) {
        case HOVER -> simulateHoverOnXArrow(cam);
        case ROTATE -> {
          if (_phaseFrame >= 1) {
            simulateDragOnXRing(cam);
          }
        }
        case SCALE -> {
          if (_phaseFrame >= 1) {
            simulateDragOnXScale(cam);
          }
        }
        default -> {
          /* TRANSLATE just renders */ }
      }

      _manager.render(renderer);

      advancePhase(renderer);
      _phaseFrame++;
      return true;
    }

    private void buildScene() {
      _root.attachChild(_target);

      _translate = new TranslateGizmo().withAllHandles();
      _rotate = new RotateGizmo().withAllHandles();
      _scale = new ScaleGizmo().withAllHandles();
      _manager.addWidget(_translate);
      _manager.addWidget(_rotate);
      _manager.addWidget(_scale);

      _root.updateGeometricState(0);
      _manager.setSpatialTarget(_target);
      _manager.setActiveWidget(_translate);
    }

    /** Move to the next phase, taking measurements at the end of each. */
    private void advancePhase(final Renderer renderer) {
      switch (_phase) {
        case TRANSLATE -> {
          if (_phaseFrame == 4) {
            classify(renderer);
            txRed = _red;
            txGreen = _green;
            txBlue = _blue;
            txHighlight = _highlight;
            // Only meaningful once something has been drawn - check the readback is real here.
            readbackUnusable = bgPixels < SIZE * SIZE / 2;
            enter(Phase.HOVER);
          }
        }
        case HOVER -> {
          if (_phaseFrame == 4) {
            classify(renderer);
            hoverRed = _red;
            hoverHighlight = _highlight;
            enter(Phase.ROTATE);
          }
        }
        case ROTATE -> {
          if (_phaseFrame == 0) {
            // Rings at rest (no drag yet): the AA-stroke pipeline rasterizes all three ring colors.
            classify(renderer);
            rotRestGreen = _green;
            rotRestBlue = _blue;
          }
          if (_phaseFrame == DRAG_STEPS) {
            classify(renderer);
            rotRed = _red;
            rotGreen = _green;
            rotBlue = _blue;
            dragAngleDeg = Math.abs(_rotate.getDragAngle()) * MathUtils.RAD_TO_DEG;
            readoutDeg = Math.abs(parseReadout(_rotate.getReadout().getText()));
            targetAngleDeg = new Quaternion().fromRotationMatrix(_target.getRotation()).toAngleAxis(new Vector3())
                * MathUtils.RAD_TO_DEG;
            enter(Phase.SCALE);
          }
        }
        case SCALE -> {
          if (_phaseFrame == 0) {
            // Shafts at rest (no drag yet): all three axis-shaft colors rasterize.
            classify(renderer);
            scRestGreen = _green;
            scRestBlue = _blue;
          }
          if (_phaseFrame == DRAG_STEPS) {
            classify(renderer);
            scRed = _red;
            scGreen = _green;
            scBlue = _blue;
            targetScaleX = _target.getScale().getX();
            scReadout = parseReadout(_scale.getReadout().getText());
            enter(Phase.DONE);
            done = true;
          }
        }
        default -> {
          /* DONE */ }
      }
    }

    private void enter(final Phase next) {
      _phase = next;
      _phaseFrame = -1; // ++ at the end of render() brings the next frame in as phase-frame 0
      _lastDragMouse = null;
      switch (next) {
        case ROTATE -> _manager.setActiveWidget(_rotate);
        case SCALE -> _manager.setActiveWidget(_scale);
        default -> {
          /* keep translate active */ }
      }
    }

    /**
     * Pretend the mouse rests mid-shaft on the +X arrow, so the render shows the hover highlight.
     * Re-applied each hover frame for parity with how the example probe works.
     */
    private void simulateHoverOnXArrow(final Camera cam) {
      final double scale = _translate.getHandle().getScale().getX();
      final Vector3 onArrow = new Vector3(0.6 * scale, 0, 0).addLocal(_target.getWorldTranslation());
      final Vector3 screen = cam.getScreenCoordinates(onArrow);
      final MouseState hover = new MouseState((int) screen.getX(), (int) screen.getY(), 0, 0, 0, null, null);
      _translate.checkMouseOver(_canvas, hover, _manager);
    }

    /** A synthetic left-button drag walking around the X ring, driving the full processInput path. */
    private void simulateDragOnXRing(final Camera cam) {
      final ReadOnlyVector3 origin = _target.getWorldTranslation();
      final double scale = _rotate.getHandle().getScale().getX();

      final Vector3 toCam = new Vector3(cam.getDirection()).negateLocal();
      final Vector3 e1 =
          toCam.subtract(Vector3.UNIT_X.multiply(toCam.dot(Vector3.UNIT_X), null), null).normalizeLocal();
      final Vector3 e2 = Vector3.UNIT_X.cross(e1, null);
      final double angle = _phaseFrame * RING_STEP_RADIANS;
      final Vector3 onRing = new Vector3(e1).multiplyLocal(Math.cos(angle) * RotateGizmo.RING_RADIUS * scale)
          .addLocal(e2.multiply(Math.sin(angle) * RotateGizmo.RING_RADIUS * scale, null)).addLocal(origin);
      final Vector3 screen = cam.getScreenCoordinates(onRing);

      applyDrag(_rotate, screen);
    }

    /** A synthetic left-button drag pulling the +X scale cube outward along its axis. */
    private void simulateDragOnXScale(final Camera cam) {
      final ReadOnlyVector3 origin = _target.getWorldTranslation();
      final double scale = _scale.getHandle().getScale().getX();

      // The target is unrotated, so its local X (the scale gizmo's frame) is world X.
      final double pull = 1.0 + _phaseFrame * SCALE_STEP;
      final Vector3 onAxis = new Vector3(ScaleGizmo.TIP_CENTER * pull * scale, 0, 0).addLocal(origin);
      final Vector3 screen = cam.getScreenCoordinates(onAxis);

      applyDrag(_scale, screen);
    }

    /**
     * Feed one frame of a held left-button drag to the active gizmo at the given screen point. The
     * first frame's previous state has the button up at the same spot, starting the drag there.
     */
    private void applyDrag(final AbstractGizmo gizmo, final Vector3 screen) {
      final EnumMap<MouseButton, ButtonState> buttons = new EnumMap<>(MouseButton.class);
      buttons.put(MouseButton.LEFT, ButtonState.DOWN);
      final MouseState previous = _lastDragMouse != null ? _lastDragMouse
          : new MouseState((int) screen.getX(), (int) screen.getY(), 0, 0, 0, null, null);
      final MouseState current = new MouseState((int) screen.getX(), (int) screen.getY(),
          (int) screen.getX() - previous.getX(), (int) screen.getY() - previous.getY(), 0, buttons, null);
      _lastDragMouse = current;

      final InputState previousState = new InputState(KeyboardState.NOTHING, previous, ControllerState.NOTHING,
          GestureState.NOTHING, CharacterInputState.NOTHING);
      final InputState currentState = new InputState(KeyboardState.NOTHING, current, ControllerState.NOTHING,
          GestureState.NOTHING, CharacterInputState.NOTHING);

      gizmo.processInput(_canvas, new TwoInputStates(previousState, currentState), new AtomicBoolean(false), _manager);
      _manager.getSpatialState().applyState(_target);
    }

    private static double parseReadout(final String text) {
      if (text == null) {
        return 0;
      }
      // RotateGizmo formats the readout with String.format's default (FORMAT-category) locale, whose
      // decimal separator may be a comma; normalize it to a dot before stripping the rest so the
      // parse matches the format instead of turning e.g. "43,0" into "430".
      final char separator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
      final String normalized = separator == '.' ? text : text.replace(separator, '.');
      final String digits = normalized.replaceAll("[^0-9.\\-]", "");
      return digits.isEmpty() ? 0 : Double.parseDouble(digits);
    }

    // Scratch pixel-count fields filled by classify(), using the same saturated-color thresholds
    // as InteractGizmoExample.grabShot so the two stay comparable.
    private int _red, _green, _blue, _highlight;

    private void classify(final Renderer renderer) {
      final ByteBuffer buff = BufferUtils.createByteBuffer(SIZE * SIZE * 3);
      renderer.grabScreenContents(buff, ImageDataFormat.RGB, 0, 0, SIZE, SIZE);

      _red = _green = _blue = _highlight = 0;
      bgPixels = 0;
      for (int i = 0; i < SIZE * SIZE; i++) {
        final int r = buff.get(i * 3) & 0xFF;
        final int g = buff.get(i * 3 + 1) & 0xFF;
        final int b = buff.get(i * 3 + 2) & 0xFF;
        if (Math.abs(r - 26) < 20 && Math.abs(g - 38) < 20 && Math.abs(b - 77) < 20) {
          bgPixels++;
        } else if (r > 170 && g < 110 && b < 130) {
          _red++;
        } else if (g > 150 && r < 160 && b < 100) {
          _green++;
        } else if (b > 170 && r < 110 && g < 180) {
          _blue++;
        } else if (r > 200 && g > 170 && b < 110) {
          _highlight++;
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
  public void gizmosRenderHoverAndDrag() throws Exception {
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
      scene.setCanvas(canvas);
      for (int i = 0; i < 200 && !scene.done; i++) {
        canvas.draw(null);
      }
      assertTrue("scene never finished its scripted frames", scene.done);

      if (scene.readbackUnusable) {
        // e.g. WSLg's D3D12 driver returns zeros from default-framebuffer readback
        skipOrFail("Framebuffer readback unusable on '" + scene.glRenderer + "' (" + scene.bgPixels + "/"
            + (SIZE * SIZE) + " background pixels)");
      }

      System.out.println("GIZMO SMOKE on '" + scene.glRenderer + "'"
          + " translate[r=" + scene.txRed + " g=" + scene.txGreen + " b=" + scene.txBlue + " hi=" + scene.txHighlight
          + "] hover[r=" + scene.hoverRed + " hi=" + scene.hoverHighlight + "]"
          + " rotate[rest g=" + scene.rotRestGreen + " b=" + scene.rotRestBlue + "; drag r=" + scene.rotRed + " g="
          + scene.rotGreen + " b=" + scene.rotBlue + " dragAngle=" + scene.dragAngleDeg + " readout=" + scene.readoutDeg
          + " target=" + scene.targetAngleDeg + "]"
          + " scale[rest g=" + scene.scRestGreen + " b=" + scene.scRestBlue + "; drag r=" + scene.scRed + " g="
          + scene.scGreen + " b=" + scene.scBlue + " sx=" + scene.targetScaleX + " readout=" + scene.scReadout + "]");

      final String on = " on '" + scene.glRenderer + "'";

      // 1. Every gizmo's three axis colors rasterize at rest (the AA-stroke line pipeline works):
      // the translate arrows, the rotate rings, and the scale shafts each build their own geometry.
      assertTrue("translate X (red) did not render" + on, scene.txRed > 40);
      assertTrue("translate Y (green) did not render" + on, scene.txGreen > 40);
      assertTrue("translate Z (blue) did not render" + on, scene.txBlue > 40);
      assertTrue("rotate rings did not render at rest" + on, scene.rotRestGreen > 40 && scene.rotRestBlue > 40);
      assertTrue("scale shafts did not render at rest" + on, scene.scRestGreen > 40 && scene.scRestBlue > 40);

      // 2. Hovering the +X arrow lights it up - a distinct highlight render path. A chromatic
      // handle brightens toward white rather than toward the yellow highlight tint, so its pixels
      // leave the saturated-red bucket: the arrow's red count drops sharply while the (unhovered)
      // red YZ-plane handle keeps the rest.
      assertTrue("hovering the +X arrow did not change its pixels (highlight path)" + on,
          scene.hoverRed < scene.txRed - 150);

      // 3. A ring drag leaves drag angle, readout and applied rotation all agreeing on a clearly
      // nonzero sweep, and the swept pie wedge (its red edges) rasterizes.
      assertTrue("rotate pie wedge did not render" + on, scene.rotRed > 40);
      assertTrue("ring drag produced no rotation" + on, scene.targetAngleDeg > 15);
      assertEquals("drag angle and readout disagree" + on, scene.dragAngleDeg, scene.readoutDeg, 3.0);
      assertEquals("drag angle and applied target rotation disagree" + on, scene.dragAngleDeg, scene.targetAngleDeg,
          6.0);

      // 4. A +X scale drag grows the target's X scale into a sane range, and the readout agrees.
      assertTrue("scale drag did not grow target X scale (was " + scene.targetScaleX + ")" + on,
          scene.targetScaleX > 1.1 && scene.targetScaleX < 20.0);
      assertEquals("scale readout disagrees with applied X factor" + on, scene.targetScaleX, scene.scReadout, 0.02);

      // 5. Drag-focus dims the inactive handles: mid-drag, the Y/Z rings and shafts (not the dragged
      // axis) fade well below their resting counts as they blend toward the background.
      assertTrue("drag-focus did not dim the inactive rotate rings" + on,
          scene.rotGreen < scene.rotRestGreen / 2 && scene.rotBlue < scene.rotRestBlue / 2);
      assertTrue("drag-focus did not dim the inactive scale shafts" + on,
          scene.scGreen < scene.scRestGreen / 2 && scene.scBlue < scene.scRestBlue / 2);
    } finally {
      canvas.close();
    }
  }
}
