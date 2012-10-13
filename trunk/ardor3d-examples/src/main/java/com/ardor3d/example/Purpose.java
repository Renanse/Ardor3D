/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * a description of a class for the RunExamples (and maybe other tools)
 */
@Target( { TYPE })
@Retention(RUNTIME)
public @interface Purpose {

    /**
     * @return The base filename of the localisation properties file to read {@link #htmlDescriptionKey() html
     *         descriptions} from.
     */
    String localisationBaseFile() default "com/ardor3d/example/i18n/example_descriptions";

    /**
     * @return The key of the description to load from the {@link #localisationBaseFile() localisation properties file}.
     */
    String htmlDescriptionKey() default "com.ardor3d.example.NotSet";

    /**
     * @return the resource path to a screenshot thumbnail, e.g. /com/ardor3d/example/thumbnails/boxexample.png
     */
    String thumbnailPath() default "com/ardor3d/example/media/images/ardor3d_white_256.jpg";

    /**
     * Default value is 64.
     * 
     * @return the value to use for max heap (in MB).
     */
    int maxHeapMemory() default 64;
}
