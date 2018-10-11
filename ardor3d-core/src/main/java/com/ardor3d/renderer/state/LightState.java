/**
 * Copyright (c) 2008-2012 Bird Dog Games, Inc..
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.renderer.state;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.ardor3d.light.Light;
import com.ardor3d.renderer.ContextCapabilities;
import com.ardor3d.renderer.state.record.LightStateRecord;
import com.ardor3d.renderer.state.record.StateRecord;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.util.export.InputCapsule;
import com.ardor3d.util.export.OutputCapsule;

/**
 * <code>LightState</code> maintains a collection of lights up to the set number of maximum lights allowed. Any subclass
 * of <code>Light</code> can be added to the light state. Each light is processed and used to modify the color of the
 * scene.
 */
public class LightState extends RenderState {

    /**
     * Debug flag for turning off all lighting.
     */
    public static boolean LIGHTS_ENABLED = true;

    /**
     * defines the maximum number of lights that are allowed to be maintained at one time.
     */
    public static final int MAX_LIGHTS_ALLOWED = 8;

    // holds the lights
    private List<Light> lightList;

    /**
     * Constructor instantiates a new <code>LightState</code> object. Initially there are no lights set.
     */
    public LightState() {
        lightList = new ArrayList<>();
    }

    @Override
    public StateType getType() {
        return StateType.Light;
    }

    /**
     *
     * <code>attach</code> places a light in the queue to be processed. If there are already eight lights placed in the
     * queue, the light is ignored and false is returned. Otherwise, true is returned to indicate success.
     *
     * @param light
     *            the light to add to the queue.
     * @return true if the light was added successfully, false if there are already eight lights in the queue.
     */
    public boolean attach(final Light light) {
        if (!lightList.contains(light)) {
            lightList.add(light);
            setNeedsRefresh(true);
            return true;
        }
        return false;
    }

    /**
     *
     * <code>detach</code> removes a light from the queue for processing.
     *
     * @param light
     *            the light to be removed.
     */
    public void detach(final Light light) {
        lightList.remove(light);
        setNeedsRefresh(true);
    }

    /**
     *
     * <code>detachAll</code> clears the queue of all lights to be processed.
     *
     */
    public void detachAll() {
        lightList.clear();
        setNeedsRefresh(true);
    }

    /**
     * Retrieves all lights handled by this LightState
     *
     * @return List of lights handled
     */
    public List<Light> getLightList() {
        return lightList;
    }

    /**
     *
     * <code>get</code> retrieves a particular light defined by an index. If there exists no light at a particular
     * index, null is returned.
     *
     * @param i
     *            the index to retrieve the light from the queue.
     * @return the light at the given index, null if no light exists at this index.
     */
    public Light get(final int i) {
        if (lightList.size() <= i) {
            return null;
        }
        return lightList.get(i);
    }

    /**
     * @return the number of lights currently in this state.
     */
    public int count() {
        return lightList.size() > MAX_LIGHTS_ALLOWED ? MAX_LIGHTS_ALLOWED : lightList.size();
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.writeSavableList(lightList, "lightList", new ArrayList<Light>());
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        lightList = capsule.readSavableList("lightList", new ArrayList<Light>());
    }

    @Override
    public RenderState extract(final Stack<? extends RenderState> stack, final Spatial spat) {
        if (spat == null) {
            return stack.peek();
        }

        final LightCombineMode mode = spat.getSceneHints().getLightCombineMode();

        final Mesh mesh = (Mesh) spat;
        LightState lightState = mesh.getLightState();
        if (lightState == null) {
            lightState = new LightState();
            mesh.setLightState(lightState);
        }

        lightState.detachAll();

        if (mode == LightCombineMode.Replace || (mode != LightCombineMode.Off && stack.size() == 1)) {
            // todo: use dummy state if off?

            final LightState copyLightState = (LightState) stack.peek();
            copyLightState(copyLightState, lightState);
        } else {
            // accumulate the lights in the stack into a single LightState object
            final Object states[] = stack.toArray();
            boolean foundEnabled = false;
            switch (mode) {
                case CombineClosest:
                case CombineClosestEnabled:
                    for (int iIndex = states.length - 1; iIndex >= 0; iIndex--) {
                        final LightState pkLState = (LightState) states[iIndex];
                        if (!pkLState.isEnabled()) {
                            if (mode == LightCombineMode.CombineClosestEnabled) {
                                break;
                            }

                            continue;
                        }

                        foundEnabled = true;
                        copyLightState(pkLState, lightState);
                    }
                    break;
                case CombineFirst:
                    for (int iIndex = 0, max = states.length; iIndex < max; iIndex++) {
                        final LightState pkLState = (LightState) states[iIndex];
                        if (!pkLState.isEnabled()) {
                            continue;
                        }

                        foundEnabled = true;
                        copyLightState(pkLState, lightState);
                    }
                    break;
                default:
                    break;
            }
            lightState.setEnabled(foundEnabled);
        }

        return lightState;
    }

    private static void copyLightState(final LightState source, final LightState destination) {
        destination.setEnabled(source.isEnabled());
        destination.setNeedsRefresh(true);

        for (int i = 0, maxL = source.getLightList().size(); i < maxL; i++) {
            final Light pkLight = source.get(i);
            if (pkLight != null) {
                destination.attach(pkLight);
            }
        }
    }

    @Override
    public StateRecord createStateRecord(final ContextCapabilities caps) {
        return new LightStateRecord();
    }
}
