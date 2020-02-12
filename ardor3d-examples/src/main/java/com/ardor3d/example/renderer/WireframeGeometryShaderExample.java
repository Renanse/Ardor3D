/**
 * Copyright (c) 2008-2020 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.renderer;

import java.util.concurrent.Callable;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.framework.CanvasRenderer;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.GameTaskQueue;
import com.ardor3d.util.GameTaskQueueManager;
import com.ardor3d.util.ReadOnlyTimer;

/**
 * An example of using geometry shaders. Requires support for geometry shaders (obviously).
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.WireframeGeometryShaderExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_WireframeGeometryShaderExample.jpg", //
        maxHeapMemory = 64)
public class WireframeGeometryShaderExample extends ExampleBase {
    private Box box;
    private final Matrix3 rotation = new Matrix3();

    public static void main(final String[] args) {
        start(WireframeGeometryShaderExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        rotation.fromAngles(0.2 * timer.getTimeInSeconds(), 0.5 * timer.getTimeInSeconds(),
                0.8 * timer.getTimeInSeconds());
        box.setRotation(rotation);
    }

    @Override
    protected void initExample() {
        final Camera cam = _canvas.getCanvasRenderer().getCamera();
        final CanvasRenderer canvasRenderer = _canvas.getCanvasRenderer();
        final RenderContext renderContext = canvasRenderer.getRenderContext();
        final Renderer renderer = canvasRenderer.getRenderer();
        GameTaskQueueManager.getManager(renderContext).getQueue(GameTaskQueue.RENDER).enqueue(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                renderer.setBackgroundColor(ColorRGBA.DARK_GRAY);
                return null;
            }
        });
        _canvas.setVSyncEnabled(true);
        _canvas.setTitle("WireframeGeometryShader Test");
        cam.setLocation(new Vector3(0, 0, 50));
        cam.lookAt(new Vector3(0, 0, 0), Vector3.UNIT_Y);

        box = new Box("box", Vector3.ZERO, 10, 10, 10);
        box.setRenderMaterial("wireframe_geometry_shader_example.yaml");

        box.setProperty("wireColor", new ColorRGBA(0, 0, 1, 1));
        box.setProperty("faceColor", new ColorRGBA(1, 1, 1, 1));
        box.setProperty("factor", .1f);
        box.setProperty("scale", new Vector2(_canvas.getCanvasRenderer().getCamera().getWidth(),
                _canvas.getCanvasRenderer().getCamera().getHeight()));

        _root.attachChild(box);
    }
}
