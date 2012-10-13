/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.animation.skeletal.state.loader;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.ResourceSource;

/**
 * Import utility used for reading and constructing AnimationLayer information for a manager using JavaScript.
 */
public final class JSLayerImporter {

    /**
     * Populate a manager with layer information.
     * 
     * @param layersFile
     *            the script file to read from.
     * @param manager
     *            the manager to add layer information to.
     * @param input
     *            the input store object, holding things like AnimationClips that the layers might need for
     *            construction.
     * @return an output store object
     * @throws IOException
     *             if there is a problem accessing the contents of the layersFile.
     * @throws ScriptException
     *             if the script given has syntax/parse errors.
     */
    public static OutputStore addLayers(final ResourceSource layersFile, final AnimationManager manager,
            final InputStore input) throws IOException, ScriptException {
        final OutputStore output = new OutputStore();
        final ScriptEngineManager mgr = new ScriptEngineManager();
        final ScriptEngine jsEngine = mgr.getEngineByExtension("js");

        jsEngine.put("MANAGER", manager);
        jsEngine.put("INPUTSTORE", input);
        jsEngine.put("OUTPUTSTORE", output);

        // load our helper functions first...
        jsEngine.eval(new InputStreamReader(ResourceLocatorTool.getClassPathResourceAsStream(JSLayerImporter.class,
                "com/ardor3d/extension/animation/skeletal/state/loader/functions.js")));

        // Add our user data...
        jsEngine.eval(new InputStreamReader(layersFile.openStream()));

        // return our output store, which may have useful items such as attachment points, etc.
        return output;
    }

}
