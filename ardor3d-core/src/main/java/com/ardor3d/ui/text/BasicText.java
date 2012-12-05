/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.ui.text;

import java.util.logging.Logger;

import com.ardor3d.annotation.SavableFactory;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.hint.TextureCombineMode;
import com.ardor3d.util.resource.ResourceLocatorTool;
import com.ardor3d.util.resource.URLResourceSource;

@SavableFactory(factoryMethod = "initSavable")
public class BasicText extends BMText {
    static Logger logger = Logger.getLogger(BasicText.class.getName());

    public static BMFont DEFAULT_FONT;

    public static double DEFAULT_FONT_SIZE = 24;

    static {
        try {
            DEFAULT_FONT = new BMFont(new URLResourceSource(ResourceLocatorTool.getClassPathResource(BasicText.class,
                    "com/ardor3d/ui/text/arial-24-bold-regular.fnt")), true);
        } catch (final Exception ex) {
            logger.throwing(BasicText.class.getCanonicalName(), "static font init", ex);
        }
    }

    public static BasicText initSavable() {
        return new BasicText();
    }

    protected BasicText() {}

    public BasicText(final String name, final String text, final BMFont font, final double fontSize) {
        super(name, text, font);
        getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        setFontScale(fontSize);
        setAutoFade(AutoFade.Off);
        setAutoScale(AutoScale.Off);
        setAutoRotate(false);
        setRotation(new Matrix3().fromAngles(-MathUtils.HALF_PI, 0, 0));

        final ZBufferState zState = new ZBufferState();
        zState.setEnabled(false);
        zState.setWritable(false);
        setRenderState(zState);

        final CullState cState = new CullState();
        cState.setEnabled(false);
        setRenderState(cState);

        final BlendState blend = new BlendState();
        blend.setBlendEnabled(true);
        blend.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        blend.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        blend.setTestEnabled(true);
        blend.setReference(0f);
        blend.setTestFunction(BlendState.TestFunction.GreaterThan);
        setRenderState(blend);

        getSceneHints().setLightCombineMode(LightCombineMode.Off);
        getSceneHints().setTextureCombineMode(TextureCombineMode.Replace);
        updateModelBound();
    }

    public static BasicText createDefaultTextLabel(final String name, final String text, final double fontSize) {
        return new BasicText(name, text, DEFAULT_FONT, fontSize);
    }

    public static BasicText createDefaultTextLabel(final String name, final String text) {
        return new BasicText(name, text, DEFAULT_FONT, DEFAULT_FONT_SIZE);
    }

}
