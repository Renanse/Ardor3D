/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 * 
 * This file is part of Ardor3D.
 * 
 * Ardor3D is free software: you can redistribute it and/or modify it under the terms of its license which may be found
 * in the accompanying LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.scenegraph;

import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.RenderContext;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.RenderState.StateType;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.visitor.Visitor;

public class SceneCompiler {

    public static void compile(final Spatial scene, final Renderer renderer, final CompileOptions options) {
        // are we making a display list?
        if (options.isDisplayList()) {
            // grab our current context
            final RenderContext context = ContextManager.getCurrentContext();

            // handle camera...
            // save the current camera...
            final Camera originalCam = context.getCurrentCamera();
            // replace with a camera that will always pass frustum checks
            final Camera yesCam = new Camera(originalCam) {
                @Override
                public FrustumIntersect contains(final BoundingVolume bound) {
                    return FrustumIntersect.Inside;
                }
            };
            context.setCurrentCamera(yesCam);

            // setup for display list...
            // force all textures to load so their setup calls are not part of the displaylist
            scene.acceptVisitor(new TextureApplyVisitor(renderer), true);
            // invalidate any current opengl state information.
            context.invalidateStates();
            // generate a DL id by starting our list
            final int id = renderer.startDisplayList();
            // push our current buckets to back
            renderer.getQueue().pushBuckets();

            // render...
            // render our spatial
            scene.draw(renderer);
            // process buckets and then pop them
            renderer.renderBuckets();
            renderer.getQueue().popBuckets();

            // end list
            renderer.endDisplayList();

            // restore old camera
            context.setCurrentCamera(originalCam);

            // add a display list delegate to the given Spatial
            scene.setRenderDelegate(new DisplayListDelegate(id, context.getGlContextRep()), context.getGlContextRep());
        }
    }

    static class TextureApplyVisitor implements Visitor {
        private final Renderer _renderer;

        public TextureApplyVisitor(final Renderer renderer) {
            _renderer = renderer;
        }

        public void visit(final Spatial spatial) {
            if (spatial instanceof Mesh) {
                final Mesh mesh = (Mesh) spatial;
                final TextureState state = (TextureState) mesh.getWorldRenderState(StateType.Texture);
                if (state != null) {
                    _renderer.applyState(state.getType(), state);
                }
            }
        }

    }
}