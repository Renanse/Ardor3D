/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.texture;

import com.ardor3d.framework.DisplaySettings;
import com.ardor3d.image.TextureCubeMap;
import com.ardor3d.image.TextureCubeMap.Face;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Renderable;
import com.ardor3d.scenegraph.Spatial;

public class CubeMapRenderUtil {
    protected TextureRenderer _textureRenderer = null;

    protected final DisplaySettings _settings;
    protected final double _near;
    protected final double _far;

    public CubeMapRenderUtil(final DisplaySettings settings, final double near, final double far) {
        _settings = settings;
        _near = near;
        _far = far;
    }

    public void init(final Renderer renderer) {
        _textureRenderer = TextureRendererFactory.INSTANCE.createTextureRenderer(_settings, renderer,
                ContextManager.getCurrentContext().getCapabilities());

        _textureRenderer.getCamera().setFrustumPerspective(90.0, 1.0, _near, _far);
    }

    public void setupTexture(final TextureCubeMap cubemap) {
        _textureRenderer.setupTexture(cubemap);
    }

    public void renderToCubeMap(final Renderable renderable, final TextureCubeMap cubemap,
            final ReadOnlyVector3 cameraPosition, final int clear) {
        final Camera cam = _textureRenderer.getCamera();
        cam.setLocation(cameraPosition);

        // render our scene from the sphere's point of view
        cam.setAxes(Vector3.NEG_UNIT_Z, Vector3.NEG_UNIT_Y, Vector3.NEG_UNIT_X);
        cubemap.setCurrentRTTFace(Face.NegativeX);
        _textureRenderer.render(renderable, cubemap, clear);

        cam.setAxes(Vector3.UNIT_Z, Vector3.NEG_UNIT_Y, Vector3.UNIT_X);
        cubemap.setCurrentRTTFace(Face.PositiveX);
        _textureRenderer.render(renderable, cubemap, clear);

        cam.setAxes(Vector3.NEG_UNIT_X, Vector3.NEG_UNIT_Z, Vector3.NEG_UNIT_Y);
        cubemap.setCurrentRTTFace(Face.NegativeY);
        _textureRenderer.render(renderable, cubemap, clear);

        cam.setAxes(Vector3.NEG_UNIT_X, Vector3.UNIT_Z, Vector3.UNIT_Y);
        cubemap.setCurrentRTTFace(Face.PositiveY);
        _textureRenderer.render(renderable, cubemap, clear);

        cam.setAxes(Vector3.UNIT_X, Vector3.NEG_UNIT_Y, Vector3.NEG_UNIT_Z);
        cubemap.setCurrentRTTFace(Face.NegativeZ);
        _textureRenderer.render(renderable, cubemap, clear);

        cam.setAxes(Vector3.NEG_UNIT_X, Vector3.NEG_UNIT_Y, Vector3.UNIT_Z);
        cubemap.setCurrentRTTFace(Face.PositiveZ);
        _textureRenderer.render(renderable, cubemap, clear);
    }

    public void renderToCubeMap(final Spatial spatial, final TextureCubeMap cubemap,
            final ReadOnlyVector3 cameraPosition, final int clear) {
        final Camera cam = _textureRenderer.getCamera();
        cam.setLocation(cameraPosition);

        // render our scene from the sphere's point of view
        cam.setAxes(Vector3.NEG_UNIT_Z, Vector3.NEG_UNIT_Y, Vector3.NEG_UNIT_X);
        cubemap.setCurrentRTTFace(Face.NegativeX);
        _textureRenderer.renderSpatial(spatial, cubemap, clear);

        cam.setAxes(Vector3.UNIT_Z, Vector3.NEG_UNIT_Y, Vector3.UNIT_X);
        cubemap.setCurrentRTTFace(Face.PositiveX);
        _textureRenderer.renderSpatial(spatial, cubemap, clear);

        cam.setAxes(Vector3.NEG_UNIT_X, Vector3.NEG_UNIT_Z, Vector3.NEG_UNIT_Y);
        cubemap.setCurrentRTTFace(Face.NegativeY);
        _textureRenderer.renderSpatial(spatial, cubemap, clear);

        cam.setAxes(Vector3.NEG_UNIT_X, Vector3.UNIT_Z, Vector3.UNIT_Y);
        cubemap.setCurrentRTTFace(Face.PositiveY);
        _textureRenderer.renderSpatial(spatial, cubemap, clear);

        cam.setAxes(Vector3.UNIT_X, Vector3.NEG_UNIT_Y, Vector3.NEG_UNIT_Z);
        cubemap.setCurrentRTTFace(Face.NegativeZ);
        _textureRenderer.renderSpatial(spatial, cubemap, clear);

        cam.setAxes(Vector3.NEG_UNIT_X, Vector3.NEG_UNIT_Y, Vector3.UNIT_Z);
        cubemap.setCurrentRTTFace(Face.PositiveZ);
        _textureRenderer.renderSpatial(spatial, cubemap, clear);
    }
}
