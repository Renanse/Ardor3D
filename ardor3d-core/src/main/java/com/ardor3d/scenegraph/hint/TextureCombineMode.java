
package com.ardor3d.scenegraph.hint;

/**
 * Describes how to combine textures from ancestor TextureStates.
 */
public enum TextureCombineMode {
    /** When updating render states, turn off texturing for this spatial. */
    Off,

    /**
     * Combine textures starting from the root node and working towards the given Spatial. Ignore disabled states.
     */
    CombineFirst,

    /**
     * Combine textures starting from the given Spatial and working towards the root. Ignore disabled states. (Default)
     */
    CombineClosest,

    /**
     * Similar to CombineClosest, but if a disabled state is encountered, it will stop combining at that point.
     */
    CombineClosestEnabled,

    /** Inherit mode from parent. */
    Inherit,

    /** Do not combine textures, just use the most recent texture state. */
    Replace;
}