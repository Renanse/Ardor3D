/**
 * Copyright (c) 2008-2024 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.example.basic;

import java.util.ArrayList;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.example.Purpose;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.MaterialUtil;
import com.ardor3d.util.geom.GeometryTool;

/**
 * A demonstration of antialising on a Line object.
 */
@Purpose(
    htmlDescriptionKey = "com.ardor3d.example.basic.LineExample", //
    thumbnailPath = "com/ardor3d/example/media/thumbnails/basic_LineExample.jpg", //
    maxHeapMemory = 64)
public class LineExample extends ExampleBase {

  public static void main(final String[] args) {
    start(LineExample.class);
  }

  @Override
  protected void setupLight() {
    // no lights
  }

  @Override
  protected void initExample() {
    _canvas.setTitle("Lines");

    // Create a line with our example "makeLine" method. See method below.
    final Line regular = makeLine(new Grapher() {
      @Override
      double getYforX(final double x) {
        // Line eq will be y = x^3 - 2x^2
        return Math.pow(x, 3) - (2 * Math.pow(x, 2));
      }
    }, -5, 5, .25);
    // Set some properties on this line.
    // Set our line width - for lines this in in screen space... not world space.
    regular.setLineWidth(3.25f);
    // This line will be green
    regular.setDefaultColor(ColorRGBA.GREEN);

    // Add our line to the scene.
    _root.attachChild(regular);

    // Create a line with our example "makeLine" method. See method below.
    final Line antialiased = makeLine(new Grapher() {
      @Override
      double getYforX(final double x) {
        // Line eq will be y = -x^3 - 2(-x^2)
        return Math.pow(-x, 3) - (2 * Math.pow(-x, 2));
      }
    }, -5, 5, .25);
    // Set some properties on this line.
    // Set our line width - for lines this in in screen space... not world space.
    antialiased.setLineWidth(3.25f);
    // This line will be Cyan.
    antialiased.setDefaultColor(ColorRGBA.CYAN);
    // Finally let us make this antialiased... see also BlendState below.
    antialiased.setAntialiased(true);
    antialiased.setStipplePattern((short) 0xFFF0);

    // Antialiased lines work by adding small pixels to the line with alpha blending values.
    // To make use of this, you need to add a blend state that blends the source color
    // with the destination color using the source alpha.
    final BlendState blend = new BlendState();
    blend.setBlendEnabled(true);
    // use source color * source alpha + (1-source color) * destination color.
    // (Note: for an interesting effect, switch them so it is OneMinusSourceAlpha/SourceAlpha.
    // This will show you the pixels that are being added to your line in antialiasing.)
    blend.setSourceFunction(SourceFunction.SourceAlpha);
    blend.setDestinationFunction(DestinationFunction.OneMinusSourceAlpha);
    antialiased.setRenderState(blend);

    // Add our antialiased line to the scene.
    _root.attachChild(antialiased);
    MaterialUtil.autoMaterials(_root);
  }

  /**
   * Create a line, using the "equation" defined by the given grapher object.
   *
   * @param grapher
   *          the equation to use for our line.
   * @param min
   *          the X value to start from
   * @param max
   *          the X value to end at.
   * @param step
   *          the value to increase by, each step. We will insert a new point in the line at each of
   *          these steps.
   * @return the created Line object.
   */
  private Line makeLine(final Grapher grapher, final double min, final double max, final double step) {
    // This is just one way to make a line... You can also generate the FloatBuffer directly.
    // Make an array to hold the Vector3 points that will make up our Line.
    final ArrayList<Vector3> vertexList = new ArrayList<>();

    // Step through our range [min, max] by our step amount.
    for (double x = min; x <= max; x += step) {
      // At each point, generate a vector and add to the list
      final Vector3 vert = new Vector3(x, grapher.getYforX(x), -10);
      vertexList.add(vert);
    }

    // Create our Line object using the vertex data. We will not be providing normals, colors or texture
    // coords.
    final Line line =
        new Line("graphed line: " + grapher, vertexList.toArray(new Vector3[vertexList.size()]), null, null, null);
    final MeshData meshData = line.getMeshData();
    // The type of line we are making is a LineStrip. You can experiment and try making this Lines, or a
    // Line Loop.
    meshData.setIndexMode(IndexMode.LineStripAdjacency);
    meshData.setIndices(GeometryTool.generateAdjacencyIndices(meshData.getIndexMode(0), meshData.getVertexCount()));
    meshData.markIndicesDirty();
    // Update the model bound of our line to fit the data we've provided.
    line.updateModelBound();
    // Send back our Line.
    return line;
  }

  /**
   * Little helper class for defining a graphing equation on the X/Y plane as Y = f(X).
   */
  abstract class Grapher {
    abstract double getYforX(double x);
  }
}
