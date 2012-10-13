/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it 
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.input.lwjgl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Controller;
import org.lwjgl.input.Controllers;

import com.ardor3d.input.ControllerEvent;
import com.ardor3d.input.ControllerState;
import com.ardor3d.input.ControllerWrapper;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

public class LwjglControllerWrapper implements ControllerWrapper {

    private static boolean inited = false;
    private final List<ControllerEvent> events = Collections.synchronizedList(new ArrayList<ControllerEvent>());
    private LwjglControllerEventIterator eventsIt = new LwjglControllerEventIterator();
    private ControllerState blankState = null;

    public synchronized ControllerState getBlankState() {
        init();

        if (blankState == null) {
            blankState = new ControllerState();
            for (int i = 0; i < Controllers.getControllerCount(); i++) {
                final Controller controller = Controllers.getController(i);
                for (int j = 0; j < controller.getAxisCount(); j++) {
                    blankState.set(controller.getName(), controller.getAxisName(j), 0);
                }
                for (int j = 0; j < controller.getButtonCount(); j++) {
                    blankState.set(controller.getName(), controller.getButtonName(j), 0);
                }
            }
        }

        return blankState;
    }

    private class LwjglControllerEventIterator extends AbstractIterator<ControllerEvent> implements
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

    public PeekingIterator<ControllerEvent> getEvents() {
        init();
        if (!eventsIt.hasNext()) {
            eventsIt = new LwjglControllerEventIterator();
        }

        while (Controllers.next()) {
            final Controller source = Controllers.getEventSource();
            if (Controllers.isEventButton()) {
                events.add(new ControllerEvent(Controllers.getEventNanoseconds(), source.getName(), source
                        .getButtonName(Controllers.getEventControlIndex()), source.isButtonPressed(Controllers
                        .getEventControlIndex()) ? 0f : 1f));
            } else if (Controllers.isEventAxis()) {
                events.add(new ControllerEvent(Controllers.getEventNanoseconds(), source.getName(), source
                        .getAxisName(Controllers.getEventControlIndex()), source.getAxisValue(Controllers
                        .getEventControlIndex())));
            }
        }

        return eventsIt;
    }

    public void init() {
        if (!inited) {
            try {
                Controllers.create();
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                inited = true;
            }
        }
    }

}
