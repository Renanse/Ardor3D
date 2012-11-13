
package com.ardor3d.extension.animation.skeletal.layer;

import com.ardor3d.extension.animation.skeletal.AnimationManager;
import com.ardor3d.extension.animation.skeletal.blendtree.BinaryLERPSource;
import com.ardor3d.extension.animation.skeletal.state.AbstractFiniteState;
import com.ardor3d.extension.animation.skeletal.state.AbstractTwoStateLerpTransition;
import com.google.common.collect.Maps;
import java.util.Map;

public class FadeTransitionStateMultilayer extends AbstractTwoStateLerpTransition {

    public FadeTransitionStateMultilayer(final String targetState, final double fadeTime, final BlendType type) {
        super(targetState, fadeTime, type);
    }

    @Override
    public AbstractFiniteState getTransitionState(final AbstractFiniteState callingState, final AnimationLayer layer) {

        // Grab current time as our start
        setStart(layer.getManager().getCurrentGlobalTime());

        // Set "current" start state
        setStateA(callingState);

        // Set destination state
        setStateB(layer.getSteadyState(getTargetState()));

        // Clear the _sourceData, the new state probably has different transform data
        if (_sourceData != null) {
            _sourceData.clear();
        }
        return this;
    }

    @Override
    public void update(double globalTime, AnimationLayer layer) {
        super.update(globalTime, layer);

        // Update both of our states
        if (getStateA() != null) {
            getStateA().update(globalTime, layer);
        }
        if (getStateB() != null) {
            getStateB().update(globalTime, layer);
        }
    }

    @Override
    public void postUpdate(final AnimationLayer layer) {
        // Post update both of our states
        if (getStateA() != null) {
            getStateA().postUpdate(layer);
        }
        if (getStateB() != null) {
            getStateB().postUpdate(layer);
        }
    }
    /**
     * The blended source data.
     */
    private Map<String, Object> _sourceData;

    @Override
    public Map<String, ? extends Object> getCurrentSourceData(final AnimationManager manager) {
        // Grab our data maps from the two states.
        // If state b is null, it will use the data on the base layer.
        final Map<String, ? extends Object> sourceAData = getStateA() != null ? getStateA().getCurrentSourceData(
                manager) : null;
        final Map<String, ? extends Object> sourceBData = getStateB() != null ? getStateB().getCurrentSourceData(
                manager) : manager.getBaseAnimationLayer().getCurrentSourceData();

        // Reuse previous _sourceData transforms to avoid re-creating
        // too many new transform data objects. This assumes that a
        // same state always returns the same transform data objects.
        if (_sourceData == null) {
            _sourceData = Maps.newHashMap();
        }

        return BinaryLERPSource.combineSourceData(sourceAData, sourceBData, getPercent(), _sourceData);

    }
}
