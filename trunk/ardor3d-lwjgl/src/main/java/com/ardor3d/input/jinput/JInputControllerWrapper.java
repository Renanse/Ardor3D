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
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Event;
import net.java.games.input.Controller.Type;

import com.ardor3d.input.ControllerEvent;
import com.ardor3d.input.ControllerState;
import com.ardor3d.input.ControllerWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

public class JInputControllerWrapper implements ControllerWrapper {

    private final Event event = new Event();
    private final List<ControllerEvent> events = Collections.synchronizedList(new ArrayList<ControllerEvent>());
    private JInputControllerEventIterator eventsIt = new JInputControllerEventIterator();
    private ControllerState blankState = null;

    public PeekingIterator<ControllerEvent> getEvents() {
        if (!eventsIt.hasNext()) {
            eventsIt = new JInputControllerEventIterator();
        }
        for (final Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
            controller.poll();
            while (controller.getEventQueue().getNextEvent(event)) {
                if (controller.getType() != Type.KEYBOARD && controller.getType() != Type.MOUSE) {
                    events.add(createControllerEvent(controller, event));
                }
            }
        }

        return eventsIt;
    }

    public void init() {
        try {
            ControllerEnvironment.getDefaultEnvironment();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private ControllerEvent createControllerEvent(final Controller controller, final Event event) {
        return new ControllerEvent(event.getNanos(), controller.getName(), event.getComponent().getIdentifier()
                .getName(), event.getValue());
    }

    private class JInputControllerEventIterator extends AbstractIterator<ControllerEvent> implements
            PeekingIterator<ControllerEvent> {

        @Override
        protected ControllerEvent computeNext() {
            if (events.size() > 0) {
                final ControllerEvent controllerEvent = events.remove(0);
                return controllerEvent;
            } else {
                return endOfData();
            }
        }
    }

    public synchronized ControllerState getBlankState() {
        if (blankState == null) {
            blankState = new ControllerState();
            for (final Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
                if (controller.getType() != Type.KEYBOARD && controller.getType() != Type.MOUSE) {
                    for (final Component component : controller.getComponents()) {
                        blankState.set(controller.getName(), component.getIdentifier().getName(), 0);
                    }
                }
            }
        }

        return blankState;
    }
}
