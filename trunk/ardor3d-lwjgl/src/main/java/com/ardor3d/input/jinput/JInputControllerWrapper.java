/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.jinput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.java.games.input.Component;
import net.java.games.input.Component.Identifier;
import net.java.games.input.Controller;
import net.java.games.input.Controller.Type;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;

import com.ardor3d.input.ControllerEvent;
import com.ardor3d.input.ControllerInfo;
import com.ardor3d.input.ControllerState;
import com.ardor3d.input.ControllerWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;

public class JInputControllerWrapper implements ControllerWrapper {

    protected final Event _event = new Event();
    protected final List<ControllerEvent> _events = Collections.synchronizedList(new ArrayList<ControllerEvent>());
    protected JInputControllerEventIterator _eventsIt = new JInputControllerEventIterator();
    protected final List<ControllerInfo> _controllers = Lists.newArrayList();
    protected static boolean _inited = false;

    public PeekingIterator<ControllerEvent> getEvents() {
        init();
        if (!_eventsIt.hasNext()) {
            _eventsIt = new JInputControllerEventIterator();
        }
        for (final Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
            controller.poll();
            while (controller.getEventQueue().getNextEvent(_event)) {
                if (controller.getType() != Type.KEYBOARD && controller.getType() != Type.MOUSE) {
                    _events.add(createControllerEvent(controller, _event));
                }
            }
        }

        return _eventsIt;
    }

    @Override
    public int getControllerCount() {
        init();
        return _controllers.size();
    }

    @Override
    public ControllerInfo getControllerInfo(final int controllerIndex) {
        init();
        return _controllers.get(controllerIndex);
    }

    public synchronized void init() {
        if (_inited) {
            return;
        }

        try {
            ControllerEnvironment.getDefaultEnvironment();

            for (final Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
                if (controller.getType() != Type.KEYBOARD && controller.getType() != Type.MOUSE) {
                    _controllers.add(getControllerInfo(controller));
                    for (final Component component : controller.getComponents()) {
                        ControllerState.NOTHING.set(controller.getName(), component.getIdentifier().getName(), 0);
                    }
                }
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    protected ControllerInfo getControllerInfo(final Controller controller) {
        final List<String> axisNames = Lists.newArrayList();
        final List<String> buttonNames = Lists.newArrayList();

        for (final Component comp : controller.getComponents()) {
            if (comp.getIdentifier() instanceof Identifier.Axis) {
                axisNames.add(comp.getName());
            } else if (comp.getIdentifier() instanceof Identifier.Button) {
                buttonNames.add(comp.getName());
            }
        }

        return new ControllerInfo(controller.getName(), axisNames, buttonNames);
    }

    protected ControllerEvent createControllerEvent(final Controller controller, final Event event) {
        return new ControllerEvent(event.getNanos(), controller.getName(), event.getComponent().getIdentifier()
                .getName(), event.getValue());
    }

    protected class JInputControllerEventIterator extends AbstractIterator<ControllerEvent> implements
            PeekingIterator<ControllerEvent> {

        @Override
        protected ControllerEvent computeNext() {
            if (_events.size() > 0) {
                final ControllerEvent controllerEvent = _events.remove(0);
                return controllerEvent;
            } else {
                return endOfData();
            }
        }
    }
}