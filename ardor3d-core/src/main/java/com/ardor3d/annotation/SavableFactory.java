/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Tells the Savable system to instantiate objects of this type using a specific static method. The method should take
 * no arguments and return a new instance of the annotated class.
 * 
 * It is recommended the method be named something indicating use for Savable system.
 */
@Target( { TYPE })
@Retention(RUNTIME)
public @interface SavableFactory {

    /**
     * @return the name of the static method to use to build this class.
     */
    String factoryMethod();

}
