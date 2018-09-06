/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.scenegraph.extension;

import java.io.IOException;
import java.nio.FloatBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingSphere;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.texture.TextureRenderer;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.scenegraph.shape.Quad;
import com.ardor3d.util.Timer;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * QuadImposterNode
 */
public class QuadImposterNode extends Node {

    protected TextureRenderer _tRenderer;

    protected Texture2D _texture;

    protected Node _targetScene;

    protected Quad _imposterQuad;

    protected double _redrawRate;
    protected double _elapsed;
    protected double _cameraAngleThreshold;
    protected double _cameraDistanceThreshold = Double.MAX_VALUE;
    protected boolean _haveDrawn;

    protected Vector3 _worldUpVector = new Vector3(0, 1, 0);

    protected boolean _doUpdate = true;

    protected Camera _cam;

    protected int _twidth, _theight;
    protected int _depth, _samples;

    protected final Vector3 _lastCamDir = new Vector3();
    protected double _lastCamDist;

    protected Vector3[] _corners = new Vector3[8];
    protected final Vector3 _center = new Vector3();
    protected final Vector3 _extents = new Vector3();
    protected final Vector2 _minScreenPos = new Vector2();
    protected final Vector2 _maxScreenPos = new Vector2();
    protected final Vector2 _minMaxScreenPos = new Vector2();
    protected final Vector2 _maxMinScreenPos = new Vector2();
    protected final Vector3 _tempVec = new Vector3();
    protected double _minZ;
    protected double _nearPlane;
    protected double _farPlane;
    protected Timer _timer;

    public QuadImposterNode() {
        this(null, 64, 64);
    }

    public QuadImposterNode(final String name, final int twidth, final int theight) {
        this(name, twidth, theight, null);
    }

    public QuadImposterNode(final String name, final int twidth, final int theight, final Timer timer) {
        this(name, twidth, theight, 8, 0, timer);
    }

    public QuadImposterNode(final String name, final int twidth, final int theight, final int depth, final int samples,
            final Timer timer) {
        super(name);

        _twidth = twidth;
        _theight = theight;
        _depth = depth;
        _samples = samples;

        _timer = timer;

        _texture = new Texture2D();

        _imposterQuad = new Quad("ImposterQuad");
        _imposterQuad.resize(1, 1);
        _imposterQuad.setModelBound(new BoundingBox());
        _imposterQuad.getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
        _imposterQuad.getSceneHints().setLightCombineMode(LightCombineMode.Off);
        super.attachChild(_imposterQuad);

        getSceneHints().setRenderBucketType(RenderBucketType.Transparent);

        _targetScene = new Node();
        super.attachChild(_targetScene);

        for (int i = 0; i < _corners.length; i++) {
            _corners[i] = new Vector3();
        }

        if (timer != null) {
            _redrawRate = _elapsed = 0.05; // 20x per sec
        } else {
            setCameraAngleThreshold(10.0);
            setCameraDistanceThreshold(0.2);
        }
        _haveDrawn = false;
    }

    @Override
    public int attachChild(final Spatial child) {
        return _targetScene.attachChild(child);
    }

    @Override
    public int attachChildAt(final Spatial child, final int index) {
        return _targetScene.attachChildAt(child, index);
    }

    @Override
    public void detachAllChildren() {
        _targetScene.detachAllChildren();
    }

    @Override
    public int detachChild(final Spatial child) {
        return _targetScene.detachChild(child);
    }

    @Override
    public Spatial detachChildAt(final int index) {
        return _targetScene.detachChildAt(index);
    }

    @Override
    public int detachChildNamed(final String childName) {
        return _targetScene.detachChildNamed(childName);
    }

    private void init(final Renderer renderer) {
        _tRenderer = renderer.createTextureRenderer(_twidth, _theight, _depth, _samples);

        _tRenderer.setBackgroundColor(new ColorRGBA(0, 0, 0, 0));
        resetTexture();
    }

    @Override
    public void draw(final Renderer r) {
        if (_timer != null && _redrawRate > 0) {
            _elapsed += _timer.getTimePerFrame();
        }

        if (_tRenderer == null) {
            init(r);
        }
        if (_cam == null) {
            _cam = Camera.getCurrentCamera();

            _tRenderer.getCamera().setFrustum(_cam.getFrustumNear(), _cam.getFrustumFar(), _cam.getFrustumLeft(),
                    _cam.getFrustumRight(), _cam.getFrustumTop(), _cam.getFrustumBottom());
            _tRenderer.getCamera().setFrame(_cam.getLocation(), _cam.getLeft(), _cam.getUp(), _cam.getDirection());
        }

        if (_doUpdate && (!_haveDrawn || shouldDoUpdate(_cam)) && _targetScene.getWorldBound() != null) {
            final BoundingVolume b = _targetScene.getWorldBound();
            _center.set(b.getCenter());

            updateCameraLookat();

            calculateImposter();

            updateCameraLookat();
            updateCameraFrustum();

            renderImposter();

            _haveDrawn = true;
        }

        _imposterQuad.draw(r);
    }

    @Override
    protected void updateChildren(final double time) {
        _imposterQuad.updateGeometricState(time, false);
        if (_doUpdate && (!_haveDrawn || shouldDoUpdate(_cam))) {
            _targetScene.updateGeometricState(time, false);
        }
    }

    private void calculateImposter() {
        final BoundingVolume worldBound = _targetScene.getWorldBound();
        _center.set(worldBound.getCenter());

        for (int i = 0; i < _corners.length; i++) {
            _corners[i].set(_center);
        }

        if (worldBound instanceof BoundingBox) {
            final BoundingBox bbox = (BoundingBox) worldBound;
            bbox.getExtent(_extents);
        } else if (worldBound instanceof BoundingSphere) {
            final BoundingSphere bsphere = (BoundingSphere) worldBound;
            _extents.set(bsphere.getRadius(), bsphere.getRadius(), bsphere.getRadius());
        }

        _corners[0].addLocal(_extents.getX(), _extents.getY(), -_extents.getZ());
        _corners[1].addLocal(-_extents.getX(), _extents.getY(), -_extents.getZ());
        _corners[2].addLocal(_extents.getX(), -_extents.getY(), -_extents.getZ());
        _corners[3].addLocal(-_extents.getX(), -_extents.getY(), -_extents.getZ());
        _corners[4].addLocal(_extents.getX(), _extents.getY(), _extents.getZ());
        _corners[5].addLocal(-_extents.getX(), _extents.getY(), _extents.getZ());
        _corners[6].addLocal(_extents.getX(), -_extents.getY(), _extents.getZ());
        _corners[7].addLocal(-_extents.getX(), -_extents.getY(), _extents.getZ());

        for (int i = 0; i < _corners.length; i++) {
            _tRenderer.getCamera().getScreenCoordinates(_corners[i], _corners[i]);
        }

        _minScreenPos.set(Double.MAX_VALUE, Double.MAX_VALUE);
        _maxScreenPos.set(-Double.MAX_VALUE, -Double.MAX_VALUE);
        _minZ = Double.MAX_VALUE;
        for (int i = 0; i < _corners.length; i++) {
            _minScreenPos.setX(Math.min(_corners[i].getX(), _minScreenPos.getX()));
            _minScreenPos.setY(Math.min(_corners[i].getY(), _minScreenPos.getY()));

            _maxScreenPos.setX(Math.max(_corners[i].getX(), _maxScreenPos.getX()));
            _maxScreenPos.setY(Math.max(_corners[i].getY(), _maxScreenPos.getY()));

            _minZ = Math.min(_corners[i].getZ(), _minZ);
        }
        _maxMinScreenPos.set(_maxScreenPos.getX(), _minScreenPos.getY());
        _minMaxScreenPos.set(_minScreenPos.getX(), _maxScreenPos.getY());

        _tRenderer.getCamera().getWorldCoordinates(_maxScreenPos, _minZ, _corners[0]);
        _tRenderer.getCamera().getWorldCoordinates(_maxMinScreenPos, _minZ, _corners[1]);
        _tRenderer.getCamera().getWorldCoordinates(_minScreenPos, _minZ, _corners[2]);
        _tRenderer.getCamera().getWorldCoordinates(_minMaxScreenPos, _minZ, _corners[3]);
        _center.set(_corners[0]).addLocal(_corners[1]).addLocal(_corners[2]).addLocal(_corners[3]).multiplyLocal(0.25);

        _lastCamDir.set(_center).subtractLocal(_tRenderer.getCamera().getLocation());
        _lastCamDist = _nearPlane = _lastCamDir.length();
        _farPlane = _nearPlane + _extents.length() * 2.0;
        _lastCamDir.normalizeLocal();

        final FloatBuffer vertexBuffer = _imposterQuad.getMeshData().getVertexBuffer();
        BufferUtils.setInBuffer(_corners[0], vertexBuffer, 3);
        BufferUtils.setInBuffer(_corners[1], vertexBuffer, 2);
        BufferUtils.setInBuffer(_corners[2], vertexBuffer, 1);
        BufferUtils.setInBuffer(_corners[3], vertexBuffer, 0);

        _imposterQuad.updateModelBound();
    }

    private void updateCameraLookat() {
        _tRenderer.getCamera().setLocation(_cam.getLocation());
        _tRenderer.getCamera().lookAt(_center, _worldUpVector);
    }

    private void updateCameraFrustum() {
        final double width = _corners[2].subtractLocal(_corners[1]).length() / 2.0;
        final double height = _corners[1].subtractLocal(_corners[0]).length() / 2.0;

        _tRenderer.getCamera().setFrustum(_nearPlane, _farPlane, -width, width, height, -height);
    }

    private boolean shouldDoUpdate(final Camera cam) {
        if (_redrawRate > 0 && _elapsed >= _redrawRate) {
            _elapsed = _elapsed % _redrawRate;
            return true;
        }

        if (_cameraAngleThreshold > 0) {
            _tempVec.set(_center).subtractLocal(cam.getLocation());

            final double currentDist = _tempVec.length();
            if (_lastCamDist != 0 && Math.abs(currentDist - _lastCamDist) / _lastCamDist > _cameraDistanceThreshold) {
                return true;
            }

            _tempVec.normalizeLocal();
            final double angle = _tempVec.smallestAngleBetween(_lastCamDir);
            if (angle > _cameraAngleThreshold) {
                return true;
            }
        }
        return false;
    }

    public void setRedrawRate(final double rate) {
        _redrawRate = _elapsed = rate;
    }

    public double getCameraDistanceThreshold() {
        return _cameraDistanceThreshold;
    }

    public void setCameraDistanceThreshold(final double cameraDistanceThreshold) {
        _cameraDistanceThreshold = cameraDistanceThreshold;
    }

    public double getCameraAngleThreshold() {
        return _cameraAngleThreshold;
    }

    public void setCameraAngleThreshold(final double cameraAngleThreshold) {
        _cameraAngleThreshold = cameraAngleThreshold;
    }

    public void resetTexture() {
        _texture.setWrap(Texture.WrapMode.EdgeClamp);
        _texture.setMinificationFilter(Texture.MinificationFilter.BilinearNoMipMaps);
        _texture.setMagnificationFilter(Texture.MagnificationFilter.Bilinear);
        _texture.setTextureStoreFormat(TextureStoreFormat.RGBA8);
        _tRenderer.setupTexture(_texture);
        final TextureState ts = new TextureState();
        ts.setEnabled(true);
        ts.setTexture(_texture, 0);
        _imposterQuad.setRenderState(ts);

        // Add a blending mode... This is so the background of the texture is
        // transparent.
        final BlendState as1 = new BlendState();
        as1.setBlendEnabled(true);
        as1.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        as1.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        as1.setTestEnabled(true);
        as1.setTestFunction(BlendState.TestFunction.GreaterThan);
        as1.setEnabled(true);
        _imposterQuad.setRenderState(as1);
    }

    public void renderImposter() {
        _tRenderer.renderSpatial(_targetScene, _texture, Renderer.BUFFER_COLOR_AND_DEPTH);
    }

    public Vector3 getWorldUpVector() {
        return _worldUpVector;
    }

    public void setWorldUpVector(final Vector3 worldUpVector) {
        _worldUpVector = worldUpVector;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_texture, "texture", null);
        capsule.write(_targetScene, "targetScene", null);
        capsule.write(_imposterQuad, "standIn", new Quad("ImposterQuad"));
        capsule.write(_redrawRate, "redrawRate", 0.05f);
        capsule.write(_cameraAngleThreshold, "cameraThreshold", 0);
        capsule.write(_worldUpVector, "worldUpVector", new Vector3(Vector3.UNIT_Y));
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _texture = (Texture2D) capsule.readSavable("texture", null);
        _targetScene = (Node) capsule.readSavable("targetScene", null);
        _imposterQuad = (Quad) capsule.readSavable("standIn", new Quad("ImposterQuad"));
        _redrawRate = capsule.readFloat("redrawRate", 0.05f);
        _cameraAngleThreshold = capsule.readFloat("cameraThreshold", 0);
        _worldUpVector = (Vector3) capsule.readSavable("worldUpVector", new Vector3(Vector3.UNIT_Y));
    }

    public Texture getTexture() {
        return _texture;
    }

    public void setDoUpdate(final boolean doUpdate) {
        _doUpdate = doUpdate;
    }

    public boolean isDoUpdate() {
        return _doUpdate;
    }
}
