/**
 * Copyright (c) 2008-2026 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.interact;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.buffer.BufferUtils;
import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.PropertiesGameSettings;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.interact.InteractManager;
import com.ardor3d.extension.interact.filter.AngleSnapFilter;
import com.ardor3d.extension.interact.filter.GridSnapFilter;
import com.ardor3d.extension.interact.widget.InteractMatrix;
import com.ardor3d.extension.interact.widget.gizmo.RotateGizmo;
import com.ardor3d.extension.interact.widget.gizmo.ScaleGizmo;
import com.ardor3d.extension.interact.widget.gizmo.TranslateGizmo;
import com.ardor3d.image.ImageDataFormat;
import com.ardor3d.image.Texture;
import com.ardor3d.input.InputState;
import com.ardor3d.input.character.CharacterInputState;
import com.ardor3d.input.controller.ControllerState;
import com.ardor3d.input.gesture.GestureState;
import com.ardor3d.input.keyboard.Key;
import com.ardor3d.input.keyboard.KeyboardState;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.KeyHeldCondition;
import com.ardor3d.input.logical.KeyPressedCondition;
import com.ardor3d.input.logical.KeyReleasedCondition;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.input.mouse.ButtonState;
import com.ardor3d.input.mouse.MouseButton;
import com.ardor3d.input.mouse.MouseState;
import com.ardor3d.intersection.PickData;
import com.ardor3d.intersection.Pickable;
import com.ardor3d.intersection.PrimitivePickResults;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Quaternion;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.math.util.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.PickingHint;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.scenegraph.shape.Sphere;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;

/**
 * Showcases the v2 interact gizmos. Hover a handle to highlight it, drag to manipulate the
 * target. Click objects to change the interact target. Press 1 for the translate gizmo, 2 for
 * rotate, 3 for scale, and R to toggle between world and local interact frames. Hold Ctrl while
 * dragging to snap: translation to a 1-unit grid, rotation to 15 degree steps.
 *
 * For unattended verification, -Dgizmo.shot=path skips the settings dialog, grabs a frame to the
 * given PNG once the scene has settled, prints a summary of gizmo-colored pixels and exits.
 * -Dgizmo.camera=far|xaxis overrides the start camera (to check constant screen sizing and axis
 * fading); -Dgizmo.hover=x simulates the mouse resting on the +X arrow (to check highlighting);
 * -Dgizmo.widget=rotate starts with the rotate gizmo active; -Dgizmo.drag=ringx simulates a slow
 * drag around the X ring (to check the pie wedge and angle readout).
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.interact.InteractGizmoExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/interact_InteractExample.jpg", //
    maxHeapMemory = 64)
public class InteractGizmoExample extends ExampleBase {

  private InteractManager manager;
  private TranslateGizmo translateGizmo;
  private RotateGizmo rotateGizmo;
  private ScaleGizmo scaleGizmo;

  private int _frames = 0;

  public static void main(final String[] args) {
    start(InteractGizmoExample.class);
  }

  @Override
  protected PropertiesGameSettings getAttributes(final PropertiesGameSettings settings) {
    if (System.getProperty("gizmo.shot") != null) {
      // Unattended screenshot run - never show the settings dialog.
      return settings;
    }
    return super.getAttributes(settings);
  }

  @Override
  protected void updateExample(final ReadOnlyTimer timer) {
    manager.update(timer);
  }

  @Override
  protected void renderExample(final Renderer renderer) {
    if (_frames == 0) {
      renderer.setBackgroundColor(new ColorRGBA(0.13f, 0.14f, 0.16f, 1f));
    }

    super.renderExample(renderer);

    _frames++;
    final String shot = System.getProperty("gizmo.shot");
    if (shot != null && _frames >= 20 && "x".equals(System.getProperty("gizmo.hover"))) {
      // Re-applied every frame: the manager's own input processing resets hover to the real
      // (absent) mouse each update.
      simulateHoverOnXArrow();
    }
    if (shot != null && _frames >= 20 && "ringx".equals(System.getProperty("gizmo.drag"))) {
      simulateDragOnXRing();
    }

    manager.render(renderer);

    if (shot != null && _frames == 40) {
      grabShot(renderer, shot);
      _exit = true;
    }
  }

  /**
   * Pretend the mouse is resting mid-shaft on the +X arrow, so a screenshot run can verify the
   * hover highlight without real input.
   */
  private void simulateHoverOnXArrow() {
    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    final Spatial target = manager.getSpatialTarget();
    final double scale = translateGizmo.getHandle().getScale().getX();
    final Vector3 onArrow = new Vector3(0.6 * scale, 0, 0).addLocal(target.getWorldTranslation());
    final Vector3 screen = cam.getScreenCoordinates(onArrow);
    final MouseState hover =
        new MouseState((int) screen.getX(), (int) screen.getY(), 0, 0, 0, null, null);
    translateGizmo.checkMouseOver(_canvas, hover, manager);
  }

  private MouseState _lastDragMouse = null;

  /**
   * Feed the rotate gizmo a synthetic left-button drag that walks around the X ring, driving the
   * full processInput path so a screenshot run can verify the pie wedge and angle readout.
   */
  private void simulateDragOnXRing() {
    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    final Spatial target = manager.getSpatialTarget();
    final ReadOnlyVector3 origin = target.getWorldTranslation();
    final double scale = rotateGizmo.getHandle().getScale().getX();

    // The point on the X ring facing the camera, swung around the ring a bit more each frame.
    final Vector3 toCam = new Vector3(cam.getDirection()).negateLocal();
    final Vector3 e1 = toCam.subtract(Vector3.UNIT_X.multiply(toCam.dot(Vector3.UNIT_X), null), null).normalizeLocal();
    final Vector3 e2 = Vector3.UNIT_X.cross(e1, null);
    final double angle = (_frames - 20) * 0.04;
    final Vector3 onRing = new Vector3(e1).multiplyLocal(Math.cos(angle) * RotateGizmo.RING_RADIUS * scale)
        .addLocal(e2.multiply(Math.sin(angle) * RotateGizmo.RING_RADIUS * scale, null)).addLocal(origin);
    final Vector3 screen = cam.getScreenCoordinates(onRing);

    final EnumMap<MouseButton, ButtonState> buttons = new EnumMap<>(MouseButton.class);
    buttons.put(MouseButton.LEFT, ButtonState.DOWN);
    // First frame: previous state has the button up at the same spot, starting the drag there.
    final MouseState previous = _lastDragMouse != null ? _lastDragMouse
        : new MouseState((int) screen.getX(), (int) screen.getY(), 0, 0, 0, null, null);
    final MouseState current = new MouseState((int) screen.getX(), (int) screen.getY(),
        (int) screen.getX() - previous.getX(), (int) screen.getY() - previous.getY(), 0, buttons, null);
    _lastDragMouse = current;

    final InputState previousState = new InputState(KeyboardState.NOTHING, previous, ControllerState.NOTHING,
        GestureState.NOTHING, CharacterInputState.NOTHING);
    final InputState currentState = new InputState(KeyboardState.NOTHING, current, ControllerState.NOTHING,
        GestureState.NOTHING, CharacterInputState.NOTHING);

    rotateGizmo.processInput(_canvas, new TwoInputStates(previousState, currentState), new AtomicBoolean(false),
        manager);
    manager.getSpatialState().applyState(target);
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Interact Gizmo Example");

    final Camera camera = _canvas.getCanvasRenderer().getCamera();
    final String cameraMode = System.getProperty("gizmo.camera", "default");
    switch (cameraMode) {
      case "far" -> camera.setLocation(36, 30, 48);
      case "xaxis" -> camera.setLocation(24, 3, 0);
      default -> camera.setLocation(12, 10, 16);
    }
    camera.lookAt(0, cameraMode.equals("xaxis") ? 3 : 2, 0, Vector3.UNIT_Y);

    addControls();
    addObjects();

    MaterialUtil.autoMaterials(_root);
  }

  private void addControls() {
    manager = new InteractManager();
    manager.setupInput(_canvas, _physicalLayer, _logicalLayer);

    translateGizmo = new TranslateGizmo().withAllHandles();
    manager.addWidget(translateGizmo);

    rotateGizmo = new RotateGizmo().withAllHandles();
    manager.addWidget(rotateGizmo);
    // the angle readout is screen-space ui - it renders with the ortho pass
    _orthoRoot.attachChild(rotateGizmo.getAngleReadout());

    scaleGizmo = new ScaleGizmo().withAllHandles();
    manager.addWidget(scaleGizmo);

    manager.setActiveWidget(switch (System.getProperty("gizmo.widget", "translate")) {
      case "rotate" -> rotateGizmo;
      case "scale" -> scaleGizmo;
      default -> translateGizmo;
    });

    // switch widgets
    manager.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.ONE),
        (source, inputStates, tpf) -> manager.setActiveWidget(translateGizmo)));
    manager.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.TWO),
        (source, inputStates, tpf) -> manager.setActiveWidget(rotateGizmo)));
    manager.getLogicalLayer().registerTrigger(new InputTrigger(new KeyPressedCondition(Key.THREE),
        (source, inputStates, tpf) -> manager.setActiveWidget(scaleGizmo)));

    // toggle world/local interact frame (the scale gizmo is always local)
    manager.getLogicalLayer()
        .registerTrigger(new InputTrigger(new KeyPressedCondition(Key.R), (source, inputStates, tpf) -> {
          final InteractMatrix next =
              translateGizmo.getInteractMatrix() == InteractMatrix.World ? InteractMatrix.Local : InteractMatrix.World;
          translateGizmo.setInteractMatrix(next);
          rotateGizmo.setInteractMatrix(next);
          manager.fireTargetDataUpdated();
        }));

    // hold Ctrl to snap: translate to a 1-unit grid, rotate to 15 degree steps
    final GridSnapFilter gridSnap = new GridSnapFilter(1.0);
    gridSnap.setEnabled(false);
    translateGizmo.addFilter(gridSnap);
    final AngleSnapFilter angleSnap = new AngleSnapFilter(15 * MathUtils.DEG_TO_RAD);
    angleSnap.setEnabled(false);
    rotateGizmo.addFilter(angleSnap);
    manager.getLogicalLayer()
        .registerTrigger(new InputTrigger(new KeyHeldCondition(Key.LEFT_CONTROL), (source, inputStates, tpf) -> {
          gridSnap.setEnabled(true);
          angleSnap.setEnabled(true);
        }));
    manager.getLogicalLayer()
        .registerTrigger(new InputTrigger(new KeyReleasedCondition(Key.LEFT_CONTROL), (source, inputStates, tpf) -> {
          gridSnap.setEnabled(false);
          angleSnap.setEnabled(false);
        }));
    if (System.getProperty("gizmo.snap") != null) {
      // Simulated-drag screenshot runs mute real input, so Ctrl can't reach the triggers.
      gridSnap.setEnabled(true);
      angleSnap.setEnabled(true);
    }
  }

  private void addObjects() {
    final Box floor = new Box("floor", Vector3.ZERO, 20, 0.5, 20);
    floor.setTranslation(0, -0.5, 0);
    final TextureState ts = new TextureState();
    ts.setTexture(TextureManager.load("models/obj/pitcher.jpg", Texture.MinificationFilter.Trilinear, true));
    floor.setRenderState(ts);
    floor.getSceneHints().setPickingHint(PickingHint.Pickable, false);
    floor.setModelBound(new BoundingBox());
    _root.attachChild(floor);

    final Box box = new Box("box", Vector3.ZERO, 3, 3, 3);
    box.setTranslation(0, 3, 0);
    TextureState boxTs = new TextureState();
    boxTs.setTexture(TextureManager.load("images/skybox/1.jpg", Texture.MinificationFilter.Trilinear, true));
    box.setRenderState(boxTs);
    box.getSceneHints().setPickingHint(PickingHint.Pickable, true);
    box.setModelBound(new BoundingBox());

    final Node boxBase = new Node("boxBase");
    boxBase.attachChild(box);
    _root.attachChild(boxBase);

    final Sphere sphere = new Sphere("sphere", Vector3.ZERO, 16, 16, 2);
    sphere.setTranslation(0, 2, 0);
    boxTs = new TextureState();
    boxTs.setTexture(TextureManager.load("images/water/dudvmap.png", Texture.MinificationFilter.Trilinear, true));
    sphere.setRenderState(boxTs);
    sphere.getSceneHints().setPickingHint(PickingHint.Pickable, true);
    sphere.setModelBound(new BoundingSphere());

    final Node sphereBase = new Node("sphereBase");
    sphereBase.setTranslation(7, 0, -4);
    sphereBase.attachChild(sphere);
    _root.attachChild(sphereBase);

    // auto select the box
    _root.updateGeometricState(0);
    manager.setSpatialTarget(boxBase);
  }

  @Override
  protected void updateLogicalLayer(final ReadOnlyTimer timer) {
    if (System.getProperty("gizmo.shot") != null && System.getProperty("gizmo.drag") != null) {
      // A simulated drag is driving the widgets; real (idle) input would end it every frame.
      return;
    }
    manager.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
  }

  @Override
  protected void processPicks(final PrimitivePickResults pickResults) {
    final PickData pick = pickResults.findFirstIntersectingPickData();
    if (pick != null) {
      final Pickable target = pick.getTarget();
      if (target instanceof Spatial) {
        manager.setSpatialTarget(((Spatial) target).getParent());
        return;
      }
    }
    manager.setSpatialTarget(null);
  }

  /**
   * Grab the framebuffer, save it as a PNG and print counts plus the bounding box of pixels
   * matching the gizmo's saturated axis colors - enough to verify the gizmo drew, and at what
   * on-screen size.
   */
  private void grabShot(final Renderer renderer, final String path) {
    final Camera cam = _canvas.getCanvasRenderer().getCamera();
    final int width = cam.getWidth();
    final int height = cam.getHeight();
    final ByteBuffer buff = BufferUtils.createByteBuffer(width * height * 3);
    renderer.grabScreenContents(buff, ImageDataFormat.RGB, 0, 0, width, height);

    int red = 0, green = 0, blue = 0, highlight = 0;
    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        final int i = (y * width + x) * 3;
        final int r = buff.get(i) & 0xFF;
        final int g = buff.get(i + 1) & 0xFF;
        final int b = buff.get(i + 2) & 0xFF;
        boolean gizmo = true;
        if (r > 170 && g < 110 && b < 130) {
          red++;
        } else if (g > 150 && r < 160 && b < 100) {
          green++;
        } else if (b > 170 && r < 110 && g < 180) {
          blue++;
        } else if (r > 200 && g > 170 && b < 110) {
          highlight++;
        } else {
          gizmo = false;
        }
        if (gizmo) {
          minX = Math.min(minX, x);
          minY = Math.min(minY, y);
          maxX = Math.max(maxX, x);
          maxY = Math.max(maxY, y);
        }
      }
    }

    System.out.println("GIZMO size=" + width + "x" + height + " redPixels=" + red + " greenPixels=" + green
        + " bluePixels=" + blue + " highlightPixels=" + highlight
        + (maxX < 0 ? " bbox=none" : " bbox=" + (maxX - minX + 1) + "x" + (maxY - minY + 1)));
    if (System.getProperty("gizmo.drag") != null) {
      final double targetAngle =
          new Quaternion().fromRotationMatrix(manager.getSpatialTarget().getRotation()).toAngleAxis(new Vector3());
      System.out.println("GIZMO dragAngle=" + rotateGizmo.getDragAngle() + " text='"
          + rotateGizmo.getAngleReadout().getText() + "' targetAngleDeg=" + targetAngle * MathUtils.RAD_TO_DEG);
    }

    try {
      final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          final int i = (y * width + x) * 3;
          final int rgb = ((buff.get(i) & 0xFF) << 16) | ((buff.get(i + 1) & 0xFF) << 8) | (buff.get(i + 2) & 0xFF);
          img.setRGB(x, height - 1 - y, rgb); // GL rows are bottom-up
        }
      }
      ImageIO.write(img, "png", new File(path));
      System.out.println("GIZMO png=" + path);
    } catch (final Exception ex) {
      System.out.println("GIZMO png write failed: " + ex);
    }
  }
}
