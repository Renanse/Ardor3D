package com.ardor3d.extension.animation.skeletal.layer;

import com.ardor3d.extension.animation.skeletal.state.AbstractFiniteState;
import com.ardor3d.extension.animation.skeletal.state.AbstractTwoStateLerpTransition.BlendType;

public class AnimationLayerFadeIn extends FadeTransitionStateMultilayer {

    /*
     * This is used on the interaction layer as a global catcher for all 
     * inconming animation when nothing is playing on that layer. It will smoothly 
     * blend the new animation with anithing that is playing on the base layer.
     */
    public AnimationLayerFadeIn(double fadeTime, BlendType type) {
        super(null, fadeTime, type);
    }

    @Override
    public AbstractFiniteState getTransitionState(final AbstractFiniteState callingState, final AnimationLayer layer) {
        callingState.resetClips(layer.getManager());
        super.getTransitionState(callingState, layer);
        return this;
    }

    @Override
    public AbstractFiniteState doTransition(final AbstractFiniteState callingState, final AnimationLayer layer) {
        /*
         * Allow to do a transition when there is no current state (layer.getCurrentState() == null) 
         * 
         * Bypass transition gate event, as this transition must happen immediatly
         */
        return getTransitionState(callingState, layer);

    }

    @Override
    protected void replaceOwnerWhenDone() {
        getLastStateOwner().replaceState(this, getStateA());
    }

    @Override
    protected double getPercent() {
        // Get the inverse percent for the fade in value
        return 1 - _percent;
    }
}
