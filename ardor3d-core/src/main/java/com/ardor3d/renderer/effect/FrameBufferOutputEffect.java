
package com.ardor3d.renderer.effect;

import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.RenderState.StateType;

public class FrameBufferOutputEffect extends RenderEffect {

    private BlendState _blend = null;

    @Override
    public void prepare(final EffectManager manager) {
        _steps.clear();
        _steps.add(new EffectStep_SetRenderTarget("*Framebuffer"));

        final EffectStep_RenderScreenOverlay drawStep = new EffectStep_RenderScreenOverlay();
        drawStep.getTargetMap().put("*Previous", 0);
        drawStep.getEnforcedStates().put(StateType.Blend, _blend);
        _steps.add(drawStep);

        super.prepare(manager);
    }

    public BlendState getBlend() {
        return _blend;
    }

    public void setBlend(final BlendState blend) {
        _blend = blend;
    }
}
