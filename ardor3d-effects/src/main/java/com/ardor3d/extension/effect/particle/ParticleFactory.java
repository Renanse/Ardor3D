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

import com.ardor3d.extension.effect.particle.ParticleSystem.ParticleType;
import com.ardor3d.scenegraph.Mesh;

public class ParticleFactory {

    public static ParticleSystem buildParticles(final String name, final int number) {
        return buildParticles(name, number, ParticleSystem.ParticleType.Triangle);
    }

    public static ParticleSystem buildParticles(final String name, final int number,
            final ParticleSystem.ParticleType particleType) {
        if (particleType == ParticleSystem.ParticleType.GeomMesh) {
            throw new IllegalArgumentException("particleType can not be GeomMesh");
        }
        ParticleSystem system;
        if (particleType == ParticleType.Point) {
            system = new ParticlePoints(name, number);
        } else if (particleType == ParticleType.Line) {
            system = new ParticleLines(name, number);
        } else {
            system = new ParticleMesh(name, number, particleType);
        }
        final ParticleController particleController = new ParticleController();
        system.addController(particleController);
        return system;
    }

    public static ParticleMesh buildMeshParticles(final String name, final Mesh mesh) {
        final ParticleMesh particleMesh = new ParticleMesh(name, mesh);
        final ParticleController particleController = new ParticleController();
        particleMesh.addController(particleController);
        return particleMesh;
    }

}
