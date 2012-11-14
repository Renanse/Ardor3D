package com.ardor3d.extension.animation.skeletal.layer;

import com.ardor3d.extension.animation.skeletal.state.AbstractFiniteState;
import com.ardor3d.extension.animation.skeletal.state.SteadyState;

public class AnimationLayerInteractive extends AnimationLayer {

    public AnimationLayerInteractive(String name) {
        super(name);
    }

    /*
     * Merge an animation with any other animation or layer
     * without the need to define it for every single animation.
     * 
     */
    @Override
    public boolean doTransition(final String key) {

        final AbstractFiniteState state = getCurrentState();
        // see if current state has a transition
        if (state instanceof SteadyState) {
            final SteadyState steadyState = (SteadyState) state;
            setCurrentState(steadyState, false);
        } else if (state == null) {
            // If there is no current state on the interaction layer, transition to it.
            // But first, set the current state of this layer to one requested.
            SteadyState newState = (SteadyState) getSteadyState(key);
            if (newState != null) {
                AbstractFiniteState nextState = newState.doInTransition(this);
                if (nextState != null) {
                    if (nextState != state) {
                        setCurrentState(nextState, true);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
