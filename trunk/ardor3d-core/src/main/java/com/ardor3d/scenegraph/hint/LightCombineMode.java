
package com.ardor3d.scenegraph.hint;

/**
 * Describes how to combine lights from ancestor LightStates.
 */
public enum LightCombineMode {
    /** When updating render states, turn off lighting for this spatial. */
    Off,

    /**
     * Combine lights starting from the root node and working towards the given Spatial. Ignore disabled states. Stop
     * combining when lights == MAX_LIGHTS_ALLOWED
     */
    CombineFirst,

    /**
     * Combine lights starting from the given Spatial and working up towards the root. Ignore disabled states. Stop
     * combining when lights == MAX_LIGHTS_ALLOWED
     */
    CombineClosest,

    /**
     * Similar to CombineClosest, but if a disabled state is encountered, it will stop combining at that point. Stop
     * combining when lights == MAX_LIGHTS_ALLOWED
     */
    CombineClosestEnabled,

    /** Inherit mode from parent. */
    Inherit,

    /** Do not combine lights, just use the most recent light state. */
    Replace;
}