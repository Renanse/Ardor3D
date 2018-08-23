/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.example.renderer;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.extension.ui.UIHud;
import com.ardor3d.extension.ui.UIPanel;
import com.ardor3d.extension.ui.backdrop.ImageBackdrop;
import com.ardor3d.extension.ui.util.SubTex;
import com.ardor3d.framework.Canvas;
import com.ardor3d.image.Image;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.image.Texture2D;
import com.ardor3d.image.TextureStoreFormat;
import com.ardor3d.image.util.GeneratedImageFactory;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TriggerConditions;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.functions.CheckerFunction3D;
import com.ardor3d.math.functions.CloudsFunction3D;
import com.ardor3d.math.functions.CylinderFunction3D;
import com.ardor3d.math.functions.FbmFunction3D;
import com.ardor3d.math.functions.Function3D;
import com.ardor3d.math.functions.Functions;
import com.ardor3d.math.functions.MandelbrotFunction3D;
import com.ardor3d.math.functions.MapperFunction3D;
import com.ardor3d.math.functions.RidgeFunction3D;
import com.ardor3d.math.functions.TurbulenceFunction3D;
import com.ardor3d.math.functions.VoroniFunction3D;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureKey;

/**
 * Illustrates the GeneratedImageFactory class and math.functions package, which allow for procedural creation of
 * textures.
 */
@Purpose(htmlDescriptionKey = "com.ardor3d.example.renderer.GeneratedTexturesExample", //
        thumbnailPath = "com/ardor3d/example/media/thumbnails/renderer_GeneratedTexturesExample.jpg", //
        maxHeapMemory = 64)
public class GeneratedTexturesExample extends ExampleBase {
    private final static int COUNT = 10;
    private final static int SPAN = 3;
    private static final float MAX_SPEED = 3f;

    ReadOnlyColorRGBA[] terrainColors;
    private int wside;
    private int hside;
    private int padding;

    private float index = 1.0f;
    private int firstTile = Integer.MIN_VALUE; // start off the chart.
    private final UIPanel[] srcs = new UIPanel[COUNT];
    private final UIPanel[] views = new UIPanel[SPAN + 1];
    private UIHud hud;
    private float speed = 0f;
    private UIPanel zoomed = null;

    public static void main(final String[] args) {
        start(GeneratedTexturesExample.class);
    }

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
        if (allowClicks && zoom) {
            if (index > COUNT - 1) {
                index = COUNT - 1;
            } else if (index < 0) {
                index = 0;
            }
            final int currentTile = MathUtils.floor(index);
            final float fract = index - currentTile;

            if (firstTile != currentTile - 1) {
                // update the textures on the tiles
                firstTile = MathUtils.floor(index - 1);

                for (int i = 0; i < views.length; i++) {
                    if (firstTile + i >= 0 && firstTile + i < COUNT) {
                        views[i].removeAllComponents();
                        views[i].add(srcs[firstTile + i]);
                        views[i].updateMinimumSizeFromContents();
                        views[i].layout();
                        views[i].setVisible(true);
                    } else {
                        views[i].setVisible(false);
                    }
                }
            }

            // update the positions of the tiles.
            final int y = (_canvas.getCanvasRenderer().getCamera().getHeight() / 2) - (hside / 2);
            for (int i = 0; i < views.length; i++) {
                final float x = (i - fract) * (wside + padding);
                views[i].setHudXY(Math.round(x), y);
            }

            // check for and apply movement
            index += timer.getTimePerFrame() * speed;
        }

        // update hud input
        hud.updateGeometricState(timer.getTimePerFrame());
    }

    @Override
    protected void updateLogicalLayer(final ReadOnlyTimer timer) {
        hud.getLogicalLayer().checkTriggers(timer.getTimePerFrame());
    }

    @Override
    protected void renderExample(final Renderer renderer) {
        super.renderExample(renderer);
        renderer.renderBuckets();
        // draw our UI elements.
        renderer.draw(hud);
    }

    @Override
    protected void initExample() {
        _canvas.setTitle("Generating textures... please wait");

        final Camera cam = _canvas.getCanvasRenderer().getCamera();
        wside = cam.getWidth() / SPAN;
        hside = wside;
        padding = wside / 10;

        // Set up hud
        hud = new UIHud(_canvas);
        hud.setupInput(_physicalLayer, _logicalLayer);

        // Set up the frames
        for (int i = 0; i < views.length; i++) {
            views[i] = new UIPanel();
            views[i].setLocalComponentSize(wside, hside);
            views[i].layout();
            hud.add(views[i]);
        }

        // Set up the panels
        for (int i = 0; i < COUNT; i++) {
            srcs[i] = new UIPanel("src-" + i, null) {
                @Override
                public boolean mousePressed(final com.ardor3d.input.MouseButton button,
                        final com.ardor3d.input.InputState state) {
                    if (zoom || this == zoomed) {
                        toggleZoom(this);
                    }
                    return true;
                };
            };
            srcs[i].setTooltipPopTime(25);
        }

        // set up our different textures...

        int index = 0;

        // check
        setCheck(srcs[index++]);

        // mandelbrot
        setMandelbrot(srcs[index++]);

        // voronoi
        setVoronoi(srcs[index++]);

        // *** 'Planet' example:
        final Function3D terrain = Functions.scaleInput(Functions.simplexNoise(), 2, 2, 1);
        terrainColors = new ReadOnlyColorRGBA[256];
        terrainColors[0] = new ColorRGBA(0, 0, .5f, 1);
        terrainColors[95] = new ColorRGBA(0, 0, 1, 1);
        terrainColors[127] = new ColorRGBA(0, .5f, 1, 1);
        terrainColors[137] = new ColorRGBA(240 / 255f, 240 / 255f, 64 / 255f, 1);
        terrainColors[143] = new ColorRGBA(32 / 255f, 160 / 255f, 0, 1);
        terrainColors[175] = new ColorRGBA(224 / 255f, 224 / 255f, 0, 1);
        terrainColors[223] = new ColorRGBA(128 / 255f, 128 / 255f, 128 / 255f, 1);
        terrainColors[255] = ColorRGBA.WHITE;
        GeneratedImageFactory.fillInColorTable(terrainColors);

        // simplex - luminance
        setTerrain(srcs[index], terrain, false, false);
//        srcs[index++].setTooltipText("Simplex noise.");

        // simplex + FBM - luminance
        setTerrain(srcs[index], terrain, true, false);
//        srcs[index++].setTooltipText("Simplex noise + Fractional Brownian Motion (fBm).");

        // color ramp
        setColors(srcs[index++]);

        // simplex + FBM - color
        setTerrain(srcs[index], terrain, true, true);
//        srcs[index++].setTooltipText("Noise + Colormap == Something that looks like a map. :)");

        // *** A few textures derived from samples in the libnoise project.
        // *** Perhaps we can get an some better ones from the community :)
        // A simple wood grain
        setWoodGrain(srcs[index++]);

        // Jade
        setJade(srcs[index++]);

        // Slime
        setSlime(srcs[index++]);
        _canvas.setTitle("Generated Textures Example - Ardor3D");
    }

    boolean zoom = true;
    boolean allowClicks = true;
    int originX = 0, originY = 0;

    private void toggleZoom(final UIPanel uiPanel) {
        if (!allowClicks) {
            return;
        }

        // ignore other clicks until we are done zooming...
        allowClicks = false;
        speed = 0;
        final UIPanel parent = ((UIPanel) uiPanel.getParent());

        if (zoom) {
            originX = parent.getHudX();
            originY = parent.getHudY();
            zoomed = uiPanel;
        } else {
            zoomed = null;
        }

        final int endY = 0, endX = (hud.getWidth() - hud.getHeight()) / 2;

        // add an animator to do the zoom
        uiPanel.addController(new SpatialController<UIPanel>() {
            float ratio = zoom ? 0 : 1;
            float zSpeed = 1;

            public void update(final double time, final UIPanel caller) {
                // update ratio
                ratio += (zoom ? 1 : -1) * zSpeed * time;
                if (ratio >= 1.0f || ratio <= 0.0) {
                    if (ratio >= 1.0f) {
                        ratio = 1.0f;
                    } else {
                        ratio = 0;
                    }
                    zoom = !zoom;
                    caller.removeController(this);
                    allowClicks = true;
                }

                // use an scurve to smoothly zoom
                final float sCurve = MathUtils.scurve5(ratio);
                final float size = sCurve * (hud.getHeight() - hside) + hside;
                parent.setLocalComponentSize((int) (size * hside / wside), (int) size);

                // use an scurve to smoothly shift origin
                parent.setHudXY(Math.round(MathUtils.lerp(sCurve, originX, endX)),
                        Math.round(MathUtils.lerp(sCurve, originY, endY)));

                parent.layout();
            }
        });
    }

    @Override
    protected void registerInputTriggers() {
        super.registerInputTriggers();

        _logicalLayer.registerTrigger(new InputTrigger(TriggerConditions.mouseMoved(), new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (!allowClicks || !zoom) {
                    return;
                }
                final int x = inputStates.getCurrent().getMouseState().getX();
                float ratio = (float) x / _canvas.getCanvasRenderer().getCamera().getWidth();
                ratio = (2 * ratio) - 1;
                // make a dead zone in center.
                if (Math.abs(ratio) < 0.1) {
                    ratio = 0;
                }
                speed = MAX_SPEED * ratio;
            }
        }));
    }

    private void setCheck(final UIPanel panel) {
        // panel.setTooltipText("Simple Checkerboard pattern");

        final Texture tex = new Texture2D();

        // Build up our function
        final Function3D finalCheck = new CheckerFunction3D();

        final Image img = GeneratedImageFactory.createRed8Image(finalCheck, wside, hside, 1, 1, 9, 1, 9, 0, 0, -1, 1);
        tex.setImage(img);
        // No need for filtering on this one...
        tex.setTextureKey(TextureKey.getRTTKey(MinificationFilter.NearestNeighborNoMipMaps));
        tex.setMagnificationFilter(MagnificationFilter.NearestNeighbor);
        tex.setMinificationFilter(MinificationFilter.NearestNeighborNoMipMaps);

        applyTexture(tex, panel);
    }

    private void setColors(final UIPanel panel) {
        // panel.setTooltipText("The 1-D color map used to colorize our gradient.");

        final Texture tex = new Texture2D();
        final Image img = GeneratedImageFactory.create1DColorImage(false, terrainColors);
        tex.setImage(img);
        tex.setTextureKey(TextureKey.getKey(null, false, TextureStoreFormat.RGB8, MinificationFilter.Trilinear));
        tex.setMagnificationFilter(MagnificationFilter.NearestNeighbor);
        tex.setMinificationFilter(MinificationFilter.Trilinear);

        applyTexture(tex, panel);
    }

    private void setMandelbrot(final UIPanel panel) {
        // panel.setTooltipText("The famous Mandelbrot fractal");

        final Texture tex = new Texture2D();

        // Build up our function
        final Function3D mandelBase = new MandelbrotFunction3D(256);
        final Function3D translatedMandel = Functions.translateInput(mandelBase, -.7, 0, 0);
        final Function3D finalMandel = Functions.scaleInput(translatedMandel, 1.5, 1.5, 1);

        final ReadOnlyColorRGBA[] colors = new ReadOnlyColorRGBA[256];
        colors[0] = ColorRGBA.BLUE;
        colors[10] = ColorRGBA.YELLOW;
        colors[25] = ColorRGBA.BLUE;
        colors[255] = ColorRGBA.BLACK;
        GeneratedImageFactory.fillInColorTable(colors);

        Image img = GeneratedImageFactory.createRed8Image(finalMandel, (int) (1.5 * wside), (int) (1.5 * hside), 1);
        img = GeneratedImageFactory.createColorImageFromLuminance8(img, false, colors);
        tex.setImage(img);
        tex.setTextureKey(TextureKey.getRTTKey(MinificationFilter.Trilinear));
        tex.setMagnificationFilter(MagnificationFilter.Bilinear);
        tex.setMinificationFilter(MinificationFilter.Trilinear);

        applyTexture(tex, panel);
    }

    private void setVoronoi(final UIPanel panel) {
        // panel.setTooltipText("Voronoi graph");

        final Texture tex = new Texture2D();

        final ReadOnlyColorRGBA[] colors = new ReadOnlyColorRGBA[256];
        colors[0] = ColorRGBA.BLUE;
        colors[255] = ColorRGBA.BLACK;
        GeneratedImageFactory.fillInColorTable(colors);

        Image img = GeneratedImageFactory.createRed8Image(new VoroniFunction3D(6, .5, true, 1), wside, hside, 1);
        img = GeneratedImageFactory.createColorImageFromLuminance8(img, false, colors);
        tex.setImage(img);
        tex.setTextureKey(TextureKey.getRTTKey(MinificationFilter.Trilinear));
        tex.setMagnificationFilter(MagnificationFilter.Bilinear);
        tex.setMinificationFilter(MinificationFilter.Trilinear);

        applyTexture(tex, panel);
    }

    private void setTerrain(final UIPanel panel, final Function3D in, final boolean useFbm, final boolean useColor) {
        final Texture tex = new Texture2D();
        Function3D func = in;
        if (useFbm) {
            func = new FbmFunction3D(func, 5, 0.5, 0.5, 3.14);
        }
        Image img = GeneratedImageFactory.createRed8Image(func, wside, hside, 1);
        if (useColor) {
            img = GeneratedImageFactory.createColorImageFromLuminance8(img, false, terrainColors);
        }
        tex.setImage(img);
        tex.setTextureKey(TextureKey.getRTTKey(MinificationFilter.Trilinear));
        tex.setMagnificationFilter(MagnificationFilter.Bilinear);
        tex.setMinificationFilter(MinificationFilter.Trilinear);

        applyTexture(tex, panel);
    }

    private void setWoodGrain(final UIPanel panel) {
        // panel.setTooltipText("Dark wood grain.");

        final Texture tex = new Texture2D();

        // Build up our function
        final Function3D baseWood = new CylinderFunction3D(18);
        final Function3D woodGrainNoise = new FbmFunction3D(Functions.simplexNoise(), 3, 40, 0.75, 2.3);
        final Function3D scaledBaseWoodGrain = Functions.scaleInput(woodGrainNoise, 1, .25, 1);
        final Function3D woodGrain = Functions.scaleBias(scaledBaseWoodGrain, .125, 0);
        final Function3D combinedWood = Functions.add(baseWood, woodGrain);
        final Function3D perturbedWood = new TurbulenceFunction3D(combinedWood, 1 / 256.0, 4, 4.0);
        final Function3D translatedWood = Functions.translateInput(perturbedWood, 0, 0, 1.5);
        final Function3D rotatedWood = Functions.rotateInput(translatedWood,
                new Matrix3().fromAngles(MathUtils.DEG_TO_RAD * 6, 0, 0));
        final Function3D finalWood = new TurbulenceFunction3D(rotatedWood, 1 / 512.0, 2, 2.0);

        final ReadOnlyColorRGBA[] woodColors = new ReadOnlyColorRGBA[256];
        woodColors[0] = new ColorRGBA(189 / 255f, 94 / 255f, 4 / 255f, 1);
        woodColors[127] = new ColorRGBA(144 / 255f, 48 / 255f, 6 / 255f, 1);
        woodColors[255] = new ColorRGBA(60 / 255f, 10 / 255f, 8 / 255f, 1);
        GeneratedImageFactory.fillInColorTable(woodColors);

        Image img = GeneratedImageFactory.createRed8Image(finalWood, wside, hside, 1);
        img = GeneratedImageFactory.createColorImageFromLuminance8(img, false, woodColors);
        tex.setImage(img);
        tex.setTextureKey(TextureKey.getRTTKey(MinificationFilter.Trilinear));
        tex.setMagnificationFilter(MagnificationFilter.Bilinear);
        tex.setMinificationFilter(MinificationFilter.Trilinear);

        applyTexture(tex, panel);
    }

    private void setJade(final UIPanel panel) {
        // panel.setTooltipText("A Jade-like texture");

        final Texture tex = new Texture2D();

        // Build up our function
        final Function3D primaryJade = new RidgeFunction3D(Functions.simplexNoise(), 6, 2.0, 2.207);
        final Function3D baseSecondaryJade = new CylinderFunction3D(2);
        final Function3D rotatedBaseSecondaryJade = Functions.rotateInput(baseSecondaryJade,
                new Matrix3().fromAngles(0, MathUtils.DEG_TO_RAD * 65, MathUtils.DEG_TO_RAD * 85));
        final Function3D perturbedBaseSecondaryJade = new TurbulenceFunction3D(rotatedBaseSecondaryJade, 1.0 / 4.0, 4,
                4.0);
        final Function3D secondaryJade = Functions.scaleBias(perturbedBaseSecondaryJade, .25, 0);
        final Function3D combinedJade = Functions.add(primaryJade, secondaryJade);
        final Function3D finalJade = new TurbulenceFunction3D(combinedJade, 1 / 16.0, 2, 4.0);

        final ReadOnlyColorRGBA[] jadeColors = new ReadOnlyColorRGBA[256];
        jadeColors[0] = new ColorRGBA(24 / 255f, 146 / 255f, 102 / 255f, 1);
        jadeColors[127] = new ColorRGBA(78 / 255f, 154 / 255f, 115 / 255f, 1);
        jadeColors[159] = new ColorRGBA(128 / 255f, 204 / 255f, 165 / 255f, 1);
        jadeColors[175] = new ColorRGBA(78 / 255f, 154 / 255f, 115 / 255f, 1);
        jadeColors[255] = new ColorRGBA(29 / 255f, 135 / 255f, 102 / 255f, 1);
        GeneratedImageFactory.fillInColorTable(jadeColors);

        Image img = GeneratedImageFactory.createRed8Image(finalJade, wside, hside, 1);
        img = GeneratedImageFactory.createColorImageFromLuminance8(img, false, jadeColors);
        tex.setImage(img);
        tex.setTextureKey(TextureKey.getRTTKey(MinificationFilter.Trilinear));
        tex.setMagnificationFilter(MagnificationFilter.Bilinear);
        tex.setMinificationFilter(MinificationFilter.Trilinear);

        applyTexture(tex, panel);
    }

    private void setSlime(final UIPanel panel) {
        // panel.setTooltipText( "Slime: Built with two bubble functions of different sizes\n" + "and blended with a
        // mapping function.");
        panel.setTooltipPopTime(25);

        final Texture tex = new Texture2D();

        // Build up our function
        final Function3D largeSlimeBase = new CloudsFunction3D(Functions.simplexNoise(), 1, 4.0, 0.5, 2.12);
        final Function3D largeSlime = Functions.scaleBias(largeSlimeBase, 1, 0.5);
        final Function3D smallSlimeBase = new CloudsFunction3D(Functions.simplexNoise(), 1, 24.0, 0.5, 2.14);
        final Function3D smallSlime = Functions.scaleBias(smallSlimeBase, 0.5, 0);
        final RidgeFunction3D slimeMap = new RidgeFunction3D(Functions.simplexNoise(), 3, 2.0, 2.207);
        final MapperFunction3D slimeMapper = new MapperFunction3D(slimeMap, -1, 1);
        slimeMapper.addFunction(largeSlime, 0, 0, 0);
        slimeMapper.addFunction(smallSlime, .125, 1, 1);
        slimeMapper.addFunction(largeSlime, 1.75, 0, 0);
        final Function3D finalSlime = new TurbulenceFunction3D(slimeMapper, 1 / 32.0, 2, 8.0);

        final ReadOnlyColorRGBA[] slimeColors = new ReadOnlyColorRGBA[256];
        slimeColors[0] = new ColorRGBA(160 / 255f, 64 / 255f, 42 / 255f, 1);
        slimeColors[127] = new ColorRGBA(64 / 255f, 192 / 255f, 64 / 255f, 1);
        slimeColors[255] = new ColorRGBA(128 / 255f, 255 / 255f, 128 / 255f, 1);
        GeneratedImageFactory.fillInColorTable(slimeColors);

        Image img = GeneratedImageFactory.createRed8Image(finalSlime, wside, hside, 1);
        img = GeneratedImageFactory.createColorImageFromLuminance8(img, false, slimeColors);
        tex.setImage(img);
        tex.setTextureKey(TextureKey.getRTTKey(MinificationFilter.Trilinear));
        tex.setMagnificationFilter(MagnificationFilter.Bilinear);
        tex.setMinificationFilter(MinificationFilter.Trilinear);

        applyTexture(tex, panel);
    }

    private void applyTexture(final Texture tex, final UIPanel panel) {
        final ImageBackdrop backdrop = new ImageBackdrop(
                new SubTex(tex, 0, 0, tex.getImage().getWidth(), -tex.getImage().getHeight()));
        panel.setBackdrop(backdrop);
    }
}
