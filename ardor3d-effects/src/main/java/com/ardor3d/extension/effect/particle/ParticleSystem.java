/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.particle;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.logging.Logger;

import com.ardor3d.extension.effect.particle.emitter.MeshEmitter;
import com.ardor3d.extension.effect.particle.emitter.SavableParticleEmitter;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Transform;
import com.ardor3d.math.Triangle;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.math.type.ReadOnlyTransform;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.ComplexSpatialController.RepeatType;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.event.DirtyType;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;
import com.ardor3d.util.geom.BufferUtils;

/**
 * ParticleSystem is an abstract class representing a particle system. A ParticleController must be attached for the
 * effect to be complete.
 */
public abstract class ParticleSystem extends Node {
    private static final Logger logger = Logger.getLogger(ParticleSystem.class.getName());

    public enum ParticleType {
        Triangle, Point, Line, GeomMesh;
    }

    protected static final double DEFAULT_END_SIZE = 4;
    protected static final double DEFAULT_START_SIZE = 20;
    protected static final double DEFAULT_MAX_ANGLE = 0.7853982;
    protected static final double DEFAULT_MAX_LIFE = 3000;
    protected static final double DEFAULT_MIN_LIFE = 2000;

    protected static final ReadOnlyColorRGBA DEFAULT_START_COLOR = new ColorRGBA(1.0f, 0.0f, 0.0f, 1.0f);
    protected static final ReadOnlyColorRGBA DEFAULT_END_COLOR = new ColorRGBA(1.0f, 1.0f, 0.0f, 0.0f);

    protected ParticleType _particleType;
    protected SavableParticleEmitter _particleEmitter;
    protected boolean _cameraFacing = true;
    protected boolean _velocityAligned = false;
    protected boolean _particlesInWorldCoords = true;

    protected double _startSize, _endSize;
    protected final ColorRGBA _startColor = new ColorRGBA(DEFAULT_START_COLOR);
    protected final ColorRGBA _endColor = new ColorRGBA(DEFAULT_END_COLOR);
    protected ParticleAppearanceRamp _ramp = new ParticleAppearanceRamp();
    protected TexAnimation _texAnimation = new TexAnimation();
    protected double _initialVelocity;
    protected double _minimumLifeTime, _maximumLifeTime;
    protected double _minimumAngle, _maximumAngle;
    protected double _startSpin, _endSpin;
    protected double _startMass, _endMass;
    protected int _startTexIndex, _texQuantity;
    protected final Vector3 _emissionDirection = new Vector3(Vector3.UNIT_Y);
    protected final Transform _emitterTransform = new Transform();
    protected final Vector3 _worldEmit = new Vector3();
    protected int _numParticles;
    protected boolean _rotateWithScene = false;
    protected final Matrix3 _rotMatrix = new Matrix3();
    protected double _particleOrientation;

    protected FloatBuffer _geometryCoordinates;
    protected FloatBuffer _appearanceColors;

    // vectors to prevent repeated object creation:
    protected final Vector3 _upXemit = new Vector3(), _absUpVector = new Vector3(), _abUpMinUp = new Vector3();
    protected final Vector3 _upVector = new Vector3(Vector3.UNIT_Y);
    protected final Vector3 _leftVector = new Vector3(-1, 0, 0);
    protected final Vector3 _invScale = new Vector3();

    // These vectors are used for determining particle orientation if you turn off camera facing
    protected final Vector3 _facingUpVector = new Vector3(Vector3.UNIT_Y);
    protected final Vector3 _facingLeftVector = new Vector3(-1, 0, 0);

    protected Particle _particles[];

    // protected Vector3 particleSpeed;
    protected int _releaseRate; // particles per second
    protected final Vector3 _originOffset = new Vector3();
    protected final Vector3 _originCenter = new Vector3();

    protected Mesh _particleMesh;
    protected ParticleController _controller;

    protected Vector3 _oldEmit = new Vector3(Float.NaN, Float.NaN, Float.NaN);
    protected double _matData[] = new double[9];

    public ParticleSystem() {}

    public ParticleSystem(final String name, final int numParticles) {
        this(name, numParticles, ParticleType.Triangle);
    }

    public ParticleSystem(final String name, final int numParticles, final ParticleType particleType) {
        super(name);
        _numParticles = numParticles;
        _particleType = particleType;
        _minimumLifeTime = DEFAULT_MIN_LIFE;
        _maximumLifeTime = DEFAULT_MAX_LIFE;
        _maximumAngle = DEFAULT_MAX_ANGLE;
        _startSize = DEFAULT_START_SIZE;
        _endSize = DEFAULT_END_SIZE;
        _startSpin = 0;
        _endSpin = 0;
        _startMass = 1;
        _endMass = 1;
        _startTexIndex = 0;
        _texQuantity = 1;
        _releaseRate = numParticles;
        _initialVelocity = 1.0;

        initializeParticles(numParticles);
    }

    protected abstract void initializeParticles(int numParticles);

    public static int getVertsForParticleType(final ParticleType type) {
        if (type == null) {
            throw new NullPointerException("type is null");
        }
        switch (type) {
            case Triangle:
            case GeomMesh:
                return 3;
            case Point:
                return 1;
            case Line:
                return 2;
        }
        throw new IllegalArgumentException("Invalid ParticleType: " + type);
    }

    public void forceRespawn() {
        for (final Particle p : _particles) {
            p.recreateParticle(0);
            p.setStatus(Particle.Status.Alive);
            p.updateAndCheck(1);
            p.setStatus(Particle.Status.Available);
        }

        if (_controller != null) {
            _controller.setActive(true);
            _controller.resetFlowCount();
        }
    }

    /**
     * Setup the rotation matrix used to determine initial particle velocity based on emission angle and emission
     * direction. called automatically by the set* methods for those parameters.
     */
    public void updateRotationMatrix() {

        if (_oldEmit.equals(_worldEmit)) {
            return;
        }

        final double upDotEmit = _upVector.dot(_worldEmit);
        if (Math.abs(upDotEmit) > 1.0 - MathUtils.EPSILON) {
            _absUpVector.setX(_upVector.getX() <= 0.0 ? -_upVector.getX() : _upVector.getX());
            _absUpVector.setY(_upVector.getY() <= 0.0 ? -_upVector.getY() : _upVector.getY());
            _absUpVector.setZ(_upVector.getZ() <= 0.0 ? -_upVector.getZ() : _upVector.getZ());
            if (_absUpVector.getX() < _absUpVector.getY()) {
                if (_absUpVector.getX() < _absUpVector.getZ()) {
                    _absUpVector.set(Vector3.UNIT_X);
                } else {
                    _absUpVector.set(Vector3.UNIT_Z);
                }
            } else if (_absUpVector.getY() < _absUpVector.getZ()) {
                _absUpVector.set(Vector3.UNIT_Y);
            } else {
                _absUpVector.set(Vector3.UNIT_Z);
            }
            _absUpVector.subtract(_upVector, _abUpMinUp);
            _absUpVector.subtract(_worldEmit, _upXemit);
            final double f4 = 2.0 / _abUpMinUp.dot(_abUpMinUp);
            final double f6 = 2.0 / _upXemit.dot(_upXemit);
            final double f8 = f4 * f6 * _abUpMinUp.dot(_upXemit);
            final double af1[] = { _abUpMinUp.getX(), _abUpMinUp.getY(), _abUpMinUp.getZ() };
            final double af2[] = { _upXemit.getX(), _upXemit.getY(), _upXemit.getZ() };
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    _matData[(i * 3) + j] = (-f4 * af1[i] * af1[j] - f6 * af2[i] * af2[j]) + f8 * af2[i] * af1[j];

                }
                _matData[(i * 3) + i]++;
            }

        } else {
            _upVector.cross(_worldEmit, _upXemit);
            final double f2 = 1.0 / (1.0 + upDotEmit);
            final double f5 = f2 * _upXemit.getX();
            final double f7 = f2 * _upXemit.getZ();
            final double f9 = f5 * _upXemit.getY();
            final double f10 = f5 * _upXemit.getZ();
            final double f11 = f7 * _upXemit.getY();
            _matData[0] = upDotEmit + f5 * _upXemit.getX();
            _matData[1] = f9 - _upXemit.getZ();
            _matData[2] = f10 + _upXemit.getY();
            _matData[3] = f9 + _upXemit.getZ();
            _matData[4] = upDotEmit + f2 * _upXemit.getY() * _upXemit.getY();
            _matData[5] = f11 - _upXemit.getX();
            _matData[6] = f10 - _upXemit.getY();
            _matData[7] = f11 + _upXemit.getX();
            _matData[8] = upDotEmit + f7 * _upXemit.getZ();
        }
        _rotMatrix.fromArray(_matData);
        _oldEmit.set(_worldEmit);
    }

    public abstract Mesh getParticleGeometry();

    public ParticleController getParticleController() {
        return _controller;
    }

    @Override
    public void addController(final SpatialController<?> c) {
        super.addController(c);
        if (c instanceof ParticleController) {
            _controller = (ParticleController) c;
        }
    }

    public Vector3 getEmissionDirection() {
        return _emissionDirection;
    }

    public void setEmissionDirection(final ReadOnlyVector3 emissionDirection) {
        _emissionDirection.set(emissionDirection);
        _worldEmit.set(emissionDirection);
    }

    public double getEndSize() {
        return _endSize;
    }

    public void setEndSize(final double size) {
        _endSize = size >= 0.0 ? size : 0.0;
    }

    public double getStartSize() {
        return _startSize;
    }

    public void setStartSize(final double size) {
        _startSize = size >= 0.0 ? size : 0.0;
    }

    /**
     * Set the start color for particles.
     *
     * @param color
     *            The new start color.
     */
    public void setStartColor(final ReadOnlyColorRGBA color) {
        _startColor.set(color);
    }

    /**
     * @return The start color.
     */
    public ReadOnlyColorRGBA getStartColor() {
        return _startColor;
    }

    /**
     * Set the end color for particles. The base color of the particle will linearly approach this color from the start
     * color over the lifetime of the particle.
     *
     * @param color
     *            ColorRGBA The ending color.
     */
    public void setEndColor(final ReadOnlyColorRGBA color) {
        _endColor.set(color);
    }

    /**
     * getEndColor returns the ending color.
     *
     * @return The ending color
     */
    public ReadOnlyColorRGBA getEndColor() {
        return _endColor;
    }

    /**
     * Set the start and end spinSpeed of particles managed by this manager. Setting it to 0 means no spin.
     *
     * @param speed
     *            double
     */
    public void setParticleSpinSpeed(final double speed) {
        _startSpin = speed;
        _endSpin = speed;
    }

    public Vector3 getInvScale() {
        return _invScale;
    }

    public void setInvScale(final ReadOnlyVector3 invScale) {
        _invScale.set(invScale);
    }

    public void updateInvScale() {
        _invScale.set(1.0 / getWorldScale().getX(), 1.0 / getWorldScale().getY(), 1.0 / getWorldScale().getZ());
    }

    /**
     * Add an external influence to the particle controller for this mesh.
     *
     * @param influence
     *            ParticleInfluence
     */
    public void addInfluence(final ParticleInfluence influence) {
        _controller.addInfluence(influence);
    }

    /**
     * Remove an influence from the particle controller for this mesh.
     *
     * @param influence
     *            ParticleInfluence
     * @return true if found and removed.
     */
    public boolean removeInfluence(final ParticleInfluence influence) {
        return _controller.removeInfluence(influence);
    }

    /**
     * Returns the list of influences acting on this particle controller.
     *
     * @return ArrayList
     */
    public List<ParticleInfluence> getInfluences() {
        return _controller.getInfluences();
    }

    public void clearInfluences() {
        _controller.clearInfluences();
    }

    public void setParticleMass(final double mass) {
        _startMass = _endMass = mass;
    }

    /**
     * Set the minimum angle (in radians) that particles can be emitted away from the emission direction. Any angle less
     * than 0 is trimmed to 0.
     *
     * @param f
     *            The new emission minimum angle.
     */
    public void setMinimumAngle(final double f) {
        _minimumAngle = f >= 0.0 ? f : 0.0;
    }

    /**
     * getEmissionMinimumAngle returns the minimum emission angle.
     *
     * @return The minimum emission angle.
     */
    public double getMinimumAngle() {
        return _minimumAngle;
    }

    /**
     * Set the maximum angle (in radians) that particles can be emitted away from the emission direction. Any angle less
     * than 0 is trimmed to 0.
     *
     * @param f
     *            The new emission maximum angle.
     */
    public void setMaximumAngle(final double f) {
        _maximumAngle = f >= 0.0 ? f : 0.0;
    }

    /**
     * getEmissionMaximumAngle returns the maximum emission angle.
     *
     * @return The maximum emission angle.
     */
    public double getMaximumAngle() {
        return _maximumAngle;
    }

    /**
     * Set the minimum lifespan of new particles (or recreated) managed by this manager. if a value less than zero is
     * given, 1.0 is used.
     *
     * @param lifeSpan
     *            in ms
     */
    public void setMinimumLifeTime(final double lifeSpan) {
        _minimumLifeTime = lifeSpan >= 0.0 ? lifeSpan : 1.0;
    }

    /**
     * getParticlesMinimumLifeTime returns the minimum life time of a particle.
     *
     * @return The current minimum life time in ms.
     */
    public double getMinimumLifeTime() {
        return _minimumLifeTime;
    }

    /**
     * Set the maximum lifespan of new particles (or recreated) managed by this manager. if a value less than zero is
     * given, 1.0 is used.
     *
     * @param lifeSpan
     *            in ms
     */
    public void setMaximumLifeTime(final double lifeSpan) {
        _maximumLifeTime = lifeSpan >= 0.0 ? lifeSpan : 1.0;
    }

    /**
     * getParticlesMaximumLifeTime returns the maximum life time of a particle.
     *
     * @return The current maximum life time in ms.
     */
    public double getMaximumLifeTime() {
        return _maximumLifeTime;
    }

    public ReadOnlyMatrix3 getRotMatrix() {
        return _rotMatrix;
    }

    public void setRotMatrix(final ReadOnlyMatrix3 rotMatrix) {
        _rotMatrix.set(rotMatrix);
    }

    public ReadOnlyTransform getEmitterTransform() {
        return _emitterTransform;
    }

    public void setEmitterTransform(final ReadOnlyTransform emitterTransform) {
        _emitterTransform.set(emitterTransform);
    }

    public double getParticleOrientation() {
        return _particleOrientation;
    }

    public void setParticleOrientation(final double orient) {
        _particleOrientation = orient;
    }

    /**
     * Set the acceleration for any new particles created (or recreated) by this manager.
     *
     * @param velocity
     *            particle v0
     */
    public void setInitialVelocity(final double velocity) {
        _initialVelocity = velocity;
    }

    /**
     * Get the acceleration set in this manager.
     *
     * @return The initialVelocity
     */
    public double getInitialVelocity() {
        return _initialVelocity;
    }

    /**
     * Set the offset for any new particles created (or recreated) by this manager. This is applicable only to managers
     * generating from a point (not a line, rectangle, etc..)
     *
     * @param offset
     *            new offset position
     */
    public void setOriginOffset(final ReadOnlyVector3 offset) {
        _originOffset.set(offset);
    }

    /**
     * Get the offset point set in this manager.
     *
     * @return origin
     */
    public ReadOnlyVector3 getOriginOffset() {
        return _originOffset;
    }

    public ReadOnlyVector3 getWorldEmit() {
        return _worldEmit;
    }

    public void setWorldEmit(final ReadOnlyVector3 worldEmit) {
        _worldEmit.set(worldEmit);
    }

    /**
     * Get the number of particles the manager should release per second.
     *
     * @return The number of particles that should be released per second.
     */
    public int getReleaseRate() {
        return _releaseRate;
    }

    /**
     * Set the number of particles the manager should release per second.
     *
     * @param particlesPerSecond
     *            number of particles per second
     */
    public void setReleaseRate(final int particlesPerSecond) {
        final int oldRate = _releaseRate;
        _releaseRate = particlesPerSecond;
        if (_controller != null && !_controller.isActive() && _controller.isControlFlow() && oldRate == 0) {
            _controller.setActive(true);
        }
    }

    public double getEndMass() {
        return _endMass;
    }

    public void setEndMass(final double endMass) {
        _endMass = endMass;
    }

    public double getEndSpin() {
        return _endSpin;
    }

    public void setEndSpin(final double endSpin) {
        _endSpin = endSpin;
    }

    public double getStartMass() {
        return _startMass;
    }

    public void setStartMass(final double startMass) {
        _startMass = startMass;
    }

    public double getStartSpin() {
        return _startSpin;
    }

    public void setStartSpin(final double startSpin) {
        _startSpin = startSpin;
    }

    public int getTexQuantity() {
        return _texQuantity;
    }

    public void setTexQuantity(final int quantity) {
        _texQuantity = quantity;
    }

    public int getStartTexIndex() {
        return _startTexIndex;
    }

    public void setStartTexIndex(final int startTexIndex) {
        _startTexIndex = startTexIndex;
    }

    /**
     * Get which particle type is being used by the underlying system. One of ParticleType.Triangle, ParticleType.Point,
     * ParticleType.Line, ParticleType.GeomMesh
     *
     * @return An enum representing the type of particle we are emitting.
     */
    public ParticleType getParticleType() {
        return _particleType;
    }

    /**
     * Set what type of particle to emit from this system. Does not have an effect unless recreate is called.
     *
     * @param type
     *            particle type to use, should be one of ParticleType.Triangle, ParticleType.Point, ParticleType.Line,
     *            ParticleType.GeomMesh
     */
    public void setParticleType(final ParticleType type) {
        _particleType = type;
    }

    /**
     * Set our particle emitter.
     *
     * @param emitter
     *            New emitter or null for default point emitter.
     */
    public void setParticleEmitter(final SavableParticleEmitter emitter) {
        _particleEmitter = emitter;
    }

    /**
     * @return the set particle emitter, or null if none is set.
     */
    public SavableParticleEmitter getParticleEmitter() {
        return _particleEmitter;
    }

    public void initAllParticlesLocation() {
        for (int i = _particles.length; --i >= 0;) {
            initParticleLocation(i);
            _particles[i].updateVerts(null);
        }
        getParticleGeometry().updateModelBound();
    }

    @Override
    public void onDraw(final Renderer r) {
        // make sure our particle world bound is correct
        updateWorldBoundManually();

        super.onDraw(r);
    };

    public void initParticleLocation(final int index) {
        final Particle p = _particles[index];
        if (getParticleType() == ParticleType.GeomMesh && getParticleEmitter() instanceof MeshEmitter) {
            final MeshEmitter emitter = (MeshEmitter) getParticleEmitter();
            final Mesh mesh = emitter.getSource();

            // Update the triangle model on each new particle creation.
            final Vector3[] vertices = new Vector3[3];
            final MeshData mData = mesh.getMeshData();
            for (int x = 0; x < 3; x++) {
                vertices[x] = new Vector3();

                final int vertIndex = mData.getVertexIndex(index, x, 0);
                BufferUtils.populateFromBuffer(vertices[x], mData.getVertexBuffer(),
                        mData.getIndices() != null ? mData.getIndices().get(vertIndex) : vertIndex);
            }
            Triangle t = p.getTriangleModel();
            if (t == null) {
                t = new Triangle(vertices[0], vertices[1], vertices[2]);
            } else {
                t.setA(vertices[0]);
                t.setB(vertices[1]);
                t.setC(vertices[2]);
            }
            // turn the triangle corners into vector offsets from center
            for (int x = 0; x < 3; x++) {
                vertices[x].subtract(t.getCenter(), vertices[x]);
                t.set(x, vertices[x]);
            }
            p.setTriangleModel(t);
            mesh.localToWorld(t.getCenter(), p.getPosition());
            p.getPosition().multiplyLocal(getInvScale());

        } else if (getParticleEmitter() instanceof MeshEmitter) {
            final MeshEmitter emitter = (MeshEmitter) getParticleEmitter();
            final Mesh mesh = emitter.getSource();
            mesh.getMeshData().randomPointOnPrimitives(p.getPosition());
            mesh.localToWorld(p.getPosition(), p.getPosition());
            p.getPosition().multiplyLocal(getInvScale());
        } else {
            if (getParticleEmitter() != null) {
                getParticleEmitter().randomEmissionPoint(p.getPosition());
            } else {
                p.getPosition().set(_originOffset);
            }

            _emitterTransform.applyForward(p.getPosition());
            p.getPosition().divideLocal(_emitterTransform.getScale());
        }
    }

    public boolean isCameraFacing() {
        return _cameraFacing;
    }

    public void setCameraFacing(final boolean cameraFacing) {
        _cameraFacing = cameraFacing;
    }

    public boolean isVelocityAligned() {
        return _velocityAligned;
    }

    public void setVelocityAligned(final boolean velocityAligned) {
        _velocityAligned = velocityAligned;
    }

    public Particle getParticle(final int i) {
        return _particles[i];
    }

    public boolean isActive() {
        return _controller.isActive();
    }

    public void setSpeed(final double f) {
        _controller.setSpeed(f);
    }

    public void setRepeatType(final RepeatType type) {
        _controller.setRepeatType(type);
    }

    public void setControlFlow(final boolean b) {
        _controller.setControlFlow(b);
    }

    public ReadOnlyVector3 getOriginCenter() {
        return _originCenter;
    }

    public ReadOnlyVector3 getUpVector() {
        return _upVector;
    }

    public void setUpVector(final ReadOnlyVector3 vector) {
        _upVector.set(vector);
    }

    public ReadOnlyVector3 getLeftVector() {
        return _leftVector;
    }

    public void setLeftVector(final ReadOnlyVector3 vector) {
        _leftVector.set(vector);
    }

    public ReadOnlyVector3 getFacingUpVector() {
        return _facingUpVector;
    }

    /**
     * Used to determine particle orientation (only if cameraFacing is false.)
     *
     * @param vector
     */
    public void setFacingUpVector(final ReadOnlyVector3 vector) {
        _facingUpVector.set(vector);
    }

    public ReadOnlyVector3 getFacingLeftVector() {
        return _facingLeftVector;
    }

    /**
     * Used to determine particle orientation (only if cameraFacing is false.)
     *
     * @param vector
     */
    public void setFacingLeftVector(final ReadOnlyVector3 vector) {
        _facingLeftVector.set(vector);
    }

    public boolean isRotateWithScene() {
        return _rotateWithScene;
    }

    public void setRotateWithScene(final boolean rotate) {
        _rotateWithScene = rotate;
    }

    public void resetParticleVelocity(final int i) {
        getRandomVelocity(_particles[i].getVelocity());
    }

    /**
     * Returns a random angle between the min and max angles.
     *
     * @return the random angle.
     */
    public double getRandomAngle() {
        return getMinimumAngle() + MathUtils.nextRandomFloat() * (getMaximumAngle() - getMinimumAngle());
    }

    /**
     * generate a random lifespan between the min and max lifespan of the particle system.
     *
     * @return the generated lifespan value
     */
    public double getRandomLifeSpan() {
        return getMinimumLifeTime() + ((getMaximumLifeTime() - getMinimumLifeTime()) * MathUtils.nextRandomFloat());
    }

    /**
     * Generate a random velocity within the parameters of max angle and the rotation matrix.
     *
     * @param store
     *            a vector to store the results in.
     */
    protected Vector3 getRandomVelocity(final Vector3 store) {
        final double randDir = MathUtils.TWO_PI * MathUtils.nextRandomFloat();
        final double randAngle = getRandomAngle();
        Vector3 result = store;
        if (result == null) {
            result = new Vector3();
        }
        result.setX(MathUtils.cos(randDir) * MathUtils.sin(randAngle));
        result.setY(MathUtils.cos(randAngle));
        result.setZ(MathUtils.sin(randDir) * MathUtils.sin(randAngle));
        rotateVectorSpeed(result);
        result.multiplyLocal(getInitialVelocity());
        return result;
    }

    /**
     * Apply the rotation matrix to a given vector representing a particle velocity.
     *
     * @param pSpeed
     *            the velocity vector to be modified.
     */
    protected void rotateVectorSpeed(final Vector3 pSpeed) {

        final double x = pSpeed.getX(), y = pSpeed.getY(), z = pSpeed.getZ();

        pSpeed.setX(-1 * ((_rotMatrix.getM00() * x) + (_rotMatrix.getM10() * y) + (_rotMatrix.getM20() * z)));
        pSpeed.setY((_rotMatrix.getM01() * x) + (_rotMatrix.getM11() * y) + (_rotMatrix.getM21() * z));
        pSpeed.setZ(-1 * ((_rotMatrix.getM02() * x) + (_rotMatrix.getM12() * y) + (_rotMatrix.getM22() * z)));
    }

    public void warmUp(final int iterations) {
        if (_controller != null) {
            _controller.warmUp(iterations, this);
        }
    }

    public int getNumParticles() {
        return _numParticles;
    }

    public void setNumParticles(final int numParticles) {
        _numParticles = numParticles;
    }

    public double getReleaseVariance() {
        if (_controller != null) {
            return _controller.getReleaseVariance();
        }
        return 0;
    }

    public void setReleaseVariance(final double var) {
        if (_controller != null) {
            _controller.setReleaseVariance(var);
        }
    }

    public ParticleAppearanceRamp getRamp() {
        return _ramp;
    }

    public void setRamp(final ParticleAppearanceRamp ramp) {
        if (ramp == null) {
            logger.warning("Can not set a null ParticleAppearanceRamp.");
            return;
        }
        _ramp = ramp;
    }

    public TexAnimation getTexAnimation() {
        return _texAnimation;
    }

    public void setTexAnimation(final TexAnimation texAnimation) {
        if (texAnimation == null) {
            logger.warning("Can not set a null TexAnimation.");
            return;
        }
        _texAnimation = texAnimation;
    }

    /**
     * @return true if the particles are already in world coordinate space (default). When true, scene-graph transforms
     *         will only affect the emission of particles, not particles that are already living.
     */
    public boolean isParticlesInWorldCoords() {
        return _particlesInWorldCoords;
    }

    public void setParticlesInWorldCoords(final boolean particlesInWorldCoords) {
        _particlesInWorldCoords = particlesInWorldCoords;
    }

    /**
     * Changes the number of particles in this particle mesh.
     *
     * @param count
     *            the desired number of particles to change to.
     */
    public void recreate(final int count) {
        _numParticles = count;
        initializeParticles(_numParticles);
    }

    @Override
    public void updateWorldBound(final boolean recurse) {
        ; // ignore this since we want it to happen only when we say it can
          // happen due to world vectors not being used
    }

    public void updateWorldBoundManually() {
        super.updateWorldBound(true);
    }

    @Override
    public void updateGeometricState(final double time, final boolean initiator) {
        super.updateGeometricState(time, initiator);
        if (isRotateWithScene()) {
            // XXX: Perhaps we can avoid this special case via an addition to the interface?
            if (getParticleEmitter() instanceof MeshEmitter) {
                ((MeshEmitter) getParticleEmitter()).getSource().getWorldRotation().applyPost(_emissionDirection,
                        _worldEmit);
            } else {
                getWorldRotation().applyPost(_emissionDirection, _worldEmit);
            }
        } else {
            _worldEmit.set(_emissionDirection);
        }

        if (_particlesInWorldCoords) {
            final Transform t = Transform.fetchTempInstance();
            t.setIdentity();
            t.setTranslation(getWorldTranslation());
            t.setScale(getScale());
            if (getParent() != null) {
                t.setRotation(getParent().getWorldRotation());
            }
            _emitterTransform.set(t);
            Transform.releaseTempInstance(t);

            _originCenter.set(getWorldTranslation()).addLocal(_originOffset);

            setWorldTranslation(Vector3.ZERO);
        } else {
            _originCenter.set(_originOffset);
        }

        setWorldRotation(Matrix3.IDENTITY);
        setWorldScale(getScale());
        markDirty(DirtyType.Transform);
    }

    @Override
    public ParticleSystem makeCopy(final boolean shareGeometricData) {
        synchronized (this) {
            // Do not call make copy on the "generated" particle geometry.
            final Spatial geom = getParticleGeometry();
            detachChild(geom);
            final ParticleSystem copy = (ParticleSystem) super.makeCopy(shareGeometricData);
            // Reattach
            attachChild(geom);
            return copy;
        }
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        synchronized (this) {
            // Do not save the "generated" particle geometry.
            final Spatial geom = getParticleGeometry();
            detachChild(geom);
            super.write(capsule);
            // Reattach
            attachChild(geom);
        }

        capsule.write(_particleType, "particleType", ParticleType.Triangle);
        capsule.write(_particleEmitter, "particleEmitter", null);
        capsule.write(_startSize, "startSize", DEFAULT_START_SIZE);
        capsule.write(_endSize, "endSize", DEFAULT_END_SIZE);
        capsule.write(_startColor, "startColor", new ColorRGBA(DEFAULT_START_COLOR));
        capsule.write(_endColor, "endColor", new ColorRGBA(DEFAULT_END_COLOR));
        capsule.write(_startSpin, "startSpin", 0);
        capsule.write(_endSpin, "endSpin", 0);
        capsule.write(_startMass, "startMass", 0);
        capsule.write(_endMass, "endMass", 0);
        capsule.write(_startTexIndex, "startTexIndex", 0);
        capsule.write(_texQuantity, "texQuantity", 1);
        capsule.write(_initialVelocity, "initialVelocity", 1);
        capsule.write(_minimumLifeTime, "minimumLifeTime", DEFAULT_MIN_LIFE);
        capsule.write(_maximumLifeTime, "maximumLifeTime", DEFAULT_MAX_LIFE);
        capsule.write(_minimumAngle, "minimumAngle", 0);
        capsule.write(_maximumAngle, "maximumAngle", DEFAULT_MAX_ANGLE);
        capsule.write(_emissionDirection, "emissionDirection", new Vector3(Vector3.UNIT_Y));
        capsule.write(_worldEmit, "worldEmit", new Vector3(Vector3.ZERO));
        capsule.write(_upVector, "upVector", new Vector3(Vector3.UNIT_Y));
        capsule.write(_leftVector, "leftVector", new Vector3(-1, 0, 0));
        capsule.write(_facingUpVector, "facingUpVector", new Vector3(Vector3.UNIT_Y));
        capsule.write(_facingLeftVector, "facingLeftVector", new Vector3(-1, 0, 0));
        capsule.write(_numParticles, "numParticles", 0);
        capsule.write(_particleOrientation, "particleOrientation", 0);
        capsule.write(_rotateWithScene, "rotateWithScene", false);
        capsule.write(_geometryCoordinates, "geometryCoordinates", null);
        capsule.write(_appearanceColors, "appearanceColors", null);
        capsule.write(_releaseRate, "releaseRate", _numParticles);
        capsule.write(_originCenter, "originCenter", new Vector3(Vector3.ZERO));
        capsule.write(_originOffset, "originOffset", new Vector3(Vector3.ZERO));
        capsule.write(_controller, "controller", null);
        capsule.write(_cameraFacing, "cameraFacing", true);
        capsule.write(_velocityAligned, "velocityAligned", false);
        capsule.write(_particlesInWorldCoords, "particlesInWorldCoords", true);
        capsule.write(_ramp, "ramp", new ParticleAppearanceRamp());
        capsule.write(_texAnimation, "texAnimation", new TexAnimation());
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _particleType = capsule.readEnum("particleType", ParticleType.class, ParticleType.Triangle);
        _particleEmitter = (SavableParticleEmitter) capsule.readSavable("particleEmitter", null);
        _startSize = capsule.readDouble("startSize", DEFAULT_START_SIZE);
        _endSize = capsule.readDouble("endSize", DEFAULT_END_SIZE);
        _startColor.set((ColorRGBA) capsule.readSavable("startColor", new ColorRGBA(DEFAULT_START_COLOR)));
        _endColor.set((ColorRGBA) capsule.readSavable("endColor", new ColorRGBA(DEFAULT_END_COLOR)));
        _startSpin = capsule.readDouble("startSpin", 0);
        _endSpin = capsule.readDouble("endSpin", 0);
        _startMass = capsule.readDouble("startMass", 0);
        _endMass = capsule.readDouble("endMass", 0);
        _startTexIndex = capsule.readInt("startTexIndex", 0);
        _texQuantity = capsule.readInt("texQuantity", 1);
        _initialVelocity = capsule.readDouble("initialVelocity", 1);
        _minimumLifeTime = capsule.readDouble("minimumLifeTime", DEFAULT_MIN_LIFE);
        _maximumLifeTime = capsule.readDouble("maximumLifeTime", DEFAULT_MAX_LIFE);
        _minimumAngle = capsule.readDouble("minimumAngle", 0);
        _maximumAngle = capsule.readDouble("maximumAngle", DEFAULT_MAX_ANGLE);
        _emissionDirection.set((Vector3) capsule.readSavable("emissionDirection", new Vector3(Vector3.UNIT_Y)));
        _worldEmit.set((Vector3) capsule.readSavable("worldEmit", new Vector3(Vector3.ZERO)));
        _upVector.set((Vector3) capsule.readSavable("upVector", new Vector3(Vector3.UNIT_Y)));
        _leftVector.set((Vector3) capsule.readSavable("leftVector", new Vector3(-1, 0, 0)));
        _facingUpVector.set((Vector3) capsule.readSavable("facingUpVector", new Vector3(Vector3.UNIT_Y)));
        _facingLeftVector.set((Vector3) capsule.readSavable("facingLeftVector", new Vector3(-1, 0, 0)));
        _numParticles = capsule.readInt("numParticles", 0);
        _rotateWithScene = capsule.readBoolean("rotateWithScene", false);
        _geometryCoordinates = capsule.readFloatBuffer("geometryCoordinates", null);
        _appearanceColors = capsule.readFloatBuffer("appearanceColors", null);

        _releaseRate = capsule.readInt("releaseRate", _numParticles);
        _particleOrientation = capsule.readDouble("particleOrientation", 0);
        _originCenter.set((Vector3) capsule.readSavable("originCenter", new Vector3()));
        _originOffset.set((Vector3) capsule.readSavable("originOffset", new Vector3()));
        _controller = (ParticleController) capsule.readSavable("controller", null);
        _cameraFacing = capsule.readBoolean("cameraFacing", true);
        _velocityAligned = capsule.readBoolean("velocityAligned", false);
        _particlesInWorldCoords = capsule.readBoolean("particlesInWorldCoords", true);
        _ramp = (ParticleAppearanceRamp) capsule.readSavable("ramp", new ParticleAppearanceRamp());
        _texAnimation = (TexAnimation) capsule.readSavable("texAnimation", new TexAnimation());

        _invScale.zero();
        _upXemit.zero();
        _absUpVector.zero();
        _abUpMinUp.zero();
        _rotMatrix.setIdentity();
        initializeParticles(_numParticles);
    }
}
