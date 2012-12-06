/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
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
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
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

    // passthru vertex shader
    private static final String s_vert_passthru_120 = //
    "#version 120\n" + //
            "" + //
            "void main()" + //
            "{" + //
            "  gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;" + //
            "}";

    private static final String s_geom_wireframe_150 = //
    "#version 150\n" + //
            "layout(triangles) in;" + //
            "layout(triangle_strip, max_vertices = 3) out;" + //
            "" + //
            "uniform vec2 scale;" + //
            "out vec3 dist;" + //
            "" + //
            "void main()" + //
            "{" + //
            "  vec2 p0 = scale * gl_in[0].gl_Position.xy/gl_in[0].gl_Position.w;" + //
            "  vec2 p1 = scale * gl_in[1].gl_Position.xy/gl_in[1].gl_Position.w;" + //
            "  vec2 p2 = scale * gl_in[2].gl_Position.xy/gl_in[2].gl_Position.w;" + //
            "" + //
            "  vec2 v0 = p2-p1;" + //
            "  vec2 v1 = p2-p0;" + //
            "  vec2 v2 = p1-p0;" + //
            "  float area = abs(v1.x*v2.y - v1.y*v2.x);" + //
            "  dist = vec3(area/length(v0),0,0);" + //
            "  gl_Position = gl_in[0].gl_Position; EmitVertex();" + //
            "  dist = vec3(0,area/length(v1),0);" + //
            "  gl_Position = gl_in[1].gl_Position; EmitVertex();" + //
            "  dist = vec3(0,0,area/length(v2));" + //
            "  gl_Position = gl_in[2].gl_Position; EmitVertex();" + //
            "  EndPrimitive();" + //
            "}";

    private static final String s_frag_wireframe_120 = "#version 120\n" + //
            "in vec3 dist;" + //
            "uniform vec4 wireColor;" + //
            "uniform vec4 faceColor;" + //
            "uniform float factor;" + //
            "" + //
            "void main()" + //
            "{" + //
            "  float d = min(dist[0],min(dist[1],dist[2]));" + //
            "  float I = exp2(-factor*d*d);" + //
            "  gl_FragColor = I*wireColor + (1.0 - I)*faceColor;" + //
            "}";

    private GLSLShaderObjectsState _wireframeShaderState;

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

        buildShader();

        box = new Box("box", Vector3.ZERO, 10, 10, 10);
        box.setRenderState(_wireframeShaderState);
        _root.attachChild(box);
    }

    private void buildShader() {
        final Vector2 scale = new Vector2(_canvas.getCanvasRenderer().getCamera().getWidth(), _canvas
                .getCanvasRenderer().getCamera().getHeight());

        _wireframeShaderState = new GLSLShaderObjectsState();
        _wireframeShaderState.setVertexShader(s_vert_passthru_120);
        _wireframeShaderState.setGeometryShader(s_geom_wireframe_150);
        _wireframeShaderState.setFragmentShader(s_frag_wireframe_120);
        _wireframeShaderState.setUniform("wireColor", new ColorRGBA(0, 0, 0, 1));
        _wireframeShaderState.setUniform("faceColor", new ColorRGBA(1, 1, 1, 1));
        _wireframeShaderState.setUniform("factor", 2f);
        _wireframeShaderState.setUniform("scale", scale);
    }
}
