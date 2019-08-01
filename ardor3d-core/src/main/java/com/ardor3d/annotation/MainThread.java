/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods flagged with this annotation should only be run in the main thread, that is, the thread that handles the
 * OpenGL calls. It is possible, and good during development but possibly not during production, to use a method
 * interceptor that enforces this constraint, but in any case, the presence of the annotation should help programmers
 * when thinking about threading.
 * 
 * This annotation should be used on any API method in the framework for which it is necessary to call it only from the
 * main thread. If it adds to clarity, it may be a good idea to use it for internal methods as well.
 * 
 * Note that this annotation, when present on an interface method, does not get inherited, and it is therefore there
 * only for clarity purposes rather than as a way of getting method interception to work. It should always be added to
 * any class directly implementing an interface method that uses it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.PARAMETER })
@Inherited
public @interface MainThread {}
