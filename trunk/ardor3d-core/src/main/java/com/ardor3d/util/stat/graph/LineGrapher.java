/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.util.stat.graph;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.IndexMode;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.renderer.queue.RenderBucketType;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Point;
import com.ardor3d.scenegraph.hint.CullHint;
import com.ardor3d.util.Constants;
import com.ardor3d.util.geom.BufferUtils;
import com.ardor3d.util.stat.MultiStatSample;
import com.ardor3d.util.stat.StatCollector;
import com.ardor3d.util.stat.StatType;

public class LineGrapher extends AbstractStatGrapher implements TableLinkable {

    public static final StatType Vertical = new StatType("_lineGrapher_vert");
    public static final StatType Horizontal = new StatType("_lineGrapher_horiz");

    public enum ConfigKeys {
        ShowPoints, PointSize, PointColor, Antialias, ShowLines, Width, Stipple, Color, FrameAverage,
    }

    protected Node _graphRoot = new Node("root");
    protected Line _horizontals, _verticals;
    protected int _eventCount = 0;
    protected int _threshold = 1;
    protected float _startMarker = 0;
    private float _off;
    private float _vSpan;
    private static final int majorHBar = 20;
    private static final int majorVBar = 10;

    private final HashMap<StatType, LineEntry> _entries = new HashMap<StatType, LineEntry>();

    private BlendState _defBlendState = null;

    public LineGrapher(final int width, final int height, final Renderer renderer, final ContextCapabilities caps) {
        super(width, height, renderer, caps);

        // Setup our static horizontal graph lines
        createHLines();

        _defBlendState = new BlendState();
        _defBlendState.setEnabled(true);
        _defBlendState.setBlendEnabled(true);
        _defBlendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        _defBlendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        _graphRoot.setRenderState(_defBlendState);
        _graphRoot.getSceneHints().setCullHint(CullHint.Never);
    }

    public void statsUpdated() {
        if (!isEnabled() || !Constants.updateGraphs) {
            return;
        }

        // Turn off stat collection while we draw this graph.
        StatCollector.pause();

        // some basic stats:
        final int texWidth = _gWidth;
        final int texHeight = _gHeight;

        // On stat event:
        // - check if enough events have been triggered to cause an update.
        _eventCount++;
        _off += StatCollector.getStartOffset();
        if (_eventCount < _threshold) {
            return;
        } else {
            _eventCount = 0;
        }

        // - (Re)attach horizontal bars.
        if (!_graphRoot.equals(_horizontals.getParent())) {
            _graphRoot.attachChild(_horizontals);
        }

        // - Check if we have valid vertical bars:
        final float newVSpan = calcVSpan();
        if (_verticals == null || newVSpan != _vSpan) {
            _vSpan = newVSpan;
            createVLines();
        }
        _off %= (StatCollector.getSampleRate() * majorVBar);

        // - (Re)attach vertical bars.
        if (!_graphRoot.equals(_verticals.getParent())) {
            _graphRoot.attachChild(_verticals);
        }

        // - shift verticals based on current time
        shiftVerticals();

        for (final StatType type : _entries.keySet()) {
            _entries.get(type).visited = false;
            _entries.get(type).verts.clear();
        }

        // - For each sample, add points and extend the lines of the
        // corresponding Line objects.
        synchronized (StatCollector.getHistorical()) {
            for (int i = 0; i < StatCollector.getHistorical().size(); i++) {
                final MultiStatSample sample = StatCollector.getHistorical().get(i);
                for (final StatType type : _config.keySet()) {
                    if (sample.containsStat(type)) {
                        LineEntry entry = _entries.get(type);
                        // Prepare our entry object as needed.
                        if (entry == null || entry.maxSamples != StatCollector.getMaxSamples()) {
                            entry = new LineEntry(StatCollector.getMaxSamples(), type);
                            _entries.put(type, entry);
                        }

                        final double value = getBooleanConfig(type, ConfigKeys.FrameAverage.name(), false) ? sample
                                .getStatValue(type).getAverageValue() : sample.getStatValue(type).getAccumulatedValue();

                        final Vector3 point = new Vector3(i, value, 0);
                        // Now, add
                        entry.verts.add(point);

                        // Update min/max
                        if (entry.max < value) {
                            entry.max = value;
                        }

                        entry.visited = true;
                    } else {
                        final LineEntry entry = _entries.get(type);
                        if (entry != null) {
                            entry.verts.add(new Vector3(i, 0, 0));
                        }
                    }
                }
            }
        }

        for (final Iterator<StatType> i = _entries.keySet().iterator(); i.hasNext();) {
            final LineEntry entry = _entries.get(i.next());
            // - Go through the entries list and remove any that were not visited.
            if (!entry.visited) {
                entry.line.removeFromParent();
                entry.point.removeFromParent();
                i.remove();
                continue;
            }

            // - Update the Point and Line params with the verts and count.
            final FloatBuffer fb = BufferUtils.createFloatBuffer(entry.verts.toArray(new Vector3[entry.verts.size()]));
            entry.point.getMeshData().setVertexBuffer(fb);
            final double scaleWidth = texWidth / (StatCollector.getMaxSamples() - 1.0);
            final double scaleHeight = texHeight / (entry.max * 1.02);
            entry.point.setScale(new Vector3(scaleWidth, scaleHeight, 1));
            entry.line.getMeshData().setVertexBuffer(fb);
            entry.line.setScale(new Vector3(scaleWidth, scaleHeight, 1));
            fb.rewind();

            // - attach point/line to root as needed
            if (!_graphRoot.equals(entry.line.getParent())) {
                _graphRoot.attachChild(entry.line);
            }
            if (!_graphRoot.equals(entry.point.getParent())) {
                _graphRoot.attachChild(entry.point);
            }
        }

        // - Now, draw to texture via a TextureRenderer
        _graphRoot.updateGeometricState(0, true);
        _textureRenderer.render(_graphRoot, _texture, Renderer.BUFFER_COLOR_AND_DEPTH);

        // Turn stat collection back on.
        StatCollector.resume();
    }

    private float calcVSpan() {
        return _textureRenderer.getWidth() * majorVBar / StatCollector.getMaxSamples();
    }

    private void shiftVerticals() {
        final int texWidth = _textureRenderer.getWidth();
        final double xOffset = -(_off * texWidth) / (StatCollector.getMaxSamples() * StatCollector.getSampleRate());
        final ReadOnlyVector3 trans = _verticals.getTranslation();
        _verticals.setTranslation(xOffset, trans.getY(), trans.getZ());
    }

    public int getThreshold() {
        return _threshold;
    }

    public void setThreshold(final int threshold) {
        _threshold = threshold;
    }

    // - Setup horizontal bars
    private void createHLines() {
        // some basic stats:
        final int texWidth = _textureRenderer.getWidth();
        final int texHeight = _textureRenderer.getHeight();

        final FloatBuffer verts = BufferUtils.createVector3Buffer((100 / majorHBar) * 2);

        final float div = texHeight * majorHBar / 100f;

        for (int y = 0, i = 0; i < verts.capacity(); i += 6, y += div) {
            verts.put(0).put(y).put(0);
            verts.put(texWidth).put(y).put(0);
        }

        _horizontals = new Line("horiz", verts, null, null, null);
        _horizontals.getMeshData().setIndexMode(IndexMode.Lines);
        _horizontals.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);

        _horizontals.setDefaultColor(getColorConfig(LineGrapher.Horizontal, ConfigKeys.Color.name(), new ColorRGBA(
                ColorRGBA.BLUE)));
        _horizontals.setLineWidth(getIntConfig(LineGrapher.Horizontal, ConfigKeys.Width.name(), 1));
        _horizontals
                .setStipplePattern(getShortConfig(LineGrapher.Horizontal, ConfigKeys.Stipple.name(), (short) 0xFF00));
        _horizontals.setAntialiased(getBooleanConfig(LineGrapher.Horizontal, ConfigKeys.Antialias.name(), true));
    }

    // - Setup enough vertical bars to have one at every (10 X samplerate)
    // secs... we'll need +1 bar.
    private void createVLines() {
        // some basic stats:
        final int texWidth = _textureRenderer.getWidth();
        final int texHeight = _textureRenderer.getHeight();

        final FloatBuffer verts = BufferUtils.createVector3Buffer(((int) (texWidth / _vSpan) + 1) * 2);

        for (float x = _vSpan; x <= texWidth + _vSpan; x += _vSpan) {
            verts.put(x).put(0).put(0);
            verts.put(x).put(texHeight).put(0);
        }

        _verticals = new Line("vert", verts, null, null, null);
        _verticals.getMeshData().setIndexMode(IndexMode.Lines);
        _verticals.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);

        _verticals.setDefaultColor(getColorConfig(LineGrapher.Vertical, ConfigKeys.Color.name(), new ColorRGBA(
                ColorRGBA.RED)));
        _verticals.setLineWidth(getIntConfig(LineGrapher.Vertical, ConfigKeys.Width.name(), 1));
        _verticals.setStipplePattern(getShortConfig(LineGrapher.Vertical, ConfigKeys.Stipple.name(), (short) 0xFF00));
        _verticals.setAntialiased(getBooleanConfig(LineGrapher.Vertical, ConfigKeys.Antialias.name(), true));
    }

    class LineEntry {
        public List<Vector3> verts = new ArrayList<Vector3>();
        public int maxSamples;
        public double min = 0;
        public double max = 10;
        public boolean visited;
        public Point point;
        public Line line;

        public LineEntry(final int maxSamples, final StatType type) {
            this.maxSamples = maxSamples;

            point = new Point("p", BufferUtils.createVector3Buffer(maxSamples), null, null, null);
            point.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);

            point.setDefaultColor(getColorConfig(type, ConfigKeys.PointColor.name(), new ColorRGBA(ColorRGBA.WHITE)));
            point.setPointSize(getIntConfig(type, ConfigKeys.PointSize.name(), 5));
            point.setAntialiased(getBooleanConfig(type, ConfigKeys.Antialias.name(), true));
            if (!getBooleanConfig(type, ConfigKeys.ShowPoints.name(), false)) {
                point.getSceneHints().setCullHint(CullHint.Always);
            }

            line = new Line("l", BufferUtils.createVector3Buffer(maxSamples), null, null, null);
            line.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
            line.getMeshData().setIndexMode(IndexMode.LineStrip);

            line.setDefaultColor(getColorConfig(type, ConfigKeys.Color.name(), new ColorRGBA(ColorRGBA.LIGHT_GRAY)));
            line.setLineWidth(getIntConfig(type, ConfigKeys.Width.name(), 3));
            line.setStipplePattern(getShortConfig(type, ConfigKeys.Stipple.name(), (short) 0xFFFF));
            line.setAntialiased(getBooleanConfig(type, ConfigKeys.Antialias.name(), true));
            if (!getBooleanConfig(type, ConfigKeys.ShowLines.name(), true)) {
                line.getSceneHints().setCullHint(CullHint.Always);
            }
        }
    }

    public Line updateLineKey(final StatType type, Line lineKey) {
        if (lineKey == null) {
            lineKey = new Line("lk", BufferUtils.createVector3Buffer(2), null, null, null);
            final FloatBuffer fb = BufferUtils.createFloatBuffer(new Vector3[] { new Vector3(0, 0, 0),
                    new Vector3(30, 0, 0) });
            fb.rewind();
            lineKey.getMeshData().setVertexBuffer(fb);
        }

        lineKey.getSceneHints().setRenderBucketType(RenderBucketType.Ortho);
        lineKey.getMeshData().setIndexMode(IndexMode.LineStrip);

        lineKey.setDefaultColor(getColorConfig(type, ConfigKeys.Color.name(), new ColorRGBA(ColorRGBA.LIGHT_GRAY)));
        lineKey.setLineWidth(getIntConfig(type, ConfigKeys.Width.name(), 3));
        lineKey.setStipplePattern(getShortConfig(type, ConfigKeys.Stipple.name(), (short) 0xFFFF));
        lineKey.setAntialiased(getBooleanConfig(type, ConfigKeys.Antialias.name(), true));
        if (!getBooleanConfig(type, ConfigKeys.ShowLines.name(), true)) {
            lineKey.getSceneHints().setCullHint(CullHint.Always);
        }

        return lineKey;
    }

    @Override
    public void reset() {
        synchronized (StatCollector.getHistorical()) {
            for (final Iterator<StatType> i = _entries.keySet().iterator(); i.hasNext();) {
                final LineEntry entry = _entries.get(i.next());
                entry.line.removeFromParent();
                entry.point.removeFromParent();
                i.remove();
            }
        }
    }
}
