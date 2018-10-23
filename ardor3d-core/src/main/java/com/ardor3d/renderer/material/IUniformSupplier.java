/**
 * Copyright (c) 2008-2018 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.material;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ardor3d.renderer.material.uniform.UniformRef;

public interface IUniformSupplier {

    List<UniformRef> getUniforms();

    /**
     * Set default values for this uniform supplier. Used when we have to generate a missing supplier (such as a light
     * for an index with no light.)
     */
    void applyDefaultUniformValues();

    static Set<String> nullProviders = new HashSet<>();

    static IUniformSupplier getDefaultProvider(final String className) {
        if (nullProviders.contains(className)) {
            return null;
        }

        try {
            final Class<?> clazz = Class.forName(className);
            final Object val = clazz.newInstance();
            if (val instanceof IUniformSupplier) {
                final IUniformSupplier supplier = (IUniformSupplier) val;
                supplier.applyDefaultUniformValues();
                return supplier;
            }
        } catch (final ClassNotFoundException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (final SecurityException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (final IllegalArgumentException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (final IllegalAccessException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } catch (final InstantiationException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }

        nullProviders.add(className);
        return null;
    }
}
