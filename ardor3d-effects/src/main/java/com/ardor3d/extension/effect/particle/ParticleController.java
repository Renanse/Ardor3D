/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.effect.particle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.MathUtils;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Camera.FrustumIntersect;
import com.ardor3d.renderer.ContextManager;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.scenegraph.controller.ComplexSpatialController;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>ParticleController</code> controls and maintains the parameters of a particle system over time.
 */
public class ParticleController extends ComplexSpatialController<ParticleSystem> {

    private static final long serialVersionUID = 1L;

    private int _particlesToCreate = 0;
    private double _releaseVariance;
    private double _currentTime;
    private double _prevTime;
    private double _releaseParticles;
    private double _timePassed;
    private double _precision;
    private boolean _controlFlow;
    private boolean _updateOnlyInView;
    private boolean _spawnEnabled = true;
    private Camera _viewCamera;

    private int iterations;
    private List<ParticleInfluence> influences;
    protected List<ParticleControllerListener> listeners;

    /**
     * ParticleController constructor
     */
    public ParticleController() {

        setMinTime(0);
        setMaxTime(Float.MAX_VALUE);
        setRepeatType(RepeatType.WRAP);
        setSpeed(1.0f);

        _releaseVariance = 0;
        _controlFlow = false;
        _updateOnlyInView = false;
        _precision = .01f; // 10ms
    }

    protected boolean _ignoreOneUpdate = false;

    protected void ignoreNextUpdate() {
        _ignoreOneUpdate = true;
    }

    /**
     * Update the particles managed by this manager. If any particles are "dead" recreate them at the origin position
     * (which may be a point, line or rectangle.)
     * 
     * @param secondsPassed
     *            double precision time
     * @param particles
     *            the particles we are updating
     */
    @Override
    public void update(final double secondsPassed, final ParticleSystem particles) {

        if (_ignoreOneUpdate) {
            _ignoreOneUpdate = false;
            return;
        }

        // If instructed, check to see if our last frustum check passed
        if (isUpdateOnlyInView()) {
            final Camera cam = _viewCamera != null ? _viewCamera : ContextManager.getCurrentContext()
                    .getCurrentCamera();
            if (cam != null) {
                final int state = cam.getPlaneState();
                final boolean out = cam.contains(particles.getWorldBound()).equals(FrustumIntersect.Outside);
                cam.setPlaneState(state);
                if (out) {
                    return;
                }
            }
        }

        // Add time and unless we have more than precision time passed
        // since last real update, do nothing
        _currentTime += secondsPassed * getSpeed();

        // Check precision passes
        _timePassed = _currentTime - _prevTime;
        if (_timePassed < _precision * getSpeed()) {
            return;
        }

        // We are actually going to do a real update,
        // so this is our new previous time
        _prevTime = _currentTime;

        // Update the current rotation matrix if needed.
        particles.updateRotationMatrix();

        // If we are in the time window where this controller is active
        // (defaults to 0 to Float.MAX_VALUE for ParticleController)
        if (_currentTime >= getMinTime() && _currentTime <= getMaxTime()) {

            // If we are controlling the flow (ie the rate of particle spawning.)
            if (_controlFlow) {
                // Release a number of particles based on release rate,
                // timePassed (already scaled for speed) and variance. This
                // is added to any current value Note this is a double value,
                // so we will keep adding up partial particles

                _releaseParticles += (particles.getReleaseRate() * _timePassed * (1.0 + _releaseVariance
                        * (MathUtils.nextRandomFloat() - 0.5)));

                // Try to create all "whole" particles we have added up
                _particlesToCreate = (int) _releaseParticles;

                // If we have any whole particles, then subtract them from
                // releaseParticles
                if (_particlesToCreate > 0) {
                    _releaseParticles -= _particlesToCreate;
                } else {
                    _particlesToCreate = 0;
                }
            }

            particles.updateInvScale();

            // If we have any influences, prepare them all
            if (influences != null) {
                for (int x = 0; x < influences.size(); x++) {
                    final ParticleInfluence inf = influences.get(x);
                    inf.prepare(particles);
                }
            }

            // Track particle index
            int i = 0;

            // Track whether the whole set of particles is "dead" - if any
            // particles are still alive, this will be set to false
            boolean dead = true;

            // opposite of above boolean, but tracked separately
            boolean anyAlive = false;

            // i is index through all particles
            while (i < particles.getNumParticles()) {
                // Current particle
                final Particle p = particles.getParticle(i);

                // If we have influences and particle is alive
                if (influences != null && p.getStatus() == Particle.Status.Alive) {
                    // Apply each enabled influence to the current particle
                    for (int x = 0; x < influences.size(); x++) {
                        final ParticleInfluence inf = influences.get(x);
                        if (inf.isEnabled()) {
                            inf.apply(_timePassed, p, i);
                        }
                    }
                }

                // Update and check the particle.
                // If this returns true, indicating particle is ready to be
                // reused, we may reuse it. Do so if we are not using
                // control flow, OR we intend to create particles based on
                // control flow count calculated above
                final boolean reuse = p.updateAndCheck(_timePassed);
                if (reuse && (!_controlFlow || _particlesToCreate > 0)) {

                    // Don't recreate the particle if it is dead, and we are clamped
                    if (p.getStatus() == Particle.Status.Dead && getRepeatType() == RepeatType.CLAMP) {
                        ;

                        // We plan to reuse the particle
                    } else {
                        if (_spawnEnabled) {
                            // Not all particles are dead (this one will be reused)
                            dead = false;

                            // If we are doing flow control, decrement
                            // particlesToCreate, since we are about to create
                            // one
                            if (_controlFlow) {
                                _particlesToCreate--;
                            }

                            // Recreate the particle
                            p.recreateParticle(particles.getRandomLifeSpan());
                            p.setStatus(Particle.Status.Alive);
                            // in case of ramp time 0 entries.
                            p.updateAndCheck(0);
                            particles.initParticleLocation(i);
                            particles.resetParticleVelocity(i);
                            p.updateVerts(null);
                        }
                    }

                } else if (!reuse || (_controlFlow && particles.getReleaseRate() > 0)) {
                    // The particle wasn't dead, or we expect more particles
                    // later, so we're not dead!
                    dead = false;
                }

                // Check for living particles so we know when to update our boundings.
                if (p.getStatus() == Particle.Status.Alive) {
                    anyAlive = true;
                }

                // Next particle
                i++;
            }

            // If we are dead, deactivate and tell our listeners
            if (dead) {
                setActive(false);
                if (listeners != null && listeners.size() > 0) {
                    for (final ParticleControllerListener listener : listeners) {
                        listener.onDead(particles);
                    }
                }
            } else {
                // if not dead make sure our particles refresh their vbos, etc.
                final MeshData md = particles.getParticleGeometry().getMeshData();
                md.getVertexCoords().setNeedsRefresh(true);
                md.getColorCoords().setNeedsRefresh(true);
                md.getTextureCoords(0).setNeedsRefresh(true);
            }

            // If we have any live particles and are offscreen, update it
            if (anyAlive) {
                boolean updateMB = true;
                final Camera cam = _viewCamera != null ? _viewCamera
                        : (ContextManager.getCurrentContext() != null ? ContextManager.getCurrentContext()
                                .getCurrentCamera() : null);
                if (cam != null) {
                    final int state = cam.getPlaneState();
                    updateMB = cam.contains(particles.getWorldBound()).equals(FrustumIntersect.Outside);
                    cam.setPlaneState(state);
                }
                if (updateMB) {
                    particles.getParticleGeometry().updateModelBound();
                    particles.updateWorldBoundManually();
                }
            }
        }
    }

    /**
     * Get how soon after the last update the manager will send updates to the particles.
     * 
     * @return The precision.
     */
    public double getPrecision() {
        return _precision;
    }

    /**
     * Set how soon after the last update the manager will send updates to the particles. Defaults to .01f (10ms)<br>
     * <br>
     * This means that if an update is called every 2ms (e.g. running at 500 FPS) the particles position and stats will
     * be updated every fifth frame with the elapsed time (in this case, 10ms) since previous update.
     * 
     * @param precision
     *            in seconds
     */
    public void setPrecision(final double precision) {
        _precision = precision;
    }

    /**
     * Get the variance possible on the release rate. 0.0f = no variance 0.5f = between releaseRate / 2f and 1.5f *
     * releaseRate
     * 
     * @return release variance as a percent.
     */
    public double getReleaseVariance() {
        return _releaseVariance;
    }

    /**
     * Set the variance possible on the release rate.
     * 
     * @param variance
     *            release rate +/- variance as a percent (eg. .5 = 50%)
     */
    public void setReleaseVariance(final double variance) {
        _releaseVariance = variance;
    }

    /**
     * Does this manager regulate the particle flow?
     * 
     * @return true if this manager regulates how many particles per sec are emitted.
     */
    public boolean isControlFlow() {
        return _controlFlow;
    }

    /**
     * Set the regulate flow property on the manager.
     * 
     * @param regulate
     *            regulate particle flow.
     */
    public void setControlFlow(final boolean regulate) {
        _controlFlow = regulate;
    }

    /**
     * Does this manager use the particle's bounding volume to limit updates?
     * 
     * @return true if this manager only updates the particles when they are in view.
     */
    public boolean isUpdateOnlyInView() {
        return _updateOnlyInView;
    }

    /**
     * Set the updateOnlyInView property on the manager.
     * 
     * @param updateOnlyInView
     *            use the particle's bounding volume to limit updates.
     */
    public void setUpdateOnlyInView(final boolean updateOnlyInView) {
        _updateOnlyInView = updateOnlyInView;
    }

    /**
     * @return the camera to be used in updateOnlyInView situations. If null, the current displaySystem's renderer
     *         camera is used.
     */
    public Camera getViewCamera() {
        return _viewCamera;
    }

    /**
     * @param viewCamera
     *            sets the camera to be used in updateOnlyInView situations. If null, the current displaySystem's
     *            renderer camera is used.
     */
    public void setViewCamera(final Camera viewCamera) {
        _viewCamera = viewCamera;
    }

    /**
     * Return the number this manager has warmed up
     * 
     * @return int
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * Sets the iterations for the warmup and calls warmUp with the number of iterations as the argument
     * 
     * @param iterations
     */
    public void setIterations(final int iterations) {
        this.iterations = iterations;
    }

    /**
     * Add an external influence to this particle controller.
     * 
     * @param influence
     *            ParticleInfluence
     */
    public void addInfluence(final ParticleInfluence influence) {
        if (influences == null) {
            influences = new ArrayList<ParticleInfluence>(1);
        }
        influences.add(influence);
    }

    /**
     * Remove an influence from this particle controller.
     * 
     * @param influence
     *            ParticleInfluence
     * @return true if found and removed.
     */
    public boolean removeInfluence(final ParticleInfluence influence) {
        if (influences == null) {
            return false;
        }
        return influences.remove(influence);
    }

    /**
     * Returns the list of influences acting on this particle controller.
     * 
     * @return ArrayList
     */
    public List<ParticleInfluence> getInfluences() {
        return influences;
    }

    public void clearInfluences() {
        if (influences != null) {
            influences.clear();
        }
    }

    /**
     * Subscribe a listener to receive mouse events. Enable event generation.
     * 
     * @param listener
     *            to be subscribed
     */
    public void addListener(final ParticleControllerListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<ParticleControllerListener>();
        }

        listeners.add(listener);
    }

    /**
     * Unsubscribe a listener. Disable event generation if no more listeners.
     * 
     * @param listener
     *            to be unsuscribed
     * @see #addListener(ParticleControllerListener)
     */
    public void removeListener(final ParticleControllerListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Remove all listeners and disable event generation.
     */
    public void removeListeners() {
        if (listeners != null) {
            listeners.clear();
        }
    }

    /**
     * Check if a listener is allready added to this ParticleController
     * 
     * @param listener
     *            listener to check for
     * @return true if listener is contained in the listenerlist
     */
    public boolean containsListener(final ParticleControllerListener listener) {
        if (listeners != null) {
            return listeners.contains(listener);
        }
        return false;
    }

    /**
     * Get all added ParticleController listeners
     * 
     * @return ArrayList of listeners added to this ParticleController
     */
    public List<ParticleControllerListener> getListeners() {
        return listeners;
    }

    /**
     * Runs the update method of this particle manager X number of times passing .1 seconds for each call. This is used
     * to "warm up" and get the particle manager going.
     * 
     * @param iterations
     *            The number of iterations to warm up.
     */
    public void warmUp(int iterations, final ParticleSystem particles) {
        // first set the initial positions of all the verts
        particles.initAllParticlesLocation();

        iterations *= 10;
        for (int i = iterations; --i >= 0;) {
            particles.updateGeometricState(0.1, false);
        }
        ignoreNextUpdate();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.write(_releaseVariance, "releaseVariance", 0);
        capsule.write(_precision, "precision", 0);
        capsule.write(_controlFlow, "controlFlow", false);
        capsule.write(_updateOnlyInView, "updateOnlyInView", false);
        capsule.write(iterations, "iterations", 0);
        capsule.writeSavableList(influences, "influences", null);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        _releaseVariance = capsule.readDouble("releaseVariance", 0);
        _precision = capsule.readDouble("precision", 0);
        _controlFlow = capsule.readBoolean("controlFlow", false);
        _updateOnlyInView = capsule.readBoolean("updateOnlyInView", false);
        iterations = capsule.readInt("iterations", 0);
        influences = capsule.readSavableList("influences", null);
    }

    public void resetFlowCount() {
        _releaseParticles = 0;
    }

    public void setSpawnEnabled(final boolean spawnEnabled) {
        _spawnEnabled = spawnEnabled;
    }
}
