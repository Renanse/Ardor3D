/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.extension.interact;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ardor3d.extension.interact.data.SpatialState;
import com.ardor3d.extension.interact.filter.UpdateFilter;
import com.ardor3d.extension.interact.widget.AbstractInteractWidget;
import com.ardor3d.framework.Canvas;
import com.ardor3d.input.PhysicalLayer;
import com.ardor3d.input.logical.BasicTriggersApplier;
import com.ardor3d.input.logical.InputTrigger;
import com.ardor3d.input.logical.LogicalLayer;
import com.ardor3d.input.logical.TriggerAction;
import com.ardor3d.input.logical.TwoInputStates;
import com.ardor3d.renderer.Renderer;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.util.ReadOnlyTimer;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class InteractManager {

    /**
     * List of widgets currently managed by this manager.
     */
    protected final List<AbstractInteractWidget> _widgets = Lists.newArrayList();

    /**
     * The logical layer used by this manager to receive input events prior to forwarding them to the scene.
     */
    protected final LogicalLayer _logicalLayer = new LogicalLayer();

    /**
     * Internal flag indicating whether the last input event was consumed by the manager. This is used to decide if we
     * will forward the event to the next LogicalLayer.
     */
    protected AtomicBoolean _inputConsumed = new AtomicBoolean(false);

    /**
     * The widget currently active.
     */
    protected AbstractInteractWidget _activeWidget;

    /**
     * The current Spatial being targeted for interaction.
     */
    protected Spatial _spatialTarget;

    /**
     * Spatial state tracking.
     */
    protected SpatialState _state = new SpatialState();

    /**
     * List of filters to modify state prior to applying to a Spatial target.
     */
    protected List<UpdateFilter> _filters = Lists.newArrayList();

    public InteractManager() {
        setupLogicalLayer();
    }

    public void update(final ReadOnlyTimer timer) {
        for (final AbstractInteractWidget widget : _widgets) {
            if (!widget.isActiveUpdateOnly() || widget == _activeWidget) {
                widget.update(timer, this);
            }
        }
    }

    public void render(final Renderer renderer) {
        for (final AbstractInteractWidget widget : _widgets) {
            if (!widget.isActiveRenderOnly() || widget == _activeWidget) {
                widget.render(renderer, this);
            }
        }
    }

    protected void offerInputToWidgets(final Canvas source, final TwoInputStates inputStates) {
        if (_activeWidget != null) {
            _activeWidget.processInput(source, inputStates, _inputConsumed, this);
        }

        if (!_inputConsumed.get()) {
            for (final AbstractInteractWidget widget : _widgets) {
                if (widget != _activeWidget && !widget.isActiveInputOnly()) {
                    widget.processInput(source, inputStates, _inputConsumed, this);
                    if (_inputConsumed.get()) {
                        break;
                    }
                }
            }
        }

        if (_spatialTarget != null && _inputConsumed.get()) {
            // apply any filters to our state
            for (final UpdateFilter filter : _filters) {
                filter.applyFilter(this);
            }

            // apply state to target
            _state.applyState(_spatialTarget);

            // fire update event
            fireTargetDataUpdated();
        }
    }

    /**
     * Set up our logical layer with a trigger that hands input to the manager and saves whether it was "consumed".
     */
    private void setupLogicalLayer() {
        _logicalLayer.registerTrigger(new InputTrigger(new Predicate<TwoInputStates>() {
            public boolean apply(final TwoInputStates arg0) {
                // always trigger this.
                return true;
            }
        }, new TriggerAction() {
            public void perform(final Canvas source, final TwoInputStates inputStates, final double tpf) {
                if (_spatialTarget != null) {
                    _state.getTransform().set(_spatialTarget.getTransform());
                }
                _inputConsumed.set(false);
                offerInputToWidgets(source, inputStates);
            }
        }));
    }

    /**
     * Convenience method for setting up the manager's connection to the Ardor3D input system, along with a forwarding
     * address for input events that the manager does not care about.
     * 
     * @param canvas
     *            the canvas to register with
     * @param physicalLayer
     *            the physical layer to register with
     * @param forwardTo
     *            a LogicalLayer to send unconsumed (by the manager) input events to.
     */
    public void setupInput(final Canvas canvas, final PhysicalLayer physicalLayer, final LogicalLayer forwardTo) {
        // Set up this logical layer to listen for events from the given canvas and PhysicalLayer
        _logicalLayer.registerInput(canvas, physicalLayer);

        // Set up forwarding for events not consumed.
        if (forwardTo != null) {
            _logicalLayer.setApplier(new BasicTriggersApplier() {

                @Override
                public void checkAndPerformTriggers(final Set<InputTrigger> triggers, final Canvas source,
                        final TwoInputStates states, final double tpf) {
                    super.checkAndPerformTriggers(triggers, source, states, tpf);

                    if (!_inputConsumed.get()) {
                        // nothing consumed
                        forwardTo.getApplier().checkAndPerformTriggers(forwardTo.getTriggers(), source, states, tpf);
                    } else {
                        // consumed, do nothing.
                    }
                }
            });
        }
    }

    public void addWidget(final AbstractInteractWidget widget) {
        _widgets.add(widget);
    }

    public void removeWidget(final AbstractInteractWidget widget) {
        if (_activeWidget == widget) {
            _activeWidget = _widgets.isEmpty() ? null : _widgets.get(0);
        }
        _widgets.remove(widget);
    }

    public void clearWidgets() {
        _widgets.clear();
    }

    public void addFilter(final UpdateFilter filter) {
        _filters.add(filter);
    }

    public void removeFilter(final UpdateFilter filter) {
        _filters.remove(filter);
    }

    public void clearFilters() {
        _filters.clear();
    }

    public LogicalLayer getLogicalLayer() {
        return _logicalLayer;
    }

    public void setActiveWidget(final AbstractInteractWidget widget) {
        _activeWidget = widget;
    }

    public AbstractInteractWidget getActiveWidget() {
        return _activeWidget;
    }

    public void setSpatialTarget(final Spatial target) {
        if (_spatialTarget != target) {
            _spatialTarget = target;
            fireTargetChanged();
        }
    }

    public void fireTargetChanged() {
        for (final AbstractInteractWidget widget : _widgets) {
            widget.targetChanged(this);
        }
    }

    public void fireTargetDataUpdated() {
        for (final AbstractInteractWidget widget : _widgets) {
            widget.targetDataUpdated(this);
        }
    }

    public Spatial getSpatialTarget() {
        return _spatialTarget;
    }

    public SpatialState getSpatialState() {
        return _state;
    }

}
