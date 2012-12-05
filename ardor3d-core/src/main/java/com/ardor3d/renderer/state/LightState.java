/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
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
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.type.ReadOnlyColorRGBA;
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

    /**
     * When applied to lightMask, implies ambient light should be set to 0 for this lightstate
     */
    public static final int MASK_AMBIENT = 1;

    /**
     * When applied to lightMask, implies diffuse light should be set to 0 for this lightstate
     */
    public static final int MASK_DIFFUSE = 2;

    /**
     * When applied to lightMask, implies specular light should be set to 0 for this lightstate
     */
    public static final int MASK_SPECULAR = 4;

    /**
     * When applied to lightMask, implies global ambient light should be set to 0 for this lightstate
     */
    public static final int MASK_GLOBALAMBIENT = 8;

    // holds the lights
    private List<Light> lightList;

    // mask value - default is no masking
    protected int lightMask = 0;

    // mask value stored by pushLightMask, retrieved by popLightMask
    protected int backLightMask = 0;

    /** When true, both sides of the model will be lighted. */
    protected boolean twoSidedOn = true;

    protected ColorRGBA _globalAmbient = new ColorRGBA(DEFAULT_GLOBAL_AMBIENT);

    public static final ReadOnlyColorRGBA DEFAULT_GLOBAL_AMBIENT = new ColorRGBA(0, 0, 0, 1);

    /**
     * When true, the eye position (as opposed to just the view direction) will be taken into account when computing
     * specular reflections.
     */
    protected boolean localViewerOn;

    /**
     * When true, specular highlights will be computed separately and added to fragments after texturing.
     */
    protected boolean separateSpecularOn;

    /**
     * Constructor instantiates a new <code>LightState</code> object. Initially there are no lights set.
     */
    public LightState() {
        lightList = new ArrayList<Light>();
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
        return lightList.get(i);
    }

    /**
     * 
     * <code>getNumberOfChildren</code> returns the number of lights currently in the queue.
     * 
     * @return the number of lights currently in the queue.
     */
    public int getNumberOfChildren() {
        return lightList.size() > MAX_LIGHTS_ALLOWED ? MAX_LIGHTS_ALLOWED : lightList.size();
    }

    /**
     * Sets if two sided lighting should be enabled for this LightState. Two sided lighting will cause the back of
     * surfaces to be colored using the inverse of the surface normal as well as the Material properties set for
     * MaterialFace.Back.
     * 
     * @param twoSidedOn
     *            If true, two sided lighting is enabled.
     */
    public void setTwoSidedLighting(final boolean twoSidedOn) {
        this.twoSidedOn = twoSidedOn;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current state of two sided lighting for this LightState. By default, it is off.
     * 
     * @return True if two sided lighting is enabled.
     */
    public boolean getTwoSidedLighting() {
        return twoSidedOn;
    }

    /**
     * Sets if local viewer mode should be enabled for this LightState.
     * 
     * @param localViewerOn
     *            If true, local viewer mode is enabled.
     */
    public void setLocalViewer(final boolean localViewerOn) {
        this.localViewerOn = localViewerOn;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current state of local viewer mode for this LightState. By default, it is off.
     * 
     * @return True if local viewer mode is enabled.
     */
    public boolean getLocalViewer() {
        return localViewerOn;
    }

    /**
     * Sets if separate specular mode should be enabled for this LightState.
     * 
     * @param separateSpecularOn
     *            If true, separate specular mode is enabled.
     */
    public void setSeparateSpecular(final boolean separateSpecularOn) {
        this.separateSpecularOn = separateSpecularOn;
        setNeedsRefresh(true);
    }

    /**
     * Returns the current state of separate specular mode for this LightState. By default, it is off.
     * 
     * @return True if separate specular mode is enabled.
     */
    public boolean getSeparateSpecular() {
        return separateSpecularOn;
    }

    public void setGlobalAmbient(final ReadOnlyColorRGBA color) {
        _globalAmbient.set(color);
        setNeedsRefresh(true);
    }

    /**
     * 
     * @param store
     * @return
     */
    public ReadOnlyColorRGBA getGlobalAmbient() {
        return _globalAmbient;
    }

    /**
     * @return Returns the lightMask - default is 0 or not masked.
     */
    public int getLightMask() {
        return lightMask;
    }

    /**
     * <code>setLightMask</code> sets what attributes of this lightstate to apply as an int comprised of bitwise or'ed
     * values.
     * 
     * @param lightMask
     *            The lightMask to set.
     */
    public void setLightMask(final int lightMask) {
        this.lightMask = lightMask;
        setNeedsRefresh(true);
    }

    /**
     * Saves the light mask to a back store. That backstore is recalled with popLightMask. Despite the name, this is not
     * a stack and additional pushes will simply overwrite the backstored value.
     */
    public void pushLightMask() {
        backLightMask = lightMask;
    }

    /**
     * Recalls the light mask from a back store or 0 if none was pushed.
     * 
     * @see com.ardor3d.renderer.state.LightState#pushLightMask()
     */
    public void popLightMask() {
        lightMask = backLightMask;
    }

    @Override
    public void write(final OutputCapsule capsule) throws IOException {
        super.write(capsule);
        capsule.writeSavableList(lightList, "lightList", new ArrayList<Light>());
        capsule.write(lightMask, "lightMask", 0);
        capsule.write(backLightMask, "backLightMask", 0);
        capsule.write(twoSidedOn, "twoSidedOn", false);
        capsule.write(_globalAmbient, "globalAmbient", new ColorRGBA(DEFAULT_GLOBAL_AMBIENT));
        capsule.write(localViewerOn, "localViewerOn", false);
        capsule.write(separateSpecularOn, "separateSpecularOn", false);
    }

    @Override
    public void read(final InputCapsule capsule) throws IOException {
        super.read(capsule);
        lightList = capsule.readSavableList("lightList", new ArrayList<Light>());
        lightMask = capsule.readInt("lightMask", 0);
        backLightMask = capsule.readInt("backLightMask", 0);
        twoSidedOn = capsule.readBoolean("twoSidedOn", false);
        _globalAmbient = (ColorRGBA) capsule.readSavable("globalAmbient", new ColorRGBA(DEFAULT_GLOBAL_AMBIENT));
        localViewerOn = capsule.readBoolean("localViewerOn", false);
        separateSpecularOn = capsule.readBoolean("separateSpecularOn", false);
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
                case Off:
                    break;
            }
            lightState.setEnabled(foundEnabled);
        }

        return lightState;
    }

    private static void copyLightState(final LightState source, final LightState destination) {
        destination.setTwoSidedLighting(source.getTwoSidedLighting());
        destination.setLocalViewer(source.getLocalViewer());
        destination.setSeparateSpecular(source.getSeparateSpecular());
        destination.setEnabled(source.isEnabled());
        destination.setGlobalAmbient(source.getGlobalAmbient());
        destination.setLightMask(source.getLightMask());
        destination.setNeedsRefresh(true);

        for (int i = 0, maxL = source.getLightList().size(); i < maxL; i++) {
            final Light pkLight = source.get(i);
            if (pkLight != null) {
                destination.attach(pkLight);
            }
        }
    }

    @Override
    public StateRecord createStateRecord() {
        return new LightStateRecord();
    }
}
